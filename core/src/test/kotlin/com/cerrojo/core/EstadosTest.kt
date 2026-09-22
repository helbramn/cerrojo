package com.cerrojo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EstadosTest {
    private val limites = Limites(objetivoMin = 40, sesionMin = 10, enfriamientoMin = 40, presupuestoMin = 40)
    private val hoy = "2026-09-22"

    private fun tics(veces: Int, desde: EstadoApp, ahoraMs: Long = 0L, enPrimerPlano: Boolean = true): EstadoApp {
        var e = desde
        repeat(veces) { i -> e = avanzar(e, Evento.Tick(enPrimerPlano, ahoraMs + i * 1000L, hoy), limites) }
        return e
    }

    @Test fun `usar la app abre una sesion`() {
        val e = tics(1, EstadoApp())
        assertEquals(Estado.EN_SESION, e.estado)
        assertEquals(1, e.segSesion)
    }

    @Test fun `fuera de primer plano no corre el reloj`() {
        val e = tics(30, EstadoApp(), enPrimerPlano = false)
        assertEquals(Estado.LIBRE, e.estado)
        assertEquals(0, e.segSesion)
    }

    @Test fun `agotar la sesion pasa a enfriamiento`() {
        val e = tics(10 * 60, EstadoApp())
        assertEquals(Estado.ENFRIANDO, e.estado)
        assertEquals(40 * 60_000L, e.finEnfriamientoMs)
    }

    @Test fun `cumplido el enfriamiento vuelve a estar libre con la sesion a cero`() {
        val enfriando = tics(10 * 60, EstadoApp())
        val despues = avanzar(enfriando, Evento.Tick(false, 40 * 60_000L, hoy), limites)
        assertEquals(Estado.LIBRE, despues.estado)
        assertEquals(0, despues.segSesion)
    }

    @Test fun `agotar el presupuesto del dia bloquea hasta el dia siguiente`() {
        var e = EstadoApp()
        var ahora = 0L
        repeat(4) {
            e = tics(10 * 60, e, ahoraMs = ahora)
            ahora += 10 * 60 * 1000L
            e = avanzar(e, Evento.Tick(false, ahora + 40 * 60_000L, hoy), limites)
            ahora += 40 * 60_000L
        }
        assertEquals(Estado.SIN_PRESUPUESTO, e.estado)
        assertEquals(40 * 60, e.segHoy)
    }

    @Test fun `cambiar de dia lo reinicia todo`() {
        val gastado = tics(10 * 60, EstadoApp())
        val manana = avanzar(gastado, Evento.Tick(false, 0L, "2026-09-23"), limites)
        assertEquals(Estado.LIBRE, manana.estado)
        assertEquals(0, manana.segHoy)
        assertEquals(0, manana.segSesion)
    }

    @Test fun `el desbloqueo con friccion da cinco minutos mas`() {
        val enfriando = tics(10 * 60, EstadoApp())
        val desbloqueado = avanzar(enfriando, Evento.Desbloqueo(ahoraMs = 1_000L), limites)
        assertEquals(Estado.EN_SESION, desbloqueado.estado)

        val casi = tics(5 * 60 - 1, desbloqueado, ahoraMs = 2_000L)
        assertEquals(Estado.EN_SESION, casi.estado)
        val agotado = tics(1, casi, ahoraMs = 2_000L + (5 * 60 - 1) * 1000L)
        assertEquals(Estado.ENFRIANDO, agotado.estado)
    }
}
