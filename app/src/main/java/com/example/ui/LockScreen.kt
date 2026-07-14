package com.example.ui

import android.widget.Toast
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.AuthenticationRequest.Biometric.Strength
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.BiometricPrompt
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppConstants
import com.example.PasswordType
import com.example.R
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.BiometricAuthManager

@Composable
fun LockScreen(viewModel: NotesViewModel) {
    var password by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val passwordType by viewModel.passwordType.collectAsState()
    val isRateLimited by viewModel.isRateLimited.collectAsState()
    val rateLimitRemaining by viewModel.rateLimitRemainingSeconds.collectAsState()
    val biometricManager = remember { BiometricAuthManager(context) }

    val authLauncher = rememberAuthenticationLauncher(
        object : AuthenticationResultCallback {
            override fun onAuthResult(result: AuthenticationResult) {
                when (result) {
                    is AuthenticationResult.Success -> {
                        val cipher = result.crypto?.cipher
                        if (cipher != null && viewModel.unlockWithBiometricCipher(cipher)) {
                            Toast.makeText(context, context.getString(R.string.toast_unlocked), Toast.LENGTH_SHORT).show()
                        } else {
                            hasError = true
                        }
                    }
                    is AuthenticationResult.Error -> {
                        Toast.makeText(context, result.errString, Toast.LENGTH_SHORT).show()
                    }
                    is AuthenticationResult.CustomFallbackSelected -> { }
                }
            }

            override fun onAuthAttemptFailed() { }
        }
    )

    fun triggerBiometric() {
        if (!biometricManager.isBiometricAvailable()) {
            Toast.makeText(context, context.getString(R.string.biometric_not_available), Toast.LENGTH_SHORT).show()
            return
        }
        val cipher = biometricManager.getDecryptCipher(
            AppConstants.BIOMETRIC_KEY_ALIAS,
            android.util.Base64.decode(
                viewModel.getBiometricIv(),
                android.util.Base64.NO_WRAP
            )
        )
        if (cipher == null) {
            Toast.makeText(context, context.getString(R.string.biometric_key_error), Toast.LENGTH_SHORT).show()
            return
        }
        val request = biometricRequest(
            title = context.getString(R.string.biometric_unlock_title),
            AuthenticationRequest.Biometric.Fallback.DeviceCredential,
        ) {
            setSubtitle(context.getString(R.string.biometric_unlock_subtitle))
            setMinStrength(Strength.Class3(BiometricPrompt.CryptoObject(cipher)))
        }
        authLauncher.launch(request)
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled) triggerBiometric()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.lock_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.status_encrypted),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF43A047),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            hasError = false
                        },
                        label = { Text(stringResource(id = R.string.label_enter_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        isError = hasError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (hasError) {
                        Text(
                            text = stringResource(id = R.string.error_wrong_password),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp)
                        )
                    }
                    if (isRateLimited) {
                        Text(
                            text = stringResource(R.string.error_rate_limit, rateLimitRemaining),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (viewModel.unlockApp(password)) {
                                hasError = false
                                Toast.makeText(context, context.getString(R.string.toast_unlocked), Toast.LENGTH_SHORT).show()
                            } else {
                                hasError = true
                            }
                        },
                        enabled = !isRateLimited,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.label_unlock),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Biometric unlock button
                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { triggerBiometric() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = stringResource(R.string.biometric_unlock_button),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.biometric_unlock_button),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
