package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.estrin217.tema.AppThemes
import com.example.DarkModeOption
import com.example.ui.theme.MyApplicationTheme

/**
 * Previews del hub de ajustes (spec 044 enmienda). Usan las mismas funciones que la pantalla real,
 * pero con estado y callbacks inertes: no hay ViewModels, base de datos ni permisos en la vista previa.
 */
@Preview(name = "Lista de categorías", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun SettingsCategoryListPreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SettingsCategoryList(
                selected = SettingsDestination.APPEARANCE,
                onSelect = {}
            )
        }
    }
}

@Preview(name = "Cabecera del detalle", showBackground = true, widthDp = 600, heightDp = 120)
@Composable
fun SettingsDetailHeaderPreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsDetailHeader(
                title = categoryLabel(SettingsDestination.APPEARANCE),
                onClose = {}
            )
        }
    }
}

@Preview(name = "Detalle · Apariencia", showBackground = true, widthDp = 720, heightDp = 900)
@Composable
fun SettingsAppearanceDetailPreview() {
    PreviewDetailPane(SettingsDestination.APPEARANCE)
}

@Preview(name = "Detalle · Idioma", showBackground = true, widthDp = 720, heightDp = 900)
@Composable
fun SettingsLanguageDetailPreview() {
    PreviewDetailPane(SettingsDestination.LANGUAGE, language = "es-VE")
}

@Preview(name = "Detalle · Información", showBackground = true, widthDp = 720, heightDp = 900)
@Composable
fun SettingsInformationDetailPreview() {
    PreviewDetailPane(SettingsDestination.INFORMATION)
}

@Preview(name = "Opciones de idioma", showBackground = true, widthDp = 420, heightDp = 700)
@Composable
fun LanguageOptionsPreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LanguageOptionsColumn(currentLanguage = "es-VE", onSelect = {})
        }
    }
}

@Composable
private fun PreviewDetailPane(
    category: SettingsDestination,
    language: String = "",
    floatingActive: Boolean = false
) {
    MyApplicationTheme(darkTheme = true) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                SettingsDetailHeader(
                    title = categoryLabel(category),
                    onClose = {}
                )
                SettingsCategoryContent(
                    category = category,
                    appTheme = AppThemes.dynamic,
                    darkMode = DarkModeOption.SYSTEM,
                    language = language,
                    floatingActive = floatingActive,
                    canUseOverlay = true,
                    onLanguageSelected = {},
                    onFloatingToggle = {},
                    onOpenCodeEditor = {},
                    onThemeSelected = {},
                    onDarkModeSelected = {},
                    onOpenOverlaySettings = {}
                )
            }
        }
    }
}

@Preview(name = "Ajustes Two-Pane Tablet/PC", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
fun SettingsTwoPanePreview() {
    MyApplicationTheme(darkTheme = true) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxSize()) {
                Surface(
                    Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    SettingsCategoryList(selected = SettingsDestination.APPEARANCE, onSelect = {})
                }
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsDetailHeader(title = categoryLabel(SettingsDestination.APPEARANCE), onClose = {})
                    }
                }
            }
        }
    }
}
