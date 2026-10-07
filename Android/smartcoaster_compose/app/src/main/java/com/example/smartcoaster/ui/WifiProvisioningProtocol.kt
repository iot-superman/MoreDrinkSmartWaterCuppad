package com.example.smartcoaster.ui

import java.nio.charset.StandardCharsets

internal object WifiProvisioningProtocol {
    const val MAX_PACKET_BYTES = 128
    private val attemptIdPattern = Regex("[0-9A-Fa-f]{12}")

    sealed interface Reply {
        data object Ignored : Reply
        data object Success : Reply
        data class Failure(val reason: String) : Reply
    }

    fun validate(ssid: String, password: String, openNetwork: Boolean): String? {
        val ssidBytes = ssid.toByteArray(StandardCharsets.UTF_8)
        val passwordBytes = password.toByteArray(StandardCharsets.UTF_8)
        return when {
            ssid.isBlank() -> "請輸入 Wi-Fi 名稱"
            ssidBytes.size > 32 -> "Wi-Fi 名稱不可超過 32 個 UTF-8 位元組"
            ':' in ssid || '\n' in ssid || '\r' in ssid -> "Wi-Fi 名稱不可包含冒號或換行"
            '\n' in password || '\r' in password -> "Wi-Fi 密碼不可包含換行"
            openNetwork && password.isNotEmpty() -> "開放網路請清除密碼"
            !openNetwork && passwordBytes.size !in 8..63 -> "Wi-Fi 密碼須為 8 至 63 個 UTF-8 位元組"
            else -> null
        }
    }

    fun command(attemptId: String, ssid: String, password: String): ByteArray {
        require(attemptIdPattern.matches(attemptId))
        return "WIFISET:$attemptId:$ssid:$password\n".toByteArray(StandardCharsets.UTF_8)
    }

    fun split(data: ByteArray, maxChunkBytes: Int): List<ByteArray> {
        require(maxChunkBytes > 0)
        return data.asList().chunked(maxChunkBytes).map { chunk -> chunk.toByteArray() }
    }

    fun canStart(isSending: Boolean, isWaitingForReply: Boolean): Boolean = !isSending && !isWaitingForReply

    fun parseReply(message: String, attemptId: String): Reply {
        if (!attemptIdPattern.matches(attemptId)) return Reply.Ignored
        val fields = message.trim().split(':')
        if (fields.size < 2 || fields[1] != attemptId) return Reply.Ignored
        return when (fields[0]) {
            "S" -> if (fields.size == 2) Reply.Success else Reply.Ignored
            "E" -> if (fields.size == 3) Reply.Failure(fields[2]) else Reply.Ignored
            else -> Reply.Ignored
        }
    }
}
