package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kotlin.math.sin

@Composable
fun Page1(
    provisioningState: WifiProvisioningState = WifiProvisioningState.Idle,
    initialSsid: String = "",
    onSsidChanged: (String) -> Unit = {},
    onSubmit: (ssid: String, password: String, openNetwork: Boolean) -> Unit = { _, _, _ -> }
) {
    var ssid by remember { mutableStateOf(initialSsid) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var openNetwork by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val isSubmitting = provisioningState == WifiProvisioningState.Sending ||
            provisioningState == WifiProvisioningState.Waiting
    val provisioningError = (provisioningState as? WifiProvisioningState.Error)?.message

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeviceConnectionAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "網路設定",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "請輸入杯墊要連線的 Wi-Fi 名稱與密碼。",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wi-Fi 網路 (SSID)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = {
                            ssid = it
                            validationError = null
                            onSsidChanged(it)
                        },
                        singleLine = true,
                        placeholder = { Text("請輸入 Wi-Fi 名稱") },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = openNetwork,
                    onCheckedChange = {
                        openNetwork = it
                        if (it) password = ""
                        validationError = null
                    },
                    enabled = !isSubmitting
                )
                Text("這是開放式 Wi-Fi（無密碼）", fontSize = 14.sp)
            }

            if (!openNetwork) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "密碼",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔒", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    validationError = null
                                },
                                placeholder = { Text("8 至 63 個 UTF-8 位元組") },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                enabled = !isSubmitting,
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                enabled = !isSubmitting
                            ) {
                                Text(if (isPasswordVisible) "👁️" else "🙈", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }

            validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            provisioningError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            if (provisioningState == WifiProvisioningState.Waiting) {
                Text(
                    "等待杯墊確認 Wi-Fi 連線…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
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
                        val error = WifiProvisioningProtocol.validate(ssid, password, openNetwork)
                        if (error != null) {
                            validationError = error
                        } else {
                            validationError = null
                            onSubmit(ssid, password, openNetwork)
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("同步中…", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("同步至設備", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("➔", fontSize = 18.sp)
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
        Page1()
    }
}