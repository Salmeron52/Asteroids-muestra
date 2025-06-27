package com.buenhijogames.asteroid

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.mutableStateOf

/**
 * Gestor de Sonido para encapsular la lógica de SoundPool.
 *
 * Esta clase se encarga de:
 * 1.  Configurar y crear una instancia de SoundPool optimizada para juegos.
 * 2.  Cargar todos los efectos de sonido de la carpeta `res/raw` en memoria.
 * 3.  Proporcionar métodos sencillos para reproducir los sonidos.
 * 4.  Liberar los recursos cuando ya no son necesarios.
 */
class GestorSonido(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val idSonidos: MutableMap<String, Int> = mutableMapOf()
    private val idExplosiones = mutableListOf<Int>()
    
    // Control para sonidos en loop (como el OVNI)
    private var idStreamSonidoOvni: Int? = null

    // El estado del volumen, observable por la UI. Inicia al 50%.
    var volumenMaestro = mutableStateOf(0.5f)

    /**
     * Carga todos los sonidos necesarios para el juego en el SoundPool.
     * Es importante llamar a este método antes de intentar reproducir cualquier sonido.
     */
    fun cargarSonidos() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(7) // Permitimos hasta 7 sonidos simultáneos
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            idSonidos["fire"] = pool.load(context, R.raw.fire, 1)
            idSonidos["thrust"] = pool.load(context, R.raw.thrust, 1)
            idSonidos["beat1"] = pool.load(context, R.raw.thumplo, 1)
            idSonidos["beat2"] = pool.load(context, R.raw.thumphi, 1)
            idSonidos["life"] = pool.load(context, R.raw.life, 1) // Sonido para vida extra

            // Sonidos de OVNI - usando los archivos originales de Asteroids
            idSonidos["ovni_grande"] = pool.load(context, R.raw.lsaucer, 1)
            idSonidos["ovni_pequeno"] = pool.load(context, R.raw.ssaucer, 1)

            // Cargamos las explosiones y las guardamos en una lista separada
            // para poder elegir una al azar fácilmente.
            idExplosiones.add(pool.load(context, R.raw.explode1, 1))
            idExplosiones.add(pool.load(context, R.raw.explode2, 1))
            idExplosiones.add(pool.load(context, R.raw.explode3, 1))
        }
    }

    /**
     * Reproduce un sonido específico basado en su nombre (la clave en el mapa).
     * @param nombre El nombre del sonido a reproducir (ej. "fire", "thrust").
     */
    fun reproducirSonido(nombre: String) {
        val idSonido = idSonidos[nombre]
        val volGlobal = volumenMaestro.value

        // Aplicamos un multiplicador específico para ciertos sonidos.
        // El disparo ("fire") sonará a la mitad de volumen que el resto.
        val multiplicadorEspecifico = when (nombre) {
            "fire" -> 0.5f
            else -> 1.0f
        }
        
        val volFinal = volGlobal * multiplicadorEspecifico

        if (volFinal > 0f) { // No reproducir si el volumen final es cero.
        idSonido?.let {
                soundPool?.play(it, volFinal, volFinal, 1, 0, 1f)
            }
        }
    }

    /**
     * Reproduce un sonido de explosión elegido al azar de los disponibles.
     */
    fun reproducirExplosion() {
        val vol = volumenMaestro.value
        if (vol > 0f && idExplosiones.isNotEmpty()) {
            val idAleatorio = idExplosiones.random()
            soundPool?.play(idAleatorio, vol, vol, 1, 0, 1f)
        }
    }

    /**
     * Comienza a reproducir el sonido del OVNI en bucle mientras está en pantalla.
     * Solo un sonido de OVNI puede estar activo a la vez.
     * 
     * @param tipoSonido El tipo de sonido del OVNI ("ovni_grande" o "ovni_pequeno")
     */
    fun iniciarSonidoOvni(tipoSonido: String) {
        // Primero detenemos cualquier sonido de OVNI que esté sonando
        detenerSonidoOvni()
        
        val volGlobal = volumenMaestro.value
        // Los sonidos de OVNI siempre suenan a la mitad del volumen global.
        val volFinal = volGlobal * 0.5f
        
        // Iniciamos el sonido SIEMPRE para obtener un streamID, incluso si el volumen es 0.
        // Así podemos volver a subirlo más tarde si el usuario cambia el volumen.
        val idSonido = idSonidos[tipoSonido]
        idSonido?.let {
            // Reproducimos en loop (-1 = loop infinito)
            idStreamSonidoOvni = soundPool?.play(it, volFinal, volFinal, 1, -1, 1f)
        }
    }

    /**
     * Detiene el sonido del OVNI si está reproduciéndose.
     */
    fun detenerSonidoOvni() {
        idStreamSonidoOvni?.let { streamId ->
            soundPool?.stop(streamId)
            idStreamSonidoOvni = null
        }
    }

    /**
     * Establece un nuevo nivel de volumen maestro y lo aplica a los sonidos activos.
     * @param nuevoVolumen El nuevo nivel de volumen, un Float entre 0.0 y 1.0.
     */
    fun setVolumen(nuevoVolumen: Float) {
        // Aseguramos que el volumen esté siempre en el rango [0.0, 1.0]
        val volumenAjustado = nuevoVolumen.coerceIn(0f, 1f)
        volumenMaestro.value = volumenAjustado

        // Ahora, actualiza el volumen del sonido del OVNI si está sonando.
        idStreamSonidoOvni?.let { streamId ->
            // El OVNI siempre suena a la mitad del volumen global.
            val volFinalOvni = volumenAjustado * 0.5f
            soundPool?.setVolume(streamId, volFinalOvni, volFinalOvni)
        }
    }

    /**
     * Libera los recursos de SoundPool para evitar fugas de memoria.
     * Debe llamarse cuando el gestor ya no se vaya a utilizar (ej. en onDispose o onDestroy).
     */
    fun liberarRecursos() {
        detenerSonidoOvni() // Asegurar que el sonido del OVNI se detenga
        soundPool?.release()
        soundPool = null
    }
} 