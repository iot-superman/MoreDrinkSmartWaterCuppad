package com.example.smartcoaster.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 原生 Google Maps 畫面：保留原網頁 310 筆飲水點與 Overpass 即時補充來源。 */
private data class WaterPlace(
    val key: String, val pos: LatLng, val name: String, val detail: String,
    val kind: String, val source: String, val meters: Int
)
private val defaultWaterCenter = LatLng(24.97756, 121.32252)
private fun parseWaterPlaces(array: JSONArray, center: LatLng, radius: Int, source: String): List<WaterPlace> {
    val result = ArrayList<WaterPlace>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val c = item.optJSONObject("center")
        val latitude = if (item.has("lat")) item.optDouble("lat") else c?.optDouble("lat") ?: Double.NaN
        val longitude = if (item.has("lon")) item.optDouble("lon") else c?.optDouble("lon") ?: Double.NaN
        if (!latitude.isFinite() || !longitude.isFinite()) continue
        val distance = FloatArray(1)
        android.location.Location.distanceBetween(center.latitude, center.longitude, latitude, longitude, distance)
        if (distance[0] > radius) continue
        val tags = item.optJSONObject("tags") ?: JSONObject()
        fun tag(vararg keys: String): String = keys.map { tags.optString(it) }.firstOrNull { it.isNotBlank() } ?: ""
        val kind = when {
            tag("hot_water").equals("yes", true) -> "熱水"
            tag("iced_warm").equals("yes", true) -> "冰溫"
            tag("warm_water").equals("yes", true) -> "溫水"
            else -> "一般"
        }
        result.add(WaterPlace(
            item.optString("type", "node") + "/" + item.optString("id", i.toString()),
            LatLng(latitude, longitude),
            tag("name", "name:zh", "name:zh-Hant").ifBlank { "OSM 飲水點" },
            tag("description", "description:zh", "description:zh-Hant"),
            kind, source, distance[0].toInt()
        ))
    }
    return result
}
/** Overpass 與原 HTML 相同：amenity=drinking_water 或 drinking_water=yes。 */
private suspend fun onlineWater(center: LatLng, radius: Int): List<WaterPlace> = withContext(Dispatchers.IO) {
    val query = "[out:json][timeout:60];(nwr(around:" + radius + "," +
        center.latitude + "," + center.longitude + ")[\"amenity\"=\"drinking_water\"];" +
        "nwr(around:" + radius + "," + center.latitude + "," + center.longitude +
        ")[\"drinking_water\"=\"yes\"];);out center tags;"
    val servers = listOf("https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter")
    var last: Exception? = null
    for (server in servers) {
        val connection = URL(server).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12000
            connection.readTimeout = 65000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.outputStream.use {
                it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray())
            }
            if (connection.responseCode !in 200..299) error("Overpass HTTP " + connection.responseCode)
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            return@withContext parseWaterPlaces(json.optJSONArray("elements") ?: JSONArray(), center, radius, "online")
        } catch (e: Exception) {
            last = e
        } finally {
            connection.disconnect()
        }
    }
    throw (last ?: IllegalStateException("Overpass 無法連線"))
}

