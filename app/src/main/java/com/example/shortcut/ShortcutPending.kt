package com.example.shortcut

/**
 * La espera de un atajo que llegó con la bóveda cerrada (spec 021, RF-127).
 *
 * Tres piezas, todas puras, y por eso todas se prueban sin `Robolectric` —que en esta plataforma no se
 * ejecuta—: el destino ([ShortcutDestination]), dónde se guarda mientras espera ([ShortcutPending]) y la
 * decisión de ejecutar o esperar ([decidir]).
 *
 * **Por qué tres casos y no un `ShortcutDestination?`**: `null` significaría a la vez «no hay destino» y «queda
 * pendiente», y confundirlas deja un atajo esperando para siempre sin que nada avise. Nombrar los tres casos
 * obliga al compilador a quejarse si mañana se añade un cuarto.
 */
sealed interface ShortcutDecision {
    /** La bóveda está abierta: el destino se cumple ahora. */
    data class Ejecutar(val destino: ShortcutDestination) : ShortcutDecision

    /** La bóveda está cerrada: el destino queda guardado hasta que se abra. */
    data class Pendiente(val destino: ShortcutDestination) : ShortcutDecision

    /** No hay destino, o el extra no dice nada que la app sepa interpretar (RF-129). */
    data object Ignorar : ShortcutDecision
}

/**
 * El único destino pendiente, como máximo.
 *
 * Vive en el `companion` de `MainActivity` porque quien lo escribe (`intentRelay`) y quien lo lee (el
 * `LaunchedEffect` que reacciona al desbloqueo) están en sitios distintos de la misma clase.
 *
 * Es una **clase y no un `object`** a propósito: con estado global una prueba deja un destino colgado y el
 * fallo aparece en la prueba siguiente, no en la que lo introdujo.
 */
class ShortcutPending {
    var destino: ShortcutDestination? = null
        private set

    /** Guarda el destino. **El último gana** (caso límite 4): cumplir los dos sería saltar de pantalla solo. */
    fun registra(nuevo: ShortcutDestination) {
        destino = nuevo
    }

    /** Devuelve el destino pendiente y **lo vacía**: si no, se cumpliría dos veces, al desbloquear y al volver. */
    fun drena(): ShortcutDestination? = destino.also { destino = null }
}

/**
 * Decide qué hacer con la acción de un `Intent`.
 *
 * El orden importa: primero se traduce la acción y, si no es de un atajo, se ignora —**también con la bóveda
 * cerrada**—. Un atajo que nadie sabe interpretar no tiene que esperar a que el usuario se identifique para
 * luego no hacer nada.
 *
 * @param abierta si la bóveda está abierta ahora mismo (sin contraseña maestra, siempre).
 * @param pendiente dónde se guarda el destino si todavía no se puede cumplir.
 */
fun decidir(accion: String?, abierta: Boolean, pendiente: ShortcutPending): ShortcutDecision {
    val destino = ShortcutDestination.desdeAccion(accion) ?: return ShortcutDecision.Ignorar
    if (abierta) return ShortcutDecision.Ejecutar(destino)
    pendiente.registra(destino)
    return ShortcutDecision.Pendiente(destino)
}