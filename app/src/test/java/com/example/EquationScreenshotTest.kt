package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.ReadOnlyTextBlock
import com.example.ui.theme.MyApplicationTheme
import com.example.util.RichTextConverter
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class EquationScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun equation_inline_readonly() {
        val segs = RichTextConverter.markupToSegments(
            "Energía <eq>E=mc^2</eq> y fracción <eq>\\frac{a}{b}</eq> fin."
        )
        composeTestRule.setContent {
            MyApplicationTheme {
                ReadOnlyTextBlock(segments = segs, onActivate = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/equation_inline.png")
    }

    @Test
    fun equation_invalid_fallback() {
        val segs = RichTextConverter.markupToSegments("Rota <eq>\\frac{a</eq> fin.")
        composeTestRule.setContent {
            MyApplicationTheme {
                ReadOnlyTextBlock(segments = segs, onActivate = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/equation_fallback.png")
    }
}
