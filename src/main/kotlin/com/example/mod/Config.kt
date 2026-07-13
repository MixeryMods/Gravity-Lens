package com.example.mod

/**
 * Central tuning constants for the Gravity Lens.
 *
 * All values are expressed in Minecraft world units (blocks) and ticks
 * (20 ticks == 1 second). Physics is applied once per server tick.
 */
object Config {
    // --- Field geometry (Requirement 3) ---
    /** Default sphere radius applied at Resonance level 0. */
    const val DEFAULT_RADIUS: Double = 8.0

    /** Hard ceiling on the sphere radius regardless of enchantments. */
    const val MAX_RADIUS: Double = 16.0

    /** Additional radius (blocks) granted per level of Resonance. */
    const val RESONANCE_RADIUS_PER_LEVEL: Double = 2.0

    // --- Force (Requirements 4, 6, 7) ---
    /**
     * Peak additive velocity (blocks/tick) applied to a unit-mass entity at
     * the exact center of the field, at full redstone signal (15).
     * The value is deliberately small: it is *added every tick*, so it
     * accumulates into a smooth, continuous curve rather than a snap.
     */
    const val BASE_FORCE: Double = 0.08

    /** Extra force multiplier per level of Resonance (adds to the 1.0 base). */
    const val RESONANCE_FORCE_PER_LEVEL: Double = 0.15

    /**
     * A living entity's force is divided by (boundingBoxVolume) to simulate
     * mass. This floor keeps very small mobs from being flung uncontrollably
     * and avoids division by absurdly small volumes.
     */
    const val MIN_MASS_VOLUME: Double = 0.25

    // --- Redstone (Requirement 7) ---
    const val MAX_SIGNAL: Int = 15

    // --- Lifetime & retrieval (Requirement 8) ---
    /** Default deployed lifetime: 30 seconds * 20 ticks. */
    const val DEFAULT_LIFETIME_TICKS: Int = 30 * 20

    /** Extra lifetime ticks granted per level of Persistence. */
    const val PERSISTENCE_TICKS_PER_LEVEL: Int = 15 * 20

    // --- Deployment (Requirement 2) ---
    /** Initial speed (blocks/tick) of a thrown lens. */
    const val THROW_SPEED: Double = 1.5

    /** Per-tick gravity applied to a flying (un-attached) lens. */
    const val FLYING_GRAVITY: Double = 0.03

    /** Per-tick horizontal+vertical drag multiplier on a flying lens. */
    const val FLYING_DRAG: Double = 0.99

    // --- Visuals (Requirement 10) ---
    /** Emit a center particle every N ticks while attached and active. */
    const val PARTICLE_INTERVAL_TICKS: Int = 4
}
