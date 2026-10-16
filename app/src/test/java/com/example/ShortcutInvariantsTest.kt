package com.example

import com.example.shortcut.ShortcutDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Invariantes de los atajos del lanzador (spec 021).
 *
 * Son **pruebas puras** que leen ficheros y no necesitan emulador: miden el **texto**, no el comportamiento, así
 * que se complementan con un `compileDebugKotlin` en verde y no sustituyen a probar en dispositivo (RNF-73).
 *
 * Dos reglas del proyecto, aprendidas por la vía mala (ver `MEMORY.md`):
 *
 * 1. **Cada recorrido quita comentarios antes de buscar.** Un invariante que busca `shortcutId` se pondría
 *    verde con el comentario del propio XML que explica el fichero, que es justo lo que no debe vigilar.
 * 2. **Cada recorrido se autocomprueba.** Sin ella, una ruta equivocada hace que `readText` lance y la prueba
 *    se vaya por un motivo que no es el que vigila, y un recorrido vacío pasa **por vacío**.
 *
 * Para el XML y el manifiesto se usa un **parser de verdad**, no expresiones regulares: un `<intent>` sin
 * `android:data` es un dato de estructura, y un `grep` que no lo encuentra no distingue «no está» de «el
 * recorrido no leyó nada».
 */
class ShortcutInvariantsTest {

    private companion object {
        const val APP = "src/main"
        const val RES = "$APP/res"
        const val JAVA = "$APP/java/com/example"

        const val SHORTCUTS = "$RES/xml/shortcuts.xml"
        const val MANIFIESTO = "$APP/AndroidManifest.xml"
        const val MAIN = "$JAVA/MainActivity.kt"
        const val DESTINOS = "$JAVA/ui/ShortcutDestinations.kt"

        /**
         * Los cinco destinos, con la etiqueta corta que cada uno usa y **en el orden del XML** (RF-124,
         * `plan.md` §4).
         *
         * El orden lo fijó quien Decide mirando **su** lanzador: el de un Infinix muestra **tres** atajos, así
         * que los tres primeros son los que importan de verdad y los otros dos quedan al final, alcanzables
         * arrastrándolos al escritorio.
         */
        val ESPERADO = linkedMapOf(
            "ai_chat" to "ai_chat_title",
            "settings" to "nav_settings",
            "new_note" to "btn_new_note",
            "search" to "search_notes",
            "code_editor" to "code_editor"
        )

        val LOCALES = listOf(
            "values", "values-es-rVE", "values-pt-rBR", "values-fr", "values-it",
            "values-en-rGB", "values-es-rES", "values-pt-rPT", "values-b+es+419"
        )

        /** Los ficheros que no pueden importar `android` ni Compose, o no se prueban sin Robolectric (RNF-73). */
        val PUROS = listOf(
            "$JAVA/shortcut/ShortcutDestination.kt",
            "$JAVA/shortcut/ShortcutPending.kt"
        )
    }

