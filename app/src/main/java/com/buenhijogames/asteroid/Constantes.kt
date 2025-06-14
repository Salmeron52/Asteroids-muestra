package com.buenhijogames.asteroid

// ---- CONSTANTES GLOBALES DE JUGABILIDAD ----
// Moviendo las constantes a su propio archivo, las hacemos públicas (quitando `private`)
// para que puedan ser accedidas desde cualquier parte del código, como MotorJuego.kt.
const val GRADOS_ROTACION_POR_SEGUNDO = 240f
const val FUERZA_EMPUJE_POR_SEGUNDO = 700f
const val FACTOR_FRICCION_POR_SEGUNDO = 1.2f
const val VELOCIDAD_BALA_POR_SEGUNDO = 900f
const val MAX_VELOCIDAD_NAVE = 900f
const val RADIO_NAVE = 25f
const val MIN_VELOCIDAD_ASTEROIDE_POR_SEGUNDO = 60f
const val MAX_VELOCIDAD_ASTEROIDE_POR_SEGUNDO = 150f
const val MIN_VELOCIDAD_FRAGMENTO_POR_SEGUNDO = 90f
const val MAX_VELOCIDAD_FRAGMENTO_POR_SEGUNDO = 210f
const val RETRASO_DISPARO_SEGUNDOS = 0.16f
const val TIEMPO_INVULNERABLE_SEGUNDOS = 3.0f

// --- PARÁMETROS DE DIFICULTAD ---

// Define con cuántos asteroides se empieza en el nivel 1.
const val ASTEROIDES_INICIALES = 3

// Define cuántos asteroides adicionales se añaden por cada nivel superado.
const val INCREMENTO_ASTEROIDES_POR_NIVEL = 1

// Define cuánto aumenta la velocidad del OVNI por nivel (0.1f = 10% por nivel).
const val FACTOR_VELOCIDAD_OVNI_POR_NIVEL = 0.08f 