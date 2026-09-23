package com.cerrojo.sistema

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

private const val PRIMERA_MIRADA_ATRAS_MS = 12 * 60 * 60 * 1000L

/**
 * Esta clase guarda estado entre llamadas: hay que crear UNA y reutilizarla,
 * no una por consulta. El servicio de vigilancia la crea en su `onCreate`.
 */
class LectorDeUso(private val context: Context) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private var enPrimerPlano: String? = null
    private var ultimaConsultaMs = 0L

    fun tienePermisoDeUso(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        )
        return modo == AppOpsManager.MODE_ALLOWED
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