    /** Nodos `<shortcut>` del XML, ya sin comentarios: `getElementsByTagName` no ve los nodos de comentario. */
    private fun atajos(): List<Element> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(SHORTCUTS))
        val lista = doc.getElementsByTagName("shortcut")
        return (0 until lista.length).map { lista.item(it) as Element }
    }

    private fun shortcut(id: String): Element = atajos().firstOrNull { it.getAttribute("android:shortcutId") == id }
        ?: throw AssertionError("no hay ningun atajo con shortcutId=$id en $SHORTCUTS")

    /** El `<intent>` de un atajo concreto: hace falta para atar su acción a su destino. */
    private fun intentDe(id: String): Element {
        val hijos = shortcut(id).getElementsByTagName("intent")
        assertEquals("el atajo $id deberia tener exactamente un intent", 1, hijos.length)
        return hijos.item(0) as Element
    }

    private fun intentions(): List<Element> =
        atajos().flatMap { atajo ->
            val hijos = atajo.getElementsByTagName("intent")
            (0 until hijos.length).map { hijos.item(it) as Element }
        }

    /**
     * Quita los comentarios de bloque y de línea antes de buscar (regla 1 de la cabecera).
     *
     * El nombre de los comentarios se dice con palabras y **no** escribiéndolos: la secuencia de cierre de un
     * comentario cierra también este KDoc, y lo que viene detrás se lee como código. Es el mismo tipo de
     * trampa que el apóstrofo crudo en `strings.xml`: el fichero que documenta la trampa se rompe con ella.
     */
    private fun sinComentarios(texto: String): String =
        texto.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//.*"), "")

    /**
     * Lo mismo para XML, y hace falta de verdad: los `sc_*` y el propio `shortcuts.xml` explican en un
     * comentario **por qué** no llevan `android:tint` ni atributo `monochrome`, así que buscar el texto
     * entero se pondría rojo con el comentario que dice justo lo contrario.
     */
    private fun sinComentariosXml(texto: String): String =
        texto.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    private fun leer(nombre: String): String {
        val texto = File(nombre).readText()
        // Autocomprobación (regla 2 de la cabecera): si el recorrido leyó el fichero entero.
        assertTrue("el recorrido de $nombre solo leio ${texto.length} caracteres", texto.length > 200)
        return texto
    }

    // ------------------------------------------------------------------ RF-124: los cinco atajos

    @Test
    fun `hay exactamente cinco atajos y en el orden previsto`() {
        val ids = atajos().map { it.getAttribute("android:shortcutId") }

        assertEquals(5, ids.size)
        // El orden no es decorativo: el lanzador de quien Decide muestra **tres**, así que el orden pone
        // delante los tres que usa de verdad (caso limite 10).
        assertEquals(ESPERADO.keys.toList(), ids)
    }

    @Test
    fun `los shortcutId son literales y estan activados`() {
        atajos().forEach { atajo ->
            val id = atajo.getAttribute("android:shortcutId")
            assertTrue("el shortcutId debe ser un literal, no un @string: $id", !id.startsWith("@"))
            assertEquals("true", atajo.getAttribute("android:enabled"))
        }
    }

    /**
     * El punto del que sale el atajo muerto: una clave en el XML que el parser no acepta no rompe nada, se
     * limita a ignorarla (RF-125). Se compara contra el **enum de verdad**, no contra su texto.
     *
     * Como **conjunto ordenado por clave**, y no en el orden en que salen: el orden del XML es una decisión de
     * producto —qué tres ve el lanzador— y el del enum es el orden lógico. Meter las dos cosas en una sola
     * comparación hace que el orden del XML tenga que ser el del enum, y hoy mienten el uno al otro:
     * cada cosa se comprueba en su invariante.
     */
    @Test
    fun `las claves del XML son exactamente las que acepta el parser`() {
        val delXml = atajos().map { it.getAttribute("android:shortcutId") }

        assertEquals(
            ShortcutDestination.entries.map { it.clave }.sorted(),
            delXml.sorted()
        )
    }

    @Test
    fun `cada atajo tiene icono propio y cada icono existe y no lleva tinte`() {
        atajos().forEach { atajo ->
            val icono = atajo.getAttribute("android:icon")
            assertTrue("falta el icono en ${atajo.getAttribute("android:shortcutId")}", icono.startsWith("@drawable/"))

            // `removePrefix` se come tambien el directorio, asi que hay que volver a ponerlo: sin esto el
            // invariante buscaba `src/main/res/sc_new_note.xml` y fallaba por una ruta, no por el icono.
            val nombre = File("$RES/drawable", icono.removePrefix("@drawable/") + ".xml")
            assertTrue("no existe el icono $nombre", nombre.exists())
            // La documentacion dice que un icono de atajo "can't include tints": el lanzador lo tiñe.
            assertFalse(
                "$nombre lleva android:tint",
                sinComentariosXml(leer(nombre.path)).contains("android:tint")
            )
        }
    }

    /**
     * `shortcuts.xml` **no tiene** atributo `monochrome`. Escribirlo daria un error de AAPT despues de que
     * Kotlin compile limpio, asi que se vigila aqui (T3).
     */
    @Test
    fun `el XML de atajos no usa un atributo monochrome que no existe`() {
        assertFalse(sinComentariosXml(leer(SHORTCUTS)).contains("monochrome"))
    }

    // ------------------------------------------------------------------ RF-124, RNF-72: las etiquetas

    @Test
    fun `cada atajo usa la etiqueta corta que le toca`() {
        ESPERADO.forEach { (id, clave) ->
            val atajo = shortcut(id)
            assertEquals("@string/$clave", atajo.getAttribute("android:shortcutShortLabel"))
            assertEquals("@string/$clave", atajo.getAttribute("android:shortcutLongLabel"))
        }
    }

    /**
     * Las cinco etiquetas tienen que existir en los **nueve** locales. Una que falta no rompe la
     * compilacion: sale en ingles y ya (el gotcha de `AGENTS.md`).
     */
    @Test
    fun `las cinco etiquetas estan en los nueve locales`() {
        ESPERADO.values.forEach { clave ->
            LOCALES.forEach { locale ->
                val fichero = File(RES, "$locale/strings.xml")
                assertTrue("falta el locale $locale", fichero.exists())
                assertTrue(
                    "falta la clave $clave en $locale",
                    fichero.readText().contains("name=\"$clave\"")
                )
            }
        }
    }

    // ------------------------------------------------------------------ RF-130: el intent

    @Test
    fun `los cinco intents apuntan a MainActivity con accion`() {
        val intents = intentions()

        assertEquals(5, intents.size)
        intents.forEach { intent ->
            assertTrue("falta la accion", intent.getAttribute("android:action").isNotBlank())
            assertEquals("com.example.MainActivity", intent.getAttribute("android:targetClass"))
            assertEquals("com.estrin217.securenotes", intent.getAttribute("android:targetPackage"))
        }
    }

    /**
     * **El invariante que faltaba y por el que los cinco atajos no hacían nada.**
     *
     * Cada `<intent>` tiene que llevar la acción del destino que su `shortcutId` promete. Sin esto, un XML con
     * `<intent>` sin destino compila, pasa AAPT —que trata `res/xml` como opaco— y abre la app sin hacer nada:
     * exactamente lo que pasó en la primera versión, que dio `BUILD SUCCESSFUL` con la función muerta.
     *
     * Cruza el XML contra el **enum de verdad**, así que un destino renombrado aquí se nota al momento.
     */
    @Test
    fun `cada intent lleva la accion del destino que su shortcutId promete`() {
        ShortcutDestination.entries.forEach { destino ->
            val accion = intentDe(destino.clave).getAttribute("android:action")
            assertEquals(
                "el atajo ${destino.clave} no lleva la accion que sabe interpretar la app",
                destino.accion,
                accion
            )
        }
    }

    /**
     * Un `<intent>` con `android:data` o `android:mimeType` se lo llevarian los filtros `VIEW` que la app ya
     * tiene para abrir archivos, y el atajo abriria el importador (RF-130).
     */
    @Test
    fun `ningun intent lleva data ni mimeType`() {
        intentions().forEach { intent ->
            assertFalse("el intent lleva android:data", intent.hasAttribute("android:data"))
            assertFalse("el intent lleva android:mimeType", intent.hasAttribute("android:mimeType"))
        }
    }

    // ------------------------------------------------------------------ RF-124: el manifiesto

    /**
     * El `meta-data` tiene que estar en una Activity **MAIN/LAUNCHER**: es lo unico que acepta la
     * plataforma, y por eso el atajo del editor de codigo no puede apuntar a su propia Activity.
     */
    @Test
    fun `el manifiesto declara los atajos en la Activity principal`() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(MANIFIESTO))
        val actividades = doc.getElementsByTagName("activity")

        val candidatas = (0 until actividades.length)
            .map { actividades.item(it) as Element }
            .filter { activity ->
                val filtros = activity.getElementsByTagName("intent-filter")
                (0 until filtros.length).map { filtros.item(it) as Element }.any { filtro ->
                    // El atributo, **no** `textContent`: un `<action>` no tiene texto, su valor esta en
                    // `android:name`, y `getTextContent()` devuelve "" sin fallar. Ese "" hacia que ningun
                    // filtro pareciera el de MAIN/LAUNCHER y el invariante saliera 0 sin dar la cara.
                    val acciones = filtro.getElementsByTagName("action")
                    val categorias = filtro.getElementsByTagName("category")
                    (0 until acciones.length).any {
                        (acciones.item(it) as Element).getAttribute("android:name") == "android.intent.action.MAIN"
                    } && (0 until categorias.length).any {
                        (categorias.item(it) as Element).getAttribute("android:name") == "android.intent.category.LAUNCHER"
                    }
                }
            }

        assertEquals("debe haber una sola Activity MAIN/LAUNCHER", 1, candidatas.size)
        val principal = candidatas.single()
        val metas = principal.getElementsByTagName("meta-data")
        val atajos = (0 until metas.length)
            .map { metas.item(it) as Element }
            .filter { it.getAttribute("android:name") == "android.app.shortcuts" }

        assertEquals("falta el meta-data android.app.shortcuts en MainActivity", 1, atajos.size)
        assertEquals("@xml/shortcuts", atajos.single().getAttribute("android:resource"))
    }

    // ------------------------------------------------------------------ RNF-73: los ficheros puros

    @Test
    fun `los ficheros de destino y pendiente son puros`() {
        PUROS.forEach { nombre ->
            val codigo = sinComentarios(leer(nombre))
            assertFalse("$nombre importa android", Regex("""^import android\.""", RegexOption.MULTILINE).containsMatchIn(codigo))
            assertFalse("$nombre importa compose", codigo.contains("androidx.compose"))
        }
    }

    // ------------------------------------------------------------------ RF-127, RF-128: el cableado

    /**
     * **Una sola función para las dos entradas**: el arranque en frío y el `onNewIntent` tienen que pasar por
     * el mismo camino, o el candado se queda en uno de los dos.
     */
    @Test
    fun `los dos caminos de entrada llaman a la misma funcion`() {
        val codigo = sinComentarios(leer(MAIN))

        assertTrue("el arranque en frio debe pasar por resolverAtajoEnArranque", codigo.contains("resolverAtajoEnArranque(activity?.intent, isUnlocked)"))
        assertTrue("el camino caliente debe pasar por resolverAtajo", codigo.contains("resolverAtajo(intent, isUnlocked)"))
        // El caliente **no** puede llevar la marca de «ya cumplido»: pulsar dos veces el mismo atajo tiene que
        // contar, y si compartieran marca la segunda pulsacion no haria nada.
        assertTrue("el caliente tiene que llamar a resolverAtajo y no a resolverAtajoEnArranque",
            codigo.contains("resolverAtajo(intent, isUnlocked)"))
    }

    /**
     * La acción del `Intent` sobrevive a un giro, así que el arranque en frío necesita **marcar** el atajo que ya
     * cumplió. Sin la marca, `LaunchedEffect(Unit)` lo volvería a hacer en cada rotación y el chat de la IA
     * dejaría una conversación nueva huérfana en cada giro.
     */
    @Test
    fun `el arranque en frio marca el atajo cumplido y la marca sobrevive al giro`() {
        val codigo = sinComentarios(leer(MAIN))

        assertTrue("falta la marca del atajo cumplido", codigo.contains("rememberSaveable { mutableStateOf<String?>(null) }"))
        assertTrue("el arranque en frio no compara contra la marca", codigo.contains("accion == atajoCumplido"))
        assertTrue("el arranque en frio no escribe la marca", codigo.contains("atajoCumplido = accion"))
    }

    /** El volcado tiene que estar en el efecto que **reacciona** a `isUnlocked`, no en uno que corre una vez. */
    @Test
    fun `el volcado vive en el efecto que observa isUnlocked`() {
        val codigo = sinComentarios(leer(MAIN))
        // `substringAfterLast` y no `substringAfter`: hay **dos** efectos con esa clave —el del actualizador y
        // el del traspaso— y con `substringAfter` este invariante miraría el primero y sería rojo sin motivo.
        val efecto = codigo.substringAfterLast("LaunchedEffect(isUnlocked)")

        assertTrue("el volcado no esta en el LaunchedEffect(isUnlocked)", efecto.contains("atajoPendiente.drena()"))
    }

    /** El volcado **no** puede estar en un `if (isUnlocked)` ajeno: si el candado esta puesto, no hay nada que hacer. */
    @Test
    fun `el volcado cumple el destino con el mismo helper`() {
        val codigo = sinComentarios(leer(DESTINOS))
        ShortcutDestination.entries.forEach { destino ->
            assertTrue(
                "el destino ${destino.clave} no tiene brazo en ejecutarDestino",
                codigo.contains("ShortcutDestination.${destino.name} ->")
            )
        }
    }

    /** El chat se crea **antes** de navegar: al revés, la pantalla abriria sin conversacion (RF-126). */
    @Test
    fun `el atajo de la IA crea la sesion antes de navegar`() {
        val codigo = sinComentarios(leer(DESTINOS))
        val bloque = codigo.substringAfter("private fun abrirChatNuevo")

        assertTrue(bloque.indexOf("createSession") < bloque.indexOf("onNavigateTo"))
        assertTrue(bloque.indexOf("createSession") > 0)
    }

    // ------------------------------------------------------------------ RNF-74: nada de datos

    /**
     * Resolver un atajo no lee ni descifra notas (RNF-74). Se mide **el camino de la decision**, que es el
     * unico que podria hacerlo sin querer: ni los DAO ni el cifrado, en ningun brazo.
     */
    @Test
    fun `el camino de la decision no toca la base de datos ni el cifrado`() {
        listOf(MAIN, DESTINOS, *PUROS.toTypedArray()).forEach { nombre ->
            val codigo = sinComentarios(leer(nombre))
            // Solo se veta en la resolucion: `createSession` escribe, pero es el camino del boton de la lista
            // reutilizado (RF-126), no algo que el atajo anada.
            assertFalse("$nombre llama a CipherService", codigo.contains("CipherService"))
            assertFalse("$nombre llama a un DAO", Regex("""\b\w+Dao\.""").containsMatchIn(codigo))
        }
    }
}