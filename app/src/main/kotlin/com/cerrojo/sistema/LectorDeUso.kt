package com.cerrojo.sistema

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

class LectorDeUso(private val context: Context) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun tienePermisoDeUso(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        )
        return modo == AppOpsManager.MODE_ALLOWED
    }

    /** Paquete de la app que está delante ahora mismo, o null si no se sabe. */
    fun appEnPrimerPlano(): String? {
        val ahora = System.currentTimeMillis()
        val eventos = usm.queryEvents(ahora - 10_000L, ahora)
        val evento = UsageEvents.Event()
        var ultimo: String? = null
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            if (evento.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                ultimo = evento.packageName
            }
        }
        return ultimo
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
            val fin = inicio + 24 * 60 * 60 * 1000L
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, inicio, fin)
            val ms = stats.filter { it.packageName == paquete }.sumOf { it.totalTimeInForeground }
            if (ms > 0) resultado += (ms / 60_000L).toInt()
        }
        return resultado
    }
}
