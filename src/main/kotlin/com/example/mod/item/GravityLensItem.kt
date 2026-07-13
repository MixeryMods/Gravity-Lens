package com.example.mod.item

import com.example.mod.Config
import com.example.mod.entity.GravityLensEntity
import com.example.mod.entity.LensEnchantments
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

/**
 * The Gravity Lens item. Right-clicking throws a [GravityLensEntity] in the
 * FLYING state along the player's look vector (Requirement 2). The lens's
 * enchantment levels are snapshotted at throw time and carried onto the entity
 * so the deployed field reflects the item that was thrown (Requirement 9).
 */
class GravityLensItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)

        if (!level.isClientSide) {
            val enchants = LensEnchantments.read(level, stack)
            val lens = GravityLensEntity.createFlying(level, player, enchants)
            level.addFreshEntity(lens)

            level.playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.ENDER_PEARL_THROW,
                SoundSource.PLAYERS,
                0.5f,
                0.4f / (level.random.nextFloat() * 0.4f + 0.8f),
            )
        }

        // Consume one lens unless in creative.
        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this))

        return InteractionResult.SUCCESS
    }

    companion object {
        /** Exposed for the entity to size the throw. */
        const val THROW_SPEED = Config.THROW_SPEED
    }
}
