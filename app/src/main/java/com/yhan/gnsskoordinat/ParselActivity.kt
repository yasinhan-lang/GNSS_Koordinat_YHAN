package com.yhan.gnsskoordinat

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

/** Haritaya taşınacak parseller (ParselActivity → MainActivity). */
object PsHolder { @Volatile var data: String? = null }

/**
 * Uygulama içi TKGM Parsel Sorgu.
 * Sitede açılan / favorilerden gösterilen her parselin GeoJSON yanıtı yakalanır;
 * "Haritaya Aktar" ya da "Haritaya Dön" ile hepsi GNSS haritasına çizilir. Geri tuşu haritaya döner.
 */
class ParselActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var btn: Button
    private val got = java.util.Collections.synchronizedList(mutableListOf<String>())
    private val main = Handler(Looper.getMainLooper())
    private var done = false

    private val cap = """(function(){if(window.__yhCap)return;window.__yhCap=1;
function g(u,t){try{if(!t||typeof t!=='string')return;if(t.indexOf('Feature')<0)return;if(!/parsel|megsis|cbsapi/i.test(u||'')&&t.indexOf('parselNo')<0)return;YHCap.add(t);}catch(e){}}
var f=window.fetch;if(f){window.fetch=function(){return f.apply(this,arguments).then(function(r){try{var u=r.url;r.clone().text().then(function(t){g(u,t)}).catch(function(){})}catch(e){}return r;});};}
var o=XMLHttpRequest.prototype.open,s=XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.open=function(m,u){this.__yu=u;return o.apply(this,arguments);};
XMLHttpRequest.prototype.send=function(){var x=this;x.addEventListener('load',function(){try{var t=(x.responseType===''||x.responseType==='text')?x.responseText:(x.responseType==='json'?JSON.stringify(x.response):'');g(x.__yu||x.responseURL,t);}catch(e){}});return s.apply(this,arguments);};
})();"""

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun pill(b: Button, bg: Int, fg: Int) {
        b.isAllCaps = false; b.setTextColor(fg); b.textSize = 13f; b.typeface = Typeface.DEFAULT_BOLD
        b.background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(bg) }
        b.setPadding(dp(12), dp(6), dp(12), dp(6)); b.minHeight = 0; b.minimumHeight = 0
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0B1724")); setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val back = Button(this).apply { text = "◀ Haritaya Dön"; setOnClickListener { finishWith() } }
        pill(back, Color.parseColor("#2A3A4E"), Color.WHITE)
        val title = TextView(this).apply { text = "Parsel Sorgu"; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
        btn = Button(this).apply { text = "Haritaya Aktar"; setOnClickListener { finishWith() } }
        pill(btn, Color.parseColor("#F2B705"), Color.parseColor("#14181D"))
        bar.addView(back, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        bar.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val hint = TextView(this).apply {
            text = "Parseli veya Favorilerim'deki parselleri haritada açın, sonra Haritaya Aktar'a basın."
            setTextColor(Color.parseColor("#C8D6E5")); textSize = 11.5f; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#123150")); setPadding(dp(8), dp(3), dp(8), dp(4))
        }
        web = WebView(this)
        root.addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(hint, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(web, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            textZoom = 100
            setGeolocationEnabled(true)
        }
        web.addJavascriptInterface(Cap(), "YHCap")
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) { view.evaluateJavascript(cap, null) }
            override fun onPageFinished(view: WebView, url: String?) { view.evaluateJavascript(cap, null) }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.scheme == "http" || u.scheme == "https") return false
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (e: Exception) { }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) { callback.invoke(origin, true, false) }
        }
        web.loadUrl(intent.getStringExtra("url") ?: "https://parselsorgu.tkgm.gov.tr/")
    }

    inner class Cap {
        @JavascriptInterface fun add(t: String) {
            if (t.length > 4_000_000) return
            got.add(t)
            main.post { btn.text = "Haritaya Aktar (" + got.size + ")" }
        }
    }

    private fun finishWith() {
        if (done) return; done = true
        val js = "(function(){try{var o={};for(var i=0;i<localStorage.length;i++){var k=localStorage.key(i);o[k]=localStorage.getItem(k);}return JSON.stringify(o);}catch(e){return '{}';}})()"
        web.evaluateJavascript(js) { st ->
            val o = JSONObject()
            o.put("texts", JSONArray(ArrayList(got)))
            o.put("storage", st ?: "")
            PsHolder.data = o.toString()
            finish()
        }
        main.postDelayed({ if (!isFinishing) { PsHolder.data = JSONObject().put("texts", JSONArray(ArrayList(got))).toString(); finish() } }, 2500)
    }

    @Deprecated("Geri tuşu: sitede geri, en başta haritaya dön")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else finishWith()
    }

    override fun onDestroy() { try { web.destroy() } catch (e: Exception) { }; super.onDestroy() }
}
