package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun Page1(
    onNavigateToNext: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // 狀態管理
    var ssid by remember { mutableStateOf("thmrb306") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // 按鈕同步狀態: 0=未同步, 1=同步中, 2=同步完成
    var syncStatus by remember { mutableIntStateOf(0) }
    var isPasswordError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 預留 88.dp 避開 SmartCoasterApp 的全域 TopAppBar
            Spacer(modifier = Modifier.height(88.dp))

            // 1. 頂部設備連線繪圖動畫 (Device & Coaster Canvas Animation)
            DeviceConnectionAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 標題與說明文字
            Text(
                text = "網路設定",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請輸入 Wi-Fi 密碼，讓智能杯墊連上網路以同步數據。",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Wi-Fi SSID 卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Wi-Fi 網路 (SSID)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📶", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ssid,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { /* 更換 Wi-Fi 邏輯 */ }) {
                            Text("⇄", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Wi-Fi 密碼輸入卡片
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isPasswordError) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "密碼",
                        fontSize = 12.sp,
                        color = if (isPasswordError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔒", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (it.isNotBlank()) isPasswordError = false
                            },
                            placeholder = {
                                Text(
                                    "請輸入 Wi-Fi 密碼",
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    fontSize = 16.sp
                                )
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Text(
                                text = if (isPasswordVisible) "👁️" else "🙈",
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            if (isPasswordError) {
                Text(
                    text = "請輸入密碼以繼續",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // 留白避免被底部按鈕遮擋
        }

        // 5. 底部固定動作按鈕
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (password.isBlank()) {
                            isPasswordError = true
                        } else {
                            isPasswordError = false
                            coroutineScope.launch {
                                syncStatus = 1 // 進入同步中
                                delay(2500)
                                syncStatus = 2 // 完成同步
                                delay(800)     // 停頓 0.8 秒展示完成圖示
                                onNavigateToNext() // 自動跳轉至下一頁
                            }
                        }
                    },
                    enabled = syncStatus != 1,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (syncStatus == 2) Color(0xFFD7E5ED) else MaterialTheme.colorScheme.primary,
                        contentColor = if (syncStatus == 2) Color(0xFF101D23) else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (syncStatus) {
                            0 -> {
                                Text("同步至設備", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("➔", fontSize = 18.sp)
                            }
                            1 -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("同步中...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            2 -> {
                                Text("✔", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("設定完成", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 設備連線向量圖與動態波浪 Component
@Composable
fun DeviceConnectionAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wifiWave")

    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val phoneX = width * 0.2f
        val phoneWidth = 36f
        val phoneHeight = 72f
        drawRoundRect(
            color = Color(0xFFF2F2F7),
            topLeft = Offset(phoneX - phoneWidth / 2f, centerY - phoneHeight / 2f + floatY),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(phoneX - (phoneWidth - 6f) / 2f, centerY - (phoneHeight - 6f) / 2f + floatY),
            size = Size(phoneWidth - 6f, phoneHeight - 6f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = 10f,
            center = Offset(phoneX, centerY + floatY)
        )

        val coasterX = width * 0.8f
        drawOval(
            color = Color(0xFFF2F2F7),
            topLeft = Offset(coasterX - 30f, centerY - 10f - floatY),
            size = Size(60f, 20f)
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(coasterX - 26f, centerY - 12f - floatY),
            size = Size(52f, 16f)
        )
        drawOval(
            color = Color(0xFFD8E2FF),
            topLeft = Offset(coasterX - 16f, centerY - 8f - floatY),
            size = Size(32f, 10f)
        )

        val waveStartX = phoneX + 25f
        val waveEndX = coasterX - 35f
        val waveDistance = waveEndX - waveStartX

        for (i in 0..2) {
            val progress = (waveOffset + i * 0.33f) % 1f
            val currentX = waveStartX + progress * waveDistance
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val waveHeight = sin(progress * Math.PI).toFloat() * 12f

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(currentX, centerY - waveHeight)
                quadraticTo(currentX + 10f, centerY, currentX, centerY + waveHeight)
            }

            drawPath(
                path = path,
                color = primaryColor.copy(alpha = alpha * 0.8f),
                style = Stroke(width = 3f)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page1Preview() {
    SmartCoasterTheme {
        Page1(onNavigateToNext = {})
    }
}