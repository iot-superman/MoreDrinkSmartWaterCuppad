package com.example.smartcoaster.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var totalIntake by mutableFloatStateOf(800f)
    var previousTotalIntake by mutableFloatStateOf(800f)
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

    // 初始化 BleManager 真實藍牙管理器
    val bleManager = remember { BleManager(context) }
    val bleConnectionState by bleManager.connectionState.collectAsState()
    val connectedDeviceName by bleManager.connectedDeviceName.collectAsState()

    // Tare Popup 狀態
    var showTareDialog by remember { mutableStateOf(false) }
    var tareDone by remember { mutableStateOf(false) }

    val coasterState = remember { CoasterState() }

    val mqttManager = remember {
        MqttManager(
            onWeightReceived = { weight -> coasterState.realTimeWeight = weight },
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
            (selectedTab == 2 && settingSubPage != "SettingMain")

    Scaffold(
        topBar = {
            Column {
                // 最頂端狀態條：顯示 MQTT 與 BLE 藍牙連線狀態
                Surface(
                    color = when {
                        bleConnectionState is BleConnectionState.Connected -> Color(0xFF2196F3) // 藍牙連線成功顯示藍色
                        mqttStatus == "Connected" -> Color(0xFF4CAF50)
                        mqttStatus == "Error" -> Color(0xFFF44336)
                        else -> Color(0xFFFF9800)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val bleStatusText = when (bleConnectionState) {
                        is BleConnectionState.Connected -> "BLE: 已連線 ($connectedDeviceName)"
                        is BleConnectionState.Connecting -> "BLE: 正在建立藍牙連線..."
                        is BleConnectionState.Error -> "BLE 錯誤: ${(bleConnectionState as BleConnectionState.Error).message}"
                        else -> "Status: $mqttStatus | $lastLog"
                    }
                    Text(
                        text = bleStatusText,
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
                                } else if (selectedTab == 2) {
                                    settingSubPage = "SettingMain"
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        val title = when (selectedTab) {
                            0 -> if (currentSubPage == "Main") "Drinking Water" else "Manual Intake"
                            1 -> "History"
                            else -> when (settingSubPage) {
                                "FindDevice" -> "Find Device"
                                "Page1" -> "Wi-Fi Setting"
                                "ConnectSuccess" -> "Connection Result"
                                "Page8" -> "Device Settings"
                                else -> "Settings"
                            }
                        }
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp).size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
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
                NavItem(Icons.Default.History, "歷史記錄", 1, selectedTab) {
                    selectedTab = 1
                }
                NavItem(Icons.Default.Settings, "設定", 2, selectedTab) {
                    selectedTab = 2
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
                            onTareClick = { mqttManager.publish("tare") }
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
                                coasterState.intakeAmount = kotlin.math.abs(coasterState.startWeight - coasterState.endWeight)
                                coasterState.totalIntake += coasterState.intakeAmount
                                currentSubPage = "DrinkStep3"
                            },
                            onBackClick = { currentSubPage = "DrinkStep1" }
                        )
                        "DrinkStep3" -> DrinkStep3(
                            intakeAmount = coasterState.intakeAmount,
                            totalIntake = coasterState.totalIntake,
                            onNavigateToHome = {
                                mqttManager.publish("legacyauto")
                                currentSubPage = "Main"
                            },
                            onNavigateToHistory = { selectedTab = 1 },
                            onBackClick = { currentSubPage = "DrinkStep2" }
                        )
                    }
                }
                1 -> {
                    Page5(
                        onNavigateToDrink = { selectedTab = 0 },
                        onNavigateToSettings = { selectedTab = 2 }
                    )
                }
                2 -> {
                    when (settingSubPage) {
                        "SettingMain" -> Setting(
                            onNavigateToDeviceConnection = { settingSubPage = "FindDevice" },
                            onNavigateToDeviceSettings = { settingSubPage = "Page8" }
                        )
                        "FindDevice" -> Page3(
                            onNavigateToNext = { macAddress, requiresPassword ->
                                // 發起對 ESP32 底層真實 BLE GATT 連線！
                                bleManager.connect(macAddress)

                                if (requiresPassword) {
                                    settingSubPage = "Page1"
                                } else {
                                    settingSubPage = "ConnectSuccess"
                                }
                            },
                            onBackClick = { settingSubPage = "SettingMain" }
                        )
                        "Page1" -> Page1(
                            onNavigateToNext = { settingSubPage = "ConnectSuccess" },
                            onBackClick = { settingSubPage = "FindDevice" }
                        )
                        "ConnectSuccess" -> ConnectSuccess(
                            onBackClick = { settingSubPage = "SettingMain" }
                        )
                        "Page8" -> Page8(
                            onNavigateToHome = { selectedTab = 0 },
                            onNavigateToHistory = { selectedTab = 1 },
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