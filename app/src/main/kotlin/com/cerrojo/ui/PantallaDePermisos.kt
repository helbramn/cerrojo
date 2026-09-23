package com.cerrojo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cerrojo.sistema.Permisos

@Composable
fun PantallaDePermisos(alTerminar: () -> Unit) {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Permisos de MIUI", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Sin estos cinco, Xiaomi mata el cerrojo a los pocos días. Púlsalos uno a uno.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(Permisos.todos) { permiso ->
            val puesto = remember(version) { permiso.puestoSegunElSistema(context) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(permiso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(permiso.explicacion, style = MaterialTheme.typography.bodySmall)
                    Text(
                        when (puesto) {
                            true -> "Puesto"
                            false -> "Falta"
                            null -> "No se puede comprobar solo: hazlo y dale por bueno"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Button(onClick = { permiso.abrir(context); version++ }) { Text("Abrir ajuste") }
                }
            }
        }
        item {
            Button(onClick = alTerminar, Modifier.fillMaxWidth()) { Text("Ya están los cinco") }
        }
    }
}
