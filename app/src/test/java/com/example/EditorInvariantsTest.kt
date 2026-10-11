package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes del editor de notas (spec 014, RF-84 y RF-88).
 *
 * El editor de notas fue un único composable de 3380 líneas dentro de un archivo de 3504. Con eso
 * no se puede razonar: para saber qué depende de qué había que leerlo entero. Esta prueba convierte
 * ese dolor de cabeza en un puñado de números que vigilan solos, de modo que el editor **no vuelva a
 * crecer** sin que alguien lo decida.
 *
 * Son **pruebas puras**: leen el texto del fuente y no necesitan emulador ni entorno de pruebas,
 * que es la única forma de que corran en la plataforma de desarrollo. Miden el **texto**, no el
 * comportamiento: por eso se complementan con un `compileDebugKotlin` en verde, y por eso no
 * sustituyen a probar en dispositivo.
 *
 * **Cada umbral es un indicador, no un objetivo.** Se fija en el valor que el código tiene **hoy** y
 * la spec 014 lo baja. Si uno sube, la prueba se pone roja y hay que decidir: ¿crecer es lo que
 * queríamos, o se nos coló algo?
 *
 * Cada recorrido lleva una **autocomprobación** que verifica que está leyendo el archivo. Sin ella
 * un detector roto devuelve una lista vacía y la prueba pasa **por vacío**, que es como esta prueba
 * se dio por buena en su primer intento: `^` no ancla al principio de cada línea en Kotlin sin
 * `RegexOption.MULTILINE`, y dos de los cinco recorridos no veían nada.
 */
class EditorInvariantsTest {

    // ── Línea base, medida el 2026-10-10 antes de mover nada ────────────────────────────────
    // 3504 líneas · 4 declaraciones de estado que nadie lee · 3 que nunca se asignan
    // · 10 funciones locales de más de 20 líneas · 8 llamadas al guardado completo.
    // Cada tarea de la spec 014 baja su número y esta prueba lo sigue.

    private companion object {
        const val RUTA = "src/main/java/com/example/ui/NoteEditorScreen.kt"

        /**
         * Suelos de los recorridos, muy por debajo de lo que ven hoy (245 declaraciones, 81 estados,
         * 37 funciones). No son un objetivo: están para que un recorrido **roto** se note, porque
         * devuelven una lista vacía y la prueba se pondría verde sin comprobar nada. Cada tarea que
         * saca funciones del composable los baja un poco más, y por eso el suelo es holgado.
         */
        const val MIN_DECLARACIONES = 150
        const val MIN_REMEMBER = 50
        const val MIN_FUNCIONES = 25

        /**
         * RF-88: el archivo no crece. Baja en cada tarea, y a 0 en T8 con el resto del recorte.
         */
        const val MAX_LINEAS = 2342

        /** RF-84: lo que nadie lee. Llega a 0 en T3. */
        const val MAX_ESTADO_MUERTO = 0

        /** RF-84: lo que se declara y nunca se asigna. Llega a 0 en T3. */
        const val MAX_ESTADO_SIN_ASIGNAR = 0

        /**
         * Por `GUIDELINES.md` §4, ninguna función local pasa de 20 líneas. Bajó de 10 a 7.
         *
         * Las que quedan, y por qué se quedan, para que el número no sea un misterio:
         *
         * - `handleBlockCommand` (109): es una **tabla de despacho**, un brazo del `when` por cada
         *   acción del editor. Se lee de un vistazo: `BlockAction.URL_DIALOG` hace lo que dice. Partir
         *   un `when` así en varios ficheros no lo hace más legible, solo más disperso. Lo que sí era
         *   ilegible —un brazo que hacía cuatro cosas— ya se ha sacado.
         * - `handlePickedFile` (40) y `openExternalFile` (31): son lógica de verdad y **quedan
         *   pendientes** de partir en la spec 015, que es quien les puede dar un hogar.
         * - `moveBlockToAnotherNote` (27) y `handleSlashSelect` (25): cierran sobre 17 y 14 variables
         *   de la pantalla. No son separables sin inventar un contenedor de estado, que es justo lo
         *   que hace la 015.
         * - `applyTag` (31) y `applyTagWithVal` (22): bajaron de 50 y 70 extrayendo el esqueleto
         *   común a `applyTagToSelection` y las tres tablas de valor. Lo que queda es la escalera de
         *   salidas tempranas y la llamada, y partirlo más ya sería trocar el código por cumplir la
         *   cuenta.
         */
        const val MAX_FUNCIONES_LARGAS = 7

        /**
         * RF-85: el guardado completo ocurre en un único sitio. Llega a 2 en T3, no a 1: la
         * llamada que mueve un bloque a otra nota guarda **esa** nota, con sus campos, y no es un
         * duplicado del guardado de la nota que se está editando.
         */
        const val MAX_GUARDADOS = 2

        /**
         * RNF-43: el techo por fichero de **lo que se extrajo del composable**, no del monolito —
         * que se mide con [MAX_LINEAS]. Lo fijan los mayores que ya había en el repositorio, para
         * que el editor no sea lo único que se salga del molde.
         */
        const val MAX_LINEAS_FICHERO = 600
    }

