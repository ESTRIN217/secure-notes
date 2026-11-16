package com.example.ui.chat

import com.example.data.ai.AiBackend
import com.example.data.ai.ModelState

/**
 * Visibilidad de la barra de carga del modelo y del banner de «no disponible» (spec 035).
 *
 * Piezas puras (sin `android`): la pantalla pinta estado y lanza eventos; aquí vive la decisión.
 */

/** Spec 035 (RF-258): barra lineal visible solo con backend on-device y motor en LOADING. */
fun shouldShowModelLoadingIndicator(backend: AiBackend, modelState: ModelState): Boolean =
    backend == AiBackend.ON_DEVICE && modelState == ModelState.LOADING

/**
 * Spec 035 (RF-259): el banner de error se reserva a NOT_LOADED/ERROR y a Ollama sin conexión.
 * Mientras el motor carga (`modelLoading = true`) no se pinta, aunque la conversación esté vacía.
 */
fun shouldShowUnavailableError(modelLoading: Boolean, available: Boolean, historyEmpty: Boolean): Boolean =
    !modelLoading && !available && historyEmpty
