package com.example.smartcoaster.ui

import kotlin.math.roundToInt

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 飲水紀錄資料結構
data class IntakeRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val iconEmoji: String = "💧",
    val title: String = "智慧杯墊",
    val amountMl: Float
) {
    val timeStr: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    val dateGroupKey: String
        get() {
            val recordCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val nowCal = Calendar.getInstance()

            val recordYear = recordCal.get(Calendar.YEAR)
            val recordDay = recordCal.get(Calendar.DAY_OF_YEAR)
            val nowYear = nowCal.get(Calendar.YEAR)
            val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)

            return when {
                recordYear == nowYear && recordDay == nowDay -> "今天"
                recordYear == nowYear && recordDay == nowDay - 1 -> "昨天"
                else -> SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date(timestamp))
            }
        }

    val dayOfWeekIndex: Int
        get() {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
        }
}

// 本地數據持久化工具
object HistoryStorage {
    private const val PREF_NAME = "smart_coaster_history"
    private const val KEY_RECORDS = "records_json"

    fun saveRecords(context: Context, records: List<IntakeRecord>) {
        val jsonArray = JSONArray()
        records.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("iconEmoji", item.iconEmoji)
                put("title", item.title)
                put("amountMl", item.amountMl.toDouble())
            }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECORDS, jsonArray.toString())
            .apply()
    }

    // 即時新增紀錄至首位
    fun addRecord(context: Context, amountMl: Float) {
        val currentRecords = loadRecords(context).toMutableList()
        currentRecords.add(0, IntakeRecord(amountMl = amountMl))
        saveRecords(context, currentRecords)
    }

    fun loadRecords(context: Context): List<IntakeRecord> {
        val jsonStr = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECORDS, null) ?: return emptyList()
        val list = mutableListOf<IntakeRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    IntakeRecord(
                        id = obj.getLong("id"),
                        timestamp = obj.getLong("timestamp"),
                        iconEmoji = obj.getString("iconEmoji"),
                        title = obj.getString("title"),
                        amountMl = obj.getDouble("amountMl").toFloat()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_RECORDS)
            .apply()
    }
}

@Composable
fun Page5(
    totalIntake: Float = 0f,
    onClearToday: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onNavigateToDrink: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedRange by remember { mutableStateOf("本週") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var showClearTodayDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    var recordsList by remember { mutableStateOf<List<IntakeRecord>>(emptyList()) }

    // 每次進入或開啟歷史紀錄頁面時，自動重新載入最新資料庫的資料
    LaunchedEffect(Unit) {
        recordsList = HistoryStorage.loadRecords(context)
    }

    val weeklyIntakes = remember(recordsList) {
        val array = FloatArray(7) { 0f }
        val nowCal = Calendar.getInstance()
        val currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = nowCal.get(Calendar.YEAR)

        recordsList.forEach { record ->
            val recordCal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            if (recordCal.get(Calendar.WEEK_OF_YEAR) == currentWeek &&
                recordCal.get(Calendar.YEAR) == currentYear) {
                val idx = record.dayOfWeekIndex
                array[idx] += record.amountMl
            }
        }
        array
    }

    val averageIntake = remember(weeklyIntakes) {
        val activeDays = weeklyIntakes.count { it > 0f }
        if (activeDays > 0) (weeklyIntakes.sum() / activeDays).toInt() else 0
    }

    val achievementRate = ((totalIntake / 2000f) * 100).toInt().coerceIn(0, 100)

    val groupedRecords = remember(recordsList) {
        recordsList.groupBy { it.dateGroupKey }
    }

    val todayIndex = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun clearTodayRecords() {
        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)

        val updatedList = recordsList.filterNot { record ->
            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
            cal.get(Calendar.YEAR) == nowYear && cal.get(Calendar.DAY_OF_YEAR) == nowDay
        }
        recordsList = updatedList
        HistoryStorage.saveRecords(context, updatedList)
        onClearToday()
    }

    fun clearAllRecords() {
        recordsList = emptyList()
        HistoryStorage.clearAll(context)
        onClearAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本週飲水量",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { isDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(selectedRange, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("▾", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("本週") }, onClick = { selectedRange = "本週"; isDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("上週") }, onClick = { selectedRange = "上週"; isDropdownExpanded = false })
                            DropdownMenuItem(text = { Text("本月") }, onClick = { selectedRange = "本月"; isDropdownExpanded = false })
                        }
                    }
                }

                WeeklyBarChart(
                    weeklyIntakes = weeklyIntakes,
                    todayIndex = todayIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("平均日飲水", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%,d", averageIntake),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("ml", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp, start = 4.dp))
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("今日達成率", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$achievementRate%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近記錄",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (recordsList.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("尚無飲水記錄", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                groupedRecords.forEach { (dateGroup, items) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = dateGroup,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                        items.forEach { item ->
                            RecordCardItem(
                                iconEmoji = item.iconEmoji,
                                title = item.title,
                                time = item.timeStr,
                                valueText = "+${item.amountMl.roundToInt()}ml",
                                isBlueValue = true
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showClearTodayDialog = true },
                shape = RoundedCornerShape(12.dp),
                enabled = recordsList.any { it.dateGroupKey == "今天" },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("清除當日紀錄", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = { showClearAllDialog = true },
                shape = RoundedCornerShape(12.dp),
                enabled = recordsList.isNotEmpty(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("清除全部紀錄", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showClearTodayDialog) {
        AlertDialog(
            onDismissRequest = { showClearTodayDialog = false },
            title = { Text("清除當日紀錄") },
            text = { Text("確定要刪除今天所有的飲水紀錄嗎？此動作無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearTodayRecords()
                        showClearTodayDialog = false
                    }
                ) {
                    Text("確定清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTodayDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("清除全部紀錄") },
            text = { Text("確定要刪除歷史庫中所有的飲水紀錄嗎？此動作無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearAllRecords()
                        showClearAllDialog = false
                    }
                ) {
                    Text("確定全部刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun WeeklyBarChart(
    weeklyIntakes: FloatArray,
    todayIndex: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val barLightBlue = Color(0xFFD8E2FF)
    val gridLineColor = Color(0xFFE3E2E7)

    val days = listOf("一", "二", "三", "四", "五", "六", "日")
    val maxTarget = 2000f

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (i in 1..3) {
                val y = (height / 4f) * i
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }

            val barWidth = 22.dp.toPx()
            val spacing = (width - (barWidth * 7)) / 8f

            for (i in 0..6) {
                val x = spacing + i * (barWidth + spacing)
                val intakeRatio = (weeklyIntakes[i] / maxTarget).coerceIn(0f, 1f)
                val barH = (height * intakeRatio).coerceAtLeast(4.dp.toPx())
                val y = height - barH

                val isToday = (i == todayIndex)

                drawRoundRect(
                    color = if (isToday && intakeRatio > 0f) primaryColor else if (intakeRatio > 0f) Color(0xFF7CA6F8) else barLightBlue.copy(alpha = 0.5f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEachIndexed { index, day ->
                val isToday = (index == todayIndex)
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecordCardItem(
    iconEmoji: String,
    title: String,
    time: String,
    valueText: String,
    isBlueValue: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iconEmoji, fontSize = 18.sp)
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = valueText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBlueValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Page5Preview() {
    SmartCoasterTheme {
        Page5(totalIntake = 450f)
    }
}