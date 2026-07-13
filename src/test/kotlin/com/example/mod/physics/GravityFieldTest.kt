package com.example.mod.physics

import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure gravity-field math. These exercise the physics that
 * back the project's success criteria without launching Minecraft:
 *  - continuous trajectory curvature scaling with signal (criterion 1)
 *  - additive stacking of two offset lenses (criterion 2)
 *  - radius scaling via Resonance and the signal-0 halt (criteria 3 & 4)
 */
class GravityFieldTest {

    private val center = Vec3(0.0, 0.0, 0.0)

    @Test
    fun `force points toward the lens center`() {
        val entity = Vec3(5.0, 0.0, 0.0)
        val force = GravityField.computeForce(center, entity, radius = 8.0, signal = 15, resonanceLevel = 0, mass = 1.0)
        // Entity is +X of center, so pull must be toward -X and only -X.
        assertTrue(force.x < 0.0, "expected pull toward center on -X, got $force")
        assertEquals(0.0, force.y, 1e-9)
        assertEquals(0.0, force.z, 1e-9)
    }

    @Test
    fun `force is stronger nearer the center`() {
        val near = GravityField.computeForce(center, Vec3(1.0, 0.0, 0.0), 8.0, 15, 0, 1.0).length()
        val far = GravityField.computeForce(center, Vec3(6.0, 0.0, 0.0), 8.0, 15, 0, 1.0).length()
        assertTrue(near > far, "near force ($near) should exceed far force ($far)")
    }

    @Test
    fun `signal scales force linearly and zero halts entirely`() {
        val entity = Vec3(4.0, 0.0, 0.0)
        val full = GravityField.computeForce(center, entity, 8.0, 15, 0, 1.0).length()
        val half = GravityField.computeForce(center, entity, 8.0, 8, 0, 1.0).length()
        val off = GravityField.computeForce(center, entity, 8.0, 0, 0, 1.0)

        assertTrue(half in (full * 0.4)..(full * 0.6), "signal 8/15 should be ~half of full")
        assertEquals(Vec3.ZERO, off, "signal 0 must produce no force")
    }

    @Test
    fun `entities beyond the radius receive no force`() {
        val outside = GravityField.computeForce(center, Vec3(9.0, 0.0, 0.0), radius = 8.0, signal = 15, resonanceLevel = 0, mass = 1.0)
        assertEquals(Vec3.ZERO, outside)
    }

    @Test
    fun `resonance increases effective radius but is clamped to the maximum`() {
        assertEquals(8.0, GravityField.effectiveRadius(0), 1e-9)
        assertEquals(10.0, GravityField.effectiveRadius(1), 1e-9)
        // Very high level clamps at MAX_RADIUS (16).
        assertEquals(16.0, GravityField.effectiveRadius(100), 1e-9)
    }

    @Test
    fun `heavier mass yields weaker force`() {
        val entity = Vec3(3.0, 0.0, 0.0)
        val light = GravityField.computeForce(center, entity, 8.0, 15, 0, mass = 1.0).length()
        val heavy = GravityField.computeForce(center, entity, 8.0, 15, 0, mass = 4.0).length()
        assertTrue(heavy < light, "heavier entity ($heavy) should be pulled less than lighter ($light)")
    }

    @Test
    fun `two offset lenses stack additively`() {
        // A projectile flying along +X between two lenses offset on +Z and -Z:
        // each lens contributes an independent additive vector; summed, the
        // net Z-force should cancel while a real single lens off to one side
        // would not — this is the additive-stacking guarantee (criterion 2).
        val entity = Vec3(0.0, 0.0, 0.0)
        val lensA = Vec3(2.0, 0.0, 3.0)
        val lensB = Vec3(2.0, 0.0, -3.0)

        val fa = GravityField.computeForce(lensA, entity, 8.0, 15, 0, 1.0)
        val fb = GravityField.computeForce(lensB, entity, 8.0, 15, 0, 1.0)
        val net = fa.add(fb)

        // Symmetric lenses: Z cancels, X reinforces (both pull toward +X).
        assertEquals(0.0, net.z, 1e-9)
        assertTrue(net.x > fa.x, "combined +X pull should exceed a single lens")
    }
}
