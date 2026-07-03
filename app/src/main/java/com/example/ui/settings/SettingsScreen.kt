package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DarkModeOption
import com.example.R
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToUpdateInfo: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val darkModeOption by viewModel.darkModeOption.collectAsState()
    val isDynamicColor by viewModel.isDynamicColor.collectAsState()
    val language by viewModel.language.collectAsState()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(id = R.string.nav_settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Hero
            HeroCard()

            // Appearance Section
            SectionHeader(title = stringResource(R.string.settings_appearance))
            SettingsCard {
                DynamicColorsRow(isEnabled = isDynamicColor, onToggle = { viewModel.setDynamicColor(it) })
                SettingsDivider()
                DarkModeRow(currentOption = darkModeOption, onSelect = { viewModel.setDarkModeOption(it) })
            }

            // Language Section
            SectionHeader(title = stringResource(R.string.settings_language))
            SettingsCard {
                ClickableSettingsRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = getLanguageDisplayName(language),
                    onClick = { showLanguageSheet = true }
                )
            }

            // Storage & Data Section
            SectionHeader(title = stringResource(R.string.settings_storage_data))
            SettingsCard {
                ClickableSettingsRow(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.settings_backup_restore),
                    subtitle = stringResource(R.string.settings_backup_restore_desc),
                    onClick = onNavigateToBackupRestore
                )
            }

            // Information Section
            SectionHeader(title = stringResource(R.string.settings_information))
            SettingsCard {
                ClickableSettingsRow(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.settings_changelog),
                    subtitle = stringResource(R.string.settings_changelog_desc),
                    onClick = { showChangelogSheet = true }
                )
                SettingsDivider()
                ClickableSettingsRow(
                    icon = Icons.Default.Update,
                    title = stringResource(R.string.settings_check_update),
                    subtitle = "",
                    onClick = onNavigateToUpdateInfo
                )
                SettingsDivider()
                ClickableSettingsRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_desc),
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            currentLanguage = language,
            onSelect = { locale ->
                viewModel.setLanguage(locale)
                showLanguageSheet = false
                Toast.makeText(context, context.getString(R.string.toast_language_changed), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showChangelogSheet) {
        ChangelogBottomSheet(
            onDismiss = { showChangelogSheet = false }
        )
    }
}

@Composable
private fun HeroCard() {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "Secure Notes Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.about_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.about_encryption),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF43A047),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun DynamicColorsRow(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = stringResource(R.string.settings_dynamic_colors),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings_dynamic_colors),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = stringResource(R.string.settings_dynamic_colors_desc),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun DarkModeRow(currentOption: DarkModeOption, onSelect: (DarkModeOption) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (currentOption) {
                    DarkModeOption.SYSTEM -> Icons.Default.BrightnessAuto
                    DarkModeOption.ON -> Icons.Default.DarkMode
                    DarkModeOption.OFF -> Icons.Default.LightMode
                },
                contentDescription = stringResource(R.string.settings_dark_mode),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_dark_mode),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModeChip(
                label = stringResource(R.string.settings_dark_mode_system),
                selected = currentOption == DarkModeOption.SYSTEM,
                onClick = { onSelect(DarkModeOption.SYSTEM) }
            )
            DarkModeChip(
                label = stringResource(R.string.settings_dark_mode_on),
                selected = currentOption == DarkModeOption.ON,
                onClick = { onSelect(DarkModeOption.ON) }
            )
            DarkModeChip(
                label = stringResource(R.string.settings_dark_mode_off),
                selected = currentOption == DarkModeOption.OFF,
                onClick = { onSelect(DarkModeOption.OFF) }
            )
        }
    }
}

@Composable
private fun RowScope.DarkModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.weight(1f),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun ClickableSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LanguageOption(
                label = stringResource(R.string.settings_lang_default),
                value = "",
                selected = currentLanguage == "",
                onClick = { onSelect("") }
            )
            LanguageOption(
                label = stringResource(R.string.settings_lang_en),
                value = "en",
                selected = currentLanguage == "en",
                onClick = { onSelect("en") }
            )
            LanguageOption(
                label = stringResource(R.string.settings_lang_es),
                value = "es-VE",
                selected = currentLanguage == "es-VE",
                onClick = { onSelect("es-VE") }
            )
            LanguageOption(
                label = stringResource(R.string.settings_lang_pt),
                value = "pt-BR",
                selected = currentLanguage == "pt-BR",
                onClick = { onSelect("pt-BR") }
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, MaterialTheme.colorScheme.surface),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    var releaseNotes by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val url = java.net.URL("https://api.github.com/repos/ESTRIN217/secure-notes/releases/latest")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = connection.inputStream.bufferedReader().readText()
            val json = org.json.JSONObject(response)
            val body = json.optString("body", "No release notes available.")
            releaseNotes = body
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            error = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.changelog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.changelog_loading))
                        }
                    }
                }
                error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    Text(
                        text = releaseNotes ?: "",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun getLanguageDisplayName(locale: String): String {
    return when (locale) {
        "" -> "System"
        "en" -> "English"
        "es-VE" -> "Español (Venezuela)"
        "pt-BR" -> "Português (Brasil)"
        else -> locale
    }
}
