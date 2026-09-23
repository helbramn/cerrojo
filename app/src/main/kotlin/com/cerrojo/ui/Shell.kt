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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cerrojo.datos.Almacen

class Shell : ComponentActivity() {
    private var web: WebView? = null

    /**
     * Vive fuera de la composicion porque el gesto de atras tambien lo
     * consulta, y ese callback no es un Composable.
     */
    private var sinConexion by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = Almacen(this).urlWeb

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val w = web
                // Con el aviso de sin conexion delante, retroceder movería un
                // WebView que el usuario no ve: parecería que atras no hace
                // nada. Ahi se sale, que es lo unico con efecto visible.
                if (!sinConexion && w != null && w.canGoBack()) w.goBack() else finish()
            }
        })

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // El WebView se queda montado SIEMPRE y el aviso de sin
                // conexion se pinta encima. Si se desmontara, reintentar
                // significaria crear otro desde cero: se perderia la posicion,
                // lo que el usuario estuviera escribiendo, y la pagina donde
                // fallo — volveria a la portada.
                Box(Modifier.fillMaxSize()) {
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
                                        // Solo el documento principal: una
                                        // imagen que no carga no es estar sin
                                        // conexion.
                                        if (req.isForMainFrame) sinConexion = true
                                    }
                                }
                                web = this
                                loadUrl(url)
                            }
                        },
                        // Sin esto el WebView se queda con sus recursos nativos
                        // sin soltar al cerrar la pantalla.
                        onRelease = {
                            it.destroy()
                            web = null
                        },
                    )

                    if (sinConexion) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                // Pintar un fondo no se come los toques: sin
                                // esto, tocar fuera del boton llegaria al
                                // WebView de debajo y se podria navegar o
                                // enviar algo en una pagina que no se ve.
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent().changes.forEach { it.consume() }
                                        }
                                    }
                                }
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Sin conexión", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text("El cerrojo sigue funcionando igual. Esto es solo la parte que necesita internet.")
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                sinConexion = false
                                // Recargar el mismo WebView reintenta la pagina
                                // que fallo, no la portada.
                                web?.reload()
                            }) { Text("Reintentar") }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
