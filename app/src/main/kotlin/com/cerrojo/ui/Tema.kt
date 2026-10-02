package com.cerrojo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cerrojo.R

/**
 * El mismo aspecto que la web, no solo los mismos colores.
 *
 * La version anterior copiaba la paleta y se quedaba ahi, y se notaba: Seal
 * salia en Roboto al lado de una web en Cinzel y EB Garamond. Una paleta es
 * como un quinto de una identidad visual; el resto es la letra y la forma.
 *
 * Los hex estan copiados de src/app/globals.css de app-disciplina, no
 * aproximados: las dos se ven una detras de la otra en la misma pantalla —el
 * WebView ocupa todo y las pantallas nativas se abren encima— y dos negros
 * parecidos pero distintos se notan mas que dos colores claramente diferentes.
 *
 * Si alla cambia la paleta, aqui hay que cambiarla a mano. No hay forma de
 * compartir un fichero entre un CSS de Next.js y un Kotlin de Android sin
 * montar un generador, y para algo que cambia una vez al ano eso cuesta mas de
 * lo que ahorra.
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

/** El oro de la web, reservado para detalles puntuales igual que alla. */
val ORO = Color(0xFFB8945A)

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
 * Las dos fuentes de la web. Son ficheros variables —un solo TTF cubre todos
 * los pesos— asi que el peso se fija con FontVariation en vez de meter un
 * fichero por peso: pesan 125 KB y 851 KB, y duplicarlos por cada grosor
 * engordaria el APK sin necesidad.
 *
 * FontVariation sigue marcada como experimental en Compose 1.7 y sin el OptIn
 * la compilacion falla. No es inestable en la practica —lleva ahi desde la
 * 1.5— pero al subir de version hay que volver a mirarlo.
 */
@OptIn(ExperimentalTextApi::class)
private fun cinzel(peso: Int) = Font(
    R.font.cinzel_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

@OptIn(ExperimentalTextApi::class)
private fun garamond(peso: Int) = Font(
    R.font.eb_garamond_variable,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

val CINZEL = FontFamily(cinzel(400), cinzel(600))
val GARAMOND = FontFamily(garamond(400), garamond(500))

/**
 * Cinzel en titulares y etiquetas, EB Garamond en el cuerpo. Igual que la web
 * y por el mismo motivo: Cinzel es una capital romana y a tamaño de parrafo se
 * lee mal.
 *
 * Las etiquetas van espaciadas, que es como se usan alla.
 */
private val LETRA = Typography(
    displayLarge = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 34.sp, letterSpacing = 1.2.sp),
    headlineLarge = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 26.sp, letterSpacing = 1.sp),
    headlineMedium = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 22.sp, letterSpacing = 0.9.sp),
    headlineSmall = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 18.sp, letterSpacing = 0.8.sp),
    titleLarge = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 17.sp, letterSpacing = 0.7.sp),
    titleMedium = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 14.sp, letterSpacing = 0.6.sp),
    titleSmall = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(400), fontSize = 12.sp, letterSpacing = 1.4.sp),

    bodyLarge = TextStyle(fontFamily = GARAMOND, fontWeight = FontWeight(400), fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = GARAMOND, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = GARAMOND, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 19.sp),

    labelLarge = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(600), fontSize = 13.sp, letterSpacing = 1.1.sp),
    labelMedium = TextStyle(fontFamily = CINZEL, fontWeight = FontWeight(400), fontSize = 11.sp, letterSpacing = 1.3.sp),
    labelSmall = TextStyle(fontFamily = GARAMOND, fontWeight = FontWeight(400), fontSize = 12.sp),
)

/**
 * La web baja --radius a 0.2rem, practicamente recto, a proposito del tono
 * gotico. Material 3 redondea mucho por defecto, asi que hay que decirlo o
 * Seal sale con botones de pastilla al lado de una web de esquinas rectas.
 */
private val ESQUINAS = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

/**
 * El halo rojo que la web pinta detras del contenido (el radial-gradient del
 * body en globals.css). Sin el, las pantallas nativas son negro plano y se ven
 * mas apagadas que la web que tienen justo al lado.
 */
private val HALO = Brush.radialGradient(
    colors = listOf(Color(0x16A52334), Color(0x00A52334)),
    center = Offset(560f, -160f),
    radius = 1200f,
)

@Composable
fun TemaDeSeal(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ESQUEMA, shapes = ESQUINAS, typography = LETRA) {
        Box(
            Modifier
                .fillMaxSize()
                .background(FONDO)
                .background(HALO)
        ) {
            // Color de texto por defecto. MaterialTheme no lo pone: lo pone
            // Surface, y estas pantallas van sobre un Box. Sin esto, todo Text
            // sin color explicito salia NEGRO sobre el fondo negro — asi
            // desaparecieron los nombres de las apps en Ajustes (2-oct).
            CompositionLocalProvider(LocalContentColor provides TEXTO) {
                contenido()
            }
        }
    }
}
