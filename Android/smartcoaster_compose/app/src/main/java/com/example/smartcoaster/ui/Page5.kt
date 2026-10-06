package com.example.smartcoaster.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page5(
    onNavigateToDrink: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedRange by remember { mutableStateOf("本週") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 18.sp)
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
                    onClick = onNavigateToDrink,
                    icon = { Text("☕", fontSize = 20.sp) },
                    label = { Text("喝水", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Text("📜", fontSize = 20.sp) },
                    label = { Text("歷史記錄", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 本週飲水量圖表卡片
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 卡片標題與選單
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "本週飲水量",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { isDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(selectedRange, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("▾", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("本週") },
                                    onClick = { selectedRange = "本週"; isDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("上週") },
                                    onClick = { selectedRange = "上週"; isDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("本月") },
                                    onClick = { selectedRange = "本月"; isDropdownExpanded = false }
                                )
                            }
                        }
                    }

                    // 柱狀圖繪製 (包含下方一到日標籤)
                    WeeklyBarChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    // 數據彙整（平均日飲水 & 達成率）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "平均日飲水",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "1,850",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ml",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "達成率",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "92%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. 最近記錄列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近記錄",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "顯示全部",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("›", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                // 分組 1: 今天
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "今天",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    RecordCardItem(
                        iconEmoji = "💧",
                        title = "智能馬克杯",
                        time = "14:30",
                        valueText = "+250ml",
                        isBlueValue = true
                    )

                    RecordCardItem(
                        iconEmoji = "☕",
                        title = "手動記錄 (咖啡)",
                        time = "10:15",
                        amountText = "+300ml",
                        valueText = "+300ml",
                        isBlueValue = false
                    )

                    RecordCardItem(
                        iconEmoji = "💧",
                        title = "智能馬克杯",
                        time = "08:45",
                        valueText = "+450ml",
                        isBlueValue = true
                    )
                }

                // 分組 2: 昨天
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "昨天",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    RecordCardItem(
                        iconEmoji = "💧",
                        title = "智能馬克杯",
                        time = "21:10",
                        valueText = "+200ml",
                        isBlueValue = true
                    )

                    RecordCardItem(
                        iconEmoji = "✏️",
                        title = "目標調整",
                        time = "15:00",
                        valueText = "已更新至 2000ml",
                        isBlueValue = false,
                        isSecondaryText = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 精準還原柱狀圖 Component
@Composable
fun WeeklyBarChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val barLightBlue = Color(0xFFD8E2FF)
    val gridLineColor = Color(0xFFE3E2E7)

    val heights = listOf(0.55f, 0.75f, 0.88f, 0.5f, 0.7f, 0.98f, 0.35f)
    val days = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            // 繪製背景 3 條平行的虛網格線
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (i in 1..3) {
                val y = (height / 4f) * i
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }

            // 繪製 7 根長條柱
            val barWidth = 22.dp.toPx()
            val spacing = (width - (barWidth * 7)) / 8f

            for (i in 0..6) {
                val x = spacing + i * (barWidth + spacing)
                val barH = height * heights[i]
                val y = height - barH

                val isSaturday = (i == 5) // 週六高亮深藍

                drawRoundRect(
                    color = if (isSaturday) primaryColor else barLightBlue,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // X 軸一到日標籤
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEachIndexed { index, day ->
                val isSaturday = (index == 5)
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = if (isSaturday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSaturday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 歷史記錄條目卡片 Component
@Composable
fun RecordCardItem(
    iconEmoji: String,
    title: String,
    time: String,
    valueText: String,
    isBlueValue: Boolean,
    amountText: String = "",
    isSecondaryText: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 淺藍色圖示圓底
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 18.sp)
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = valueText,
                fontSize = if (isSecondaryText) 13.sp else 18.sp,
                fontWeight = if (isBlueValue) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isBlueValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page5Preview() {
    SmartCoasterTheme {
        Page5()
    }
}