package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

data class CodeLanguage(val code: String, val label: String)

object CodeLanguages {
    val all = listOf(
        CodeLanguage("", "Plain text"),
        CodeLanguage("kotlin", "Kotlin"),
        CodeLanguage("java", "Java"),
        CodeLanguage("python", "Python"),
        CodeLanguage("javascript", "JavaScript"),
        CodeLanguage("typescript", "TypeScript"),
        CodeLanguage("html", "HTML"),
        CodeLanguage("css", "CSS"),
        CodeLanguage("json", "JSON"),
        CodeLanguage("xml", "XML"),
        CodeLanguage("sql", "SQL"),
        CodeLanguage("c", "C"),
        CodeLanguage("cpp", "C++"),
        CodeLanguage("csharp", "C#"),
        CodeLanguage("go", "Go"),
        CodeLanguage("rust", "Rust"),
        CodeLanguage("swift", "Swift"),
        CodeLanguage("bash", "Shell")
    )

    fun labelFor(code: String?): String =
        all.firstOrNull { it.code == code }?.label ?: "Plain text"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeLanguageSheet(
    current: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.block_code_language),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
            )
            CodeLanguages.all.forEach { lang ->
                val isSelected = lang.code == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelected(lang.code)
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lang.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
