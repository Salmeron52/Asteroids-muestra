package com.buenhijogames.asteroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buenhijogames.asteroid.ui.theme.AsteroidTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalDensity

// Las constantes de jugabilidad se han movido a su propio archivo: Constantes.kt
// Ya no son necesarias aquí.

/**
 * Enum para diferenciar el origen de una bala.
 */
enum class OrigenBala {
    JUGADOR,
    OVNI
}

/**
 * Data Class para representar una Bala.
 *
 * @param posX La posición X actual de la bala.
 * @param posY La posición Y actual de la bala.
 * @param velX La velocidad horizontal de la bala.
 * @param velY La velocidad vertical de la bala.
 * @param origen De quién es la bala.
 * @param vidaUtil El "tiempo de vida" restante de la bala, en fotogramas.
 * @param radio El radio de la bala.
 */
data class Bala(
    var posX: Float,
    var posY: Float,
    var velX: Float,
    var velY: Float,
    val origen: OrigenBala,
    var vidaUtil: Int = 100,
    val radio: Float = 5f
)

/**
 * Enum para representar los tamaños de los asteroides.
 * Un enum es un tipo especial que representa un grupo de constantes.
 */
enum class TamanoAsteroide(val radio: Float, val puntos: Int) {
    GRANDE(80f, 20),
    MEDIANO(40f, 50),
    PEQUENO(20f, 100)
}

/**
 * Data Class para representar un Asteroide.
 */
data class Asteroide(
    var posX: Float,
    var posY: Float,
    var velX: Float,
    var velY: Float,
    var tamano: TamanoAsteroide,
    val path: Path,
    val vertices: List<Offset>,
    var rotacion: Float = 0f,
    val velocidadRotacion: Float = Random.nextFloat() * 2f - 1f
)

/**
 * Data class para representar un OVNI.
 */
data class Ovni(
    var posX: Float,
    var posY: Float,
    val velX: Float,
    val radio: Float = 30f,
    val puntos: Int = 200,
    var tiempoParaDisparo: Float = Random.nextFloat() * 3f + 2f // Tiempo inicial en segundos
)

/**
 * Enum para controlar el estado actual del juego.
 */
// SE ELIMINA ESTE ENUM. AHORA USAMOS `EstadoJuegoEnum` de `MotorJuego.kt`
/*
enum class EstadoJuego {
    JUGANDO,
    GAME_OVER,
    NUEVO_RECORD
}
*/

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AsteroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    PantallaJuego()
                }
            }
        }
    }
}

