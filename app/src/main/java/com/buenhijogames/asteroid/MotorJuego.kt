package com.buenhijogames.asteroid

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Fichero: MotorJuego.kt

// --- CLASES DE ESTADO PURAS ---
// Estas clases contienen los datos del juego. Usan variables estándar (`var`)
// en lugar de `mutableStateOf` para que su modificación no cause recomposiciones.
// El motor se encargará de leer y escribir estos datos en cada fotograma.

data class EstadoNave(
    var posX: Float = 0f,
    var posY: Float = 0f,
    var velX: Float = 0f,
    var velY: Float = 0f,
    var angulo: Float = 0f,
    var esInvulnerable: Boolean = false,
    var tiempoInvulnerableRestante: Float = 0f,
)

// Reutilizamos las data classes existentes (Bala, Asteroide, Ovni) ya que sus propiedades
// son `var` y se ajustan a este modelo. No es necesario duplicarlas.

// La clase principal que aglutina todo el estado del juego.
// Una única instancia de esta clase será el "mundo" de nuestro juego.
class EstadoJuego {
    val nave = EstadoNave()
    val balas = mutableListOf<Bala>()
    val asteroides = mutableListOf<Asteroide>()
    var ovni: Ovni? = null

    // La puntuación y las vidas sí pueden ser `mutableStateOf` porque queremos que la UI
    // (los Text) se recomponga CUANDO y SÓLO CUANDO cambien. Estos cambios son infrecuentes.
    var puntuacion = mutableStateOf(0)
    var vidas = mutableStateOf(3)
    var nivel = mutableStateOf(1)
    var estadoActual = mutableStateOf(EstadoJuegoEnum.JUGANDO) // Usaremos un enum más explícito
    var enPausa = mutableStateOf(false) // Estado de pausa del juego
    var puntuacionParaSiguienteVida: Int = PUNTUACION_PRIMERA_VIDA_EXTRA

    // Variables de control del jugador (entradas)
    var factorRotacion: Float = 0f
    var acelerando: Boolean = false
    var disparando: Boolean = false

    // Timers y variables internas que no necesita la UI
    var tiempoParaSiguienteOvni: Float = 8.0f
    var tiempoParaLatido: Float = 0f
    var siguienteLatido: String = "beat1"
    var tiempoDesdeUltimoDisparo: Float = 0f

    var listaRecords = mutableStateOf<List<PuntuacionRecord>>(listOf())
}

// Un enum más descriptivo para los estados del juego
enum class EstadoJuegoEnum {
    JUGANDO,
    GAME_OVER,
    NUEVO_RECORD
}

