package com.cerrojo.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerrojo.datos.Almacen
import kotlinx.coroutines.delay

/**
 * Las dos puertas de la app, una encima de otra.
 *
 * Hasta ahora el icono del lanzador abria Disciplina directamente y Seal solo
 * existia detras de un engranaje en una esquina: la mitad de la app estaba
 * escondida. El usuario pidio explicitamente el menu con los dos accesos, asi
 * que esto sustituye a aquella decision (spec §11, criterio 5).
 *
 * Cada tarjeta dice en que estado esta lo suyo. Una puerta que no dice nada de
 * lo que hay detras obliga a entrar para averiguarlo.
 */
@Composable
fun Menu(
    almacen: Almacen,
    alAbrirDisciplina: () -> Unit,
    alAbrirSeal: () -> Unit,
) {
    // El latido tiene que moverse solo: un numero congelado se lee como "todo
    // bien" justo cuando el servicio acaba de morir.
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ahora = System.currentTimeMillis()
        }
    }

    val vigiladas = almacen.appsVigiladas().size
    val latido = almacen.ultimaComprobacionMs
    val segundos = if (latido == 0L) -1L else (ahora - latido) / 1000

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Seal", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "¿A dónde vas?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        Puerta(
            titulo = "Disciplina",
            descripcion = "Tus tareas, el calendario, las estadísticas y el chat.",
            estado = null,
            alerta = false,
            alTocar = alAbrirDisciplina,
        )

        Spacer(Modifier.height(12.dp))

        Puerta(
            titulo = "Límites",
            descripcion = "Qué apps te corta y cuánto tiempo te deja.",
            estado = when {
                vigiladas == 0 -> "Aún no vigilas ninguna app"
                segundos < 0 -> "$vigiladas ${plural(vigiladas)} · el cerrojo aún no ha dado señales"
                segundos > 120 -> "$vigiladas ${plural(vigiladas)} · sin señales desde hace ${segundos / 60} min"
                else -> "$vigiladas ${plural(vigiladas)} · cerrojo vivo"
            },
            // El aviso sale cuando hay algo que mirar de verdad: sin apps no
            // bloquea nada, y sin latido el servicio esta muerto.
            alerta = vigiladas == 0 || segundos < 0 || segundos > 120,
            alTocar = alAbrirSeal,
        )
    }
}

private fun plural(n: Int) = if (n == 1) "app vigilada" else "apps vigiladas"

@Composable
private fun Puerta(
    titulo: String,
    descripcion: String,
    estado: String?,
    alerta: Boolean,
    alTocar: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (alerta) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.medium,
            )
            .clickable(onClick = alTocar)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                titulo,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "→",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (estado != null) {
            Text(
                estado,
                style = MaterialTheme.typography.bodySmall,
                color = if (alerta) MaterialTheme.colorScheme.primary else ORO,
            )
        }
    }
}
