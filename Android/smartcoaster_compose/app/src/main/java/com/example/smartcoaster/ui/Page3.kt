package com.example.smartcoaster.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import com.example.smartcoaster.ui.theme.SmartCoasterTheme

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val requiresPassword: Boolean = false
)

@SuppressLint("MissingPermission")
@Composable
fun Page3(
    onNavigateToNext: (macAddress: String, requiresPassword: Boolean) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    bleManager: BleManager? = null
) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }

    var discoveredDevices by remember { mutableStateOf<List<BleDeviceItem>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BleDeviceItem?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var pendingDevice by remember { mutableStateOf<BleDeviceItem?>(null) }
    val connectionState = bleManager?.connectionState?.collectAsState()?.value
        ?: BleConnectionState.Disconnected

    LaunchedEffect(connectionState, pendingDevice) {
        val device = pendingDevice ?: return@LaunchedEffect
        if (connectionState == BleConnectionState.Connected) {
            pendingDevice = null
            onNavigateToNext(device.address, device.requiresPassword)
        }
    }
    DisposableEffect(bleManager) {
        onDispose {
            if (bleManager?.connectionState?.value == BleConnectionState.Connecting) {
                bleManager.disconnect()
            }
        }
    }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
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
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermission = true
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            onDispose { }
        } else {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.let { scanResult ->
                        val device = scanResult.device
                        val address = device.address

                        val recordName = scanResult.scanRecord?.deviceName
                        val rawName = recordName ?: device.name
                        val finalName = if (!rawName.isNullOrBlank()) rawName else "未命名裝置 (${address.takeLast(5)})"
                        val rssi = scanResult.rssi

                        val needPass = finalName.contains("LOCK", ignoreCase = true)
                        val newItem = BleDeviceItem(name = finalName, address = address, rssi = rssi, requiresPassword = needPass)

                        discoveredDevices = discoveredDevices.toMutableList().apply {
                            val index = indexOfFirst { it.address == address }
                            if (index != -1) {
                                this[index] = newItem
                            } else {
                                add(newItem)
                            }
                        }.sortedByDescending { !it.name.startsWith("未命名") }
                    }
                }
            }

            scanner?.startScan(null, settings, scanCallback)

            onDispose {
                scanner?.stopScan(scanCallback)
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
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = "尋找設備",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (connectionState) {
                    is BleConnectionState.Error -> connectionState.message
                    BleConnectionState.Connecting -> "正在連線及訂閱設備通知..."
                    else -> if (hasPermission) "請將智慧水壺底座靠近您的手機" else "請授予藍牙與定位權限以搜尋裝置"
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
                            .clickable(enabled = connectionState != BleConnectionState.Connecting) {
                                pendingDevice = null
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
                            pendingDevice = device
                            bleManager?.connect(device.address)
                        }
                    },
                    enabled = selectedDevice != null && hasPermission && bleManager != null &&
                        connectionState != BleConnectionState.Connecting,
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
                        selectedDevice?.requiresPassword == true -> "連接此藍牙裝置"
                        else -> "開始藍牙連線"
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