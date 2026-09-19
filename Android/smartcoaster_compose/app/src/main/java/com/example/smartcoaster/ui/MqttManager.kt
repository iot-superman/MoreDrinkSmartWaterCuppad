package com.example.smartcoaster.ui

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import android.util.Log

class MqttManager(
    private val onWeightReceived: (Float) -> Unit,
    private val onStatusChanged: (String) -> Unit,
    private val onLogReceived: (String) -> Unit,
    private val onModeConfirmed: (String) -> Unit,
    private val onStabilityChanged: (Boolean) -> Unit = {},
    // Tare 狀態直接由 ESP32 MQTT 訊息驅動 UI Popup。true=開始，false=完成/結束。
    private val onTareStateChanged: (Boolean) -> Unit = {}
) {
    private var client: MqttClient? = null

    private val serverUri = "tcp://mqttgo.io:1883"
    private val clientId = "SmartCoaster_App_${System.currentTimeMillis()}"

    fun connect() {
        try {
            if (client?.isConnected == true) return

            onStatusChanged("Connecting...")
            client = MqttClient(serverUri, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 20
                isAutomaticReconnect = true
            }

            client?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    onStatusChanged("Connected")
                    subscribe()
                }

                override fun connectionLost(cause: Throwable?) {
                    onStatusChanged("Disconnected")
                    onLogReceived("Connection Lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString()?.trim() ?: ""
                    Log.d("MQTT_RX", "[$topic] $payload")
                    onLogReceived("RX: $payload")
                    
                    // 1. 模式確認
                    if (payload.contains("MODE:")) {
                        val mode = payload.substringAfter("MODE:").trim()
                        onModeConfirmed(mode)
                    }

                    // 2. Tare 校準狀態偵測。
                    // ESP32 executeTare() 開始會送「執行去皮歸零 (Tare)...」，
                    // 完成會送固定格式「TARE_DONE:<重量>」。
                    // UI 不用猜時間：開始訊息出現 Popup，TARE_DONE 才真正完成。
                    if (payload.contains("執行去皮歸零") ||
                        payload.contains("正在進行開機精準去皮歸零") ||
                        payload.contains("深度去皮歸零")) {
                        onTareStateChanged(true)
                    }
                    if (payload.contains("TARE_DONE") || payload.contains("TARE_ERROR")) {
                        onTareStateChanged(false)
                    }

                    // 3. 穩定性偵測 (根據 log，出現 TARE_DONE 或是防線布署完成等字眼代表去皮成功且回穩)
                    if (payload.contains("TARE_DONE") || payload.contains("防線布署完成") || payload.contains("EMPTY_IDLE")) {
                        onStabilityChanged(true)
                    } else if (payload.contains("執行去皮歸零") || payload.contains("深度去皮歸零") || payload.contains("硬體錯誤")) {
                        // 正在進行耗時操作，視為不穩定
                        onStabilityChanged(false)
                    }
                    
                    // 4. 重量讀取
                    if (payload.contains("WEIGHT_ONCE:")) {
                        val weight = payload.substringAfter("WEIGHT_ONCE:").trim()
                            .takeWhile { (it.isDigit() || it == '.' || it == '-') }
                            .toFloatOrNull() ?: 0f
                        onWeightReceived(weight)
                    } else if (payload.contains("Weight =")) {
                        // 處理 "RAW = ... Weight = 0.15 g ..." 格式
                        val weightStr = payload.substringAfter("Weight =").trim()
                            .takeWhile { it.isDigit() || it == '.' || it == '-' }
                        weightStr.toFloatOrNull()?.let { weight ->
                            onWeightReceived(weight)
                        }
                    } else {
                        // 處理純數字格式
                        payload.toFloatOrNull()?.let { weight ->
                            onWeightReceived(weight)
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            client?.connect(options)
        } catch (e: Exception) {
            onStatusChanged("Error")
            onLogReceived("Error: ${e.message}")
        }
    }

    private fun subscribe() {
        try {
            client?.subscribe("esp32/msg", 1)
            client?.subscribe("esp32/weight", 1)
        } catch (e: Exception) { /* ... */ }
    }

    fun publish(cmd: String) {
        try {
            if (client?.isConnected == true) {
                client?.publish("esp32/cmd", MqttMessage(cmd.toByteArray()))
                onLogReceived("TX: $cmd")
            }
        } catch (e: Exception) { /* ... */ }
    }
}
