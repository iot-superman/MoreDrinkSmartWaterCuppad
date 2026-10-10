#!/usr/bin/env python
import os
import sys


def main():
    # V3：Django Project 名稱與外層資料夾統一為 water_dashboard_sqlite。
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "water_dashboard_sqlite.settings")
    try:
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        # Raspberry Pi 版本請使用 Linux 安裝腳本建立 .venv。
        raise ImportError("找不到 Django，請先執行 ./install_pi.sh") from exc
    execute_from_command_line(sys.argv)


if __name__ == "__main__":
    main()