    private val fuente: String = File(RUTA).readText()

    private val lineas: List<String> =
        fuente.split("\n").let { if (it.last().isEmpty()) it.dropLast(1) else it }

    /**
     * Las mismas líneas sin cadenas ni comentarios.
     *
     * Hace falta para contar llaves: un `{` dentro de un texto (`"${x}"`) o de un comentario
     * descuadraría el emparejado y daría longitudes de función inventadas.
     */
    private val sinTextos: List<String> = lineas.map { linea ->
        val limpia = StringBuilder()
        var dentroDeCadena = false
        var i = 0
        while (i < linea.length) {
            val c = linea[i]
            when {
                dentroDeCadena && c == '\\' -> i++
                dentroDeCadena && c == '"' -> dentroDeCadena = false
                c == '"' -> dentroDeCadena = true
                c == '/' && i + 1 < linea.length && linea[i + 1] == '/' -> break
                else -> limpia.append(c)
            }
            i++
        }
        limpia.toString()
    }

    // ── RF-88: el tamaño ─────────────────────────────────────────────────────────────────

    @Test
    fun `el editor de notas no supera su presupuesto de lineas`() {
        assertEquals(
            "NoteEditorScreen.kt tiene ${lineas.size} líneas y el presupuesto es $MAX_LINEAS",
            MAX_LINEAS,
            lineas.size
        )
    }

    /**
     * RNF-43: ningún fichero **del editor de notas** supera las 600 líneas.
     *
     * Antes solo lo cumplía `NoteEditorScreen.kt`, que era el único que se miraba. T6 y T7 movieron
     * los diálogos, las hojas y la superficie de IA a ficheros propios, y el requisito habla de
     * todos: sin esta prueba, un cuarto fichero podría crecer hasta lo que quisiera sin que nadie se
     * entere. Es la diferencia entre un presupuesto y una costumbre.
     *
     * **El alcance son los ficheros en que se partió el editor, y `NoteEditorScreen.kt` queda
     * fuera a propósito.** RNF-43 escrita sin más dice «ningún fichero del editor de notas supera
     * las 600 líneas», y eso es imposible mientras el composable siga entero: el propio
     * `NoteEditorScreen.kt` mide 2342. Que baje de 600 exige partir el composable, que es
     * justamente lo que la spec 014 declara **fuera de alcance** (agrupar el estado de las 95
     * declaraciones, que es lo que le daría un hogar a cada mitad). Por eso el monolito se mide con
     * su propio invariante, [MAX_LINEAS], que baja en cada tarea, y las 600 líneas son el techo de
     * **lo que sale de él**: si unextraído vuelve a crecer, se note.
     *
     * Tampoco se cuelan `BlockEditor` (1030), `EditableTableBlock` (1456) ni `FloatingEditorToolbar`
     * (759): superan las 600 desde antes de esta spec y son de otro fichero, con sus defectos
     * pendientes. Meterlos daría un invariante rojo de partida, y uno que nace rojo entrena a
     * ignorar el rojo.
     *
     * Se cuentan los ficheros que existen **de verdad**: un nombre que no esté en disco hace fallar
     * la prueba, para que el inventario no se quede viejo en silencio.
     */
    @Test
    fun `ningun fichero extraido del editor supera las seiscientas lineas`() {
        val declarados = listOf(
            "NoteEditorDialogs", "NoteEditorSheets", "NoteEditorAiSurfaces", "NoteEditorTabsHost"
        )
        val medidos = declarados.map { nombre ->
            val ruta = java.io.File("src/main/java/com/example/ui/$nombre.kt")
            assertTrue("Falta $nombre.kt: el inventario del editor está desactualizado", ruta.exists())
            nombre to ruta.readLines().size
        }
        val grandes = medidos.filter { it.second > MAX_LINEAS_FICHERO }
        assertTrue(
            "Hay ${grandes.size} ficheros del editor por encima de las $MAX_LINEAS_FICHERO líneas: " +
                grandes.joinToString { "${it.first} (${it.second})" },
            grandes.isEmpty()
        )
    }

    // ── RF-84: el estado muerto ───────────────────────────────────────────────────────────

    @Test
    fun `el editor de notas no declara estado que nadie lee`() {
        val declarados = declaracionesLocales()
        assertTrue(
            "El recorrido solo ve ${declarados.size} declaraciones locales; el regex se ha roto",
            declarados.size >= MIN_DECLARACIONES
        )
        val muertos = declarados.filter { (_, nombre) ->
            Regex("\\b" + Regex.escape(nombre) + "\\b").findAll(fuente).count() == 1
        }
        val detalle = muertos.joinToString { "${it.second}@${it.first}" }
        assertTrue(
            "Hay ${muertos.size} declaraciones que nadie lee (el presupuesto es " +
                "$MAX_ESTADO_MUERTO): $detalle",
            muertos.size <= MAX_ESTADO_MUERTO
        )
    }

