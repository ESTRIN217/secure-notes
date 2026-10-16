package com.example.ui.markdown

/**
 * Qué texto se entrega al motor de GFM del chat con IA (spec 017, RF-99).
 *
 * Un mensaje puede llegar con dos dialectos mezclados: el **Markdown** que el modelo escribe —que el motor
 * ya entiende— y el **markup heredado** de las notas, que el motor **no** entiende y se vería tal cual:
 * `<color=#FF0000>` en pantalla es peor que no tener color.
 *
 * Aquí no se reescribe el contenido ni se decide cómo se pinta: se **retira el markup que no se sabe
 * pintar** y se **traduce el que sí aporta algo**. Las reglas son de línea, no de documento: no hay un
 * parser de etiquetas anidadas, y a propósito. El analizador de texto enriquecido ya tiene 973 líneas y
 * funciona para las notas; aquí solo hace falta que no se vean etiquetas crudas, y un parser completo sería
 * más correcto y mucho más caro a cambio de distinguirse solo en markup mal formado, que no es el caso de uso.
 *
 * **El fichero no importa nada de composición ni de la plataforma**, y no es purismo: es lo que permite
 * ejercitarlo con JUnit en una plataforma donde la mitad de la suite no corre por falta de runtime nativo de
 * Robolectric. Un invariante lo comprueba.
 */
object ChatMarkdownSource {

    /**
     * Devuelve el texto que se entrega al motor de GFM. Ver [markdownForMessage].
     *
     * Es un `object` porque sus dos funciones son **puras**: no guardan estado y su respuesta depende solo de
     * lo que reciben. Si algún día guardaran algo, dejarían de ser comprobables en aislamiento.
     */


    /**
     * Etiquetas que el motor de GFM **no** sabe pintar. Se retiran enteras y su texto se queda.
     *
     * `<b>`, `<i>`, `<u>`, `<s>`, `<sub>`, `<sup>` y `<code>` **no** están aquí a propósito: son HTML
     * estándar, el motor las pinta con su estilo, y retirarlas sería tirar el formato del usuario.
     *
     * `details` y `summary` **no** están tampoco, y es una omisión consciente: son HTML estándar, así que
     * el motor los pinta como plegables de verdad, y una nota con un bloque plegable pegada en el chat se
     * vería como lo que es. Retirarlos funcionaría igual, pero se perdería algo que hoy ya se ve.
     */
    private val etiquetasQueNoSePintan = listOf(
        "color", "bg", "size", "font", "normal",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "ol", "ul", "li", "hr",
        "img", "video", "audio"
    )

    /**
     * Nombres de las etiquetas retiradas, **ordenados por longitud descendente**.
     *
     * El orden no es cosmetismo: `<h1>` y `<hr>` comparten el prefijo `h`, así que una alternancia que
     * empezara por los cortos recognition `<hr>` como `<h` seguido de `r`, y la etiqueta retirada dejaría una
     * `r` colgando en la línea. Con los largos primero no hay prefijo ambiguo.
     */
    private val nombresOrdenados = etiquetasQueNoSePintan.sortedByDescending { it.length }

    /**
     * Una cercada: tres o más backticks o tildes y, opcionalmente, el **nombre del lenguaje**.
     *
     * El nombre importa y no es un detalle: `` ```kotlin `` es una cercada igual que `` ``` ``, y si el
     * patrón exigiera una línea de solo la marca, el bloque de código de Kotlin —el caso más común de todos—
     * se trataría como texto normal y su contenido **se limpiaría**, que es justo lo que la guarda impide.
     *
     * El nombre no puede llevar espacios: `` ```kotlinly <color=#FF0000>x</color> `` no es una cercada
     * (CommonMark tampoco lo acepta), y por eso la línea se limpia.
     */
    private val regexCercada = Regex("^\\s{0,3}(?:`{3,}|~{3,})[ \\t]*\\S*\\s*$")

    /** `<url=direccion>`, con la dirección hasta el cierre del corchete angular. */
    private val regexUrl = Regex("<url=([^>]*)>")

