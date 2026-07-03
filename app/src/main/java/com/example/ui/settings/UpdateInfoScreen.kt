package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateInfoScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var checkResult by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var autoCheckEnabled by remember { mutableStateOf(true) }

    val currentVersion = BuildConfig.VERSION_NAME

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
                        text = stringResource(R.string.update_title),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Version Card
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.update_current_version),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "v$currentVersion",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Update Settings Card
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.update_auto_check),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { autoCheckEnabled = it }
                    )
                }
            }

            // Check for Update Button
            Button(
                onClick = {
                    isChecking = true
                    checkResult = null
                    checkForUpdate(currentVersion) { result ->
                        isChecking = false
                        checkResult = result
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isChecking) stringResource(R.string.update_checking)
                    else stringResource(R.string.update_check_now),
                    fontWeight = FontWeight.Bold
                )
            }

            // Result
            checkResult?.let { result ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isUpToDate = result.startsWith("up_to_date")
                        Icon(
                            imageVector = if (isUpToDate) Icons.Default.CheckCircle else Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = if (isUpToDate) Color(0xFF43A047) else Color(0xFFFFA000),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isUpToDate) stringResource(R.string.update_up_to_date)
                            else stringResource(R.string.update_available, result.removePrefix("update:")),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isUpToDate) Color(0xFF43A047) else Color(0xFFFFA000)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun checkForUpdate(currentVersion: String, onResult: (String) -> Unit) {
    Thread {
        try {
            val url = java.net.URL("https://api.github.com/repos/ESTRIN217/secure-notes/releases/latest")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val latestTag = json.optString("tag_name", "") ?: ""
            val latestVersion = latestTag.removePrefix("v")

            if (latestVersion.isEmpty()) {
                onResult("up_to_date")
            } else {
                val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val latest = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val isNewer = compareVersions(latest, current) > 0
                if (isNewer) {
                    onResult("update:$latestVersion")
                } else {
                    onResult("up_to_date")
                }
            }
        } catch (e: Exception) {
            onResult("up_to_date")
        }
    }.start()
}

private fun compareVersions(a: List<Int>, b: List<Int>): Int {
    for (i in 0 until maxOf(a.size, b.size)) {
        val va = a.getOrElse(i) { 0 }
        val vb = b.getOrElse(i) { 0 }
        if (va != vb) return va - vb
    }
    return 0
}
