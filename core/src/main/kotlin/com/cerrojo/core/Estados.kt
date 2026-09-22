package com.cerrojo.core

const val BONUS_DESBLOQUEO_SEG = 5 * 60

enum class Estado { LIBRE, EN_SESION, ENFRIANDO, SIN_PRESUPUESTO }

data class EstadoApp(
    val estado: Estado = Estado.LIBRE,
    val segSesion: Int = 0,
    val segHoy: Int = 0,
    val extraSesionSeg: Int = 0,
    val extraHoySeg: Int = 0,
    val finEnfriamientoMs: Long = 0L,
    val dia: String = "",
)

sealed interface Evento {
    data class Tick(val enPrimerPlano: Boolean, val ahoraMs: Long, val dia: String) : Evento
    data class Desbloqueo(val ahoraMs: Long) : Evento
}

fun avanzar(previo: EstadoApp, evento: Evento, limites: Limites): EstadoApp = when (evento) {
    is Evento.Desbloqueo -> previo.copy(
        estado = Estado.EN_SESION,
        extraSesionSeg = previo.extraSesionSeg + BONUS_DESBLOQUEO_SEG,
        extraHoySeg = previo.extraHoySeg + BONUS_DESBLOQUEO_SEG,
        finEnfriamientoMs = 0L,
    )

    is Evento.Tick -> {
        var e = if (evento.dia != previo.dia) {
            EstadoApp(dia = evento.dia)
        } else previo

        if (e.estado == Estado.ENFRIANDO && evento.ahoraMs >= e.finEnfriamientoMs) {
            e = e.copy(estado = Estado.LIBRE, segSesion = 0, extraSesionSeg = 0, finEnfriamientoMs = 0L)
        }

        if (evento.enPrimerPlano && (e.estado == Estado.LIBRE || e.estado == Estado.EN_SESION)) {
            e = e.copy(estado = Estado.EN_SESION, segSesion = e.segSesion + 1, segHoy = e.segHoy + 1)

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
