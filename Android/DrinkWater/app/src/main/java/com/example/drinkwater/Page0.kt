package com.example.drinkwater

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drinkwater.ui.theme.DrinkWaterTheme
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page0(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToManualIntake: () -> Unit = {} // 新增手動喝水 (Page2) 跳轉參數
) {
    // 狀態：目前飲水量與目標值
    var currentIntake by remember { mutableIntStateOf(800) }
    val maxIntake = 2000

    // 底部導覽列選取狀態 (0: 喝水, 1: 歷史紀錄, 2: 設定)
    var selectedTab by remember { mutableIntStateOf(0) }

    // 水位動畫補間
    val animatedIntake by animateIntAsState(
        targetValue = currentIntake,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "waterIntakeAnimation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Drinking Water",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
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
                        Text(
                            text = "👤",
                            fontSize = 18.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            // 底部導覽列 (Bottom Navigation Bar)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("☕", fontSize = 20.sp) },
                    label = { Text("喝水") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToHistory() // 點擊歷史紀錄前往 Page5
                    },
                    icon = { Text("📜", fontSize = 20.sp) },
                    label = { Text("歷史紀錄") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onNavigateToSettings() // 點擊設定前往 Page9
                    },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("設定") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 頂部飲水量標題與數據
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "HYDRATION STATUS",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$animatedIntake",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = " / $maxIntake ml",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
            }

            // 2. 中間人體水位 + 加水按鈕
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                HumanWaterBody(
                    progress = animatedIntake.toFloat() / maxIntake,
                    modifier = Modifier
                        .width(130.dp)
                        .fillMaxHeight()
                )

                // 圓形 + 按鈕：點擊跳轉至 Page2
                FloatingActionButton(
                    onClick = onNavigateToManualIntake,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.dp)
                ) {
                    Text(text = "+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 3. 智慧杯墊狀態卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📡", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "智慧杯墊已連線",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "目前水溫: 45°C",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Button(onClick = { }) {
                        Text("重試")
                    }
                }
            }

            // 4. 補水建議橫向滾動卡片
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "補水建議",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        SuggestionCard(
                            time = "10:00 AM",
                            title = "早晨喚醒",
                            desc = "建議飲用 250ml 溫水，幫助腸胃蠕動。"
                        )
                    }
                    item {
                        SuggestionCard(
                            time = "即將到來 - 12:30 PM",
                            title = "餐前補水",
                            desc = "午餐前半小時飲用 300ml，增加飽足感。",
                            isHighlight = true
                        )
                    }
                }
            }
        }
    }
}

// 繪製人體與波浪動畫 Component
@Composable
fun HumanWaterBody(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val bodyPath = Path().apply {
            moveTo(width * 0.5f, height * 0.06f)
            cubicTo(width * 0.58f, height * 0.06f, width * 0.63f, height * 0.09f, width * 0.63f, height * 0.13f)
            cubicTo(width * 0.63f, height * 0.16f, width * 0.58f, height * 0.19f, width * 0.5f, height * 0.19f)
            cubicTo(width * 0.42f, height * 0.19f, width * 0.37f, height * 0.16f, width * 0.37f, height * 0.13f)
            cubicTo(width * 0.37f, height * 0.09f, width * 0.42f, height * 0.06f, width * 0.5f, height * 0.06f)

            moveTo(width * 0.32f, height * 0.23f)
            cubicTo(width * 0.42f, height * 0.21f, width * 0.58f, height * 0.21f, width * 0.68f, height * 0.23f)
            cubicTo(width * 0.8f, height * 0.26f, width * 0.85f, height * 0.34f, width * 0.82f, height * 0.44f)
            cubicTo(width * 0.8f, height * 0.48f, width * 0.72f, height * 0.48f, width * 0.69f, height * 0.42f)
            lineTo(width * 0.67f, height * 0.56f)
            lineTo(width * 0.73f, height * 0.88f)
            cubicTo(width * 0.74f, height * 0.92f, width * 0.62f, height * 0.92f, width * 0.58f, height * 0.88f)
            lineTo(width * 0.5f, height * 0.64f)
            lineTo(width * 0.42f, height * 0.88f)
            cubicTo(width * 0.38f, height * 0.92f, width * 0.26f, height * 0.92f, width * 0.27f, height * 0.88f)
            lineTo(width * 0.33f, height * 0.56f)
            lineTo(width * 0.31f, height * 0.42f)
            cubicTo(width * 0.28f, height * 0.48f, width * 0.2f, height * 0.48f, width * 0.18f, height * 0.44f)
            cubicTo(width * 0.15f, height * 0.34f, width * 0.2f, height * 0.26f, width * 0.32f, height * 0.23f)
        }

        drawPath(
            path = bodyPath,
            color = Color.LightGray.copy(alpha = 0.3f)
        )

        clipPath(bodyPath) {
            val waterLevelY = height * (1f - progress.coerceIn(0f, 1f))

            val wavePath = Path().apply {
                moveTo(0f, height)
                lineTo(0f, waterLevelY)

                for (x in 0..width.toInt()) {
                    val y = waterLevelY + sin((x / width * 2 * Math.PI) + waveOffset).toFloat() * 8f
                    lineTo(x.toFloat(), y)
                }

                lineTo(width, height)
                close()
            }

            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, primaryColor),
                    startY = waterLevelY,
                    endY = height
                )
            )
        }
    }
}

// 補水建議卡片
@Composable
fun SuggestionCard(
    time: String,
    title: String,
    desc: String,
    isHighlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "☀️",
                    fontSize = 14.sp
                )
                Text(
                    text = time,
                    fontSize = 11.sp,
                    color = if (isHighlight) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlight) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    maxLines = 2,
                    color = if (isHighlight) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 即時預覽面板
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page0Preview() {
    DrinkWaterTheme {
        Page0()
    }
}