package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * DataBitsValues 常量测试
 */
class DataBitsValuesTest {

    @Test
    fun `data bits values are correct`() {
        assertEquals(5, DataBitsValues.FIVE)
        assertEquals(6, DataBitsValues.SIX)
        assertEquals(7, DataBitsValues.SEVEN)
        assertEquals(8, DataBitsValues.EIGHT)
    }

    @Test
    fun `data bits list contains all values`() {
        assertEquals(4, DataBitsValues.ALL.size)
        assertTrue(DataBitsValues.ALL.contains(5))
        assertTrue(DataBitsValues.ALL.contains(6))
        assertTrue(DataBitsValues.ALL.contains(7))
        assertTrue(DataBitsValues.ALL.contains(8))
    }
}
