package com.example.data.backup

import android.content.SharedPreferences
import com.estrin217.tema.AppTheme
import com.estrin217.tema.AppThemes
import com.example.AppConstants
import com.example.PasswordType
import com.example.data.theme.resolveStoredTheme
import org.json.JSONObject

/**
 * Cuál es el tipo de un ajuste que viaja en la copia. Es la **única** tabla de esa correspondencia: la
 * usan tanto las dos funciones puras de este fichero como las dos que leen y escriben las preferencias,
 * para que un tipo no pueda estar bien en un sentido y mal en el otro.
 */
enum class SettingType { TEXT, FLAG, NUMBER }

/**
 * Un ajuste **con su tipo ya resuelto**: si el valor no es del tipo que declara la lista, no hay ajuste.
 *
 * [value] está en la base como `Any` para poder escribirla en el bloque de la copia sin mirar el tipo, y
 * en cada subclase con su tipo, que es como se lee cuando se importa.
 *
 * El nombre no dice «restaurado» a propósito, porque la tabla también se usa al **copiar**: es un ajuste
 * con su tipo, ni más ni menos.
 */
sealed class BackupSetting(val key: String, open val value: Any) {
    class Text(key: String, override val value: String) : BackupSetting(key, value)
    class Flag(key: String, override val value: Boolean) : BackupSetting(key, value)
    class Number(key: String, override val value: Long) : BackupSetting(key, value)

    companion object {
        /**
         * Envuelve el valor con el tipo que declara la lista, o `null` si **no** es de ese tipo.
         *
         * Que se salte en vez de convertir es lo que evita que un `boolean` acabe escrito como la cadena
         * `"true"` en las preferencias: son cosas distintas y solo una se puede deshacer.
         */
        fun de(key: String, tipo: SettingType, valor: Any?): BackupSetting? = when (tipo) {
            SettingType.TEXT -> (valor as? String)?.let { Text(key, it) }
            SettingType.FLAG -> (valor as? Boolean)?.let { Flag(key, it) }
            // `kotlin.Number` hay que nombrarlo entero: dentro de esta clase, `Number` es la subclase
            // `BackupSetting.Number` y el compilador lo toma por el otro.
            SettingType.NUMBER -> (valor as? kotlin.Number)?.let { Number(key, it.toLong()) }
        }
    }
}

/**
 * Los ajustes que viajan en la copia de seguridad (spec 028, RF-187, RF-191).
 *
 * **Esta es la lista.** Estaba replicada en cinco sitios —los tres que crean la copia y los dos que la
 * restauran— y por eso el tema elegido se quedó fuera de las cinco: no fue un descuido al añadir el
 * tema, fue el precio de no tener un sitio único. Las cinco funciones pasan ahora por aquí.
 *
 * Cada clave se declara **una** vez, con su tipo. Añadir un ajuste a la copia es añadirle **una línea**.
 */
val BACKUP_SETTINGS: Map<String, SettingType> = mapOf(
    AppConstants.DARK_MODE_OPTION_KEY to SettingType.TEXT,
    AppConstants.DYNAMIC_COLORS_KEY to SettingType.FLAG,
    AppConstants.LANGUAGE_KEY to SettingType.TEXT,
    AppConstants.AUTO_UPDATE_CHECK_KEY to SettingType.FLAG,
    AppConstants.UPDATE_NOTIFICATIONS_KEY to SettingType.FLAG,
    AppConstants.CUSTOM_ORDER_KEY to SettingType.TEXT,
    AppConstants.PASSWORD_TYPE_KEY to SettingType.TEXT,
    AppConstants.BIOMETRIC_ENABLED_KEY to SettingType.FLAG,
    AppConstants.SCREENSHOT_ENABLED_KEY to SettingType.FLAG,
    AppConstants.AUTO_LOCK_TIMEOUT_KEY to SettingType.NUMBER,
    AppConstants.ENCRYPT_BACKUPS_KEY to SettingType.FLAG,
    AppConstants.AUTO_BACKUP_ENABLED_KEY to SettingType.FLAG,
    AppConstants.AUTO_BACKUP_INTERVAL_KEY to SettingType.TEXT,
    AppConstants.THEME_ID_KEY to SettingType.TEXT
)

