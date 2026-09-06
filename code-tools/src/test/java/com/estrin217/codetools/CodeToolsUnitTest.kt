package com.estrin217.codetools

import com.estrin217.codetools.formatter.CodeFormattingManager
import com.estrin217.codetools.formatter.FormatOptions
import com.estrin217.codetools.formatter.FormattingResult
import com.estrin217.codetools.syntax.SupportedLanguage
import com.estrin217.codetools.syntax.SyntaxHighlighter
import com.estrin217.codetools.syntax.SyntaxThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeToolsUnitTest {

  @Test
  fun testLanguageDetectionFromFileName() {
    assertEquals(SupportedLanguage.KOTLIN, SupportedLanguage.fromFileName("MainActivity.kt"))
    assertEquals(SupportedLanguage.JAVA, SupportedLanguage.fromFileName("App.java"))
    assertEquals(SupportedLanguage.JAVASCRIPT, SupportedLanguage.fromFileName("index.js"))
    assertEquals(SupportedLanguage.TYPESCRIPT, SupportedLanguage.fromFileName("app.tsx"))
    assertEquals(SupportedLanguage.HTML, SupportedLanguage.fromFileName("index.html"))
    assertEquals(SupportedLanguage.CSS, SupportedLanguage.fromFileName("styles.css"))
    assertEquals(SupportedLanguage.XML, SupportedLanguage.fromFileName("AndroidManifest.xml"))
    assertEquals(SupportedLanguage.SQL, SupportedLanguage.fromFileName("schema.sql"))
    assertEquals(SupportedLanguage.C, SupportedLanguage.fromFileName("main.c"))
    assertEquals(SupportedLanguage.CPP, SupportedLanguage.fromFileName("server.cpp"))
    assertEquals(SupportedLanguage.CSHARP, SupportedLanguage.fromFileName("Program.cs"))
    assertEquals(SupportedLanguage.GO, SupportedLanguage.fromFileName("main.go"))
    assertEquals(SupportedLanguage.RUST, SupportedLanguage.fromFileName("lib.rs"))
    assertEquals(SupportedLanguage.SWIFT, SupportedLanguage.fromFileName("AppDelegate.swift"))
    assertEquals(SupportedLanguage.PHP, SupportedLanguage.fromFileName("index.php"))
    assertEquals(SupportedLanguage.RUBY, SupportedLanguage.fromFileName("Gemfile"))
    assertEquals(SupportedLanguage.DART, SupportedLanguage.fromFileName("main.dart"))
    assertEquals(SupportedLanguage.LUA, SupportedLanguage.fromFileName("init.lua"))
    assertEquals(SupportedLanguage.BASH, SupportedLanguage.fromFileName("deploy.sh"))
    assertEquals(SupportedLanguage.JSON, SupportedLanguage.fromFileName("config.json"))
    assertEquals(SupportedLanguage.YAML, SupportedLanguage.fromFileName("docker-compose.yaml"))
    assertEquals(SupportedLanguage.PYTHON, SupportedLanguage.fromFileName("script.py"))
  }

  @Test
  fun testSyntaxHighlighters() {
    val languagesToTest = listOf(
      SupportedLanguage.KOTLIN to "fun main() {\n  val name: String = \"Kotlin\"\n  println(name)\n}",
      SupportedLanguage.JAVA to "public class App {\n  public static void main(String[] args) {\n    System.out.println(42);\n  }\n}",
      SupportedLanguage.JAVASCRIPT to "const express = require('express');\nfunction test() { return true; }",
      SupportedLanguage.TYPESCRIPT to "interface User {\n  id: number;\n  name: string;\n}\nconst u: User = { id: 1, name: 'Alice' };",
      SupportedLanguage.HTML to "<!DOCTYPE html>\n<html>\n<head><title>Test</title></head>\n<body><div class=\"box\">Hello</div></body>\n</html>",
      SupportedLanguage.CSS to ".container {\n  display: flex;\n  margin: 16px;\n  color: #ff0000;\n}",
      SupportedLanguage.XML to "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n  <string name=\"app\">Code</string>\n</resources>",
      SupportedLanguage.SQL to "SELECT id, name, created_at FROM users WHERE status = 'active' ORDER BY id DESC LIMIT 10;",
      SupportedLanguage.C to "#include <stdio.h>\nint main() {\n  printf(\"Hello %d\\n\", 100);\n  return 0;\n}",
      SupportedLanguage.CPP to "#include <iostream>\n#include <vector>\nusing namespace std;\nint main() { cout << \"Hi\" << endl; }",
      SupportedLanguage.CSHARP to "using System;\nclass Program {\n  static void Main() {\n    Console.WriteLine(\"Hello\");\n  }\n}",
      SupportedLanguage.GO to "package main\nimport \"fmt\"\nfunc main() {\n  fmt.Println(\"Go\")\n}",
      SupportedLanguage.RUST to "fn main() {\n  let mut x: i32 = 42;\n  println!(\"Number: {}\", x);\n}",
      SupportedLanguage.SWIFT to "import Foundation\nfunc greet(name: String) -> String {\n  return \"Hello \\(name)\"\n}",
      SupportedLanguage.PHP to "<?php\n\$name = 'PHP';\necho \"Hola \$name\";\n?>",
      SupportedLanguage.RUBY to "def calculate_total(items)\n  items.map { |i| i.price }.sum\nend\nputs calculate_total([])",
      SupportedLanguage.DART to "void main() {\n  final String lang = 'Dart';\n  print('Hello \$lang');\n}",
      SupportedLanguage.LUA to "local function add(a, b)\n  return a + b\nend\nprint(add(10, 20))"
    )

    for ((lang, code) in languagesToTest) {
      val highlighted = SyntaxHighlighter.highlight(
        code = code,
        language = lang,
        theme = SyntaxThemes.OneDark
      )
      assertNotNull(highlighted)
      assertEquals(code, highlighted.text)
      assertTrue("Language ${lang.displayName} should produce span styles", highlighted.spanStyles.isNotEmpty())
    }
  }

  @Test
  fun testSearchQueryHighlighting() {
    val code = "val serverPort = 8080\nval clientPort = 8081"
    val highlighted = SyntaxHighlighter.highlight(
      code = code,
      language = SupportedLanguage.PLAIN_TEXT,
      theme = SyntaxThemes.OneDark,
      searchQuery = "port"
    )

    assertNotNull(highlighted)
    assertEquals(code, highlighted.text)
    assertTrue(highlighted.spanStyles.size >= 2)
  }

  @Test
  fun testExternalFileLanguageDetectionComplexCases() {
    assertEquals(SupportedLanguage.BASH, SupportedLanguage.fromFileName("entrypoint.sh"))
    assertEquals(SupportedLanguage.BASH, SupportedLanguage.fromFileName("Dockerfile"))
    assertEquals(SupportedLanguage.JSON, SupportedLanguage.fromFileName("package.json"))
    assertEquals(SupportedLanguage.YAML, SupportedLanguage.fromFileName("k8s-deployment.yml"))
    assertEquals(SupportedLanguage.PYTHON, SupportedLanguage.fromFileName("app.py"))
    assertEquals(SupportedLanguage.ENV, SupportedLanguage.fromFileName(".env.local"))
    assertEquals(SupportedLanguage.INI_TOML, SupportedLanguage.fromFileName("supervisord.conf"))
  }

  @Test
  fun testKotlinSyntaxHighlighterFeatures() {
    val kotlinCode = """
      package com.estrin217.codetools.demo
      
      import kotlinx.coroutines.flow.Flow
      
      /**
       * KDoc comment for service
       * @param id identificador
       * @return resultado
       */
      @Composable
      fun procesarDatos(id: Long, nombre: String?): String {
          val texto = "Pana, usuario ${'$'}nombre con id: ${'$'}{id + 1}"
          val activo = true
          val codigo = 0xFF12
          return if (activo && id > 0L) texto ?: "Vacio" else "Inactivo"
      }
    """.trimIndent()

    val highlighted = SyntaxHighlighter.highlight(
      code = kotlinCode,
      language = SupportedLanguage.KOTLIN,
      theme = SyntaxThemes.OneDark
    )

    assertNotNull(highlighted)
    assertEquals(kotlinCode, highlighted.text)
    assertTrue("Kotlin code should have numerous syntax styling spans", highlighted.spanStyles.size >= 10)
  }

  @Test
  fun testKotlinCommentsAndStringsMasking() {
    // Ensuring keywords inside comments or strings are properly masked and not broken
    val code = "// fun inside comment should not be keyword\nval msg = \"val class in string\""
    val highlighted = SyntaxHighlighter.highlight(
      code = code,
      language = SupportedLanguage.KOTLIN,
      theme = SyntaxThemes.OneDark
    )

    assertNotNull(highlighted)
    assertEquals(code, highlighted.text)
    // Verify comment style covers the first line
    val firstLineEnd = code.indexOf('\n')
    val commentSpan = highlighted.spanStyles.firstOrNull { it.start == 0 && it.end == firstLineEnd }
    assertNotNull("First line must be styled as a comment", commentSpan)
    assertEquals(SyntaxThemes.OneDark.comment, commentSpan?.item?.color)
  }

  @Test
  fun testJsonFormattingAndMinification() {
    val rawJson = "{\"nombre\":\"Caracas\",\"activo\":true,\"detalles\":{\"tasa\":42.5,\"items\":[1,2,3]}}"
    val formatResult = CodeFormattingManager.format(rawJson, SupportedLanguage.JSON, FormatOptions(indentSize = 2))
    assertTrue("JSON formatting should succeed", formatResult is FormattingResult.Success)
    val formatted = (formatResult as FormattingResult.Success).formattedCode
    assertTrue("Formatted JSON should contain newlines and indentation", formatted.contains("\n  \"nombre\": \"Caracas\""))

    val minifyResult = CodeFormattingManager.minify(formatted, SupportedLanguage.JSON)
    assertTrue("JSON minification should succeed", minifyResult is FormattingResult.Success)
    val minified = (minifyResult as FormattingResult.Success).formattedCode
    assertTrue("Minified JSON should not contain spaces or newlines between keys", minified.contains("\"nombre\":\"Caracas\""))
  }

  @Test
  fun testJsonSyntaxErrorDiagnostic() {
    val invalidJson = "{\"nombre\": \"Caracas\", inactivo}"
    val result = CodeFormattingManager.format(invalidJson, SupportedLanguage.JSON)
    assertTrue("Invalid JSON should produce error result", result is FormattingResult.Error)
    val errorMsg = (result as FormattingResult.Error).message
    assertTrue("Error message should be in Venezuelan Spanish", errorMsg.contains("pifia") || errorMsg.contains("JSON"))
  }

  @Test
  fun testBraceLanguageFormattingKotlin() {
    val unformattedKotlin = """
    class Servicio{
    fun procesar(a:Int,b:String):Boolean{
    if(a>0){
    return true
    }else{
    return false
    }
    }
    }
    """.trimIndent()

    val result = CodeFormattingManager.format(unformattedKotlin, SupportedLanguage.KOTLIN, FormatOptions(indentSize = 4))
    assertTrue("Kotlin formatting should succeed", result is FormattingResult.Success)
    val formatted = (result as FormattingResult.Success).formattedCode

    assertTrue("Opening braces should have preceding space", formatted.contains("class Servicio {"))
    assertTrue("Control structures should have space", formatted.contains("if (a>0) {"))
    assertTrue("Functions should be indented inside class", formatted.contains("    fun procesar"))
  }

  @Test
  fun testSqlFormatting() {
    val messySql = "select id, name, balance from users where status = 'active' and balance > 100 order by balance desc limit 5;"
    val result = CodeFormattingManager.format(messySql, SupportedLanguage.SQL)
    assertTrue("SQL formatting should succeed", result is FormattingResult.Success)
    val formatted = (result as FormattingResult.Success).formattedCode

    assertTrue("SQL keywords must be capitalized", formatted.contains("SELECT") && formatted.contains("FROM"))
    assertTrue("WHERE and ORDER BY should be placed cleanly", formatted.contains("WHERE") && formatted.contains("ORDER BY"))
  }

  @Test
  fun testXmlHtmlFormatting() {
    val rawXml = "<root><item id=\"1\"><name>Laptop</name></item></root>"
    val result = CodeFormattingManager.format(rawXml, SupportedLanguage.XML, FormatOptions(indentSize = 2))
    assertTrue("XML formatting should succeed", result is FormattingResult.Success)
    val formatted = (result as FormattingResult.Success).formattedCode

    assertTrue("Root tag should be at base indent", formatted.startsWith("<root>"))
    assertTrue("Child tag should be indented", formatted.contains("  <item id=\"1\">"))
    assertTrue("Single-line tag content should be preserved cleanly", formatted.contains("    <name>Laptop</name>"))
  }

  @Test
  fun testCssFormatting() {
    val rawCss = ".card { margin:16px; padding:8px; color:#fff; }"
    val result = CodeFormattingManager.format(rawCss, SupportedLanguage.CSS, FormatOptions(indentSize = 2))
    assertTrue("CSS formatting should succeed", result is FormattingResult.Success)
    val formatted = (result as FormattingResult.Success).formattedCode

    assertTrue("Selector should precede open brace", formatted.contains(".card {"))
    assertTrue("Declarations should be indented", formatted.contains("  margin: 16px;"))
    assertTrue("Closing brace should close rule block", formatted.contains("}"))
  }

  @Test
  fun testPythonFormatting() {
    val messyPython = "def calcular(total,tasa):\n\tresultado = total * tasa\n\treturn resultado\n"
    val result = CodeFormattingManager.format(messyPython, SupportedLanguage.PYTHON, FormatOptions(indentSize = 4))
    assertTrue("Python formatting should succeed", result is FormattingResult.Success)
    val formatted = (result as FormattingResult.Success).formattedCode

    assertTrue("Tabs should be normalized to 4 spaces", formatted.contains("    resultado"))
    assertTrue("Comma spacing should be standardized", formatted.contains("def calcular(total, tasa):"))
  }

  @Test
  fun testKotlinAnnotationHighlightingUniformColor() {
    val kotlinCode = "@Suppress(\"unused\")\n@Composable\nclass Servicio"
    val theme = com.estrin217.codetools.syntax.SyntaxThemes.OneDark
    val highlighted = com.estrin217.codetools.syntax.SyntaxHighlighter.highlight(kotlinCode, SupportedLanguage.KOTLIN, theme)

    // Verify spans
    val spans = highlighted.spanStyles
    val atSpan = spans.find { it.start == 0 && it.end == 9 } // @Suppress
    assertTrue("Full @Suppress annotation must be styled together in one span", atSpan != null)
    assertEquals("Annotation color must match theme.annotation", theme.annotation, atSpan?.item?.color)

    val composableSpan = spans.find { it.start == 20 && it.end == 31 } // @Composable
    assertTrue("Full @Composable annotation must be styled together in one span", composableSpan != null)
    assertEquals("Annotation color must match theme.annotation", theme.annotation, composableSpan?.item?.color)

    // Verify 'Suppress' inside @Suppress is not overwritten as a type
    val suppressOverlapType = spans.find { it.start == 1 && it.item.color == theme.type }
    assertTrue("Suppress within @Suppress should NOT be overwritten with type color", suppressOverlapType == null)
  }
}