@Composable
fun PantallaJuego() {
    val context = LocalContext.current
    val motorJuego = remember {
        val gestorSonido = GestorSonido(context)
        val gestorPuntuacion = GestorPuntuacion(context)
        MotorJuego(gestorSonido, gestorPuntuacion)
    }
    val estadoUI = motorJuego.estado

    DisposableEffect(Unit) {
        motorJuego.iniciar()
        onDispose {
            motorJuego.liberarRecursos()
        }
    }
    
    // La UI vuelve a estar contenida en un BoxWithConstraints para calcular el aspect ratio
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gameHeight = this.maxHeight
        val gameWidth = this.maxWidth
        val ratio = 4f / 3f

        // Calculamos las dimensiones del canvas del juego para que sea 4:3
        val canvasWidth = if (gameWidth / gameHeight < ratio) gameWidth else gameHeight * ratio
        val canvasHeight = if (gameWidth / gameHeight < ratio) gameWidth / ratio else gameHeight

        // El espacio sobrante se divide entre los dos paneles de control
        val controlPanelWidth = (gameWidth - canvasWidth) / 2

        val onHiperespacio: () -> Unit = { motorJuego.ejecutarHiperespacio() }
        val onDisparo: () -> Unit = { motorJuego.disparar() }
        val onGiroChanged: (Float) -> Unit = { nuevoAngulo -> motorJuego.establecerRotacionNave(nuevoAngulo) }
        val onEmpujeChanged: (Boolean) -> Unit = { motorJuego.establecerEmpujeNave(it) }

        // El layout principal es una Fila (Row) con los paneles a los lados y el juego en el centro.
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Panel de Control Izquierdo ---
            PanelControlIzquierdo(
                modifier = Modifier.width(controlPanelWidth).fillMaxHeight(),
                onGiroChanged = onGiroChanged,
                onHiperespacio = onHiperespacio
            )

            // --- Área de Juego Principal (Lienzo) ---
            Box(
                modifier = Modifier.size(canvasWidth, canvasHeight)
            ) {
                // El SurfaceView se ajusta al tamaño calculado.
                val density = LocalDensity.current
                AndroidView(
                    factory = { ctx ->
                        // La factory se llama una sola vez para crear la vista.
                        JuegoSurfaceView(ctx, motorJuego)
                    },
                    update = { view ->
                        // El bloque update se llama cuando el Composable se actualiza.
                        // Aquí es donde comunicamos el tamaño correcto en píxeles al motor.
                        val widthInPx = with(density) { canvasWidth.toPx() }
                        val heightInPx = with(density) { canvasHeight.toPx() }
                        motorJuego.establecerLimites(widthInPx, heightInPx)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // La UI de información (puntuación, vidas, nivel) se superpone al juego.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Puntuación: ${estadoUI.puntuacion.value}", color = Color.White, fontSize = 20.sp)
                    Text("Nivel: ${estadoUI.nivel.value}", color = Color.White, fontSize = 20.sp)
                    Text("Vidas: ${estadoUI.vidas.value}", color = Color.White, fontSize = 20.sp)
                    if (estadoUI.estadoActual.value == EstadoJuegoEnum.GAME_OVER) {
                        Text("GAME OVER", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Panel de Control Derecho ---
            PanelControlDerecho(
                modifier = Modifier.width(controlPanelWidth).fillMaxHeight(),
                onEmpujeChanged = onEmpujeChanged,
                onDisparo = onDisparo
            )
        }
    }
}

/**
 * Una zona de control analógico para la rotación de la nave.
 * Reemplaza los dos botones de giro por una superficie única e intuitiva.
 * El usuario arrastra el dedo horizontalmente para controlar la velocidad y dirección de giro.
 *
 * @param modifier Modificador para dar estilo y tamaño a la zona de control.
 * @param onRotate Lambda que se invoca en cada evento de arrastre, devolviendo un
 *                 factor de rotación entre -1.0 (izquierda) y 1.0 (derecha).
 */
@Composable
fun ZonaControlRotacion(
    modifier: Modifier = Modifier,
    onRotate: (factor: Float) -> Unit
) {
    var posicionInicial by remember { mutableStateOf(Offset.Zero) }
    var posicionActual by remember { mutableStateOf(Offset.Zero) }
    val estaArrastrando by remember {
        derivedStateOf {
            posicionInicial != Offset.Zero
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.05f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        posicionInicial = offset
                        posicionActual = offset
                    },
                    onDragEnd = {
                        onRotate(0f)
                        posicionInicial = Offset.Zero
                        posicionActual = Offset.Zero
                    },
                    onDragCancel = {
                        onRotate(0f)
                        posicionInicial = Offset.Zero
                        posicionActual = Offset.Zero
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        posicionActual = change.position
                        val deltaX = posicionActual.x - posicionInicial.x

                        // Definimos una "sensibilidad": cuántos píxeles de arrastre para llegar a la rotación máxima.
                        val sensibilidad = 200f
                        val factor = (deltaX / sensibilidad).coerceIn(-1f, 1f)
                        onRotate(factor)
                    }
                )
            }
    ) {
        // --- Feedback Visual del Control ---
        if (estaArrastrando) {
            // Dibujamos un "joystick" visual para que el usuario entienda la mecánica.
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Círculo exterior en la posición inicial del dedo.
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 60.dp.toPx(),
                    center = posicionInicial,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Círculo interior que sigue la posición actual del dedo.
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 30.dp.toPx(),
                    center = posicionActual
                )
            }
        } else {
            // Texto indicativo cuando la zona no está en uso.
            Text(text = "GIRAR", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Botón de control circular y estilizado para el juego.
 * Usa `pointerInput` para detectar la pulsación y liberación, permitiendo acciones contínuas.
 */
@Composable
fun BotonControl(
    texto: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(100.dp) // Un tamaño generoso para facilitar la pulsación
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        try {
                            awaitRelease()
                        } finally {
                            onRelease()
                        }
                    }
                )
            }
            .background(Color.Gray.copy(alpha = 0.2f), shape = CircleShape)
            .border(2.dp, Color.White, CircleShape)
    ) {
        Text(text = texto, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/**
 * Un botón personalizado que detecta cuándo se mantiene pulsado y cuándo se suelta.
 * Esencial para acciones contínuas como girar o acelerar en un juego.
 *
 * @param texto El texto que se mostrará en el botón.
 * @param onPress La acción a ejecutar cuando el botón es presionado.
 * @param onRelease La acción a ejecutar cuando el botón es soltado.
 * @param modifier Modificadores estándar de Compose.
 */
@Composable
fun BotonPulsacionLarga(
    texto: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        try {
                            awaitRelease()
                        } finally {
                            onRelease()
                        }
                    }
                )
            }
            .background(
                color = ButtonDefaults.buttonColors().containerColor,
                shape = RoundedCornerShape(100) // Forma de píldora, como los botones por defecto
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = texto)
    }
}

/**
 * Path predefinido para la forma del OVNI.
 */
val pathOvni = Path().apply {
    moveTo(-30f, 0f)
    lineTo(-15f, -15f)
    lineTo(15f, -15f)
    lineTo(30f, 0f)
    lineTo(-30f, 0f)
    moveTo(-20f, 0f)
    cubicTo(-10f, 15f, 10f, 15f, 20f, 0f)
}

@Preview(showBackground = true)
@Composable
fun VistaPreviaPantallaJuego() {
    AsteroidTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            PantallaJuego()
        }
    }
}

@Composable
fun PanelControlIzquierdo(
    modifier: Modifier = Modifier,
    onGiroChanged: (Float) -> Unit,
    onHiperespacio: () -> Unit
) {
    // Usamos una Columna para apilar los controles verticalmente.
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ZonaControlRotacion(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onRotate = onGiroChanged
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onHiperespacio) {
            Text("HIPER")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PanelControlDerecho(
    modifier: Modifier = Modifier,
    onEmpujeChanged: (Boolean) -> Unit,
    onDisparo: () -> Unit
) {
    // Usamos una Columna para apilar los controles verticalmente.
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotonControl(
            texto = "ACELERAR",
            onPress = { onEmpujeChanged(true) },
            onRelease = { onEmpujeChanged(false) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        BotonControl(
            texto = "DISPARAR",
            onPress = onDisparo,
            onRelease = { /* No hace nada al soltar */ }
        )
    }
}

