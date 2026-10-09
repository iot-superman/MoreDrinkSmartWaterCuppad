# Google Maps SDK 找水喝 V2

來源分支：`Feture/googele_SDK`
V2 分支：`Feture/googele_SDK_v2`

## V2 變更
- `WaterDropMarkerFactory.kt`：原生 Canvas 水滴形狀、數字編號、水溫顏色與 NEW 線上點位。
- `NativeGoogleWaterMapScreen.kt`：在 Google Maps MapView 上增添 Compose 雷達波紋與掃描線，搜尋完成停止。
- 「GPS」改為「我的位置」，「重整」改為「重新整理」，「搜尋」改為「搜尋飲水點」。
- 搜尋完成狀態顯示「Hybrid 完成｜本地 + 線上新增 = 總數」。
- 上方控制介面調整圓角、陰影、間距與青綠色搜尋按鈕。
- 保留 310 筆本地水點、Overpass、地址搜尋、搜尋半徑、列表與外部導航。
- 不修改 `dev` 或來源分支，也不變更其他 Tab。

## 注意
- 此版本尚未經 Android Studio 編譯或真機驗證。
- Google Maps 和 HTML Leaflet 的底圖樣式不同，並非同一張底圖。
- V2 仍須在真機上檢查雷達動畫座標在縮放／拖曳地圖時的同步、點位縮放與下方地圖高度。
- 仍須進一步微調為 HTML 同等的滿版地圖浮動工具列、定位中心標記、動畫效能及搜尋體驗。
- Google Cloud 憑證維持原有 Gradle 私密設定，不要提交金鑰。

## 官方文件
https://developers.google.com/maps/documentation/android-sdk/marker
https://developer.android.com/develop/ui/compose/graphics/draw/overview
https://developer.android.com/develop/ui/compose/animation/quick-guide
