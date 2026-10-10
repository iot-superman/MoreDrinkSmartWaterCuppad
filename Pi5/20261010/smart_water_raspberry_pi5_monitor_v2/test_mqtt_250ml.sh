#!/usr/bin/env bash
# 不接 ESP32 也可發布一筆 250 ml MQTT 測試資料。
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

if [[ ! -x ".venv/bin/python" ]]; then
  echo "尚未安裝，請先執行 ./install_pi.sh"
  exit 1
fi

.venv/bin/python manage.py mqtt_publish_test 250 --uid 311
