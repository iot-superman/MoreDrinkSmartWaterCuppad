package com.example.smartcoaster.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoasterState {
    var startWeight by mutableFloatStateOf(0f)
    var endWeight by mutableFloatStateOf(0f)
    var isStartRead by mutableStateOf(false)
    var isEndRead by mutableStateOf(false)
    var deviceMode by mutableStateOf("AUTO")

    var realTimeWeight by mutableFloatStateOf(0f)
    var isStable by mutableStateOf(false)
    var intakeAmount by mutableFloatStateOf(0f)
    var totalIntake by mutableFloatStateOf(0f)
    var previousTotalIntake by mutableFloatStateOf(0f)
}

@Composable
fun SmartCoasterApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubPage by remember { mutableStateOf("Main") }
    var settingSubPage by remember { mutableStateOf("SettingMain") }
    var mqttStatus by remember { mutableStateOf("Initializing...") }
    var lastLog by remember { mutableStateOf("Ready") }

    var forceFreshMapSearch by rememberSaveable { mutableStateOf(true) }

    val bleManager = remember { BleManager(context) }
    val bleConnectionState by bleManager.connectionState.collectAsState()
    val connectedDeviceName by bleManager.connectedDeviceName.collectAsState()
    val connectedDeviceAddress by bleManager.connectedDeviceAddress.collectAsState()
    val provisioningState by bleManager.provisioningState.collectAsState()
    DisposableEffect(bleManager) {
        onDispose { bleManager.disconnect() }
    }

    var showTareDialog by remember { mutableStateOf(false) }
    var tareDone by remember { mutableStateOf(false) }

    val coasterState = remember { CoasterState() }

    val mqttManager = remember {
        MqttManager(
            onWeightReceived = { weight ->
                coasterState.realTimeWeight = weight
            },
            onStatusChanged = { status -> mqttStatus = status },
            onLogReceived = { log -> lastLog = log },
            onModeConfirmed = { mode -> coasterState.deviceMode = mode },
            onStabilityChanged = { stable -> coasterState.isStable = stable },
            onTareStateChanged = { isTaring ->
                if (isTaring) {
                    tareDone = false
                    showTareDialog = true
                } else if (showTareDialog) {
                    tareDone = true
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            mqttManager.connect()
        }
    }

    val showBackButton = (selectedTab == 0 && currentSubPage != "Main") ||
            (selectedTab == 3 && settingSubPage != "SettingMain")

    Scaffold(
        topBar = {
            if (selectedTab != 1) {
                Column {
                    Surface(
                        color = when {
                            bleConnectionState is BleConnectionState.Connected -> Color(0xFF2196F3)
                            mqttStatus == "Connected" -> Color(0xFF4CAF50)
                            mqttStatus == "Error" -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusText = when (bleConnectionState) {
                            is BleConnectionState.Connected -> "BLE: 已連線 ($connectedDeviceName)"
                            is BleConnectionState.Connecting -> "BLE: 正在建立藍牙連線..."
                            is BleConnectionState.Error -> "BLE 錯誤: ${(bleConnectionState as BleConnectionState.Error).message}"
                            else -> "Status: $mqttStatus | $lastLog"
                        }
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }

                    Surface(shadowElevation = 2.dp) {
                        Row(
                            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showBackButton) {
                                IconButton(onClick = {
                                    if (selectedTab == 0) {
                                        currentSubPage = when (currentSubPage) {
                                            "DrinkStep1" -> "Main"
                                            "DrinkStep2" -> "DrinkStep1"
                                            "DrinkStep3" -> "DrinkStep2"
                                            else -> "Main"
                                        }
                                    } else if (selectedTab == 3) {
                                        settingSubPage = when (settingSubPage) {
                                            "Page1" -> "FindDevice"
                                            else -> "SettingMain"
                                        }
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                            } else {
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            val title = when (selectedTab) {
                                0 -> if (currentSubPage == "Main") "Drinking Water" else "Manual Intake"
                                2 -> "History"
                                3 -> when (settingSubPage) {
                                    "FindDevice" -> "Find Device"
                                    "Page1" -> "Wi-Fi Setting"
                                    "ConnectSuccess" -> "Connection Result"
                                    "Page8" -> "Device Settings"
                                    else -> "Settings"
                                }
                                else -> ""
                            }
                            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp).size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavItem(Icons.Default.LocalCafe, "喝水", 0, selectedTab) {
                    selectedTab = 0
                    if (currentSubPage != "Main") mqttManager.publish("legacyauto")
                    currentSubPage = "Main"
                }
                NavItem(Icons.Default.PinDrop, "找水喝", 1, selectedTab) { selectedTab = 1 }
                NavItem(Icons.Default.History, "歷史記錄", 2, selectedTab) { selectedTab = 2 }
                NavItem(Icons.Default.Settings, "設定", 3, selectedTab) {
                    selectedTab = 3
                    settingSubPage = "SettingMain"
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    when (currentSubPage) {
                        "Main" -> DrinkingWaterScreen(
                            totalIntake = coasterState.totalIntake,
                            previousIntake = coasterState.previousTotalIntake,
                            realTimeWeight = coasterState.realTimeWeight,
                            onNavigateToDrinkStep1 = {
                                mqttManager.publish("manualdrink")
                                coasterState.isStartRead = false
                                coasterState.isEndRead = false
                                coasterState.previousTotalIntake = coasterState.totalIntake
                                currentSubPage = "DrinkStep1"
                            },
                            onTareClick = {
                                mqttManager.publish("tare")
                            }
                        )
                        "DrinkStep1" -> DrinkStep1(
                            realTimeWeight = coasterState.realTimeWeight,
                            isStable = coasterState.isStable,
                            startWeight = coasterState.startWeight,
                            isRead = coasterState.isStartRead,
                            onReadWeight = {
                                coasterState.startWeight = coasterState.realTimeWeight
                                coasterState.isStartRead = true
                            },
                            onNavigateToDrinkStep2 = { currentSubPage = "DrinkStep2" },
                            onBackClick = {
                                mqttManager.publish("legacyauto")
                                currentSubPage = "Main"
                            }
                        )
                        "DrinkStep2" -> DrinkStep2(
                            realTimeWeight = coasterState.realTimeWeight,
                            isStable = coasterState.isStable,
                            startWeight = coasterState.startWeight,
                            endWeight = coasterState.endWeight,
                            isRead = coasterState.isEndRead,
                            onReadWeight = {
                                coasterState.endWeight = coasterState.realTimeWeight
                                coasterState.isEndRead = true
                            },
                            onNavigateToDrinkStep3 = {
                                val amount = kotlin.math.abs(coasterState.startWeight - coasterState.endWeight)
                                coasterState.intakeAmount = amount
                                coasterState.totalIntake += amount

                                // 關鍵修復：在結算完成時，立即同步寫入歷史紀錄資料庫！
                                if (amount > 0f) {
                                    HistoryStorage.addRecord(context, amount)
                                    //同時mqtt pushlish to topic: esp32
                                    //{
                                    //  "uid": "android-00AE",(android-MAC ADDRESS後4碼)
                                    //  "amount_ml": ${amount}
                                    //}
                                    // 1. 組成 JSON 字串 (以水杯墊 BLE MAC 位址後 4 碼作為 uid)
                                    val rawMac = connectedDeviceAddress
                                        ?.takeIf { it.isNotBlank() }
                                        ?: bleManager.connectedDeviceName.value?.takeIf { it.isNotBlank() }
                                        ?: bleManager.getSavedMacAddress()
                                    Log.d("main", "rawMac:$rawMac")
                                    val cleanMac = rawMac.replace(":", "").replace("-", "")
                                    val last4Code = if (cleanMac.length >= 4) cleanMac.takeLast(4).uppercase() else "0000"
                                    val uid = "android-$last4Code"
                                    val jsonPayload = """
                                        {
                                          "uid": "$uid",
                                          "amount_ml": $amount
                                        }
                                    """.trimIndent()

                                    // 2. 透過 mqttManager 發布至指定 Topic (例如 esp32 或 esp32/cmd)
                                    mqttManager.publishPayload("esp32", jsonPayload)
                                }

                                currentSubPage = "DrinkStep3"
                            },
                            onBackClick = {
                                currentSubPage = "DrinkStep1"
                            }
                        )
                        "DrinkStep3" -> DrinkStep3(
                            intakeAmount = coasterState.intakeAmount,
                            totalIntake = coasterState.totalIntake,
                            onNavigateToHome = {
                                mqttManager.publish("legacyauto")
                                currentSubPage = "Main"
                            },
                            onNavigateToHistory = { selectedTab = 2 },
                            onBackClick = { currentSubPage = "DrinkStep2" }
                        )
                    }
                }
                1 -> WaterMapScreen(
                    forceFreshSearch = forceFreshMapSearch,
                    onForceFreshSearchConsumed = {
                        forceFreshMapSearch = false
                    }
                )
                2 -> {
                    Page5(
                        totalIntake = coasterState.totalIntake,
                        onClearToday = {
                            // 清除當日紀錄時，首頁當日累積水量與水位同步歸零
                            coasterState.totalIntake = 0f
                            coasterState.previousTotalIntake = 0f
                        },
                        onClearAll = {
                            // 清除全部紀錄時，首頁當日累積水量與水位同步歸零
                            coasterState.totalIntake = 0f
                            coasterState.previousTotalIntake = 0f
                        },
                        onNavigateToDrink = { selectedTab = 0 },
                        onNavigateToSettings = {
                            selectedTab = 3
                            settingSubPage = "SettingMain"
                        }
                    )
                }
                3 -> {
                    when (settingSubPage) {
                        "SettingMain" -> Setting(
                            onNavigateToDeviceConnection = { settingSubPage = "FindDevice" },
                            onNavigateToDeviceSettings = { settingSubPage = "Page8" }
                        )
                        "FindDevice" -> Page3(
                            bleManager = bleManager,
                            onNavigateToNext = { _, _ ->
                                settingSubPage = "Page1"
                            },
                            onBackClick = { settingSubPage = "SettingMain" }
                        )
                        "Page1" -> Page1(
                            bleManager = bleManager,
                            onNavigateToNext = { settingSubPage = "ConnectSuccess" },
                            onBackClick = { settingSubPage = "FindDevice" }
                        )
                        "ConnectSuccess" -> ConnectSuccess(
                            deviceName = connectedDeviceName ?: "未知設備",
                            ssid = (provisioningState as? WifiProvisioningState.Success)?.ssid ?: "",
                            ip = (provisioningState as? WifiProvisioningState.Success)?.ip ?: "未提供",
                            onBackClick = { settingSubPage = "SettingMain" }
                        )
                        "Page8" -> Page8(
                            onNavigateToHome = {
                                selectedTab = 0
                                currentSubPage = "Main"
                            },
                            onNavigateToHistory = { selectedTab = 2 },
                            onBackClick = { settingSubPage = "SettingMain" }
                        )
                    }
                }
            }

            TareCalibrationDialog(
                visible = showTareDialog,
                tareDone = tareDone,
                onFinished = {
                    showTareDialog = false
                    tareDone = false
                }
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, index: Int, selected: Int, onClick: () -> Unit) {
    NavigationBarItem(selected = selected == index, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 11.sp) })
}

@Composable
private fun PlaceholderScreen(icon: ImageVector, title: String, body: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(body, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}