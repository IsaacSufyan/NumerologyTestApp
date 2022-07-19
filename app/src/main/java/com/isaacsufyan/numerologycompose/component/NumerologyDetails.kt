package com.isaacsufyan.numerologycompose.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NumerologyDetails(result: Int) {

    val url =
        "https://www.numerology.com/articles/about-numerology/single-digit-number-$result-meaning/"
    val visibility = remember { mutableStateOf(false) }
    val progress = remember { mutableStateOf(0.0F) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true

                webViewClient = object : WebViewClient() {

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        visibility.value = true
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        visibility.value = false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        progress.value = newProgress.toFloat()
                    }
                }
                loadUrl(url)
            }
        }, update = {
            it.loadUrl(url)
        })

        if (visibility.value) {
            CircularProgressIndicator(modifier = Modifier.size(200.dp))
            Text(
                text = "${progress.value.roundToInt()}%",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