@Composable
fun NativeGoogleWaterMapScreen(
    forceFreshSearch: Boolean = false,
    onForceFreshSearchConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var center by remember { mutableStateOf(defaultWaterCenter) }
    var radius by remember { mutableIntStateOf(2000) }
    var places by remember { mutableStateOf<List<WaterPlace>>(emptyList()) }
    var chosen by remember { mutableStateOf<WaterPlace?>(null) }
    var status by remember { mutableStateOf("正在初始化 Google 地圖") }
    var address by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var firstTime by remember { mutableStateOf(true) }
    var generation by remember { mutableIntStateOf(0) }
    var listVisible by remember { mutableStateOf(false) }

    fun drawMarkers() {
        val map = googleMap ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(center).title("搜尋中心：可拖曳")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(true))
        map.addCircle(CircleOptions().center(center).radius(radius.toDouble())
            .strokeColor(android.graphics.Color.BLUE).fillColor(0x0F1685E5))
        places.forEachIndexed { index, place ->
            val hue = when (place.kind) {
                "熱水", "溫水" -> BitmapDescriptorFactory.HUE_RED
                "冰溫" -> BitmapDescriptorFactory.HUE_ORANGE
                else -> if (place.source == "online") BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_CYAN
            }
            map.addMarker(MarkerOptions().position(place.pos)
                .title((index + 1).toString() + ". " + place.name)
                .snippet(place.kind + "｜" + place.meters + " 公尺｜" + place.detail)
                .icon(BitmapDescriptorFactory.defaultMarker(hue)))?.tag = place
        }
    }
    fun moveCenter(point: LatLng) {
        center = point
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 15f))
        status = "搜尋中心已變更，請按搜尋飲水點"
        drawMarkers()
    }
    fun searchWater() {
        val token = ++generation
        val searchCenter = center
        val searchRadius = radius
        searching = true
        scope.launch {
            val local = withContext(Dispatchers.IO) {
                val json = context.assets.open("water_points.json").bufferedReader().use { it.readText() }
                parseWaterPlaces(JSONArray(json), searchCenter, searchRadius, "local")
            }
            if (token != generation) return@launch
            places = local.sortedBy { it.meters }
            status = "內建 " + local.size + " 筆，正在搜尋線上飲水點"
            drawMarkers()
            try {
                val online = onlineWater(searchCenter, searchRadius)
                if (token != generation) return@launch
                val merged = LinkedHashMap<String, WaterPlace>()
                local.forEach { merged[it.key] = it }
                online.forEach { if (!merged.containsKey(it.key)) merged[it.key] = it }
                places = merged.values.sortedBy { it.meters }
                status = "內建 " + local.size + "＋線上新增 " + (merged.size - local.size) + "＝" + merged.size + " 點"
            } catch (e: Exception) {
                if (token != generation) return@launch
                status = "線上資料暫時無法取得，仍保留本地 " + local.size + " 點"
            } finally {
                if (token == generation) {
                    searching = false
                    drawMarkers()
                }
            }
        }
    }
    fun getGps() {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) { status = "請先允許定位權限"; return }
        @Suppress("MissingPermission")
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener {
                if (it != null) moveCenter(LatLng(it.latitude, it.longitude))
                searchWater()
            }
            .addOnFailureListener { status = "GPS 定位失敗，可點選地圖指定中心"; searchWater() }
    }
    val gpsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.any { granted -> granted }) getGps() else searchWater()
    }
    fun locate() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) getGps()
        else gpsPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    fun geocode() {
        if (address.isBlank()) { status = "請輸入地址"; return }
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) {
                    val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=tw&q=" +
                        URLEncoder.encode(address, "UTF-8")
                    val conn = URL(url).openConnection() as HttpURLConnection
                    try {
                        conn.setRequestProperty("User-Agent", "SmartCoaster/1.0 Android")
                        conn.connectTimeout = 12000
                        conn.readTimeout = 12000
                        JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                    } finally { conn.disconnect() }
                }
                if (found.length() == 0) status = "查無符合的地址"
                else {
                    val point = found.getJSONObject(0)
                    moveCenter(LatLng(point.getDouble("lat"), point.getDouble("lon")))
                }
            } catch (e: Exception) { status = "地址查詢失敗，請稍後重試" }
        }
    }

    DisposableEffect(mapView) {
        mapView.onCreate(Bundle())
        mapView.onStart()
        mapView.onResume()
        onDispose {
            generation++
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(googleMap) {
        if (googleMap != null && firstTime) {
            firstTime = false
            if (forceFreshSearch) onForceFreshSearchConsumed()
            locate()
        }
    }
    LaunchedEffect(center, radius, places, googleMap) { drawMarkers() }

    Column(Modifier.fillMaxSize()) {
        Surface(shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(address, { address = it }, label = { Text("輸入地址") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { geocode() }) { Text("找地址") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) { Text((radius / 1000f).toString() + " 公里") }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(500, 1000, 2000, 5000, 10000).forEach { r ->
                                DropdownMenuItem(text = { Text(r.toString() + " 公尺") }, onClick = {
                                    radius = r
                                    expanded = false
                                })
                            }
                        }
                    }
                    TextButton(onClick = { locate() }) { Text("GPS") }
                    TextButton(onClick = { googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15f)) }) {
                        Text("重整")
                    }
                    Button(onClick = {
                        if (searching) { generation++; searching = false; status = "搜尋已取消" }
                        else searchWater()
                    }) { Text(if (searching) "取消" else "搜尋") }
                }
                Text(status, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { listVisible = !listVisible }) {
                    Text("飲水點 " + places.size + " 個｜" + if (listVisible) "收合列表" else "顯示列表")
                }
            }
        }
        Box(Modifier.weight(1f)) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                mapView.apply {
                    getMapAsync { map ->
                        googleMap = map
                        map.uiSettings.isZoomControlsEnabled = true
                        map.setOnMapClickListener { moveCenter(it) }
                        map.setOnMarkerDragEndListener { moveCenter(it.position) }
                        map.setOnMarkerClickListener {
                            chosen = it.tag as? WaterPlace
                            false
                        }
                    }
                }
            })
            if (listVisible) {
                Surface(Modifier.fillMaxWidth().fillMaxHeight(0.6f).align(Alignment.BottomCenter),
                    shadowElevation = 10.dp) {
                    Column {
                        TextButton(onClick = { listVisible = false }) { Text("關閉列表") }
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(places.size) { index ->
                                val item = places[index]
                                TextButton(onClick = {
                                    chosen = item
                                    listVisible = false
                                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(item.pos, 17f))
                                }) { Text(item.name + "｜" + item.meters + " 公尺｜" + item.source) }
                            }
                        }
                    }
                }
            }
            val selected = chosen
            if (selected != null && !listVisible) {
                Surface(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp),
                    shadowElevation = 8.dp) {
                    Column(Modifier.padding(12.dp)) {
                        Text(selected.name, style = MaterialTheme.typography.titleMedium)
                        Text(selected.kind + "｜距中心 " + selected.meters + " 公尺｜" + selected.source)
                        if (selected.detail.isNotEmpty()) Text(selected.detail)
                        Row {
                            TextButton(onClick = { chosen = null }) { Text("關閉") }
                            Button(onClick = {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                                    selected.pos.latitude + "," + selected.pos.longitude + "&travelmode=walking")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) { Text("Google Maps 導航") }
                        }
                    }
                }
            }
        }
    }
}
