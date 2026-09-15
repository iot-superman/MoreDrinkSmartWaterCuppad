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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkwater.ui.theme.DrinkWaterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page2(
    onNavigateToSettings: () -> Unit = {}, // 新增：設定跳轉回調
    onNavigateToPage3: () -> Unit = {},
    onBackClick: () -> Unit = {}
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
                    selected = false,
                    onClick = onBackClick,
                    icon = { Text("💧", fontSize = 20.sp) },
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
                    onClick = onNavigateToSettings, // 點擊連至 Page9
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
            // 1. 步驟進度卡片
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
                            text = "STEP 1 OF 3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "享用飲水中",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

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
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StepLabel(number = "1", title = "享用飲水", isActive = true)
                        StepLabel(number = "2", title = "放回結算", isActive = false)
                        StepLabel(number = "3", title = "紀錄完成", isActive = false)
                    }
                }
            }

            // 2. 校準完成通知膠囊
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFD8E2FF).copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "自動歸零與基準校準完成 (Tare & Baseline OK)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF174589),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = "320.5 g",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. 核心互動視覺卡片
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            PulsingDot()
                            Text(
                                text = "杯子就位・初始重量已鎖定",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InteractiveCupIllustration(
                        modifier = Modifier.size(220.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "320.5",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "g",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "請拿起水杯喝水",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "杯墊已自動完成歸零校準，現在您可以隨時拿起杯子暢飲。喝完後請點擊下一步。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // 4. 智慧小叮嚀 Bento 卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD8E2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "智慧小叮嚀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "少量多次飲用有助於體內水分平衡吸收。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToPage3,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("已喝完，下一步", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("➔", fontSize = 16.sp)
                    }
                }

                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "取消並返回",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StepLabel(number: String, title: String, isActive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = "$number. $title",
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
fun InteractiveCupIllustration(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        drawCircle(
            color = Color(0xFFEBF2FE),
            radius = width * 0.45f,
            center = Offset(centerX, centerY)
        )

        val dropletY = centerY - 65f
        drawCircle(
            color = primaryColor.copy(alpha = 0.8f),
            radius = 6f,
            center = Offset(centerX - 16f, dropletY)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.5f),
            radius = 4f,
            center = Offset(centerX + 16f, dropletY - 4f)
        )

        val coasterY = centerY + 42f
        val coasterWidth = 140f
        val coasterHeight = 22f

        drawRoundRect(
            color = Color(0xFFC9DBF6),
            topLeft = Offset(centerX - coasterWidth / 2f, coasterY),
            size = Size(coasterWidth, coasterHeight),
            cornerRadius = CornerRadius(11f, 11f)
        )
        drawCircle(
            color = primaryColor,
            radius = 5f,
            center = Offset(centerX, coasterY + coasterHeight / 2f)
        )

        val cupWidth = 80f
        val cupHeight = 105f
        val cupX = centerX - cupWidth / 2f
        val cupY = coasterY - cupHeight + 6f

        drawRoundRect(
            color = Color(0xFFD3E4FE).copy(alpha = 0.75f),
            topLeft = Offset(cupX, cupY),
            size = Size(cupWidth, cupHeight),
            cornerRadius = CornerRadius(12f, 12f)
        )

        val waterHeight = 55f
        val waterY = cupY + (cupHeight - waterHeight)
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(cupX + 3f, waterY - 3f),
            size = Size(cupWidth - 6f, waterHeight),
            cornerRadius = CornerRadius(0f, 0f)
        )

        val waveYCenter = waterY + waterHeight / 2f - 6f
        for (i in -1..1) {
            val lineY = waveYCenter + (i * 6f)
            drawLine(
                color = Color.White.copy(alpha = 0.95f),
                start = Offset(centerX - 14f, lineY),
                end = Offset(centerX + 14f, lineY),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page2Preview() {
    DrinkWaterTheme {
        Page2()
    }
}