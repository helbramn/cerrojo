package com.cerrojo.sistema

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Los dias se cuentan aqui igual que en el servidor, no en la zona del movil. */
private const val ZONA = "Europe/Madrid"

/** Un dia de uso de una app concreta. */
data class UsoDeApp(val paquete: String, val fecha: String, val minutos: Int)

private const val PRIMERA_MIRADA_ATRAS_MS = 12 * 60 * 60 * 1000L

/**
 * Esta clase guarda estado entre llamadas: hay que crear UNA y reutilizarla,
 * no una por consulta. El servicio de vigilancia la crea en su `onCreate`.
 */
class LectorDeUso(private val context: Context) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private var enPrimerPlano: String? = null
    private var ultimaConsultaMs = 0L

    /**
     * El tipo se queda en `Boolean?` porque las llamadas ya tratan `null` como
     * "no bloquear", pero esta funcion ya no devuelve `null`. `MODE_DEFAULT`
     * es el modo por defecto de `OP_GET_USAGE_STATS` en AOSP: es el estado
     * normal de "nunca concedido", no uno indescifrable. En vez de adivinar
     * que significa, se prueba de verdad: se pide una consulta de uso real y
     * se mira si trae algo. Eso comprueba exactamente la capacidad que
     * importa y no puede equivocarse en ningun sentido.
     */
    fun tienePermisoDeUso(): Boolean? {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        )
        return when (modo) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_DEFAULT -> probarAccesoReal()
            else -> false
        }
    }

    /**
     * Corre en el hilo del servicio de vigilancia: cualquier excepcion aqui
     * degrada a "no tiene permiso" en vez de tumbar el proceso.
     */
    private fun probarAccesoReal(): Boolean = try {
        val ahora = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, ahora - 24 * 60 * 60 * 1000L, ahora)
        !stats.isNullOrEmpty()
    } catch (_: Exception) {
        false
    }

    /**
     * Paquete de la app que esta delante ahora mismo, o null si no se sabe.
     *
     * `ACTIVITY_RESUMED` se emite UNA vez, al entrar la app en primer plano, y
     * no se repite mientras sigues dentro. Por eso no vale mirar una ventana
     * fija de los ultimos segundos: quien lleve mas de esa ventana en la misma
     * app no genera ningun evento y pareceria no estar en ninguna parte. Lo que
     * se hace es arrastrar el ultimo paquete conocido entre llamadas y
     * consumir solo los eventos nuevos desde la consulta anterior.
     */
    fun appEnPrimerPlano(): String? {
        val ahora = System.currentTimeMillis()
        val desde = if (ultimaConsultaMs == 0L) ahora - PRIMERA_MIRADA_ATRAS_MS else ultimaConsultaMs
        val eventos = usm.queryEvents(desde, ahora)
        val evento = UsageEvents.Event()
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            when (evento.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> enPrimerPlano = evento.packageName
                UsageEvents.Event.ACTIVITY_PAUSED ->
                    if (evento.packageName == enPrimerPlano) enPrimerPlano = null
            }
        }
        ultimaConsultaMs = ahora
        return enPrimerPlano
    }

    /**
     * Uso de TODAS las apps del usuario, día a día. Es lo que se sube al
     * servidor para que el coach pueda analizarlo.
     *
     * Una consulta por día en vez de una por app y día: [minutosPorDia] sirve
     * para una sola app, pero para subirlo todo serían decenas de consultas
     * por día. Aquí se pide el día entero y se reparte por paquete.
     *
     * Solo apps que el usuario puede abrir: sin ese filtro entran cientos de
     * servicios del sistema que no dicen nada sobre cómo gasta el tiempo.
     */
    fun usoPorAppYDia(dias: Int = 14, seQuedan: (String) -> Boolean): List<UsoDeApp> {
        // Zona fija, no la del dispositivo: el servidor y la web cuentan los
        // dias en Europe/Madrid, y si el movil viajara o tuviera mal la zona,
        // las fechas subidas no cuadrarian con las que el coach lee.
        val zona = TimeZone.getTimeZone(ZONA)
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zona }
        val resultado = mutableListOf<UsoDeApp>()
        val cal = Calendar.getInstance(zona).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            // Empieza HOY, no ayer: el dato que el coach querria fresco es
            // justamente el de hoy ("ya llevas 90 minutos"), y era el unico
            // que no se subia.
            add(Calendar.DAY_OF_YEAR, 1)
        }
        repeat(dias) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val inicio = cal.timeInMillis
            val fin = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val fecha = formatoFecha.format(Date(inicio))

            // Android puede devolver varias filas del mismo paquete en un dia;
            // hay que sumarlas, no quedarse con la primera.
            val porPaquete = mutableMapOf<String, Long>()
            for (s in usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, inicio, fin).orEmpty()) {
                if (!seQuedan(s.packageName)) continue
                porPaquete[s.packageName] = (porPaquete[s.packageName] ?: 0L) + s.totalTimeInForeground
            }
            for ((paquete, ms) in porPaquete) {
                val minutos = (ms / 60_000L).toInt()
                if (minutos > 0) resultado += UsoDeApp(paquete, fecha, minutos)
            }
        }
        return resultado
    }

    /**
     * Minutos de uso por día de los últimos [dias] días, sin contar el de hoy.
     * Se consulta día a día porque los cubos que devuelve Android al pedir un
     * rango largo no se pueden separar por fecha de forma fiable.
     */
    fun minutosPorDia(paquete: String, dias: Int = 14): List<Int> {
        val resultado = mutableListOf<Int>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        repeat(dias) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val inicio = cal.timeInMillis
            // El fin se calcula con el calendario, no sumando 24 h fijas: los
            // dos dias del año en que cambia la hora duran 23 o 25 horas, y con
            // el offset fijo una hora se contaria dos veces o no se contaria.
            val fin = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            // MIUI se desvia de lo documentado mas de una vez; si devuelve null
            // en vez de una lista vacia, aqui reventaria el bucle del servicio.
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, inicio, fin).orEmpty()
            val ms = stats.filter { it.packageName == paquete }.sumOf { it.totalTimeInForeground }
            if (ms > 0) resultado += (ms / 60_000L).toInt()
        }
        return resultado
    }
}
