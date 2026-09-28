package com.cerrojo.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.cerrojo.datos.Almacen
import com.cerrojo.datos.Sesion
import com.cerrojo.servicio.ServicioDeVigilancia
import com.cerrojo.sistema.Permisos

class Principal : ComponentActivity() {
    /** Sube cada vez que la pantalla vuelve, para releer el estado de los permisos. */
    private var visitas by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // En Android 13+ sin esto no se ve ni el latido ni los avisos del
        // reenganche, aunque el servicio funcione perfectamente.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Se lee una sola vez al crear la Activity: distingue el arranque
        // normal por el icono del lanzador de la entrada explicita desde el
        // boton de ajustes de Shell, y no debe cambiar aunque la pantalla se
        // recomponga (rotacion, vuelta de onResume, etc.).
        val abrirAjustes = intent.getBooleanExtra(EXTRA_AJUSTES, false)
        setContent {
            TemaDeSeal {
                val almacen = remember { Almacen(this@Principal) }
                var hecho by remember { mutableStateOf(almacen.asistenteHecho) }
                // Se comprueba si hay cuenta guardada, no si el token responde
                // ahora mismo: preguntarselo a la red haria aparecer la
                // pantalla de entrada cada vez que estas sin cobertura,
                // pidiendo una contraseña que nadie ha invalidado.
                var hayCuenta by remember { mutableStateOf(Sesion(this@Principal).hayCuenta()) }
                var entrarDespues by remember { mutableStateOf(false) }
                // El menu abre los limites sin cambiar de Activity; atras
                // vuelve al menu en vez de cerrar la app.
                var enLimites by remember { mutableStateOf(abrirAjustes) }
                val faltan = remember(visitas) {
                    Permisos.todos.any { it.puestoSegunElSistema(this@Principal) == false }
                }

                // El asistente reaparece solo si falta algo de verdad. Quien ya
                // paso por el no vuelve a verlo en cada arranque; a quien le
                // revoquen un permiso, si.
                if (faltan || !hecho) {
                    PantallaDePermisos(recuento = visitas, alTerminar = {
                        almacen.asistenteHecho = true
                        hecho = true
                    })
                } else if (!hayCuenta && !entrarDespues) {
                    // ANTES del WebView, no despues. El puente hacia la web se
                    // crea al entrar aqui; si se entra primero en la web, el
                    // lado nativo no se entera y acabas escribiendo la
                    // contraseña dos veces, que es justo lo que pasaba.
                    //
                    // Manda tambien sobre EXTRA_AJUSTES: la notificacion de
                    // "sesion perdida" trae ese extra, y si Ajustes ganara,
                    // aterrizaria en una pantalla sin forma de reconectar.
                    PantallaDeEntrada(
                        alEntrar = { hayCuenta = true },
                        alSaltar = { entrarDespues = true },
                    )
                } else if (enLimites) {
                    BackHandler(enabled = true) { enLimites = false }
                    PantallaDeAjustes()
                } else {
                    // El menu con las dos puertas, pedido explicitamente por el
                    // usuario. Sustituye a la decision anterior de que el icono
                    // abriera Disciplina directamente (spec §11, criterio 5):
                    // asi la mitad de la app —los limites— dejaba de existir
                    // salvo para quien encontrara el engranaje de una esquina.
                    Menu(
                        almacen = almacen,
                        alAbrirDisciplina = {
                            startActivity(Intent(this@Principal, Shell::class.java))
                        },
                        alAbrirSeal = { enLimites = true },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        visitas++
        // Si MIUI mato el servicio a media mañana, abrir la app lo revive sin
        // obligar a pasar otra vez por el asistente. El reinicio del movil lo
        // cubre ReceptorDeArranque; esto cubre el resto.
        if (Permisos.todos.none { it.puestoSegunElSistema(this) == false }) {
            ServicioDeVigilancia.arrancar(this)
        }
    }

    companion object {
        /** Distingue el boton de ajustes de Shell del arranque normal por icono. */
        const val EXTRA_AJUSTES = "ajustes"
    }
}