    /**
     * `<item>`, `<item checked>`, `<item checked="true">` y `<item checked="false">`.
     *
     * El valor se captura **entero**, no como una lista de valores buenos: si el patrón solo admitiera
     * `true`/`x`/`checked`, `<item checked="false">` no casaría con nada y se vería la etiqueta tal cual.
     * Decide el valor después [marcaDeListaDeTarea], que sí sabe qué es verdadero y qué no.
     */
    private val regexItem = Regex(
        """<item(\s+checked(?:\s*=\s*["']?([^"'>]*)["']?)?)?\s*>""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Una etiqueta con nombre y su valor, en las dos formas del markup propio.
     *
     * El valor va detrás de un **`=`** sin espacio (`<color=#FF0000>`, `<size=14>`, `<font=mono>`), y
     * `<item checked="true">` lleva un espacio. Por eso el patrón admite los dos: si solo aceptara el espacio,
     * **ninguna** etiqueta de color ni de tamaño se retiraría, que es la mitad de lo que hay que limpiar.
     */
    private val regexEtiquetaRetirada = Regex("<(${nombresOrdenados.joinToString("|")})(?:\\s*=\\s*[^>]*?|\\s[^>]*?)?/?>")

    /** Una etiqueta de cierre suelta, por si alguna de las anteriores abría y no cerraba. */
    private val regexCierre = Regex("</(${nombresOrdenados.joinToString("|")})\\s*>")

    /**
     * El cierre de `<item>`.
     *
     * Va aparte porque `item` **no** está en [etiquetasQueNoSePintan]: no se retira, se **traduce**, y quien
     * lo tradujo es quien tiene que quitar su cierre. Dejarlo en la lista común significaría borrar también
     * la apertura, que es justo lo que hay que convertir en `- [ ]`.
     */
    private val regexCierreItem = Regex("</item\\s*>", RegexOption.IGNORE_CASE)

    /**
     * Devuelve el texto que se entrega al motor: el Markdown tal cual y el markup heredado adaptado.
     *
     * El recorrido es **por líneas** y no por caracteres porque la única decisión que necesita mirar más
     * allá de una línea es la de la cercada, y esa se decide línea a línea: dentro de un bloque de código un
     * `<color=#FF0000>` es **código**, no markup. El analizador de texto enriquecido copia el interior de una
     * cercada verbatim, así que limpiarlo ahí sería una regresión: hoy ese texto se ve tal cual y también
     * tiene que verse tal cual.
     */
    fun markdownForMessage(raw: String): String {
        if (raw.isEmpty()) return raw
        val lineas = raw.split("\n")
        val salida = ArrayList<String>(lineas.size)
        var dentroDeCercada = false
        for (linea in lineas) {
            if (esCercada(linea)) {
                dentroDeCercada = !dentroDeCercada
                salida.add(linea)
                continue
            }
            salida.add(if (dentroDeCercada) linea else adaptarLinea(linea))
        }
        return salida.joinToString("\n")
    }

    /** Una línea que abre o cierra una cercada de bloque. */
    private fun esCercada(linea: String): Boolean = regexCercada.matches(linea)

    /** Las tres operaciones de una línea: traducir enlace, traducir lista de tarea, retirar el resto. */
    private fun adaptarLinea(linea: String): String {
        val conEnlace = traducirUrl(linea)
        val conLista = traducirItem(conEnlace)
        return retirarEtiquetas(conLista)
    }

    /**
     * `<url=direccion>texto</url>` → `[texto](direccion)`.
     *
     * Retirar la etiqueta en vez de traducirla **perdería el texto**, que es justo lo que la persona está
     * leyendo. El cierre `</url>` solo se busca si hay algo que cerrar: un `<url=…>` sin cerrar **no** es un
     * enlace —no hay texto que hipervincular— y se queda como texto, que es como se ve hoy.
     */
    private fun traducirUrl(linea: String): String {
        val salida = StringBuilder(linea)
        var busqueda = 0
        while (true) {
            val apertura = regexUrl.find(salida, busqueda) ?: break
            val cierre = salida.indexOf("</url>", apertura.range.last + 1)
            if (cierre < 0) {
                // Sin cierre **no** hay texto que hipervincular: se retira la etiqueta y la dirección se
                // queda como texto, que es exactamente como se ve hoy. El índice de búsqueda se vuelve a
                // colocar DESPUÉS de la sustitución porque la cadena se ha acortado: seguir con el índice
                // viejo buscaría más allá del final y lanzaría.
                salida.replace(apertura.range.first, apertura.range.last + 1, "")
                busqueda = apertura.range.first
                continue
            }
            val direccion = apertura.groupValues[1]
            val texto = salida.substring(apertura.range.last + 1, cierre)
            val reemplazo = "[$texto]($direccion)"
            salida.replace(apertura.range.first, cierre + "</url>".length, reemplazo)
            busqueda = apertura.range.first + reemplazo.length
        }
        return salida.toString()
    }

    /**
     * `<item>`, `<item checked="true">` y `<item checked="false">` → `- [ ]`, `- [x]` y `- [ ]`.
     *
     * Sin esto, una nota con lista de tarea pegada en el chat saldría **sin casillas**: se vería peor que
     * hoy. La lista ya está en un dialecto que el motor entiende, así que solo hay que traducir la
     * etiqueta. De paso se quita el cierre `</item>`, que hoy se ve **a la vista** al final de cada línea
     * (`☐ Comprar pan</item>`): es un fallo del analizador actual que esta traducción no arrastra.
     */
    private fun traducirItem(linea: String): String {
        val salida = StringBuilder(linea)
        var busqueda = 0
        while (true) {
            val apertura = regexItem.find(salida, busqueda) ?: break
            val reemplazo = marcaDeListaDeTarea(apertura.groupValues[1], apertura.groupValues[2])
            salida.replace(apertura.range.first, apertura.range.last + 1, reemplazo)
            busqueda = apertura.range.first + reemplazo.length
        }
        return salida.toString().replace(regexCierreItem, "")
    }

    /**
     * El prefijo con el que sale una etiqueta de lista de tarea traducida.
     *
     * **`<item>` a secas es una casilla SIN marcar, no una viñeta**, y se decidió mirando lo que se ve hoy:
     * el analizador actual pinta `☐ Comprar pan` para `<item>` y para `<item checked="false">` por igual.
     * Traducirlo a `- ` le quitaría una casilla a quien hoy la ve, y este cambio no va de eso.
     *
     * `checked` sin `=` significa marcada, y `checked=""` también: el atributo está presente y basta.
     */
    private fun marcaDeListaDeTarea(atributo: String, valor: String): String {
        if (atributo.isBlank()) return "- [ ] "
        val hayValor = valor.isNotBlank()
        return if (!hayValor || esValorVerdadero(valor)) "- [x] " else "- [ ] "
    }

    /** `true`, `x` y `checked` son las tres formas con las que el markup propio marca una casilla. */
    private fun esValorVerdadero(valor: String): Boolean =
        valor.equals("true", ignoreCase = true) ||
            valor.equals("x", ignoreCase = true) ||
            valor.equals("checked", ignoreCase = true)

    /** Retira las etiquetas que el motor no sabe pintar, dejando su texto dentro. */
    private fun retirarEtiquetas(linea: String): String =
        linea.replace(regexEtiquetaRetirada, "").replace(regexCierre, "")
}
