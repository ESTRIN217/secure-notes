package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Con qué id se escribe la nota que se está editando, y si queda algo por descartar (spec 027).
 *
 * Existe porque `noteId == 0` **parece** una verdad sobre la nota y en realidad son dos hechos distintos:
 * «no hay nota todavía» y «la nota es esta pero ya tiene fila». El editor solo conocía el primero, así que
 * escribía cada vez con `id = 0`, la base insertaba una nota nueva **por cada escritura**, y el aviso de
 * «descartar» seguía preguntando por contenido que ya estaba en la base de datos (RF-180, RF-182).
 *
 * Las dos reglas son **puras** y no importan `android`: se prueban sin Robolectric, que no se ejecuta en esta
 * plataforma (RNF-113). Quien las aplica es [DraftWriteGate], y quien las consulta, el editor y la barra de
 * pestañas (RF-186).
 */

/**
 * Hay una escritura lanzada y todavía no ha vuelto de la base de datos el id de la fila.
 *
 * No es un id: es un estado, y por eso es negativo. Sirve para dos cosas —que la escritura se serialice detrás
 * de la que ya está en marcha (RF-181) y que el aviso de «descartar» se apague en el acto (RF-182)—.
 */
const val WRITE_IN_FLIGHT = -1

/**
 * El id con el que se escribe: `0` significa **insertar** (RF-180).
 *
 * La pestaña manda siempre que tenga id. Si es un borrador, manda la fila en cuanto existe, y mientras no —o
 * mientras la escritura anterior sigue en vuelo— se inserta.
 */
fun draftWriteId(noteId: Int, writeState: Int): Int = when {
    noteId != 0 -> noteId
    writeState > 0 -> writeState
    else -> 0
}

/**
 * ¿Queda algo por guardar, de modo que el aviso de «descartar» tenga algo que avisar? (RF-182, RF-183)
 *
 * Solo mientras el borrador **no** se haya escrito: en cuanto se lanza una escritura, lo que hay en pantalla ya
 * está yendo a la base de datos. Y si no hay contenido, no hay nada que perder.
 */
fun isDraftUnsaved(noteId: Int, writeState: Int, hasContent: Boolean): Boolean =
    noteId == 0 && writeState == 0 && hasContent

/**
 * La puerta de escritura de la nota que se está editando.
 *
 * Hace tres cosas, y las tres son la misma: **que un borrador acabe en una sola fila**.
 *
 * 1. **Recuerda** el id que devolvió la primera inserción, para que lo de después actualice esa fila en vez de
 *    crear otra (RF-180). Es lo que hoy se tira: `saveNote` no lo expone y la pestaña sigue siendo `note:0`.
 * 2. **Serializa** las escrituras con un candado (RF-181). Sin él, dos escrituras seguidas —adjuntar un fichero
 *    y pulsar atrás— leen las dos que el borrador no tiene fila e insertan dos. El id se lee **dentro** del
 *    candado: leído antes, sería el de antes.
 * 3. **Marca «en vuelo» al lanzar**, para que el aviso de «descartar» se apague en el acto y no un fotograma
 *    después (RF-182).
 *
 * El estado es observable porque el aviso tiene que **reaccionar** a que la inserción termine; el resto —la
 * regla y el candado— no necesita Compose. Se escribe en el ámbito del `ViewModel`, no en el de la pantalla, para
 * que una rotación con la escritura a medias no la cancele (RNF-119).
 */
class DraftWriteGate(private val noteId: Int) {

    private val lock = Mutex()

    /** Lo que se sabe de la escritura: nunca escrita (`0`), en vuelo ([WRITE_IN_FLIGHT]) o el id de la fila. */
    var state by mutableIntStateOf(if (noteId != 0) noteId else 0)
        private set

    /** Hay una escritura lanzada: lo de la pantalla está yendo a la base de datos, así que no hay nada que perder. */
    fun launch() {
        if (noteId == 0) state = WRITE_IN_FLIGHT
    }

    /**
     * Escribe la nota y devuelve el id del guardado (RF-180, RF-181, RF-184).
     *
     * [save] recibe el id con el que escribir —`0` para insertar— y devuelve el id que resultó de guardarla.
     * Es el mismo camino para un borrador que para una nota que ya existía: la diferencia está solo en el id.
     */
    suspend fun write(save: suspend (Int) -> Int): Int = lock.withLock {
        val destino = draftWriteId(noteId, state)
        val id = save(destino)
        if (destino == 0) state = if (id > 0) id else 0
        id
    }
}