// --- MOTOR DEL JUEGO ---
// La clase principal que contiene toda la lógica.
class MotorJuego(
    private val gestorSonido: GestorSonido,
    private val gestorPuntuacion: GestorPuntuacion,
) {
    // El motor tiene su propia instancia interna del estado del juego.
    val estado = EstadoJuego()
    private var tamanoCanvas: Size = Size.Zero
    private val estadoLock = Any() // Objeto para sincronizar el acceso al estado

    // Expone el estado del volumen directamente desde el GestorSonido.
    // La UI puede observar este estado para reaccionar a los cambios de volumen.
    val estadoVolumen = gestorSonido.estadoVolumen

    // Listas para optimización del bucle, para no crearlas en cada tick
    private val balasAeliminar = mutableSetOf<Bala>()
    private val asteroidesAeliminar = mutableSetOf<Asteroide>()
    private val asteroidesAnadir = mutableListOf<Asteroide>()

    // --- MÉTODOS PÚBLICOS DE CONTROL (API para la UI) ---

    fun establecerLimites(width: Float, height: Float) = synchronized(estadoLock) {
        tamanoCanvas = Size(width, height)
        estado.nave.posX = width / 2
        estado.nave.posY = height / 2
        inicializarNivel()
    }

    fun establecerEmpujeNave(activo: Boolean) = synchronized(estadoLock) {
        estado.acelerando = activo
    }

    fun establecerRotacionNave(factor: Float) = synchronized(estadoLock) {
        estado.factorRotacion = factor
    }

    fun disparar() = synchronized(estadoLock) {
        estado.disparando = true // El motor lo gestionará en el tick
    }

    fun ejecutarHiperespacio() = synchronized(estadoLock) {
        if (estado.nave.esInvulnerable) return@synchronized
        estado.nave.posX = Random.nextFloat() * tamanoCanvas.width
        estado.nave.posY = Random.nextFloat() * tamanoCanvas.height
        estado.nave.velX = 0f
        estado.nave.velY = 0f
        estado.nave.tiempoInvulnerableRestante = TIEMPO_INVULNERABLE_SEGUNDOS
        estado.nave.esInvulnerable = true
        // TODO: Añadir sonido del hiperespacio en el futuro
    }

    // --- MÉTODOS DEL CICLO DE VIDA ---

    fun iniciar() {
        gestorSonido.cargarSonidos()
        estado.listaRecords.value = gestorPuntuacion.cargarPuntuaciones()
    }

    fun reiniciarJuego() = synchronized(estadoLock) {
        estado.nivel.value = 1
        estado.vidas.value = 3
        estado.puntuacion.value = 0
        estado.puntuacionParaSiguienteVida = PUNTUACION_PRIMERA_VIDA_EXTRA
        estado.nave.esInvulnerable = false
        estado.nave.tiempoInvulnerableRestante = 0f
        estado.estadoActual.value = EstadoJuegoEnum.JUGANDO
        estado.enPausa.value = false // Asegurar que el juego no esté en pausa al reiniciar
        gestorSonido.detenerSonidoOvni() // Detener cualquier sonido del OVNI al reiniciar
        inicializarNivel()
    }

    fun liberarRecursos() {
        gestorSonido.liberarRecursos()
    }

    fun guardarNuevoRecord(iniciales: String) = synchronized(estadoLock) {
        val nuevaLista = estado.listaRecords.value.toMutableList()
        nuevaLista.add(PuntuacionRecord(iniciales = iniciales, puntos = estado.puntuacion.value))
        // Ordena la lista de mayor a menor puntuación y coge los 30 primeros.
        val listaOrdenada = nuevaLista.sortedByDescending { it.puntos }.take(30)
        gestorPuntuacion.guardarPuntuaciones(listaOrdenada)
        estado.listaRecords.value = listaOrdenada
        estado.estadoActual.value = EstadoJuegoEnum.GAME_OVER // Vuelve a la pantalla de Game Over
    }

    private fun esPuntuacionDeRecord(puntuacion: Int): Boolean {
        val records = estado.listaRecords.value
        // Hay hueco si la lista no está llena (menos de 30) o si la puntuación
        // es mayor que la más baja de la lista.
        return records.size < 30 || puntuacion > (records.lastOrNull()?.puntos ?: 0)
    }

    /**
     * Obtiene la puntuación más alta registrada (el record).
     * Esta función es útil para mostrar el record en la interfaz de usuario.
     * 
     * @return La puntuación más alta registrada, o 0 si no hay records.
     */
    fun obtenerRecordMaximo(): Int {
        return estado.listaRecords.value.maxOfOrNull { it.puntos } ?: 0
    }

    /**
     * Alterna el estado de pausa del juego.
     * Si el juego está en pausa, lo reanuda. Si está corriendo, lo pausa.
     * También gestiona el sonido del OVNI.
     */
    fun alternarPausa() {
        estado.enPausa.value = !estado.enPausa.value
        if (estado.enPausa.value) {
            gestorSonido.detenerSonidoOvni()
        } else {
            // Si hay un OVNI al reanudar, reiniciamos su sonido
            estado.ovni?.let {
                gestorSonido.iniciarSonidoOvni(it.sonido)
            }
        }
    }

    /**
     * Pausa el juego si no está ya en pausa.
     * Ideal para ser llamado desde eventos del ciclo de vida de la app (ej. onPause).
     */
    fun pausarJuego() {
        if (!estado.enPausa.value) {
            alternarPausa()
        }
    }

    /**
     * Delega la acción de ciclar el volumen al gestor de sonido.
     */
    fun ciclarVolumen() {
        gestorSonido.ciclarVolumen()
    }

    // Este es el corazón del motor. Se llamará en cada fotograma.
    fun tick(deltaTime: Float) = synchronized(estadoLock) {
        if (estado.estadoActual.value != EstadoJuegoEnum.JUGANDO || estado.enPausa.value) {
            // Si no estamos jugando o el juego está en pausa, nos aseguramos de que el disparo se desactive
            // para no procesar un disparo pendiente al reiniciar.
            if (estado.disparando) estado.disparando = false
            return
        }

        // --- LÓGICA DE MOVIMIENTO Y ACCIONES ---
        actualizarNave(deltaTime)
        actualizarAsteroides(deltaTime)
        actualizarOvni(deltaTime)
        actualizarBalas(deltaTime)

        // --- TIMERS Y EVENTOS PERIÓDICOS ---
        gestionarTimers(deltaTime)

        // --- LÓGICA DE COLISIONES ---
        detectarColisiones(deltaTime)

        // --- LÓGICA DE NIVEL ---
        comprobarSiguienteNivel()
    }

    private fun actualizarNave(deltaTime: Float) {
        val nave = estado.nave
        nave.angulo += estado.factorRotacion * GRADOS_ROTACION_POR_SEGUNDO * deltaTime
        if (estado.acelerando) {
            val rads = Math.toRadians(nave.angulo - 90.0)
            nave.velX += cos(rads).toFloat() * FUERZA_EMPUJE_POR_SEGUNDO * deltaTime
            nave.velY += sin(rads).toFloat() * FUERZA_EMPUJE_POR_SEGUNDO * deltaTime
            gestorSonido.reproducirSonido("thrust")
        }

        nave.velX -= nave.velX * FACTOR_FRICCION_POR_SEGUNDO * deltaTime
        nave.velY -= nave.velY * FACTOR_FRICCION_POR_SEGUNDO * deltaTime

        if (nave.velX * nave.velX + nave.velY * nave.velY < 1f) {
            nave.velX = 0f
            nave.velY = 0f
        }

        val velocidadCuadrada = nave.velX * nave.velX + nave.velY * nave.velY
        if (velocidadCuadrada > MAX_VELOCIDAD_NAVE * MAX_VELOCIDAD_NAVE) {
            val factor = MAX_VELOCIDAD_NAVE / sqrt(velocidadCuadrada)
            nave.velX *= factor
            nave.velY *= factor
        }

        if (estado.disparando) {
            if (estado.tiempoDesdeUltimoDisparo >= RETRASO_DISPARO_SEGUNDOS) {
                val rads = Math.toRadians(nave.angulo - 90.0)
                estado.balas.add(
                    Bala(
                        posX = nave.posX,
                        posY = nave.posY,
                        velX = cos(rads).toFloat() * VELOCIDAD_BALA_POR_SEGUNDO,
                        velY = sin(rads).toFloat() * VELOCIDAD_BALA_POR_SEGUNDO,
                        origen = OrigenBala.JUGADOR
                    )
                )
                gestorSonido.reproducirSonido("fire")
                estado.tiempoDesdeUltimoDisparo = 0f
            }
            // Importante: Resetear el estado de disparo después de procesarlo.
            estado.disparando = false
        }
        estado.tiempoDesdeUltimoDisparo += deltaTime

        nave.posX += nave.velX * deltaTime
        nave.posY += nave.velY * deltaTime

        if (nave.posX < 0) nave.posX = tamanoCanvas.width
        if (nave.posX > tamanoCanvas.width) nave.posX = 0f
        if (nave.posY < 0) nave.posY = tamanoCanvas.height
        if (nave.posY > tamanoCanvas.height) nave.posY = 0f
    }

    private fun actualizarAsteroides(deltaTime: Float) {
        estado.asteroides.forEach { ast ->
            ast.posX += ast.velX * deltaTime
            ast.posY += ast.velY * deltaTime
            ast.rotacion += ast.velocidadRotacion * deltaTime
            if (ast.posX < -ast.tamano.radio) ast.posX = tamanoCanvas.width + ast.tamano.radio
            if (ast.posX > tamanoCanvas.width + ast.tamano.radio) ast.posX = -ast.tamano.radio
            if (ast.posY < -ast.tamano.radio) ast.posY = tamanoCanvas.height + ast.tamano.radio
            if (ast.posY > tamanoCanvas.height + ast.tamano.radio) ast.posY = -ast.tamano.radio
        }
    }

    private fun actualizarOvni(deltaTime: Float) {
        estado.ovni?.let { o ->
            o.posX += o.velX * deltaTime
            o.tiempoParaDisparo -= deltaTime
            if (o.tiempoParaDisparo <= 0f) {
                val anguloRads = atan2(estado.nave.posY - o.posY, estado.nave.posX - o.posX)
                estado.balas.add(
                    Bala(
                        o.posX,
                        o.posY,
                        cos(anguloRads) * VELOCIDAD_BALA_POR_SEGUNDO * 0.7f,
                        sin(anguloRads) * VELOCIDAD_BALA_POR_SEGUNDO * 0.7f,
                        OrigenBala.OVNI
                    )
                )
                o.tiempoParaDisparo = Random.nextFloat() * 3f + 2f
            }
            if (o.posX < -o.radio || o.posX > tamanoCanvas.width + o.radio) {
                gestorSonido.detenerSonidoOvni()
                estado.ovni = null
            }
        }
    }

    private fun actualizarBalas(deltaTime: Float) {
        estado.balas.forEach {
            it.posX += it.velX * deltaTime
            it.posY += it.velY * deltaTime
            it.vidaUtil--
        }
    }

    private fun gestionarTimers(deltaTime: Float) {
        if (estado.nave.tiempoInvulnerableRestante > 0) {
            estado.nave.tiempoInvulnerableRestante -= deltaTime
            estado.nave.esInvulnerable = estado.nave.tiempoInvulnerableRestante > 0
        }

        if (estado.tiempoParaLatido <= 0) {
            gestorSonido.reproducirSonido(estado.siguienteLatido)
            estado.siguienteLatido = if (estado.siguienteLatido == "beat1") "beat2" else "beat1"
            estado.tiempoParaLatido = (20 + estado.asteroides.size * 10) / 60f
        } else {
            estado.tiempoParaLatido -= deltaTime
        }

        if (estado.ovni == null) {
            if (estado.tiempoParaSiguienteOvni > 0) {
                estado.tiempoParaSiguienteOvni -= deltaTime
            } else {
                // Decidir tipo de OVNI: más probabilidad de OVNI grande en niveles bajos
                val tipoOvni = if (estado.nivel.value <= 3) {
                    // Niveles 1-3: 70% grande, 30% pequeño
                    if (Random.nextFloat() < 0.7f) TipoOvni.GRANDE else TipoOvni.PEQUENO
                } else {
                    // Niveles 4+: 40% grande, 60% pequeño
                    if (Random.nextFloat() < 0.4f) TipoOvni.GRANDE else TipoOvni.PEQUENO
                }
                
                val aparecePorIzquierda = Random.nextBoolean()
                val velocidadBaseOvni = if (aparecePorIzquierda) {
                    MAX_VELOCIDAD_ASTEROIDE_POR_SEGUNDO * 0.8f
                } else {
                    -MAX_VELOCIDAD_ASTEROIDE_POR_SEGUNDO * 0.8f
                }
                
                // Los OVNI pequeños van más rápido que los grandes
                val multiplicadorVelocidad = if (tipoOvni == TipoOvni.PEQUENO) 1.4f else 1.0f
                val velocidadOvniAjustada = velocidadBaseOvni * multiplicadorVelocidad * 
                    (1 + estado.nivel.value * FACTOR_VELOCIDAD_OVNI_POR_NIVEL)

                val nuevoOvni = Ovni(
                    posX = if (aparecePorIzquierda) -tipoOvni.radio else tamanoCanvas.width + tipoOvni.radio,
                    posY = Random.nextFloat() * tamanoCanvas.height * 0.8f,
                    velX = velocidadOvniAjustada,
                    tipo = tipoOvni
                )
                
                estado.ovni = nuevoOvni
                // Iniciar el sonido del OVNI correspondiente
                gestorSonido.iniciarSonidoOvni(nuevoOvni.sonido)
                estado.tiempoParaSiguienteOvni = Random.nextDouble(8.0, 15.0).toFloat()
            }
        }
    }

    private fun detectarColisiones(deltaTime: Float) {
        balasAeliminar.clear()
        asteroidesAeliminar.clear()
        asteroidesAnadir.clear()

        // Colisiones de Balas
        for (bala in estado.balas) {
            if (balasAeliminar.contains(bala)) continue

            val posX_vieja = bala.posX
            val posY_vieja = bala.posY
            val deltaX = bala.velX * deltaTime
            val deltaY = bala.velY * deltaTime
            val distancia = sqrt(deltaX * deltaX + deltaY * deltaY)
            val numPasos = ceil(distancia / bala.radio).toInt().coerceAtLeast(1)

            if (bala.vidaUtil <= 0 || bala.posX < 0 || bala.posX > tamanoCanvas.width || bala.posY < 0 || bala.posY > tamanoCanvas.height) {
                balasAeliminar.add(bala)
                continue
            }

            var colisionDetectadaBala = false

            for (paso in 1..numPasos) {
                if (colisionDetectadaBala) break
                val fraccion = paso.toFloat() / numPasos
                val posX_intermedia = posX_vieja + deltaX * fraccion
                val posY_intermedia = posY_vieja + deltaY * fraccion

                for (asteroide in estado.asteroides) {
                    if (asteroidesAeliminar.contains(asteroide)) continue
                    if (hayColision(
                            posX_intermedia,
                            posY_intermedia,
                            bala.radio,
                            asteroide.posX,
                            asteroide.posY,
                            asteroide.tamano.radio
                        ) &&
                        comprobarVerticeEnAsteroide(
                            posX_intermedia,
                            posY_intermedia,
                            asteroide,
                            true
                        )
                    ) {
                        balasAeliminar.add(bala)
                        asteroidesAeliminar.add(asteroide)
                        actualizarPuntuacion(asteroide.tamano.puntos)
                        gestorSonido.reproducirExplosion()
                        if (asteroide.tamano != TamanoAsteroide.PEQUENO) {
                            val nuevoTamano =
                                if (asteroide.tamano == TamanoAsteroide.GRANDE) TamanoAsteroide.MEDIANO else TamanoAsteroide.PEQUENO
                            asteroidesAnadir.addAll(
                                listOf(
                                    crearAsteroideFragmento(
                                        asteroide,
                                        nuevoTamano
                                    ), crearAsteroideFragmento(asteroide, nuevoTamano)
                                )
                            )
                        }
                        colisionDetectadaBala = true
                        break
                    }
                }
            }

            if (colisionDetectadaBala) continue

            estado.ovni?.let { o ->
                if (bala.origen == OrigenBala.JUGADOR && hayColision(
                        bala.posX,
                        bala.posY,
                        bala.radio,
                        o.posX,
                        o.posY,
                        o.radio
                    )
                ) {
                    actualizarPuntuacion(o.puntos)
                    gestorSonido.detenerSonidoOvni()
                    estado.ovni = null
                    balasAeliminar.add(bala)
                    gestorSonido.reproducirExplosion()
                }
            }
        }

        // Colisiones de la Nave
        var naveDestruida = false
        if (!estado.nave.esInvulnerable) {
            for (asteroide in estado.asteroides) {
                if (asteroidesAeliminar.contains(asteroide)) continue
                if (hayColisionNaveAsteroide(estado.nave, asteroide)) {
                    if (!estado.nave.esInvulnerable) {
                        estado.nave.esInvulnerable = true
                        estado.nave.tiempoInvulnerableRestante = TIEMPO_INVULNERABLE_SEGUNDOS
                        estado.vidas.value--
                        gestorSonido.reproducirExplosion()
                        if (estado.vidas.value <= 0) {
                            gestorSonido.detenerSonidoOvni() // Detener sonido al terminar partida
                            if (esPuntuacionDeRecord(estado.puntuacion.value)) {
                                estado.estadoActual.value = EstadoJuegoEnum.NUEVO_RECORD
                            } else {
                                estado.estadoActual.value = EstadoJuegoEnum.GAME_OVER
                            }
                        }
                    }
                    asteroidesAeliminar.add(asteroide)
                    break
                }
            }
            if (!naveDestruida) {
                for (bala in estado.balas) {
                    if (balasAeliminar.contains(bala)) continue
                    if (bala.origen == OrigenBala.OVNI && hayColision(
                            estado.nave.posX,
                            estado.nave.posY,
                            RADIO_NAVE,
                            bala.posX,
                            bala.posY,
                            bala.radio
                        )
                    ) {
                        estado.nave.esInvulnerable = true
                        estado.nave.tiempoInvulnerableRestante = TIEMPO_INVULNERABLE_SEGUNDOS
                        estado.vidas.value--
                        gestorSonido.reproducirExplosion()
                        balasAeliminar.add(bala)
                        if (estado.vidas.value <= 0) {
                            gestorSonido.detenerSonidoOvni() // Detener sonido al terminar partida
                            if (esPuntuacionDeRecord(estado.puntuacion.value)) {
                                estado.estadoActual.value = EstadoJuegoEnum.NUEVO_RECORD
                            } else {
                                estado.estadoActual.value = EstadoJuegoEnum.GAME_OVER
                            }
                        }
                        break
                    }
                }
            }
            if (!naveDestruida) {
                estado.ovni?.let { o ->
                    if (hayColision(
                            estado.nave.posX,
                            estado.nave.posY,
                            RADIO_NAVE,
                            o.posX,
                            o.posY,
                            o.radio
                        )
                    ) {
                        gestorSonido.detenerSonidoOvni()
                        estado.ovni = null
                        gestorSonido.reproducirExplosion()
                    }
                }
            }
        }

        estado.balas.removeAll(balasAeliminar)
        estado.asteroides.removeAll(asteroidesAeliminar)
        estado.asteroides.addAll(asteroidesAnadir)
    }

    private fun comprobarSiguienteNivel() {
        if (estado.asteroides.isEmpty() && estado.ovni == null && tamanoCanvas != Size.Zero) {
            estado.nivel.value++
            estado.balas.clear()
            estado.asteroides.addAll(
                crearAsteroidesIniciales(
                    5 + estado.nivel.value - 1,
                    tamanoCanvas
                )
            )
        }
    }

    fun inicializarNivel() {
        estado.asteroides.clear()
        estado.balas.clear()
        gestorSonido.detenerSonidoOvni()
        estado.ovni = null
        reiniciarPosicionNave()

        // Usamos la nueva fórmula de dificultad.
        val numAsteroides =
            ASTEROIDES_INICIALES + (estado.nivel.value - 1) * INCREMENTO_ASTEROIDES_POR_NIVEL
        repeat(numAsteroides) {
            estado.asteroides.add(crearAsteroideBorde(tamanoCanvas))
        }
    }

    fun reiniciarPosicionNave() {
        estado.nave.posX = tamanoCanvas.width / 2
        estado.nave.posY = tamanoCanvas.height / 2
        estado.nave.velX = 0f
        estado.nave.velY = 0f
        estado.nave.angulo = 0f
    }

    private fun actualizarPuntuacion(puntos: Int) {
        estado.puntuacion.value += puntos
        // Usamos un bucle por si el jugador gana suficientes puntos para dos vidas a la vez.
        while (estado.puntuacion.value >= estado.puntuacionParaSiguienteVida) {
            estado.vidas.value++
            gestorSonido.reproducirSonido("life")
            // Actualizamos el siguiente objetivo de puntos.
            if (estado.puntuacionParaSiguienteVida == PUNTUACION_PRIMERA_VIDA_EXTRA) {
                estado.puntuacionParaSiguienteVida = PUNTUACION_SEGUNDA_VIDA_EXTRA
            } else {
                estado.puntuacionParaSiguienteVida += INTERVALO_VIDA_EXTRA
            }
        }
    }
}

