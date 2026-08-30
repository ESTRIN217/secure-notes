package com.example

import com.example.util.CodeLanguageDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeLanguageDetectorTest {

    @Test
    fun `maps common extensions to language codes`() {
        assertEquals("kotlin", CodeLanguageDetector.fromFileName("Main.kt"))
        assertEquals("java", CodeLanguageDetector.fromFileName("App.java"))
        assertEquals("python", CodeLanguageDetector.fromFileName("script.py"))
        assertEquals("javascript", CodeLanguageDetector.fromFileName("app.js"))
        assertEquals("typescript", CodeLanguageDetector.fromFileName("main.ts"))
        assertEquals("html", CodeLanguageDetector.fromFileName("index.html"))
        assertEquals("css", CodeLanguageDetector.fromFileName("style.css"))
        assertEquals("json", CodeLanguageDetector.fromFileName("config.json"))
        assertEquals("xml", CodeLanguageDetector.fromFileName("manifest.xml"))
        assertEquals("sql", CodeLanguageDetector.fromFileName("query.sql"))
        assertEquals("c", CodeLanguageDetector.fromFileName("util.c"))
        assertEquals("cpp", CodeLanguageDetector.fromFileName("main.cpp"))
        assertEquals("csharp", CodeLanguageDetector.fromFileName("Program.cs"))
        assertEquals("go", CodeLanguageDetector.fromFileName("main.go"))
        assertEquals("rust", CodeLanguageDetector.fromFileName("lib.rs"))
        assertEquals("swift", CodeLanguageDetector.fromFileName("View.swift"))
        assertEquals("bash", CodeLanguageDetector.fromFileName("deploy.sh"))
    }

    @Test
    fun `extension matching is case insensitive`() {
        assertEquals("kotlin", CodeLanguageDetector.fromFileName("Main.KT"))
    }

    @Test
    fun `no known extension returns null`() {
        assertNull(CodeLanguageDetector.fromFileName("readme.txt"))
        assertNull(CodeLanguageDetector.fromFileName("notes.md"))
        assertNull(CodeLanguageDetector.fromFileName("archive.zip"))
    }

    @Test
    fun `null or blank names return null`() {
        assertNull(CodeLanguageDetector.fromFileName(null))
        assertNull(CodeLanguageDetector.fromFileName(""))
        assertNull(CodeLanguageDetector.fromFileName("   "))
    }

    @Test
    fun `detect falls back to json for raw objects`() {
        assertEquals("json", CodeLanguageDetector.detect("noext", "{\"a\": 1}"))
        assertEquals("json", CodeLanguageDetector.detect(null, "[1, 2, 3]"))
    }

    @Test
    fun `detect prefers extension over content`() {
        assertEquals("kotlin", CodeLanguageDetector.detect("Main.kt", "{\"not\": \"json\"}"))
    }

    @Test
    fun `detect returns null for plain text`() {
        assertNull(CodeLanguageDetector.detect(null, "just some text"))
    }
}
