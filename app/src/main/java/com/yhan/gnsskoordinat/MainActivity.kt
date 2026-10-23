package com.yhan.gnsskoordinat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlin.concurrent.thread
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.webkit.WebViewAssetLoader
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * GNSS Koordinat Okuyucu - YHAN
 * Arayüz assets/index.html içindedir; bu sınıf telefonun yerel özelliklerini sayfaya açar:
 * tam konum izni, tek dokunuşla konumu açma penceresi, ham GPS okuması, uydu bilgisi,
 * İndirilenler'e kaydetme, paylaşma, pano ve ekranı açık tutma.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var lm: LocationManager
    private val main = Handler(Looper.getMainLooper())
    private var listening = false
    private var lastGpsFix = 0L
    private var lastGnssPush = 0L
    private var gnssCb: GnssStatus.Callback? = null
    private var waitingSettings = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { afterPermission() }

    private var afterBtPerm: (() -> Unit)? = null
    private val btPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasBtPerm()) afterBtPerm?.invoke()
        else {
            js("window.__btPerm && window.__btPerm(false)")
            js("window.__btList && window.__btList({ok:false,msg:'Bluetooth izni verilmedi. Uygulama izinlerinden Yakındaki cihazlar iznini açın.'})")
        }
        afterBtPerm = null
    }

    // Sayfadaki <input type=file> için dosya seçici (Aplikasyon: TXT / NCN)
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        val cb = fileCb; fileCb = null
        val d = r.data
        val list = mutableListOf<Uri>()
        if (r.resultCode == RESULT_OK && d != null) {
            val cd = d.clipData
            if (cd != null) for (n in 0 until cd.itemCount) cd.getItemAt(n).uri?.let { list.add(it) }
            else d.data?.let { list.add(it) }
        }
        cb?.onReceiveValue(if (list.isEmpty()) null else list.toTypedArray())
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { report(if (isLocationOn()) "ok" else "off") }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        setContentView(web)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            textZoom = 100
            databaseEnabled = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == "appassets.androidplatform.net") return false
                openExternal(u.toString())
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCb?.onReceiveValue(null)
                fileCb = cb
                val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    if (params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    fileLauncher.launch(Intent.createChooser(pick, "Dosya seç (TXT / NCN / DNS / KML / GeoJSON)"))
                    true
                } catch (e: Exception) {
                    fileCb = null
                    cb.onReceiveValue(null)
                    false
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        takeIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__back ? window.__back() : '')") { r ->
                    if (r != "\"handled\"") finish()
                }
            }
        })
    }

    // ---------- Paylaşılan metin (ör. TKGM Parsel Sorgu'dan "Paylaş") ----------
    private val sharedTexts = mutableListOf<String>()
    private fun takeIntent(i: Intent?) {
        if (i == null || (i.action != Intent.ACTION_SEND && i.action != Intent.ACTION_SEND_MULTIPLE)) return
        val tx = listOfNotNull(i.getStringExtra(Intent.EXTRA_SUBJECT), i.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()).joinToString("\n").trim()
        if (tx.isNotEmpty()) synchronized(sharedTexts) { sharedTexts.add(tx) }
        // Paylaşılan dosyalar (Parsel Sorgu'dan indirilen KML / GeoJSON vb.)
        val uris = mutableListOf<Uri>()
        @Suppress("DEPRECATION")
        if (i.action == Intent.ACTION_SEND) (i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uris.add(it) }
        else i.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
        i.clipData?.let { cd -> for (n in 0 until cd.itemCount) cd.getItemAt(n).uri?.let { if (!uris.contains(it)) uris.add(it) } }
        for (u in uris.take(30)) {
            try {
                contentResolver.openInputStream(u)?.use { st ->
                    val b = st.readBytes(); if (b.size < 8_000_000) synchronized(sharedTexts) { sharedTexts.add(String(b, Charsets.UTF_8)) }
                }
            } catch (e: Exception) { }
        }
        js("window.__shared && window.__shared()")
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        try { (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(5) } catch (e: Exception) { }
        PsHolder.data?.let { d -> PsHolder.data = null; js("window.__psParcels && window.__psParcels(" + JSONObject.quote(d) + ")") }
        if (waitingSettings) {
            waitingSettings = false
            report(currentState())
        }
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        stopUpdates()
        btWanted = false; closeBt()
        ntripWanted = false; closeNtrip()
        web.destroy()
        super.onDestroy()
    }

    // ---------- İzin ve konum durumu ----------
    private fun has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    private fun hasFine() = has(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun hasCoarse() = has(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun isLocationOn() = LocationManagerCompat.isLocationEnabled(lm)

    private fun currentState(): String = when {
        hasFine() && isLocationOn() -> "ok"
        hasFine() -> "off"
        hasCoarse() -> "approx"
        else -> "none"
    }

    private fun ensureLocation() {
        if (!hasFine()) {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        checkSettings()
    }

    private fun afterPermission() {
        when {
            hasFine() -> checkSettings()
            hasCoarse() -> report("approx")
            else -> report("denied")
        }
    }

    /** Konum kapalıysa Android'in "Konum açılsın mı?" penceresini gösterir (tek dokunuş). */
    private fun checkSettings() {
        if (isLocationOn()) { report("ok"); return }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        val settingsReq = LocationSettingsRequest.Builder().addLocationRequest(req).setAlwaysShow(true).build()
        LocationServices.getSettingsClient(this).checkLocationSettings(settingsReq)
            .addOnSuccessListener { report(if (isLocationOn()) "ok" else "off") }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        settingsLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
                    } catch (ex: Exception) {
                        openLocationSettings()
                    }
                } else {
                    openLocationSettings() // Google Play Hizmetleri olmayan telefonlar
                }
            }
    }

    private fun openLocationSettings() {
        waitingSettings = true
        try { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) } catch (e: Exception) { waitingSettings = false; report("off") }
    }

    private fun openAppSettings() {
        waitingSettings = true
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } catch (e: Exception) { waitingSettings = false }
    }

    private fun report(state: String) = js("window.__locState && window.__locState('$state')")

    private fun js(code: String) = main.post { web.evaluateJavascript(code, null) }

    // ---------- Konum okuma ----------
    private val listener = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            val now = System.currentTimeMillis()
            if (l.provider == LocationManager.GPS_PROVIDER) lastGpsFix = now
            else if (now - lastGpsFix < 5000) return // GPS varken ağ konumunu kullanma
            pushFix(l)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) js("window.__locErr && window.__locErr(2)")
        }
        @Deprecated("Eski Android sürümleri için gerekli")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @Suppress("DEPRECATION")
    private fun isMock(l: Location): Boolean = if (Build.VERSION.SDK_INT >= 31) l.isMock else l.isFromMockProvider

    @Suppress("DEPRECATION")
    private fun doVibrate(ms: Int) {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(ms.toLong())
        } catch (_: Exception) {}
    }

    private fun pushFix(l: Location) {
        val o = JSONObject()
        o.put("lat", l.latitude); o.put("lon", l.longitude)
        o.put("alt", if (l.hasAltitude()) l.altitude else JSONObject.NULL)
        o.put("acc", if (l.hasAccuracy()) l.accuracy.toDouble() else 99.0)
        o.put("vacc", if (Build.VERSION.SDK_INT >= 26 && l.hasVerticalAccuracy()) l.verticalAccuracyMeters.toDouble() else JSONObject.NULL)
        o.put("t", l.time)
        o.put("prov", l.provider ?: "")
        val mock = isMock(l)
        o.put("mock", mock)
        js("window.__fix && window.__fix($o)")
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        if (!hasFine()) { js("window.__locErr && window.__locErr(1)"); return }
        if (listening) return
        if (!isLocationOn()) js("window.__locErr && window.__locErr(2)")
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            if (lm.allProviders.contains(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, listener, Looper.getMainLooper())
            listening = true
        } catch (e: Exception) {
            js("window.__locErr && window.__locErr(2)")
            return
        }
        val cb = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val now = System.currentTimeMillis()
                if (now - lastGnssPush < 900) return
                lastGnssPush = now
                pushGnss(status)
            }
        }
        try { lm.registerGnssStatusCallback(cb, main); gnssCb = cb } catch (_: Exception) {}
    }

    private fun stopUpdates() {
        try { lm.removeUpdates(listener) } catch (_: Exception) {}
        gnssCb?.let { try { lm.unregisterGnssStatusCallback(it) } catch (_: Exception) {} }
        gnssCb = null
        listening = false
    }

    private fun pushGnss(s: GnssStatus) {
        var used = 0; var cnSum = 0.0; var l5 = 0
        val cons = HashMap<String, Int>()
        for (i in 0 until s.satelliteCount) {
            if (!s.usedInFix(i)) continue
            used++
            cnSum += s.getCn0DbHz(i)
            val name = when (s.getConstellationType(i)) {
                GnssStatus.CONSTELLATION_GPS -> "GPS"
                GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
                GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
                GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                else -> "DIGER"
            }
            cons[name] = (cons[name] ?: 0) + 1
            if (Build.VERSION.SDK_INT >= 26 && s.hasCarrierFrequencyHz(i)) {
                val f = s.getCarrierFrequencyHz(i)
                if (f > 1.15e9 && f < 1.21e9) l5++ // L5 / E5a / B2a
            }
        }
        val o = JSONObject()
        o.put("used", used); o.put("vis", s.satelliteCount)
        o.put("c", JSONObject(cons.toMap()))
        o.put("l5", l5)
        o.put("cn0", if (used > 0) cnSum / used else 0.0)
        js("window.__gnss && window.__gnss($o)")
    }

    // ---------- Dosya, paylaşım, diğer ----------
    private fun saveFile(name: String, text: String, mime: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/YHAN_GNSS")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return ""
                contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                "İndirilenler/YHAN_GNSS/$name"
            } else {
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "YHAN_GNSS").apply { mkdirs() }
                File(dir, name).writeText(text, Charsets.UTF_8)
                dir.absolutePath + "/" + name
            }
        } catch (e: Exception) { "" }
    }

    private fun shareFiles(json: String) {
        try {
            val arr = JSONArray(json)
            val dir = File(cacheDir, "paylas").apply { mkdirs() }
            val uris = ArrayList<Uri>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val f = File(dir, o.getString("name"))
                f.writeText(o.getString("text"), Charsets.UTF_8)
                uris.add(FileProvider.getUriForFile(this, "$packageName.files", f))
            }
            val intent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
                type = "text/plain"
                if (uris.size > 1) putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) else putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Koordinatları paylaş"))
        } catch (e: Exception) {
            Toast.makeText(this, "Paylaşılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    /** KML dosyasını Google Earth uygulamasında açar; yoksa KML açabilen başka bir uygulama seçtirir. */
    /** Başka uygulamaya geçerken: "Haritaya dön" bildirimi (geri tuşu çalışmazsa buradan dönülür). */
    private fun returnNote() {
        try {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 77); return
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(android.app.NotificationChannel("geri", "Uygulamaya dönüş", android.app.NotificationManager.IMPORTANCE_HIGH))
            val pi = android.app.PendingIntent.getActivity(this, 5, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
            @Suppress("DEPRECATION")
            val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(this, "geri") else android.app.Notification.Builder(this)
            b.setSmallIcon(android.R.drawable.ic_dialog_map).setContentTitle("GNSS BOZOKLUHAN GPS")
                .setContentText("Haritaya geri dönmek için dokunun").setContentIntent(pi).setAutoCancel(true)
            nm.notify(5, b.build())
        } catch (e: Exception) { }
    }

    private fun openPS(url: String) {
        try { startActivity(Intent(this, ParselActivity::class.java).putExtra("url", url)) } catch (e: Exception) { openExternal(url) }
    }

    private fun openKml(name: String, text: String) {
        returnNote()
        try {
            val dir = File(cacheDir, "paylas").apply { mkdirs() }
            val f = File(dir, name)
            f.writeText(text, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(this, "$packageName.files", f)
            val view = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.google-earth.kml+xml")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(Intent(view).setPackage("com.google.earth"))
            } catch (e: Exception) {
                try {
                    startActivity(Intent.createChooser(view, "KML dosyasını aç"))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Google Earth yüklü değil. Play Store'dan yükleyebilirsiniz.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "KML açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    /** Yalnız TKGM parsel servislerine (cbsapi, parselsorgu) GET isteği (WebView'in CORS kısıtını aşmak için). */
    private fun httpGet(id: String, url: String) {
        thread(name = "yhan-http") {
            var code = 0
            var body: String
            try {
                val u = java.net.URL(url)
                if (u.protocol != "https" || (u.host != "cbsapi.tkgm.gov.tr" && u.host != "parselsorgu.tkgm.gov.tr")) throw Exception("İzin verilmeyen adres")
                val c = u.openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 20000
                c.setRequestProperty("Accept", "application/json")
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) GNSS-BOZOKLUHAN")
                c.setRequestProperty("Referer", "https://parselsorgu.tkgm.gov.tr/")
                code = c.responseCode
                val s = if (code in 200..299) c.inputStream else c.errorStream
                body = s?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                c.disconnect()
            } catch (e: Exception) {
                code = 0
                body = e.message ?: "İnternet bağlantısı yok"
            }
            val o = JSONObject(); o.put("id", id); o.put("status", code); o.put("body", body)
            js("window.__http && window.__http($o)")
        }
    }

    // ---------- LİSANS (çevrimdışı): cihaz kodu + HMAC anahtar, deneme başlangıcı ----------
    private val licPrefs by lazy { getSharedPreferences("yhan_lic", Context.MODE_PRIVATE) }
    private fun hx(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun licSecret(): ByteArray {
        val a = hx("85047cab429c91841a17b1ad0b1f3633a58aac51b33df30e41cbe105a7abba03")
        val b = hx("cb1c85a3218305ff9621a9c750c0780fba9ccd08a0706d4dc9066c5d4a636319")
        return ByteArray(32) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }
    private fun toHex(h: ByteArray) = h.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    @SuppressLint("HardwareIds")
    private fun deviceCode(): String {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "0"
        val hex = toHex(java.security.MessageDigest.getInstance("SHA-256").digest(("GNSS-BZK|$id").toByteArray(Charsets.UTF_8)))
        return hex.substring(0, 4) + "-" + hex.substring(4, 8)
    }
    private fun licKeyFor(dev: String): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(licSecret(), "HmacSHA256"))
        return toHex(mac.doFinal(("BZK1|$dev").toByteArray(Charsets.UTF_8))).substring(0, 12)
    }
    private fun licInfo(): String {
        val now = System.currentTimeMillis()
        var start = licPrefs.getLong("start", 0L)
        if (start == 0L) { start = now; licPrefs.edit().putLong("start", now).apply() }
        val lastSeen = licPrefs.getLong("last", 0L)
        val tamper = lastSeen > 0L && now < lastSeen - 86_400_000L      // saat 1 günden fazla geri alınmış
        if (now > lastSeen) licPrefs.edit().putLong("last", now).apply()
        val dev = deviceCode()
        val key = licPrefs.getString("key", "") ?: ""
        val o = JSONObject()
        o.put("device", dev); o.put("start", start); o.put("now", now); o.put("tamper", tamper)
        o.put("licensed", key.isNotEmpty() && key == licKeyFor(dev))
        return o.toString()
    }
    private fun licActivate(input: String): String {
        val k = input.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        return if (k.length == 12 && k == licKeyFor(deviceCode())) { licPrefs.edit().putString("key", k).apply(); "ok" } else "bad"
    }

    /** Uygulama yüklüyse onu açar ("app"), değilse adresi tarayıcıda açar ("web"). Geri tuşu bu uygulamaya döner. */
    private fun openAppOr(pkg: String, url: String): String {
        return try {
            val li = packageManager.getLaunchIntentForPackage(pkg)
            if (li != null) { returnNote(); startActivity(li); "app" } else { openExternal(url); "web" }
        } catch (e: Exception) { openExternal(url); "web" }
    }

    private fun openExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show() }
    }

    // ---------- RTK ALICI: Bluetooth (SPP) ----------
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    @Volatile private var btWanted = false
    @Volatile private var btGen = 0
    @Volatile private var btSocket: BluetoothSocket? = null
    @Volatile private var btOut: OutputStream? = null
    @Volatile private var lastGga: String? = null
    private val nmeaQueue = ArrayList<String>()
    private val outLock = Any()

    private fun hasBtPerm(): Boolean =
        if (Build.VERSION.SDK_INT >= 31) has(Manifest.permission.BLUETOOTH_CONNECT) else true

    private fun withBtPerm(action: () -> Unit) {
        if (hasBtPerm()) { action(); return }
        afterBtPerm = action
        btPermLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
    }

    private fun btAdapter(): BluetoothAdapter? =
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @SuppressLint("MissingPermission")
    private fun btList() = withBtPerm {
        val a = btAdapter()
        val r = JSONObject()
        when {
            a == null -> { r.put("ok", false); r.put("msg", "Bu telefonda Bluetooth yok.") }
            !a.isEnabled -> {
                r.put("ok", false); r.put("msg", "Bluetooth kapalı. Açılması için izin istendi; açtıktan sonra tekrar deneyin.")
                try { startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) } catch (_: Exception) {}
            }
            else -> {
                val arr = JSONArray()
                for (d in a.bondedDevices) {
                    val o = JSONObject(); o.put("name", d.name ?: ""); o.put("addr", d.address); arr.put(o)
                }
                r.put("ok", true); r.put("list", arr)
            }
        }
        js("window.__btList && window.__btList($r)")
    }

    private fun btState(state: String, name: String = "", msg: String = "") {
        val o = JSONObject(); o.put("state", state); o.put("name", name); o.put("msg", msg)
        js("window.__btState && window.__btState($o)")
    }

    private fun btConnect(addr: String, name: String) = withBtPerm {
        val g = ++btGen          // eski bağlantı döngüsü bu numarayı görünce kendiliğinden biter
        closeBt()
        btWanted = true
        thread(name = "yhan-bt") { btLoop(addr, name, g) }
    }

    private fun btDisconnect() {
        btWanted = false; btGen++; closeBt()
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(dev: BluetoothDevice): BluetoothSocket {
        val tries = listOf<() -> BluetoothSocket>(
            { dev.createRfcommSocketToServiceRecord(sppUuid) },
            { dev.createInsecureRfcommSocketToServiceRecord(sppUuid) },
            { dev.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType).invoke(dev, 1) as BluetoothSocket }
        )
        var last: Exception? = null
        for (mk in tries) {
            try { val s = mk(); s.connect(); return s } catch (e: Exception) { last = e }
        }
        throw last ?: Exception("Bağlanılamadı")
    }

    @SuppressLint("MissingPermission")
    private fun btLoop(addr: String, name: String, g: Int) {
        val alive = { btWanted && g == btGen }
        var first = true
        while (alive()) {
            btState(if (first) "connecting" else "reconnect", name)
            first = false
            var s: BluetoothSocket? = null
            try {
                val a = btAdapter() ?: throw Exception("Bluetooth yok")
                if (!a.isEnabled) throw Exception("Bluetooth kapalı")
                try { a.cancelDiscovery() } catch (_: Exception) {}
                s = openSocket(a.getRemoteDevice(addr))
                if (!alive()) break
                btSocket = s
                btOut = s.outputStream
                btState("on", name)
                readNmea(s.inputStream, alive)
            } catch (e: Exception) {
                if (alive()) btState("reconnect", name, e.message ?: "")
            } finally {
                try { s?.close() } catch (_: Exception) {}
                if (btSocket === s) { btSocket = null; btOut = null }
            }
            if (alive()) try { Thread.sleep(3000) } catch (_: InterruptedException) {}
        }
        if (g == btGen) btState("off", name)
    }

    private fun readNmea(inp: InputStream, alive: () -> Boolean) {
        val buf = ByteArray(4096)
        val line = StringBuilder()
        var lastFlush = System.currentTimeMillis()
        while (alive()) {
            val n = inp.read(buf)
            if (n < 0) throw Exception("Bağlantı kapandı")
            for (i in 0 until n) {
                val c = (buf[i].toInt() and 0xFF).toChar()
                if (c == '\n' || c == '\r') {
                    if (line.isNotEmpty()) {
                        val s = line.toString()
                        val k = s.indexOf('$')
                        if (k >= 0) {
                            val clean = s.substring(k)
                            if (clean.length > 6 && clean.substring(3, 6) == "GGA") lastGga = clean
                            synchronized(nmeaQueue) { if (nmeaQueue.size < 200) nmeaQueue.add(clean) }
                        }
                        line.setLength(0)
                    }
                } else if (line.length < 200) line.append(c)
            }
            val now = System.currentTimeMillis()
            if (now - lastFlush > 200) { lastFlush = now; flushNmea() }
        }
    }

    private fun flushNmea() {
        val copy: List<String>
        synchronized(nmeaQueue) { if (nmeaQueue.isEmpty()) return; copy = ArrayList(nmeaQueue); nmeaQueue.clear() }
        js("window.__nmea && window.__nmea(${JSONArray(copy)})")
    }

    private fun closeBt() {
        try { btSocket?.close() } catch (_: Exception) {}
        btSocket = null; btOut = null
    }

    private fun btOpenSettings() {
        try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } catch (_: Exception) {}
    }

    // ---------- NTRIP istemcisi (TUSAGA-Aktif vb.) ----------
    @Volatile private var ntripWanted = false
    @Volatile private var ntripGen = 0
    @Volatile private var ntripSock: Socket? = null
    @Volatile private var ntripBytes = 0L

    private fun ntState(state: String, msg: String = "") {
        val o = JSONObject(); o.put("state", state); o.put("msg", msg); o.put("bytes", ntripBytes)
        js("window.__ntripState && window.__ntripState($o)")
    }

    private fun readLine(inp: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = inp.read()
            if (b < 0) break
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
            if (sb.length > 2000) break
        }
        return sb.toString()
    }

    private fun ntripTable(host: String, port: Int) {
        thread(name = "yhan-ntrip-table") {
            val r = JSONObject()
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 10000)
                    s.soTimeout = 15000
                    val req = "GET / HTTP/1.0\r\nUser-Agent: NTRIP YHAN-GNSS/1.0\r\nAccept: */*\r\nConnection: close\r\n\r\n"
                    s.getOutputStream().write(req.toByteArray(Charsets.ISO_8859_1))
                    val inp = s.getInputStream()
                    val arr = JSONArray()
                    var lines = 0
                    while (lines < 5000) {
                        val l = readLine(inp)
                        lines++
                        if (l.startsWith("ENDSOURCETABLE")) break
                        if (l.startsWith("STR;")) {
                            val f = l.split(";")
                            val o = JSONObject()
                            o.put("mount", f.getOrElse(1) { "" }); o.put("id", f.getOrElse(2) { "" })
                            o.put("fmt", f.getOrElse(3) { "" }); o.put("nav", f.getOrElse(6) { "" })
                            o.put("lat", f.getOrElse(9) { "" }.toDoubleOrNull() ?: 0.0)
                            o.put("lon", f.getOrElse(10) { "" }.toDoubleOrNull() ?: 0.0)
                            o.put("nmea", f.getOrElse(11) { "0" } == "1")
                            arr.put(o)
                        }
                        if (l.isEmpty() && inp.available() == 0 && lines > 50) { /* boş satırlar */ }
                    }
                    r.put("ok", arr.length() > 0); r.put("list", arr)
                    if (arr.length() == 0) r.put("msg", "Sunucu bağlantı noktası listesi vermedi. Adresi ve portu kontrol edin.")
                }
            } catch (e: Exception) {
                r.put("ok", false); r.put("list", JSONArray()); r.put("msg", "Sunucuya ulaşılamadı: " + (e.message ?: ""))
            }
            js("window.__ntripTable && window.__ntripTable($r)")
        }
    }

    private fun ntripConnect(host: String, port: Int, mount: String, user: String, pass: String) {
        val g = ++ntripGen
        closeNtrip()
        ntripWanted = true; ntripBytes = 0
        thread(name = "yhan-ntrip") {
            val alive = { ntripWanted && g == ntripGen }
            var first = true
            while (alive()) {
                ntState(if (first) "connecting" else "reconnect")
                first = false
                var s: Socket? = null
                try {
                    s = Socket()
                    s.connect(InetSocketAddress(host, port), 10000)
                    s.soTimeout = 30000
                    ntripSock = s
                    val out = s.getOutputStream()
                    val auth = Base64.encodeToString("$user:$pass".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val req = "GET /$mount HTTP/1.0\r\nUser-Agent: NTRIP YHAN-GNSS/1.0\r\nAccept: */*\r\nAuthorization: Basic $auth\r\nConnection: close\r\n\r\n"
                    out.write(req.toByteArray(Charsets.ISO_8859_1))
                    lastGga?.let { out.write((it + "\r\n").toByteArray(Charsets.ISO_8859_1)) }
                    out.flush()
                    val inp = s.getInputStream()
                    val status = readLine(inp)
                    when {
                        status.contains("401") -> { ntripWanted = false; throw Exception("Kullanıcı adı veya şifre hatalı") }
                        status.startsWith("SOURCETABLE") -> { ntripWanted = false; throw Exception("Bağlantı noktası bulunamadı: $mount") }
                        status.startsWith("ICY 200") -> {}
                        status.startsWith("HTTP/") && status.contains(" 200") -> {
                            while (true) { val h = readLine(inp); if (h.isEmpty()) break }
                        }
                        else -> throw Exception("Sunucu yanıtı: ${status.take(60)}")
                    }
                    ntState("on")
                    val buf = ByteArray(4096)
                    var lastGgaSent = System.currentTimeMillis()
                    var lastReport = 0L
                    while (alive()) {
                        val n = inp.read(buf)
                        if (n < 0) throw Exception("Sunucu bağlantıyı kapattı")
                        ntripBytes += n
                        val o = btOut
                        if (o != null) synchronized(outLock) { try { o.write(buf, 0, n); o.flush() } catch (_: Exception) {} }
                        val now = System.currentTimeMillis()
                        if (now - lastGgaSent > 10000) {
                            lastGgaSent = now
                            lastGga?.let { try { out.write((it + "\r\n").toByteArray(Charsets.ISO_8859_1)); out.flush() } catch (_: Exception) {} }
                        }
                        if (now - lastReport > 1000) { lastReport = now; ntState("on") }
                    }
                } catch (e: Exception) {
                    if (alive()) ntState("reconnect", e.message ?: "")
                    else if (g == ntripGen && e.message?.let { it.contains("hatalı") || it.contains("bulunamadı") } == true) ntState("error", e.message ?: "")
                } finally {
                    try { s?.close() } catch (_: Exception) {}
                    if (ntripSock === s) ntripSock = null
                }
                if (alive()) try { Thread.sleep(5000) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun ntripDisconnect() {
        ntripWanted = false; ntripGen++
        closeNtrip()
        ntState("off")
    }

    private fun closeNtrip() {
        try { ntripSock?.close() } catch (_: Exception) {}
        ntripSock = null
    }

    /** Sayfanın çağırdığı köprü: window.Android.* */
    inner class Bridge {
        @JavascriptInterface fun ensureLocation() { main.post { this@MainActivity.ensureLocation() } }
        @JavascriptInterface fun checkState(): String = currentState()
        @JavascriptInterface fun startUpdates() { main.post { this@MainActivity.startUpdates() } }
        @JavascriptInterface fun stopUpdates() { main.post { this@MainActivity.stopUpdates() } }
        @JavascriptInterface fun openLocationSettings() { main.post { this@MainActivity.openLocationSettings() } }
        @JavascriptInterface fun openAppSettings() { main.post { this@MainActivity.openAppSettings() } }
        @JavascriptInterface fun saveFile(name: String, text: String, mime: String): String = this@MainActivity.saveFile(name, text, mime)
        @JavascriptInterface fun shareFiles(json: String) { main.post { this@MainActivity.shareFiles(json) } }
        @JavascriptInterface fun openUrl(url: String) { main.post { openExternal(url) } }
        @JavascriptInterface fun openPS(url: String) { main.post { this@MainActivity.openPS(url) } }
        @JavascriptInterface fun openAppOr(pkg: String, url: String): String { val l = packageManager.getLaunchIntentForPackage(pkg) != null; main.post { this@MainActivity.openAppOr(pkg, url) }; return if (l) "app" else "web" }
        @JavascriptInterface fun openKml(name: String, text: String) { main.post { this@MainActivity.openKml(name, text) } }
        @JavascriptInterface fun takeShared(): String = synchronized(sharedTexts) { val a = JSONArray(sharedTexts); sharedTexts.clear(); a.toString() }
        @JavascriptInterface fun copy(text: String) {
            main.post {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("koordinat", text))
            }
        }
        @JavascriptInterface fun keepScreen(on: Boolean) {
            main.post {
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        @JavascriptInterface fun vibrate(ms: Int) { main.post { doVibrate(ms) } }
        @JavascriptInterface fun licInfo(): String = this@MainActivity.licInfo()
        @JavascriptInterface fun licActivate(key: String): String = this@MainActivity.licActivate(key)
        @JavascriptInterface fun httpGet(id: String, url: String) { this@MainActivity.httpGet(id, url) }
        @JavascriptInterface fun btPermState(): String = if (hasBtPerm()) "ok" else "no"
        @JavascriptInterface fun btRequestPerm() { main.post { withBtPerm { js("window.__btPerm && window.__btPerm(true)") } } }
        @JavascriptInterface fun btList() { main.post { this@MainActivity.btList() } }
        @JavascriptInterface fun btConnect(addr: String, name: String) { main.post { this@MainActivity.btConnect(addr, name) } }
        @JavascriptInterface fun btDisconnect() { this@MainActivity.btDisconnect() }
        @JavascriptInterface fun btOpenSettings() { main.post { this@MainActivity.btOpenSettings() } }
        @JavascriptInterface fun ntripTable(host: String, port: Int) { this@MainActivity.ntripTable(host, port) }
        @JavascriptInterface fun ntripConnect(host: String, port: Int, mount: String, user: String, pass: String) {
            this@MainActivity.ntripConnect(host, port, mount, user, pass)
        }
        @JavascriptInterface fun ntripDisconnect() { this@MainActivity.ntripDisconnect() }
    }
}
