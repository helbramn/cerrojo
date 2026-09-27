package com.cerrojo.datos

import android.content.Context
import com.cerrojo.sistema.LectorDeUso
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DIAS_A_SUBIR = 14

/**
 * Puente entre el movil y el servidor para lo del cerrojo.
 *
 * Sube cuanto usa el usuario cada app —para que el coach del chat sepa de que
 * habla cuando le dice "te pasas con YouTube"— y baja que apps ha decidido
 * vigilar desde el chat.
 *
 * Nada de esto es necesario para bloquear: el servicio sigue trabajando con la
 * copia local. Si no hay red, o no hay cuenta, el cerrojo funciona igual y esto
 * simplemente no corre.
 */
class SincronizadorDeUso(private val context: Context) {
    private val sesion = Sesion(context)
    private val almacen = Almacen(context)
    private val lector = LectorDeUso(context)

    /**
     * Todo dentro de un try: esto corre en un hilo suelto lanzado por el
     * servicio, y una excepcion ahi mata el proceso entero y con el, el
     * cerrojo. Subir estadisticas no puede tumbar la vigilancia.
     */
    @Synchronized
    fun sincronizar() = try {
        sincronizarDeVerdad()
    } catch (_: Throwable) {
    }

    private fun sincronizarDeVerdad() {
        if (!sesion.hayCuenta()) return
        val subida = subirUso()
        val bajada = reconciliarVigiladas()
        if (subida && bajada) almacen.ultimaSyncOkMs = System.currentTimeMillis()
    }

    // ----------------------------------------------------------------- subir

    private fun subirUso(): Boolean {
        val pm = context.packageManager
        val filas = lector.usoPorAppYDia(DIAS_A_SUBIR) { paquete ->
            // Solo lo que el usuario puede abrir: sin este filtro entran
            // cientos de servicios del sistema que no dicen nada de como gasta
            // el tiempo, y el coach los leeria como si fueran apps suyas.
            pm.getLaunchIntentForPackage(paquete) != null
        }
        if (filas.isEmpty()) return true

        val cuerpo = JSONArray()
        for (fila in filas) {
            val nombre = try {
                pm.getApplicationLabel(pm.getApplicationInfo(fila.paquete, 0)).toString()
            } catch (_: Exception) {
                fila.paquete
            }
            cuerpo.put(
                JSONObject()
                    .put("paquete", fila.paquete)
                    .put("nombre", nombre)
                    .put("fecha", fila.fecha)
                    .put("minutos", fila.minutos)
            )
        }
        return sesion.enviar("/rest/v1/uso_apps", cuerpo.toString())
    }

    // ---------------------------------------------------------------- bajar

    /**
     * Los dos lados pueden editar la lista de apps vigiladas, asi que hay que
     * decidir quien gana: el cambio mas reciente. Cada fila del servidor trae
     * `actualizado_en` y aqui se guarda cuando se toco en el movil.
     *
     * Sin esta regla, dos sitios escribiendo lo mismo se pisan en silencio y el
     * usuario ve reaparecer apps que acababa de quitar.
     */
    private fun reconciliarVigiladas(): Boolean {
        val json = sesion.obtener(
            "/rest/v1/apps_vigiladas?select=paquete,nombre,vigilada,suelo_min,actualizado_en"
        ) ?: return false

        val delServidor = JSONArray(json)
        val vistos = mutableSetOf<String>()
        var vigiladas = almacen.appsVigiladas().toMutableList()
        val pendientesDeSubir = JSONArray()

        for (i in 0 until delServidor.length()) {
            val fila = delServidor.getJSONObject(i)
            val paquete = fila.optString("paquete").ifEmpty { continue }
            vistos += paquete

            val enServidorMs = instanteDe(fila.optString("actualizado_en"))
            val enMovilMs = almacen.cambiadoEn(paquete)

            if (enMovilMs > enServidorMs) {
                // Aqui se tocó después: manda el móvil.
                pendientesDeSubir.put(filaParaSubir(paquete, fila.optString("nombre"), vigiladas))
                continue
            }

            // Manda el servidor.
            val vigilada = fila.optBoolean("vigilada", true)
            if (vigilada && paquete !in vigiladas) vigiladas += paquete
            if (!vigilada) vigiladas.remove(paquete)
            val suelo = fila.optInt("suelo_min", 0)
            if (suelo > 0) almacen.guardarSuelo(paquete, suelo)
        }

        // Apps que se vigilan aquí y el servidor no conoce todavía.
        for (paquete in vigiladas) {
            if (paquete in vistos) continue
            val nombre = try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(paquete, 0)).toString()
            } catch (_: Exception) {
                paquete
            }
            pendientesDeSubir.put(filaParaSubir(paquete, nombre, vigiladas))
        }

        almacen.guardarAppsVigiladas(vigiladas)

        if (pendientesDeSubir.length() > 0) {
            return sesion.enviar("/rest/v1/apps_vigiladas", pendientesDeSubir.toString())
        }
        return true
    }

    private fun filaParaSubir(paquete: String, nombre: String, vigiladas: List<String>) =
        JSONObject()
            .put("paquete", paquete)
            .put("nombre", nombre.ifEmpty { paquete })
            .put("vigilada", paquete in vigiladas)
            .put("suelo_min", almacen.suelo(paquete))
            .put("actualizado_en", enISO(almacen.cambiadoEn(paquete)))
            .put("actualizado_por", "movil")

    // ------------------------------------------------------------- utilidades

    private fun formato(patron: String) =
        SimpleDateFormat(patron, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun enISO(ms: Long): String =
        formato("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date(if (ms == 0L) System.currentTimeMillis() else ms))

    /**
     * Postgres devuelve la marca de tiempo con fracciones y desplazamiento
     * ("2026-09-27T15:24:05.685+00:00"). Se recorta a segundos: sobra precisión
     * para decidir cuál de dos cambios fue antes, y así no hace falta un
     * analizador completo de ISO 8601.
     */
    private fun instanteDe(texto: String): Long = try {
        if (texto.isEmpty()) 0L
        else formato("yyyy-MM-dd'T'HH:mm:ss").parse(texto.take(19))?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}
