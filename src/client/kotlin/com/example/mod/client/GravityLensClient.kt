package com.example.mod.client

import com.example.mod.client.render.GravityLensRenderer
import com.example.mod.registry.ModEntities
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry

/**
 * Gravity Lens — client entrypoint.
 *
 * Registers the renderer for the deployed/flying lens entity so it is visible
 * on both integrated and dedicated-server clients. The visible center-of-field
 * effect is driven by server-sent particles (see [com.example.mod.entity.GravityLensEntity]),
 * so nothing client-only is required for the field shimmer itself.
 */
object GravityLensClient : ClientModInitializer {
    // EntityRendererRegistry carries a deprecation annotation in the 26.2
    // snapshot API, but the vanilla replacement (EntityRenderers.register) is
    // private — this Fabric entry point remains the supported way to register
    // an entity renderer. Suppress the expected warning.
    @Suppress("DEPRECATION")
    override fun onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GRAVITY_LENS, ::GravityLensRenderer)
    }
}
