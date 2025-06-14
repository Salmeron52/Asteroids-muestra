package com.buenhijogames.asteroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.ui.graphics.asAndroidPath

class JuegoSurfaceView(context: Context, private val motorJuego: MotorJuego) : SurfaceView(context), SurfaceHolder.Callback {

    private var hiloJuego: HiloJuego? = null
    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val paintBalaRoja = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    private val paintBalaBlanca = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
    }

    private val pathNave = Path().apply {
        moveTo(0f, -25f)
        lineTo(-15f, 15f)
        lineTo(15f, 15f)
        close()
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        hiloJuego = HiloJuego(holder, motorJuego, this)
        hiloJuego?.setJuegoCorriendo(true)
        hiloJuego?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // No es necesario en este caso
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var reintentar = true
        hiloJuego?.setJuegoCorriendo(false)
        while (reintentar) {
            try {
                hiloJuego?.join()
                reintentar = false
            } catch (e: InterruptedException) {
                // reintentar
            }
        }
    }

    fun dibujarEnCanvas(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)

        val estadoUI = motorJuego.estado

        // Dibujar Balas
        estadoUI.balas.forEach { bala ->
            val paintBala = if (bala.origen == OrigenBala.JUGADOR) paintBalaBlanca else paintBalaRoja
            canvas.drawCircle(bala.posX, bala.posY, bala.radio, paintBala)
        }
        // Dibujar Asteroides
        estadoUI.asteroides.forEach { asteroide ->
            canvas.save()
            canvas.translate(asteroide.posX, asteroide.posY)
            canvas.rotate(asteroide.rotacion)
            canvas.drawPath(asteroide.path.asAndroidPath(), paint)
            canvas.restore()
        }
        // Dibujar OVNI
        estadoUI.ovni?.let { o ->
            canvas.save()
            canvas.translate(o.posX, o.posY)
            canvas.drawPath(pathOvni.asAndroidPath(), paint)
            canvas.restore()
        }
        // Dibujar Nave
        if (estadoUI.vidas.value > 0) {
            val esVisible = if (estadoUI.nave.esInvulnerable) (System.currentTimeMillis() / 200) % 2 == 0L else true
            if (esVisible) {
                canvas.save()
                canvas.translate(estadoUI.nave.posX, estadoUI.nave.posY)
                canvas.rotate(estadoUI.nave.angulo)
                canvas.drawPath(pathNave, paint)
                canvas.restore()
            }
        }
    }
}

private class HiloJuego(
    private val surfaceHolder: SurfaceHolder,
    private val motorJuego: MotorJuego,
    private val juegoSurfaceView: JuegoSurfaceView
) : Thread() {

    private var corriendo = false
    private val tiempoFrameObjetivo = (1000 / 30).toLong() // 30 FPS

    fun setJuegoCorriendo(estaCorriendo: Boolean) {
        this.corriendo = estaCorriendo
    }

    override fun run() {
        var ultimoTiempoNanos = System.nanoTime()
        val nanosPorSegundo = 1_000_000_000.0
        val tiempoFrameObjetivoNanos = nanosPorSegundo / 30.0

        while (corriendo) {
            val tiempoInicioFrameNanos = System.nanoTime()
            val deltaTime = (tiempoInicioFrameNanos - ultimoTiempoNanos) / nanosPorSegundo.toFloat()
            ultimoTiempoNanos = tiempoInicioFrameNanos

            // 1. Actualiza la lógica del juego (fuera de cualquier bloqueo de canvas)
            // Evita saltos enormes si el juego se pausa y reanuda.
            val dtCorregido = if (deltaTime > 0.5f) 1f/60f else deltaTime
            motorJuego.tick(dtCorregido)

            // 2. Dibuja el estado resultante en el canvas
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        juegoSurfaceView.dibujarEnCanvas(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val tiempoProcesadoNanos = System.nanoTime() - tiempoInicioFrameNanos
            val tiempoEsperaNanos = (tiempoFrameObjetivoNanos - tiempoProcesadoNanos).toLong()

            if (tiempoEsperaNanos > 0) {
                try {
                    // Convertimos a milisegundos para sleep
                    sleep(tiempoEsperaNanos / 1_000_000)
                } catch (e: InterruptedException) {
                    // Thread interrumpido, podemos simplemente salir del bucle.
                    break
                }
            }
        }
    }
} 