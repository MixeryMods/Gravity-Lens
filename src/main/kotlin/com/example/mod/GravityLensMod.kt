package com.example.mod

import com.example.mod.entity.GravityLensEntity
import com.example.mod.registry.ModEntities
import com.example.mod.registry.ModItems
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory

/**
 * Gravity Lens — main (common) entrypoint.
 *
 * Registers the item, entity type and creative-tab entry, and wires the
 * empty-handed recall interaction (Requirement 8). All per-tick field physics
 * lives on [GravityLensEntity] itself, so no explicit server-tick hook is
 * needed here — the entity ticks as part of the standard server loop.
 */
object GravityLensMod : ModInitializer {
    const val MOD_ID = "gravitylens"
    val logger = LoggerFactory.getLogger(MOD_ID)!!

    /** Vanilla "Tools & Utilities" creative tab key (constants are private in 26.2). */
    private val TOOLS_AND_UTILITIES: ResourceKey<CreativeModeTab> =
        ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("tools_and_utilities"),
        )

    override fun onInitialize() {
        ModItems.initialize()
        ModEntities.initialize()

        // Creative tab entry (Requirement 1).
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES)
            .register(CreativeModeTabEvents.ModifyOutput { output ->
                output.accept(ModItems.GRAVITY_LENS)
            })

        // Empty-handed recall (Requirement 8): using a deployed lens with an
        // empty hand recalls it. Interacting with any item still routes through
        // GravityLensEntity.interact, which also recalls.
        UseEntityCallback.EVENT.register(UseEntityCallback { player, level, hand, entity, _ ->
            if (entity is GravityLensEntity && player.getItemInHand(hand).isEmpty) {
                if (!level.isClientSide) entity.retrieveTo(player)
                InteractionResult.SUCCESS
            } else {
                InteractionResult.PASS
            }
        })

        logger.info("$MOD_ID initialized")
    }
}
