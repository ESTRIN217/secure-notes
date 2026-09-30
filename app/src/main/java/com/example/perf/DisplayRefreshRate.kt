package com.example.perf

import android.app.Activity
import android.util.Log
import android.view.Display
import android.view.WindowManager

/**
 * Pide al compositor la tasa de refresco más alta disponible para la resolución actual.
 *
 * Android elige el modo de pantalla por defecto (normalmente 60 Hz) salvo que la app lo pida
 * explícitamente. Se fija `preferredDisplayModeId` (el sistema lo honra siempre) y
 * `preferredRefreshRate` como respaldo para dispositivos que solo respeten el refresco.
 */
object DisplayRefreshRate {

    private const val TAG = "RefreshRate"

    /**
     * @return la tasa de refresco solicitada en Hz (o la actual si no se pudo mejorar).
     */
    fun requestPeak(activity: Activity): Float {
        val window = activity.window ?: return 60f
        val display = resolveDisplay(activity, window)
        val current = display?.mode ?: return 60f
        val peak = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: return current.refreshRate
        applyMode(window, peak)
        Log.i(TAG, "requested mode=${peak.modeId} @ ${peak.refreshRate}Hz (was ${current.refreshRate}Hz)")
        return peak.refreshRate
    }

    /** Reaplica la tasa máxima (p. ej. al volver del segundo plano o tras un cambio de pantalla). */
    fun reapplyPeak(activity: Activity) {
        runCatching { requestPeak(activity) }
            .onFailure { Log.w(TAG, "could not re-apply peak refresh rate", it) }
    }

    private fun applyMode(window: android.view.Window, mode: Display.Mode) {
        val lp = window.attributes
        lp.preferredDisplayModeId = mode.modeId
        lp.preferredRefreshRate = mode.refreshRate
        window.attributes = lp
    }

    private fun resolveDisplay(activity: Activity, window: android.view.Window): Display? =
        runCatching { activity.display }.getOrNull()
            ?: runCatching {
                @Suppress("DEPRECATION")
                (window.context.getSystemService(Activity.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }.getOrNull()
}
