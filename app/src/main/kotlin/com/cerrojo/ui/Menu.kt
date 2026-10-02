package com.cerrojo.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cerrojo.R
import com.cerrojo.datos.Almacen
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val ROJO = Color(0xFFE0475A)

/**
 * Las dos puertas de la app.
 *
 * La primera version era una columna de dos recuadros con texto, y el usuario
 * la llamo "feisima": al lado de la web, que tiene brasas, un circulo ritual y
 * sellos que brillan, el menu parecia de otra app. Esta copia ese lenguaje:
 * brasas subiendo, el circulo girando muy despacio detras, el sello de Seal
 * arriba con su halo, y dos puertas grandes con su propio simbolo — el anillo
 * de Voluntad (lo primero que se ve en la web) y el sigilo del cerrojo.
 *
 * Cada puerta sigue diciendo en que estado esta lo suyo: una puerta que no
 * dice nada de lo que hay detras obliga a entrar para averiguarlo.
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
    val alerta = vigiladas == 0 || segundos < 0 || segundos > 120

    Box(Modifier.fillMaxSize()) {
        CirculoRitual(Modifier.align(Alignment.Center))
        Brasas(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SelloConHalo()
            Spacer(Modifier.height(14.dp))
            Text(
                "SEAL",
                style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 10.sp),
                color = ROJO,
            )
            Text(
                "Elige tu puerta",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(30.dp))

            Puerta(
                titulo = "Disciplina",
                descripcion = "Hoy, el calendario, el Oráculo y tu camino.",
                estado = null,
                color = ROJO,
                alerta = false,
                simbolo = { AnilloDeVoluntad() },
                alTocar = alAbrirDisciplina,
            )

            Spacer(Modifier.height(14.dp))

            Puerta(
                titulo = "Límites",
                descripcion = "Qué apps te corta y cuánto tiempo te deja.",
                estado = when {
                    vigiladas == 0 -> "Aún no vigilas ninguna app"
                    segundos < 0 -> "$vigiladas ${plural(vigiladas)} · el cerrojo aún no ha dado señales"
                    segundos > 120 -> "$vigiladas ${plural(vigiladas)} · sin señales desde hace ${segundos / 60} min"
                    else -> "$vigiladas ${plural(vigiladas)} · cerrojo vivo"
                },
                color = ORO,
                alerta = alerta,
                simbolo = {
                    Image(
                        painterResource(R.mipmap.ic_sigilo),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                    )
                },
                alTocar = alAbrirSeal,
            )
        }
    }
}

private fun plural(n: Int) = if (n == 1) "app vigilada" else "apps vigiladas"

@Composable
private fun SelloConHalo() {
    val latido = rememberInfiniteTransition(label = "halo")
    val brillo by latido.animateFloat(
        0.25f, 0.55f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brillo",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(150.dp)
                .alpha(brillo)
                .background(Brush.radialGradient(listOf(ROJO.copy(alpha = 0.55f), Color.Transparent)))
        )
        Image(
            painterResource(R.mipmap.ic_sigilo),
            contentDescription = null,
            modifier = Modifier.size(104.dp),
        )
    }
}

@Composable
private fun Puerta(
    titulo: String,
    descripcion: String,
    estado: String?,
    color: Color,
    alerta: Boolean,
    simbolo: @Composable () -> Unit,
    alTocar: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pulsada) 0.97f else 1f, label = "escala")
    // Borde que respira, como la tarjeta de la tarea en foco de la web.
    val vivo = rememberInfiniteTransition(label = "borde")
    val intensidad by vivo.animateFloat(
        0.35f, 0.85f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "intensidad",
    )
    val tono = if (alerta) ROJO else color

    Row(
        Modifier
            .fillMaxWidth()
            .scale(escala)
            .border(1.dp, tono.copy(alpha = intensidad), MaterialTheme.shapes.medium)
            .background(
                Brush.horizontalGradient(listOf(tono.copy(alpha = 0.14f), Color(0xCC0F0808))),
                MaterialTheme.shapes.medium,
            )
            .clickable(interactionSource = interaccion, indication = null, onClick = alTocar)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { simbolo() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                titulo.uppercase(),
                style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 3.sp),
                color = color,
            )
            Text(
                descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (estado != null) {
                Text(
                    estado,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alerta) ROJO else ORO,
                )
            }
        }
        Text("›", style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp), color = color)
    }
}

/** El anillo de Voluntad de la portada de la web, en pequeño y girando. */
@Composable
private fun AnilloDeVoluntad() {
    val giro = rememberInfiniteTransition(label = "anillo")
    val angulo by giro.animateFloat(
        0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "angulo",
    )
    Canvas(Modifier.size(52.dp)) {
        val r = size.minDimension / 2
        rotate(angulo) {
            for (i in 0 until 24) {
                val a = i * (2 * PI / 24)
                val largo = if (i % 6 == 0) 6f else 3f
                drawLine(
                    ORO.copy(alpha = if (i % 6 == 0) 0.9f else 0.45f),
                    Offset(center.x + cos(a).toFloat() * r, center.y + sin(a).toFloat() * r),
                    Offset(center.x + cos(a).toFloat() * (r - largo), center.y + sin(a).toFloat() * (r - largo)),
                    strokeWidth = 2f,
                )
            }
        }
        drawCircle(Color(0x33A52334), radius = r - 9.dp.toPx(), style = Stroke(5.dp.toPx()))
        drawArc(
            ROJO, -90f, 270f, useCenter = false,
            topLeft = Offset(9.dp.toPx(), 9.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width - 18.dp.toPx(), size.height - 18.dp.toPx()),
            style = Stroke(5.dp.toPx(), cap = StrokeCap.Butt),
        )
    }
}

