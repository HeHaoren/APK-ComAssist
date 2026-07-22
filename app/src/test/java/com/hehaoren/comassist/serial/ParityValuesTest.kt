package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * ParityValues 常量测试
 */
class ParityValuesTest {

    @Test
    fun `parity values are correct`() {
        assertEquals(0, ParityValues.NONE)
        assertEquals(1, ParityValues.ODD)
        assertEquals(2, ParityValues.EVEN)
    }

    @Test
    fun `parity list contains all values`() {
        assertEquals(3, ParityValues.ALL.size)
        assertTrue(ParityValues.ALL.contains(0))
        assertTrue(ParityValues.ALL.contains(1))
        assertTrue(ParityValues.ALL.contains(2))
    }

    @Test
    fun `labelOf returns correct labels`() {
        assertEquals("None", ParityValues.labelOf(ParityValues.NONE))
        assertEquals("Odd", ParityValues.labelOf(ParityValues.ODD))
        assertEquals("Even", ParityValues.labelOf(ParityValues.EVEN))
        assertEquals("?", ParityValues.labelOf(-1))
    }
}
