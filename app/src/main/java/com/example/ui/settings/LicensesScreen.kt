package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.CustomTopBar

private data class OssLibrary(
    val name: String,
    val license: String,
    val url: String
)

private val ossLibraries = listOf(
    OssLibrary("Kotlin", "Apache 2.0", "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE"),
    OssLibrary("Jetpack Compose", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OssLibrary("Material 3 (Material Design)", "Apache 2.0", "https://github.com/material-components/material-components-android"),
    OssLibrary("Room", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    OssLibrary("OkHttp", "Apache 2.0", "https://github.com/square/okhttp/blob/master/LICENSE.txt"),
    OssLibrary("Retrofit", "Apache 2.0", "https://github.com/square/retrofit/blob/master/LICENSE.txt"),
    OssLibrary("Moshi", "Apache 2.0", "https://github.com/square/moshi/blob/master/LICENSE.txt"),
    OssLibrary("Coil", "Apache 2.0", "https://github.com/coil-kt/coil/blob/main/LICENSE.txt"),
    OssLibrary("Firebase (Google)", "Apache 2.0", "https://firebase.google.com/terms"),
    OssLibrary("Kotlin Coroutines", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
    OssLibrary("WorkManager", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/work"),
    OssLibrary("Android Biometric", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/biometric"),
    OssLibrary("compose-markdown", "Apache 2.0", "https://github.com/jeziellago/compose-markdown/blob/main/LICENSE"),
    OssLibrary("Robolectric", "MIT", "https://github.com/robolectric/robolectric/blob/master/LICENSE"),
    OssLibrary("JUnit 4", "EPL 2.0", "https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("LicensesScreen", "openUrl failed", e)
        }
    }

    Scaffold(
        topBar = {
            CustomTopBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Text(
                        text = stringResource(R.string.licenses_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl("https://github.com/ESTRIN217/secure-notes/blob/master/LICENSE") }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.licenses_app_license),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "MIT — ESTRIN217",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                SettingsCardGroup {
                    ossLibraries.forEachIndexed { index, lib ->
                        OssLibraryItem(lib = lib, onClick = { openUrl(lib.url) })
                        if (index < ossLibraries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OssLibraryItem(
    lib: OssLibrary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lib.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = lib.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
