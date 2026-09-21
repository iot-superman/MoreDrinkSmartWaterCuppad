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
    
    // 控制按鈕何時顯示的狀態
    var animationFinished by remember { mutableStateOf(false) }

    // 當 isRead 重設時（例如重新開始流程），也要重設動畫狀態
    LaunchedEffect(isRead) {
        if (!isRead) animationFinished = false
    }

    // 當動畫完成後，自動向下捲動顯示結果
    LaunchedEffect(animationFinished) {
        if (animationFinished) {
            delay(300) // 增加延遲，確保 AnimatedVisibility 完全展開且佈署計算完成
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 800)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
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
                    Step2StepLabel(number = "1", title = "享用飲水", isActive = false, isDone = true)
                    Step2StepLabel(number = "2", title = "放回結算", isActive = true, isDone = false)
                    Step2StepLabel(number = "3", title = "紀錄完成", isActive = false, isDone = false)
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
                            isRead = isRead,
                            onAnimationFinished = { animationFinished = true }
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = animationFinished,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
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
        
        // 額外留白確保底部不會被遮擋
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun Step2StepLabel(number: String, title: String, isActive: Boolean, isDone: Boolean) {
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
