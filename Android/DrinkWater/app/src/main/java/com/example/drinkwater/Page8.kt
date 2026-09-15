package com.example.drinkwater

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkwater.ui.theme.DrinkWaterTheme

enum class DeviceMode {
    AUTO, CUP, PILLBOX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page8(
    onNavigateToHome: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // 選擇模式狀態 (預設: 智慧自動)
    var selectedMode by remember { mutableStateOf(DeviceMode.AUTO) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 16.sp)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHome,
                    icon = { Text("💧", fontSize = 20.sp) },
                    label = { Text("喝水", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Text("📜", fontSize = 20.sp) },
                    label = { Text("歷史記錄", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = true, // 高亮顯示設定
                    onClick = { },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("設定", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 設備操作區塊 (Tare and Clear Actions)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "設備操作",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 去皮歸零卡片
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* 去皮動作 */ }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD8E2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚖️", fontSize = 18.sp)
                            }
                            Text(
                                text = "去皮歸零",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "重置當前重量為零",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 紀錄清零卡片
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* 清零動作 */ }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑️", fontSize = 18.sp)
                            }
                            Text(
                                text = "紀錄清零",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "清除今日所有數據",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. 識別模式選擇區塊 (Mode Selection)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "識別模式",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 智慧自動
                    ModeOptionCard(
                        title = "智慧自動",
                        description = "自動偵測水杯或藥盒，提供最佳體驗。",
                        iconEmoji = "✨",
                        isSelected = selectedMode == DeviceMode.AUTO,
                        onClick = { selectedMode = DeviceMode.AUTO }
                    )

                    // 強制水杯
                    ModeOptionCard(
                        title = "強制水杯",
                        description = "固定為水杯模式，停用自動識別功能。",
                        iconEmoji = "🥤",
                        isSelected = selectedMode == DeviceMode.CUP,
                        onClick = { selectedMode = DeviceMode.CUP }
                    )

                    // 強制藥盒
                    ModeOptionCard(
                        title = "強制藥盒",
                        description = "固定為藥盒模式，停用自動識別功能。",
                        iconEmoji = "💊",
                        isSelected = selectedMode == DeviceMode.PILLBOX,
                        onClick = { selectedMode = DeviceMode.PILLBOX }
                    )
                }
            }

            // 3. 儲存設定按鈕
            Button(
                onClick = { isSaving = true },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💾", fontSize = 18.sp)
                    Text(
                        text = if (isSaving) "儲存中..." else "儲存設定",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 模式單選卡片 Component
@Composable
fun ModeOptionCard(
    title: String,
    description: String,
    iconEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Radio 勾選圓圈
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 標題與說明
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // 右側模式 Icon
            Text(iconEmoji, fontSize = 20.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page8Preview() {
    DrinkWaterTheme {
        Page8()
    }
}