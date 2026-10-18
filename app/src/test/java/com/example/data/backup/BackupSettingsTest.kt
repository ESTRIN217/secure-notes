package com.example.data.backup

import android.content.SharedPreferences
import com.estrin217.tema.AppThemes
import com.example.AppConstants
import com.example.PasswordType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué ajustes viajan en la copia de seguridad, y cuáles se pueden aplicar al restaurarla (spec 028).
 *
 * Son **pruebas puras**: ni `Context` ni Robolectric, que en esta plataforma no se ejecuta. Por eso
 * la decisión está en dos funciones que solo hablan `JSONObject` y `Map`, y las dos adaptadoras de
 * `SharedPreferences` —que no deciden nada— tienen su propio bloque más abajo.
 *
 * `org.json` funciona aquí sin Robolectric porque está declarado como dependencia de prueba
 * (`testImplementation(libs.org.json)`); el precedente es `UpdaterViewModelTest`.
 */
class BackupSettingsTest {

    private companion object {
        /** Identificador de tema que esta versión **no** tiene en su catálogo. */
        const val TEMA_DESCONOCIDO = "tema_que_no_existe"
    }

    /**
     * Los catorce valores de una copia bien hecha, con un valor **distinto** en cada clave: si uno se
     * colara en el sitio de otro, el valor distinto lo delata.
     */
    private val valores = mapOf<String, Any>(
        AppConstants.DARK_MODE_OPTION_KEY to "ON",
        AppConstants.DYNAMIC_COLORS_KEY to true,
        AppConstants.LANGUAGE_KEY to "es",
        AppConstants.AUTO_UPDATE_CHECK_KEY to false,
        AppConstants.UPDATE_NOTIFICATIONS_KEY to false,
        AppConstants.CUSTOM_ORDER_KEY to "3,1,2",
        AppConstants.PASSWORD_TYPE_KEY to PasswordType.PIN.name,
        AppConstants.BIOMETRIC_ENABLED_KEY to true,
        AppConstants.SCREENSHOT_ENABLED_KEY to true,
        AppConstants.AUTO_LOCK_TIMEOUT_KEY to 15L,
        AppConstants.ENCRYPT_BACKUPS_KEY to true,
        AppConstants.AUTO_BACKUP_ENABLED_KEY to true,
        AppConstants.AUTO_BACKUP_INTERVAL_KEY to "12h",
        AppConstants.THEME_ID_KEY to "one_dark"
    )

    /**
     * Compara el bloque con lo esperado, cada clave con su getter.
     *
     * No se comparan los `JSONObject` enteros porque su `equals` no es de fiar entre versiones: dos
     * bloques con las mismas claves pueden no ser iguales según el orden o el tipo con que se construyó
     * el número. Lo que importa es que **la clave está y su valor es el que tocaba**.
     */
    private fun assertCoincide(mensaje: String, json: JSONObject, esperados: Map<String, Any>) {
        assertEquals("$mensaje: las claves del bloque", esperados.keys, json.keys().asSequence().toSet())
        esperados.forEach { (clave, valor) ->
            when (valor) {
                is Boolean -> assertEquals(clave, valor, json.getBoolean(clave))
                is Long -> assertEquals(clave, valor, json.getLong(clave))
                else -> assertEquals(clave, valor.toString(), json.getString(clave))
            }
        }
    }

    private fun bloque(vararg pares: Pair<String, Any>): JSONObject =
        JSONObject().apply { pares.forEach { (clave, valor) -> put(clave, valor) } }

    // ── La lista: el olvido que arregla esta spec es una clave de menos ─────────────────────

    /**
     * El primero a propósito. El defecto era que `theme_id` no estaba en la lista, y una lista no se
     * comprueba probando lo que hace: se comprueba **contándola**. Por eso la cuenta es exacta.
     */
    @Test
    fun `la lista de ajustes tiene las catorce claves y el tema esta en ella`() {
        assertEquals(
            "La lista de la copia debe tener 14 claves: las 13 de siempre y el tema",
            14,
            BACKUP_SETTINGS.size
        )
        assertTrue(
            "El tema elegido tiene que viajar en la copia",
            AppConstants.THEME_ID_KEY in BACKUP_SETTINGS
        )
    }

    /**
     * Los catorce valores por defecto, uno a uno y con su literal.
     *
     * Los tres serializadores leen sus valores por defecto en su propio código, y el plan los mueve a
     * una lista. Esta prueba es lo que impide que ese movimiento **cambie de paso lo que viaja**: si un
     * defecto pasa de `"SYSTEM"` a `""`, la copia deja de llevar el modo «sistema» aunque nadie lo haya
     * tocado. El valor por defecto del tema es el dinámico, que es lo que la app muestra de fábrica.
     */
    @Test
    fun `los valores por defecto son los mismos que se leian antes`() {
        assertEquals(
            mapOf(
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
            ),
            DEFAULT_SETTINGS
        )
    }

