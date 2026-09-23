package com.cerrojo.servicio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReceptorDeArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ServicioDeVigilancia.arrancar(context)
    }
}
