package com.example.drinkwater

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkwater.ui.theme.DrinkWaterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page4(
    onNavigateToNext: () -> Unit = {}, // 確認儲存後前往下一步 (如 Page0 或 完成頁)
    onBackClick: () -> Unit = {}        // 返回 Page2
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manual Intake",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFD8E2FF),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("⚡", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "藍牙已連線",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF174589)
                            )
                        }
                    }
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
                    selected = true,
                    onClick = { },
                    icon = { Text("💧", fontSize = 20.sp) },
                    label = { Text("喝水", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Text("📜", fontSize = 20.sp) },
                    label = { Text("歷史記錄", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 步驟進度卡片 (Step 2 of 3)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STEP 2 OF 3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "放回結算中",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 進度條：前段與中段亮藍，後段灰色
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // 步驟標籤
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ ", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("1. 享用飲水", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot()
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("2. 放回結算", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("3. 記錄完成", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // 2. 智慧底座感測卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚖️", fontSize = 16.sp)
                            }
                            Column {
                                Text("智慧底座感測", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("高精度微重力即時計算", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD8E2FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🔒", fontSize = 10.sp)
                                Text("讀數已平穩 ±0.1g", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF001A41))
                            }
                        }
                    }

                    // 放回重量繪圖卡片內區塊
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 平放繪圖
                            CoasterSettlingIllustration(modifier = Modifier.size(100.dp))

                            Spacer(modifier = Modifier.height(8.dp))

                            // 即時放回重量
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("85.5", fontSize = 42.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
                                Text("g", fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✅", fontSize = 12.sp)
                                Text("放回重量 (endWeight) 鎖定完成", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // 指引提示
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("ℹ️", fontSize = 14.sp)
                            Text("請將水杯放回杯墊，待讀數平穩後即可確認存入。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 3. 飲用差額運算卡片 (Formula Breakdown Card)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🧮", fontSize = 16.sp)
                            Text("飲用差額運算", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("本次飲水量計算完成", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    // 減法算式 Row: 起始初重 - 放回尾重
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("起始初重 (Start)", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                Text("320.5 g", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("放回尾重 (End)", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                Text("85.5 g", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 淨補充水分 Highlight 藍色大塊
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💧", fontSize = 22.sp)
                                }
                                Column {
                                    Text("淨補充水分 (NET HYDRATION)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                    Text("+235 ml", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 今日進度累計預估進度條
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🚩", fontSize = 12.sp)
                                Text("今日進度累計預估", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("69%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // 雙段漸層累積條 (已存 1150ml 57.5% + 本次 235ml 11.75%)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().weight(0.575f).background(MaterialTheme.colorScheme.secondary))
                            Box(modifier = Modifier.fillMaxHeight().weight(0.1175f).background(MaterialTheme.colorScheme.primary))
                            Box(modifier = Modifier.fillMaxHeight().weight(0.3075f).background(Color.Transparent))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("先前累計 1,150 ml", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text("存入後將達 1,385 / 2,000 ml", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // 4. 感測器文字說明 Note
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F0FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍵", fontSize = 24.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PRECISION SENSOR NOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "感測器已自動扣除杯身皮重，若此時杯身受外力晃動，讀數將重新偵測並校準。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // 5. 按鈕卡片（確認儲存 & 重新秤重）
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToNext,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("確認存入飲水量 (+235ml)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { /* 重新秤重邏輯 */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔄", fontSize = 14.sp)
                            Text("重新偵測放回重量", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// 智慧底座平放杯身動畫與繪圖
@Composable
fun CoasterSettlingIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // 外圍發光波動圈
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = (width * 0.4f) * pulseScale,
            center = Offset(centerX, centerY)
        )

        // 底座
        drawRoundRect(
            color = Color(0xFFC9DBF6),
            topLeft = Offset(centerX - 40f, centerY + 15f),
            size = Size(80f, 16f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // 水杯杯身
        drawRoundRect(
            color = Color(0xFFD3E4FE),
            topLeft = Offset(centerX - 24f, centerY - 30f),
            size = Size(48f, 45f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // 杯內少許剩餘水
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(centerX - 22f, centerY + 2f),
            size = Size(44f, 12f),
            cornerRadius = CornerRadius(0f, 0f)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page4Preview() {
    DrinkWaterTheme {
        Page4()
    }
}