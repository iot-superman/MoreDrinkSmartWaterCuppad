package com.example.smartcoaster.ui

import android.Manifest
import android.app.Activity
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.example.smartcoaster.ui.theme.SmartCoasterTheme

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = 0
)

@SuppressLint("MissingPermission")
@Composable
fun Page3(
    connectionState: BleConnectionState = BleConnectionState.Disconnected,
    connectedDeviceAddress: String? = null,
    initialSelectedAddress: String? = null,
    onNavigateToNext: (device: BleDeviceItem) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }

    var discoveredDevices by remember { mutableStateOf<List<BleDeviceItem>>(emptyList()) }
    var selectedDevice by remember(initialSelectedAddress) {
        mutableStateOf(discoveredDevices.firstOrNull { it.address == initialSelectedAddress })
    }
    var hasPermission by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var scanAttempt by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var stopActiveScan by remember { mutableStateOf<(() -> Unit)?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var permissionSettingsRequired by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        permissionSettingsRequired = !hasPermission && permissionRequested &&
                (context as? Activity)?.let { activity ->
                    requiredPermissions.any {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                    }
                } == true
        if (!hasPermission) {
            scanError = if (permissionSettingsRequired) {
                "藍牙權限已停用，請至系統設定允許"
            } else {
                "未授予藍牙權限，請允許後重新搜尋"
            }
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    bluetoothEnabled = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    ) == BluetoothAdapter.STATE_ON
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermission = true
        } else {
            permissionRequested = true
            permissionLauncher.launch(requiredPermissions)
        }
    }

    LaunchedEffect(hasPermission) {
        bluetoothEnabled = if (hasPermission) {
            runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)
        } else {
            false
        }
    }

    DisposableEffect(hasPermission, bluetoothEnabled, scanAttempt) {
        if (!hasPermission || bluetoothAdapter == null || !bluetoothEnabled) {
            isScanning = false
            stopActiveScan = null
            if (hasPermission && !bluetoothEnabled) scanError = "請開啟藍牙後重新搜尋"
            onDispose { }
        } else {
            val scanner = runCatching { bluetoothAdapter.bluetoothLeScanner }.getOrNull()
            if (scanner == null) {
                scanError = "無法啟動藍牙掃描，請確認權限與藍牙狀態"
                onDispose { }
            } else {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { scanResult ->
                            try {
                                val device = scanResult.device
                                val address = device.address
                                val recordName = scanResult.scanRecord?.deviceName
                                val rawName = recordName ?: device.name
                                val finalName = if (!rawName.isNullOrBlank()) {
                                    rawName
                                } else {
                                    "未命名裝置 (${address.takeLast(5)})"
                                }
                                val newItem = BleDeviceItem(name = finalName, address = address, rssi = scanResult.rssi)

                                discoveredDevices = discoveredDevices.toMutableList().apply {
                                    val index = indexOfFirst { it.address == address }
                                    if (index != -1) this[index] = newItem else add(newItem)
                                }.sortedByDescending { !it.name.startsWith("未命名") }
                                if (address == initialSelectedAddress) selectedDevice = newItem
                            } catch (_: SecurityException) {
                                scanError = "藍牙權限已變更，請重新授予"
                                stopActiveScan?.invoke()
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        isScanning = false
                        scanError = "藍牙搜尋失敗（$errorCode），請重新搜尋"
                    }
                }

                try {
                    scanner.startScan(null, settings, scanCallback)
                    isScanning = true
                    scanError = null
                    val stop = {
                        runCatching { scanner.stopScan(scanCallback) }
                        isScanning = false
                        Unit
                    }
                    stopActiveScan = stop
                } catch (_: SecurityException) {
                    isScanning = false
                    scanError = "缺少藍牙搜尋權限，請重新授予"
                } catch (_: IllegalStateException) {
                    isScanning = false
                    scanError = "藍牙搜尋無法啟動，請稍後重試"
                } catch (_: IllegalArgumentException) {
                    isScanning = false
                    scanError = "藍牙掃描參數無效，請重新搜尋"
                }

                onDispose {
                    runCatching { scanner.stopScan(scanCallback) }
                    isScanning = false
                    stopActiveScan = null
                }
            }
        }
    }

    LaunchedEffect(scanAttempt, isScanning) {
        if (isScanning) {
            kotlinx.coroutines.delay(15_000)
            if (isScanning) {
                stopActiveScan?.invoke()
                scanError = "搜尋已逾時，請重新搜尋"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "尋找設備",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    !hasPermission -> "請授予藍牙權限以搜尋裝置"
                    !bluetoothEnabled -> "請開啟藍牙並將杯墊靠近手機"
                    else -> "請將智慧水壺底座靠近您的手機"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            BluetoothSearchPulseAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "找到的藍牙設備 (${discoveredDevices.size})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
                TextButton(
                    onClick = {
                        when {
                            !hasPermission && permissionSettingsRequired -> runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                            !hasPermission -> {
                                permissionRequested = true
                                permissionLauncher.launch(requiredPermissions)
                            }
                            !bluetoothEnabled -> runCatching {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                            else -> {
                                discoveredDevices = emptyList()
                                selectedDevice = null
                                scanError = null
                                scanAttempt++
                            }
                        }
                    },
                    enabled = !isScanning
                ) {
                    Text(
                        when {
                            isScanning -> "搜尋中…"
                            !hasPermission -> "授予權限"
                            !bluetoothEnabled -> "開啟藍牙"
                            else -> "重新搜尋"
                        }
                    )
                }
            }

            scanError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (connectionState is BleConnectionState.Error) {
                Text(
                    connectionState.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            } else if (connectionState == BleConnectionState.Connecting) {
                Text("正在連接並確認設備服務…", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(discoveredDevices, key = { it.address }) { device ->
                    val isSelected = selectedDevice?.address == device.address
                    val isNamed = !device.name.startsWith("未命名")
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFD8E2FF)
                            else if (isNamed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedDevice = if (isSelected) null else device
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isNamed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isNamed) "☕" else "📡", fontSize = 20.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isNamed) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isNamed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${device.address} (${device.rssi} dBm)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Text(
                                    text = "✔",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        selectedDevice?.let { device ->
                            stopActiveScan?.invoke()
                            onNavigateToNext(device)
                        }
                    },
                    enabled = selectedDevice != null && connectionState != BleConnectionState.Connecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val btnText = when {
                        selectedDevice == null -> "請選擇裝置"
                        connectionState == BleConnectionState.Connecting -> "連線中…"
                        connectionState == BleConnectionState.Connected &&
                                connectedDeviceAddress == selectedDevice?.address -> "繼續設定"
                        else -> "連接此藍牙裝置"
                    }
                    Text(
                        text = btnText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothSearchPulseAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        for (i in 0..2) {
            val progress = (waveProgress + i * 0.33f) % 1f
            val radius = 25f + progress * 55f
            val alpha = (1f - progress).coerceIn(0f, 1f) * 0.4f

            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY)
            )
        }

        drawCircle(
            color = Color.White,
            radius = 28f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFFE8F0FE),
            radius = 28f,
            center = Offset(centerX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        drawCircle(
            color = Color(0xFF355DA3),
            radius = 4f,
            center = Offset(centerX - 45f, centerY - 25f + floatOffset)
        )
        drawCircle(
            color = Color(0xFFBBC9D0),
            radius = 5f,
            center = Offset(centerX + 40f, centerY + 25f - floatOffset)
        )
        drawCircle(
            color = primaryColor,
            radius = 3f,
            center = Offset(centerX + 50f, centerY - 8f + floatOffset * 0.5f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page3Preview() {
    SmartCoasterTheme {
        Page3()
    }
}