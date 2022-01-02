package com.flxrs.dankchat.main.stream

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.flxrs.dankchat.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@SuppressLint("SetJavaScriptEnabled")
@AndroidEntryPoint
class StreamWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {
    private var currentUrl = ""

    init {
        with(settings) {
            javaScriptEnabled = true
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = false
        }
        webViewClient = StreamWebViewClient()

        // ViewModelStoreOwner & LifecycleOwner are only available after attach
        doOnAttach {
            val viewModelStoreOwner = findViewTreeViewModelStoreOwner() ?: return@doOnAttach
            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return@doOnAttach
            val mainViewModel = ViewModelProvider(viewModelStoreOwner)[MainViewModel::class.java]

            mainViewModel.streamEnabled
                .flowWithLifecycle(lifecycleOwner.lifecycle)
                .onEach { streamEnabled ->
                    isVisible = streamEnabled
                    val url = when {
                        streamEnabled -> {
                            val channel = mainViewModel.activeChannel.value
                            "https://player.twitch.tv/?channel=$channel&enableExtensions=true&muted=false&parent=flxrs.com"
                        }
                        else          -> BLANK_URL
                    }
                    if (currentUrl == url) {
                        return@onEach
                    }

                    currentUrl = url
                    stopLoading()
                    loadUrl(url)
                }
                .launchIn(lifecycleOwner.lifecycleScope)
        }
    }

    private inner class StreamWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url.isNullOrBlank()) {
                return true
            }

            return url != BLANK_URL && url != currentUrl
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString()
            if (url.isNullOrBlank()) {
                return true
            }

            return url != BLANK_URL && url != currentUrl
        }
    }

    private companion object {
        private const val BLANK_URL = "about:blank"
    }
}