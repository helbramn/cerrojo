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

    private fun intentarMiui(context: Context, clase: String) {
        try {
            context.startActivity(
                Intent().setComponent(ComponentName("com.miui.securitycenter", clase))
                    .putExtra("extra_pkgname", context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            ajustesDeLaApp(context)
        }
    }

    val todos: List<Permiso> = listOf(
        Permiso(
            "Acceso al uso",
            "Para leer tu historial y saber qué app tienes delante.",
            { LectorDeUso(it).tienePermisoDeUso() },
            { it.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
        ),
        Permiso(
            "Mostrar sobre otras apps",
            "Para poder pintar el bloqueo encima de la app.",
            { Settings.canDrawOverlays(it) },
            {
                it.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${it.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        ),
        Permiso(
            "Ventanas emergentes en segundo plano",
            "Permiso propio de MIUI. Sin él el bloqueo no llega a aparecer nunca.",
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
            "Para que el ahorro de energía no la mate por la noche.",
            { ctx ->
                (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(ctx.packageName)
            },
            { it.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
        ),
    )
}
