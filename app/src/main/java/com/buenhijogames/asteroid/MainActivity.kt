package com.buenhijogames.asteroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.buenhijogames.asteroid.ui.theme.AsteroidTheme
import kotlin.random.Random
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels


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
    val radio: Float = 5f,
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
    val velocidadRotacion: Float = Random.nextFloat() * 2f - 1f,
)

/**
 * Enum para representar los tipos de OVNI disponibles.
 * En el juego original Asteroids había dos tipos con características diferentes.
 */
enum class TipoOvni(val radio: Float, val puntos: Int, val sonido: String) {
    PEQUENO(25f, 1000, "ovni_pequeno"),  // OVNI pequeño: más rápido, más puntos, sonido agudo
    GRANDE(40f, 500, "ovni_grande")      // OVNI grande: más lento, menos puntos, sonido grave
}

/**
 * Data class para representar un OVNI.
 * Ahora incluye el tipo de OVNI que determina su tamaño, puntos y sonido.
 */
data class Ovni(
    var posX: Float,
    var posY: Float,
    val velX: Float,
    val tipo: TipoOvni,
    var tiempoParaDisparo: Float = Random.nextFloat() * 3f + 2f, // Tiempo inicial en segundos
) {
    // Propiedades derivadas del tipo de OVNI
    val radio: Float get() = tipo.radio
    val puntos: Int get() = tipo.puntos
    val sonido: String get() = tipo.sonido
}


class MainActivity : ComponentActivity() {

    // Obtenemos una instancia del ViewModel.
    // El sistema se encarga de crearla la primera vez y de devolver la misma
    // instancia en futuras recreaciones de la Activity (ej. al girar la pantalla).
    private val juegoViewModel: JuegoViewModel by viewModels()

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
                    // Pasamos el motor del juego, que ahora vive dentro del ViewModel,
                    // a nuestra pantalla principal.
                    PantallaJuego(juegoViewModel.motorJuego)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pausamos el juego a través del ViewModel.
        juegoViewModel.motorJuego.pausarJuego()
    }

    override fun onResume() {
        super.onResume()
        // Delegamos la lógica de reanudación al ViewModel.
        // Él decidirá si debe pausar el juego o no.
        juegoViewModel.alReanudarActividad()
    }
}

@Composable
fun rememberMotorJuego(): MotorJuego {
    val context = LocalContext.current
    return remember {
        val gestorSonido = GestorSonido(context)
        val gestorPuntuacion = GestorPuntuacion(context)
        MotorJuego(gestorSonido, gestorPuntuacion)
    }
}

