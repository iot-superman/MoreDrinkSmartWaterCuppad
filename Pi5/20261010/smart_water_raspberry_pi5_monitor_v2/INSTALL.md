# 智慧水杯墊監看與 MQTT 轉發服務：安裝與交接手冊

> 適用：Raspberry Pi 4 / Raspberry Pi 5（建議 64 位元 Raspberry Pi OS）。  
> 專案：`smart_water_raspberry_pi5_monitor_v2`  
> 文件版本：2026-10-10  
> **說明**：本手冊依現有 `INSTALL_AND_START_PI5.sh`、`install_systemd.sh`、`start_pi.sh`、`stop_pi.sh` 與使用者提供的 `requirements.txt` 編寫。尚未取得 `install_pi.sh`、`mqtt_subscriber.py`、`mqtt_config.ini`、`app.env.example` 的內容，因此對這些檔案的內部設定不做臆測；正式上線前必須依本手冊驗證。

## 1. 系統用途與架構

本專案在樹莓派上執行兩個獨立程序：

1. **Django 儀表板**：使用 Gunicorn 監聽 `0.0.0.0:8000`，WSGI 入口為 `water_dashboard_sqlite.wsgi:application`。
2. **MQTT 訂閱程式**：執行 `python manage.py mqtt_subscriber`。實際訂閱主題、Broker、資料處理與遠端轉發行為，須依 `mqtt_subscriber.py` 和組態檔確認。

服務預設使用專案內的 `db.sqlite3`（是否有其他資料庫設定，仍以 Django 設定檔為準）。

```text
智慧水杯墊／ESP32
        │ MQTT
        ▼
 MQTT Broker
        │
        ▼
 Raspberry Pi 4 / 5
 ├─ smart-water-mqtt.service
 │    └─ .venv/bin/python manage.py mqtt_subscriber
 ├─ smart-water-dashboard.service
 │    └─ .venv/bin/gunicorn 0.0.0.0:8000
 ├─ db.sqlite3
 ├─ mqtt_config.ini
 └─ app.env
        │
        └─ 遠端轉發（若已在程式及組態啟用）
```

## 2. 安裝前需求

- Raspberry Pi 4 或 Raspberry Pi 5、電源、microSD／SSD、可用網路。
- 建議安裝 **64 位元 Raspberry Pi OS**，並啟用 SSH。
- Python **3.12 以上**（本專案鎖定 Django 6.0.8；原 Pi 5 曾使用 Python 3.13）。
- 一份完整專案原始碼及設定檔；**不要直接沿用舊機器的 `.venv`**。
- MQTT Broker 連線資訊，以及如有遠端轉發所需的憑證與網址。
- 安裝者須有 `sudo` 權限。

> Raspberry Pi 4 是硬體相容性目標，能否安裝仍取決於 OS／Python 版本與套件可用性。若 `python3` 低於 3.12，請先安裝支援的作業系統／Python，不要直接把 Django 降版而不測試。

## 3. 準備新樹莓派

在新樹莓派終端機或 SSH 中執行：

```bash
# 查看作業系統、CPU 架構及 Python 版本
cat /etc/os-release
uname -m
python3 --version

# 更新系統套件清單與已安裝套件
sudo apt update
sudo apt upgrade -y

# 安裝基本工具與建立虛擬環境所需元件
sudo apt install -y python3 python3-venv python3-pip git rsync curl

# 確認 Python 至少為 3.12；若不是，先處理版本相容性
python3 --version
```

設定 SSH、主機名稱與 Wi-Fi，請參閱 Raspberry Pi 官方文件。若新機器的使用者不是 `pi`，以下命令中的使用者名稱及路徑都應依實際環境調整。

## 4. 從 Mac 複製完整專案到新樹莓派

Mac 上的來源範例：

```text
/Users/chengmax/MoreDrinkSmartWaterCuppad/Pi5/RaspberryPi5_Backup/20261010/smart_water_raspberry_pi5_monitor_v2
```

在 **Mac 終端機**執行（將 `NEW_PI_IP` 改為新樹莓派 IP，`pi` 改為實際登入帳號）：

