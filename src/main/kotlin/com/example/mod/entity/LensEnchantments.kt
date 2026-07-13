package com.example.mod.entity

import com.example.mod.registry.ModEnchantments
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.Level

/**
 * Snapshot of the three custom enchantment levels taken from the thrown item
 * stack and carried onto the deployed lens (Requirement 9). Kept as a plain
 * data class so it is trivial to persist and to feed into the physics layer.
 */
data class LensEnchantments(
    val resonance: Int = 0,
    val attunement: Int = 0,
    val persistence: Int = 0,
) {
    companion object {
        val NONE = LensEnchantments()

        /**
         * Read the three custom enchantment levels off [stack]. In 26.x,
         * [EnchantmentHelper.getItemEnchantmentLevels] returns a
         * [net.minecraft.world.item.enchantment.ItemEnchantments] mapping
         * [net.minecraft.core.Holder]&lt;Enchantment&gt; -> level, so we resolve
         * each [ResourceKey] to its holder through the level's registry.
         */
        fun read(level: Level, stack: ItemStack): LensEnchantments {
            val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            val enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack)

            fun levelOf(key: ResourceKey<Enchantment>): Int {
                val enchantment = registry.getOptional(key).orElse(null) ?: return 0
                return enchants.getLevel(registry.wrapAsHolder(enchantment))
            }

            return LensEnchantments(
                resonance = levelOf(ModEnchantments.RESONANCE),
                attunement = levelOf(ModEnchantments.ATTUNEMENT),
                persistence = levelOf(ModEnchantments.PERSISTENCE),
            )
        }

        /**
         * Re-encode these enchantment levels back onto a retrieved lens
         * [stack] (Requirement 8 — the recovered item keeps its enchantments).
         * No-op for levels that are 0.
         */
        fun write(level: Level, stack: ItemStack, enchantments: LensEnchantments) {
            val registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)

            fun apply(key: ResourceKey<Enchantment>, lvl: Int) {
                if (lvl <= 0) return
                val enchantment = registry.getOptional(key).orElse(null) ?: return
                val holder = registry.wrapAsHolder(enchantment)
                EnchantmentHelper.updateEnchantments(stack) { mutable ->
                    mutable.set(holder, lvl)
                }
            }

            apply(ModEnchantments.RESONANCE, enchantments.resonance)
            apply(ModEnchantments.ATTUNEMENT, enchantments.attunement)
            apply(ModEnchantments.PERSISTENCE, enchantments.persistence)
        }
    }
}
