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
private const val AVISO_SESION = -1

class EspejoDeAvisos(private val context: Context) {
    private val sesion = Sesion(context)
    private val prefs = context.getSharedPreferences("espejo", Context.MODE_PRIVATE)

    /**
     * Todo va dentro de un try: esto corre en un hilo pelado lanzado por el
     * servicio, y una excepcion sin capturar ahi mata el proceso entero. Como
     * START_STICKY lo reinicia, el fallo se repetiria en bucle y el cerrojo
     * dejaria de vigilar por culpa de una notificacion.
     */
    @Synchronized
    fun comprobar() = try {
        comprobarDeVerdad()
    } catch (_: Exception) {
    }

    private fun comprobarDeVerdad() {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        olvidarDiasPasados(hoy)

        val json = sesion.obtener(
            "/rest/v1/task_logs?select=task_id,status,reminded_at&log_date=eq.$hoy"
        )
        if (json == null) {
            avisarSiSePerdioLaSesion()
            return
        }
        prefs.edit().putBoolean("sesionAvisada", false).apply()
        val filas = JSONArray(json)

        crearCanal()
        for (i in 0 until filas.length()) {
            val fila = filas.getJSONObject(i)
            val id = fila.optString("task_id")
            if (id.isEmpty()) continue
            val estado = fila.optString("status")
            val huella = estado + "|" + fila.optString("reminded_at")
            if (prefs.getString("visto:$id:$hoy", null) == huella) continue
            prefs.edit().putString("visto:$id:$hoy", huella).apply()

            val texto = when {
                estado == "fallida" -> "Has fallado una obligatoria. La barra ya lo ha notado."
                // Posponer es una decision del usuario, no un descuido: darle
                // la lata igual seria castigarle por haber hecho algo.
                estado == "pospuesta" || estado == "hecha" -> continue
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

    /**
     * Si hay cuenta guardada pero ya no se consigue token, la sesion murio y el
     * reenganche esta callado. Sin este aviso, dejaria de dar la lata en
     * silencio y el usuario lo tomaria por buena conducta suya.
     */
    private fun avisarSiSePerdioLaSesion() {
        if (!sesion.hayCuenta() || prefs.getBoolean("sesionAvisada", false)) return
        if (sesion.token() != null) return
        // Solo cuando el servidor rechaza las credenciales. Sin cobertura no es
        // sesion muerta, y un error del servidor tampoco: avisar en cada tunel
        // volveria el aviso ruido de fondo, y entonces no serviria el dia que
        // la sesion muera de verdad.
        if (sesion.ultimoFallo != Sesion.Fallo.RECHAZADA) return

        prefs.edit().putBoolean("sesionAvisada", true).apply()
        // Se olvida DESPUES de decidir avisar, nunca antes: borrar primero
        // dejaria sin cuenta a quien tiene que dar el aviso. Y hace falta
        // olvidarla para que la pantalla de ajustes ofrezca entrar de nuevo.
        sesion.olvidar()
        crearCanal()
        context.getSystemService(NotificationManager::class.java)
            .notify(AVISO_SESION, NotificationCompat.Builder(context, CANAL)
                .setContentTitle("Cerrojo")
                .setContentText("Se perdió la conexión con tu cuenta: entra en Ajustes para reconectarla.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build())
    }

    /** Las marcas de "ya avisado" son por dia; las de ayer no valen para nada. */
    private fun olvidarDiasPasados(hoy: String) {
        if (prefs.getString("dia", null) == hoy) return
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("visto:") }.forEach { editor.remove(it) }
        editor.putString("dia", hoy).apply()
    }

    private fun crearCanal() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL, "Reenganche", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
