package com.cerrojo.servicio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Disparada por el AlarmManager cada ~15 min (ver `ServicioDeVigilancia.armarVigilante`):
 *  el vigilante que revive el servicio si MIUI lo mato y nadie abrio la app
 *  ni reinicio el movil. */
const val ACCION_VIGILAR = "com.cerrojo.VIGILAR"

/** Xiaomi entrega QUICKBOOT_POWERON en vez de BOOT_COMPLETED tras un reinicio rapido. */
private val ACCIONES_DE_ARRANQUE = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    "android.intent.action.QUICKBOOT_POWERON",
    ACCION_VIGILAR,
)

class ReceptorDeArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exportado porque asi es como el sistema entrega BOOT_COMPLETED, pero
        // QUICKBOOT_POWERON no es un broadcast protegido: cualquier app puede
        // mandarlo. Que ese envio ajeno reviente aqui no puede tirar el
        // proceso entero, o se perderia justo el revivir que este receptor
        // existe para hacer.
        runCatching {
            if (intent.action in ACCIONES_DE_ARRANQUE) {
                ServicioDeVigilancia.arrancar(context)
                // Se rearma en cada disparo, no solo en el arranque del movil:
                // barato (setInexactRepeating es idempotente) y cubre el caso
                // de que el sistema haya tirado la alarma sin que haya habido
                // reinicio de por medio.
                ServicioDeVigilancia.armarVigilante(context)
            }
        }
    }
}