    // ── Copiar: qué se lleva la copia ───────────────────────────────────────────────────────

    @Test
    fun `la copia lleva los catorce ajustes con su valor`() {
        assertCoincide("la copia", backupSettingsJson(valores), valores)
    }

    /**
     * Lo que **no** está en la lista no viaja. Es el otro lado de la lista única: sin esto, añadir una
     * clave sería « meterla en el mapa », y una copia podría llevar de más.
     */
    @Test
    fun `un ajuste que no esta en la lista no viaja`() {
        val conDeSobra = valores + ("clave_inventada" to "valor")
        val json = backupSettingsJson(conDeSobra)
        assertCoincide("la copia", json, valores)
        assertTrue("La clave inventada no puede aparecer en la copia", !json.has("clave_inventada"))
    }

    /**
     * Un valor del tipo equivocado **no** se copia. Convierte el fallo en algo visible: si una clave
     * declarada como texto recibiera un `boolean`, el bloque llevaría una cadena y la restauración
     * escribiría basura en las preferencias.
     */
    @Test
    fun `un valor de tipo equivocado no viaja`() {
        val json = backupSettingsJson(valores + (AppConstants.LANGUAGE_KEY to true))
        assertCoincide(
            "la copia",
            json,
            valores - AppConstants.LANGUAGE_KEY
        )
    }

    // ── Restaurar: qué se aplica de ESTA copia ─────────────────────────────────────────────

    @Test
    fun `se aplican los catorce ajustes y el tema con su valor`() {
        val ajustes = restorableSettings(backupSettingsJson(valores))
        assertEquals("Deben volver los catorce ajustes", 14, ajustes.size)
        val tema = ajustes.first { it.key == AppConstants.THEME_ID_KEY }
        assertEquals(
            "El tema tiene que volver con su identificador, no con su paleta",
            "one_dark",
            (tema as BackupSetting.Text).value
        )
    }

    /** Una copia hecha antes de esta spec no trae tema: se aplica todo lo demás y el tema no se toca. */
    @Test
    fun `una copia sin tema aplica el resto y no toca el tema`() {
        val ajustes = restorableSettings(backupSettingsJson(valores - AppConstants.THEME_ID_KEY))
        assertEquals("Deben volver los trece ajustes de siempre", 13, ajustes.size)
        assertTrue(
            "El tema no puede tocarse si la copia no lo trae",
            ajustes.none { it.key == AppConstants.THEME_ID_KEY }
        )
    }

    /**
     * El tema desconocido, con nombre propio y no de última: es el caso que decide esta spec. Una
     * copia vieja —o de una versión más nueva— trae un identificador que esta app no tiene, y lo que
     * hay que hacer es **no escribir nada**, no escribir un tema cualquiera.
     */
    @Test
    fun `un tema que esta version no tiene no se escribe`() {
        val ajustes = restorableSettings(backupSettingsJson(valores + (AppConstants.THEME_ID_KEY to TEMA_DESCONOCIDO)))
        assertEquals("Solo se salta el tema: los otros trece se aplican", 13, ajustes.size)
        assertTrue(
            "Un tema desconocido no puede pisar el que ya hay",
            ajustes.none { it.key == AppConstants.THEME_ID_KEY }
        )
    }

    /** En blanco **no** es «sin elegir»: es un identificador que tampoco existe en el catálogo. */
    @Test
    fun `un tema en blanco tampoco se escribe`() {
        val ajustes = restorableSettings(backupSettingsJson(valores + (AppConstants.THEME_ID_KEY to "")))
        assertTrue(
            "El tema en blanco no puede pisar el que ya hay",
            ajustes.none { it.key == AppConstants.THEME_ID_KEY }
        )
    }

    /** Y el que sí está en el catálogo se escribe, incluido el dinámico, que es un tema de verdad. */
    @Test
    fun `un tema del catalogo se escribe incluido el dinamico`() {
        listOf(AppThemes.dynamic.id, AppThemes.default.id, AppThemes.oneDark.id).forEach { id ->
            val ajustes = restorableSettings(backupSettingsJson(valores + (AppConstants.THEME_ID_KEY to id)))
            assertEquals(
                "El tema $id está en el catálogo y tiene que aplicarse",
                id,
                (ajustes.first { it.key == AppConstants.THEME_ID_KEY } as BackupSetting.Text).value
            )
        }
    }

    /** Una copia sin bloque de ajustes no rompe nada: se aplica una lista vacía. */
    @Test
    fun `una copia sin ajustes no falla`() {
        assertEquals(emptyList<BackupSetting>(), restorableSettings(JSONObject()))
    }

