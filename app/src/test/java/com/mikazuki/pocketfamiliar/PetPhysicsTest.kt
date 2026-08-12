package com.mikazuki.pocketfamiliar

import com.mikazuki.pocketfamiliar.pet.physics.EdgeCollision
import com.mikazuki.pocketfamiliar.pet.physics.PetPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPhysicsTest {

    private fun engine() = PetPhysicsEngine(
        screenWidth = 1080,
        screenHeight = 1920,
        petWidth = 80,
        petHeight = 80
    )

    @Test
    fun `gravity accumulates velocity over time`() {
        val eng = engine().apply { y = 500f }
        val landed = eng.applyGravityStep(0.1f)
        assertTrue("Should not land immediately", !landed)
        assertTrue("Velocity should be positive", eng.velocityY > 0f)
        assertTrue("Y should have increased", eng.y > 500f)
    }

    @Test
    fun `pet lands at bottom boundary`() {
        val eng = engine().apply {
            y = 1839f      // very close to bottom (1920 - 80 = 1840)
            velocityY = 500f
        }
        val landed = eng.applyGravityStep(1f)
        assertTrue("Should have landed", landed)
        assertEquals(1840f, eng.y)
    }

    @Test
    fun `left edge collision detected`() {
        val eng = engine().apply { x = -10f }
        val collision = eng.clampToScreenBounds()
        assertEquals(EdgeCollision.LEFT, collision)
        assertEquals(0f, eng.x)
    }

    @Test
    fun `right edge collision detected`() {
        val eng = engine().apply { x = 1100f }  // > 1080 - 80 = 1000
        val collision = eng.clampToScreenBounds()
        assertEquals(EdgeCollision.RIGHT, collision)
        assertEquals(1000f, eng.x)
    }
}