```bash
# 確認 Mac 上備份來源存在
ls -la "/Users/chengmax/MoreDrinkSmartWaterCuppad/Pi5/RaspberryPi5_Backup/20261010/smart_water_raspberry_pi5_monitor_v2"

# 建立遠端放置位置
ssh pi@NEW_PI_IP 'mkdir -p ~/smart_water_raspberry_pi5_monitor_v2'

# 複製專案內容；排除舊 Pi 的虛擬環境、快取、執行時 PID 與日誌
# 注意：預設會帶入 app.env、mqtt_config.ini、db.sqlite3 等檔案，請保護憑證及個資
rsync -av --progress \
  --exclude='.venv/' \
  --exclude='__pycache__/' \
  --exclude='*.pyc' \
  --exclude='runtime/' \
  "/Users/chengmax/MoreDrinkSmartWaterCuppad/Pi5/RaspberryPi5_Backup/20261010/smart_water_raspberry_pi5_monitor_v2/" \
  "pi@NEW_PI_IP:~/smart_water_raspberry_pi5_monitor_v2/"
```

**新環境建議**：若不需要保留舊機器飲水紀錄，可先把 `db.sqlite3` 另存備份後，依第 7 節建立新資料庫；不要直接刪除唯一一份資料庫。

## 5. 安裝 Python 套件

在 **新樹莓派**執行：

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2

# 建立本機專屬的虛擬環境
python3 -m venv .venv

# 更新 pip
.venv/bin/python -m pip install --upgrade pip

# 安裝已確認的依賴版本
.venv/bin/python -m pip install -r requirements.txt

# 確認 Django、Gunicorn、MQTT 套件可載入
.venv/bin/python -m django --version
.venv/bin/python -m pip show gunicorn paho-mqtt
```

本專案已提供的 `requirements.txt` 內容如下：

```text
asgiref==3.12.1
Django==6.0.8
gunicorn==23.0.0
packaging==26.3
paho-mqtt==2.1.0
sqlparse==0.6.0
waitress==3.0.2
```

**替代方式**：專案另有 `install_pi.sh`，但目前尚未審閱其內容。確認該腳本沒有不適用於 Pi 4 的版本鎖定或路徑假設後，才可執行 `chmod +x install_pi.sh && ./install_pi.sh`。`INSTALL_AND_START_PI5.sh` 會依序執行 `install_pi.sh` 與 `start_pi.sh`，因此它是**手動測試啟動流程**，不是 systemd 安裝指令。

## 6. 設定 MQTT 與環境變數

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2
ls -l app.env app.env.example mqtt_config.ini mqtt_config.ini.save
```

- `mqtt_config.ini`：依專案實際格式填寫 MQTT Broker、連接埠、訂閱 Topic、裝置識別資訊等。
- `app.env`：依 `app.env.example` 及程式實際讀取的環境變數設定 Django 與遠端轉發參數。
- 如需連線到 Render 或其他外部服務，請確認遠端網址、驗證資訊與開關是否正確。
- **不得直接複製本手冊中的範例值作為正式服務設定**；本手冊沒有取得實際組態內容。

保護設定檔：

```bash
chmod 600 app.env mqtt_config.ini
```

`install_systemd.sh` 會用 `EnvironmentFile=-<專案路徑>/app.env` 載入環境變數。**systemd 的 EnvironmentFile 格式與 Shell 腳本的 `source` 不完全相同**：建議使用單純 `KEY=VALUE` 形式，避免在檔案中放入 `export KEY=...` 或 Shell 指令；若要使用 `start_pi.sh` 手動啟動，該檔案也必須能被 Bash `source` 正確解析。

## 7. Django 資料庫與檢查

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2

# 先檢查 Django 設定
.venv/bin/python manage.py check

# 確認 migration 狀態
.venv/bin/python manage.py showmigrations

# 只有在確定資料庫來源與備份策略後才執行 migration
.venv/bin/python manage.py migrate
```

如果沿用舊 `db.sqlite3`，請先備份並確認 migration 不會破壞既有資料。若要從零建立新資料庫，應另外保留舊資料庫檔案，並以 Django migration 建立結構。

## 8. 第一次手動測試（安裝 systemd 前）

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2
chmod +x start_pi.sh stop_pi.sh install_systemd.sh

# 啟動兩個背景程序，日誌寫入 runtime/
./start_pi.sh

# 查看監看程式及 MQTT 訂閱日誌
tail -n 50 runtime/django_error.log
tail -n 50 runtime/mqtt.log

# 確認本機 HTTP 回應
curl -I http://127.0.0.1:8000/

# 手動測試完成後，務必停止，避免與 systemd 重複執行
./stop_pi.sh
```

