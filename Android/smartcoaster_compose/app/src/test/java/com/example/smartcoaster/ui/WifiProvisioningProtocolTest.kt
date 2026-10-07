package com.example.smartcoaster.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class WifiProvisioningProtocolTest {
    private val attemptId = "0123456789ab"

    @Test
    fun validatesUtf8ByteLengthsAndLegacyDelimiterLimits() {
        assertNull(WifiProvisioningProtocol.validate("水".repeat(10), "12345678", false))
        assertEquals(
            "Wi-Fi 名稱不可超過 32 個 UTF-8 位元組",
            WifiProvisioningProtocol.validate("水".repeat(11), "12345678", false)
        )
        assertEquals(
            "Wi-Fi 名稱不可包含冒號或換行",
            WifiProvisioningProtocol.validate("office:5G", "12345678", false)
        )
    }

    @Test
    fun supportsOnlyExplicitOpenNetworksOrValidPasswords() {
        assertNull(WifiProvisioningProtocol.validate("open", "", true))
        assertEquals("開放網路請清除密碼", WifiProvisioningProtocol.validate("open", "12345678", true))
        assertEquals("Wi-Fi 密碼須為 8 至 63 個 UTF-8 位元組", WifiProvisioningProtocol.validate("home", "", false))
        assertNull(WifiProvisioningProtocol.validate("home", "12345678", false))
        assertEquals(
            "Wi-Fi 密碼須為 8 至 63 個 UTF-8 位元組",
            WifiProvisioningProtocol.validate("home", "密".repeat(22), false)
        )
    }

    @Test
    fun framesUtf8CredentialsAndSplitsAtAttPayloadBoundary() {
        val command = WifiProvisioningProtocol.command(attemptId, "home", "12345678")
        assertTrue(String(command, StandardCharsets.UTF_8).endsWith("\n"))
        val chunks = WifiProvisioningProtocol.split(command, 20)
        assertTrue(chunks.all { it.size <= 20 })
        assertEquals(command.toList(), chunks.flatMap { it.asList() })
    }

    @Test
    fun acceptsOnlyTheCurrentAttemptReply() {
        assertEquals(WifiProvisioningProtocol.Reply.Success, WifiProvisioningProtocol.parseReply("S:$attemptId", attemptId))
        assertEquals(
            WifiProvisioningProtocol.Reply.Failure("NOAP"),
            WifiProvisioningProtocol.parseReply("E:$attemptId:NOAP", attemptId)
        )
        assertEquals(
            WifiProvisioningProtocol.Reply.Ignored,
            WifiProvisioningProtocol.parseReply("S:ffffffffffff", attemptId)
        )
        assertEquals(
            WifiProvisioningProtocol.Reply.Ignored,
            WifiProvisioningProtocol.parseReply("🎉 [WiFi 連線成功] IP: 192.0.2.1", attemptId)
        )
        assertFalse(WifiProvisioningProtocol.parseReply("E:$attemptId:NOAP:extra", attemptId) is WifiProvisioningProtocol.Reply.Failure)
    }

    @Test
    fun preventsDuplicateSubmissionsUntilDeviceReplies() {
        assertFalse(WifiProvisioningProtocol.canStart(isSending = true, isWaitingForReply = false))
        assertFalse(WifiProvisioningProtocol.canStart(isSending = false, isWaitingForReply = true))
        assertTrue(WifiProvisioningProtocol.canStart(isSending = false, isWaitingForReply = false))
    }
}
