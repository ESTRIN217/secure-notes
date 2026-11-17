package com.example.ui.chat

import com.example.data.ai.MODEL_CATALOG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 040 (RF-288, RNF-176): guardia del catálogo on-device. `paramsLabel` es obligatoria,
 * no vacía y sin duplicados de id. JVM puro.
 */
class ModelInfoCatalogTest {

    @Test
    fun every_catalog_entry_declares_a_nonBlank_paramsLabel() {
        MODEL_CATALOG.forEach { model ->
            assertTrue("paramsLabel vacío en ${model.id}", model.paramsLabel.isNotBlank())
        }
    }

    @Test
    fun catalog_has_no_duplicate_ids() {
        assertEquals(
            MODEL_CATALOG.size,
            MODEL_CATALOG.map { it.id }.distinct().size
        )
    }
}
