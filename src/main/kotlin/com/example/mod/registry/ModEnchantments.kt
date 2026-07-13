package com.example.mod.registry

import com.example.mod.GravityLensMod
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.enchantment.Enchantment

/**
 * Enchantment [ResourceKey]s for the three custom Gravity Lens enchantments
 * (Requirement 9). In Minecraft 26.x enchantments are fully data-driven: the
 * definitions themselves live in JSON under
 * `data/gravitylens/enchantment/{resonance,attunement,persistence}.json`.
 *
 * We only need stable keys here to (a) read a deployed lens's stored levels and
 * (b) look levels up from an [net.minecraft.world.item.ItemStack] via
 * [net.minecraft.world.item.enchantment.EnchantmentHelper]. The runtime
 * *behavior* of each enchantment lives in the entity tick code, keyed off these
 * levels — the JSON is pure declaration (name, max level, supported items).
 */
object ModEnchantments {
    val RESONANCE: ResourceKey<Enchantment> = key("resonance")
    val ATTUNEMENT: ResourceKey<Enchantment> = key("attunement")
    val PERSISTENCE: ResourceKey<Enchantment> = key("persistence")

    private fun key(name: String): ResourceKey<Enchantment> =
        ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(GravityLensMod.MOD_ID, name),
        )
}
