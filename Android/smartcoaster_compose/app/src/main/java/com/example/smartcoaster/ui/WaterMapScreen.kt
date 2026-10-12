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
 */
fun isExternalMapNavigationUri(uri: Uri): Boolean {
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
 * 優先以 Google Maps App 開啟導航。
 */
fun openExternalMapNavigation(context: Context, uri: Uri): Boolean {
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

    return true
}

/**
 * 第 2 個 Tab「找水喝」。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WaterMapScreen(
    forceFreshSearch: Boolean = false,
    onForceFreshSearchConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val mapUrl = "https://iot-superman.github.io/water_map/"

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

                    .wrap > .toolbar {
                        position: fixed !important;
                        z-index: 1200 !important;
                        left: 8px !important;
                        right: 8px !important;
                        top: calc(8px + env(safe-area-inset-top)) !important;
                        bottom: auto !important;
                        width: auto !important;
                        margin: 0 !important;
                        overflow: visible !important;
                        isolation: isolate !important;
                    }

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
                addressInput = document.getElementById('addressInput');
                addressSuggestions = document.getElementById('addressSuggestions');
                if (!addressInput || !addressSuggestions) return;

                var inputRect = addressInput.getBoundingClientRect();
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

            restoreMapViewport();
            setTimeout(restoreMapViewport, 100);
            setTimeout(restoreMapViewport, 500);
            setTimeout(restoreMapViewport, 1200);
        })();
        """.trimIndent()
    }

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

            if (hadCachedSearch) {
                window.location.reload();
            }
        })();
        """.trimIndent()
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMainFrameError by remember { mutableStateOf(false) }

    var hasConsumedForceFreshSearch by remember { mutableStateOf(false) }

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
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setGeolocationEnabled(true)

                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false

                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false

                        allowFileAccess = false
                        allowContentAccess = false

                        val defaultUa = userAgentString.orEmpty()
                        userAgentString = "$defaultUa SmartCoaster-Android"
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
                            view?.evaluateJavascript(mobileMapOnlyScript, null)

                            if (forceFreshSearch && !hasConsumedForceFreshSearch) {
                                hasConsumedForceFreshSearch = true
                                onForceFreshSearchConsumed()
                                view?.evaluateJavascript(forceFreshSearchScript, null)
                            }

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

                            if (isExternalMapNavigationUri(targetUri)) {
                                return openExternalMapNavigation(context, targetUri)
                            }

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
            pendingGeoCallback?.invoke(pendingGeoOrigin, false, false)
            pendingGeoCallback = null
            pendingGeoOrigin = null

            val targetWebView = webView
            webView = null
            targetWebView?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
        }
    }
}