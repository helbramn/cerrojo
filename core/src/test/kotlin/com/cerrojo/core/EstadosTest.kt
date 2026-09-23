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
        // El enfriamiento cuenta desde el instante del tic que agota la sesion.
        // Los 600 tics van de 0 a 599_000 ms, asi que ese instante es 599_000,
        // no 600_000: el reloj arranca en el primer tic, no antes.
        assertEquals(599_000L + 40 * 60_000L, e.finEnfriamientoMs)
    }

    @Test fun `cumplido el enfriamiento vuelve a estar libre con la sesion a cero`() {
        val enfriando = tics(10 * 60, EstadoApp())
        // Justo en el instante de vencimiento ya esta libre, no un tic despues.
        val despues = avanzar(enfriando, Evento.Tick(false, enfriando.finEnfriamientoMs, hoy), limites)
        assertEquals(Estado.LIBRE, despues.estado)
        assertEquals(0, despues.segSesion)
    }

    /** Consume los 40 minutos de presupuesto del dia en cuatro sesiones. */
    private fun sinPresupuesto(): EstadoApp {
        var e = EstadoApp()
        var ahora = 0L
        repeat(4) {
            e = tics(10 * 60, e, ahoraMs = ahora)
            ahora += 10 * 60 * 1000L
            e = avanzar(e, Evento.Tick(false, ahora + 40 * 60_000L, hoy), limites)
            ahora += 40 * 60_000L
        }
        return e
    }

    @Test fun `agotar el presupuesto del dia bloquea hasta el dia siguiente`() {
        val e = sinPresupuesto()
        assertEquals(Estado.SIN_PRESUPUESTO, e.estado)
        assertEquals(40 * 60, e.segHoy)
    }

    @Test fun `salir de la app congela el reloj de sesion, no lo reinicia`() {
        val enMarcha = tics(120, EstadoApp())
        assertEquals(120, enMarcha.segSesion)

        val fuera = tics(60, enMarcha, ahoraMs = 120_000L, enPrimerPlano = false)
        assertEquals(120, fuera.segSesion)

        // Si volver reiniciara la sesion, el limite se esquivaria saltando de
        // app y volviendo.
        val vuelta = tics(1, fuera, ahoraMs = 180_000L)
        assertEquals(121, vuelta.segSesion)
    }

    @Test fun `sin presupuesto tambien se reinicia al cambiar de dia`() {
        val agotado = sinPresupuesto()
        val manana = avanzar(agotado, Evento.Tick(false, 0L, "2026-09-23"), limites)
        assertEquals(Estado.LIBRE, manana.estado)
        assertEquals(0, manana.segHoy)
    }

    @Test fun `sin presupuesto tambien se puede desbloquear con friccion`() {
        val agotado = sinPresupuesto()
        val desbloqueado = avanzar(agotado, Evento.Desbloqueo, limites)
        assertEquals(Estado.EN_SESION, desbloqueado.estado)

        // Los cinco minutos tienen que llegar a los dos relojes: si solo
        // subiera el de sesion, el presupuesto del dia volveria a cortar al
        // primer tic.
        val casi = tics(5 * 60 - 1, desbloqueado, ahoraMs = 1_000L)
        assertEquals(Estado.EN_SESION, casi.estado)
    }

    @Test fun `el desbloqueo no hace nada si la app no esta bloqueada`() {
        val libre = tics(5, EstadoApp())
        assertEquals(libre, avanzar(libre, Evento.Desbloqueo, limites))
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
        val desbloqueado = avanzar(enfriando, Evento.Desbloqueo, limites)
        assertEquals(Estado.EN_SESION, desbloqueado.estado)

        val casi = tics(5 * 60 - 1, desbloqueado, ahoraMs = 2_000L)
        assertEquals(Estado.EN_SESION, casi.estado)
        val agotado = tics(1, casi, ahoraMs = 2_000L + (5 * 60 - 1) * 1000L)
        assertEquals(Estado.ENFRIANDO, agotado.estado)
    }
}
