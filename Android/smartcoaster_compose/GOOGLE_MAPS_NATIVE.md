# 找水喝：Google Maps SDK Android 原生分支

- 分支：`Feture/googele_SDK`（從 `dev` 建立）
- 新增：`NativeGoogleWaterMapScreen.kt`
- 改用：`com.google.android.gms:play-services-maps:19.2.0`
- 第 2 個 Tab 由 WebView 改為 MapView；其他 Tabs 不變。
- 原始 HTML 與 `WaterMapScreen.kt` 仍保留，不刪除舊版。

## Google Maps API Key 設定（不可提交到 Git）

1. 在 [Google Cloud Console](https://console.cloud.google.com/) 建立專案、啟用帳單與 **Maps SDK for Android**。
2. 建立 Android 限制的 API Key，設定 Android **套件名稱** `com.example.smartcoaster` 及本機簽章 SHA-1。正式上架需要另外加入 Play App Signing SHA-1。
3. 在開發機的 `~/.gradle/gradle.properties`（Windows：`%USERPROFILE%\.gradle\gradle.properties`）加入：
   ```properties
   MAPS_API_KEY=你的_Android_Maps_SDK_API_Key
   ```
4. 使用 Android Studio 開啟 `Android/smartcoaster_compose`，進行 Gradle Sync、Build、Run。
5. **切勿**把真實 API Key 寫入 Git、提交到遠端，或把未限制用途的金鑰上傳到版本庫。

## 資料與功能

- `app/src/main/assets/water_points.json`：從公開的 [water_map/index.html](https://github.com/iot-superman/water_map/blob/main/index.html) 擷取的 **310 筆**內建飲水點。
- 半徑：500／1000／2000／5000／10000 公尺。
- 先篩選內建資料，後向原有三個 Overpass 端點依序補充資料，按 `type/id` 去重。
- 地址搜尋：Nominatim（最多 6 筆候選、手動搜尋）、GPS、點選地圖、拖動搜尋中心、Marker 內容、結果列表、Google Maps 外部步行導航。
- Google Maps SDK 只繪製原生地圖，不使用付費的 Maps JavaScript API 及 Places API。
- **非完全一致功能**：舊 HTML 的打字即時候選、localStorage 5 分鐘快取、雷達特效、Marker 自訂圖示、回歸定位的完整生命週期，以及不同裝置畫面微調尚未完全重現。上線前需補完及在真機測試。

## 測試項目

1. 設定 Key，確認地圖正常顯示；未設定時會顯示 Google Maps 錯誤／空地圖。
2. 首次同意／拒絕位置權限，確認不閃退。
3. 在桃園搜尋 2 公里，確認內建點先顯示，Overpass 接著補充。
4. 切換半徑、選擇地址、拖動藍色搜尋中心、點地圖，再按搜尋。
5. 點任一 Marker，確認名稱、類型、來源、說明、距離與步行導航。
6. 切換 Tab、旋轉畫面，確認地圖及既有 BLE／MQTT 流程。
7. Google Maps SDK 的 Maps 資源載入須啟用帳單，地圖載入計價詳見官方說明。

## 官方參考

- https://developers.google.com/maps/documentation/android-sdk/overview
- https://developers.google.com/maps/documentation/android-sdk/usage-and-billing
- https://developers.google.com/maps/documentation/android-sdk/reference/com/google/android/libraries/maps/MapView
- https://wiki.openstreetmap.org/wiki/Overpass_API
