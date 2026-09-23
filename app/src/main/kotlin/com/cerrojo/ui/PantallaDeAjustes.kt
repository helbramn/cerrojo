package com.cerrojo.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cerrojo.core.limitesDe
import com.cerrojo.core.mediaDeUso
import com.cerrojo.datos.Almacen
import com.cerrojo.datos.Sesion
import com.cerrojo.sistema.LectorDeUso

private data class AppInstalada(val paquete: String, val nombre: String, val deUsuario: Boolean)

@Composable
fun PantallaDeAjustes() {
    val context = LocalContext.current
    val almacen = remember { Almacen(context) }
    val lector = remember { LectorDeUso(context) }
    val alcance = rememberCoroutineScope()
    var vigiladas by remember { mutableStateOf(almacen.appsVigiladas()) }
    var refresco by remember { mutableIntStateOf(0) }

    // Listar las apps instaladas resuelve un intent y un nombre por cada una;
    // en el hilo principal eso congela el primer fotograma de la pantalla.
    var instaladas by remember { mutableStateOf(emptyList<AppInstalada>()) }
    LaunchedEffect(Unit) {
        instaladas = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(0)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    AppInstalada(
                        it.packageName,
                        pm.getApplicationLabel(it).toString(),
                        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0,
                    )
                }
                .sortedBy { it.nombre.lowercase() }
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Button(
                onClick = { context.startActivity(Intent(context, Shell::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Abrir Disciplina") }
        }
        item {
            val sesion = remember { Sesion(context) }
            var conectado by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { Thread { conectado = sesion.token() != null }.start() }

            if (!conectado) {
                var correo by remember { mutableStateOf("") }
                var clave by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Conecta tu cuenta para recibir los avisos",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = correo, onValueChange = { correo = it },
                        label = { Text("Correo") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = clave, onValueChange = { clave = it },
                        label = { Text("Contrasena") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        Thread { conectado = sesion.entrar(correo, clave) }.start()
                    }) { Text("Entrar") }
                }
            }
        }
        item {
            Text("Apps vigiladas", style = MaterialTheme.typography.headlineSmall)

            // El latido tiene que moverse solo. Un numero congelado se lee como
            // "todo bien" justo cuando el servicio acaba de morir, que es el
            // unico momento en que esta linea importa.
            var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    ahora = System.currentTimeMillis()
                }
            }
            val latido = almacen.ultimaComprobacionMs
            Text(
                if (latido == 0L) "El servicio aún no ha dado señales"
                else "Última comprobación hace ${(ahora - latido) / 1000} s",
                style = MaterialTheme.typography.labelMedium,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Text(
                    "Has denegado las notificaciones: el cerrojo sigue funcionando, pero no verás el aviso de que está vivo.",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        items(instaladas.filter { it.deUsuario || vigiladas.contains(it.paquete) }) { app ->
            val paquete = app.paquete
            val activa = vigiladas.contains(paquete)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = activa, onCheckedChange = { marcada ->
                            vigiladas = if (marcada) vigiladas + paquete else vigiladas - paquete
                            almacen.guardarAppsVigiladas(vigiladas)
                            if (marcada) {
                                // Catorce consultas al sistema: fuera del hilo
                                // principal, o la interaccion mas importante de
                                // la app se queda pillada medio segundo.
                                alcance.launch(Dispatchers.IO) {
                                    val media = mediaDeUso(lector.minutosPorDia(paquete))
                                    almacen.guardarLimites(
                                        paquete,
                                        limitesDe(
                                            media,
                                            almacen.semanaDeLosLimites.coerceAtLeast(1),
                                            almacen.suelo(paquete),
                                        ),
                                    )
                                    refresco++
                                }
                            }
                        })
                        Text(app.nombre)
                    }
                    if (activa) {
                        val l = remember(refresco, paquete) { almacen.limites(paquete) }
                        Text(
                            if (l == null) "Calculando límites…"
                            else "Objetivo ${l.objetivoMin} min/día · sesión ${l.sesionMin} min · espera ${l.enfriamientoMin} min",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
