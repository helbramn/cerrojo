package com.cerrojo.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LimitesTest {
    @Test fun `la media es la mediana de los dias con datos`() {
        assertEquals(30, mediaDeUso(listOf(10, 30, 90, 25, 35)))
    }

    @Test fun `sin historial suficiente usa el valor por defecto`() {
        assertEquals(MEDIA_POR_DEFECTO_MIN, mediaDeUso(listOf(80, 90)))
    }

    @Test fun `la primera semana el objetivo es la media`() {
        val l = limitesDe(media = 80, semana = 1)
        assertEquals(80, l.objetivoMin)
        assertEquals(20, l.sesionMin)
        assertEquals(80, l.enfriamientoMin)
        assertEquals(80, l.presupuestoMin)
    }

    @Test fun `el objetivo baja un diez por ciento por semana`() {
        assertEquals(52, limitesDe(80, 5).objetivoMin)
        assertEquals(34, limitesDe(80, 9).objetivoMin)
    }

    @Test fun `el objetivo nunca baja del suelo`() {
        assertEquals(20, limitesDe(80, 14).objetivoMin)
        assertEquals(20, limitesDe(80, 40).objetivoMin)
        assertEquals(45, limitesDe(80, 40, sueloMin = 45).objetivoMin)
    }

    @Test fun `la semana avanza los lunes, no a los siete dias de instalar`() {
        val juevesDeInstalacion = LocalDate.of(2026, 9, 24)
        assertEquals(1, semana(juevesDeInstalacion, LocalDate.of(2026, 9, 27)))
        assertEquals(2, semana(juevesDeInstalacion, LocalDate.of(2026, 9, 28)))
        assertEquals(3, semana(juevesDeInstalacion, LocalDate.of(2026, 10, 5)))
    }

    @Test fun `la sesion y el enfriamiento se mantienen dentro de sus topes`() {
        val corto = limitesDe(media = 8, semana = 1)
        assertEquals(5, corto.sesionMin)
        assertEquals(20, corto.enfriamientoMin)

        val largo = limitesDe(media = 600, semana = 1)
        assertEquals(20, largo.sesionMin)
        assertEquals(90, largo.enfriamientoMin)
    }
}
