package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Page4(
    realTimeWeight: Float = 0f,
    isStable: Boolean = false,
    startWeight: Float = 0f,
    endWeight: Float = 0f,
    isRead: Boolean = false,
    onReadWeight: () -> Unit = {},
    onNavigateToPage7: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StepLabel(number = "1", title = "享用飲水", isActive = false, isDone = true)
                    StepLabel(number = "2", title = "放回結算", isActive = true, isDone = false)
                    StepLabel(number = "3", title = "紀錄完成", isActive = false, isDone = false)
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
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) { Text("⚖️", fontSize = 16.sp) }
                        Column {
                            Text("智慧底座感測", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = when {
                                    isRead -> "重量已鎖定"
                                    !isStable -> "正在偵測穩定度..."
                                    else -> "請點擊按鈕讀取重量"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onReadWeight()
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },

                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)

                    ) {
                        Text(
                            // Page 4：依需求固定顯示「已放回」，不再依 isRead / isStable 切換文字。
                            text = "我放回水杯了",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ReturningCupIllustration(
                            modifier = Modifier.size(220.dp),
                            isRead = isRead
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                // Page 4：主重量區永遠顯示 MQTT 最新的即時重量。
                                // 不再等待按下「已放回」才顯示，也不使用 --.- placeholder。
                                text = String.format("%.1f", realTimeWeight),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("g", fontSize = 16.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isRead) "✅" else if (!isStable) "⏳" else "🔒", fontSize = 12.sp)
                            Text(
                                text = when {
                                    isRead -> "放回重量鎖定完成"
                                    !isStable -> "等待重量回穩中..."
                                    else -> "請將水杯放回杯墊"
                                },
                                fontSize = 13.sp,
                                color = if (isRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 3. 飲用差額運算卡片
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
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("起始初重 (Start)", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text("${String.format("%.1f", startWeight)} g", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("放回尾重 (End)", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text("${if(isRead) String.format("%.1f", endWeight) else "--"} g", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val diff = kotlin.math.abs(startWeight - endWeight)
                Surface(
                    onClick = onNavigateToPage7,
                    enabled = isRead,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text("💧", fontSize = 22.sp) }
                            Column {
                                Text("淨補充水分 (NET HYDRATION)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                Text("+${String.format("%.0f", diff)} ml", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if(isRead) Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                    }
                }
            }
        }

//        // 4. 按鈕區
//        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//            val diff = kotlin.math.abs(startWeight - endWeight)
//            Button(
//                onClick = onNavigateToPage7,
//                enabled = isRead,
//                shape = RoundedCornerShape(28.dp),
//                modifier = Modifier.fillMaxWidth().height(50.dp)
//            ) {
//                Text("確認存入飲水量 (+${String.format("%.0f", diff)}ml)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
//            }
////            TextButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
////                Text("重新偵測放回重量", color = MaterialTheme.colorScheme.onSurfaceVariant)
////            }
//        }
    }
}

@Composable
fun StepLabel(number: String, title: String, isActive: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape)
                .background(if (isDone || isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = "${if(isDone) "✓ " else ""}$number. $title",
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive || isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ReturningCupIllustration(
    modifier: Modifier = Modifier,
    isRead: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // 歸位動畫控制 (1 -> 0 代表從喝水狀態回到初始位置)
    val returnProgress = remember { Animatable(1f) }
    
    LaunchedEffect(isRead) {
        if (isRead) {
            // 點擊「已放回」後，執行歸位動畫
            returnProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
        } else {
            // 未點擊前，保持在右上方拿起狀態
            returnProgress.snapTo(1f)
        }
    }
    
    val progress = returnProgress.value
    
    // 與 Page2 對應的位移參數 (拿起狀態：Y:-90, X:60, Rot:45)
    val translateY = progress * -90f
    val translateX = progress * 60f
    val rotation = progress * 45f

    // 呼吸動畫 (歸位後的提示)
    val infiniteTransition = rememberInfiniteTransition(label = "cupIdleAnimation")
    val idleTranslateY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleTranslateY"
    )
    
    // 水波律動
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. 底層背景圓圈
        drawCircle(
            color = Color(0xFFEBF2FE),
            radius = width * 0.45f,
            center = Offset(centerX, centerY)
        )

        // 2. 杯墊 (固定不動)
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

        // 3. 水杯與水 (執行歸位動畫)
        withTransform({
            translate(left = translateX, top = translateY + (if (progress < 0.01f) idleTranslateY else 0f))
            rotate(degrees = rotation, pivot = Offset(centerX, coasterY))
        }) {
            val cupWidth = 80f
            val cupHeight = 105f
            val cupX = centerX - cupWidth / 2f
            val cupY = coasterY - cupHeight + 6f

            // 畫杯身
            drawRoundRect(
                color = Color(0xFFD3E4FE).copy(alpha = 0.85f),
                topLeft = Offset(cupX, cupY),
                size = Size(cupWidth, cupHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // 畫杯中的水
            val waterHeight = 55f
            val waterY = cupY + (cupHeight - waterHeight)
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(cupX + 3f, waterY - 3f),
                size = Size(cupWidth - 6f, waterHeight),
                cornerRadius = CornerRadius(0f, 0f)
            )

            // 畫水面波紋
            val waveYCenter = waterY + waterHeight / 2f - 6f
            for (i in -1..1) {
                val lineY = waveYCenter + (i * 6f)
                drawLine(
                    color = Color.White.copy(alpha = 0.95f),
                    start = Offset(centerX - 14f + waveOffset, lineY),
                    end = Offset(centerX + 14f + waveOffset, lineY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page4Preview() {
    SmartCoasterTheme {
        Page4(
            startWeight = 320.5f,
            endWeight = 85.5f,
            isRead = true
        )
    }
}
