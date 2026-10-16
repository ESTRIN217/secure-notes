package com.example.ui.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.CrashInformeUiState

/**
 * La pantalla del informe de fallo (spec 020, RF-121, RF-123).
 *
 * Pinta y lanza eventos: ni un `ClipboardManager`, ni un `Intent`, ni un fichero. Todo eso vive en la
 * `Activity` (constitución 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(
    estado: CrashInformeUiState,
    onCerrar: () -> Unit,
    onCopiar: () -> Unit,
    onCompartir: () -> Unit,
    onEnviar: () -> Unit,
    onDescartar: () -> Unit
) {
    Scaffold(
        topBar = { BarraSuperior(onCerrar) },
        bottomBar = {
            if (estado.texto.isNotEmpty()) {
                Acciones(
                    onCopiar = onCopiar,
                    onCompartir = onCompartir,
                    onEnviar = onEnviar,
                    onDescartar = onDescartar
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (estado.texto.isEmpty()) {
                SinInforme(cargando = estado.cargando)
            } else {
                TextoDelInforme(estado.texto)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSuperior(onCerrar: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.crash_report_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onCerrar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
    )
}

/**
 * El informe, seleccionable y desplazable.
 *
 * `SelectionContainer` y no un `Text` normal: con doscientas líneas de traza, elegir a mano lo que se quiere
 * copiar es la diferencia entre copiar el informe y copiar tres líneas equivocadas.
 */
@Composable
private fun TextoDelInforme(texto: String) {
    SelectionContainer {
        Text(
            text = texto,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun SinInforme(cargando: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (cargando) return@Column
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.crash_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.crash_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Las tres salidas del informe y la de descartarlo.
 *
 * El aviso va **siempre** y está justo encima del botón que publica el texto: el mensaje de la excepción es
 * el único campo del informe que la app no controla (RNF-67).
 */
@Composable
private fun Acciones(
    onCopiar: () -> Unit,
    onCompartir: () -> Unit,
    onEnviar: () -> Unit,
    onDescartar: () -> Unit
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.crash_send_privacy_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCopiar,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.weight(0.1f))
                    Text(
                        text = stringResource(R.string.crash_copy),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onCompartir,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.weight(0.1f))
                    Text(
                        text = stringResource(R.string.crash_share),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    onClick = onEnviar,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.crash_send_github),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            TextButton(
                onClick = onDescartar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.crash_discard))
            }
        }
    }
}