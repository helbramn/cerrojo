package com.cerrojo.servicio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Xiaomi entrega QUICKBOOT_POWERON en vez de BOOT_COMPLETED tras un reinicio rapido. */
private val ACCIONES_DE_ARRANQUE = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    "android.intent.action.QUICKBOOT_POWERON",
)

class ReceptorDeArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in ACCIONES_DE_ARRANQUE) ServicioDeVigilancia.arrancar(context)
    }
}
