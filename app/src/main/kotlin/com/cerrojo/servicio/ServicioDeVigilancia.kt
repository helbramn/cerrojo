package com.cerrojo.servicio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.cerrojo.core.Evento
import com.cerrojo.core.bloqueada
import com.cerrojo.core.avanzar
import com.cerrojo.core.limitesDe
import com.cerrojo.core.mediaDeUso
import com.cerrojo.core.semana
import com.cerrojo.datos.Almacen
import com.cerrojo.sistema.LectorDeUso
import com.cerrojo.ui.PantallaDeBloqueo
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private const val CANAL = "vigilancia"
private const val ID_NOTIFICACION = 1
private const val PERIODO_MS = 1_000L
private const val LATIDO_MS = 10_000L

/**
 * Android 10+ descarta en silencio el arranque de una Activity desde segundo
 * plano si falta el permiso de superposicion. Si tras este numero de vueltas la
 * app bloqueada sigue delante, es que la pantalla no llego a aparecer: se
 * reintenta en vez de darla por mostrada.
 */
private const val TICS_PARA_REINTENTAR_BLOQUEO = 3

class ServicioDeVigilancia : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var almacen: Almacen
    private lateinit var lector: LectorDeUso
    private val formatoDia = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var ultimoAvisoDeLatido = 0L
    private var bloqueoMostradoPara: String? = null
    private var ticsDesdeElBloqueo = 0

    @Volatile
    private var recalculando = false

    override fun onCreate() {
        super.onCreate()
        almacen = Almacen(this)
        lector = LectorDeUso(this)
        crearCanal()
        ServiceCompat.startForeground(
            this, ID_NOTIFICACION, notificacion("arrancando…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        handler.post(vuelta)
    }

    private val vuelta = object : Runnable {
        override fun run() {
            try { comprobar() } catch (_: Exception) { }
            handler.postDelayed(this, PERIODO_MS)
        }
    }

    private fun comprobar() {
        val ahora = System.currentTimeMillis()
        val dia = diaLogico(ahora)
        // Con la pantalla apagada el sistema sigue diciendo cual fue la ultima
        // app en primer plano; sin esta comprobacion el reloj correria con el
        // movil en el bolsillo (spec 4.2).
        val encendida = getSystemService(PowerManager::class.java).isInteractive
        val delante = if (encendida) lector.appEnPrimerPlano() else null
        revisarSemana()

        for (paquete in almacen.appsVigiladas()) {
            val limites = almacen.limites(paquete) ?: continue
            val previo = almacen.estado(paquete)
            val nuevo = avanzar(previo, Evento.Tick(paquete == delante, ahora, dia), limites)
            if (nuevo != previo) almacen.guardarEstado(paquete, nuevo)

            // Solo al entrar en bloqueo: la vuelta es cada segundo y relanzar
            // la Activity 60 veces por minuto se comeria la bateria y pisaria
            // la cuenta atras de los 45 s. Pero si la app bloqueada sigue
            // delante varias vueltas despues, la pantalla no llego a salir y
            // hay que reintentarlo: darla por mostrada dejaria la app sin
            // bloquear hasta que el usuario cambiase de aplicacion.
            if (paquete == delante && nuevo.bloqueada()) {
                if (bloqueoMostradoPara != paquete) {
                    bloqueoMostradoPara = paquete
                    ticsDesdeElBloqueo = 0
                    PantallaDeBloqueo.mostrar(this, paquete, nuevo.estado)
                } else if (++ticsDesdeElBloqueo >= TICS_PARA_REINTENTAR_BLOQUEO) {
                    bloqueoMostradoPara = null
                }
            } else if (bloqueoMostradoPara == paquete) {
                bloqueoMostradoPara = null
            }
        }

        if (ahora - ultimoAvisoDeLatido > LATIDO_MS) {
            ultimoAvisoDeLatido = ahora
            almacen.ultimaComprobacionMs = ahora
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(ID_NOTIFICACION, notificacion(textoDeLatido(ahora)))
        }
    }

    /**
     * El latido lleva la hora de la ultima comprobacion, no un texto fijo: una
     * notificacion identica cada vez no distingue un servicio vivo de uno
     * atascado, y comprobar que sigue en pie es justamente para lo que esta.
     *
     * Si MIUI revoca el acceso al uso —lo hace— las consultas dejan de devolver
     * eventos sin lanzar ninguna excepcion: nada se bloquearia y nada lo diria.
     * Por eso el latido tambien vigila el permiso.
     */
    private fun textoDeLatido(ahora: Long): String =
        if (!lector.tienePermisoDeUso()) "sin permiso de uso — abre Cerrojo"
        else "vigilando · última comprobación ${formatoHora.format(Date(ahora))}"

    /** El dia nuevo empieza a la hora de reinicio configurada, no a medianoche. */
    private fun diaLogico(ahora: Long): String =
        formatoDia.format(Date(ahora - almacen.horaDeReinicioH * 3_600_000L))

    /**
     * Recalcular cuesta 14 consultas por app, asi que solo se hace cuando el
     * numero de semana cambia de verdad, o cuando falta algun limite porque se
     * acaba de anadir una app. En su propio hilo para no frenar la vuelta de
     * cada segundo.
     */
    private fun revisarSemana() {
        // Sin esta guarda se lanzaria un hilo por vuelta mientras el anterior
        // sigue trabajando: el recalculo tarda segundos (14 consultas por app)
        // y durante ese rato los limites aun no estan completos, asi que cada
        // tic arrancaria otro recalculo. Justo al elegir apps por primera vez.
        if (recalculando) return
        val instalacion = Instant.ofEpochMilli(almacen.instaladoEl)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val n = semana(instalacion, LocalDate.now())
        val completos = almacen.appsVigiladas().all { almacen.limites(it) != null }
        if (n == almacen.semanaDeLosLimites && completos) return
        almacen.semanaDeLosLimites = n
        recalculando = true
        Thread {
            try {
                for (paquete in almacen.appsVigiladas()) {
                    almacen.guardarLimites(
                        paquete,
                        limitesDe(mediaDeUso(lector.minutosPorDia(paquete)), n, almacen.suelo(paquete)),
                    )
                }
            } catch (_: Exception) {
                // Una excepcion sin capturar en un Thread pelado mata el proceso
                // entero, y START_STICKY lo reiniciaria contra el mismo fallo.
            } finally {
                recalculando = false
            }
        }.start()
    }

    private fun crearCanal() {
        val canal = NotificationChannel(CANAL, "Vigilancia", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun notificacion(texto: String): Notification =
        NotificationCompat.Builder(this, CANAL)
            .setContentTitle("Cerrojo")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { handler.removeCallbacks(vuelta); super.onDestroy() }

    companion object {
        fun arrancar(context: Context) {
            context.startForegroundService(Intent(context, ServicioDeVigilancia::class.java))
        }
    }
}
