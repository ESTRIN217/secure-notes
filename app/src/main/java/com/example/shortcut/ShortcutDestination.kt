package com.example.shortcut

/**
 * El destino al que lleva un atajo del lanzador (spec 021, RF-125).
 *
 * Es un **conjunto cerrado de cinco destinos**, y por eso es una función pura sin `android` ni Compose: el
 * destino viaja en la **acción** del `Intent` que dispara el atajo, y este fichero es lo único que sabe qué
 * significa cada palabra.
 *
 * **Por qué la acción y no un extra.** Un `<extra>` dentro de `<intent>` en `shortcuts.xml` no se puede
 * comprobar sin dispositivo —AAPT trata `res/xml` como fichero opaco y lo valida en tiempo de ejecución, así que
 * un `BUILD SUCCESSFUL` no dice nada—, y en la primera versión el destino viajaba en un extra que **no estaba
 * declarado**: los cinco atajos abrían la app y no hacían nada. La acción, en cambio, es el único atributo que la
 * plataforma **exige** en un `<intent>` de atajo, así que no hay forma de que pase inadvertida.
 *
 * **Que [desdeAccion] devuelva `null` en vez de reventar es parte del contrato** (RF-129): `MainActivity` es
 * `exported="true"` desde antes de esta spec —por los filtros que abren archivos—, así que cualquiera puede
 * construir un `Intent` con cualquier acción. Una acción desconocida tiene que abrir la lista y seguir.
 */
const val PREFIJO_ACCION = "com.estrin217.securenotes.shortcut."

enum class ShortcutDestination(val clave: String, val accion: String) {
    NEW_NOTE("new_note", "${PREFIJO_ACCION}NEW_NOTE"),
    SEARCH("search", "${PREFIJO_ACCION}SEARCH"),
    AI_CHAT("ai_chat", "${PREFIJO_ACCION}AI_CHAT"),
    SETTINGS("settings", "${PREFIJO_ACCION}SETTINGS"),
    CODE_EDITOR("code_editor", "${PREFIJO_ACCION}CODE_EDITOR");

    companion object {
        /**
         * Traduce la acción del `Intent` a su destino, o `null` si no es de un atajo.
         *
         * Cubre a los tres que llegan de verdad desde fuera: `ACTION_MAIN` al tocar el icono,
         * `ACTION_VIEW` de los widgets y del importador de archivos, y una acción cualquiera construida a mano.
         * Los tres tienen que salir por aquí como `Ignorar`, sin reventar y sin mover nada.
         */
        fun desdeAccion(accion: String?): ShortcutDestination? = entries.firstOrNull { it.accion == accion }

        /** Por clave, para las pruebas y para el invariante que cruza el XML con esta lista. */
        fun desdeClave(clave: String?): ShortcutDestination? = entries.firstOrNull { it.clave == clave }
    }
}