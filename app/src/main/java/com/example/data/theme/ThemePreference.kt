package com.example.data.theme

import com.estrin217.tema.AppTheme
import com.estrin217.tema.AppThemes

/**
 * Qué tema estaba usando quien ya usaba la app, sin escribir nada (spec 025, RF-164, RF-165, RNF-104).
 *
 * Antes de la spec el interruptor de colores dinámicos era un `boolean` con `true` por defecto, así que "el tema"
 * no estaba guardado en ninguna parte: estaba repartido entre un interruptor y una línea base de colores morados.
 * Quitar el interruptor sin más habría dejado a quien lo tenía apagado con Material You, que es justo lo contrario
 * de lo que había elegido.
 *
 * De ahí la regla, que son tres ramas y ninguna necesita `Context` ni disco:
 *
 * - Hay tema guardado → ese, sea el que sea. Si no existe en el catálogo, [AppThemes.byId] cae en el dinámico.
 * - No hay, pero el interruptor estaba **apagado** → `default`, que es el morado que esa persona estaba viendo.
 * - No hay nada, o el interruptor estaba encendido → el dinámico, que es lo que la app muestra por defecto.
 *
 * El parámetro [legacyDynamicColor] es `null` cuando la clave antigua **nunca se escribió**, que no es lo mismo que
 * `false`: la primera vez que se abre la app no hay nada guardado y hay que quedarse donde se estaba.
 */
fun resolveStoredTheme(storedId: String?, legacyDynamicColor: Boolean?): AppTheme = when {
    storedId != null -> AppThemes.byId(storedId)
    legacyDynamicColor == false -> AppThemes.byId(DEFAULT_THEME_ID)
    else -> AppThemes.dynamic
}

/** El identificador del tema que era el de la app antes de esta spec. */
const val DEFAULT_THEME_ID = "default"