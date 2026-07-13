package com.example.mod.client.render

import com.example.mod.entity.GravityLensEntity
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState

/**
 * Renderer for the Gravity Lens entity.
 *
 * The field is invisible by default (Requirement 10); the visible center
 * "shimmer" is provided by the server-sent particle emitted from
 * [GravityLensEntity.emitParticles], which reaches both integrated and
 * dedicated-server clients. We therefore register a renderer with no custom
 * geometry — this keeps the entity valid on the client without pulling in the
 * item-in-world render pipeline (whose exact 26.2 signatures vary), while still
 * satisfying the "minimal / subtle" visual requirement.
 *
 * Swapping in a billboarded amethyst model later only requires overriding
 * [render] and drawing from the render state; the registration wiring stays
 * the same.
 */
class GravityLensRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<GravityLensEntity, EntityRenderState>(context) {

    override fun createRenderState(): EntityRenderState = EntityRenderState()
}
