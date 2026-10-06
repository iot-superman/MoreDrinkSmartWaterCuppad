package com.example.smartcoaster.ui

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
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

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleManager", "已成功連接至 GATT Server")
                    _connectionState.value = BleConnectionState.Connected
                    _connectedDeviceName.value = gatt?.device?.name ?: gatt?.device?.address
                    // 連線成功後開始搜尋裝置服務 (Services)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BleManager", "已從 GATT Server 斷開連線")
                    _connectionState.value = BleConnectionState.Disconnected
                    _connectedDeviceName.value = null
                    closeGatt()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "已成功搜尋到裝置服務 (${gatt?.services?.size} 個)")
            } else {
                Log.w("BleManager", "搜尋服務失敗，Status: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = String(value, Charsets.UTF_8)
                Log.d("BleManager", "收到讀取數據: $data")
            }
        }
    }

    // 呼叫此方法開始真實連線至 ESP32 / 藍牙裝置
    fun connect(macAddress: String) {
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

        // 關閉舊連線
        closeGatt()

        // 建立真實 GATT 連線 (autoConnect = false 確保立即連線)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
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
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    // 釋放資源
    fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}