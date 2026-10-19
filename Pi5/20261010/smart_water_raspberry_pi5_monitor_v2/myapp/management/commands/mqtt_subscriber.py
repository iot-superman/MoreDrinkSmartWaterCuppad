import configparser
import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

import paho.mqtt.client as mqtt
from django.conf import settings
from django.core.management.base import BaseCommand
from django.db import close_old_connections
from django.utils import timezone
from django.utils.dateparse import parse_datetime

from myapp.models import DrinkRecord


class Command(BaseCommand):
    """常駐訂閱 MQTT，收到喝水資料後直接透過 ORM 寫入 SQLite。"""

    help = "常駐訂閱 MQTT，將喝水紀錄直接寫入本機 SQLite"

    def handle(self, *args, **options):
        config = self.load_config()
        self.config = config
        mqtt_config = config["mqtt"]

        # Arduino 端規格為 esp32 + UID，例如 UID 311 使用 esp32311。
        self.topic = mqtt_config.get("topic", "esp32311")
        self.default_uid = config.get("device", "default_uid", fallback="311")

        client = mqtt.Client(
            callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
            client_id="django-water-subscriber",
            protocol=mqtt.MQTTv311,
        )

        username = mqtt_config.get("username", "").strip()
        password = mqtt_config.get("password", "").strip()
        if username:
            client.username_pw_set(username, password)

        if mqtt_config.getboolean("use_tls", fallback=False):
            # 使用系統預設 CA 憑證驗證 Broker。
            client.tls_set()

        client.on_connect = self.on_connect
        client.on_disconnect = self.on_disconnect
        client.on_message = self.on_message
        client.reconnect_delay_set(min_delay=1, max_delay=120)

        broker = mqtt_config.get("broker", "broker.emqx.io")
        port = mqtt_config.getint("port", 1883)
        self.stdout.write(f"正在連線 MQTT：{broker}:{port}")
        self.stdout.write(f"準備訂閱 Topic：{self.topic}")

        try:
            # 使用非同步初次連線：樹莓派剛開機、Wi-Fi 或 DNS 尚未就緒時，
            # Subscriber 不會直接結束，而會依 reconnect_delay_set 持續重試。
            client.connect_async(broker, port, keepalive=60)
            client.loop_forever(retry_first_connection=True)
        except KeyboardInterrupt:
            self.stdout.write(self.style.WARNING("\nMQTT Subscriber 已停止。"))
            client.disconnect()

    @staticmethod
    def load_config():
        """從專案根目錄 mqtt_config.ini 讀取設定。"""
        config_path = Path(settings.BASE_DIR) / "mqtt_config.ini"
        if not config_path.exists():
            raise FileNotFoundError(f"找不到 MQTT 設定檔：{config_path}")
        config = configparser.ConfigParser()
        config.read(config_path, encoding="utf-8")
        return config

    def on_connect(self, client, userdata, flags, reason_code, properties):
        if reason_code == 0:
            client.subscribe(self.topic, qos=1)
            self.stdout.write(self.style.SUCCESS(f"MQTT 已連線並訂閱：{self.topic}"))
        else:
            self.stderr.write(self.style.ERROR(f"MQTT 連線失敗：{reason_code}"))

    def on_disconnect(
        self, client, userdata, disconnect_flags, reason_code, properties
    ):
        self.stdout.write(self.style.WARNING(f"MQTT 連線中斷：{reason_code}，等待重連…"))

    def on_message(self, client, userdata, message):
        """支援 amount_ml 或負數 weight_diff_g 兩種喝水量格式。"""
        try:
            payload_text = message.payload.decode("utf-8")
            payload = json.loads(payload_text)
            raw_amount = payload.get("amount_ml", payload.get("weight_diff_g"))
            if raw_amount is None:
                raise ValueError("缺少 amount_ml 或 weight_diff_g")

            amount_ml = int(round(abs(float(raw_amount))))
            if not 1 <= amount_ml <= 3000:
                raise ValueError("喝水量必須介於 1～3000 ml")

            recorded_at = self.parse_timestamp(payload.get("timestamp"))

            # 長時間執行的程序先清理過期資料庫連線，再操作 ORM。
            close_old_connections()
            record = DrinkRecord.objects.create(
                amount_ml=amount_ml,
                recorded_at=recorded_at,
                source="mqtt",
                device_uid=str(payload.get("uid", self.default_uid))[:50],
                note=f"MQTT：{message.topic}"[:100],
            )
            close_old_connections()

            self.stdout.write(
                self.style.SUCCESS(
                    f"SQLite 儲存成功：ID={record.id}、{amount_ml} ml、"
                    f"UID={record.device_uid}、時間={timezone.localtime(recorded_at):%Y-%m-%d %H:%M:%S}"
                )
            )
            # 先確保 SQLite 已保存，再視設定選擇是否同步至 Render。
            # Render 暫時休眠或網路斷線時，本機監看紀錄仍不會遺失。
            self.sync_to_render(
                amount_ml=amount_ml,
                recorded_at=recorded_at,
                device_uid=record.device_uid,
        record=record,
            )
        except (UnicodeDecodeError, json.JSONDecodeError, TypeError, ValueError) as error:
            self.stderr.write(
                self.style.ERROR(
                    f"忽略無效訊息：Topic={message.topic}、錯誤={error}"
                )
            )
        except Exception as error:
            close_old_connections()
            self.stderr.write(self.style.ERROR(f"寫入 SQLite 失敗：{error}"))

    def sync_to_render(self, amount_ml, recorded_at, device_uid, record=None):
        """依 mqtt_config.ini 設定，把相同喝水紀錄同步至 Render API。"""
        # 單元測試可能直接呼叫 on_message，因此 config 尚未建立時不進行同步。
        config = getattr(self, "config", None)
        if config is None or not config.has_section("render"):
            return

        render_config = config["render"]
        if not render_config.getboolean("enabled", fallback=False):
            return

        api_url = render_config.get("api_url", "").strip()
        api_key_env = render_config.get("api_key_env", "MQTT_API_KEY").strip()
        api_key = os.getenv(api_key_env, "").strip()
        timeout_seconds = render_config.getint("timeout_seconds", fallback=20)

        if not api_url:
            self.stderr.write(self.style.WARNING("Render 同步已啟用，但未設定 api_url。"))
            return
        if not api_key:
            self.stderr.write(
                self.style.WARNING(
                    f"Render 同步已啟用，但環境變數 {api_key_env} 沒有 API Key。"
                )
            )
            return

        payload = json.dumps(
            {
                "uid": device_uid,
                "amount_ml": amount_ml,
                "timestamp": timezone.localtime(recorded_at).isoformat(
                    timespec="seconds"
                ),
            }
        ).encode("utf-8")
        request = Request(
            api_url,
            data=payload,
            method="POST",
            headers={
                "Content-Type": "application/json; charset=utf-8",
                "X-API-Key": api_key,
                "User-Agent": "SmartWaterPiMonitor/1.0",
            },
        )

        try:
            with urlopen(request, timeout=timeout_seconds) as response:
                response_text = response.read().decode("utf-8")
            if response.status in (200, 201):
                self.stdout.write(
                    self.style.SUCCESS(f"Render 同步成功：{response_text}")
                )
                if record:
                    record.synced_to_render = True
                    record.save(update_fields=['synced_to_render'])
            else:
                    self.stderr.write(
                        self.style.WARNING(
                            f"Render 回應非預期狀態 {response.status}：{response_text}"
                        )
                    )
        except HTTPError as error:
            error_text = error.read().decode("utf-8", errors="replace")
            self.stderr.write(
                self.style.ERROR(f"Render 同步失敗 HTTP {error.code}：{error_text}")
            )
        except URLError as error:
            self.stderr.write(self.style.WARNING(f"Render 暫時無法連線：{error.reason}"))

    @staticmethod
    def parse_timestamp(value):
        """解析 ESP32 ISO 8601 時間；未提供時使用收到訊息的時間。"""
        if not value:
            return timezone.now()
        parsed = parse_datetime(str(value))
        if parsed is None:
            raise ValueError("timestamp 必須是 ISO 8601 格式")
        if timezone.is_naive(parsed):
            parsed = timezone.make_aware(parsed, timezone.get_current_timezone())
        return parsed
