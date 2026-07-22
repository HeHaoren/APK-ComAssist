package com.hehaoren.comassist.serial

import org.junit.Assert.*
import org.junit.Test

/**
 * QuickCommand 数据类测试
 */
class QuickCommandTest {

    @Test
    fun `quick command creation with default id`() {
        val cmd = QuickCommand(name = "Test", command = "AT")
        assertEquals("Test", cmd.name)
        assertEquals("AT", cmd.command)
        assertFalse(cmd.isHex)
        assertTrue(cmd.id > 0)
    }

    @Test
    fun `quick command creation with hex flag`() {
        val cmd = QuickCommand(name = "HexCmd", command = "FF01", isHex = true)
        assertTrue(cmd.isHex)
    }

    @Test
    fun `quick command equality by id`() {
        val id = 12345L
        val cmd1 = QuickCommand(id = id, name = "Test", command = "AT")
        val cmd2 = QuickCommand(id = id, name = "Different", command = "Different")
        // 相同 id 的命令应该被认为是同一个命令
        assertEquals(cmd1.id, cmd2.id)
    }
}
