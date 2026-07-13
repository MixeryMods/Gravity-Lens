package com.example.mod.physics

import com.example.mod.Config
import net.minecraft.world.phys.Vec3

/**
 * Pure gravitational-field math for the Gravity Lens.
 *
 * Nothing in here touches Minecraft entities, worlds, or ticking — it operates
 * only on [Vec3] positions and scalars. That keeps the core physics (radius,
 * distance falloff, mass scaling, additive stacking) independent and easy to
 * reason about; the entity layer merely feeds positions in and adds the
 * resulting vector onto an entity's delta movement.
 *
 * Force model (Requirement 4):
 *   dir       = normalize(center - entity)          // points toward the lens
 *   falloff   = 1 - clamp(dist / radius, 0, 1)      // 1 at center, 0 at rim
 *   magnitude = BASE_FORCE * falloff * signalScale * resonanceForce / mass
 *   result    = dir * magnitude                     // ADDED to current velocity
 */
object GravityField {

    /**
     * Effective sphere radius given the base radius and Resonance level,
     * clamped to [Config.MAX_RADIUS] (Requirement 3 & 9-Resonance).
     */
    fun effectiveRadius(resonanceLevel: Int): Double {
        val raw = Config.DEFAULT_RADIUS + resonanceLevel * Config.RESONANCE_RADIUS_PER_LEVEL
        return raw.coerceIn(Config.DEFAULT_RADIUS, Config.MAX_RADIUS)
    }

    /**
     * Redstone signal (0..15) mapped to a linear force multiplier (Requirement 7).
     * Signal 0 returns 0.0 — callers use that to skip all physics entirely.
     */
    fun signalScale(signal: Int): Double {
        val s = signal.coerceIn(0, Config.MAX_SIGNAL)
        return s.toDouble() / Config.MAX_SIGNAL
    }

    /** Extra multiplier from Resonance on force strength (never below 1.0). */
    fun resonanceForceMultiplier(resonanceLevel: Int): Double =
        1.0 + resonanceLevel.coerceAtLeast(0) * Config.RESONANCE_FORCE_PER_LEVEL

    /**
     * Distance-based falloff: 1.0 at the center, decreasing linearly to 0.0 at
     * the rim. Returns 0.0 for anything at or beyond [radius].
     */
    fun falloff(distance: Double, radius: Double): Double {
        if (radius <= 0.0) return 0.0
        val ratio = (distance / radius).coerceIn(0.0, 1.0)
        return 1.0 - ratio
    }

    /**
     * True when [entityPos] lies within the sphere of [radius] around [center].
     * Uses squared distance to avoid a sqrt in the hot path.
     */
    fun withinSphere(center: Vec3, entityPos: Vec3, radius: Double): Boolean =
        center.distanceToSqr(entityPos) <= radius * radius

    /**
     * Compute the additive velocity contribution of a single lens on a single
     * entity. Returns [Vec3.ZERO] when the entity is at/beyond the rim, exactly
     * at the center (no defined direction), or the signal disables the field.
     *
     * @param mass a mass proxy (e.g. bounding-box volume); use 1.0 for
     *        projectiles and dropped items so they feel the full force.
     */
    fun computeForce(
        center: Vec3,
        entityPos: Vec3,
        radius: Double,
        signal: Int,
        resonanceLevel: Int,
        mass: Double,
    ): Vec3 {
        val signalScale = signalScale(signal)
        if (signalScale <= 0.0) return Vec3.ZERO

        val delta = center.subtract(entityPos)
        val distance = delta.length()
        // No meaningful direction at the exact center — avoid NaN from normalize.
        if (distance <= 1.0e-4) return Vec3.ZERO

        val f = falloff(distance, radius)
        if (f <= 0.0) return Vec3.ZERO

        val safeMass = mass.coerceAtLeast(Config.MIN_MASS_VOLUME)
        val magnitude =
            Config.BASE_FORCE * f * signalScale * resonanceForceMultiplier(resonanceLevel) / safeMass

        // delta / distance == normalized direction toward the center.
        return delta.scale(magnitude / distance)
    }
}
