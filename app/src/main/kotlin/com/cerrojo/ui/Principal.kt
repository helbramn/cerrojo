package com.cerrojo.ui

import android.Manifest
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
                } else {
                    PantallaDeAjustes()
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
}
