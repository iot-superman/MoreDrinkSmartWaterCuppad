package com.example.smartcoaster.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import kotlinx.coroutines.delay

private const val MAX_INTAKE = 2000

@Composable
fun DrinkingWaterScreen(
    totalIntake: Float = 0f,
    previousIntake: Float = totalIntake, // 預設值為當前值
    realTimeWeight: Float = 0f,
    onNavigateToDrinkStep1: () -> Unit = {},
    onTareClick: () -> Unit = {}
) {
    // 內部狀態用來觸發動畫
    var targetIntake by remember { mutableFloatStateOf(previousIntake) }

    // 當返回主頁時，水位緩緩上升
    LaunchedEffect(totalIntake) {
        if (totalIntake != previousIntake) {
            delay(400) // 先顯示舊水位，等待一下再開始上升
            targetIntake = totalIntake
        } else {
            targetIntake = totalIntake
        }
    }

    // 動畫進度
    val animatedProgress by animateFloatAsState(
        targetValue = targetIntake / MAX_INTAKE,
        animationSpec = tween(1500, easing = FastOutSlowInEasing), // 較長的動畫時間以便觀察
        label = "waterLevelAnimation"
    )

    // 數字跳動效果
    val animatedIntakeValue by animateFloatAsState(
        targetValue = targetIntake,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "intakeTextAnimation"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp) // 為 TabBar 留出空間
    ) {
        Box(Modifier.fillMaxWidth().height(390.dp).padding(horizontal = 20.dp)) {
            Column(Modifier.align(Alignment.TopCenter).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HYDRATION STATUS", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    // 顯示跳動的數字
                    Text(String.format("%.0f", animatedIntakeValue), fontSize = 56.sp, fontWeight = FontWeight.Light)
                    Text(" / 2000 ml", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 9.dp))
                }
                HumanWaterIndicator(animatedProgress, Modifier.size(width = 140.dp, height = 280.dp))
            }
            FloatingActionButton(
                onClick = onNavigateToDrinkStep1,
                modifier = Modifier.align(Alignment.CenterEnd), containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "新增飲水紀錄", tint = Color.White) }
        }

        // 智慧杯墊即時數據卡片
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), 
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onTareClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // 底層秤重圖示
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = "歸零校準",
                            tint = Color.White
                        )
                        // 中間疊加一個小型的回轉箭頭，代表 Tare / 重設
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(1.dp)
                        )
                    }
                }
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("智慧杯墊即時數據", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot()
                        Text("  目前重量: ${String.format("%.1f", realTimeWeight)} g", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        
        Text("補水建議", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdviceCard(Icons.Default.WbSunny, "10:00 AM", "早晨喚醒", "建議飲用 250ml 溫水，幫助腸胃蠕動。", false)
            AdviceCard(Icons.Default.WaterDrop, "即將到來 - 12:30 PM", "餐前補水", "午餐前半小時飲用 300ml，增加飽足感。", true)
        }
    }
}

@Composable
private fun HumanWaterIndicator(progress: Float, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "waterWave")
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wavePhase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wavePhase2"
    )

    Canvas(modifier) {
        val sx = size.width / 100f
        val sy = size.height / 250f
        val body = Path().apply {
            moveTo(50*sx, 15*sy)
            cubicTo(58*sx,15*sy,63*sx,23*sy,63*sx,32*sy)
            cubicTo(63*sx,41*sy,58*sx,48*sy,50*sx,48*sy)
            cubicTo(42*sx,48*sy,37*sx,41*sy,37*sx,32*sy)
            cubicTo(37*sx,23*sy,42*sx,15*sy,50*sx,15*sy)
            close()
            moveTo(32*sx,58*sy)
            cubicTo(42*sx,53*sy,58*sx,53*sy,68*sx,58*sy)
            cubicTo(80*sx,66*sy,85*sx,85*sy,82*sx,110*sy)
            cubicTo(80*sx,120*sy,72*sx,120*sy,69*sx,105*sy)
            lineTo(67*sx,140*sy); lineTo(73*sx,220*sy)
            cubicTo(74*sx,230*sy,62*sx,230*sy,58*sx,220*sy)
            lineTo(50*sx,160*sy); lineTo(42*sx,220*sy)
            cubicTo(38*sx,230*sy,26*sx,230*sy,27*sx,220*sy)
            lineTo(33*sx,140*sy); lineTo(31*sx,105*sy)
            cubicTo(28*sx,120*sy,20*sx,120*sy,18*sx,110*sy)
            cubicTo(15*sx,85*sy,20*sx,66*sy,32*sx,58*sy)
            close()
        }
        drawPath(body, Color(0xFFE3E2E7).copy(alpha = .45f))
        clipPath(body) {
            val waterTop = size.height * (1f - progress.coerceIn(0f, 1f))
            drawRect(color = Color(0xFF5A9DF5), topLeft = Offset(0f, waterTop), size = Size(size.width, size.height - waterTop))

            fun createWavePath(phase: Float, amplitude: Float, wavelength: Float): Path {
                val path = Path()
                var x = -wavelength
                var first = true
                while (x <= size.width + wavelength) {
                    val radians = ((x / wavelength) + phase) * (2f * Math.PI).toFloat()
                    val y = waterTop + kotlin.math.sin(radians) * amplitude
                    if (first) { path.moveTo(x, y); first = false } else { path.lineTo(x, y) }
                    x += 3f
                }
                return path
            }
            drawPath(path = createWavePath(wavePhase1, 7.dp.toPx(), size.width * 0.72f), color = Color(0xFFD8E2FF).copy(alpha = 0.78f), style = Stroke(width = 7.dp.toPx()))
            drawPath(path = createWavePath(wavePhase2 + 0.30f, 4.5.dp.toPx(), size.width * 0.58f), color = Color(0xFFADC6FF).copy(alpha = 0.95f), style = Stroke(width = 4.dp.toPx()))
        }
        drawPath(body, color.copy(alpha = .45f), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
}

@Composable
private fun AdviceCard(icon: ImageVector, time: String, title: String, body: String, highlighted: Boolean) {
    val bg = if (highlighted) MaterialTheme.colorScheme.primary else Color.White
    val fg = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.width(256.dp).border(if (highlighted) 0.dp else 1.dp, Color(0xFFDCDDDE), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), color = bg, shadowElevation = if (highlighted) 4.dp else 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (highlighted) Color.White else MaterialTheme.colorScheme.primary)
                Text(time, fontSize = 12.sp, color = fg.copy(alpha = .8f))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = fg, fontWeight = FontWeight.SemiBold)
            Text(body, fontSize = 13.sp, color = fg.copy(alpha = .8f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DrinkingWaterScreenPreview() {
    SmartCoasterTheme {
        DrinkingWaterScreen()
    }
}
