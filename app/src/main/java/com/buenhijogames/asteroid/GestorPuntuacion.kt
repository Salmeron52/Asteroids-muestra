package com.buenhijogames.asteroid

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor para guardar y recuperar la puntuación más alta.
 *
 * Utiliza SharedPreferences, que es el mecanismo estándar de Android para
 * guardar pequeñas cantidades de datos clave-valor de forma persistente.
 * Es ideal para guardar configuraciones, preferencias o, como en este caso,
 * una puntuación máxima.
 *
 * @param context El contexto de la aplicación, necesario para acceder a SharedPreferences.
 */
class GestorPuntuacion(context: Context) {

    private val prefs: SharedPreferences
    private val PREFS_FILENAME = "com.buenhijogames.asteroid.prefs"
    private val KEY_HISCORE = "hiscore"
    private val KEY_INICIALES = "iniciales"

    init {
        prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda una nueva puntuación máxima y las iniciales del jugador.
     * @param puntuacion La nueva puntuación a guardar.
     * @param iniciales Las iniciales del jugador.
     */
    fun guardarPuntuacion(puntuacion: Int, iniciales: String) {
        val editor = prefs.edit()
        editor.putInt(KEY_HISCORE, puntuacion)
        editor.putString(KEY_INICIALES, iniciales)
        editor.apply() // `apply()` guarda los cambios de forma asíncrona.
    }

    /**
     * Carga la puntuación máxima guardada.
     * @return Un Pair que contiene la puntuación (Int) y las iniciales (String).
     */
    fun cargarPuntuacion(): Pair<Int, String> {
        val puntuacion = prefs.getInt(KEY_HISCORE, 0)
        val iniciales = prefs.getString(KEY_INICIALES, "---") ?: "---"
        return puntuacion to iniciales
    }
} 