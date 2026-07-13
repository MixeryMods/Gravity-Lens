package com.example.mod.registry

import com.example.mod.GravityLensMod
import com.example.mod.entity.GravityLensEntity
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

/**
 * Entity registration for the deployable/flying Gravity Lens entity
 * (Requirement 2). A single [EntityType] backs both the FLYING and ATTACHED
 * states of [GravityLensEntity].
 */
object ModEntities {

    val GRAVITY_LENS: EntityType<GravityLensEntity> = register("gravity_lens") { key ->
        EntityType.Builder
            .of(::GravityLensEntity, MobCategory.MISC)
            // Small, roughly amethyst-shard sized hit box.
            .sized(0.4f, 0.4f)
            // Keep it ticking/visible for clients near the field even if the
            // thrower moves away.
            .clientTrackingRange(8)
            .updateInterval(2)
            .build(key)
    }

    private fun <T : net.minecraft.world.entity.Entity> register(
        name: String,
        builder: (ResourceKey<EntityType<*>>) -> EntityType<T>,
    ): EntityType<T> {
        val key: ResourceKey<EntityType<*>> = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(GravityLensMod.MOD_ID, name),
        )
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder(key))
    }

    fun initialize() {}
}