    /**
     * Un tipo que no cuadra se salta **solo**. Es lo que distingue una copia manipulada o de una versión
     * antigua de un fallo: la resto de la copia se aplica igual.
     */
    @Test
    fun `un ajuste de tipo equivocado se salta solo`() {
        val ajustes = restorableSettings(
            bloque(AppConstants.LANGUAGE_KEY to 7L, AppConstants.THEME_ID_KEY to "one_dark")
        )
        assertEquals("Solo se salta el idioma, que llega como número", 1, ajustes.size)
        assertEquals(AppConstants.THEME_ID_KEY, ajustes.single().key)
    }

    /**
     * Los tres tipos llegan **con su tipo**. Si todo se escribiera como texto, un `boolean` de
     * `SharedPreferences` y una cadena pasarían a ser lo mismo y el ajuste se rompería al aplicarse.
     */
    @Test
    fun `los tres tipos llegan con su tipo`() {
        val ajustes = restorableSettings(
            bloque(
                AppConstants.LANGUAGE_KEY to "es",
                AppConstants.BIOMETRIC_ENABLED_KEY to true,
                AppConstants.AUTO_LOCK_TIMEOUT_KEY to 15L
            )
        )
        assertEquals(3, ajustes.size)
        assertTrue(ajustes.any { it is BackupSetting.Text && it.value == "es" })
        assertTrue(ajustes.any { it is BackupSetting.Flag && it.value })
        assertTrue(ajustes.any { it is BackupSetting.Number && it.value == 15L })
    }

    // ── Las dos adaptadoras: leen y escriben preferencias ──────────────────────────────────
    //
    // Estas dos no deciden nada —la decisión ya está probada arriba—, pero son donde un tipo se
    // colaría sin que ninguna prueba de las de arriba lo notase: una clave declarada `FLAG` leída
    // como texto viajaría por la copia sin que nadie se entere. Se prueban con un doble escrito a mano
    // `SharedPreferences`, que es una interfaz: unas 60 líneas y **ningún** comportamiento de Android,
    // así que no hace falta Robolectric para esto.

    /** El doble se autocomprueba antes de que le fiemos una lectura. */
    @Test
    fun `el doble de preferencias se autocomprueba`() {
        val prefs = PrefsFalso()
        prefs.texto("clave", "valor")
        assertEquals("valor", prefs.getString("clave", "otro"))
        assertEquals("putString", prefs.porMetodo["clave"])
    }

    /**
     * Con las preferencias vacías salen los catorce defectos y el tema es el **dinámico**, que es lo
     * que una instalación nueva muestra de fábrica. Se compara contra la lista y no contra catorce
     * `assertEquals` sueltos: si mañana se añade un ajuste a la copia, esta prueba avisa sola.
     */
    @Test
    fun `sin preferencias guardadas salen los catorce por defecto y el tema dinamico`() {
        assertEquals(DEFAULT_SETTINGS, readBackupSettings(PrefsFalso()))
    }

    /**
     * El caso de quien lleva años usando la app: tiene el interruptor antiguo **apagado** y ningún
     * tema guardado, así que está viendo el morado. Su copia tiene que decir `default`, y no «sin
     * tema»: si no, al restaurar saldría el dinámico y esta spec no le habría servido de nada a la
     * gente que más lo nota.
     */
    @Test
    fun `con el interruptor antiguo apagado y sin tema guardado el tema copiado es el default`() {
        val prefs = PrefsFalso()
        prefs.bandera(AppConstants.DYNAMIC_COLORS_KEY, false)
        assertEquals(AppThemes.default.id, readBackupSettings(prefs)[AppConstants.THEME_ID_KEY])
    }

    /** Y con un tema guardado, ese gana: el interruptor antiguo ya no pincha. */
    @Test
    fun `un tema guardado manda sobre el interruptor antiguo`() {
        val prefs = PrefsFalso()
        prefs.bandera(AppConstants.DYNAMIC_COLORS_KEY, false)
        prefs.texto(AppConstants.THEME_ID_KEY, AppThemes.oneDark.id)
        assertEquals(AppThemes.oneDark.id, readBackupSettings(prefs)[AppConstants.THEME_ID_KEY])
    }

    /**
     * Los catorce leídos con su **tipo**, no convertidos a texto. Se escribe con los mismos tipos que
     * declara la lista y se lee: si algo se convirtiera al pasar, la comparación fallaría, porque
     * `"true"` no es `true`.
     */
    @Test
    fun `los catorce se leen con su tipo y no convertidos a texto`() {
        val prefs = PrefsFalso()
        prefs.escribeTodo(valores)
        assertEquals(valores, readBackupSettings(prefs))
    }

