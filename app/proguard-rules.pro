# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Reglas de ProGuard/R8 para el proyecto Asteroid

# Añade reglas de ProGuard aquí. Por ejemplo:
# -keep public class com.buenhijogames.asteroid.MyClass {
#    public <methods>;
# }

# Reglas para kotlinx.serialization
# La librería de serialización de Kotlin necesita poder acceder a las clases
# que tienen la anotación @Serializable y a sus campos en tiempo de ejecución.
# Si R8 las renombra u ofusca, la deserialización desde JSON fallará.
# Esta regla le dice a R8 que mantenga intactas todas las clases que
# implementen la interfaz kotlin.serialization.Serializable.
# Nuestra clase PuntuacionRecord está anotada con @Serializable, que a su vez
# implementa esta interfaz.
-keep class * implements kotlinx.serialization.Serializable {
    <fields>;
    <methods>;
}

# Es una buena práctica también mantener los nombres de las clases anotadas
# para que los mensajes de error, si ocurren, sean más fáciles de depurar.
-keepnames class kotlinx.serialization.Serializable
-keepnames class com.buenhijogames.asteroid.PuntuacionRecord

# -----------------------------------------------------------------------------
# Reglas específicas para kotlinx.serialization
# Estas reglas son cruciales para evitar que R8/ProGuard elimine o renombre
# las clases de datos y sus propiedades que se usan para convertir objetos a JSON y viceversa.
# Sin estas reglas, la app fallaría en modo 'release' al intentar leer los récords guardados.
# Fuente: Documentación oficial de kotlinx.serialization en GitHub.
# -----------------------------------------------------------------------------
-keepclassmembers class kotlinx.serialization.internal.* {
    *;
}

-keep class * implements kotlinx.serialization.KSerializer {
    *;
}

-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
    public final kotlinx.serialization.KSerializer[] childSerializers();
    public final kotlinx.serialization.KSerializer[] typeParametersSerializers();
}

# Para clases anotadas con @Serializable
-keep,allowobfuscation @kotlinx.serialization.Serializable class * {
  * Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <fields>;
    <init>(...);
    kotlinx.serialization.KSerializer serializer();
}

# Para clases anotadas con @Polymorphic
-keep,allowobfuscation @kotlinx.serialization.Polymorphic class *

# Para clases anotadas con @Contextual
-keep,allowobfuscation @kotlinx.serialization.Contextual class *

# -----------------------------------------------------------------------------
# Reglas específicas para el juego Asteroids
# Estas reglas protegen las clases críticas del juego para evitar problemas en release
# -----------------------------------------------------------------------------

# Mantener todas las clases principales del juego
-keep class com.buenhijogames.asteroid.MainActivity { *; }
-keep class com.buenhijogames.asteroid.MotorJuego { *; }
-keep class com.buenhijogames.asteroid.GestorSonido { *; }
-keep class com.buenhijogames.asteroid.GestorPuntuacion { *; }
-keep class com.buenhijogames.asteroid.JuegoSurfaceView { *; }

# Mantener todas las data classes del juego (críticas para el funcionamiento)
-keep class com.buenhijogames.asteroid.Bala { *; }
-keep class com.buenhijogames.asteroid.Asteroide { *; }
-keep class com.buenhijogames.asteroid.Ovni { *; }
-keep class com.buenhijogames.asteroid.EstadoNave { *; }

# Mantener todos los enums del juego (ProGuard puede romperlos)
-keep enum com.buenhijogames.asteroid.OrigenBala { *; }
-keep enum com.buenhijogames.asteroid.TamanoAsteroide { *; }
-keep enum com.buenhijogames.asteroid.TipoOvni { *; }
-keep enum com.buenhijogames.asteroid.EstadoJuegoEnum { *; }

# Reglas para SoundPool y recursos de audio (crítico para el audio del juego)
-keep class android.media.SoundPool { *; }
-keep class android.media.AudioAttributes { *; }
-keep class android.media.AudioAttributes$Builder { *; }

# Reglas para Canvas y gráficos (crítico para el renderizado)
-keep class android.graphics.Canvas { *; }
-keep class android.graphics.Paint { *; }
-keep class android.graphics.Path { *; }

# Reglas para clases Compose ViewInterop (para AndroidView)
-keep class androidx.compose.ui.viewinterop.** { *; }

# Reglas generales para evitar crashes
-keep class * extends android.app.Activity
-keep class * extends androidx.activity.ComponentActivity

# Mantener constructores por defecto para evitar crashes de reflexión
-keepclassmembers class com.buenhijogames.asteroid.** {
    <init>(...);
}

# Reglas para debugging - mantener información de líneas para crashes más claros
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reglas para evitar warnings comunes en Play Store
-dontwarn java.lang.invoke.StringConcatFactory