    @Test
    fun `el editor de notas no declara estado que nunca se asigna`() {
        val declarados = rememberLocales()
        assertTrue(
            "El recorrido solo ve ${declarados.size} estados; el regex se ha roto",
            declarados.size >= MIN_REMEMBER
        )
        val jamasAsignados = declarados.filter { (_, nombre) ->
            Regex("\\b" + Regex.escape(nombre) + "\\s*(?:=[^=]|\\+=|-=|\\+\\+|--)")
                .findAll(fuente).count() == 0
        }
        assertTrue(
            "Hay ${jamasAsignados.size} estados que se declaran y nunca se asignan (el presupuesto " +
                "es $MAX_ESTADO_SIN_ASIGNAR): ${jamasAsignados.map { it.second }}",
            jamasAsignados.size <= MAX_ESTADO_SIN_ASIGNAR
        )
    }

    /**
     * `var`/`val` declarados dentro del composable, con su número de línea.
     *
     * Se miran también los declarados a ocho espacios: viven dentro de un `LaunchedEffect` o de
     * un bloque `if`, siguen siendo estado de la pantalla y ahí es donde se esconde alguno, como
     * el `insertEnd` que se calcula y no lo lee nadie.
     */
    private fun declaracionesLocales(): List<Pair<Int, String>> =
        Regex("^ {4}(?: {4})?(?:var|val) ([A-Za-z_]\\w*)", RegexOption.MULTILINE)
            .findAll(fuente)
            .map { it.range.first + 1 to it.groupValues[1] }
            .toList()

    /**
     * `var … by remember`, que es como se declara el estado de la pantalla.
     *
     * De estos sale el caso que el conteo de apariciones **no** ve: un estado cuyo nombre aparece
     * otra vez, pero solo en la condición que lo consulta y nunca en una asignación. Leer una
     * variable que siempre vale lo mismo que se le dio al declararla es ruido con forma de guarda.
     */
    private fun rememberLocales(): List<Pair<Int, String>> =
        Regex("^ {4}(?: {4})?var ([A-Za-z_]\\w*) by remember", RegexOption.MULTILINE)
            .findAll(fuente)
            .map { it.range.first + 1 to it.groupValues[1] }
            .toList()

    // ── GUIDELINES.md §4: funciones de ≤ 20 líneas ───────────────────────────────────────

    @Test
    fun `ninguna funcion local del editor pasa de veinte lineas`() {
        val todas = funcionesLocales()
        assertTrue(
            "El recorrido solo ve ${todas.size} funciones locales; el regex se ha roto",
            todas.size >= MIN_FUNCIONES
        )
        val largas = todas.filter { it.lineas > 20 }.sortedByDescending { it.lineas }
        assertTrue(
            "Hay ${largas.size} funciones locales de más de 20 líneas (el presupuesto es " +
                "$MAX_FUNCIONES_LARGAS): ${largas.joinToString { "${it.nombre}@${it.desde}" }}",
            largas.size <= MAX_FUNCIONES_LARGAS
        )
    }

    private data class FuncionLocal(val nombre: String, val desde: Int, val hasta: Int) {
        val lineas: Int get() = hasta - desde + 1
    }

    /**
     * Funciones locales del composable, con su extensión medida por **emparejado de llaves**.
     *
     * No vale medir hasta la siguiente llave de cierre por sangría: el editor tiene un tramo
     * indentado ocho espacios de más, resto de un refactor anterior, y ese método mide `applyTag`
     * como si ocupara 119 líneas cuando ocupa 50.
     *
     * El recorrido va **línea a línea**, no sobre el texto entero: el índice que devuelve el
     * emparejadopor expresión regular es un desplazamiento en caracteres, y usarlo como índice de
     * línea deja el rango vacío y descarta todas las funciones sin avisar.
     */
    private fun funcionesLocales(): List<FuncionLocal> =
        lineas.mapIndexedNotNull { i, linea ->
            val nombre = Regex("^ {4}fun ([A-Za-z_]\\w*)").find(linea)?.groupValues?.get(1)
                ?: return@mapIndexedNotNull null
            var profundidad = 0
            var abierta = false
            for (j in i until sinTextos.size) {
                profundidad += sinTextos[j].count { it == '{' } - sinTextos[j].count { it == '}' }
                if ('{' in sinTextos[j]) abierta = true
                if (abierta && profundidad <= 0) return@mapIndexedNotNull FuncionLocal(nombre, i + 1, j + 1)
            }
            // Cuerpo de una sola expresión, sin llaves: se extiende hasta el final de su bloque.
            null
        }

    // ── RF-85: un único camino de guardado ────────────────────────────────────────────────

    @Test
    fun `el guardado completo ocurre en un solo sitio`() {
        val guardados = Regex("viewModel\\.saveNote\\(").findAll(fuente).count()
        assertEquals(
            "Hay $guardados llamadas al guardado completo y el presupuesto es $MAX_GUARDADOS. " +
                "Cada copia es una forma de que una se quede atrás.",
            MAX_GUARDADOS,
            guardados
        )
    }
}