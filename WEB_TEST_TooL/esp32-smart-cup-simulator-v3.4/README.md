# ESP32 Smart Water Cup Pad Simulator v3.2

此版只修改 Simulator，不修改使用者已驗證成功的 Dashboard HTML。

## v3.2 主要修正

### 1. 修正「空秤待機 ⇄ 水杯模式」反覆跳
原 Arduino 的 `appManualDrinkMode == true` 時：
- ESP32 不啟動舊版自動水杯／藥盒狀態機。
- currentState 固定 STATE_IDLE。
- App 自己負責 startWeight / endWeight / CALC_WATER。

v3.1 雖然設了 manual=true，但穩定計數到門檻後仍呼叫舊版 settle()，
所以可能又把 EMPTY_IDLE 判成 CUP_SETTLED。v3.2 已禁止這件事。

### 2. getweight 完整相容 Arduino
Dashboard Step 3 / Step 5 都送：
`esp32/cmd -> getweight`

Simulator v3.2 回：
- `{TopicBase}/msg -> WEIGHT_ONCE:xx.xx`
- `{TopicBase}/serialraw -> WEIGHT_ONCE:xx.xx`

這與 Arduino `appPrint()` 行為一致，Dashboard 才能設定 pendingWeightTarget
並真正記錄 startWeight / endWeight。

### 3. TARE 手動飲水流程
方案二正確流程：

manualdrink
→ tare
→ getweight (START)
→ 拿杯 / 喝水 / 放回
→ getweight (END)
→ Dashboard 本機 CALC_WATER = START - END
→ legacyauto

例如：
- 杯子 + 水 630g
- tare 後 0.00g
- START = 0.00g
- 喝 50cc 放回
- END = -50.00g
- CALC_WATER = 0 - (-50) = 50cc

這正是 Arduino 原始註解「保留正負號；例如 -350.20 代表相對起始重量減少 350.20g」的設計。

### 4. SerialRAW Topic
完整每秒 RAW 行只發布到 `{TopicBase}/serialraw`（並保留舊版 `esp/msg/seialraw` 相容），
不再額外把完整 RAW 行送到 `{TopicBase}/msg`。

## 建議測試
1. 關閉實體 ESP32，避免公開 esp32/... Topic 有兩個 Publisher。
2. 只開 Dashboard + Simulator v3.2。
3. Simulator 確認 `REAL / MQTTGO`。
4. Dashboard 手動測試：
   Step 1 manualdrink
   Step 2 tare
   Step 3 getweight
5. START 應顯示約 0.0g，不再出現「尚未取得 startWeight」。
6. Simulator 拿杯 → 喝 50cc → 放回。
7. Dashboard Step 5 getweight，END 應約 -50.0g。
8. Step 6 CALC_WATER，結果應約 50.0cc。
9. Step 7 legacyauto，才重新啟用舊版自動 FSM。


## v3.3 UI
- 水杯支援滑鼠/觸控 Pointer Events 拖曳。
- 離開秤盤自動變成拿起；拖回藍色秤盤區自動吸附放回。
- 黃色拿取區、藍色放回區、狀態文字提供操作提示。
- 原本「拿起杯子／放回杯子」按鈕仍可用，並與拖曳位置同步。
- 不修改使用者 Dashboard HTML，也不改 v3.2 MQTT / manualdrink / tare / getweight 相容邏輯。


## v3.4 修正
- 移除「拿取區」：水杯只要拖出秤盤有效範圍，就是「已拿起」。
- 拖回秤盤有效範圍並放開，才是「放回」；會自動吸附秤中央。
- 修正 v3.3 REBOOT 仍顯示約 630g：
  - 原 Arduino 開機會執行 `scale.tare(30)`。
  - v3.4 REBOOT 時先把水杯回到秤上。
  - boot() 將當下「杯子＋水」設成 tareOffset。
  - 開機後 Weight = 0.00g，並立即 Publish 0.00 給 Dashboard。
- Dashboard HTML、MQTT Topic、manualdrink/getweight 流程不修改。
