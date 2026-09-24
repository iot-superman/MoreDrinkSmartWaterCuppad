package com.example.smartcoaster.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * 判斷 WebView 正在開啟的網址是否屬於 Google Maps／地圖導航。
 *
 * 網頁目前產生的格式是：
 * https://www.google.com/maps/dir/?api=1&destination=緯度,經度&travelmode=walking
 * 同時支援 Android 常見的 geo: 與 google.navigation: URI，方便未來網頁改版。
 */
private fun isExternalMapNavigationUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return false

    if (scheme == "geo" || scheme == "google.navigation") {
        return true
    }

    if (scheme != "http" && scheme != "https") {
        return false
    }

    val host = uri.host?.lowercase() ?: return false
    val isGoogleHost = host == "google.com" || host.endsWith(".google.com")
    val isMapsPath = uri.path.orEmpty().startsWith("/maps")

    return isGoogleHost && isMapsPath
}

/**
 * 優先以 Google Maps App 開啟導航；若手機未安裝 Google Maps，則交給系統選擇
 * 其他地圖 App 或外部瀏覽器。無論成功與否都由 Android 消化這次點擊，避免導航
 * 網址覆蓋原本 WebView，造成返回 App 時只剩「飲水機地圖載入失敗」。
 */
private fun openExternalMapNavigation(context: Context, uri: Uri): Boolean {
    val googleMapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    val openedByGoogleMaps = runCatching {
        context.startActivity(googleMapsIntent)
    }.isSuccess

    if (openedByGoogleMaps) {
        return true
    }

    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    val openedByFallback = runCatching {
        context.startActivity(fallbackIntent)
    }.isSuccess

    if (!openedByFallback) {
        Toast.makeText(
            context,
            "找不到可開啟導航的地圖 App 或瀏覽器",
            Toast.LENGTH_LONG
        ).show()
    }

    // 即使沒有可處理的 App，也不要讓 Google Maps 網址取代飲水機 WebView。
    return true
}