/**
 * El valor por defecto de cada ajuste, **los mismos que se leían antes** en cada uno de los tres sitios
 * que crean la copia.
 *
 * Vive en una lista y no dentro de las lecturas de preferencias por un motivo comprobable: aquí una
 * prueba puede comparar los catorce con su literal y fijar que el refactor no cambió **de paso** lo que
 * viaja. Si un defecto pasara de `"SYSTEM"` a `""`, la copia dejaría de llevar el modo «sistema» sin que
 * nadie lo hubiera tocado, y ninguna prueba de las que leen preferencias lo notaría.
 *
 * El del tema es el **dinámico**, que es lo que la app muestra de fábrica. [readBackupSettings] nunca
 * llega a usarlo: resuelve el tema con [com.example.data.theme.resolveStoredTheme].
 */
val DEFAULT_SETTINGS: Map<String, Any> = mapOf(
    AppConstants.DARK_MODE_OPTION_KEY to "SYSTEM",
    AppConstants.DYNAMIC_COLORS_KEY to true,
    AppConstants.LANGUAGE_KEY to "",
    AppConstants.AUTO_UPDATE_CHECK_KEY to true,
    AppConstants.UPDATE_NOTIFICATIONS_KEY to true,
    AppConstants.CUSTOM_ORDER_KEY to "",
    AppConstants.PASSWORD_TYPE_KEY to PasswordType.PASSWORD.name,
    AppConstants.BIOMETRIC_ENABLED_KEY to false,
    AppConstants.SCREENSHOT_ENABLED_KEY to false,
    AppConstants.AUTO_LOCK_TIMEOUT_KEY to AppConstants.AUTO_LOCK_TIMEOUT_DEFAULT,
    AppConstants.ENCRYPT_BACKUPS_KEY to false,
    AppConstants.AUTO_BACKUP_ENABLED_KEY to false,
    AppConstants.AUTO_BACKUP_INTERVAL_KEY to "6h",
    AppConstants.THEME_ID_KEY to AppThemes.dynamic.id
)

/**
 * El bloque `settings` de la copia, con lo que hay en [values] (RF-187).
 *
 * Puro, sin `SharedPreferences` ni `Context`: entra un mapa y sale un bloque. Solo viaja lo que está en
 * [BACKUP_SETTINGS] y solo si el valor es del tipo que declara, así que ni una clave de sobra ni un tipo
 * equivocado llegan al fichero.
 */
fun backupSettingsJson(values: Map<String, Any>): JSONObject {
    val bloque = JSONObject()
    BACKUP_SETTINGS.forEach { (clave, tipo) ->
        BackupSetting.de(clave, tipo, values[clave])?.let { bloque.put(clave, it.value) }
    }
    return bloque
}

/**
 * Qué ajustes de **esta** copia se pueden escribir (RF-188, RF-189, RF-190).
 *
 * Puro, y es donde vive la decisión de qué se restaura:
 *
 * - Lo que la copia **no trae** no se escribe (RF-190). Una copia hecha antes de esta spec no trae tema,
 *   y no tocarla es lo que la deja seguir siendo segura.
 * - Lo que llega con un **tipo que no cuadra** se salta, y solo él: el resto de la copia se aplica igual.
 * - Lo que llega con un **tema que esta versión no tiene** no se escribe (RF-189). Se conserva el que hay,
 *   porque una copia vieja no puede cambiarle el aspecto a alguien sin que lo haya pedido.
 */
fun restorableSettings(json: JSONObject): List<BackupSetting> =
    BACKUP_SETTINGS.mapNotNull { (clave, tipo) -> restaurar(json, clave, tipo) }

private fun restaurar(json: JSONObject, clave: String, tipo: SettingType): BackupSetting? {
    val ajuste = BackupSetting.de(clave, tipo, json.opt(clave)) ?: return null
    if (ajuste is BackupSetting.Text && clave == AppConstants.THEME_ID_KEY && !existeElTema(ajuste.value)) {
        return null
    }
    return ajuste
}

/**
 * Si el catálogo de temas tiene ese identificador.
 *
 * Se pregunta al **catálogo** y no a `AppThemes.byId`, porque `byId` cae al dinámico cuando no lo
 * encuentra: eso sirve para pintar, y aquí lo que se quiere es saber si el tema **existe**. En blanco
 * tampoco existe, así que un tema en blanco es un tema desconocido.
 */
