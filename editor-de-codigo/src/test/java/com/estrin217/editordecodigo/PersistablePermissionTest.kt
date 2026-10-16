package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.utils.PersistablePermission
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué URIs merecen que el editor pida el permiso persistente (spec 024, RF-158).
 *
 * Solo el almacenamiento compartido (`content://`) tiene permiso que persistir; una ruta local
 * (`file://` o absoluta) no lo necesita. La regla vive en una pieza **pura** para poder probarla
 * sin Robolectric (RNF-92).
 */
class PersistablePermissionTest {

    @Test
    fun `el almacenamiento compartido si se persiste`() {
        assertTrue(PersistablePermission.appliesTo("content"))
    }

    @Test
    fun `una ruta local no se persiste`() {
        assertFalse(PersistablePermission.appliesTo("file"))
    }

    @Test
    fun `sin esquema no se persiste`() {
        assertFalse(PersistablePermission.appliesTo(null))
    }

    @Test
    fun `un esquema vacio no se persiste`() {
        assertFalse(PersistablePermission.appliesTo(""))
    }
}
