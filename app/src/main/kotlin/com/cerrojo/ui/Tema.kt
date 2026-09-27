package com.cerrojo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * La misma paleta que la web, valor por valor.
 *
 * Los hex estan copiados de src/app/globals.css de app-disciplina, no
 * aproximados: Seal y la web se ven una detras de la otra en la misma
 * pantalla —el WebView ocupa la pantalla entera y los ajustes nativos se
 * abren encima— y dos negros parecidos pero distintos se notan mas que dos
 * colores claramente diferentes.
 *
 * Si alla cambia la paleta, aqui hay que cambiarla a mano. No hay forma de
 * compartir un fichero entre un CSS de Next.js y un Kotlin de Android sin
 * montar un generador, y para una paleta que cambia una vez al ano eso cuesta
 * mas de lo que ahorra.
 */
private val FONDO = Color(0xFF060404)
private val TEXTO = Color(0xFFE6DCD6)
private val TARJETA = Color(0xFF0F0808)
private val ROJO = Color(0xFFE0475A)
private val ROJO_OSCURO = Color(0xFFA5222F)
private val APAGADO = Color(0xFF130A0A)
private val TEXTO_APAGADO = Color(0xFF8C7570)
private val ACENTO = Color(0xFF2A1013)
private val ACENTO_TEXTO = Color(0xFFF0929D)

private val ESQUEMA = darkColorScheme(
    background = FONDO,
    onBackground = TEXTO,
    surface = TARJETA,
    onSurface = TEXTO,
    surfaceVariant = APAGADO,
    onSurfaceVariant = TEXTO_APAGADO,
    primary = ROJO,
    onPrimary = FONDO,
    primaryContainer = ACENTO,
    onPrimaryContainer = ACENTO_TEXTO,
    secondary = APAGADO,
    onSecondary = TEXTO,
    error = ROJO_OSCURO,
    onError = TEXTO,
    outline = ROJO_OSCURO.copy(alpha = 0.45f),
    outlineVariant = ROJO_OSCURO.copy(alpha = 0.18f),
)

/**
 * --radius de la web es 0.2rem, practicamente recto, a proposito del tono
 * gotico. Material 3 redondea mucho por defecto, asi que el tema tiene que
 * decirlo o Seal saldria con botones de pastilla al lado de una web de
 * esquinas rectas.
 */
private val ESQUINAS = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(3.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
)

@Composable
fun TemaDeSeal(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ESQUEMA, shapes = ESQUINAS, content = contenido)
}
