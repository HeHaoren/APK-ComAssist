package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * DisplayModes 常量测试
 */
class DisplayModesTest {

    @Test
    fun `display modes have correct values`() {
        assertEquals(0, DisplayModes.HEX)
        assertEquals(1, DisplayModes.ASCII)
    }

    @Test
    fun `display modes list contains all modes`() {
        assertEquals(2, DisplayModes.ALL.size)
        assertTrue(DisplayModes.ALL.contains(DisplayModes.HEX))
        assertTrue(DisplayModes.ALL.contains(DisplayModes.ASCII))
    }

    @Test
    fun `labelOf returns correct labels`() {
        assertEquals("HEX", DisplayModes.labelOf(DisplayModes.HEX))
        assertEquals("ASCII", DisplayModes.labelOf(DisplayModes.ASCII))
        assertEquals("?", DisplayModes.labelOf(-1))
    }
}
