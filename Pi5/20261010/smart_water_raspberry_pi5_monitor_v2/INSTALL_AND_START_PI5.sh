#!/usr/bin/env bash
# Raspberry Pi 5 第一次快速測試：自動安裝套件、建立 SQLite，然後啟動儀表板與 MQTT Subscriber。
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

chmod +x install_pi.sh start_pi.sh stop_pi.sh test_mqtt_250ml.sh install_systemd.sh
./install_pi.sh
./start_pi.sh

echo
echo "快速測試已啟動。"
echo "若要上傳 Render，請先依 README_PI5_QUICKSTART.md 設定 app.env 與 mqtt_config.ini。"

