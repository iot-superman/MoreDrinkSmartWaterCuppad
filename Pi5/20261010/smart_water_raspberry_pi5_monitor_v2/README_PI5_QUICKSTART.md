# 智慧喝水儀表板－Raspberry Pi 5 快速測試

## 1. 解壓縮

假設 ZIP 位於 Raspberry Pi 的 `Downloads`：

```bash
cd ~
unzip ~/Downloads/smart_water_raspberry_pi5_monitor_v2.zip
cd ~/smart_water_raspberry_pi5_monitor_v2
```

如果沒有 `unzip`：

```bash
sudo apt update
sudo apt install -y unzip python3 python3-venv
```

## 2. 第一次安裝並啟動

```bash
chmod +x INSTALL_AND_START_PI5.sh
./INSTALL_AND_START_PI5.sh
```

啟動後終端機會顯示網址，例如：

```text
本機網址：http://127.0.0.1:8000/
區網網址：http://192.168.1.50:8000/
```

同一個 Wi-Fi 的電腦或手機可開啟區網網址。

## 3. MQTT 測試

預設設定：

```text
Broker：broker.emqx.io
Port：1883
Topic：esp32311（esp32＋UID 311）
UID：311
```

發布一筆 250 ml 測試資料：

```bash
./test_mqtt_250ml.sh
```

回到儀表板按 F5，應看到 UID 311、250 ml 的喝水紀錄。

查看 MQTT 日誌：

```bash
tail -f runtime/mqtt.log
```

## 4. 同步到 Render.com PostgreSQL

本版預設只寫本機 SQLite。要同步 Render，先建立 `app.env`：

```bash
cp app.env.example app.env
nano app.env
```

將內容中的 `MQTT_API_KEY` 改成與 Render Web Service → Environment 裡完全相同的值：

```ini
DJANGO_DEBUG=true
DJANGO_ALLOWED_HOSTS=127.0.0.1,localhost,0.0.0.0,*
DJANGO_SECRET_KEY=請改成一串至少50字元的隨機內容
MQTT_API_KEY=請填入與Render完全相同的API金鑰
```

接著編輯：

```bash
nano mqtt_config.ini
```

將 `[render]` 改為：

```ini
[render]
enabled = true
api_url = https://django-water-cuppad-dashboard.onrender.com/api/drinks/mqtt/
api_key_env = MQTT_API_KEY
timeout_seconds = 90
```

重新啟動：

```bash
./stop_pi.sh
./start_pi.sh
```

成功時 `runtime/mqtt.log` 會顯示 `Render 同步成功`。

## 5. 安裝成開機常駐服務

確認一般測試成功後執行：

```bash
sudo ./install_systemd.sh
```

查詢狀態：

```bash
sudo systemctl status smart-water-dashboard
sudo systemctl status smart-water-mqtt
```

即時查看 MQTT／Render 同步日誌：

```bash
journalctl -u smart-water-mqtt -f
```

停止服務：

```bash
sudo systemctl stop smart-water-dashboard smart-water-mqtt
```

重新啟動服務：

```bash
sudo systemctl restart smart-water-dashboard smart-water-mqtt
```

## 6. V2 注意事項

- MQTT 採事件驅動，收到訊息才新增資料，不是每秒輪詢。
- Render API 失敗時，本機 SQLite 紀錄仍會保存。
- V2 尚未自動補傳 Render 失敗紀錄。
- `app.env` 含 API Key，不要上傳到 GitHub。
