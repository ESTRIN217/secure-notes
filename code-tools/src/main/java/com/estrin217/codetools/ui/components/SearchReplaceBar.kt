package com.estrin217.codetools.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.codetools.syntax.SyntaxTheme
import com.estrin217.codetools.ui.theme.JetBrainsMono

@Composable
fun SearchReplaceBar(
    isVisible: Boolean,
    theme: SyntaxTheme,
    searchQuery: String,
    replaceQuery: String,
    matchCount: Int,
    currentMatchIndex: Int,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onReplaceSingle: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplaceRow by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = theme.gutterBackground,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search text input
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(theme.background, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = JetBrainsMono, color = theme.text, fontSize = 14.sp),
                            cursorBrush = SolidColor(theme.function),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onNextMatch() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_text_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Buscar en código...",
                                        color = theme.lineNumber,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            val matchText = if (matchCount > 0) {
                                "${currentMatchIndex + 1}/$matchCount"
                            } else {
                                "0/0"
                            }
                            Text(
                                text = matchText,
                                color = if (matchCount > 0) theme.function else theme.variable,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onPrevMatch,
                        enabled = matchCount > 0,
                        modifier = Modifier.size(36.dp).testTag("search_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Coincidencia anterior",
                            tint = if (matchCount > 0) theme.text else theme.lineNumber
                        )
                    }

                    IconButton(
                        onClick = onNextMatch,
                        enabled = matchCount > 0,
                        modifier = Modifier.size(36.dp).testTag("search_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Siguiente coincidencia",
                            tint = if (matchCount > 0) theme.text else theme.lineNumber
                        )
                    }

                    IconButton(
                        onClick = { showReplaceRow = !showReplaceRow },
                        modifier = Modifier.size(36.dp).testTag("toggle_replace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "Alternar Reemplazar",
                            tint = if (showReplaceRow) theme.function else theme.lineNumber
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp).testTag("close_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar búsqueda",
                            tint = theme.text
                        )
                    }
                }

                // Replace Row (Expandable)
                AnimatedVisibility(visible = showReplaceRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(theme.background, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = replaceQuery,
                                onValueChange = onReplaceChange,
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = JetBrainsMono, color = theme.text, fontSize = 14.sp),
                                cursorBrush = SolidColor(theme.function),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("replace_text_input"),
                                decorationBox = { innerTextField ->
                                    if (replaceQuery.isEmpty()) {
                                        Text(
                                            text = "Reemplazar con...",
                                            color = theme.lineNumber,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = onReplaceSingle,
                            enabled = matchCount > 0,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.function),
                            modifier = Modifier.height(36.dp).testTag("replace_single_btn")
                        ) {
                            Text("Reemplazar", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = onReplaceAll,
                            enabled = matchCount > 0,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.keyword),
                            modifier = Modifier.height(36.dp).testTag("replace_all_btn")
                        ) {
                            Text("Todo", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
