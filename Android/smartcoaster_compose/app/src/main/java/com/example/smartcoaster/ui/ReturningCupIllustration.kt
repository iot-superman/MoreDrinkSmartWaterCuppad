package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.material3.MaterialTheme

@Composable
fun ReturningCupIllustration(
    modifier: Modifier = Modifier,
    isRead: Boolean = false,
    onAnimationFinished: () -> Unit = {}
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
            onAnimationFinished() // 動態完成，通知外部
        } else {
            // 未點擊前，保持在右上方拿起狀態
            returnProgress.snapTo(1f)
        }
    }
    
    val progress = returnProgress.value
    
    // 與 DrinkStep1 對應的位移參數 (拿起狀態：Y:-90, X:60, Rot:45)
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
