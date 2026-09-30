package com.example.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Propietario del "sesión de IA local": es lo ÚNICO que crea [OnDeviceService] / [LlamaCppEngine]
 * y, por tanto, lo único que puede llegar a materializar el motor nativo y los pesos del modelo.
 *
 * Reglas que impone este host:
 *  1. **Nada de IA hasta que el usuario la pide.** El grafo de servicios se construye en el
 *     primer `acquire()`; los pesos solo se leen del disco en `ensureReady()`.
 *  2. **Los `StateFlow` son observables sin crear nada.** `modelState` / `loadedModelInfo` viven
 *     aquí y se replican desde el servicio cuando (y solo cuando) este existe.
 *  3. **Contado por referencias.** Mientras alguien `acquire()`-a el modelo se mantiene en RAM;
 *     al soltar el último (`release()`) se agenda una descarga perezosa; si la app deja de estar
 *     visible se libera de inmediato (`releaseAll()`).
 *
 * No se llama nunca a `destroy()` del binding de llama.cpp: su `companion` cachea el singleton
 * JNI y destruirlo envenenaría el motor para el resto del proceso (solo `cleanUp()` es seguro).
 */
class AiModelHost(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadMutex = Mutex()
    private val graphLock = Any()

    private val _modelState = MutableStateFlow(ModelState.NOT_LOADED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _loadedModelInfo = MutableStateFlow<LoadedModelInfo?>(null)
    val loadedModelInfo: StateFlow<LoadedModelInfo?> = _loadedModelInfo.asStateFlow()

    private var service: OnDeviceService? = null
    private var mirrorJob: Job? = null
    private var idleJob: Job? = null
    private var holders = 0
    private var loader: (suspend (OnDeviceService) -> Result<Unit>)? = null

    /** `true` solo si ya se construyó el grafo de servicios (NO implica pesos en memoria). */
    val isGraphCreated: Boolean get() = synchronized(graphLock) { service != null }

    /** El servicio existente, o `null` sin crearlo. Nunca inicializa nada por efecto secundario. */
    fun serviceOrNull(): OnDeviceService? = synchronized(graphLock) { service }

    /** Registra quién sabe qué modelo cargar (lo aporta el `AiViewModel`, que conoce prefs/UX). */
    fun installLoader(loader: suspend (OnDeviceService) -> Result<Unit>) {
        synchronized(graphLock) { this.loader = loader }
    }

    /** Registra interés en el modelo: crea el grafo de servicios (barato) la primera vez. */
    fun acquire(): OnDeviceService {
        idleJob?.cancel()
        synchronized(graphLock) { holders++ }
        return resolveService()
    }

    /** Suelta una referencia; agenda la descarga si ya no queda ninguna. */
    fun release() {
        val remaining = synchronized(graphLock) { if (holders > 0) --holders else 0 }
        if (remaining == 0) scheduleIdleUnload()
    }

    /** Descarga inmediata (app en segundo plano / acción explícita del usuario). */
    fun releaseAll(reason: String) {
        idleJob?.cancel()
        synchronized(graphLock) { holders = 0 }
        launchUnload(reason)
    }

    /**
     * Garantiza pesos en memoria. Idempotente y serializado: varias llamadas concurrentes
     * (p. ej. abrir el asistente + pulsar "enviar") comparten una única carga.
     */
    suspend fun ensureReady(): Result<Unit> = loadMutex.withLock {
        val svc = resolveService()
        when (svc.modelState.value) {
            ModelState.READY -> return@withLock Result.success(Unit)
            ModelState.LOADING -> return@withLock Result.failure(IllegalStateException("load in progress"))
            else -> Unit
        }
        val load = synchronized(graphLock) { loader }
            ?: return@withLock Result.failure(IllegalStateException("no model loader registered"))
        Log.i(TAG, "ensureReady: materializando pesos bajo demanda")
        val result = load(svc)
        if (result.isFailure) Log.e(TAG, "ensureReady failed", result.exceptionOrNull())
        result
    }

    private fun resolveService(): OnDeviceService = synchronized(graphLock) {
        service?.let { return it }
        val created = OnDeviceService(LlamaCppEngine(appContext), appContext)
        service = created
        mirrorJob = scope.launch { mirror(created) }
        Log.i(TAG, "Grafo de IA creado bajo demanda (el motor nativo sigue sin inicializar)")
        created
    }

    private suspend fun mirror(svc: OnDeviceService) {
        combine(svc.modelState, svc.loadedModelInfo) { state, info -> state to info }
            .collect { (state, info) ->
                _modelState.value = state
                _loadedModelInfo.value = info
            }
    }

    private fun scheduleIdleUnload() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_UNLOAD_DELAY_MS)
            if (holdersCount() == 0) launchUnload("idle")
        }
    }

    private fun launchUnload(reason: String) {
        val svc = serviceOrNull() ?: return
        // Si nunca se cargó un modelo, ni se toca la capa nativa (evita crear el engine en JNI).
        if (svc.modelState.value == ModelState.NOT_LOADED) return
        Log.i(TAG, "Liberando pesos del modelo (motivo=$reason)")
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { svc.unloadModel() } }
                .onFailure { Log.e(TAG, "unload falló", it) }
        }
    }

    private fun holdersCount(): Int = synchronized(graphLock) { holders }

    private companion object {
        const val TAG = "AiModelHost"

        /** Margen antes de liberar por inactividad: evita recargar pesos al saltar entre pantallas. */
        const val IDLE_UNLOAD_DELAY_MS = 120_000L
    }
}
