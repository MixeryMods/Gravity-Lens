package com.example.mod.registry

import com.example.mod.GravityLensMod
import com.example.mod.item.GravityLensItem
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item

/**
 * Item registration for the Gravity Lens (Requirement 1).
 *
 * Uses the 26.x Mojang-mapped pattern: build a [ResourceKey], stamp it onto the
 * [Item.Properties] via `setId`, then register into [BuiltInRegistries.ITEM].
 */
object ModItems {

    val GRAVITY_LENS: Item = register(
        "gravity_lens",
        ::GravityLensItem,
        // Stackable but modest; each lens is a single deployment.
        Item.Properties().stacksTo(16),
    )

    private fun <T : Item> register(
        name: String,
        factory: (Item.Properties) -> T,
        properties: Item.Properties,
    ): T {
        val itemKey: ResourceKey<Item> = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(GravityLensMod.MOD_ID, name),
        )
        val item = factory(properties.setId(itemKey))
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item)
    }

    /** Forces static initialization of this object from the mod initializer. */
    fun initialize() {}
}
