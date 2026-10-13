package com.example

import com.example.util.GfmMarkup
import com.example.util.ReferenceDefinition
import com.example.util.SetextKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la gramática GFM nueva, sobre las **funciones puras**.
 *
 * JUnit puro a propósito (RNF-45): sin `RobolectricTestRunner`, porque en esta plataforma Robolectric no
 * tiene runtime nativo para `linux/aarch64` y sus pruebas no se ejecutan. Aquí no hace falta pantalla: cada
 * función recibe cadenas y devuelve una respuesta.
 */
class GfmMarkupTest {

    // ---------------------------------------------------------------------------------------------------
    // Definiciones de referencia (RF-91)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun definicionSimpleSeReconoce() {
        val defs = GfmMarkup.collectReferenceDefinitions("[r]: https://ejemplo.com")
        assertEquals("https://ejemplo.com", defs.urlFor("r"))
    }

    @Test
    fun definicionRepetidaGanaLaPrimera() {
        val defs = GfmMarkup.collectReferenceDefinitions("[r]: primera\n[r]: segunda")
        assertEquals("primera", defs.urlFor("r"))
    }

    @Test
    fun definicionEntreAngulosSeTomaEntera() {
        val defs = GfmMarkup.collectReferenceDefinitions("[r]: <https://ejemplo.com/a b>")
        assertEquals("<https://ejemplo.com/a b>", defs.urlFor("r"))
    }

    @Test
    fun etiquetaSeNormalizaAMinusculas() {
        val defs = GfmMarkup.collectReferenceDefinitions("[Ref]: https://ejemplo.com")
        assertEquals("https://ejemplo.com", defs.urlFor("ref"))
        assertEquals("https://ejemplo.com", defs.urlFor("REF"))
    }

    @Test
    fun espaciosInternosDeLaEtiquetaSeColapsan() {
        val defs = GfmMarkup.collectReferenceDefinitions("[mi   ref]: https://ejemplo.com")
        assertEquals("https://ejemplo.com", defs.urlFor("mi ref"))
    }

    @Test
    fun textoSinCorcheteNoRecorreElDocumento() {
        val defs = GfmMarkup.collectReferenceDefinitions("una nota normal sin nada de esto")
        assertTrue(defs.isEmpty)
        assertNull(defs.urlFor("r"))
    }

    @Test
    fun definicionNoSeReconoceEnMedioDeLinea() {
        val defs = GfmMarkup.collectReferenceDefinitions("texto antes [r]: https://ejemplo.com")
        assertNull(defs.urlFor("r"))
    }

    @Test
    fun definicionGuardaSuRangoCompleto() {
        val texto = "antes\n[r]: https://ejemplo.com\ndespués"
        val defs = GfmMarkup.collectReferenceDefinitions(texto)
        val inicio = texto.indexOf('\n') + 1
        val definicion = defs.at(inicio)!!
        assertEquals("[r]: https://ejemplo.com", texto.substring(definicion.start, definicion.end))
    }

    @Test
    fun sinDefinicionNoHayNadaQueEnseñar() {
        val defs = GfmMarkup.collectReferenceDefinitions("sin corchete")
        assertNull(defs.at(0))
        assertNull(defs.at(3))
    }

    @Test
    fun definicionDeLineaSiguienteTambienSeVe() {
        val texto = "[a]: 1\n[b]: 2"
        val defs = GfmMarkup.collectReferenceDefinitions(texto)
        assertEquals("1", defs.urlFor("a"))
        assertEquals("2", defs.urlFor("b"))
        // Cada definición se encuentra **por la posición donde empieza**, que es lo que necesita el bucle.
        // Las posiciones se anclan con `indexOf` y no a ojo: un índice escrito a mano se desplaza en cuanto
        // el texto de ejemplo cambia, y el assert sigue verde mientras mira el sitio equivocado.
        val segundaLinea = texto.indexOf('\n') + 1
        assertEquals("a", defs.at(0)!!.label)
        assertEquals("b", defs.at(segundaLinea)!!.label)
        assertNull(defs.at(3))
    }

    // ---------------------------------------------------------------------------------------------------
    // Setext (RF-90)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun igualesBajoTextoDanPrimerNivel() {
        assertEquals(SetextKind.H1, GfmMarkup.setextUnderlineKind("===", previousLineIsPlainText = true))
    }

    @Test
    fun guionesBajoTextoDanSegundoNivel() {
        assertEquals(SetextKind.H2, GfmMarkup.setextUnderlineKind("---", previousLineIsPlainText = true))
    }

    @Test
    fun unSoloGuionBajoTextoTambienEsSetext() {
        // El mínimo de GFM es un carácter: `Texto\n-` es un encabezado, no una raya.
        assertEquals(SetextKind.H2, GfmMarkup.setextUnderlineKind("-", previousLineIsPlainText = true))
    }

    @Test
    fun subrayadoConEspaciosAlrededorSeReconoce() {
        assertEquals(SetextKind.H1, GfmMarkup.setextUnderlineKind("  ===  ", previousLineIsPlainText = true))
    }

    @Test
    fun sinTextoEncimaNoHayEncabezado() {
        // Raya de separación, no setext: la decisión se toma con la línea de arriba, no con la de abajo.
        assertNull(GfmMarkup.setextUnderlineKind("===", previousLineIsPlainText = false))
        assertNull(GfmMarkup.setextUnderlineKind("---", previousLineIsPlainText = false))
    }

    @Test
    fun lineaMixtaDeIgualesYGuionesNoEsSubrayado() {
        assertNull(GfmMarkup.setextUnderlineKind("=-=", previousLineIsPlainText = true))
    }

    @Test
    fun lineaVaciaNoEsSubrayado() {
        assertNull(GfmMarkup.setextUnderlineKind("", previousLineIsPlainText = true))
        assertNull(GfmMarkup.setextUnderlineKind("   ", previousLineIsPlainText = true))
    }

    @Test
    fun textoEnLaLineaNoEsSubrayado() {
        assertNull(GfmMarkup.setextUnderlineKind("= uno", previousLineIsPlainText = true))
    }

    // ---------------------------------------------------------------------------------------------------
    // Autolink con ángulos (RF-92)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun autolinkHttpSeReconoce() {
        val texto = "<https://ejemplo.com>"
        val enlace = GfmMarkup.angleAutolink(texto, 0)!!
        assertEquals("https://ejemplo.com", enlace.url)
        assertEquals(0, enlace.start)
        assertEquals(texto.length, enlace.end)
    }

    @Test
    fun autolinkConRutaLargaYComillasNoSeCorta() {
        val url = "https://ejemplo.com/a/b?c=d&e=f"
        val enlace = GfmMarkup.angleAutolink("<$url>", 0)!!
        assertEquals(url, enlace.url)
    }

    @Test
    fun autolinkDeCorreoSeReconoce() {
        val enlace = GfmMarkup.angleAutolink("<correo@ejemplo.com>", 0)!!
        assertEquals("correo@ejemplo.com", enlace.url)
    }

    @Test
    fun autolinkMailtoSeReconoce() {
        val enlace = GfmMarkup.angleAutolink("<mailto:correo@ejemplo.com>", 0)!!
        assertEquals("mailto:correo@ejemplo.com", enlace.url)
    }

    @Test
    fun etiquetaHtmlNoEsAutolink() {
        // Lo que no casa vuelve al camino de etiquetas de siempre: es lo que impide que <b> sea un enlace.
        assertNull(GfmMarkup.angleAutolink("<b>negrita</b>", 0))
        assertNull(GfmMarkup.angleAutolink("<color=#FF0000>", 0))
        assertNull(GfmMarkup.angleAutolink("<url=https://x.com>", 0))
    }

    @Test
    fun sinCierreDeAnguloNoHayAutolink() {
        assertNull(GfmMarkup.angleAutolink("<https://ejemplo.com", 0))
    }

    @Test
    fun angleBracketSueltoNoEsAutolink() {
        assertNull(GfmMarkup.angleAutolink("<>", 0))
        assertNull(GfmMarkup.angleAutolink("< https://x.com>", 0))
    }

    @Test
    fun autolinkEnMedioDeTextoSeEncuentra() {
        val url = "https://ejemplo.com"
        val texto = "ver <$url> ahora"
        val inicio = texto.indexOf('<')
        val enlace = GfmMarkup.angleAutolink(texto, inicio)!!
        assertEquals(inicio, enlace.start)
        assertEquals(inicio + url.length + 2, enlace.end)
    }

    @Test
    fun indiceFueraDeRangoNoRompe() {
        assertNull(GfmMarkup.angleAutolink("texto", 99))
        assertNull(GfmMarkup.angleAutolink("", 0))
    }

    // ---------------------------------------------------------------------------------------------------
    // Énfasis intraword (RF-96)
    //
    // La regla es la de `MarkdownInline` (spec 012), y a propósito: el editor de código y el de notas son
    // dos superficies del mismo dialecto y no pueden decidir distinto. Abre si lo de detrás no es espacio y
    // lo de delante no es letra ni dígito; cierra si lo de delante no es espacio.
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun asteriscoPegadoALaLetraNoAbre() {
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("a*b*c", 1, 1))
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("a*b*c", 3, 1))
    }

    @Test
    fun guionBajoDentroDePalabraNoAplica() {
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("ancho_maximo", 5, 1))
    }

    @Test
    fun asteriscoEntreDigitosTampocoAbre() {
        // `2*3*4`: delante hay un dígito, que la regla trata igual que una letra. No es el caso límite 12 de
        // la spec: se corrigió al escribir el código, y aquí queda la regla que de verdad se implementa.
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("2*3*4", 1, 1))
    }

    @Test
    fun asteriscoEntreEspaciosSiAbre() {
        assertTrue(GfmMarkup.emphasisDelimiterAppliesAt("por *esto* pasa", 4, 1))
    }

    @Test
    fun dobleAsteriscoEntreEspaciosSiAbre() {
        assertTrue(GfmMarkup.emphasisDelimiterAppliesAt("por **esto** pasa", 4, 2))
    }

    @Test
    fun delimitadorAlPrincipioSiAbre() {
        assertTrue(GfmMarkup.emphasisDelimiterAppliesAt("*esto*", 0, 1))
    }

    @Test
    fun delimitadorAlFinalNoAbre() {
        // Un `*` al final del documento no puede abrir: detrás no hay nada que enfatizar. Abrir y cerrar son
        // reglas distintas, y es el cierre el que pega a la letra.
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("precio*", 6, 1))
    }

    @Test
    fun asteriscoTrasEspacioNoAbre() {
        // Detrás hay un espacio: sería una apertura que se abre sobre nada.
        assertFalse(GfmMarkup.emphasisDelimiterAppliesAt("por * esto", 4, 1))
    }

    @Test
    fun asteriscoPegadoALaLetraSiCierra() {
        // Cerrar sí puede: `*esto*` cierra pegado a la letra, y es lo que hace que siga siendo cursiva.
        assertTrue(GfmMarkup.emphasisDelimiterClosesAt("*esto*", 5, 1))
    }

    @Test
    fun asteriscoTrasEspacioNoCierra() {
        assertFalse(GfmMarkup.emphasisDelimiterClosesAt("esto * ", 5, 1))
    }

    // ---------------------------------------------------------------------------------------------------
    // Código en línea con tiradas (RF-95)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun tiradaDeDosCierraConDos() {
        // "`` `a` ``": la apertura son los dos primeros y el cierre los dos últimos.
        val texto = "`` `a` ``"
        assertEquals(7, GfmMarkup.closingBacktickRun(texto, openEnd = 2))
    }

    @Test
    fun tiradaImparSinCierreNoEsCodigo() {
        assertNull(GfmMarkup.closingBacktickRun("```a``", openEnd = 3))
    }

    @Test
    fun sinNingunAcentoGraveNoHayCierre() {
        assertNull(GfmMarkup.closingBacktickRun("```texto normal", openEnd = 3))
    }

    @Test
    fun cierreEsElPrimerAcentoGraveQueFormaLaTirada() {
        // "`` a ` b ``": el acento suelto del medio no cierra una tirada de dos; el de dos del final sí.
        val texto = "`` a ` b ``"
        assertEquals(9, GfmMarkup.closingBacktickRun(texto, openEnd = 2))
    }

    @Test
    fun ausenciaDeAcentosGraveDevuelveCierreNulo() {
        assertNull(GfmMarkup.closingBacktickRun("texto", openEnd = 0))
    }

    // ---------------------------------------------------------------------------------------------------
    // Fila separadora de tabla (RF-97)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun separadoraDeDosColumnasSeReconoce() {
        assertTrue(GfmMarkup.isTableDelimiterRow("| --- | --- |"))
    }

    @Test
    fun separadoraConAlineacionesSeReconoce() {
        assertTrue(GfmMarkup.isTableDelimiterRow("| :--- | ---: | :---: |"))
        assertTrue(GfmMarkup.isTableDelimiterRow("|:---|---:|"))
    }

    @Test
    fun separadoraDeUnaSolaColumnaNoEsTabla() {
        // El mínimo de GFM son dos columnas.
        assertFalse(GfmMarkup.isTableDelimiterRow("| --- |"))
    }

    @Test
    fun separadoraSinBarraInicialNoSeReconoce() {
        // La capa de bloques exige `|` inicial; si el analizador lo aceptara, las dos capas decidirían distinto.
        assertFalse(GfmMarkup.isTableDelimiterRow("--- | ---"))
    }

    @Test
    fun separadoraConTextoDentroNoEsSeparadora() {
        assertFalse(GfmMarkup.isTableDelimiterRow("| --- | x |"))
    }

    @Test
    fun filaDeDatosNoEsSeparadora() {
        assertFalse(GfmMarkup.isTableDelimiterRow("| A | B |"))
    }

    @Test
    fun lineaVaciaNoEsSeparadora() {
        assertFalse(GfmMarkup.isTableDelimiterRow(""))
    }

    @Test
    fun tuberiaDeCabeceraSeCuenta() {
        assertEquals(3, GfmMarkup.pipeCount("| A | B |"))
        assertEquals(1, GfmMarkup.pipeCount("A | B"))
    }

    @Test
    fun tuberiaEscapadaNoCuentaComoColumna() {
        // "| a \| b | c |": hay cuatro barras, una escapada, así que tres cuentan como columna delimitadora.
        assertEquals(3, GfmMarkup.pipeCount("| a \\| b | c |"))
    }

    // ---------------------------------------------------------------------------------------------------
    // Cierre de encabezado (RF-93)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun hashesDeCierreConEspacioSeReconocen() {
        assertEquals(2, GfmMarkup.closingHashCount("## Título ##"))
        assertEquals(1, GfmMarkup.closingHashCount("# Título #"))
    }

    @Test
    fun hashesPegadosAlTituloNoCierran() {
        assertEquals(0, GfmMarkup.closingHashCount("## Título#"))
    }

    @Test
    fun tituloSinHashesNoCierra() {
        assertEquals(0, GfmMarkup.closingHashCount("## Título"))
    }

    @Test
    fun lineaConSoloHashesNoCierra() {
        assertEquals(0, GfmMarkup.closingHashCount("###"))
    }

    // ---------------------------------------------------------------------------------------------------
    // Tipos (RF-91)
    // ---------------------------------------------------------------------------------------------------

    @Test
    fun definicionExponeEtiquetaUrlYRango() {
        val texto = "x\n[r]: https://ejemplo.com"
        val inicio = texto.indexOf('\n') + 1
        val definicion: ReferenceDefinition = GfmMarkup.collectReferenceDefinitions(texto).at(inicio)!!
        assertEquals("r", definicion.label)
        assertEquals("https://ejemplo.com", definicion.url)
        assertEquals(inicio, definicion.start)
        assertEquals(texto.length, definicion.end)
    }
}
