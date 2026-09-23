package com.cerrojo.sistema

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

data class Permiso(
    val titulo: String,
    val explicacion: String,
    val puestoSegunElSistema: (Context) -> Boolean?,
    val abrir: (Context) -> Unit,
)

object Permisos {
    private fun ajustesDeLaApp(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Ningun intent de ajustes esta garantizado en una ROM de fabricante: MIUI
     * renombra y quita pantallas. Si una no existe, se cae a la ficha de la app
     * en vez de tumbar la primera pantalla que ve el usuario.
     */
    private fun abrirOCaer(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            ajustesDeLaApp(context)
        }
    }

    private fun intentarMiui(context: Context, clase: String) {
        abrirOCaer(
            context,
            Intent().setComponent(ComponentName("com.miui.securitycenter", clase))
                .putExtra("extra_pkgname", context.packageName),
        )
    }

    // El de superposicion va primero a proposito: es el unico sin el cual la
    // app no bloquea absolutamente nada, y ademas es de los que si se pueden
    // comprobar. Los demas degradan el servicio; este lo anula.
    val todos: List<Permiso> = listOf(
        Permiso(
            "Mostrar sobre otras apps",
            "El más importante de los cinco. Sin él, Android descarta en silencio el aviso de bloqueo: la app parecerá estar funcionando y no bloqueará nada, nunca.",
            { Settings.canDrawOverlays(it) },
            {
                abrirOCaer(
                    it,
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${it.packageName}")),
                )
            },
        ),
        Permiso(
            "Acceso al uso",
            "Para leer tu historial y saber qué app tienes delante.",
            { LectorDeUso(it).tienePermisoDeUso() },
            { abrirOCaer(it, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        ),
        Permiso(
            "Ventanas emergentes en segundo plano",
            "El hermano del primero, pero de Xiaomi: MIUI lo controla por su cuenta y sin él tampoco aparece el bloqueo.",
            { null },
            { intentarMiui(it, "com.miui.permcenter.permissions.PermissionsEditorActivity") },
        ),
        Permiso(
            "Inicio automático",
            "Para volver sola después de reiniciar el móvil.",
            { null },
            { intentarMiui(it, "com.miui.permcenter.autostart.AutoStartManagementActivity") },
        ),
        Permiso(
            "Batería sin restricciones",
            "Para que el ahorro de energía no la mate por la noche. Ojo: MIUI tiene además su propio ahorro en Ajustes › Batería, que hay que quitar aparte y que aquí no se puede comprobar.",
            { ctx ->
                (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(ctx.packageName)
            },
            {
                abrirOCaer(
                    it,
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${it.packageName}")),
                )
            },
        ),
    )
}
