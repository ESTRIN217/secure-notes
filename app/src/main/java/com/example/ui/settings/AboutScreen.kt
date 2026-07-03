package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon & Name
            item {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.about_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Encryption
            item {
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Lock,
                        title = stringResource(R.string.about_encryption),
                        trailingIcon = null,
                        onClick = {}
                    )
                }
            }

            // Version
            item {
                SettingsSectionTitle(title = stringResource(R.string.update_current_version))
                SettingsCardGroup {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.update_current_version_label, versionName),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        leadingContent = {
                            SettingsIconContainer(icon = Icons.Default.Info)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    )
                }
            }

            // GitHub
            item {
                SettingsSectionTitle(title = stringResource(R.string.about_github))
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Code,
                        title = stringResource(R.string.about_github),
                        subtitle = "ESTRIN217/secure-notes",
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/ESTRIN217/secure-notes")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
