package com.cerrojo.core

const val BONUS_DESBLOQUEO_SEG = 5 * 60

/**
 * Salir de la app al menos este tiempo cierra la sesion: la siguiente vez
 * empieza de cero. Ajustes promete "X minutos seguidos", y sin esto el reloj
 * de sesion sumaba todos los ratos del dia hasta el siguiente descanso — tres
 * ratos sueltos de 7 minutos bloqueaban 80 minutos sin haber estado nunca 20
 * seguidos. Las salidas cortas (contestar un mensaje) no cuentan como pausa,
 * para que entrar y salir no sirva de trampa. Elegido por el usuario el 1-oct.
 */
const val PAUSA_QUE_CIERRA_SESION_MS = 5 * 60_000L

enum class Estado { LIBRE, EN_SESION, ENFRIANDO, SIN_PRESUPUESTO }

@kotlinx.serialization.Serializable
data class EstadoApp(
    val estado: Estado = Estado.LIBRE,
    val segSesion: Int = 0,
    val segHoy: Int = 0,
    val extraSesionSeg: Int = 0,
    val extraHoySeg: Int = 0,
    val finEnfriamientoMs: Long = 0L,
    val dia: String = "",
    /** Ultimo tic con la app delante. Con valor por defecto: los estados ya guardados siguen leyendose. */
    val ultimoUsoMs: Long = 0L,
)

sealed interface Evento {
    data class Tick(val enPrimerPlano: Boolean, val ahoraMs: Long, val dia: String) : Evento

    /** Salida de friccion: 45 s de espera con la pantalla encendida. */
    data object Desbloqueo : Evento
}

fun avanzar(previo: EstadoApp, evento: Evento, limites: Limites): EstadoApp = when (evento) {
    // Solo desbloquea lo que esta bloqueado. Si llega dos veces seguidas, o si
    // llega cuando el enfriamiento ya habia vencido por su cuenta, no regala
    // otros cinco minutos: la maquina se defiende sola en vez de fiarse de que
    // la interfaz no dispare el evento de mas.
    is Evento.Desbloqueo -> if (!previo.bloqueada()) previo else previo.copy(
        estado = Estado.EN_SESION,
        extraSesionSeg = previo.extraSesionSeg + BONUS_DESBLOQUEO_SEG,
        extraHoySeg = previo.extraHoySeg + BONUS_DESBLOQUEO_SEG,
        finEnfriamientoMs = 0L,
        // A cero: el tiempo que paso bloqueada no es una pausa. Si contara, un
        // desbloqueo tras 5 min de descanso reiniciaria la sesion y daria la
        // sesion entera en vez de los 5 minutos de la friccion.
        ultimoUsoMs = 0L,
    )

    is Evento.Tick -> {
        var e = if (evento.dia != previo.dia) {
            EstadoApp(dia = evento.dia)
        } else previo

        if (e.estado == Estado.ENFRIANDO && evento.ahoraMs >= e.finEnfriamientoMs) {
            e = e.copy(estado = Estado.LIBRE, segSesion = 0, extraSesionSeg = 0, finEnfriamientoMs = 0L)
        }

        // Pausa larga: la sesion se cierra. Se mira tanto con la app fuera (para
        // que el estado pase a LIBRE) como al volver a ella (por si el servicio
        // estuvo muerto durante la pausa y no hubo tics en medio).
        if (e.estado == Estado.EN_SESION && e.ultimoUsoMs > 0L &&
            evento.ahoraMs - e.ultimoUsoMs >= PAUSA_QUE_CIERRA_SESION_MS
        ) {
            e = e.copy(estado = Estado.LIBRE, segSesion = 0, extraSesionSeg = 0)
        }

        if (evento.enPrimerPlano && (e.estado == Estado.LIBRE || e.estado == Estado.EN_SESION)) {
            e = e.copy(
                estado = Estado.EN_SESION,
                segSesion = e.segSesion + 1,
                segHoy = e.segHoy + 1,
                ultimoUsoMs = evento.ahoraMs,
            )

            if (e.segHoy >= limites.presupuestoMin * 60 + e.extraHoySeg) {
                e = e.copy(estado = Estado.SIN_PRESUPUESTO)
            } else if (e.segSesion >= limites.sesionMin * 60 + e.extraSesionSeg) {
                e = e.copy(
                    estado = Estado.ENFRIANDO,
                    finEnfriamientoMs = evento.ahoraMs + limites.enfriamientoMin * 60_000L,
                )
            }
        }
        e
    }
}

fun EstadoApp.bloqueada(): Boolean = estado == Estado.ENFRIANDO || estado == Estado.SIN_PRESUPUESTO
