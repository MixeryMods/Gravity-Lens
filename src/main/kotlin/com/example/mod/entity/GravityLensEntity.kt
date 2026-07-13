package com.example.mod.entity

import com.example.mod.Config
import com.example.mod.item.GravityLensItem
import com.example.mod.physics.AttunementFilter
import com.example.mod.physics.GravityField
import com.example.mod.registry.ModEntities
import com.example.mod.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * The Gravity Lens entity — a single entity with two states (Requirement 2):
 *
 *  - **FLYING**: thrown along the player's look vector, affected by light
 *    gravity/drag. On contact with any solid block face it snaps to the impact
 *    point and switches to ATTACHED.
 *  - **ATTACHED**: stationary, anchored to a block. Every server tick it reads
 *    the strongest adjacent redstone signal, and if powered, projects a
 *    spherical gravity field that additively bends the velocity of eligible
 *    entities within range (Requirements 3-7). Counts down a lifetime timer and
 *    can be recalled by the player (Requirement 8).
 *
 * Only the ATTACHED-state field logic runs on the server; velocity changes are
 * strictly additive and never overwrite existing motion (project constraint).
 */
class GravityLensEntity(type: EntityType<GravityLensEntity>, level: Level) :
    Entity(type, level) {

    enum class State { FLYING, ATTACHED }

    // Enchantment snapshot taken at throw time (Requirement 9). Persisted.
    var enchantments: LensEnchantments = LensEnchantments.NONE

    // Remaining deployed lifetime in ticks (Requirement 8). Persisted.
    private var lifetimeTicks: Int = Config.DEFAULT_LIFETIME_TICKS

    // Block this lens is stuck to, for redstone sampling. Persisted.
    private var attachedPos: BlockPos? = null

    private var particleClock: Int = 0

    // ------------------------------------------------------------------ data

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(DATA_ATTACHED, false)
    }

    var isAttached: Boolean
        get() = entityData.get(DATA_ATTACHED)
        private set(value) = entityData.set(DATA_ATTACHED, value)

    private val state: State
        get() = if (isAttached) State.ATTACHED else State.FLYING

    // --------------------------------------------------------------- ticking

    override fun tick() {
        super.tick()
        when (state) {
            State.FLYING -> tickFlying()
            State.ATTACHED -> tickAttached()
        }
    }

    /** Ballistic flight until we hit a solid block (Requirement 2). */
    private fun tickFlying() {
        val from = position()
        var motion = deltaMovement

        // Detect a solid-block impact along this tick's motion segment.
        val to = from.add(motion)
        val hit = level().clip(
            ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this,
            ),
        )

        if (hit.type == HitResult.Type.BLOCK) {
            if (!level().isClientSide) attachAt(hit.blockPos, hit.location)
            return
        }

        // No hit: integrate simple ballistic motion.
        setPos(to.x, to.y, to.z)
        motion = motion.scale(Config.FLYING_DRAG)
        motion = Vec3(motion.x, motion.y - Config.FLYING_GRAVITY, motion.z)
        deltaMovement = motion
    }

    /** Snap onto the impacted block face and flip to ATTACHED. */
    private fun attachAt(blockPos: BlockPos, location: Vec3) {
        attachedPos = blockPos
        deltaMovement = Vec3.ZERO
        setPos(location.x, location.y, location.z)
        isAttached = true
        playLensSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f, 1.2f)
    }

    /** Field projection, redstone, lifetime, particles (Requirements 3-8, 10). */
    private fun tickAttached() {
        deltaMovement = Vec3.ZERO

        // Lifetime countdown (Requirement 8) — Persistence extends it.
        if (--lifetimeTicks <= 0) {
            if (!level().isClientSide) discard()
            return
        }

        val serverLevel = level() as? ServerLevel ?: return
        val anchor = attachedPos ?: BlockPos.containing(position())

        // Redstone gate (Requirement 7): strongest adjacent power.
        val signal = serverLevel.getBestNeighborSignal(anchor)
        if (signal <= 0) return // signal 0 => no scanning, no physics at all.

        applyField(serverLevel, signal)
        emitParticles(serverLevel)
    }

    // ------------------------------------------------------------- the field

    private fun applyField(level: ServerLevel, signal: Int) {
        val radius = GravityField.effectiveRadius(enchantments.resonance)
        val center = position()
        val filter = AttunementFilter.fromLevel(enchantments.attunement)

        // Broad-phase: AABB query, then narrow-phase sphere test.
        val box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2)
        val candidates = level.getEntities(this, box) { isEligible(it, filter) }

        for (entity in candidates) {
            val entityPos = entity.position()
            if (!GravityField.withinSphere(center, entityPos, radius)) continue

            val mass = massProxy(entity)
            val force = GravityField.computeForce(
                center = center,
                entityPos = entityPos,
                radius = radius,
                signal = signal,
                resonanceLevel = enchantments.resonance,
                mass = mass,
            )
            if (force == Vec3.ZERO) continue

            // STRICTLY additive — never replace existing velocity (constraint).
            entity.addDeltaMovement(force)

            // Players need explicit velocity sync or the client won't feel it.
            if (entity is Player) entity.hurtMarked = true
        }
    }

    /**
     * Entity filtering (Requirement 6) + Attunement exclusion (Requirement 9).
     * Excludes vehicles and other lenses to avoid recursive physics loops
     * (constraint).
     */
    private fun isEligible(entity: Entity, filter: AttunementFilter): Boolean {
        // Global exclusions first.
        if (entity is GravityLensEntity) return false
        if (entity is AbstractBoat || entity is AbstractMinecart) return false
        if (!entity.isAlive) return false

        val isProjectile = entity is Projectile
        val isItem = entity is ItemEntity
        val isMobOrPlayer = entity is LivingEntity

        if (!(isProjectile || isItem || isMobOrPlayer)) return false

        return when (filter) {
            AttunementFilter.ALL -> true
            AttunementFilter.PROJECTILES_ONLY -> isProjectile
            AttunementFilter.MOBS_ONLY -> isMobOrPlayer
            AttunementFilter.ITEMS_ONLY -> isItem
        }
    }

    /**
     * Mass proxy for inverse-mass scaling (Requirement 6). Projectiles and
     * dropped items are treated as ~unit mass so they curve strongly; living
     * entities scale with bounding-box volume so heavy mobs resist more.
     */
    private fun massProxy(entity: Entity): Double {
        if (entity !is LivingEntity) return 1.0
        val bb = entity.boundingBox
        return bb.xsize * bb.ysize * bb.zsize
    }

    // -------------------------------------------------------------- visuals

    private fun emitParticles(level: ServerLevel) {
        if (++particleClock % Config.PARTICLE_INTERVAL_TICKS != 0) return
        val c = position()
        level.sendParticles(
            ParticleTypes.PORTAL,
            c.x, c.y, c.z,
            2,          // count
            0.15, 0.15, 0.15, // spread
            0.01,       // speed
        )
    }

    // ------------------------------------------------------------ retrieval

    /** Direct interaction retrieval (Requirement 8). */
    override fun interact(player: Player, hand: InteractionHand, location: Vec3): InteractionResult {
        if (!level().isClientSide) {
            retrieveTo(player)
        }
        return InteractionResult.SUCCESS
    }

    /**
     * Give the lens back (re-encoding stored enchantments) and destroy the
     * field instantly without touching any nearby entity's velocity — trapped
     * or orbiting entities keep their tangential motion (Requirement 8 +
     * constraint).
     */
    fun retrieveTo(player: Player) {
        val stack = ItemStack(ModItems.GRAVITY_LENS)
        LensEnchantments.write(level(), stack, enchantments)
        if (!player.addItem(stack)) {
            player.drop(stack, false)
        }
        playLensSound(SoundEvents.ITEM_PICKUP, 0.4f, 1.5f)
        discard()
    }

    // --------------------------------------------------------------- persist

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.putBoolean("Attached", isAttached)
        output.putInt("Lifetime", lifetimeTicks)
        output.putInt("Resonance", enchantments.resonance)
        output.putInt("Attunement", enchantments.attunement)
        output.putInt("Persistence", enchantments.persistence)
        attachedPos?.let {
            output.putInt("AttachX", it.x)
            output.putInt("AttachY", it.y)
            output.putInt("AttachZ", it.z)
        }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        isAttached = input.getBooleanOr("Attached", false)
        lifetimeTicks = input.getIntOr("Lifetime", Config.DEFAULT_LIFETIME_TICKS)
        enchantments = LensEnchantments(
            resonance = input.getIntOr("Resonance", 0),
            attunement = input.getIntOr("Attunement", 0),
            persistence = input.getIntOr("Persistence", 0),
        )
        val ax = input.getInt("AttachX")
        if (ax.isPresent) {
            attachedPos = BlockPos(
                ax.get(),
                input.getIntOr("AttachY", 0),
                input.getIntOr("AttachZ", 0),
            )
        }
    }

    // gravity lens is not pushable and ignores its own physics.
    override fun isPickable(): Boolean = isAttached
    override fun isPushable(): Boolean = false
    override fun isAttackable(): Boolean = false

    // Indestructible via damage — retrieval/expiry are the only ways it ends.
    override fun hurtServer(level: ServerLevel, source: DamageSource, damage: Float): Boolean = false

    private fun playLensSound(sound: net.minecraft.sounds.SoundEvent, volume: Float, pitch: Float) {
        level().playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch)
    }

    companion object {
        private val DATA_ATTACHED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(
                GravityLensEntity::class.java,
                EntityDataSerializers.BOOLEAN,
            )

        /**
         * Build a FLYING lens at the thrower's eye, launched along their look
         * vector, carrying the given enchantment snapshot.
         */
        fun createFlying(
            level: Level,
            player: Player,
            enchantments: LensEnchantments,
        ): GravityLensEntity {
            val lens = GravityLensEntity(ModEntities.GRAVITY_LENS, level)
            lens.enchantments = enchantments
            // Persistence extends the deployed lifetime (Requirement 9).
            lens.lifetimeTicks = Config.DEFAULT_LIFETIME_TICKS +
                enchantments.persistence * Config.PERSISTENCE_TICKS_PER_LEVEL

            val eye = player.getEyePosition(1.0f)
            lens.setPos(eye.x, eye.y, eye.z)

            val look = player.getViewVector(1.0f).normalize()
            lens.deltaMovement = look.scale(GravityLensItem.THROW_SPEED)
            return lens
        }
    }
}
