import json
from types import SimpleNamespace

from django.test import TestCase
from django.urls import reverse
from django.utils import timezone

from .models import DrinkRecord, WaterGoal
from .management.commands.mqtt_subscriber import Command as MqttSubscriberCommand


class DashboardTests(TestCase):
    def test_dashboard_opens_and_creates_default_goal(self):
        response = self.client.get(reverse("dashboard"))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "智慧喝水儀表板")
        self.assertEqual(WaterGoal.objects.get(pk=1).daily_goal_ml, 2000)

    def test_manual_record_can_be_added(self):
        response = self.client.post(
            reverse("dashboard"),
            {
                "amount_ml": 250,
                "recorded_at": timezone.localtime().strftime("%Y-%m-%dT%H:%M"),
                "note": "測試紀錄",
            },
        )
        self.assertRedirects(response, reverse("dashboard"))
        record = DrinkRecord.objects.get()
        self.assertEqual(record.amount_ml, 250)
        self.assertEqual(record.source, "manual")

    def test_mqtt_api_converts_negative_weight_to_positive_ml(self):
        response = self.client.post(
            reverse("mqtt_ingest"),
            data=json.dumps({"uid": "311", "weight_diff_g": -217.4}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        record = DrinkRecord.objects.get()
        self.assertEqual(record.amount_ml, 217)
        self.assertEqual(record.source, "mqtt")
        self.assertEqual(record.device_uid, "311")

    def test_mqtt_api_uses_device_timestamp(self):
        response = self.client.post(
            reverse("mqtt_ingest"),
            data=json.dumps(
                {
                    "uid": "311",
                    "amount_ml": 300,
                    "timestamp": "2026-09-09T21:35:12+08:00",
                }
            ),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)
        record = DrinkRecord.objects.get()
        self.assertEqual(
            timezone.localtime(record.recorded_at).strftime("%Y-%m-%d %H:%M:%S"),
            "2026-09-09 21:35:12",
        )

    def test_invalid_mqtt_amount_is_rejected(self):
        response = self.client.post(
            reverse("mqtt_ingest"),
            data=json.dumps({"uid": "311", "amount_ml": 5000}),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(DrinkRecord.objects.count(), 0)

    def test_mqtt_subscriber_writes_directly_to_sqlite(self):
        """模擬 Paho 訊息，確認不經 HTTP API 也能透過 ORM 寫入。"""
        command = MqttSubscriberCommand()
        command.default_uid = "311"
        message = SimpleNamespace(
            topic="esp32311",
            payload=json.dumps(
                {
                    "uid": "311",
                    "weight_diff_g": -217.4,
                    "timestamp": "2026-09-10T15:30:45+08:00",
                }
            ).encode("utf-8"),
        )

        command.on_message(None, None, message)

        record = DrinkRecord.objects.get()
        self.assertEqual(record.amount_ml, 217)
        self.assertEqual(record.source, "mqtt")
        self.assertEqual(record.device_uid, "311")
        self.assertEqual(record.note, "MQTT：esp32311")
