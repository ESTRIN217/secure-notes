package com.example.perf

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.size.Precision

/**
 * Nivel de capacidad del hardware. Determina cuánto trabajo de renderizado nos podemos permitir.
 */
enum class PerformanceTier { LOW, MID, HIGH }

/**
 * Snapshot de las capacidades del dispositivo, calculado UNA vez por proceso.
 *
 * Concentra todas las decisiones de "adaptación de rendimiento" para que ni la UI ni los
 * ViewModel tengan que volver a preguntar al sistema (nada de ActivityManager/DisplayManager
 * dentro de un bloque de recomposición).
 */
class DevicePerformanceProfile private constructor(
    val tier: PerformanceTier,
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean,
    val maxRefreshRateHz: Float,
    /** Duración de un frame a la tasa de refresco máxima. Base para throttling de emissions. */
    val frameBudgetMs: Long,
    val imageMemoryCacheBytes: Long,
    val imageDiskCacheBytes: Long,
    /** EXACT decodifica a precisión total (más caro); INEXACT permite RGB_565 / submuestreo. */
    val imagePrecision: Precision,
    val useHardwareLayerForDrawing: Boolean
) {
    companion object {
        private const val TAG = "PerfProfile"
        private const val MB = 1024L * 1024L

        /** Perfil por defecto para previews/tests: conservative y sin I/O real. */
        val Default: DevicePerformanceProfile = DevicePerformanceProfile(
            tier = PerformanceTier.MID,
            memoryClassMb = 192,
            isLowRamDevice = false,
            maxRefreshRateHz = 60f,
            frameBudgetMs = 16L,
            imageMemoryCacheBytes = 192L * MB,
            imageDiskCacheBytes = 128L * MB,
            imagePrecision = Precision.INEXACT,
            useHardwareLayerForDrawing = true
        )

        fun detect(context: Context): DevicePerformanceProfile {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryClassMb = am?.memoryClass ?: DEFAULT_MEMORY_CLASS_MB
            val lowRam = am?.isLowRamDevice == true
            val refresh = maxRefreshRate(context)
            val tier = tierOf(memoryClassMb, lowRam, refresh)
            val profile = DevicePerformanceProfile(
                tier = tier,
                memoryClassMb = memoryClassMb,
                isLowRamDevice = lowRam,
                maxRefreshRateHz = refresh,
                frameBudgetMs = frameBudgetOf(refresh),
                imageMemoryCacheBytes = imageMemoryBytesOf(tier, memoryClassMb),
                imageDiskCacheBytes = imageDiskBytesOf(tier),
                imagePrecision = if (tier == PerformanceTier.HIGH) Precision.EXACT else Precision.INEXACT,
                useHardwareLayerForDrawing = tier != PerformanceTier.LOW
            )
            Log.i(
                TAG,
                "tier=${tier} ram=${memoryClassMb}MB lowRam=$lowRam refresh=${refresh}Hz frame=${profile.frameBudgetMs}ms"
            )
            return profile
        }

        private fun tierOf(memoryMb: Int, lowRam: Boolean, refreshHz: Float): PerformanceTier = when {
            lowRam || memoryMb <= 96 -> PerformanceTier.LOW
            memoryMb >= 256 && refreshHz >= 90f -> PerformanceTier.HIGH
            else -> PerformanceTier.MID
        }

        private fun frameBudgetOf(refreshHz: Float): Long {
            val safeHz = if (refreshHz.isFinite() && refreshHz > 1f) refreshHz else 60f
            return (1000f / safeHz).toLong().coerceAtLeast(1L)
        }

        /** Porcentaje del heap de la app dedicado a bitmaps decodificados. */
        private fun imageMemoryBytesOf(tier: PerformanceTier, memoryClassMb: Int): Long {
            val fraction = when (tier) {
                PerformanceTier.HIGH -> 0.25
                PerformanceTier.MID -> 0.20
                PerformanceTier.LOW -> 0.10
            }
            return (memoryClassMb.toLong() * MB * fraction).toLong().coerceAtLeast(16L * MB)
        }

        private fun imageDiskBytesOf(tier: PerformanceTier): Long = when (tier) {
            PerformanceTier.HIGH -> 256L * MB
            PerformanceTier.MID -> 128L * MB
            PerformanceTier.LOW -> 48L * MB
        }

        private fun maxRefreshRate(context: Context): Float = try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val current = display?.mode
            val modes = display?.supportedModes.orEmpty()
            val best = if (current == null) null else modes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate }
            (best ?: current)?.refreshRate ?: 60f
        } catch (e: Exception) {
            Log.w(TAG, "refresh rate detection failed, assuming 60Hz", e)
            60f
        }

        private const val DEFAULT_MEMORY_CLASS_MB = 192
    }
}

/**
 * Inyecta el perfil en el árbol de composición. `staticCompositionLocalOf` es intencionado:
 * el perfil NO cambia durante la vida del proceso, así que leerlo no debe registrar
 * invalidaciones (cero coste en recomposición).
 */
val LocalPerformanceProfile = staticCompositionLocalOf { DevicePerformanceProfile.Default }

@Composable
fun ProvidePerformanceProfile(
    profile: DevicePerformanceProfile,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPerformanceProfile provides profile, content = content)
}
