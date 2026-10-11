# 多喝水沒事 Flutter 版（V0.1）

此目錄為原生 Android Jetpack Compose 專案的 **Flutter 移植工作區**。

## 目前已完成
- 建立獨立 Flutter 專案的 Dart 程式與 pubspec 設定
- 首頁與底部導覽 UI 骨架

## 尚未完成
- Kotlin 畫面 1:1 移植
- BLE GATT、Wi-Fi 配網、MQTT、飲水計算與校正
- 地圖、素材、測試、Android/iOS 實機驗證
- Flutter 自動產生的平台工程檔（android/、ios/）

## 在 Mac 上初始化
需要已安裝 Flutter SDK，於本目錄執行：

```bash
flutter create --platforms=android,ios --project-name smartcoaster_flutter .
flutter pub get
flutter run
```

> 注意：V0.1 僅為 UI 骨架，不會與 ESP32 通訊。請勿用於正式飲水數據記錄。

## 原始專案
`Android/smartcoaster_compose/`（本 Repository）

## 開發原則
- 不修改既有 Android、Arduino、Django 目錄
- BLE UUID、MQTT Topic 與 JSON 欄位必須以 Kotlin／韌體原始碼為準
- 後續以小型 PR 驗證各功能
