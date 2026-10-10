#!/usr/bin/env bash
# 一鍵啟動 Django 儀表板與 MQTT Subscriber；PID 與輸出存放在 runtime/。
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$PROJECT_DIR/runtime"
ENV_FILE="$PROJECT_DIR/app.env"

cd "$PROJECT_DIR"
mkdir -p "$RUNTIME_DIR"

if [[ ! -x ".venv/bin/python" ]]; then
  echo "尚未安裝，請先執行：chmod +x install_pi.sh && ./install_pi.sh"
  exit 1
fi

# 若有 pi.env 就載入；沒有時仍能以本機預設值執行。
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ -f "$RUNTIME_DIR/django.pid" ]] && kill -0 "$(cat "$RUNTIME_DIR/django.pid")" 2>/dev/null; then
  echo "Django 已經在執行。"
else
  nohup .venv/bin/gunicorn \
    --bind 0.0.0.0:8000 \
    --workers 2 \
    --access-logfile "$RUNTIME_DIR/django_access.log" \
    --error-logfile "$RUNTIME_DIR/django_error.log" \
    water_dashboard_sqlite.wsgi:application \
    >/dev/null 2>&1 &
  echo $! > "$RUNTIME_DIR/django.pid"
fi

if [[ -f "$RUNTIME_DIR/mqtt.pid" ]] && kill -0 "$(cat "$RUNTIME_DIR/mqtt.pid")" 2>/dev/null; then
  echo "MQTT Subscriber 已經在執行。"
else
  nohup .venv/bin/python manage.py mqtt_subscriber \
    >"$RUNTIME_DIR/mqtt.log" 2>&1 &
  echo $! > "$RUNTIME_DIR/mqtt.pid"
fi

PI_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
echo "智慧喝水監看程式已啟動。"
echo "本機網址：http://127.0.0.1:8000/"
if [[ -n "$PI_IP" ]]; then
  echo "區網網址：http://$PI_IP:8000/"
fi
echo "MQTT 日誌：$RUNTIME_DIR/mqtt.log"