/**
 * 第 2 個 Tab「找水喝」。
 *
 * 使用原生 WebView 內嵌附近飲水機 LBS 地圖，並針對手機操作做以下處理：
 * 1. 讓網頁依手機寬度排版，避免出現桌面版橫向捲動。
 * 2. 支援 JavaScript、DOM Storage 與 HTML5 定位。
 * 3. 網頁要求定位時，串接 Android 執行階段位置權限。
 * 4. 顯示載入中與錯誤畫面；正常狀態不覆蓋任何浮動操作按鈕。
 * 5. WebView 有上一頁時，系統返回鍵先回到網頁上一頁。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WaterMapScreen(
    forceFreshSearch: Boolean = false,
    onForceFreshSearchConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val mapUrl = "https://iot-superman.github.io/water_map/"

    // 網站的手機版原本會把搜尋結果清單排在整張地圖後面；WebView 若保留捲動位置，
    // 畫面就可能停在清單區，看起來像「地圖消失」。App 內只保留地圖模式：
    // 上方搜尋控制列 + 滿版地圖，搜尋結果仍會以 Marker 顯示在地圖上。
    val mobileMapOnlyScript = remember {
        """
        (function () {
            var style = document.getElementById('smart-coaster-map-only-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'smart-coaster-map-only-style';
                style.textContent = `
                    html, body {
                        width: 100% !important;
                        height: 100% !important;
                        margin: 0 !important;
                        padding: 0 !important;
                        overflow: hidden !important;
                        overscroll-behavior: none !important;
                    }

                    .wrap {
                        width: 100% !important;
                        height: 100% !important;
                        max-width: none !important;
                        margin: 0 !important;
                        padding: 0 !important;
                        overflow: hidden !important;
                    }

                    /* App 內不顯示網頁標題、狀態診斷、結果清單與除錯文字。 */
                    .wrap > h1,
                    .wrap > .sub,
                    .wrap > .status,
                    .panel,
                    .wrap > .hint,
                    .wrap > .debug {
                        display: none !important;
                    }

                    .grid {
                        display: block !important;
                        position: static !important;
                        width: 100% !important;
                        margin: 0 !important;
                        padding: 0 !important;
                    }

                    #map {
                        display: block !important;
                        /* 直接以 WebView 視窗為座標，不再依賴 .grid／.wrap 的百分比高度。 */
                        position: fixed !important;
                        inset: 0 !important;
                        z-index: 1 !important;
                        width: auto !important;
                        height: auto !important;
                        min-height: 0 !important;
                        max-height: none !important;
                        margin: 0 !important;
                        border: 0 !important;
                        border-radius: 0 !important;
                    }

                    /* 搜尋控制列固定於地圖頂端，效果與手機瀏覽器窄版畫面一致。 */
                    .wrap > .toolbar {
                        position: fixed !important;
                        z-index: 1200 !important;
                        left: 8px !important;
                        right: 8px !important;
                        top: calc(8px + env(safe-area-inset-top)) !important;
                        bottom: auto !important;
                        width: auto !important;
                        margin: 0 !important;
                        /* 候選清單屬於浮動內容，工具列本身不可裁切。 */
                        overflow: visible !important;
                        isolation: isolate !important;
                    }

                    /*
                     * 地址候選清單原本位於 .toolbar 內，Android 軟鍵盤開啟後會被
                     * 工具列第二排遮住。清單移到 body 後，改以輸入框位置固定定位。
                     */
                    #addressSuggestions.address-suggestions {
                        position: fixed !important;
                        z-index: 2147483647 !important;
                        top: var(--smart-address-suggestions-top, 64px) !important;
                        left: var(--smart-address-suggestions-left, 14px) !important;
                        right: var(--smart-address-suggestions-right, 14px) !important;
                        bottom: auto !important;
                        width: auto !important;
                        max-height: var(--smart-address-suggestions-max-height, 360px) !important;
                        box-sizing: border-box !important;
                        overflow-x: hidden !important;
                        overflow-y: auto !important;
                        overscroll-behavior: contain !important;
                        pointer-events: auto !important;
                        visibility: visible !important;
                        opacity: 1 !important;
                        transform: translateZ(0) !important;
                        -webkit-overflow-scrolling: touch !important;
                    }

                    /* hidden 屬性仍由原網站控制；一旦有結果就強制顯示成獨立浮層。 */
                    #addressSuggestions.address-suggestions:not([hidden]) {
                        display: block !important;
                    }

                    #addressSuggestions .address-candidate {
                        display: flex !important;
                        position: relative !important;
                        z-index: 1 !important;
                        width: 100% !important;
                        min-height: 42px !important;
                        visibility: visible !important;
                        opacity: 1 !important;
                    }

                    /*
                     * 鍵盤開啟時，工具列第二排會剛好落在候選清單後方。
                     * 候選清單展開期間先隱藏第二排，避免任何 WebView 合成層遮住候選項目；
                     * 選取地址或清單關閉後，原本控制項會立即恢復。
                     */
                    html.smart-address-menu-open .wrap > .toolbar > .info,
                    html.smart-address-menu-open .wrap > .toolbar > #radius,
                    html.smart-address-menu-open .wrap > .toolbar > #refresh,
                    html.smart-address-menu-open .wrap > .toolbar > #gps,
                    html.smart-address-menu-open .wrap > .toolbar > #search {
                        visibility: hidden !important;
                        pointer-events: none !important;
                    }
                `;
                document.head.appendChild(style);
            }

            var addressInput = document.getElementById('addressInput');
            var addressSuggestions = document.getElementById('addressSuggestions');

            function positionAddressSuggestions() {
                // 網頁日後若重建搜尋元件，必須取得新節點，不能沿用已失效的參照。
                addressInput = document.getElementById('addressInput');
                addressSuggestions = document.getElementById('addressSuggestions');
                if (!addressInput || !addressSuggestions) return;

                var inputRect = addressInput.getBoundingClientRect();
                // visualViewport 會反映 Android 軟鍵盤真正留下的可見區域。
                var viewport = window.visualViewport;
                var viewportTop = viewport ? viewport.offsetTop : 0;
                var viewportHeight = viewport
                    ? viewport.height
                    : document.documentElement.clientHeight;
                var viewportBottom = viewportTop + viewportHeight;
                var suggestionsTop = Math.max(
                    viewportTop + 8,
                    Math.round(inputRect.bottom + 6)
                );
                var availableHeight = Math.max(
                    120,
                    Math.round(viewportBottom - suggestionsTop - 8)
                );

                // 清單寬度對齊整個浮動工具列，在窄螢幕也保留 8px 安全邊界。
                var toolbar = addressInput.closest('.toolbar');
                var toolbarRect = toolbar ? toolbar.getBoundingClientRect() : inputRect;
                var viewportWidth = viewport
                    ? viewport.width
                    : document.documentElement.clientWidth;
                var suggestionsLeft = Math.max(8, Math.round(toolbarRect.left));
                var suggestionsRight = Math.max(
                    8,
                    Math.round(viewportWidth - toolbarRect.right)
                );

                document.documentElement.style.setProperty(
                    '--smart-address-suggestions-top',
                    suggestionsTop + 'px'
                );
                document.documentElement.style.setProperty(
                    '--smart-address-suggestions-left',
                    suggestionsLeft + 'px'
                );
                document.documentElement.style.setProperty(
                    '--smart-address-suggestions-right',
                    suggestionsRight + 'px'
                );
                document.documentElement.style.setProperty(
                    '--smart-address-suggestions-max-height',
                    Math.min(360, availableHeight) + 'px'
                );

                document.documentElement.classList.toggle(
                    'smart-address-menu-open',
                    !addressSuggestions.hidden && addressSuggestions.childElementCount > 0
                );
            }

            if (addressSuggestions && addressSuggestions.parentElement !== document.body) {
                // 保留網站建立的同一節點與點擊事件，只移出 toolbar 的堆疊／裁切範圍。
                document.body.appendChild(addressSuggestions);
            }

            if (addressInput && addressInput.dataset.smartCoasterSuggestionsBound !== 'true') {
                addressInput.dataset.smartCoasterSuggestionsBound = 'true';
                addressInput.addEventListener('focus', positionAddressSuggestions);
                addressInput.addEventListener('click', positionAddressSuggestions);
                addressInput.addEventListener('input', positionAddressSuggestions);
            }

            if (addressSuggestions && addressSuggestions.dataset.smartCoasterObserved !== 'true') {
                addressSuggestions.dataset.smartCoasterObserved = 'true';
                // 網站非同步填入候選資料時，再次校正高度與位置。
                new MutationObserver(positionAddressSuggestions).observe(addressSuggestions, {
                    attributes: true,
                    attributeFilter: ['hidden'],
                    childList: true,
                    subtree: true
                });
            }

            if (document.documentElement.dataset.smartCoasterViewportBound !== 'true') {
                document.documentElement.dataset.smartCoasterViewportBound = 'true';
                window.addEventListener('resize', positionAddressSuggestions);
                window.addEventListener('orientationchange', positionAddressSuggestions);
                if (window.visualViewport) {
                    window.visualViewport.addEventListener('resize', positionAddressSuggestions);
                    window.visualViewport.addEventListener('scroll', positionAddressSuggestions);
                }
            }

            positionAddressSuggestions();

            function restoreMapViewport() {
                window.scrollTo(0, 0);
                document.documentElement.scrollTop = 0;
                document.body.scrollTop = 0;
                window.dispatchEvent(new Event('resize'));
            }

            // 網頁、Leaflet 與定位搜尋完成的時間不同，分段重繪可避免地圖尺寸為 0。
            restoreMapViewport();
            setTimeout(restoreMapViewport, 100);
            setTimeout(restoreMapViewport, 500);
            setTimeout(restoreMapViewport, 1200);
        })();
        """.trimIndent()
    }

    // 網頁本身會把最近一次搜尋結果保存於 localStorage，五分鐘內重新載入時會直接
    // 還原快取並略過 GPS。App 新工作階段第一次進入地圖時，只刪除這一個搜尋快取，
    // 再重新載入網頁，讓網頁既有的「GPS → 2 公里搜尋」流程完整執行。
    val forceFreshSearchScript = remember {
        """
        (function () {
            var hadCachedSearch = false;
            try {
                hadCachedSearch = localStorage.getItem('water_map_v39_search_state') !== null;
                localStorage.removeItem('water_map_v39_search_state');
            } catch (error) {
                console.warn('SmartCoaster: 無法清除地圖搜尋快取', error);
            }

            // 有舊快取才需要 reload；首次安裝或快取已逾期時，網頁本身已會自動定位搜尋。
            if (hadCachedSearch) {
                window.location.reload();
            }
        })();
        """.trimIndent()
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMainFrameError by remember { mutableStateOf(false) }

    // 避免 reload 後的第二次 onPageFinished 再次 reload，形成無限循環。
    var hasConsumedForceFreshSearch by remember { mutableStateOf(false) }

    // WebView 的定位詢問需暫存，等 Android 權限對話框回傳後再答覆網頁。
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember {
        mutableStateOf<GeolocationPermissions.Callback?>(null)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
        pendingGeoOrigin = null
        pendingGeoCallback = null
    }

    // 僅當 WebView 內確實有瀏覽歷程時，攔截系統返回鍵。
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    webView = this

                    settings.apply {
                        // 地圖是互動式網頁，必須啟用 JavaScript 與瀏覽器端儲存空間。
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        // WebSettings 只有 setter，不能使用 Kotlin 屬性指定語法。
                        // 明確呼叫方法可避免「Val cannot be reassigned／無法解析屬性」編譯錯誤。
                        setGeolocationEnabled(true)

                        // 手機最佳化：套用 viewport，並讓內容自動符合 WebView 寬度。
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false

                        // 導航連結雖使用 target="_blank"，仍交由目前 WebViewClient 攔截，
                        // 再外拉 Google Maps；不在 WebView 內建立無法管理的新視窗。
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false

                        // 地圖只需網路內容，關閉本機檔案存取以縮小攻擊面。
                        allowFileAccess = false
                        allowContentAccess = false

                        // 在既有 User-Agent 後加入 App 標記，不破壞網站的手機裝置判斷。
                        userAgentString = "$userAgentString SmartCoaster-Android"
                    }

                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            hasMainFrameError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            // 固定為「滿版地圖模式」，避免 WebView 停留在下方結果清單。
                            view?.evaluateJavascript(mobileMapOnlyScript, null)

                            if (forceFreshSearch && !hasConsumedForceFreshSearch) {
                                // 先在原生端標記已執行，再要求網頁 reload；即使 reload 很快完成，
                                // 第二次 onPageFinished 也不會重複清快取或再次重新載入。
                                hasConsumedForceFreshSearch = true
                                onForceFreshSearchConsumed()
                                view?.evaluateJavascript(forceFreshSearchScript, null)
                            }

                            // 部分 Android WebView 會在 onPageFinished 後才完成動態工具列；
                            // 延後再注入兩次，腳本本身具冪等性，不會重複建立樣式或監聽器。
                            view?.postDelayed({
                                if (view.isAttachedToWindow) {
                                    view.evaluateJavascript(mobileMapOnlyScript, null)
                                }
                            }, 300L)
                            view?.postDelayed({
                                if (view.isAttachedToWindow) {
                                    view.evaluateJavascript(mobileMapOnlyScript, null)
                                }
                            }, 1_000L)
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            // 圖磚或圖示等子資源偶爾失敗，不應整頁改成錯誤畫面。
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasMainFrameError = true
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUri = request?.url ?: return false

                            // Google Maps 導航必須離開 WebView，由原生 Android Intent 開啟。
                            // 回到 App 時，原本的 Leaflet 飲水機地圖仍保持在原畫面。
                            if (isExternalMapNavigationUri(targetUri)) {
                                return openExternalMapNavigation(context, targetUri)
                            }

                            // HTTP(S) 頁面留在 WebView；電話、地圖導航等特殊協定交由手機 App。
                            if (targetUri.scheme == "http" || targetUri.scheme == "https") {
                                return false
                            }

                            return runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                true
                            }.getOrDefault(false)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            if (origin == null || callback == null) return

                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                callback.invoke(origin, true, false)
                            } else {
                                pendingGeoOrigin = origin
                                pendingGeoCallback = callback
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    }

                    loadUrl(mapUrl)
                }
            },
            update = { currentWebView ->
                // Compose 重新組合時保留同一個 WebView，不重複載入地圖。
                webView = currentWebView
            }
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (hasMainFrameError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text("飲水機地圖載入失敗", style = MaterialTheme.typography.titleMedium)
                Text(
                    "請確認網路連線後再試一次。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { webView?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("重新載入", modifier = Modifier.padding(start = 8.dp))
                }
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "開啟 App 設定")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 離開此 Tab 時釋放 WebView，避免 Activity 長時間持有網頁資源。
            pendingGeoCallback?.invoke(pendingGeoOrigin, false, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null

            webView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
            webView = null
        }
    }
}
