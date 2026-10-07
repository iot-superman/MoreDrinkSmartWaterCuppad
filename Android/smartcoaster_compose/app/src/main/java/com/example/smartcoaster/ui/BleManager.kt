package com.example.smartcoaster.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom
import java.util.UUID

sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Connecting : BleConnectionState()
    data object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

sealed class WifiProvisioningState {
    data object Idle : WifiProvisioningState()
    data object Sending : WifiProvisioningState()
    data object Waiting : WifiProvisioningState()
    data class Success(val ssid: String) : WifiProvisioningState()
    data class Error(val message: String) : WifiProvisioningState()
}

@SuppressLint("MissingPermission")
class BleManager(context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var mtu = 23
    private var pendingWrites: List<ByteArray> = emptyList()
    private var nextWriteIndex = 0
    private var activeAttemptId: String? = null
    private var activeSsid: String? = null
    private var connectionTimeout: Runnable? = null
    private var provisioningTimeout: Runnable? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress

    private val _provisioningState = MutableStateFlow<WifiProvisioningState>(WifiProvisioningState.Idle)
    val provisioningState: StateFlow<WifiProvisioningState> = _provisioningState

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) {
                gatt.close()
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(gatt, "藍牙連線失敗（$status）")
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleManager", "GATT connected; discovering services")
                    try {
                        if (!gatt.requestMtu(185)) discoverServices(gatt)
                    } catch (_: SecurityException) {
                        failConnection(gatt, "缺少藍牙連線權限")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val hadActiveConnection = _connectionState.value == BleConnectionState.Connecting ||
                            _connectionState.value == BleConnectionState.Connected
                    failProvisioning("藍牙連線中斷，請重新設定")
                    closeGatt(gatt)
                    _connectionState.value = if (hadActiveConnection) {
                        BleConnectionState.Error("藍牙已中斷，請重新連線")
                    } else {
                        BleConnectionState.Disconnected
                    }
                    _connectedDeviceName.value = null
                    _connectedDeviceAddress.value = null
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (gatt !== bluetoothGatt) return
            if (status == BluetoothGatt.GATT_SUCCESS) this@BleManager.mtu = mtu.coerceAtLeast(23)
            discoverServices(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(gatt, "無法探索設備服務")
                return
            }
            val service = gatt.getService(SERVICE_UUID)
            val rx = service?.getCharacteristic(RX_UUID)
            val tx = service?.getCharacteristic(TX_UUID)
            if (rx == null || tx == null ||
                (rx.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0 ||
                (tx.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
            ) {
                failConnection(gatt, "此藍牙裝置不支援杯墊設定服務")
                return
            }
            val cccd = tx.getDescriptor(CCCD_UUID)
            if (cccd == null || !runCatching { gatt.setCharacteristicNotification(tx, true) }.getOrDefault(false)) {
                failConnection(gatt, "無法啟用設備通知")
                return
            }
            rxCharacteristic = rx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (runCatching {
                        gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    }.getOrDefault(BluetoothGatt.GATT_FAILURE) != BluetoothGatt.GATT_SUCCESS
                ) {
                    failConnection(gatt, "無法訂閱設備通知")
                }
            } else {
                val started = runCatching {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(cccd)
                }.getOrDefault(false)
                if (!started) failConnection(gatt, "無法訂閱設備通知")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (gatt !== bluetoothGatt || descriptor.uuid != CCCD_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failConnection(gatt, "無法訂閱設備通知")
                return
            }
            connectionTimeout?.let(handler::removeCallbacks)
            connectionTimeout = null
            _connectionState.value = BleConnectionState.Connected
            Log.d("BleManager", "Compatible service and TX notifications are ready")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (gatt !== bluetoothGatt || characteristic.uuid != RX_UUID || activeAttemptId == null) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                failProvisioning("傳送 Wi-Fi 設定失敗，請重試")
                return
            }
            nextWriteIndex++
            writeNextChunk(gatt)
        }

        @Deprecated("Kept for Android versions before API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                receiveNotification(gatt, characteristic, characteristic.value ?: return)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            receiveNotification(gatt, characteristic, value)
        }
    }

    fun connect(macAddress: String, deviceName: String? = null) {
        if (_connectionState.value == BleConnectionState.Connecting &&
            _connectedDeviceAddress.value == macAddress
        ) return
        if (_connectionState.value == BleConnectionState.Connected &&
            _connectedDeviceAddress.value == macAddress
        ) return
        val adapter = bluetoothAdapter
        val adapterEnabled = try {
            adapter?.isEnabled == true
        } catch (_: SecurityException) {
            _connectionState.value = BleConnectionState.Error("缺少藍牙連線權限")
            return
        }
        if (!adapterEnabled || adapter == null) {
            _connectionState.value = BleConnectionState.Error("藍牙未開啟")
            return
        }
        val device = try {
            adapter.getRemoteDevice(macAddress)
        } catch (_: IllegalArgumentException) {
            _connectionState.value = BleConnectionState.Error("無效的藍牙裝置位址")
            return
        } catch (_: SecurityException) {
            _connectionState.value = BleConnectionState.Error("缺少藍牙連線權限")
            return
        }

        cancelProvisioning()
        closeGatt()
        mtu = 23
        _connectedDeviceAddress.value = macAddress
        _connectedDeviceName.value = deviceName ?: device.name ?: macAddress
        _connectionState.value = BleConnectionState.Connecting
        try {
            bluetoothGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK,
                handler
            )
            if (bluetoothGatt == null) {
                _connectionState.value = BleConnectionState.Error("無法建立藍牙連線")
                return
            }
            val connectingGatt = bluetoothGatt ?: return
            connectionTimeout = Runnable {
                if (bluetoothGatt === connectingGatt && _connectionState.value == BleConnectionState.Connecting) {
                    failConnection(connectingGatt, "藍牙連線逾時，請重試")
                }
            }.also { handler.postDelayed(it, CONNECTION_TIMEOUT_MS) }
        } catch (_: SecurityException) {
            _connectionState.value = BleConnectionState.Error("缺少藍牙連線權限")
            closeGatt()
        } catch (_: IllegalArgumentException) {
            _connectionState.value = BleConnectionState.Error("無效的藍牙裝置位址")
            closeGatt()
        }
    }

    fun startProvisioning(ssid: String, password: String, openNetwork: Boolean): Boolean {
        if (_connectionState.value != BleConnectionState.Connected || bluetoothGatt == null || rxCharacteristic == null) {
            _provisioningState.value = WifiProvisioningState.Error("藍牙服務尚未就緒，請重新連線")
            return false
        }
        if (!WifiProvisioningProtocol.canStart(
                isSending = _provisioningState.value == WifiProvisioningState.Sending,
                isWaitingForReply = _provisioningState.value == WifiProvisioningState.Waiting
            )
        ) return false
        val validationError = WifiProvisioningProtocol.validate(ssid, password, openNetwork)
        if (validationError != null) {
            _provisioningState.value = WifiProvisioningState.Error(validationError)
            return false
        }

        val attemptId = newAttemptId()
        val packet = WifiProvisioningProtocol.command(attemptId, ssid, if (openNetwork) "" else password)
        if (packet.size > WifiProvisioningProtocol.MAX_PACKET_BYTES) {
            _provisioningState.value = WifiProvisioningState.Error("Wi-Fi 設定封包過長")
            return false
        }
        activeAttemptId = attemptId
        activeSsid = ssid
        pendingWrites = WifiProvisioningProtocol.split(packet, mtu - 3)
        nextWriteIndex = 0
        _provisioningState.value = WifiProvisioningState.Sending
        provisioningTimeout = Runnable {
            if (activeAttemptId == attemptId) failProvisioning("設備未能連上 Wi-Fi，請檢查名稱與密碼後重試")
        }.also { handler.postDelayed(it, PROVISIONING_TIMEOUT_MS) }
        writeNextChunk(bluetoothGatt)
        return true
    }

    fun cancelProvisioning() {
        provisioningTimeout?.let(handler::removeCallbacks)
        provisioningTimeout = null
        activeAttemptId = null
        activeSsid = null
        pendingWrites = emptyList()
        nextWriteIndex = 0
        _provisioningState.value = WifiProvisioningState.Idle
    }

    fun cancelConnectionIfConnecting() {
        if (_connectionState.value == BleConnectionState.Connecting) {
            closeGatt()
            _connectionState.value = BleConnectionState.Disconnected
            _connectedDeviceAddress.value = null
            _connectedDeviceName.value = null
        }
    }

    fun disconnect() {
        cancelProvisioning()
        bluetoothGatt?.disconnect()
    }

    fun closeGatt() {
        connectionTimeout?.let(handler::removeCallbacks)
        connectionTimeout = null
        provisioningTimeout?.let(handler::removeCallbacks)
        provisioningTimeout = null
        bluetoothGatt?.let { gatt ->
            bluetoothGatt = null
            gatt.close()
        }
        rxCharacteristic = null
    }

    private fun discoverServices(gatt: BluetoothGatt) {
        val started = runCatching { gatt.discoverServices() }.getOrDefault(false)
        if (gatt === bluetoothGatt && !started) {
            failConnection(gatt, "無法開始探索設備服務")
        }
    }

    private fun writeNextChunk(gatt: BluetoothGatt?) {
        if (gatt == null || gatt !== bluetoothGatt || activeAttemptId == null) return
        if (nextWriteIndex >= pendingWrites.size) {
            pendingWrites = emptyList()
            _provisioningState.value = WifiProvisioningState.Waiting
            return
        }
        val characteristic = rxCharacteristic ?: run {
            failProvisioning("設備服務已中斷，請重新連線")
            return
        }
        val chunk = pendingWrites[nextWriteIndex]
        val writeType = if (
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
        ) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, chunk, writeType)
            } else {
                characteristic.writeType = writeType
                characteristic.value = chunk
                if (gatt.writeCharacteristic(characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
            }
        } catch (_: SecurityException) {
            BluetoothGatt.GATT_FAILURE
        }
        if (result != BluetoothGatt.GATT_SUCCESS) failProvisioning("傳送 Wi-Fi 設定失敗，請重試")
    }

    private fun receiveNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (gatt !== bluetoothGatt || characteristic.uuid != TX_UUID) return
        val attemptId = activeAttemptId ?: return
        val reply = WifiProvisioningProtocol.parseReply(String(value, Charsets.UTF_8), attemptId)
        when (reply) {
            WifiProvisioningProtocol.Reply.Ignored -> Unit
            WifiProvisioningProtocol.Reply.Success -> {
                val ssid = activeSsid ?: return
                provisioningTimeout?.let(handler::removeCallbacks)
                provisioningTimeout = null
                activeAttemptId = null
                activeSsid = null
                pendingWrites = emptyList()
                _provisioningState.value = WifiProvisioningState.Success(ssid)
            }
            is WifiProvisioningProtocol.Reply.Failure -> failProvisioning(
                when (reply.reason) {
                    "NOAP" -> "找不到此 Wi-Fi 網路，請確認網路名稱"
                    "AUTH" -> "Wi-Fi 密碼錯誤，請重新輸入"
                    "TIME" -> "設備連線逾時，請確認路由器後重試"
                    "BAD" -> "設備拒絕此 Wi-Fi 設定"
                    else -> "設備無法連上 Wi-Fi，請重試"
                }
            )
        }
    }

    private fun failProvisioning(message: String) {
        if (activeAttemptId == null) return
        provisioningTimeout?.let(handler::removeCallbacks)
        provisioningTimeout = null
        activeAttemptId = null
        activeSsid = null
        pendingWrites = emptyList()
        nextWriteIndex = 0
        _provisioningState.value = WifiProvisioningState.Error(message)
    }

    private fun failConnection(gatt: BluetoothGatt, message: String) {
        if (gatt !== bluetoothGatt) return
        connectionTimeout?.let(handler::removeCallbacks)
        connectionTimeout = null
        failProvisioning(message)
        closeGatt(gatt)
        _connectedDeviceName.value = null
        _connectedDeviceAddress.value = null
        _connectionState.value = BleConnectionState.Error(message)
    }

    private fun closeGatt(gatt: BluetoothGatt) {
        if (gatt === bluetoothGatt) {
            connectionTimeout?.let(handler::removeCallbacks)
            connectionTimeout = null
            bluetoothGatt = null
            rxCharacteristic = null
            gatt.close()
        }
    }

    private fun newAttemptId(): String {
        val bytes = ByteArray(6)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val RX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private val TX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val CONNECTION_TIMEOUT_MS = 20_000L
        private const val PROVISIONING_TIMEOUT_MS = 45_000L
    }
}
