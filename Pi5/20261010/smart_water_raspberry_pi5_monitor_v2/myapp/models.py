from django.db import models


class DrinkRecord(models.Model):
    """每一次由手動輸入或 MQTT 裝置上傳的喝水紀錄。"""

    SOURCE_CHOICES = [
        ("manual", "手動輸入"),
        ("mqtt", "MQTT 智慧杯墊"),
    ]

    amount_ml = models.PositiveIntegerField("喝水量（ml）")
    recorded_at = models.DateTimeField("喝水時間")
    source = models.CharField(
        "資料來源", max_length=10, choices=SOURCE_CHOICES, default="manual"
    )
    device_uid = models.CharField("裝置 UID", max_length=50, blank=True)
    note = models.CharField("備註", max_length=100, blank=True)
    synced_to_render = models.BooleanField(default=False, verbose_name="已轉發至 Render")
    created_at = models.DateTimeField("建立時間", auto_now_add=True)

    class Meta:
        ordering = ["-recorded_at", "-id"]
        verbose_name = "喝水紀錄"
        verbose_name_plural = "喝水紀錄"

    def __str__(self):
        return f"{self.recorded_at:%Y-%m-%d %H:%M}－{self.amount_ml} ml"


class WaterGoal(models.Model):
    """單機版只使用第一筆資料作為每日喝水目標。"""

    daily_goal_ml = models.PositiveIntegerField("每日目標（ml）", default=2000)
    updated_at = models.DateTimeField("更新時間", auto_now=True)

    def __str__(self):
        return f"每日目標 {self.daily_goal_ml} ml"

