package com.cerrojo.datos

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL = "https://csjjimdyqvhekzqkatir.supabase.co"
private const val CLAVE_PUBLICABLE = "sb_publishable_MPY7khAEC3acWNQfsDWU4Q_B5-cZcMI"

class Sesion(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "sesion",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun entrar(email: String, password: String): Boolean {
        val cuerpo = JSONObject().put("email", email).put("password", password).toString()
        val respuesta = post("/auth/v1/token?grant_type=password", cuerpo, conToken = false) ?: return false
        guardar(respuesta)
        return true
    }

    fun token(): String? {
        val caduca = prefs.getLong("caduca", 0L)
        if (System.currentTimeMillis() < caduca - 60_000L) return prefs.getString("access", null)
        val refresco = prefs.getString("refresh", null) ?: return null
        val cuerpo = JSONObject().put("refresh_token", refresco).toString()
        val respuesta = post("/auth/v1/token?grant_type=refresh_token", cuerpo, conToken = false) ?: return null
        guardar(respuesta)
        return prefs.getString("access", null)
    }

    private fun guardar(json: JSONObject) {
        prefs.edit()
            .putString("access", json.getString("access_token"))
            .putString("refresh", json.getString("refresh_token"))
            .putLong("caduca", System.currentTimeMillis() + json.getLong("expires_in") * 1000L)
            .apply()
    }

    private fun post(ruta: String, cuerpo: String, conToken: Boolean): JSONObject? = try {
        val c = (URL(SUPABASE_URL + ruta).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("apikey", CLAVE_PUBLICABLE)
            setRequestProperty("Content-Type", "application/json")
            if (conToken) setRequestProperty("Authorization", "Bearer ${token()}")
            doOutput = true
            outputStream.use { it.write(cuerpo.toByteArray()) }
        }
        if (c.responseCode in 200..299) JSONObject(c.inputStream.bufferedReader().readText()) else null
    } catch (_: Exception) { null }

    fun obtener(ruta: String): String? = try {
        val t = token() ?: return null
        val c = (URL(SUPABASE_URL + ruta).openConnection() as HttpURLConnection).apply {
            setRequestProperty("apikey", CLAVE_PUBLICABLE)
            setRequestProperty("Authorization", "Bearer $t")
        }
        if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
    } catch (_: Exception) { null }
}