/**
 * El circulo ritual de fondo de la web (Ambiente): circulos y dos triangulos
 * cruzados en oro y rojo, casi invisibles, girando muy despacio.
 */
@Composable
private fun CirculoRitual(modifier: Modifier) {
    val giro = rememberInfiniteTransition(label = "ritual")
    val angulo by giro.animateFloat(
        0f, 360f, infiniteRepeatable(tween(240_000, easing = LinearEasing)), label = "angulo",
    )
    Canvas(modifier.size(560.dp).alpha(0.07f)) {
        rotate(angulo) {
            val r = size.minDimension / 2
            drawCircle(ORO, r * 0.95f, style = Stroke(2f))
            drawCircle(ORO, r * 0.75f, style = Stroke(1.5f))
            drawCircle(ROJO, r * 0.55f, style = Stroke(2f))
            for (inicio in listOf(-PI / 2, PI / 2)) {
                val p = Path()
                for (k in 0..3) {
                    val a = inicio + k * 2 * PI / 3
                    val x = center.x + cos(a).toFloat() * r * 0.9f
                    val y = center.y + sin(a).toFloat() * r * 0.9f
                    if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, ORO, style = Stroke(1.5f))
            }
        }
    }
}

private class Brasa(var x: Float, var y: Float, val r: Float, val v: Float, val fase: Float, val oro: Boolean)

/**
 * Las brasas que suben por el fondo de la web. 36 puntos y un solo Canvas:
 * cuesta menos que una imagen animada y se para solo cuando la pantalla no se
 * ve, porque withFrameNanos deja de llamarse.
 */
@Composable
private fun Brasas(modifier: Modifier) {
    val brasas = remember {
        List(36) { Brasa(Random.nextFloat(), Random.nextFloat(), 1.2f + Random.nextFloat() * 2.4f, 0.0006f + Random.nextFloat() * 0.0016f, Random.nextFloat() * 6.28f, Random.nextFloat() < 0.3f) }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            t += 0.016f
            for (b in brasas) {
                b.y -= b.v
                b.x += sin(t + b.fase) * 0.0004f
                if (b.y < -0.02f) { b.y = 1.02f; b.x = Random.nextFloat() }
            }
        }
    }
    Canvas(modifier) {
        // Leer t aqui hace que el Canvas se redibuje en cada fotograma.
        val tiempo = t
        for (b in brasas) {
            val brillo = 0.35f + 0.35f * sin(tiempo * 2 + b.fase)
            drawCircle(
                (if (b.oro) Color(0xFFE0C05A) else ROJO).copy(alpha = brillo.coerceIn(0f, 1f)),
                radius = b.r,
                center = Offset(b.x * size.width, b.y * size.height),
            )
        }
    }
}
