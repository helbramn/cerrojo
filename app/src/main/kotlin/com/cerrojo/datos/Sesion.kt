package com.cerrojo.datos

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL = "https://csjjimdyqvhekzqkatir.supabase.co"
private const val CLAVE_PUBLICABLE = "sb_publishable_MPY7khAEC3acWNQfsDWU4Q_B5-cZcMI"

private const val ESPERA_CONEXION_MS = 10_000
private const val ESPERA_LECTURA_MS = 15_000

class Sesion(context: Context) {
    /**
     * Construir esto puede lanzar si el almacen de claves del movil esta
     * corrupto o se reinicio en una actualizacion del sistema — pasa. Como se
     * crea perezosamente dentro de un hilo del servicio, una excepcion aqui
     * mataria el proceso entero y con el, el cerrojo. Por eso se captura y la
     * clase queda inutil pero inofensiva.
     */
    private val prefs: SharedPreferences? = try {
        EncryptedSharedPreferences.create(
            context, "sesion",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        null
    }

    /** Hay sesion guardada, aunque su token haya caducado. */
    fun hayCuenta(): Boolean = prefs?.getString("refresh", null) != null

    fun entrar(email: String, password: String): Boolean {
        val cuerpo = JSONObject().put("email", email).put("password", password).toString()
        val respuesta = post("/auth/v1/token?grant_type=password", cuerpo) ?: return false
        guardar(respuesta)
        return true
    }

    /**
     * Sincronizado porque Supabase rota el token de refresco en cada uso y
     * trata su reutilizacion como un robo: dos hilos refrescando a la vez
     * pueden acabar con la sesion revocada y el usuario teniendo que volver a
     * entrar sin saber por que.
     */
    @Synchronized
    fun token(): String? {
        val p = prefs ?: return null
        val caduca = p.getLong("caduca", 0L)
        if (System.currentTimeMillis() < caduca - 60_000L) return p.getString("access", null)
        val refresco = p.getString("refresh", null) ?: return null
        val cuerpo = JSONObject().put("refresh_token", refresco).toString()
        val respuesta = post("/auth/v1/token?grant_type=refresh_token", cuerpo) ?: return null
        guardar(respuesta)
        return p.getString("access", null)
    }

    private fun guardar(json: JSONObject) {
        prefs?.edit()
            ?.putString("access", json.getString("access_token"))
            ?.putString("refresh", json.getString("refresh_token"))
            ?.putLong("caduca", System.currentTimeMillis() + json.getLong("expires_in") * 1000L)
            ?.apply()
    }

    private fun post(ruta: String, cuerpo: String): JSONObject? = try {
        val c = (URL(SUPABASE_URL + ruta).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            // Sin esto, una red que traga paquetes sin contestar deja el hilo
            // esperando para siempre y se van acumulando.
            connectTimeout = ESPERA_CONEXION_MS
            readTimeout = ESPERA_LECTURA_MS
            setRequestProperty("apikey", CLAVE_PUBLICABLE)
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(cuerpo.toByteArray()) }
        }
        if (c.responseCode in 200..299) JSONObject(c.inputStream.bufferedReader().readText()) else null
    } catch (_: Exception) { null }

    fun obtener(ruta: String): String? = try {
        val t = token() ?: return null
        val c = (URL(SUPABASE_URL + ruta).openConnection() as HttpURLConnection).apply {
            connectTimeout = ESPERA_CONEXION_MS
            readTimeout = ESPERA_LECTURA_MS
            setRequestProperty("apikey", CLAVE_PUBLICABLE)
            setRequestProperty("Authorization", "Bearer $t")
        }
        if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
    } catch (_: Exception) { null }
}
