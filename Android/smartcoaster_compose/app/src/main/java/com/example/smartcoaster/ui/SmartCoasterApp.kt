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
    var previousTotalIntake by mutableFloatStateOf(800f) // 紀錄加水前的舊數值
}

@Composable
fun SmartCoasterApp() {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubPage by remember { mutableStateOf("Main") }
    var mqttStatus by remember { mutableStateOf("Initializing...") }
    var lastLog by remember { mutableStateOf("Ready") }

    // Tare Popup 狀態：只有收到 ESP32 的 Tare 開始訊息才顯示。
    // 收到 TARE_DONE 後先顯示 100% 約 0.5 秒，再自動消失。
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
                    // TARE_DONE / TARE_ERROR 到達。正常 TARE_DONE 會讓動畫到 100%。
                    // 現有韌體正常流程使用 TARE_DONE；Popup 隨後自動關閉。
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

    val showBackButton = selectedTab == 0 && currentSubPage != "Main"

    Scaffold(
        topBar = {
            Column {
                Surface(
                    color = when(mqttStatus) {
                        "Connected" -> Color(0xFF4CAF50)
                        "Error" -> Color(0xFFF44336)
                        else -> Color(0xFFFF9800)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Status: $mqttStatus | $lastLog",
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
                                currentSubPage = when (currentSubPage) {
                                    "Page2" -> "Main"
                                    "Page4" -> "Page2"
                                    "Page7" -> "Page4"
                                    else -> "Main"
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
                            else -> "Settings"
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
                NavItem(Icons.Default.History, "歷史記錄", 1, selectedTab) { selectedTab = 1 }
                NavItem(Icons.Default.Settings, "設定", 2, selectedTab) { selectedTab = 2 }
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
                            onNavigateToPage2 = {
                                // 進入手動飲水模式只切換模式，不再自動 Tare。
                                // START 必須保留杯子目前的真實重量；若在這裡 Tare，
                                // 會把杯重歸近 0，造成「不穩定中」以及 Start = 0 / -0.x 的錯誤。
                                mqttManager.publish("manualdrink")
                                coasterState.isStartRead = false
                                coasterState.isEndRead = false
                                // 進入流程時，同步舊數值
                                coasterState.previousTotalIntake = coasterState.totalIntake
                                currentSubPage = "Page2"
                            }
                        )
                        "Page2" -> Page2(
                            realTimeWeight = coasterState.realTimeWeight,
                            isStable = coasterState.isStable,
                            startWeight = coasterState.startWeight,
                            isRead = coasterState.isStartRead,
                            onReadWeight = {
                                // START：直接記住畫面目前正在收到的即時重量。
                                // 不送 tare、不送 getweight、不等待硬體重新量測，
                                // 因此不會因重新 Tare 進入「不穩定中」，也不會有 MQTT 回覆競速問題。
                                coasterState.startWeight = coasterState.realTimeWeight
                                coasterState.isStartRead = true
                            },
                            onNavigateToPage4 = { currentSubPage = "Page4" },
                            onBackClick = {
                                mqttManager.publish("legacyauto")
                                currentSubPage = "Main"
                            }
                        )
                        "Page4" -> Page4(
                            realTimeWeight = coasterState.realTimeWeight,
                            isStable = coasterState.isStable,
                            startWeight = coasterState.startWeight,
                            endWeight = coasterState.endWeight,
                            isRead = coasterState.isEndRead,
                            onReadWeight = {
                                // END：同樣直接記住目前即時重量，不 Tare、不重新量測。
                                coasterState.endWeight = coasterState.realTimeWeight
                                coasterState.isEndRead = true
                            },
                            onNavigateToPage7 = {
                                coasterState.intakeAmount = kotlin.math.abs(coasterState.startWeight - coasterState.endWeight)
                                coasterState.totalIntake += coasterState.intakeAmount
                                currentSubPage = "Page7"
                            },
                            onBackClick = { 
                                // 返回 Page2 時不需要重新 tare，只需切換頁面
                                currentSubPage = "Page2" 
                            }
                        )
                        "Page7" -> Page7(
                            intakeAmount = coasterState.intakeAmount,
                            totalIntake = coasterState.totalIntake,
                            onNavigateToHome = {
                                mqttManager.publish("legacyauto")
                                currentSubPage = "Main"
                            },
                            onNavigateToHistory = { selectedTab = 1 },
                            onBackClick = { currentSubPage = "Page4" }
                        )
                    }
                }
                1 -> PlaceholderScreen(Icons.Default.History, "歷史記錄", "紀錄內容...")
                else -> PlaceholderScreen(Icons.Default.Settings, "設定", "設定內容...")
            }

            // 全 App 共用的 Tare 校準 Popup。
            // 不論目前在哪個頁面，只要 MQTT 偵測到 ESP32 正在 Tare 就覆蓋顯示。
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
