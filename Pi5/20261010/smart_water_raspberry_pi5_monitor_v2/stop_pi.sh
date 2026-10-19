#!/usr/bin/env bash
# 停止由 start_pi.sh 啟動的 Django 與 MQTT 背景程序。
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="$PROJECT_DIR/runtime"

stop_process() {
  local name="$1"
  local pid_file="$2"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid"
      echo "已停止 $name，PID=$pid"
    else
      echo "$name 未執行。"
    fi
    rm -f "$pid_file"
  else
    echo "找不到 $name PID 檔。"
  fi
}

stop_process "Django" "$RUNTIME_DIR/django.pid"
stop_process "MQTT Subscriber" "$RUNTIME_DIR/mqtt.pid"
