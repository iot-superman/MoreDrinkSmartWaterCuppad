package com.example.smartcoaster.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme

@Composable
fun Setting(
    onNavigateToDeviceSettings: () -> Unit = {}, // 跳轉至 Page8 (進階硬體設定)
    onNavigateToDeviceConnection: () -> Unit = {}, // 跳轉至 FindDevice (設備連線與綁定)
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 預留 88.dp 避開 SmartCoasterApp 全域 TopAppBar 遮擋
        Spacer(modifier = Modifier.height(88.dp))

        // 1. 設備管理
        SettingSection(title = "設備管理") {
            SettingRowItem(
                iconEmoji = "⚡",
                title = "設備連線與綁定",
                onClick = onNavigateToDeviceConnection
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRowItem(
                iconEmoji = "🛠️",
                title = "進階硬體設定",
                onClick = onNavigateToDeviceSettings
            )
        }

        // 2. 個人目標
        SettingSection(title = "個人目標") {
            SettingRowItem(
                iconEmoji = "💧",
                title = "每日飲水目標",
                onClick = { }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRowItem(
                iconEmoji = "⚖️",
                title = "個人身體資料",
                onClick = { }
            )
        }

        // 3. 提醒與通知
        SettingSection(title = "提醒與通知") {
            SettingRowItem(
                iconEmoji = "🔔",
                title = "智慧補水提醒",
                onClick = { }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRowItem(
                iconEmoji = "🌙",
                title = "勿擾模式",
                onClick = { }
            )
        }

        // 4. 關於與支援
        SettingSection(title = "關於與支援") {
            SettingRowItem(
                iconEmoji = "❓",
                title = "幫助與反饋",
                onClick = { }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            SettingRowItem(
                iconEmoji = "ℹ️",
                title = "版本資訊",
                onClick = { }
            )
        }

        // 頁尾宣告
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Precision Wellness Inc.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// 設定區塊容器卡片 Component
@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

// 設定項目單條列 Component
@Composable
fun SettingRowItem(
    iconEmoji: String,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 16.sp)
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "›",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingPreview() {
    SmartCoasterTheme {
        Setting()
    }
}