// --- FUNCIONES AUXILIARES DE LÓGICA (PRIVADAS AL FICHERO) ---

private fun generarFormaAsteroide(
    radio: Float,
    irregularidad: Float = 0.4f,
): Pair<Path, List<Offset>> {
    val path = Path()
    val vertices = mutableListOf<Offset>()
    val numVertices = 12
    val anguloPaso = (2 * PI / numVertices).toFloat()
    for (i in 0 until numVertices) {
        val angulo = i * anguloPaso
        val radioActual = radio * (1f + Random.nextFloat() * irregularidad - irregularidad / 2f)
        val x = radioActual * cos(angulo)
        val y = radioActual * sin(angulo)
        vertices.add(Offset(x, y))
    }
    path.moveTo(vertices[0].x, vertices[0].y)
    for (i in 1 until vertices.size) {
        path.lineTo(vertices[i].x, vertices[i].y)
    }
    path.close()
    return Pair(path, vertices)
}

private fun crearAsteroidesIniciales(cantidad: Int, tamanoCanvas: Size): List<Asteroide> {
    // Esta función ahora está en desuso, pero la mantenemos por si la necesitamos en el futuro.
    val asteroides = mutableListOf<Asteroide>()
    for (i in 0 until cantidad) {
        asteroides.add(crearAsteroideBorde(tamanoCanvas))
    }
    return asteroides
}

