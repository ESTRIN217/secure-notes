package com.example.ui.viewmodel

import com.example.data.updates.GithubAsset
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdaterViewModelTest {

    @Test
    fun newerVersionIsPositive() {
        assertTrue(UpdaterViewModel.compareVersionsInternal("3.1", "3.0") > 0)
        assertTrue(UpdaterViewModel.compareVersionsInternal("2.0", "1.9") > 0)
        assertTrue(UpdaterViewModel.compareVersionsInternal("10.0", "2.0") > 0)
    }

    @Test
    fun olderVersionIsNegative() {
        assertTrue(UpdaterViewModel.compareVersionsInternal("3.0", "3.1") < 0)
        assertTrue(UpdaterViewModel.compareVersionsInternal("1.0", "1.0.1") < 0)
    }

    @Test
    fun equalVersionsAreZero() {
        assertEquals(0, UpdaterViewModel.compareVersionsInternal("3.0", "3.0"))
        assertEquals(0, UpdaterViewModel.compareVersionsInternal("1.2.3", "1.2.3"))
    }

    @Test
    fun debugBetaSuffixIsIgnoredForComparison() {
        assertTrue(UpdaterViewModel.compareVersionsInternal("3.0", "3.0-Beta.29082026") == 0)
        assertTrue(UpdaterViewModel.compareVersionsInternal("3.1", "3.0-Beta.29082026") > 0)
    }

    @Test
    fun prereleaseAndBuildMetadataIgnored() {
        assertEquals(0, UpdaterViewModel.compareVersionsInternal("1.0-rc1", "1.0"))
        assertEquals(0, UpdaterViewModel.compareVersionsInternal("1.0+build5", "1.0"))
    }

    @Test
    fun cleanVersionStripsSuffix() {
        assertEquals(listOf("3", "0"), UpdaterViewModel.cleanVersion("3.0"))
        assertEquals(listOf("3", "0"), UpdaterViewModel.cleanVersion("3.0-Beta.29082026"))
        assertEquals(listOf("3", "0", "1"), UpdaterViewModel.cleanVersion("3.0.1+build7"))
    }

    @Test
    fun parseApkUrlFindsApkAsset() {
        val asset = JSONObject()
            .put("name", "secure-notes.apk")
            .put("browser_download_url", "https://github.com/ESTRIN217/secure-notes/releases/download/v3.0/secure-notes.apk")
        val assets = JSONArray().put(asset)
        val json = JSONObject().put("assets", assets)
        assertEquals(
            "https://github.com/ESTRIN217/secure-notes/releases/download/v3.0/secure-notes.apk",
            UpdaterViewModel.parseApkUrlInternal(json)
        )
    }

    @Test
    fun parseApkUrlIgnoresNonApkAssetsAndSkipsOthers() {
        val aab = JSONObject()
            .put("name", "app.aab")
            .put("browser_download_url", "https://example.com/app.aab")
        val apk = JSONObject()
            .put("name", "secure-notes.apk")
            .put("browser_download_url", "https://example.com/secure-notes.apk")
        val assets = JSONArray().put(aab).put(apk)
        val json = JSONObject().put("assets", assets)
        assertEquals("https://example.com/secure-notes.apk", UpdaterViewModel.parseApkUrlInternal(json))
    }

    @Test
    fun parseApkUrlReturnsNullWhenNoApk() {
        val asset = JSONObject()
            .put("name", "app.aab")
            .put("browser_download_url", "https://example.com/app.aab")
        val json = JSONObject().put("assets", JSONArray().put(asset))
        assertNull(UpdaterViewModel.parseApkUrlInternal(json))
        assertNull(UpdaterViewModel.parseApkUrlInternal(JSONObject()))
    }

    @Test
    fun shouldCheckRespectsInterval() {
        val interval = UpdaterViewModel.UPDATE_CHECK_INTERVAL_MS
        val now = 1_000_000_000_000L
        assertTrue(UpdaterViewModel.shouldCheck(0L, now, interval))
        assertTrue(UpdaterViewModel.shouldCheck(now - interval - 1000, now, interval))
        assertFalse(UpdaterViewModel.shouldCheck(now, now, interval))
        assertFalse(UpdaterViewModel.shouldCheck(now - 1000, now, interval))
    }

    @Test
    fun extractSha256FindsHexToken() {
        val expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        assertEquals(expected, UpdaterViewModel.extractSha256("SHA256: $expected  secure-notes.apk"))
        assertEquals(expected, UpdaterViewModel.extractSha256("$expected  secure-notes.apk"))
        assertEquals(expected, UpdaterViewModel.extractSha256(expected.uppercase()))
        assertNull(UpdaterViewModel.extractSha256("no checksum here"))
        assertNull(UpdaterViewModel.extractSha256(""))
    }

    @Test
    fun selectApkAssetPrefersAbiThenUniversalThenFirst() {
        val perAbi = listOf(
            GithubAsset("secure-notes-arm64-v8a.apk", "https://example.com/arm64.apk"),
            GithubAsset("secure-notes-armeabi-v7a.apk", "https://example.com/v7a.apk"),
            GithubAsset("secure-notes-x86_64.apk", "https://example.com/x86.apk")
        )
        assertEquals("https://example.com/arm64.apk", UpdaterViewModel.selectApkAsset(perAbi, "arm64-v8a")?.url)
        assertEquals("https://example.com/v7a.apk", UpdaterViewModel.selectApkAsset(perAbi, "armeabi-v7a")?.url)
        assertEquals("https://example.com/arm64.apk", UpdaterViewModel.selectApkAsset(perAbi, null)?.url)

        val universal = listOf(
            GithubAsset("secure-notes-x86_64.apk", "https://example.com/x86.apk"),
            GithubAsset("secure-notes-universal.apk", "https://example.com/universal.apk")
        )
        assertEquals("https://example.com/universal.apk", UpdaterViewModel.selectApkAsset(universal, "arm64-v8a")?.url)

        assertNull(UpdaterViewModel.selectApkAsset(emptyList(), "arm64-v8a"))
        assertNull(UpdaterViewModel.selectApkAsset(listOf(GithubAsset("app.aab", "https://example.com/app.aab")), "arm64-v8a"))
    }

    @Test
    fun parseApkUrlForAbiPicksArm64Asset() {
        val arm64 = JSONObject()
            .put("name", "secure-notes-arm64-v8a.apk")
            .put("browser_download_url", "https://example.com/arm64.apk")
        val v7a = JSONObject()
            .put("name", "secure-notes-armeabi-v7a.apk")
            .put("browser_download_url", "https://example.com/v7a.apk")
        val json = JSONObject().put("assets", JSONArray().put(v7a).put(arm64))
        assertEquals("https://example.com/arm64.apk", UpdaterViewModel.parseApkUrlForAbiInternal(json, "arm64-v8a"))
    }

    @Test
    fun sha256OfComputesKnownHash() {
        val file = File.createTempFile("updater", ".tmp")
        try {
            file.writeText("hello")
            assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                UpdaterViewModel.sha256Of(file)
            )
            file.writeText("hello!")
            assertEquals(
                "ce06092fb948d9ffac7d1a376e404b26b7575bcc11ee05a4615fef4fec3a308b",
                UpdaterViewModel.sha256Of(file)
            )
        } finally {
            file.delete()
        }
    }
}
