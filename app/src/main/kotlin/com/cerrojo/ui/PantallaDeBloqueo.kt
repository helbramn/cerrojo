package com.cerrojo.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerrojo.core.Estado
import com.cerrojo.core.Evento
import com.cerrojo.core.avanzar
import com.cerrojo.core.limitesDe
import com.cerrojo.datos.Almacen
import kotlinx.coroutines.delay

private const val SEGUNDOS_DE_FRICCION = 45

class PantallaDeBloqueo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paquete = intent.getStringExtra("paquete") ?: return finish()
        val estado = Estado.valueOf(intent.getStringExtra("estado") ?: Estado.ENFRIANDO.name)
        val almacen = Almacen(this)
        val nombre = nombreDeApp(paquete)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var restantes by remember { mutableIntStateOf(-1) }

                LaunchedEffect(restantes) {
                    if (restantes > 0) { delay(1000); restantes -= 1 }
                    else if (restantes == 0) {
                        val limites = almacen.limites(paquete) ?: limitesDe(30, 1)
                        almacen.guardarEstado(
                            paquete,
                            avanzar(almacen.estado(paquete), Evento.Desbloqueo, limites)
                        )
                        finish()
                    }
                }

                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(nombre, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (estado == Estado.SIN_PRESUPUESTO)
                                "Se te acabaron los minutos de hoy."
                            else "Toca descansar. Vuelve luego.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(40.dp))
                        Button(onClick = { irAlInicio() }, Modifier.fillMaxWidth()) { Text("Salir") }
                        Spacer(Modifier.height(16.dp))
                        if (restantes < 0) {
                            TextButton(onClick = { restantes = SEGUNDOS_DE_FRICCION }) {
                                Text("Desbloquear igualmente")
                            }
                        } else {
                            Text("Desbloqueando en $restantes s — no cierres esta pantalla")
                        }
                    }
                }
            }
        }
    }

    private fun nombreDeApp(paquete: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(paquete, 0)).toString()
    } catch (_: Exception) { paquete }

    private fun irAlInicio() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onBackPressed() = irAlInicio()

    companion object {
        fun mostrar(context: Context, paquete: String, estado: Estado) {
            context.startActivity(
                Intent(context, PantallaDeBloqueo::class.java)
                    .putExtra("paquete", paquete)
                    .putExtra("estado", estado.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }
}
