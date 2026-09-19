package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * TareCalibrationDialog
 *
 * 將使用者提供 HTML 的 Tare Popup 動畫轉為 Jetpack Compose：
 * 1. 秤盤高頻微幅抖動。
 * 2. 三層向外擴散的校準波紋。
 * 3. 中央 TARE 秤盤與狀態燈脈衝。
 * 4. 校準進度在等待 ESP32 時逐步前進到 95%，不會自行宣告完成。
 * 5. 真正收到 ESP32 的 TARE_DONE 後才到 100%，再由上層關閉 Popup。
 *
 * 注意：progress 是「使用者等待提示」，真正完成條件永遠以 ESP32 MQTT TARE_DONE 為準。
 */
@Composable
fun TareCalibrationDialog(
    visible: Boolean,
    tareDone: Boolean,
    onFinished: () -> Unit
) {
    if (!visible) return

    var progress by remember(visible, tareDone) { mutableFloatStateOf(if (tareDone) 1f else 0.06f) }

    // 未完成前逐步推進，但最多停在 95%；不能用假計時器代替硬體完成訊號。
    LaunchedEffect(visible, tareDone) {
        if (!visible) return@LaunchedEffect
        if (tareDone) {
            progress = 1f
            delay(500) // 讓使用者看見 100% / 已就緒，再自動消失。
            onFinished()
        } else {
            while (progress < 0.95f) {
                delay(180)
                progress = (progress + if (progress < 0.70f) 0.035f else 0.012f).coerceAtMost(0.95f)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(220),
        label = "tareProgress"
    )

    // HTML multiDirectionalJitter 的 Compose 對應：快速左右/上下/旋轉微振。
    val jitterTransition = rememberInfiniteTransition(label = "tareJitter")
    val jitterX by jitterTransition.animateFloat(
        initialValue = -2.2f, targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitterX"
    )
    val jitterY by jitterTransition.animateFloat(
        initialValue = 1.4f, targetValue = -1.4f,
        animationSpec = infiniteRepeatable(tween(210, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitterY"
    )
    val jitterRotation by jitterTransition.animateFloat(
        initialValue = -0.45f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(170, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitterRotation"
    )

    val waveTransition = rememberInfiniteTransition(label = "tareWaves")
    val wave by waveTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "wave"
    )
    val ledAlpha by waveTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "ledPulse"
    )

    val content = @Composable {
        TareCalibrationDialogContent(
            tareDone = tareDone,
            animatedProgress = animatedProgress,
            jitterX = jitterX,
            jitterY = jitterY,
            jitterRotation = jitterRotation,
            wave = wave,
            ledAlpha = ledAlpha
        )
    }

    if (LocalInspectionMode.current) {
        // 在 Preview 中直接渲染内容，避免 AlertDialog 弹窗在预览界面显示为空白的问题
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = { /* Tare 由硬體完成訊號控制，不允許背景點擊關閉 */ },
            confirmButton = {},
            title = null,
            text = content,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * 抽離出的對話框內容 Composable，便於在 Preview 中直接顯示。
 */
@Composable
private fun TareCalibrationDialogContent(
    tareDone: Boolean,
    animatedProgress: Float,
    jitterX: Float,
    jitterY: Float,
    jitterRotation: Float,
    wave: Float,
    ledAlpha: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 即時微振幅提示；這是視覺提示值，不冒充 HX711 真實 raw noise。
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Default.Sync,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (tareDone) "±0.00g" else "校準取樣中",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 三層聲波 + 微振秤盤，保留 HTML Popup 的主要視覺語言。
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            TareRipple(progress = (wave + 0.66f) % 1f, alphaBase = 0.16f)
            TareRipple(progress = (wave + 0.33f) % 1f, alphaBase = 0.22f)
            TareRipple(progress = wave, alphaBase = 0.30f)

            Surface(
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        translationX = jitterX
                        translationY = jitterY
                        rotationZ = jitterRotation
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.25f),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = Color(0xFF0058BC).copy(alpha = 0.28f),
                            radius = size.minDimension * 0.30f,
                            style = Stroke(width = 2f)
                        )
                    }

                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Scale,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "TARE",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // HTML 頂部藍色 LED glow。
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .size(9.dp)
                            .graphicsLayer { alpha = ledAlpha }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Text(
                        "AQUASCALE S1",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            if (tareDone) "零點校準完成" else "秤台自動去皮校準中 (Tare)...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (tareDone) "已取得 0.00g 基準，準備返回操作畫面。"
            else "秤台正在偵測零點基準，請勿放置任何物品並避免晃動桌面。",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (tareDone) "零點基準已收斂" else "正在濾除桌面環境微位移",
                        fontSize = 12.sp
                    )
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "HX711 多筆平均取樣中",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (tareDone) "0.00g（已就緒）" else "等待 TARE_DONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "校準期間請保持秤盤淨空，避免碰觸盤面",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** HTML acousticRipple1/2/3 的 Compose Canvas 版本。 */
@Composable
private fun TareRipple(progress: Float, alphaBase: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.coerceIn(0f, 1f)
        val radius = size.minDimension * (0.34f + p * 0.18f)
        drawCircle(
            color = Color(0xFF0070EB).copy(alpha = alphaBase * (1f - p)),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 3f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TareCalibrationDialogPreview() {
    SmartCoasterTheme {
        TareCalibrationDialog(
            visible = true,
            tareDone = false,
            onFinished = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TareCalibrationDialogDonePreview() {
    SmartCoasterTheme {
        TareCalibrationDialog(
            visible = true,
            tareDone = true,
            onFinished = {}
        )
    }
}
