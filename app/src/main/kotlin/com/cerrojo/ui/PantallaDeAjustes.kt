package com.cerrojo.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cerrojo.core.limitesDe
import com.cerrojo.core.mediaDeUso
import com.cerrojo.datos.Almacen
import com.cerrojo.sistema.LectorDeUso

@Composable
fun PantallaDeAjustes() {
    val context = LocalContext.current
    val almacen = remember { Almacen(context) }
    val lector = remember { LectorDeUso(context) }
    var vigiladas by remember { mutableStateOf(almacen.appsVigiladas()) }

    val instaladas = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || vigiladas.contains(it.packageName) }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Apps vigiladas", style = MaterialTheme.typography.headlineSmall)
            val hace = (System.currentTimeMillis() - almacen.ultimaComprobacionMs) / 1000
            Text("Última comprobación hace $hace s", style = MaterialTheme.typography.labelMedium)
        }
        items(instaladas) { app ->
            val paquete = app.packageName
            val activa = vigiladas.contains(paquete)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = activa, onCheckedChange = { marcada ->
                            vigiladas = if (marcada) vigiladas + paquete else vigiladas - paquete
                            almacen.guardarAppsVigiladas(vigiladas)
                            if (marcada) {
                                val media = mediaDeUso(lector.minutosPorDia(paquete))
                                almacen.guardarLimites(paquete, limitesDe(media, 1, almacen.suelo(paquete)))
                            }
                        })
                        Text(context.packageManager.getApplicationLabel(app).toString())
                    }
                    if (activa) {
                        val l = almacen.limites(paquete)
                        if (l != null) {
                            Text(
                                "Objetivo ${l.objetivoMin} min/día · sesión ${l.sesionMin} min · espera ${l.enfriamientoMin} min",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