private fun existeElTema(id: String): Boolean = AppThemes.all.any { it.id == id }

// ── Las dos adaptadoras ─────────────────────────────────────────────────────────────────
//
// Ninguna de las dos decide nada: la decisión está arriba, en las dos funciones puras. Estas son
// fontantería, y es justo por eso por lo que se prueban con un doble de `SharedPreferences`: un tipo
// equivocado aquí no lo vería ninguna prueba de arriba, porque arriba el valor ya llega tipado.

/**
 * Los catorce ajustes de [BACKUP_SETTINGS] leídos de las preferencias (RF-187).
 *
 * El tema se **resuelve** en vez de copiarse en crudo: quien nunca abrió el selector tiene el
 * interruptor antiguo apagado y ningún `theme_id` guardado, y su copia tiene que decir `default`, que es
 * el morado que esa persona estaba viendo. Copiar el identificador en crudo dejaría a esa persona sin
 * tema en la copia, que es justo el caso que vino a arreglar esta spec.
 *
 * Los dos valores del tema se leen **igual** que los lee `SharedPreferencesRepository.getAppTheme()`,
 * y la regla que hay detrás es [resolveStoredTheme], que ya tiene sus pruebas en
 * `ThemePreferenceTest`. Si esa migración cambiara, hay que tocar los dos lectores; por eso el KDoc lo
 * dice en voz alta en vez de dejar los dos callados.
 */
fun readBackupSettings(prefs: SharedPreferences): Map<String, Any> {
    val leidos = BACKUP_SETTINGS.entries
        .filter { it.key != AppConstants.THEME_ID_KEY }
        .associate { (clave, tipo) -> clave to leer(prefs, clave, tipo) }
    return leidos + (AppConstants.THEME_ID_KEY to temaEnUso(prefs).id)
}

/**
 * Un ajuste con el tipo que declara la lista, o su valor por defecto si no está escrito.
 *
 * El tipo llega de parámetro y no se busca por clave a propósito: el recorrido de arriba ya lo tiene, y
 * buscarlo daría un `SettingType?` que obligaría a inventarse una rama muerta.
 */
private fun leer(prefs: SharedPreferences, clave: String, tipo: SettingType): Any {
    val porDefecto = DEFAULT_SETTINGS[clave]
    // El `?:` final repite el defecto de la lista y no un vacío: si la clave llegara escrita a nulo, el
    // intervalo de la copia automática volvería a "" en vez de a "6h", que es lo que se leía antes.
    return when (tipo) {
        SettingType.TEXT -> prefs.getString(clave, porDefecto as? String) ?: (porDefecto as? String ?: "")
        SettingType.FLAG -> prefs.getBoolean(clave, porDefecto as? Boolean ?: false)
        SettingType.NUMBER -> prefs.getLong(clave, (porDefecto as? Number)?.toLong() ?: 0L)
    }
}

/** El tema que está en uso, con la regla de la spec 025. */
private fun temaEnUso(prefs: SharedPreferences): AppTheme = resolveStoredTheme(
    prefs.getString(AppConstants.THEME_ID_KEY, null),
    if (prefs.contains(AppConstants.DYNAMIC_COLORS_KEY)) {
        prefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true)
    } else {
        null
    }
)

/**
 * Escribe en las preferencias lo que [restorableSettings] ha dicho que se puede escribir (RF-192).
 *
 * Cada ajuste con **su** método, porque `putString(clave, "true")` y `putBoolean(clave, true)` son dos
 * preferencias distintas y solo una se puede deshacer. Y todo en **una** edición con **un** `apply()`: la
 * lista ya viene filtrada y resuelta de una vez, así que no hay nada que hacer ajuste a ajuste.
 */
fun applyRestoredSettings(prefs: SharedPreferences, settings: List<BackupSetting>) {
    if (settings.isEmpty()) return
    val editor = prefs.edit()
    settings.forEach { ajuste ->
        when (ajuste) {
            is BackupSetting.Text -> editor.putString(ajuste.key, ajuste.value)
            is BackupSetting.Flag -> editor.putBoolean(ajuste.key, ajuste.value)
            is BackupSetting.Number -> editor.putLong(ajuste.key, ajuste.value)
        }
    }
    editor.apply()
}