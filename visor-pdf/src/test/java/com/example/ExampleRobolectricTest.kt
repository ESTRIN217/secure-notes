package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Visor PDF", appName)
  }

  @Test
  fun `verify theme resolves Material3 attributes for PdfViewerFragment`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.setTheme(R.style.Theme_MyApplication)
    val typedArray = context.obtainStyledAttributes(
      intArrayOf(
        com.google.android.material.R.attr.colorOnSurface,
        com.google.android.material.R.attr.textAppearanceLabelMedium
      )
    )
    val hasColorOnSurface = typedArray.hasValue(0)
    val hasTextAppearance = typedArray.hasValue(1)
    typedArray.recycle()
    org.junit.Assert.assertTrue(hasColorOnSurface)
    org.junit.Assert.assertTrue(hasTextAppearance)
  }
}
