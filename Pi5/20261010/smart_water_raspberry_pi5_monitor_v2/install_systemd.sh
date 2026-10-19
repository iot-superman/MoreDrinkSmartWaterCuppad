#!/usr/bin/env bash
# 將 Django 與 MQTT Subscriber 安裝成 systemd 服務，開機後自動常駐。
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CURRENT_USER="$(id -un)"
DJANGO_SERVICE="/etc/systemd/system/smart-water-dashboard.service"
MQTT_SERVICE="/etc/systemd/system/smart-water-mqtt.service"

if [[ ! -x "$PROJECT_DIR/.venv/bin/gunicorn" ]]; then
  echo "尚未完成安裝，請先執行 ./install_pi.sh"
  exit 1
fi

DJANGO_TEMP="$(mktemp)"
MQTT_TEMP="$(mktemp)"
trap 'rm -f "$DJANGO_TEMP" "$MQTT_TEMP"' EXIT

printf '%s\n' \
  '[Unit]' \
  'Description=Smart Water Django Dashboard' \
  'After=network-online.target' \
  'Wants=network-online.target' \
  '' \
  '[Service]' \
  'Type=simple' \
  "User=$CURRENT_USER" \
  "WorkingDirectory=$PROJECT_DIR" \
  "EnvironmentFile=-$PROJECT_DIR/app.env" \
  "ExecStart=$PROJECT_DIR/.venv/bin/gunicorn --bind 0.0.0.0:8000 --workers 2 water_dashboard_sqlite.wsgi:application" \
  'Restart=always' \
  'RestartSec=5' \
  '' \
  '[Install]' \
  'WantedBy=multi-user.target' > "$DJANGO_TEMP"

printf '%s\n' \
  '[Unit]' \
  'Description=Smart Water MQTT Subscriber' \
  'After=network-online.target smart-water-dashboard.service' \
  'Wants=network-online.target' \
  '' \
  '[Service]' \
  'Type=simple' \
  "User=$CURRENT_USER" \
  "WorkingDirectory=$PROJECT_DIR" \
  "EnvironmentFile=-$PROJECT_DIR/app.env" \
  "ExecStart=$PROJECT_DIR/.venv/bin/python manage.py mqtt_subscriber" \
  'Restart=always' \
  'RestartSec=5' \
  '' \
  '[Install]' \
  'WantedBy=multi-user.target' > "$MQTT_TEMP"

sudo install -m 0644 "$DJANGO_TEMP" "$DJANGO_SERVICE"
sudo install -m 0644 "$MQTT_TEMP" "$MQTT_SERVICE"
sudo systemctl daemon-reload
sudo systemctl enable --now smart-water-dashboard.service smart-water-mqtt.service

echo "systemd 常駐服務安裝完成。"
echo "狀態：sudo systemctl status smart-water-dashboard smart-water-mqtt"
echo "MQTT 日誌：journalctl -u smart-water-mqtt -f"
