package com.example.smartcoaster.ui

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    object Connecting : BleConnectionState()
    object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

sealed class WifiProvisioningState {
    object Idle : WifiProvisioningState()
    object Sending : WifiProvisioningState()
    data class Success(val ssid: String, val ip: String?) : WifiProvisioningState()
    data class Error(val message: String) : WifiProvisioningState()
}

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null
    private var provisioning: LegacyWifiProvisioning? = null
    private var mtu = 23
    private val pendingChunks = ArrayDeque<ByteArray>()
    private var allChunksWritten = false
    private var lastWriteTime = 0L
    private val _provisioningState = MutableStateFlow<WifiProvisioningState>(WifiProvisioningState.Idle)
    val provisioningState: StateFlow<WifiProvisioningState> = _provisioningState
    private val serviceUuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val rxUuid = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val txUuid = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            synchronized(this@BleManager) {
                if (gatt == null || gatt !== bluetoothGatt) return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failConnection("BLE 連線失敗 ($status)")
                    return
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d("BleManager", "已成功連接至 GATT Server")
                        _connectedDeviceName.value = gatt.device.name ?: gatt.device.address
                        // dev sends complete, unframed UTF-8 log notifications.
                        if (!gatt.requestMtu(185) && !gatt.discoverServices()) {
                            failConnection("無法搜尋設備服務")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d("BleManager", "已從 GATT Server 斷開連線")
                        if (provisioning != null) {
                            _provisioningState.value = WifiProvisioningState.Error("BLE 連線已中斷，請重新連線")
                        }
                        _connectionState.value = BleConnectionState.Disconnected
                        closeGatt()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            synchronized(this@BleManager) {
                if (gatt !== bluetoothGatt || _connectionState.value != BleConnectionState.Connecting) return
                this@BleManager.mtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu.coerceAtLeast(23) else 23
                if (!gatt.discoverServices()) {
                    failConnection("無法搜尋設備服務")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            synchronized(this@BleManager) {
                if (gatt == null || gatt !== bluetoothGatt ||
                    _connectionState.value != BleConnectionState.Connecting) return
                val service = gatt.getService(serviceUuid)
                val rx = service?.getCharacteristic(rxUuid)
                val tx = service?.getCharacteristic(txUuid)
                val cccd = tx?.getDescriptor(cccdUuid)
                if (status != BluetoothGatt.GATT_SUCCESS || rx == null ||
                    rx.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0 ||
                    tx == null || tx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0 ||
                    cccd == null) {
                    failConnection("設備不支援所需 BLE 服務或通知")
                    return
                }
                if (!gatt.setCharacteristicNotification(tx, true)) {
                    failConnection("無法啟用設備通知")
                    return
                }
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (!gatt.writeDescriptor(cccd)) failConnection("無法寫入通知訂閱設定")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            synchronized(this@BleManager) {
                if (gatt !== bluetoothGatt || descriptor.uuid != cccdUuid ||
                    _connectionState.value != BleConnectionState.Connecting) return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    failConnection("通知訂閱失敗 ($status)")
                } else {
                    clearTimeout()
                    _connectionState.value = BleConnectionState.Connected
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            synchronized(this@BleManager) {
                if (gatt !== bluetoothGatt || characteristic.uuid != rxUuid) return
                if (provisioning != null && status != BluetoothGatt.GATT_SUCCESS) {
                    failProvisioning("Wi-Fi 設定傳送失敗 ($status)，請重新連線")
                } else if (provisioning != null) {
                    if (pendingChunks.isEmpty()) {
                        allChunksWritten = true
                    } else if (SystemClock.elapsedRealtime() - lastWriteTime >= 400) {
                        // dev processes the receive buffer after 500 ms of silence.
                        failProvisioning("BLE 傳送過慢，請重新連線")
                    } else {
                        writeNextChunk(gatt, characteristic)
                    }
                }
            }
        }

        @Deprecated("Used on Android versions below API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            receiveNotification(gatt, characteristic, characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            receiveNotification(gatt, characteristic, value)
        }
    }

    @Synchronized
    private fun receiveNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (gatt !== bluetoothGatt || characteristic.uuid != txUuid) return
        if (!allChunksWritten) return
        val attempt = provisioning ?: return
        val result = attempt.receive(value) ?: return
        clearTimeout()
        provisioning = null
        _provisioningState.value = WifiProvisioningState.Success(attempt.ssid, result.ip)
    }

    // 呼叫此方法開始真實連線至 ESP32 / 藍牙裝置
    @Synchronized
    fun connect(macAddress: String) {
        closeGatt()
        mtu = 23
        _provisioningState.value = WifiProvisioningState.Idle
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = BleConnectionState.Error("藍牙未開啟")
            return
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(macAddress)
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.Error("無效的 MAC 位址: $macAddress")
            return
        }

        _connectionState.value = BleConnectionState.Connecting
        Log.d("BleManager", "正在嘗試連線至: ${device.name ?: macAddress}")

        // 建立真實 GATT 連線 (autoConnect = false 確保立即連線)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        setTimeout(20_000) { failConnection("BLE 連線或通知訂閱逾時") }
    }

    @Synchronized
    fun provisionWifi(ssid: String, password: String) {
        if (provisioning != null) return
        val gatt = bluetoothGatt
        val rx = gatt?.getService(serviceUuid)?.getCharacteristic(rxUuid)
        if (_connectionState.value != BleConnectionState.Connected || gatt == null || rx == null) {
            _provisioningState.value = WifiProvisioningState.Error("請先連接設備")
            return
        }
        val request = try {
            LegacyWifiProvisioning(ssid, password, mtu - 3)
        } catch (e: IllegalArgumentException) {
            _provisioningState.value = WifiProvisioningState.Error(e.message ?: "Wi-Fi 設定無效")
            return
        }
        provisioning = request
        allChunksWritten = false
        pendingChunks.addAll(request.payload.asList().chunked(mtu - 3).map { it.toByteArray() })
        _provisioningState.value = WifiProvisioningState.Sending
        // dev has no BLE failure reply or attempt IDs. Never equate a write ACK
        // with Wi-Fi success; disconnect on timeout so late replies cannot satisfy a retry.
        setTimeout(45_000) { failProvisioning("未收到 Wi-Fi 成功回覆，請檢查帳密及網路後重新連線") }
        writeNextChunk(gatt, rx)
    }

    private fun writeNextChunk(gatt: BluetoothGatt, rx: BluetoothGattCharacteristic) {
        rx.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        rx.value = pendingChunks.removeFirst()
        lastWriteTime = SystemClock.elapsedRealtime()
        if (!gatt.writeCharacteristic(rx)) failProvisioning("無法傳送 Wi-Fi 設定，請重新連線")
    }

    @Synchronized
    fun cancelProvisioning() {
        if (provisioning != null) failProvisioning("設定已取消，請重新連線")
    }

    private fun clearTimeout() {
        timeout?.let { handler.removeCallbacks(it) }
        timeout = null
    }

    private fun setTimeout(delayMillis: Long, action: () -> Unit) {
        clearTimeout()
        val task = object : Runnable {
            override fun run() {
                synchronized(this@BleManager) {
                    if (timeout !== this) return
                    timeout = null
                    action()
                }
            }
        }
        timeout = task
        handler.postDelayed(task, delayMillis)
    }

    private fun failConnection(message: String) {
        if (provisioning != null) _provisioningState.value = WifiProvisioningState.Error(message)
        closeGatt()
        _connectionState.value = BleConnectionState.Error(message)
    }

    private fun failProvisioning(message: String) {
        _provisioningState.value = WifiProvisioningState.Error(message)
        closeGatt()
        _connectionState.value = BleConnectionState.Disconnected
    }

    // 發送資料給 ESP32 (如 Wi-Fi 帳密或設定指令)
    fun writeData(serviceUuid: UUID, characteristicUuid: UUID, data: String) {
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUuid)
        if (characteristic != null) {
            characteristic.value = data.toByteArray(Charsets.UTF_8)
            bluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    // 斷開連線
    @Synchronized
    fun disconnect() {
        cancelProvisioning()
        closeGatt()
        _connectionState.value = BleConnectionState.Disconnected
    }

    // 釋放資源
    @Synchronized
    fun closeGatt() {
        clearTimeout()
        provisioning = null
        pendingChunks.clear()
        allChunksWritten = false
        _connectedDeviceName.value = null
        val gatt = bluetoothGatt
        bluetoothGatt = null
        gatt?.disconnect()
        gatt?.close()
    }
}