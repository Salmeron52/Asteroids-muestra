package com.buenhijogames.asteroid

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data class serializable para representar una entrada en la tabla de récords.
 * `@Serializable` le indica a la librería de kotlinx que puede convertir esta clase a JSON.
 */
@Serializable
data class PuntuacionRecord(val iniciales: String, val puntos: Int)

/**
 * Gestor para guardar y recuperar la lista de puntuaciones más altas.
 *
 * Utiliza SharedPreferences para guardar los datos, pero en lugar de valores simples,
 * guarda la lista entera de récords convertida a un string en formato JSON.
 *
 * @param context El contexto de la aplicación, necesario para acceder a SharedPreferences.
 */
class GestorPuntuacion(context: Context) {

    private val prefs: SharedPreferences
    private val PREFS_FILENAME = "com.buenhijogames.asteroid.prefs"
    private val KEY_HISCORES = "hiscores_list" // Nueva clave para la lista

    init {
        prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    /**
     * Guarda una nueva lista de récords.
     * @param puntuaciones La lista de objetos PuntuacionRecord a guardar.
     */
    fun guardarPuntuaciones(puntuaciones: List<PuntuacionRecord>) {
        val editor = prefs.edit()
        // Convierte la lista de récords a un string JSON
        val jsonString = Json.encodeToString(puntuaciones)
        editor.putString(KEY_HISCORES, jsonString)
        editor.apply()
    }

    /**
     * Carga la lista de récords guardada.
     * @return Una `List<PuntuacionRecord>`. Si no hay nada guardado, devuelve una lista vacía.
     */
    fun cargarPuntuaciones(): List<PuntuacionRecord> {
        val jsonString = prefs.getString(KEY_HISCORES, null)
        return if (jsonString != null) {
            try {
                // Intenta convertir el string JSON de vuelta a una lista de récords
                Json.decodeFromString<List<PuntuacionRecord>>(jsonString)
            } catch (e: Exception) {
                // Si hay un error en el formato (p.ej. versión antigua), devuelve una lista vacía.
                listOf()
            }
        } else {
            // Si no había nada guardado, devuelve una lista vacía.
            listOf()
        }
    }
} 