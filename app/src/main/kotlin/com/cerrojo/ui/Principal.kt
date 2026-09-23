package com.cerrojo.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import com.cerrojo.datos.Almacen
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
            MaterialTheme(colorScheme = darkColorScheme()) {
                val almacen = remember { Almacen(this@Principal) }
                var hecho by remember { mutableStateOf(almacen.asistenteHecho) }
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
                } else if (abrirAjustes) {
                    // Entrada explicita desde Shell, no el arranque por icono:
                    // aqui es donde vive de verdad la lista de apps vigiladas
                    // y la cuenta.
                    PantallaDeAjustes()
                } else {
                    // El icono del lanzador tiene que llevar a la app de
                    // disciplina (spec §11), no a una lista de permisos y
                    // apps vigiladas: esta pantalla solo decide adonde ir y
                    // se cierra, no se queda de por medio.
                    LaunchedEffect(Unit) {
                        startActivity(Intent(this@Principal, Shell::class.java))
                        finish()
                    }
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