private fun crearAsteroideBorde(
    tamanoCanvas: Size = Size(
        800f,
        600f
    ),
    /* Valor por defecto seguro */
): Asteroide {
    val tamano = TamanoAsteroide.GRANDE
    val posX: Float
    val posY: Float

    if (Random.nextBoolean()) {
        // Aparece en los bordes izquierdo o derecho
        posX = if (Random.nextBoolean()) -tamano.radio else tamanoCanvas.width + tamano.radio
        posY = Random.nextFloat() * tamanoCanvas.height
    } else {
        // Aparece en los bordes superior o inferior
        posX = Random.nextFloat() * tamanoCanvas.width
        posY = if (Random.nextBoolean()) -tamano.radio else tamanoCanvas.height + tamano.radio
    }

    val anguloHaciaElCentro =
        atan2((tamanoCanvas.height / 2f) - posY, (tamanoCanvas.width / 2f) - posX)
    // Añadimos una pequeña desviación al ángulo para que no todos vayan al centro exacto.
    val anguloFinal = anguloHaciaElCentro + Random.nextDouble(-0.5, 0.5).toFloat()

    val velocidad =
        MIN_VELOCIDAD_ASTEROIDE_POR_SEGUNDO + Random.nextFloat() * (MAX_VELOCIDAD_ASTEROIDE_POR_SEGUNDO - MIN_VELOCIDAD_ASTEROIDE_POR_SEGUNDO)
    val (path, vertices) = generarFormaAsteroide(tamano.radio)

    return Asteroide(
        posX,
        posY,
        cos(anguloFinal) * velocidad,
        sin(anguloFinal) * velocidad,
        tamano,
        path,
        vertices
    )
}

