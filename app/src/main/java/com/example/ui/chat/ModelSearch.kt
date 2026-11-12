package com.example.ui.chat

import com.example.data.ai.OnDeviceModel

/**
 * Búsqueda y orden del selector de modelo (spec 033, RF-229/RF-233, RNF-146).
 *
 * Son **funciones puras** sin `android` ni Compose: la consulta llega como `String` y sale una
 * decisión booleana u ordenado, así que corren en tests JVM donde Robolectric no corre.
 */

/** Clave de favorito de un modelo on-device (RF-233). */
fun favoriteOnDeviceKey(id: String): String = "gguf:$id"

/** Clave de favorito de un modelo de Ollama (RF-233). */
fun favoriteOllamaKey(name: String): String = "ollama:$name"

/**
 * ¿Coincide `query` con alguno de [parts]? La consulta en blanco coincide con todo; la
 * comparación ignora mayúsculas y tildes limpias de espacios.
 */
fun queryMatches(query: String, vararg parts: String?): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase()
    return parts.any { it?.trim()?.lowercase()?.contains(q) == true }
}

/** El catálogo filtrado por la consulta (RF-229): nombre, id, quantización, archivo y repo. */
fun filterOnDeviceModels(models: List<OnDeviceModel>, query: String): List<OnDeviceModel> =
    models.filter { queryMatches(query, it.displayName, it.id, it.quantLabel, it.ggufFileName, it.huggingFaceRepo) }

/** La lista de Ollama filtrada por la consulta (RF-229). */
fun filterOllamaModels(names: List<String>, query: String): List<String> =
    names.filter { queryMatches(query, it) }

/** Favoritos primero con orden estable dentro de cada grupo (RF-233). */
fun <T> favoritesFirst(items: List<T>, isFavorite: (T) -> Boolean): List<T> {
    val favorites = items.filter(isFavorite)
    if (favorites.isEmpty()) return items
    return favorites + items.filterNot(isFavorite)
}