若 `curl -I` 不支援 HEAD 請求，可改用 `curl -v http://127.0.0.1:8000/`。請確認日誌顯示 MQTT 連線成功、接收到測試資料，並檢查儀表板是否顯示新紀錄。**未確認 Broker／Topic 及資料格式前，不要對正式主題發送測試訊息。**

## 9. 安裝開機自動啟動服務（正式部署）

目前的 `install_systemd.sh` 會建立：

| 服務名稱 | 執行內容 | 重新啟動 |
|---|---|---|
| `smart-water-dashboard.service` | Gunicorn `0.0.0.0:8000`、2 workers | `Restart=always`、5 秒 |
| `smart-water-mqtt.service` | `.venv/bin/python manage.py mqtt_subscriber` | `Restart=always`、5 秒 |

兩者都以**執行安裝腳本當下的使用者**身分執行，工作目錄為**腳本所在的專案路徑**。因此請以一般部署帳號執行 `./install_systemd.sh`，不要用 `sudo ./install_systemd.sh`（腳本內部會自行以 sudo 安裝服務檔）。

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2

# 確認沒有舊的手動啟動程序
./stop_pi.sh

# 安裝並立即啟動兩個 systemd 服務
./install_systemd.sh

# 確認兩個服務狀態
sudo systemctl status smart-water-dashboard.service --no-pager
sudo systemctl status smart-water-mqtt.service --no-pager

# 檢查開機自動啟動
systemctl is-enabled smart-water-dashboard.service
systemctl is-enabled smart-water-mqtt.service
```

`install_systemd.sh` 會建立 `/etc/systemd/system/` 下的服務設定，因此**不需要從舊樹莓派複製整個 `/etc/systemd/system`**。

## 10. 驗收與重新開機測試

```bash
# 查看 IP 與服務狀態
hostname -I
systemctl is-active smart-water-dashboard.service
systemctl is-active smart-water-mqtt.service

# 檢查 HTTP
curl -I http://127.0.0.1:8000/

# 查看 MQTT 最近 100 行日誌
journalctl -u smart-water-mqtt.service -n 100 --no-pager

# 查看 Django 最近 100 行日誌
journalctl -u smart-water-dashboard.service -n 100 --no-pager
```

同一個區域網路的電腦／手機開啟 `http://<樹莓派IP>:8000/`，確認儀表板可用。接著依專案的 MQTT 測試資料格式發送一筆測試訊息，核對 **MQTT 日誌 → SQLite 紀錄 → 儀表板 →（若有啟用）遠端轉發**，每個環節都應通過。測試資料格式尚需對照 `mqtt_subscriber.py` 或 `test_mqtt_250ml.sh`。

確認服務正常後，才執行：

```bash
sudo reboot
```

重新 SSH 登入，再次執行：

```bash
systemctl is-active smart-water-dashboard.service
systemctl is-active smart-water-mqtt.service
journalctl -u smart-water-mqtt.service -n 50 --no-pager
```

## 11. 日常管理

```bash
# 查看狀態
sudo systemctl status smart-water-dashboard.service smart-water-mqtt.service --no-pager

# 重新啟動服務
sudo systemctl restart smart-water-dashboard.service smart-water-mqtt.service

# 停止服務
sudo systemctl stop smart-water-dashboard.service smart-water-mqtt.service

# 啟動服務
sudo systemctl start smart-water-dashboard.service smart-water-mqtt.service

# 持續查看 MQTT 日誌
journalctl -u smart-water-mqtt.service -f

# 持續查看 Django 日誌
journalctl -u smart-water-dashboard.service -f
```

**重要**：`stop_pi.sh` 只會停止由 `start_pi.sh` 建立 PID 檔的程序，**不能用來停止 systemd 服務**。正式部署後請使用 `systemctl` 管理。

## 12. 更新部署

