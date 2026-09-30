package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.svg.SvgDecoder
import com.example.data.ai.AiModelHost
import com.example.perf.DevicePerformanceProfile
import java.util.concurrent.atomic.AtomicInteger

/**
 * Contenedor de dependencias de alcance "proceso".
 *
 * - [aiModelHost] es lo único que puede materializar el motor de IA: se construye una vez y
 *   solo toca el disco/los pesos cuando alguien lo adquiere.
 * - [performanceProfile] se detecta una vez y se comparte con la UI y los ViewModels.
 */
class SecureNotesApplication : Application(), SingletonImageLoader.Factory {

    private val startedActivities = AtomicInteger(0)

    val performanceProfile: DevicePerformanceProfile by lazy { DevicePerformanceProfile.detect(this) }

    val aiModelHost: AiModelHost by lazy { AiModelHost(this) }

    /** `true` mientras haya al menos una Activity visible (evita tirar el modelo al rotar). */
    fun hasVisibleActivity(): Boolean = startedActivities.get() > 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(VisibilityCallbacks())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val profile = performanceProfile
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(SvgDecoder.Factory())
            }
            // Caché de memoria: solo bitmaps ya decodificados y con tamaño controlado por el
            // heap de la app (evita presión de memoria al pasar fotos en la lista de notas).
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(profile.imageMemoryCacheBytes)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Directorio por defecto de Coil (cacheDir/image_cache); solo limitamos el tamaño
            // para no dejar que la caché de disco crezca sin límite.
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(profile.imageDiskCacheBytes)
                    .build()
            }
            .precision(profile.imagePrecision)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private inner class VisibilityCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) {
            startedActivities.incrementAndGet()
        }
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) {
            val remaining = startedActivities.decrementAndGet()
            if (remaining <= 0) Log.i(TAG, "app oculta: los pesos de IA pueden liberarse")
        }
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private companion object {
        const val TAG = "SecureNotesApp"
    }
}
