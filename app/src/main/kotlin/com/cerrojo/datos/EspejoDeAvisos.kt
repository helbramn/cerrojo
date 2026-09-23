package com.cerrojo.datos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CANAL = "reenganche"

class EspejoDeAvisos(private val context: Context) {
    private val sesion = Sesion(context)
    private val prefs = context.getSharedPreferences("espejo", Context.MODE_PRIVATE)

    fun comprobar() {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val json = sesion.obtener(
            "/rest/v1/task_logs?select=task_id,status,reminded_at&log_date=eq.$hoy"
        ) ?: return
        val filas = JSONArray(json)

        crearCanal()
        for (i in 0 until filas.length()) {
            val fila = filas.getJSONObject(i)
            val id = fila.getString("task_id")
            val huella = fila.optString("status") + "|" + fila.optString("reminded_at")
            if (prefs.getString("visto:$id:$hoy", null) == huella) continue
            prefs.edit().putString("visto:$id:$hoy", huella).apply()

            val texto = when {
                fila.optString("status") == "fallida" -> "Has fallado una obligatoria. La barra ya lo ha notado."
                !fila.isNull("reminded_at") -> "Tienes una obligatoria sin marcar. ¿A qué esperas?"
                else -> continue
            }
            context.getSystemService(NotificationManager::class.java)
                .notify(id.hashCode(), NotificationCompat.Builder(context, CANAL)
                    .setContentTitle("Disciplina")
                    .setContentText(texto)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setAutoCancel(true)
                    .build())
        }
    }

    private fun crearCanal() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL, "Reenganche", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
