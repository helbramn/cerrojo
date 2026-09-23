package com.cerrojo.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cerrojo.datos.Almacen

class Shell : ComponentActivity() {
    private var web: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = Almacen(this).urlWeb

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val w = web
                if (w != null && w.canGoBack()) w.goBack() else finish()
            }
        })

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var sinConexion by remember { mutableStateOf(false) }
                var recarga by remember { mutableIntStateOf(0) }

                if (sinConexion) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Sin conexión", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("El cerrojo sigue funcionando igual. Esto es solo la parte que necesita internet.")
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { sinConexion = false; recarga++ }) { Text("Reintentar") }
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                                        CookieManager.getInstance().flush()
                                    }
                                    override fun onReceivedError(
                                        v: WebView, req: WebResourceRequest, err: WebResourceError,
                                    ) {
                                        if (req.isForMainFrame) sinConexion = true
                                    }
                                }
                                web = this
                                loadUrl(url)
                            }
                        },
                        update = { if (recarga > 0) it.reload() },
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
