package com.example.smartcoaster.ui

// Temporary compatibility with dev's handleCommand()/appPrint(); remove when
// firmware supports framed WIFISET requests and attempt-correlated replies.
internal class LegacyWifiProvisioning(
    val ssid: String,
    password: String,
    private val notificationLimit: Int
) {
    data class Result(val ip: String?)
    val payload: ByteArray
    private var credentialsSaved = false

    init {
        require(ssid.isNotBlank() && ssid.toByteArray(Charsets.UTF_8).size <= 32) {
            "SSID 必須為 1–32 bytes"
        }
        require(':' !in ssid && ssid == ssid.trim() && password == password.trim() &&
            (ssid + password).none { it == '\r' || it == '\n' || it == '\u0000' }) {
            "舊版韌體不支援 SSID 冒號、換行或帳密前後空白"
        }
        // Empty passwords are passed directly to WiFi.begin() by dev.
        payload = "$ssid:$password".toByteArray(Charsets.UTF_8)
        require(payload.size <= 64) { "舊版韌體的 Wi-Fi 帳密合計不可超過 64 bytes" }
    }

    fun receive(value: ByteArray): Result? {
        val receipt = "💾 [NVS 儲存成功] 新 WiFi 寫入！SSID: $ssid，準備重新嘗試連線..."
            .toByteArray(Charsets.UTF_8)
        // dev sends one unframed notification per log; at MTU 23 it may be
        // truncated mid-codepoint. Compare bytes, not replacement UTF-8 characters.
        if (value.contentEquals(receipt) ||
            value.contentEquals(receipt.copyOf(minOf(receipt.size, notificationLimit)))) {
            credentialsSaved = true
            return null
        }
        if (!credentialsSaved) return null
        val successPrefix = "🎉 [WiFi 連線成功] IP: ".toByteArray(Charsets.UTF_8)
        if (value.size == notificationLimit &&
            value.take(minOf(successPrefix.size, notificationLimit)) ==
            successPrefix.take(minOf(successPrefix.size, notificationLimit))) {
            return Result(null)
        }
        val notification = String(value, Charsets.UTF_8)
        val ip = Regex("🎉 \\[WiFi 連線成功] IP: (\\d{1,3}(?:\\.\\d{1,3}){3})")
            .matchEntire(notification)?.groupValues?.get(1) ?: return null
        return if (ip.split('.').all { it.toInt() in 0..255 }) Result(ip) else null
    }
}
