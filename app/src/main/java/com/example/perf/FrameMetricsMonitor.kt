package com.example.perf

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import com.example.BuildConfig

/**
 * Muestrea `FrameMetrics` del `ViewRootImpl` y reporta frames perdidos (jank).
 *
 * Es la forma barata y con coste cero de producción de "detectar" problemas de renderizado:
 * `FrameMetrics` lo entrega ya calculado por el sistema (no añade trabajo por frame).
 * Solo registra en builds DEBUG; en release no se registra ni se asigna nada.
 */
class FrameMetricsMonitor(
    private val window: Window,
    private val profile: DevicePerformanceProfile
) {
    private val handler = Handler(Looper.getMainLooper())
    private val totalDuration = LongArray(BUCKETS)
    private var bucketIndex = 0
    private var samples = 0
    private var attached = false

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        if (BuildConfig.DEBUG) onFrame(metrics)
    }

    fun start() {
        if (attached || !BuildConfig.DEBUG) return
        window.addOnFrameMetricsAvailableListener(listener, handler)
        attached = true
        Log.i(TAG, "frame metrics enabled (budget=${profile.frameBudgetMs}ms @ ${profile.maxRefreshRateHz}Hz)")
    }

    fun stop() {
        if (!attached) return
        window.removeOnFrameMetricsAvailableListener(listener)
        attached = false
    }

    private fun onFrame(metrics: FrameMetrics) {
        val rawDeadline = metrics.getMetric(FrameMetrics.DEADLINE)
        val deadlineNs: Float = if (rawDeadline > 0L) rawDeadline.toFloat()
        else profile.maxRefreshRateHz * 1_000_000f
        val totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        totalDuration[bucketIndex] = totalNs
        bucketIndex = (bucketIndex + 1) % BUCKETS
        samples++
        if (samples % REPORT_EVERY != 0) return
        report(deadlineNs)
    }

    private fun report(deadlineNs: Float) {
        val sorted = totalDuration.filter { it > 0 }.sorted()
        if (sorted.isEmpty()) return
        val janky = sorted.count { it > deadlineNs }
        val idx = ((sorted.size - 1) * 0.9f).toInt().coerceIn(0, sorted.lastIndex)
        val p90Ms = (sorted[idx] / 1_000_000f).coerceAtLeast(0f)
        val ratioPct = janky * 100f / sorted.size
        val label = if (ratioPct > JANK_WARN_PCT) "JANK" else "ok"
        Log.i(TAG, "frames=${sorted.size} janky=$janky (${ratioPct.toInt()}%) p90=${p90Ms}ms [$label]")
        totalDuration.fill(0L)
    }

    private companion object {
        const val TAG = "FrameMetrics"
        const val BUCKETS = 120
        const val REPORT_EVERY = 120
        const val JANK_WARN_PCT = 10f
    }
}