@Composable
fun PantallaJuego(motorJuego: MotorJuego) {
    val estadoUI = motorJuego.estado

    var mostrandoRecords by remember { mutableStateOf(false) }
    var mostrarSliderVolumen by remember { mutableStateOf(false) }

    val onBotonVolumenClick: () -> Unit = {
        val seraVisible = !mostrarSliderVolumen
        mostrarSliderVolumen = seraVisible
        // Si vamos a mostrar el slider y el juego no está en pausa, lo pausamos.
        if (seraVisible && !estadoUI.enPausa.value) {
            motorJuego.alternarPausa()
        }
    }

    DisposableEffect(Unit) {
        motorJuego.iniciar()
        onDispose {
            // motorJuego.liberarRecursos() // Se gestiona en el onDestroy de la Activity
        }
    }

    // El layout principal ahora es un Box para permitir la superposición de elementos.
    // Esto es clave para el diseño adaptativo.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo negro para todo el espacio
            .statusBarsPadding()
    ) {
        val gameHeight = this.maxHeight
        val gameWidth = this.maxWidth
        val ratioJuego = 4f / 3f

        // 1. Calculamos las dimensiones del canvas del juego para que siempre sea 4:3
        val canvasWidth: Dp
        val canvasHeight: Dp
        if (gameWidth / gameHeight < ratioJuego) {
            // La pantalla es más alta que ancha (en relación al 4:3), el ancho es el límite
            canvasWidth = gameWidth
            canvasHeight = gameWidth / ratioJuego
        } else {
            // La pantalla es más ancha que alta, la altura es el límite
            canvasHeight = gameHeight
            canvasWidth = gameHeight * ratioJuego
        }

        val haySolapamiento = canvasWidth > gameWidth - (120.dp * 2)

        val onHiperespacio: () -> Unit = { motorJuego.ejecutarHiperespacio() }
        val onDisparo: () -> Unit = { motorJuego.disparar() }
        val onGiroChanged: (Float) -> Unit =
            { nuevoAngulo -> motorJuego.establecerRotacionNave(nuevoAngulo) }
        val onEmpujeChanged: (Boolean) -> Unit = { motorJuego.establecerEmpujeNave(it) }

        val density = LocalDensity.current
        val widthInPx = with(density) { canvasWidth.toPx() }
        val heightInPx = with(density) { canvasHeight.toPx() }

        // El orden aquí define el eje Z (lo último se dibuja encima)

        // 1. El canvas del juego, en el fondo
        AndroidView(
        )
        // Superposición de textos (record, nivel, pausa), se escala con el juego
        Box(
            modifier = Modifier
                .size(canvasWidth, canvasHeight)
                .align(Alignment.Center) // Centramos el juego en el espacio disponible
        ) {
            JuegoInfoOverlay(motorJuego)
            // Superposición de menús (Game Over, Nuevo Récord)
            JuegoMenuOverlay(motorJuego, onMostrarRecords = { mostrandoRecords = true })
        }


        // 2. Capa superpuesta para cerrar el slider de volumen al tocar fuera.
        //    Aparece solo cuando el slider es visible.
        //    Se coloca encima del juego pero detrás de los controles.
        if (mostrarSliderVolumen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Sin efecto visual de click
                    ) {
                        mostrarSliderVolumen = false // Oculta el slider al tocar
                        // Al tocar fuera, si el juego está en pausa, lo reanudamos.
                        if (estadoUI.enPausa.value) {
                            motorJuego.alternarPausa()
                        }
                    }
            )
        }

        // 3. Paneles de control, por encima de todo
        val panelModifierBase = if (haySolapamiento) {
            Modifier
                .fillMaxHeight()
                .width(120.dp) // Ancho fijo para el panel
                .background(Color.Black.copy(alpha = 0.4f)) // Fondo semi-transparente
        } else {
            Modifier
                .fillMaxHeight()
                .width((gameWidth - canvasWidth) / 2) // Ocupa el espacio sobrante
                .background(Color.Black)
        }

        PanelControlIzquierdo(
            modifier = panelModifierBase.align(Alignment.CenterStart),
            puntuacion = estadoUI.puntuacion.value,
            onHiperespacio = onHiperespacio,
            onGiroChanged = onGiroChanged
        )

        PanelControlDerecho(
            modifier = panelModifierBase.align(Alignment.CenterEnd),
            vidas = estadoUI.vidas.value,
            enPausa = estadoUI.enPausa.value,
            volumenActual = motorJuego.volumenMaestro.value,
            mostrarSliderVolumen = mostrarSliderVolumen,
            onEmpujeChanged = onEmpujeChanged,
            onDisparo = onDisparo,
            onPausa = { motorJuego.alternarPausa() },
            onBotonVolumenClick = onBotonVolumenClick,
            onVolumenChanged = { nuevoVolumen -> motorJuego.setVolumen(nuevoVolumen) }
        )
    }

    if (mostrandoRecords) {
        PantallaRecords(
            records = motorJuego.estado.listaRecords.value,
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
    onRotate: (factor: Float) -> Unit,
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
            Text(
                text = "GIRAR",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
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
    modifier: Modifier = Modifier,
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
    modifier: Modifier = Modifier,
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
}

@Preview(showBackground = true)
@Composable
fun VistaPreviaPantallaJuego() {
    AsteroidTheme {
    }
}

@Composable
fun PanelControlIzquierdo(
    modifier: Modifier = Modifier,
    puntuacion: Int,
    onHiperespacio: () -> Unit,
    onGiroChanged: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 4.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                onRotate = onGiroChanged
            )
        }
    }
}

