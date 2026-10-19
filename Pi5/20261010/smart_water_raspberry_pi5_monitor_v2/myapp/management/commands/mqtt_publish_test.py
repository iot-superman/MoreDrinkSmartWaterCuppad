import configparser
import json
from datetime import datetime
from pathlib import Path

import paho.mqtt.publish as publish
from django.conf import settings
from django.core.management.base import BaseCommand
from django.utils import timezone


class Command(BaseCommand):
    """不用 ESP32，也能從 Windows 發布一筆 MQTT 喝水測試資料。"""

    help = "發布一筆 MQTT 測試喝水資料"

    def add_arguments(self, parser):
        parser.add_argument("amount", nargs="?", type=float, default=250)
        parser.add_argument("--uid", default=None)

    def handle(self, *args, **options):
        config_path = Path(settings.BASE_DIR) / "mqtt_config.ini"
        config = configparser.ConfigParser()
        config.read(config_path, encoding="utf-8")
        mqtt_config = config["mqtt"]

        uid = options["uid"] or config.get("device", "default_uid", fallback="311")
        payload = {
            "uid": uid,
            "amount_ml": abs(options["amount"]),
            "timestamp": timezone.localtime().isoformat(timespec="seconds"),
        }

        username = mqtt_config.get("username", "").strip()
        auth = None
        if username:
            auth = {
                "username": username,
                "password": mqtt_config.get("password", "").strip(),
            }

        tls = {} if mqtt_config.getboolean("use_tls", fallback=False) else None
        # 與 Arduino 端使用相同預設 Topic：esp32 + UID。
        topic = mqtt_config.get("topic", "esp32311")
        publish.single(
            topic,
            payload=json.dumps(payload, ensure_ascii=False),
            qos=1,
            hostname=mqtt_config.get("broker", "broker.emqx.io"),
            port=mqtt_config.getint("port", 1883),
            auth=auth,
            tls=tls,
        )

        self.stdout.write(self.style.SUCCESS(f"測試訊息已發布：{topic}"))
        self.stdout.write(json.dumps(payload, ensure_ascii=False, indent=2))
