package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.ui.settings.SettingsCardGroup
import com.example.ui.settings.SettingsListTile
import com.example.util.exportMultipleToHtml
import com.example.util.exportMultipleToJson
import com.example.util.exportMultipleToMarkdown
import com.example.util.exportMultipleToPdf
import com.example.util.exportMultipleToTxt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFormatSheet(
    selectedNotes: List<DecryptedNote>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.share_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            @Suppress("DEPRECATION")
            val articleIcon = Icons.Default.Article

            SettingsCardGroup {
                SettingsListTile(
                    leadingIcon = Icons.Default.Description,
                    title = stringResource(id = R.string.share_format_txt),
                    modifier = Modifier.testTag("share_format_txt_btn"),
                    onClick = {
                        exportMultipleToTxt(context, selectedNotes)
                        onDismiss()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsListTile(
                    leadingIcon = Icons.Default.Code,
                    title = stringResource(id = R.string.share_format_md),
                    modifier = Modifier.testTag("share_format_md_btn"),
                    onClick = {
                        exportMultipleToMarkdown(context, selectedNotes)
                        onDismiss()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsListTile(
                    leadingIcon = articleIcon,
                    title = stringResource(id = R.string.share_format_pdf),
                    modifier = Modifier.testTag("share_format_pdf_btn"),
                    onClick = {
                        exportMultipleToPdf(context, selectedNotes)
                        onDismiss()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsListTile(
                    leadingIcon = Icons.Default.Web,
                    title = stringResource(id = R.string.share_format_html),
                    modifier = Modifier.testTag("share_format_html_btn"),
                    onClick = {
                        exportMultipleToHtml(context, selectedNotes)
                        onDismiss()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsListTile(
                    leadingIcon = Icons.Default.Settings,
                    title = stringResource(id = R.string.share_format_json),
                    modifier = Modifier.testTag("share_format_json_btn"),
                    onClick = {
                        exportMultipleToJson(context, selectedNotes)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
