package com.estrin217.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Las tres semillas con las que se siembra un tema (spec 025).
 *
 * Un tema no son 48 colores escritos a mano: son **tres** colores de los que se derivan. Estos tres son los
 * acentos de la familia —el azul de VS Code, el rosa de Monokai— y el resto sale de la matemática de abajo.
 */
data class ThemeSeeds(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

/**
 * Genera el esquema de Material 3 de un tema a partir de sus tres semillas (spec 025, RF-161, RNF-101).
 *
 * **No es HCT.** Material 3 no publica el generador tonal que usa para sus propios colores dinámicos: lo único
 * público son las paletas que leen un `Context`. Este generador hace lo mismo con aritmética propia:
 *
 * 1. Convierte la semilla a **OKLCH** (ligereza, croma y matiz), que separa lo que se quiere conservar —el
 *    matiz— de lo que hay que poder mover —la ligereza— sin que el color derive hacia otro lado.
 * 2. Construye una **rampa** de 101 tonos con el matiz de la semilla y una croma que baja en los extremos, que es
 *    donde un gamut pequeño obliga a recortarla.
 * 3. Coloca cada **rol** en el tono que la línea base de Material 3 le asigna ([TONOS_LIGHT], [TONOS_DARK]).
 * 4. Recorta al gamut de sRGB reduciendo croma, nunca el matiz.
 * 5. **Ajusta el tono del acento** hasta que se lean los dos contrastes que lo寄生虫.
 *
 * El paso 5 es el que hace que esto sirva: las semillas salen de paletas reales, con matices claros, y el verde
 * de Monokai a tono 40 sobre una superficie clara se queda en 1.2:1. Mover el tono y no el matiz deja el tema
 * reconocible como el de su familia y, además, legible.
 *
 * Es determinista: las mismas semillas dan siempre el mismo esquema.
 */
object TonalScheme {

    /** Contraste que un acento tiene que alcanzar contra su propia superficie. */
    private const val MIN_CONTRAST_ACCENT = 3.0

    /** Contraste del texto que va **encima** de un acento: AA también lo pide. */
    private const val MIN_CONTRAST_ON_ACCENT = 4.5

    /** Croma de los neutros: apenas un matiz del primario, como los neutros de Material. */
    private const val CHROMA_NEUTRAL = 0.012

    /** El rojo de error de la línea base de Material 3, el mismo para todos los temas. */
    private val SEED_ERROR = Color(0xFFB3261E)

    /** El esquema completo de las semillas, en claro o en oscuro. */
    fun scheme(seeds: ThemeSeeds, dark: Boolean): ColorScheme {
        val tones = if (dark) TONOS_DARK else TONOS_LIGHT
        val neutral = RampaTonal.neutral(seeds.primary, CHROMA_NEUTRAL)
        val surface = neutral.color(tones.surface)
        return esquema(
            tones,
            neutral,
            accent(seeds.primary, tones.accent, surface, dark),
            accent(seeds.secondary, tones.accent, surface, dark),
            accent(seeds.tertiary, tones.accent, surface, dark),
            RampaTonal.deSemilla(SEED_ERROR),
        )
    }

    /** La croma OKLCH de un color, en `0..0.4` aproximadamente. */
    internal fun chroma(color: Color): Double = Oklch.de(color).c

    /** El acento de un tema: su rampa y el tono que le tocó tras ajustar el contraste. */
    private class Accent(private val ramp: RampaTonal, private val adjustedTone: Int) {
        fun principal(): Color = ramp.color(adjustedTone)
        fun at(tone: Int): Color = ramp.color(tone)
    }

    /**
     * La rampa del acento con su tono principal movido hasta que se lea.
     *
     * Un acento tiene que cumplir **dos** contrastes a la vez: 3:1 contra la superficie —porque es un
     * color, no texto— y 4.5:1 con el color que va encima, que en claro es blanco y en oscuro casi negro. En
     * claro se oscurece y en oscuro se aclara, que es la dirección en la que la superficie se aleja.
     *
     * El segundo contraste es el que pesa con los temas de esta spec: un azul claro a tono 40 se ve bien sobre el
     * blanco, pero el blanco encima queda justo. Si se llegara al extremo sin cumplirlo, se acepta el extremo: el
     * test de contraste lo dirá, y bajarlo no está en las manos de esta función.
     */
    private fun accent(seed: Color, startTone: Int, surface: Color, dark: Boolean): Accent {
        val ramp = RampaTonal.deSemilla(seed)
        val onAccentTone = if (dark) TONOS_DARK.onAccent else TONOS_LIGHT.onAccent
        val step = if (dark) 2 else -2
        var tone = startTone
        while (!seLee(ramp, tone, onAccentTone, surface) ) {
            val next = tone + step
            if (next !in 0..100) break
            tone = next
        }
        return Accent(ramp, tone)
    }

    /** Si un acento se lee: contra la superficie y bajo el color que va encima. */
    private fun seLee(ramp: RampaTonal, tone: Int, onAccentTone: Int, surface: Color): Boolean {
        val acento = ramp.color(tone)
        return Contrast.ratio(acento, surface) >= MIN_CONTRAST_ACCENT &&
            Contrast.ratio(ramp.color(onAccentTone), acento) >= MIN_CONTRAST_ON_ACCENT
    }

    /**
     * Los 48 roles, cada uno en el tono que le toca y de la rampa que le corresponde.
     *
     * Es la única función que pasa de las cinco rampas a un [ColorScheme], y es larga a propósito: es una tabla
     * de 48 casillas, y partirla en trozos la escondería.
     *
     * Se construye con el constructor de [ColorScheme] y no con `lightColorScheme`/`darkColorScheme` porque
     * estos últimos dejan en los morados de la línea base los roles que no se les pasa, que son justo los que
     * hay que pintar. Sus cachés de colores de botón quedan sin valor y Material los calcula la primera vez que
     * se piden: se ha comprobado en el bytecode de `ButtonDefaults` que el caso sin valor tiene su propio camino.
     */
    private fun esquema(
        t: Tones,
        n: RampaTonal,
        p: Accent,
        s: Accent,
        te: Accent,
        e: RampaTonal,
    ): ColorScheme = ColorScheme(
        primary = p.principal(),
        onPrimary = p.at(t.onAccent),
        primaryContainer = p.at(t.accentContainer),
        onPrimaryContainer = p.at(t.onAccentContainer),
        inversePrimary = p.at(t.accentInverse),
        secondary = s.principal(),
        onSecondary = s.at(t.onAccent),
        secondaryContainer = s.at(t.accentContainer),
        onSecondaryContainer = s.at(t.onAccentContainer),
        tertiary = te.principal(),
        onTertiary = te.at(t.onAccent),
        tertiaryContainer = te.at(t.accentContainer),
        onTertiaryContainer = te.at(t.onAccentContainer),
        background = n.color(t.surface),
        onBackground = n.color(t.onSurface),
        surface = n.color(t.surface),
        onSurface = n.color(t.onSurface),
        surfaceVariant = n.color(t.surfaceVariant),
        onSurfaceVariant = n.color(t.onSurfaceVariant),
        surfaceTint = p.at(t.accent),
        inverseSurface = n.color(t.inverseSurface),
        inverseOnSurface = n.color(t.onInverseSurface),
        error = e.color(t.error),
        onError = e.color(t.onError),
        errorContainer = e.color(t.errorContainer),
        onErrorContainer = e.color(t.onErrorContainer),
        outline = n.color(t.outline),
        outlineVariant = n.color(t.outlineVariant),
        scrim = n.color(t.scrim),
        surfaceBright = n.color(t.bright),
        surfaceDim = n.color(t.dim),
        surfaceContainer = n.color(t.container),
        surfaceContainerHigh = n.color(t.high),
        surfaceContainerHighest = n.color(t.highest),
        surfaceContainerLow = n.color(t.low),
        surfaceContainerLowest = n.color(t.lowest),
        primaryFixed = p.at(t.fixed),
        primaryFixedDim = p.at(t.fixedDim),
        onPrimaryFixed = p.at(t.onFixed),
        onPrimaryFixedVariant = p.at(t.onFixedVariant),
        secondaryFixed = s.at(t.fixed),
        secondaryFixedDim = s.at(t.fixedDim),
        onSecondaryFixed = s.at(t.onFixed),
        onSecondaryFixedVariant = s.at(t.onFixedVariant),
        tertiaryFixed = te.at(t.fixed),
        tertiaryFixedDim = te.at(t.fixedDim),
        onTertiaryFixed = te.at(t.onFixed),
        onTertiaryFixedVariant = te.at(t.onFixedVariant),
    )
}

/**
 * Un color en OKLCH: ligereza `l` (0..1), croma `c` y matiz `h` en grados.
 *
 * Es el espacio en el que se recorta: bajar la croma deja el matiz intacto, que es lo que hace falta para no
 * convertir el verde de un tema en el de otro.
 */
internal data class Oklch(val l: Double, val c: Double, val h: Double) {

    /** Si el color cabe en sRGB sin recortarse. */
    fun enGamut(): Boolean = lineal().let { (r, g, b) ->
        val bajo = -TOLERANCE
        val alto = 1.0 + TOLERANCE
        r in bajo..alto && g in bajo..alto && b in bajo..alto
    }

    fun aColor(): Color {
        val (r, g, b) = lineal()
        return Color(
            red = linealASrgb(r).coerceIn(0.0, 1.0).toFloat(),
            green = linealASrgb(g).coerceIn(0.0, 1.0).toFloat(),
            blue = linealASrgb(b).coerceIn(0.0, 1.0).toFloat(),
            alpha = 1f,
        )
    }

    /** El color con la mayor croma que sigue cabiendo en sRGB, hallada por búsqueda binaria. */
    fun aColorEnGamut(): Color {
        if (enGamut()) return aColor()
        var dentro = 0.0
        var fuera = c
        repeat(16) {
            val medio = (dentro + fuera) / 2
            if (copy(c = medio).enGamut()) dentro = medio else fuera = medio
        }
        return copy(c = dentro).aColor()
    }

    private fun lineal(): Triple<Double, Double, Double> {
        val rad = Math.toRadians(h)
        return oklabToLineal(Oklab(l, c * cos(rad), c * sin(rad)))
    }

    companion object {
        fun de(color: Color): Oklch {
            val lab = aOklab(srgbALineal(color.red), srgbALineal(color.green), srgbALineal(color.blue))
            val chroma = sqrt(lab.a * lab.a + lab.b * lab.b)
            return Oklch(lab.l, chroma, if (chroma < 1e-9) 0.0 else Math.toDegrees(atan2(lab.b, lab.a)))
        }
    }
}

/** Si un color cabe en sRGB, según la misma cuenta que usa el generador. */
internal fun Color.enGamutSrgb(): Boolean = Oklch.de(this).enGamut()

/**
 * Una rampa de 101 tonos con el matiz de una semilla.
 *
 * Los tonos cachean porque un esquema se pinta entero varias veces por el mismo acento, y el cálculo no es
 * gratuito. Los tonos extremos llevan poca croma: es donde el gamut de sRGB no da para más.
 */
internal class RampaTonal private constructor(private val hue: Double, private val chroma: Double) {

    private val cache = arrayOfNulls<Color>(101)

    fun color(tone: Int): Color = cache[tone]
        ?: Oklch(ligereza(tone), chroma * factorChroma(tone), hue).aColorEnGamut().also { cache[tone] = it }

    companion object {
        /** La rampa de una semilla: su matiz y su croma. */
        fun deSemilla(seed: Color): RampaTonal = Oklch.de(seed).let { RampaTonal(it.h, it.c) }

        /** La rampa neutra: el matiz de la semilla con una croma fija y baja. */
        fun neutral(seed: Color, chroma: Double): RampaTonal = RampaTonal(Oklch.de(seed).h, chroma)
    }
}

/** Cuánta croma admite cada tono: la de en medio es la que mejor cabe en sRGB. */
private fun factorChroma(tone: Int): Double = when {
    tone < 20 -> 0.25 + 0.75 * (tone / 20.0)
    tone <= 70 -> 1.0
    else -> 0.25 + 0.75 * ((100 - tone) / 30.0)
}

/**
 * El tono de Material —un `L*` de CIELAB— traducido a la ligereza de OKLab.
 *
 * **No son la misma escala**, y confundirlo se nota:Material pone la superficie oscura en `#141218`, que en
 * OKLab tiene ligereza `0.19`, y no en el `0.06` que saldría de tomar el tono por la ligereza. Con el error, todos
 * los tonos bajos salen casi negros y el tema oscuro parece un Pitch black sin fondo.
 *
 * La cuenta es la de siempre: de `L*` a luminancia (`Y = ((L*+16)/116)³`, o la rama lineal por debajo de 8) y de
 * ahí a lig perceptual, que para un gris es la raíz cúbica. Por debajo de 8 los dos cubos no se cancelan y queda
 * la potencia.
 */
private fun ligereza(tone: Int): Double {
    val luminancia = if (tone > 8) Math.pow((tone + 16.0) / 116.0, 3.0) else tone / 903.2962962
    return Math.pow(luminancia, 1.0 / 3.0)
}

// ── La aritmética de OKLab (Björn Ottosson), en matrices al revés ──────────────────────────

private const val TOLERANCE = 1e-4

internal data class Oklab(val l: Double, val a: Double, val b: Double)

private fun aOklab(r: Double, g: Double, b: Double): Oklab {
    val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    val lc = cbrt(l)
    val mc = cbrt(m)
    val sc = cbrt(s)
    return Oklab(
        l = 0.2104542553 * lc + 0.7936177850 * mc - 0.0040720468 * sc,
        a = 1.9779984951 * lc - 2.4285922050 * mc + 0.4505937099 * sc,
        b = 0.0259040371 * lc + 0.7827717662 * mc - 0.8086757660 * sc,
    )
}

private fun oklabToLineal(lab: Oklab): Triple<Double, Double, Double> {
    val lc = lab.l + 0.3963377774 * lab.a + 0.2158037573 * lab.b
    val mc = lab.l - 0.1055613458 * lab.a - 0.0638541728 * lab.b
    val sc = lab.l - 0.0894841775 * lab.a - 1.2914855480 * lab.b
    val l = lc * lc * lc
    val m = mc * mc * mc
    val s = sc * sc * sc
    return Triple(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )
}

private fun linealASrgb(c: Double): Double =
    if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055

// ── El tono que Material 3 le asigna a cada rol ─────────────────────────────────────────────

/**
 * Los tonos por rol, con los valores de la línea base de Material 3.
 *
 * Los tres acentos comparten cifras porque Material les da las mismas, y por eso hay un solo grupo de campos
 * para los tres en vez de tres tablas iguales.
 */
private data class Tones(
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val outlineVariant: Int,
    val inverseSurface: Int,
    val onInverseSurface: Int,
    val accent: Int,
    val onAccent: Int,
    val accentContainer: Int,
    val onAccentContainer: Int,
    val accentInverse: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
    val scrim: Int,
    val lowest: Int,
    val low: Int,
    val container: Int,
    val high: Int,
    val highest: Int,
    val dim: Int,
    val bright: Int,
    val fixed: Int,
    val fixedDim: Int,
    val onFixed: Int,
    val onFixedVariant: Int,
)

private val TONOS_LIGHT = Tones(
    surface = 98, onSurface = 10,
    surfaceVariant = 90, onSurfaceVariant = 30,
    outline = 50, outlineVariant = 80,
    inverseSurface = 20, onInverseSurface = 95,
    accent = 40, onAccent = 100,
    accentContainer = 90, onAccentContainer = 10,
    accentInverse = 80,
    error = 40, onError = 100,
    errorContainer = 90, onErrorContainer = 10,
    scrim = 0,
    lowest = 100, low = 96, container = 94, high = 92, highest = 90,
    dim = 87, bright = 98,
    fixed = 90, fixedDim = 80, onFixed = 10, onFixedVariant = 30,
)

private val TONOS_DARK = Tones(
    surface = 6, onSurface = 90,
    surfaceVariant = 30, onSurfaceVariant = 80,
    outline = 60, outlineVariant = 30,
    inverseSurface = 90, onInverseSurface = 20,
    accent = 80, onAccent = 20,
    accentContainer = 30, onAccentContainer = 90,
    accentInverse = 40,
    error = 80, onError = 20,
    errorContainer = 30, onErrorContainer = 90,
    scrim = 0,
    lowest = 4, low = 10, container = 12, high = 17, highest = 22,
    dim = 6, bright = 24,
    fixed = 90, fixedDim = 80, onFixed = 10, onFixedVariant = 30,
)