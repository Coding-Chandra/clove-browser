package com.clove.browser.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.clove.browser.BrowserViewModel
import com.clove.browser.TabState
import org.json.JSONObject
import org.json.JSONTokener

const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

/**
 * Builds a fully-configured [WebView] wired to a [TabState]. Each tab keeps its own
 * WebView instance so back/forward history and scroll position survive tab switches.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createBrowserWebView(
    context: Context,
    tab: TabState,
    viewModel: BrowserViewModel,
    onScroll: (dy: Int, scrollY: Int) -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, !tab.isPrivate)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !tab.isPrivate
            databaseEnabled = !tab.isPrivate
            cacheMode = if (tab.isPrivate) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mediaPlaybackRequiresUserGesture = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            textZoom = tab.textZoom

            if (tab.isDesktopSite) {
                userAgentString = DESKTOP_USER_AGENT
            } else {
                userAgentString = userAgentString.replace("; wv", "")
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                tab.isLoading = true
                tab.progress = 0.05f
                if (url != null) tab.url = url
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                tab.isLoading = false
                tab.progress = 1f
                if (url != null) tab.url = url
                tab.canGoBack = view.canGoBack()
                tab.canGoForward = view.canGoForward()
                viewModel.recordVisit(url ?: tab.url, view.title ?: tab.title)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val url = request.url.toString()
                if (url.startsWith("http://") || url.startsWith("https://")) return false
                return try {
                    val intent = if (url.startsWith("intent:")) {
                        Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                tab.progress = newProgress / 100f
                tab.isLoading = newProgress < 100
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) tab.title = title
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
                if (icon != null) tab.favicon = icon
            }
        }

        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            tab.findCurrentMatch = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
            tab.findTotalMatches = numberOfMatches
        }

        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            viewModel.enqueueDownload(url, userAgent, contentDisposition, mimeType)
        }

        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onScroll(scrollY - oldScrollY, scrollY)
        }
    }
}

/** Updates user-agent and text zoom when modified from Safari page menu. */
fun WebView.applySafariSettings(tab: TabState) {
    settings.textZoom = tab.textZoom
    val currentUa = settings.userAgentString
    val targetUa = if (tab.isDesktopSite) DESKTOP_USER_AGENT else currentUa.replace("; wv", "")
    if (tab.isDesktopSite && currentUa != DESKTOP_USER_AGENT) {
        settings.userAgentString = DESKTOP_USER_AGENT
        reload()
    } else if (!tab.isDesktopSite && currentUa == DESKTOP_USER_AGENT) {
        settings.userAgentString = WebSettings.getDefaultUserAgent(context).replace("; wv", "")
        reload()
    }
}

/** Extracts article content for Safari Reader Mode. */
fun WebView.extractReaderContent(onResult: (title: String, contentHtml: String) -> Unit) {
    val js = """
        (function() {
            var article = document.querySelector('article') || document.querySelector('main') || document.body;
            var h1 = document.querySelector('h1');
            var title = h1 ? h1.innerText : document.title;
            var elements = article.querySelectorAll('p, h2, h3, img');
            var html = '';
            for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                if (el.tagName === 'IMG' && el.src) {
                    html += '<img src="' + el.src + '" style="max-width:100%; border-radius:12px; margin:16px 0;" />';
                } else if (el.innerText && el.innerText.trim().length > 15) {
                    var tag = el.tagName.toLowerCase();
                    html += '<' + tag + ' style="margin-bottom:16px; line-height:1.7;">' + el.innerHTML + '</' + tag + '>';
                }
            }
            return JSON.stringify({title: title, content: html});
        })()
    """.trimIndent()

    evaluateJavascript(js) { rawJson ->
        try {
            if (!rawJson.isNullOrBlank() && rawJson != "null") {
                val parsed = JSONObject(JSONTokener(rawJson).nextValue() as String)
                val title = parsed.optString("title", "")
                val content = parsed.optString("content", "")
                onResult(title, content)
            } else {
                onResult(title ?: "", "Reader view is not available for this page.")
            }
        } catch (_: Exception) {
            onResult(title ?: "", "Reader view is not available for this page.")
        }
    }
}

/** Injects interactive element-hider for Safari Distraction Control. */
fun WebView.enableDistractionControl() {
    val js = """
        (function() {
            if (window.__dcActive) return;
            window.__dcActive = true;
            var lastEl = null;
            window.__dcHighlight = function(e) {
                if (lastEl) lastEl.style.outline = '';
                var el = e.target;
                if (el && el !== document.body && el !== document.documentElement) {
                    el.style.outline = '2.5px dashed #0088FF';
                    lastEl = el;
                }
            };
            window.__dcClick = function(e) {
                e.preventDefault();
                e.stopPropagation();
                var el = e.target;
                if (el && el !== document.body) {
                    el.style.transition = 'all 0.35s ease';
                    el.style.opacity = '0';
                    el.style.transform = 'scale(0.85)';
                    setTimeout(function() { el.remove(); }, 350);
                }
            };
            document.addEventListener('mouseover', window.__dcHighlight, true);
            document.addEventListener('click', window.__dcClick, true);
        })()
    """.trimIndent()
    evaluateJavascript(js, null)
}

/** Disables Safari Distraction Control mode. */
fun WebView.disableDistractionControl() {
    val js = """
        (function() {
            if (window.__dcHighlight) document.removeEventListener('mouseover', window.__dcHighlight, true);
            if (window.__dcClick) document.removeEventListener('click', window.__dcClick, true);
            window.__dcActive = false;
        })()
    """.trimIndent()
    evaluateJavascript(js, null)
}
