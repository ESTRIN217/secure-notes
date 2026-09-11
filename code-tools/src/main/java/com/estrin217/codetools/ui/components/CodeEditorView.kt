package com.estrin217.codetools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.codetools.syntax.SupportedLanguage
import com.estrin217.codetools.syntax.SyntaxHighlighter
import com.estrin217.codetools.syntax.SyntaxTheme
import com.estrin217.codetools.syntax.SyntaxVisualTransformation
import com.estrin217.codetools.ui.theme.JetBrainsMono
import com.estrin217.codetools.R

@Composable
fun CodeEditorView(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    theme: SyntaxTheme,
    isEditMode: Boolean,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    fontSizeSp: Float,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val codeText = textFieldValue.text
    val lines = remember(codeText) {
        val split = codeText.split("\n")
        if (split.isEmpty()) listOf("") else split
    }
    val lineCount = lines.size

    // Determine current active line from cursor selection
    val activeLineNumber = remember(codeText, textFieldValue.selection) {
        val cursor = textFieldValue.selection.min.coerceIn(0, codeText.length)
        val textBefore = codeText.substring(0, cursor)
        textBefore.count { it == '\n' } + 1
    }

    val lineHeight = (fontSizeSp * 1.55f).sp
    val codeTextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = fontSizeSp.sp,
        lineHeight = lineHeight,
        color = theme.text
    )

    val visualTransformation = remember(language, theme, searchQuery) {
        SyntaxVisualTransformation(
            language = language,
            theme = theme,
            searchQuery = searchQuery
        )
    }

    val highlightedViewerText = remember(codeText, language, theme, searchQuery, isEditMode) {
        if (!isEditMode) {
            SyntaxHighlighter.highlight(
                code = codeText,
                language = language,
                theme = theme,
                searchQuery = searchQuery
            )
        } else {
            AnnotatedString("")
        }
    }

    // Number of characters in lineCount for gutter width
    val gutterDigits = remember(lineCount) {
        maxOf(lineCount.toString().length, 2)
    }
    val gutterWidth = (gutterDigits * (fontSizeSp * 0.65f) + 24).dp

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .verticalScroll(verticalScrollState)
            .testTag("code_editor_container")
    ) {
        // Line Numbers Gutter
        if (showLineNumbers) {
            val gutterAnnotatedText = remember(lineCount, activeLineNumber, theme) {
                buildAnnotatedString {
                    for (i in 1..lineCount) {
                        val isActive = i == activeLineNumber
                        val lineStr = i.toString().padStart(gutterDigits, ' ')
                        val style = SpanStyle(
                            color = if (isActive) theme.activeLineNumber else theme.lineNumber,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        pushStyle(style)
                        append(lineStr)
                        pop()
                        if (i < lineCount) append("\n")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .background(theme.gutterBackground)
                    .padding(vertical = 12.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = gutterAnnotatedText,
                    style = codeTextStyle.copy(
                        color = theme.lineNumber,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("line_numbers_gutter")
                )
            }

            // Gutter divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(theme.gutterBackground.copy(alpha = 0.9f))
            )
        }

        // Code Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (!wordWrap) Modifier.horizontalScroll(horizontalScrollState)
                    else Modifier.fillMaxWidth()
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            if (isEditMode) {
                // Interactive editable text field
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    textStyle = codeTextStyle,
                    cursorBrush = SolidColor(theme.function),
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor_input"),
                    decorationBox = { innerTextField ->
                        if (codeText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_placeholder),
                                style = codeTextStyle.copy(color = theme.comment)
                            )
                        }
                        innerTextField()
                    }
                )
            } else {
                // Viewer Mode (read-only, high performance, selectable text)
                SelectionContainer {
                    Text(
                        text = highlightedViewerText,
                        style = codeTextStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_viewer_text")
                    )
                }
            }
        }
    }
}
