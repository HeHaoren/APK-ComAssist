package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * StopBitsValues 常量测试
 */
class StopBitsValuesTest {

    @Test
    fun `stop bits values are correct`() {
        assertEquals(1.0f, StopBitsValues.ONE, 0.001f)
        assertEquals(1.5f, StopBitsValues.ONE_AND_HALF, 0.001f)
        assertEquals(2.0f, StopBitsValues.TWO, 0.001f)
    }

    @Test
    fun `stop bits list contains all values`() {
        assertEquals(3, StopBitsValues.ALL.size)
        assertTrue(StopBitsValues.ALL.contains(1.0f))
        assertTrue(StopBitsValues.ALL.contains(1.5f))
        assertTrue(StopBitsValues.ALL.contains(2.0f))
    }

    @Test
    fun `labelOf returns correct labels`() {
        assertEquals("1", StopBitsValues.labelOf(1.0f))
        assertEquals("1.5", StopBitsValues.labelOf(1.5f))
        assertEquals("2", StopBitsValues.labelOf(2.0f))
        assertEquals("?", StopBitsValues.labelOf(0.5f))
    }
}
