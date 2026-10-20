package com.yhan.gnsskoordinat

import android.Manifest
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
            databaseEnabled = true
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = true
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
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.__back ? window.__back() : '')") { r ->
                    if (r != "\"handled\"") finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
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

    private fun openExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show() }
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
    }
}
