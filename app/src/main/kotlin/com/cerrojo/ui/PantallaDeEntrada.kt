package com.cerrojo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cerrojo.datos.Sesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * La unica pantalla de entrada de Seal.
 *
 * Existe porque el inicio de sesion unico anterior estaba construido al reves:
 * el puente hacia la web se creaba dentro de Sesion.entrar(), y entrar() solo
 * se llamaba desde un formulario escondido en Ajustes. Pero el icono del
 * lanzador va derecho al WebView, asi que el camino real era abrir la app y
 * que te pidiera la contraseña la web — sin que el lado nativo se enterara
 * nunca. Dos inicios de sesion, uno detras de otro.
 *
 * Ahora esta pantalla sale ANTES que el WebView cuando no hay cuenta, que es
 * la unica forma de garantizar que la entrega a la web llegue a existir.
 *
 * @param alEntrar se llama cuando la sesion ya esta guardada y la entrega para
 *        el WebView creada. Quien la reciba puede cargar la web ya dentro.
 * @param alSaltar si no es null, se ofrece una salida para entrar en la web a
 *        mano. Sirve de escape si la contraseña del movil falla por lo que sea;
 *        sin ella, un fallo aqui dejaria la app sin forma de avanzar.
 */
@Composable
fun PantallaDeEntrada(
    titulo: String = "Seal",
    explicacion: String = "Entra una vez aquí y no te lo vuelvo a pedir: la misma sesión vale para la app y para la web de dentro.",
    alEntrar: () -> Unit,
    alSaltar: (() -> Unit)? = null,
) {
    val contexto = LocalContext.current
    val sesion = remember { Sesion(contexto) }
    val alcance = rememberCoroutineScope()

    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var entrando by remember { mutableStateOf(false) }
    var fallo by remember { mutableStateOf<String?>(null) }

    fun entrar() {
        if (entrando || correo.isBlank() || clave.isBlank()) return
        fallo = null
        entrando = true
        // En una coroutine y no en un Thread suelto: la respuesta se asigna a
        // estado de Compose, y eso tiene que volver al hilo principal.
        alcance.launch {
            val ok = withContext(Dispatchers.IO) { sesion.entrar(correo.trim(), clave) }
            entrando = false
            if (ok) {
                alEntrar()
            } else {
                // Decir POR QUE, no solo que no. Un "no se pudo" generico manda
                // a probar la contraseña otra vez cuando el problema era que no
                // habia cobertura.
                fallo = when (sesion.ultimoFalloDeSesion) {
                    Sesion.Fallo.SIN_RED -> "Sin conexión. La contraseña puede estar bien; inténtalo cuando haya red."
                    Sesion.Fallo.DEL_SERVIDOR -> "El servidor ha fallado, no es cosa tuya. Prueba en un minuto."
                    else -> "Correo o contraseña incorrectos."
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            explicacion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            enabled = !entrando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña", style = MaterialTheme.typography.labelMedium) },
            singleLine = true,
            enabled = !entrando,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { entrar() }),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { entrar() },
            enabled = !entrando && correo.isNotBlank() && clave.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                if (entrando) "Entrando…" else "Entrar",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (fallo != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                fallo!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }

        if (alSaltar != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = alSaltar) {
                Text("Entrar solo en la web", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
