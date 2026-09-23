package com.cerrojo.datos

import android.content.Context
import com.cerrojo.core.EstadoApp
import com.cerrojo.core.Limites
import com.cerrojo.core.SUELO_POR_DEFECTO_MIN
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val FICHERO = "cerrojo"
private const val URL_WEB_POR_DEFECTO = "https://app-disciplina-theta.vercel.app"

class Almacen(context: Context) {
    private val prefs = context.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun appsVigiladas(): List<String> =
        prefs.getStringSet("apps", emptySet())!!.toList().sorted()

    fun guardarAppsVigiladas(paquetes: List<String>) =
        prefs.edit().putStringSet("apps", paquetes.toSet()).apply()

    fun estado(paquete: String): EstadoApp =
        prefs.getString("estado:$paquete", null)?.let { json.decodeFromString(it) } ?: EstadoApp()

    fun guardarEstado(paquete: String, estado: EstadoApp) =
        prefs.edit().putString("estado:$paquete", json.encodeToString(estado)).apply()

    fun limites(paquete: String): Limites? =
        prefs.getString("limites:$paquete", null)?.let { json.decodeFromString(it) }

    fun guardarLimites(paquete: String, limites: Limites) =
        prefs.edit().putString("limites:$paquete", json.encodeToString(limites)).apply()

    fun suelo(paquete: String): Int = prefs.getInt("suelo:$paquete", SUELO_POR_DEFECTO_MIN)

    fun guardarSuelo(paquete: String, minutos: Int) =
        prefs.edit().putInt("suelo:$paquete", minutos).apply()

    var instaladoEl: Long
        get() = prefs.getLong("instaladoEl", 0L).let {
            if (it != 0L) it else System.currentTimeMillis().also { ahora ->
                prefs.edit().putLong("instaladoEl", ahora).apply()
            }
        }
        set(valor) = prefs.edit().putLong("instaladoEl", valor).apply()

    var urlWeb: String
        get() = prefs.getString("urlWeb", URL_WEB_POR_DEFECTO)!!
        set(valor) = prefs.edit().putString("urlWeb", valor).apply()

    /** Semana con la que se calcularon los limites vigentes. */
    var semanaDeLosLimites: Int
        get() = prefs.getInt("semana", 0)
        set(valor) = prefs.edit().putInt("semana", valor).apply()

    /** Hora a la que empieza el dia nuevo a efectos de presupuesto (spec 4.2). */
    var horaDeReinicioH: Int
        get() = prefs.getInt("horaReinicio", 0)
        set(valor) = prefs.edit().putInt("horaReinicio", valor).apply()

    var ultimaComprobacionMs: Long
        get() = prefs.getLong("latido", 0L)
        set(valor) = prefs.edit().putLong("latido", valor).apply()
}
