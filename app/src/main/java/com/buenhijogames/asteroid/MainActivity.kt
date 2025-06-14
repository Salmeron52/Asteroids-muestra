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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

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
        // Hacemos que la app se dibuje a pantalla completa (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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

    var mostrandoRecords by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        motorJuego.iniciar()
        onDispose {
            motorJuego.liberarRecursos()
        }
    }
    
    // La UI vuelve a estar contenida en un BoxWithConstraints para calcular el aspect ratio
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Añadimos un padding que respeta el espacio de la barra de estado.
            .statusBarsPadding()
    ) {
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
                puntuacion = estadoUI.puntuacion.value,
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

                // Superposición (Overlay) para el estado de GAME OVER o NUEVO_RECORD
                when (estadoUI.estadoActual.value) {
                    EstadoJuegoEnum.GAME_OVER -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "GAME OVER", color = Color.Red, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Asteroid by buenhijoGames", color = Color.White, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row {
                                    BotonControl(
                                        texto = "REINICIAR",
                                        onPress = { motorJuego.reiniciarJuego() },
                                        onRelease = { }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    BotonControl(
                                        texto = "RÉCORDS",
                                        onPress = { mostrandoRecords = true },
                                        onRelease = { }
                                    )
                                }
                            }
                        }
                    }
                    EstadoJuegoEnum.NUEVO_RECORD -> {
                        var iniciales by remember { mutableStateOf(TextFieldValue("")) }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.7f)).padding(16.dp)) {
                                Text("¡NUEVO RÉCORD!", color = Color.Yellow, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${estadoUI.puntuacion.value}", color = Color.White, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                TextField(
                                    value = iniciales,
                                    onValueChange = {
                                        if (it.text.length <= 3) iniciales = it
                                    },
                                    label = { Text("Tus iniciales") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                BotonControl(
                                    texto = "GUARDAR",
                                    onPress = {
                                        motorJuego.guardarNuevoRecord(iniciales.text.uppercase())
                                    },
                                    onRelease = {}
                                )
                            }
                        }
                    }
                    else -> { /* No se muestra nada si está JUGANDO */ }
                }
            }

            // --- Panel de Control Derecho ---
            PanelControlDerecho(
                modifier = Modifier.width(controlPanelWidth).fillMaxHeight(),
                vidas = estadoUI.vidas.value,
                onEmpujeChanged = onEmpujeChanged,
                onDisparo = onDisparo
            )
        }
    }

    if (mostrandoRecords) {
        PantallaRecords(
            records = estadoUI.listaRecords.value,
            onCerrar = { mostrandoRecords = false }
        )
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
        Text(
            text = texto,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
    puntuacion: Int,
    onGiroChanged: (Float) -> Unit,
    onHiperespacio: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = puntuacion.toString(),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BotonControl(
                texto = "HIPER\nESPACIO",
                onPress = onHiperespacio,
                onRelease = { /* No es una acción continua, no hace nada al soltar */ }
            )
            Spacer(modifier = Modifier.height(20.dp))
            ZonaControlRotacion(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                onRotate = onGiroChanged
            )
        }
    }
}

@Composable
fun PanelControlDerecho(
    modifier: Modifier = Modifier,
    vidas: Int,
    onEmpujeChanged: (Boolean) -> Unit,
    onDisparo: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // Distribuye el espacio
    ) {
        Row {
            val pathVida = remember {
                Path().apply {
                    moveTo(0f, -15f)
                    lineTo(-10f, 10f)
                    lineTo(10f, 10f)
                    close()
                }
            }
            repeat(vidas.coerceAtLeast(0)) {
                Canvas(modifier = Modifier.size(25.dp).padding(horizontal = 2.dp)) {
                    translate(left = center.x, top = center.y) {
                        drawPath(pathVida, color = Color.White, style = Stroke(3f))
                    }
                }
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
}

@Composable
fun PantallaRecords(
    records: List<PuntuacionRecord>,
    onCerrar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MEJORES PUNTUACIONES", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(records) { index, record ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Usamos pesos para crear un sistema de columnas y alinear el texto.
                        Text(
                            text = "${index + 1}.",
                            modifier = Modifier.weight(0.2f),
                            textAlign = TextAlign.End,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = record.iniciales,
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = record.puntos.toString(),
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Start,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BotonControl(texto = "CERRAR", onPress = onCerrar, onRelease = {})
        }
    }
}