private fun crearAsteroideFragmento(
    asteroidePadre: Asteroide,
    nuevoTamano: TamanoAsteroide,
): Asteroide {
    val angulo = Random.nextFloat() * 2 * PI.toFloat()
    val velocidad =
        MIN_VELOCIDAD_FRAGMENTO_POR_SEGUNDO + Random.nextFloat() * (MAX_VELOCIDAD_FRAGMENTO_POR_SEGUNDO - MIN_VELOCIDAD_FRAGMENTO_POR_SEGUNDO)
    val (path, vertices) = generarFormaAsteroide(nuevoTamano.radio)
    return Asteroide(
        asteroidePadre.posX,
        asteroidePadre.posY,
        cos(angulo) * velocidad,
        sin(angulo) * velocidad,
        nuevoTamano,
        path,
        vertices
    )
}

private fun hayColision(x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float): Boolean {
    val sumaRadios = r1 + r2
    val dx = x1 - x2
    val dy = y1 - y2
    return (dx * dx + dy * dy) < (sumaRadios * sumaRadios)
}

private fun estaPuntoEnPoligono(puntoX: Float, puntoY: Float, poligono: List<Offset>): Boolean {
    var estaDentro = false
    var j = poligono.size - 1
    for (i in poligono.indices) {
        val verticeI = poligono[i]
        val verticeJ = poligono[j]
        val intersecta = ((verticeI.y > puntoY) != (verticeJ.y > puntoY)) &&
                (puntoX < (verticeJ.x - verticeI.x) * (puntoY - verticeI.y) / (verticeJ.y - verticeI.y) + verticeI.x)
        if (intersecta) {
            estaDentro = !estaDentro
        }
        j = i
    }
    return estaDentro
}

