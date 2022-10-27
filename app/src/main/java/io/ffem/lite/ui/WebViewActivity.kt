package io.ffem.lite.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ffem.lite.databinding.ActivityWebViewBinding

class WebViewActivity : BaseActivity() {
    private lateinit var b: ActivityWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityWebViewBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        val url = intent.getStringExtra("url")

        b.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                invalidateOptionsMenu()
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                b.webView.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }
        }
        b.webView.settings.javaScriptEnabled = true
        b.webView.settings.setSupportZoom(true)
        b.webView.settings.builtInZoomControls = true
        b.webView.settings.displayZoomControls = false
        b.webView.isHorizontalScrollBarEnabled = false
        b.webView.loadUrl(url!!)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (b.webView.canGoBack()) {
            b.webView.goBack()
        } else {
            finish()
        }
    }
}