```bash
cd ~/smart_water_raspberry_pi5_monitor_v2

# 先停止服務，避免更新期間寫入資料
sudo systemctl stop smart-water-mqtt.service smart-water-dashboard.service

# 備份資料庫與設定；檔名含日期時間
BACKUP_DIR="$HOME/smart_water_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -a db.sqlite3 app.env mqtt_config.ini "$BACKUP_DIR/"

# 更新程式碼後（由部署者自行複製或 git pull）
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python manage.py check
.venv/bin/python manage.py migrate

# 重新啟動
sudo systemctl start smart-water-dashboard.service smart-water-mqtt.service
```

**提醒**：上面 `cp` 假設三個檔案均存在；若不存在，請先確認檔案路徑。更新程式碼時不要覆寫正式機器專屬的 `app.env`、`mqtt_config.ini`、`db.sqlite3`。SQLite 正在寫入時，直接 `cp` 不一定能取得一致快照；本流程已先停止服務。若有其他程序仍使用 SQLite，應使用 SQLite backup API 或 `.backup` 指令。

## 13. 常見故障排除

| 問題 | 檢查方式 | 處理方向 |
|---|---|---|
| `Django==6.0.8` 安裝失敗 | `python3 --version` | 確保 Python ≥ 3.12 |
| `No module named django` | `.venv/bin/python -m pip show Django` | 確認使用 `.venv` 並重裝依賴 |
| Port 8000 已被占用 | `sudo ss -ltnp '( sport = :8000 )'` | 避免手動程序與 systemd 同時執行 |
| MQTT 沒有資料 | `journalctl -u smart-water-mqtt -n 100` | 檢查 Broker、Topic、網路及登入資訊 |
| 儀表板無法連線 | `systemctl status smart-water-dashboard`、`hostname -I` | 檢查服務、IP、防火牆、區網 |
| `app.env` 載入失敗 | `journalctl -u smart-water-mqtt -n 100` | 檢查檔案格式、路徑與權限 |
| 開機後沒有自動啟動 | `systemctl is-enabled ...` | 重新執行 `install_systemd.sh` 或 `systemctl enable` |
| SQLite 權限錯誤 | `ls -l db.sqlite3; ls -ld .` | 確認部署帳號有寫入權限 |
| 重複收到／轉發訊息 | `ps aux | grep '[m]qtt_subscriber'` | 排查是否啟動多份訂閱程序 |

## 14. 交接驗收清單

- [ ] Raspberry Pi 4／5 已安裝 64 位元 OS，SSH 可連線。
- [ ] Python 版本 ≥ 3.12。
- [ ] 專案原始碼完整；未複製舊 `.venv`。
- [ ] `requirements.txt` 安裝成功。
- [ ] `app.env`、`mqtt_config.ini` 已填入新環境的正確資訊並妥善保護。
- [ ] `manage.py check` 與 `migrate` 成功。
- [ ] Django 儀表板可透過 `http://<IP>:8000/` 開啟。
- [ ] MQTT Subscriber 成功連線並收到實際測試資料。
- [ ] SQLite 新紀錄正確；若啟用遠端轉發，遠端亦已收到資料。
- [ ] `smart-water-dashboard.service` 與 `smart-water-mqtt.service` 均為 `active`、`enabled`。
- [ ] 重新開機後兩項服務自動恢復。
- [ ] 交接者已取得憑證保管方式、備份位置、維護聯絡資訊（不要把密碼寫進本文件）。

## 15. 尚待專案維護者確認

1. `install_pi.sh` 是否固定使用 Python 3.13 或有其他 OS 套件依賴。
2. `mqtt_subscriber.py` 真正讀取的組態鍵、Topic、Broker、資料格式與轉發條件。
3. `app.env.example` 是否完整涵蓋必要的環境變數。
4. `mqtt_config.ini` 是否含帳密、TLS 或其他特殊設定。
5. `test_mqtt_250ml.sh` 的測試資料是否會寫入正式資料庫／遠端系統。
6. 是否需另行設定 HTTPS、反向代理、VPN 或防火牆。Gunicorn 現在綁定 `0.0.0.0:8000`，**請勿未經保護直接暴露到公網**。

## 官方參考資料

- Raspberry Pi 文件：https://www.raspberrypi.com/documentation/
- Python venv：https://docs.python.org/3/library/venv.html
- pip requirements：https://pip.pypa.io/en/stable/reference/requirements-file-format/
- Django 6.0 安裝：https://docs.djangoproject.com/en/6.0/topics/install/
- systemd service：https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html
- OpenSSH scp：https://man.openbsd.org/scp.1
