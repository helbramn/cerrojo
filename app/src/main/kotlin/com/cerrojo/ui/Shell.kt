package com.cerrojo.ui

import android.content.Intent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cerrojo.datos.Almacen
import com.cerrojo.datos.Sesion
import java.net.URLEncoder

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
        val primeraCarga = urlDeEntrada(url)

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
            TemaDeSeal {
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
                                // El WebView pinta blanco hasta que la pagina
                                // se dibuja. Sobre un tema negro eso es un
                                // fogonazo en cada apertura.
                                setBackgroundColor(0xFF060404.toInt())
                                web = this
                                loadUrl(primeraCarga)
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

                    // Ajustes de Seal (apps vigiladas, cuenta, permisos de
                    // MIUI) — no de la web. Desde que el icono del lanzador
                    // lleva directo aqui, sin este boton esa pantalla se
                    // quedaria sin ninguna entrada obvia. Fijo y pequeño, por
                    // encima incluso del aviso de "sin conexión": los ajustes
                    // de Seal no dependen de la red.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            .clickable {
                                startActivity(
                                    Intent(this@Shell, Principal::class.java)
                                        .putExtra(Principal.EXTRA_AJUSTES, true)
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚙", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }

    /**
     * Si Seal acaba de iniciar sesion, la web se abre por una ruta que recibe
     * los tokens y los planta como sesion del navegador. Asi solo se entra una
     * vez: en Seal.
     *
     * Van en el fragmento de la URL (detras de #) a proposito. El fragmento no
     * viaja al servidor, asi que los tokens no acaban en los registros de
     * Vercel; es el mismo sitio donde los pone Supabase en sus propios enlaces
     * de acceso.
     */
    private fun urlDeEntrada(url: String): String {
        val entrega = Sesion(this).tomarEntregaParaWeb() ?: return url
        fun cod(v: String) = URLEncoder.encode(v, "UTF-8")
        return url.trimEnd('/') + "/auth/sesion-movil#access_token=" +
            cod(entrega.first) + "&refresh_token=" + cod(entrega.second)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
