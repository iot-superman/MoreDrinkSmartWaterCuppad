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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import kotlinx.coroutines.delay

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
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        onClick = onReadWeight,
                        enabled = isStable || isRead,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRead) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .alpha(if (isStable || isRead) 1f else 0.1f)
                    ) {
                        Text(
                            text = if (isRead) "已放回" else "放好了",
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
                        CoasterSettlingIllustration(modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (isRead) String.format("%.1f", endWeight) else "--.-",
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

                val diff = (kotlin.math.abs(startWeight) - kotlin.math.abs(endWeight)).coerceAtLeast(0f)
                Surface(
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

        // 4. 按鈕區
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val diff = (kotlin.math.abs(startWeight) - kotlin.math.abs(endWeight)).coerceAtLeast(0f)
            Button(
                onClick = onNavigateToPage7,
                enabled = isRead,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("確認存入飲水量 (+${String.format("%.0f", diff)}ml)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("重新偵測放回重量", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
fun CoasterSettlingIllustration(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        drawCircle(color = primaryColor.copy(alpha = 0.15f), radius = (size.width * 0.4f) * pulseScale, center = Offset(size.width / 2f, size.height / 2f))
        drawRoundRect(color = Color(0xFFC9DBF6), topLeft = Offset(size.width / 2f - 40f, size.height / 2f + 15f), size = Size(80f, 16f), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(color = Color(0xFFD3E4FE), topLeft = Offset(size.width / 2f - 24f, size.height / 2f - 30f), size = Size(48f, 45f), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(color = primaryColor, topLeft = Offset(size.width / 2f - 22f, size.height / 2f + 2f), size = Size(44f, 12f), cornerRadius = CornerRadius(0f, 0f))
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
