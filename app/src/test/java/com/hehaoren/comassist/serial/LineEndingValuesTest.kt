package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * LineEndingValues 常量测试
 */
class LineEndingValuesTest {

    @Test
    fun `line ending values are correct`() {
        assertEquals(0, LineEndingValues.NONE)
        assertEquals(1, LineEndingValues.CR)
        assertEquals(2, LineEndingValues.LF)
        assertEquals(3, LineEndingValues.CRLF)
    }

    @Test
    fun `line ending list contains all values`() {
        assertEquals(4, LineEndingValues.ALL.size)
        assertTrue(LineEndingValues.ALL.contains(LineEndingValues.NONE))
        assertTrue(LineEndingValues.ALL.contains(LineEndingValues.CR))
        assertTrue(LineEndingValues.ALL.contains(LineEndingValues.LF))
        assertTrue(LineEndingValues.ALL.contains(LineEndingValues.CRLF))
    }

    @Test
    fun `labelOf returns correct labels`() {
        assertEquals("无", LineEndingValues.labelOf(LineEndingValues.NONE))
        assertEquals("CR", LineEndingValues.labelOf(LineEndingValues.CR))
        assertEquals("LF", LineEndingValues.labelOf(LineEndingValues.LF))
        assertEquals("CRLF", LineEndingValues.labelOf(LineEndingValues.CRLF))
        assertEquals("?", LineEndingValues.labelOf(-1))
    }

    @Test
    fun `bytesOf returns correct bytes`() {
        assertNull(LineEndingValues.bytesOf(LineEndingValues.NONE))

        val cr = LineEndingValues.bytesOf(LineEndingValues.CR)
        assertNotNull(cr)
        assertArrayEquals(byteArrayOf(0x0D), cr)

        val lf = LineEndingValues.bytesOf(LineEndingValues.LF)
        assertNotNull(lf)
        assertArrayEquals(byteArrayOf(0x0A), lf)

        val crlf = LineEndingValues.bytesOf(LineEndingValues.CRLF)
        assertNotNull(crlf)
        assertArrayEquals(byteArrayOf(0x0D, 0x0A), crlf)
    }
}
