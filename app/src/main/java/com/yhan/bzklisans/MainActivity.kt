package com.yhan.bzklisans

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

/**
 * BZK Lisans Üretici - Yasin HAN
 * GNSS BOZOKLUHAN GPS için cihaz koduna özel lisans anahtarı üretir.
 * Anahtar: HMAC-SHA256(gizli, "BZK1|" + cihazKodu) ilk 12 onaltılık hane (GNSS uygulamasıyla aynı).
 */
class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val main = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ekran görüntüsü ve son uygulamalar önizlemesi kapalı (anahtarlar görünmesin)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private fun hx(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun secret(): ByteArray {
        val a = hx("85047cab429c91841a17b1ad0b1f3633a58aac51b33df30e41cbe105a7abba03")
        val b = hx("cb1c85a3218305ff9621a9c750c0780fba9ccd08a0706d4dc9066c5d4a636319")
        return ByteArray(32) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }
    private fun toHex(h: ByteArray) = h.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    private fun keyFor(dev: String): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret(), "HmacSHA256"))
        val k = toHex(mac.doFinal(("BZK1|$dev").toByteArray(Charsets.UTF_8))).substring(0, 12)
        return k.substring(0, 4) + "-" + k.substring(4, 8) + "-" + k.substring(8, 12)
    }

    inner class Bridge {
        @JavascriptInterface fun key(dev: String): String = keyFor(dev)
        @JavascriptInterface fun copy(text: String) {
            main.post {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("lisans", text))
            }
        }
        @JavascriptInterface fun share(text: String) {
            main.post {
                val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
                startActivity(Intent.createChooser(i, "Lisans anahtarını gönder"))
            }
        }
    }
}
