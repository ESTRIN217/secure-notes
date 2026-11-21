package com.example.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