@Composable
fun PanelControlDerecho(
    modifier: Modifier = Modifier,
    vidas: Int,
    enPausa: Boolean,
    volumenActual: Float,
    mostrarSliderVolumen: Boolean,
    onEmpujeChanged: (Boolean) -> Unit,
    onDisparo: () -> Unit,
    onPausa: () -> Unit,
    onBotonVolumenClick: () -> Unit,
    onVolumenChanged: (Float) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Sección de Vidas
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 1. Dibujamos UNA SOLA nave como icono. Reducimos el ancho del Canvas para que se ajuste al dibujo.
                Canvas(modifier = Modifier.size(width = 40.dp, height = 60.dp), onDraw = { // Ancho ajustado
                })

                // 2. Mostramos el número de vidas restantes en texto
                Text(
                    text = "x ${vidas.coerceAtLeast(0)}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botones de acción y controles
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Controles de Pausa y Volumen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Contenedor para el botón de volumen y el slider
                // Lo hacemos clickable para "consumir" el click y que no se propague
                // a la capa trasera que cierra el slider.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Volumen
                        IconButton(onClick = onBotonVolumenClick) {
                            val iconoVolumen = when {
                                volumenActual == 0f -> R.drawable.volume_off_ico
                                volumenActual < 0.6f -> R.drawable.volume_down_ico
                                else -> R.drawable.volume_up_ico
                            }
                            Icon(
                                painter = painterResource(id = iconoVolumen),
                                contentDescription = "Control de Volumen",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        // El Slider de volumen, solo visible cuando se activa
                        if (mostrarSliderVolumen) {
                            Slider(
                        }
                    }
                }


                Spacer(modifier = Modifier.width(8.dp))

                // Botón de Pausa
                IconButton(onClick = onPausa) {
                    val iconoPausa = if (enPausa) {
                        R.drawable.play_arrow_ico
                    } else {
                        R.drawable.pause_ico
                    }
                    Icon(
                        painter = painterResource(id = iconoPausa),
                        contentDescription = "Pausa / Reanudar",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(47.dp)
                    )
                }
            }

            BotonControl(
                texto = "ACELERAR",
                onPress = { onEmpujeChanged(true) },
                onRelease = { onEmpujeChanged(false) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            BotonControl(
                texto = "DISPARAR",
                onPress = onDisparo,
                onRelease = { /* No es una acción continua, no hace nada al soltar */ }
            )
        }
    }
}

@Composable
fun PantallaRecords(
    records: List<PuntuacionRecord>,
    onCerrar: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "MEJORES PUNTUACIONES",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(records) { index, record ->
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(),
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

/**
 * Un Composable dedicado a mostrar la información superpuesta durante el juego.
 * Esto mantiene la `PantallaJuego` principal más limpia y organizada.
 */
@Composable
fun JuegoInfoOverlay(motorJuego: MotorJuego) {
    val estadoUI = motorJuego.estado
    if (estadoUI.estadoActual.value == EstadoJuegoEnum.JUGANDO) {
        // Obtenemos el record máximo actual para mostrarlo
        val recordMaximo = motorJuego.obtenerRecordMaximo()

        // Información en la parte superior de la pantalla de juego
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "RECORD\n$recordMaximo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "NIVEL\n${estadoUI.nivel.value}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Indicación de pausa en el centro de la pantalla
        if (estadoUI.enPausa.value) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAUSA",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Un Composable dedicado a mostrar los menús de fin de partida (Game Over, Nuevo Récord).
 */
@Composable
fun JuegoMenuOverlay(motorJuego: MotorJuego, onMostrarRecords: () -> Unit) {
    val estadoUI = motorJuego.estado
    when (estadoUI.estadoActual.value) {
        EstadoJuegoEnum.GAME_OVER -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GAME OVER",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "by buenhijoGames",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        BotonControl(
                            texto = "REINICIAR",
                            onPress = { motorJuego.reiniciarJuego() },
                            onRelease = { }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        BotonControl(
                            texto = "RÉCORDS",
                            onPress = onMostrarRecords,
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(24.dp)
                        .border(1.dp, Color.White)
                ) {
                    Text(
                        "¡NUEVO RÉCORD!",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${estadoUI.puntuacion.value}",
                        color = Color.White,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = iniciales,
                        onValueChange = {
                            if (it.text.length <= 3) iniciales = it
                        },
                        label = { Text("Tus iniciales (3 letras)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BotonControl(
                        texto = "GUARDAR",
                        onPress = {
                            if (iniciales.text.isNotBlank()) {
                                motorJuego.guardarNuevoRecord(iniciales.text.uppercase())
                            }
                        },
                        onRelease = {}
                    )
                }
            }
        }

        else -> { /* No se muestra nada si está JUGANDO */
        }
    }
}

