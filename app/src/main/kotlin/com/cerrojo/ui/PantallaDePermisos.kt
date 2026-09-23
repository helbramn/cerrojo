package com.cerrojo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cerrojo.sistema.Permisos

/**
 * [recuento] cambia cada vez que la Activity vuelve a primer plano. Volver de
 * los ajustes de Android no recompone nada por si solo: sin ese empujon, la
 * tarjeta seguiria diciendo "Falta" para un permiso recien concedido y el
 * usuario se pondria a buscar un problema que ya no existe.
 */
@Composable
fun PantallaDePermisos(recuento: Int, alTerminar: () -> Unit) {
    val context = LocalContext.current
    val estados = remember(recuento) { Permisos.todos.map { it.puestoSegunElSistema(context) } }
    val faltaAlguno = estados.any { it == false }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Permisos de MIUI", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Sin estos cinco, Xiaomi mata el cerrojo a los pocos días. Concédelos uno a uno.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        itemsIndexed(Permisos.todos) { indice, permiso ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(permiso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(permiso.explicacion, style = MaterialTheme.typography.bodySmall)
                    Text(
                        when (estados[indice]) {
                            true -> "Puesto"
                            false -> "Falta"
                            null -> "No se puede comprobar solo: hazlo y dale por bueno"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Button(onClick = { permiso.abrir(context) }) { Text("Abrir ajuste") }
                }
            }
        }
        item {
            // La spec dice que el asistente no deja pasar hasta que esten los
            // cinco. Solo tres se pueden comprobar; los dos de MIUI van a
            // palabra del usuario, que es lo unico que se puede hacer.
            Button(onClick = alTerminar, Modifier.fillMaxWidth(), enabled = !faltaAlguno) {
                Text(if (faltaAlguno) "Faltan permisos por dar" else "Ya están los cinco")
            }
        }
    }
}
