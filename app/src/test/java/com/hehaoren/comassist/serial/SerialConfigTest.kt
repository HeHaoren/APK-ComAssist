package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * SerialConfig 数据类测试
 */
class SerialConfigTest {

    @Test
    fun `default config has correct values`() {
        val config = SerialConfig()
        assertEquals(115200, config.baudRate)
        assertEquals(DataBitsValues.EIGHT, config.dataBits)
        assertEquals(StopBitsValues.ONE, config.stopBits, 0.001f)
        assertEquals(ParityValues.NONE, config.parity)
        assertEquals(LineEndingValues.NONE, config.lineEnding)
    }

    @Test
    fun `config copy preserves unchanged fields`() {
        val config = SerialConfig()
        val modified = config.copy(baudRate = 9600)
        assertEquals(9600, modified.baudRate)
        assertEquals(config.dataBits, modified.dataBits)
        assertEquals(config.stopBits, modified.stopBits, 0.001f)
        assertEquals(config.parity, modified.parity)
        assertEquals(config.lineEnding, modified.lineEnding)
    }

    @Test
    fun `config equality works correctly`() {
        val config1 = SerialConfig(baudRate = 9600, dataBits = 8)
        val config2 = SerialConfig(baudRate = 9600, dataBits = 8)
        val config3 = SerialConfig(baudRate = 115200, dataBits = 8)
        assertEquals(config1, config2)
        assertNotEquals(config1, config3)
    }
}