private fun hayColisionNaveAsteroide(nave: EstadoNave, asteroide: Asteroide): Boolean {
    if (!hayColision(
            nave.posX,
            nave.posY,
            RADIO_NAVE,
            asteroide.posX,
            asteroide.posY,
            asteroide.tamano.radio
        )
    ) {
        return false
    }
    val v1x = 0f;
    val v1y = -25f
    val v2x = -15f;
    val v2y = 15f
    val v3x = 15f;
    val v3y = 15f
    val anguloNaveRad = (nave.angulo - 90f) * (PI / 180f).toFloat()
    val cosNave = cos(anguloNaveRad)
    val sinNave = sin(anguloNaveRad)

    val v1MundoX = nave.posX + (v1x * cosNave - v1y * sinNave)
    val v1MundoY = nave.posY + (v1x * sinNave + v1y * cosNave)
    if (comprobarVerticeEnAsteroide(v1MundoX, v1MundoY, asteroide)) return true

    val v2MundoX = nave.posX + (v2x * cosNave - v2y * sinNave)
    val v2MundoY = nave.posY + (v2x * sinNave + v2y * cosNave)
    if (comprobarVerticeEnAsteroide(v2MundoX, v2MundoY, asteroide)) return true

    val v3MundoX = nave.posX + (v3x * cosNave - v3y * sinNave)
    val v3MundoY = nave.posY + (v3x * sinNave + v3y * cosNave)
    if (comprobarVerticeEnAsteroide(v3MundoX, v3MundoY, asteroide)) return true

    return false
}

private fun comprobarVerticeEnAsteroide(
    verticeX: Float,
    verticeY: Float,
    asteroide: Asteroide,
    esBala: Boolean = false,
): Boolean {
    val puntoX_enEspacioAsteroide = verticeX - asteroide.posX
    val puntoY_enEspacioAsteroide = verticeY - asteroide.posY

    val anguloAsteroideRad = asteroide.rotacion * (PI / 180f).toFloat()
    val cosAsteroideInv = cos(-anguloAsteroideRad)
    val sinAsteroideInv = sin(-anguloAsteroideRad)

    val puntoFinalX =
        puntoX_enEspacioAsteroide * cosAsteroideInv - puntoY_enEspacioAsteroide * sinAsteroideInv
    val puntoFinalY =
        puntoX_enEspacioAsteroide * sinAsteroideInv + puntoY_enEspacioAsteroide * cosAsteroideInv

    return estaPuntoEnPoligono(puntoFinalX, puntoFinalY, asteroide.vertices)
}

const val PUNTUACION_PRIMERA_VIDA_EXTRA = 5000
const val PUNTUACION_SEGUNDA_VIDA_EXTRA = 10000
const val INTERVALO_VIDA_EXTRA = 10000 