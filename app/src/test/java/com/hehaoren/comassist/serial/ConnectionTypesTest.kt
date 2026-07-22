package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * ConnectionTypes 常量测试
 */
class ConnectionTypesTest {

    @Test
    fun `connection types have correct values`() {
        assertEquals("USB", ConnectionTypes.USB)
        assertEquals("蓝牙", ConnectionTypes.BLUETOOTH)
    }
}
