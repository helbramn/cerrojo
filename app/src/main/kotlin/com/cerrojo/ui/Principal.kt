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
import com.cerrojo.servicio.ServicioDeVigilancia

class Principal : ComponentActivity() {
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
                var permisosListos by remember { mutableStateOf(false) }
                if (!permisosListos) {
                    PantallaDePermisos(alTerminar = {
                        permisosListos = true
                        ServicioDeVigilancia.arrancar(this@Principal)
                    })
                } else {
                    PantallaDeAjustes()
                }
            }
        }
    }
}
