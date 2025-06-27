package com.buenhijogames.asteroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * ViewModel para gestionar el estado y la instancia del MotorJuego.
 *
 * Al utilizar un ViewModel, nos aseguramos de que la instancia de `MotorJuego`
 * sobreviva a los cambios de configuración que destruyen y recrean la Activity
 * (como girar la pantalla o que el sistema la detenga temporalmente).
 * Esto previene que el juego se reinicie inesperadamente.
 *
 * Hereda de `AndroidViewModel` para poder acceder de forma segura al contexto
 * de la aplicación, necesario para los gestores de sonido y puntuación.
 */
class JuegoViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Variable para controlar si es la primera vez que la UI se muestra.
     * Esto nos permite evitar pausar el juego en el arranque inicial.
     * Se almacena en el ViewModel para que su estado sobreviva a la recreación de la Activity.
     */
    private var esPrimerArranque = true

    /**
     * La única instancia del motor del juego.
     * Se inicializa de forma perezosa (lazy) la primera vez que se accede a ella.
     * El `ViewModel` se encargará de mantener viva esta instancia.
     */
    val motorJuego: MotorJuego by lazy {
        val gestorSonido = GestorSonido(application)
        val gestorPuntuacion = GestorPuntuacion(application)
        MotorJuego(gestorSonido, gestorPuntuacion).apply {
            // Iniciamos la carga de sonidos y puntuaciones desde aquí.
            iniciar()
        }
    }

    /**
     * Este método se llama automáticamente cuando el ViewModel está a punto de ser destruido.
     * Es el lugar perfecto para liberar recursos y evitar fugas de memoria.
     */
    override fun onCleared() {
        super.onCleared()
        motorJuego.liberarRecursos()
    }

    /**
     * Gestiona la lógica de reanudación de la Activity.
     * En el primer arranque no hace nada, permitiendo que el juego comience inmediatamente.
     * En reanudaciones posteriores (ej. al volver a la app), pausa el juego.
     */
    fun alReanudarActividad() {
        if (esPrimerArranque) {
            esPrimerArranque = false
            return
        }
        motorJuego.pausarJuego()
    }
} 