    /**
     * Se escribe con el getter que toca a cada tipo y en **un** `edit()` con **un** `apply()`.
     *
     * El método importa: `putString(LANGUAGE, "es")` es una preferencia distinta de
     * `putBoolean(LANGUAGE, …)`, y escribirla con el equivocado deja el ajuste hecho polvo. El `apply()`
     * único evita el `commit()` por clave, que es lo que pasa si alguien «simplifica» el bucle.
     */
    @Test
    fun `se escribe cada ajuste con su metodo en una sola edicion`() {
        val prefs = PrefsFalso()
        applyRestoredSettings(
            prefs,
            restorableSettings(
                bloque(
                    AppConstants.LANGUAGE_KEY to "es",
                    AppConstants.BIOMETRIC_ENABLED_KEY to true,
                    AppConstants.AUTO_LOCK_TIMEOUT_KEY to 15L,
                    AppConstants.THEME_ID_KEY to AppThemes.oneDark.id
                )
            )
        )
        assertEquals("putString", prefs.porMetodo[AppConstants.LANGUAGE_KEY])
        assertEquals("putBoolean", prefs.porMetodo[AppConstants.BIOMETRIC_ENABLED_KEY])
        assertEquals("putLong", prefs.porMetodo[AppConstants.AUTO_LOCK_TIMEOUT_KEY])
        assertEquals(AppThemes.oneDark.id, prefs.valores[AppConstants.THEME_ID_KEY])
        assertEquals(1, prefs.ediciones)
        assertEquals(1, prefs.aplicados)
    }

    /** Una lista vacía no abre ni una sola edición: no hay nada que escribir. */
    @Test
    fun `una lista vacia no escribe nada`() {
        val prefs = PrefsFalso()
        applyRestoredSettings(prefs, emptyList<BackupSetting>())
        assertEquals(0, prefs.ediciones)
        assertEquals(0, prefs.aplicados)
    }
}

/**
 * Doble de `SharedPreferences` escrito a mano.
 *
 * Se implementa la interfaz entera —no hace falta `Context` ni Robolectric— y además apunta **con qué
 * método** se escribió cada clave, que es lo que permite comprobar que un `FLAG` no acabó escrito como
 * texto. Los tres ayudantes de abajo escriben y aplican, que es como escribe la app.
 */
private class PrefsFalso : SharedPreferences {

    val valores = mutableMapOf<String, Any?>()
    val porMetodo = mutableMapOf<String, String>()
    var ediciones = 0
    var aplicados = 0

    fun texto(clave: String, valor: String) = edit().putString(clave, valor).apply()
    fun bandera(clave: String, valor: Boolean) = edit().putBoolean(clave, valor).apply()
    fun numero(clave: String, valor: Long) = edit().putLong(clave, valor).apply()

    /** Escribe un mapa entero eligiendo el método que declara [BACKUP_SETTINGS]. */
    fun escribeTodo(datos: Map<String, Any>) {
        datos.forEach { (clave, valor) ->
            when (val ajuste = BackupSetting.de(clave, BACKUP_SETTINGS[clave] ?: SettingType.TEXT, valor)) {
                is BackupSetting.Text -> texto(clave, ajuste.value)
                is BackupSetting.Flag -> bandera(clave, ajuste.value)
                is BackupSetting.Number -> numero(clave, ajuste.value)
                null -> Unit
            }
        }
    }

    fun anota(clave: String, metodo: String) {
        porMetodo[clave] = metodo
    }

    override fun getAll(): MutableMap<String, Any?> = valores
    override fun getString(key: String, defValue: String?): String? = valores[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (valores[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String, defValue: Int): Int = (valores[key] as? Number)?.toInt() ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (valores[key] as? Number)?.toLong() ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (valores[key] as? Number)?.toFloat() ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = valores[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = valores.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(this).also { ediciones++ }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private class Editor(private val prefs: PrefsFalso) : SharedPreferences.Editor {

        private fun anota(clave: String, metodo: String) = prefs.also { it.anota(clave, metodo) }

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            anota(key, "putString"); prefs.valores[key] = value; return this
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
            anota(key, "putStringSet"); prefs.valores[key] = values; return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            anota(key, "putInt"); prefs.valores[key] = value; return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            anota(key, "putLong"); prefs.valores[key] = value; return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            anota(key, "putFloat"); prefs.valores[key] = value; return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            anota(key, "putBoolean"); prefs.valores[key] = value; return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            prefs.valores.remove(key); return this
        }

        override fun clear(): SharedPreferences.Editor {
            prefs.valores.clear(); return this
        }

        override fun commit(): Boolean {
            prefs.aplicados++; return true
        }

        override fun apply() {
            prefs.aplicados++
        }
    }
}