package com.example.ui.settings

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.AuthenticationRequest.Biometric.Strength
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.AutoLockTimeout
import androidx.compose.material3.TopAppBar
import com.example.PasswordType
import com.example.R
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.BiometricAuthManager
import com.example.util.PasswordStrength
import com.example.util.checkPasswordStrength
import com.example.util.toColor
import com.example.util.toLabelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isPasswordSet by viewModel.isPasswordSet.collectAsStateWithLifecycle()
    val passwordType by viewModel.passwordType.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val screenshotEnabled by viewModel.screenshotEnabled.collectAsStateWithLifecycle()
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var pendingEnableBiometric by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val biometricManager = remember { BiometricAuthManager(context) }
    val enrollCipher = remember { mutableStateOf<javax.crypto.Cipher?>(null) }

    val enrollLauncher = rememberAuthenticationLauncher(
        object : AuthenticationResultCallback {
            override fun onAuthResult(result: AuthenticationResult) {
                when (result) {
                    is AuthenticationResult.Success -> {
                        result.crypto?.cipher?.let { c ->
                            viewModel.saveBiometricEncryptedPassword(c)
                            viewModel.setBiometricEnabled(true)
                            Toast.makeText(context, context.getString(R.string.biometric_enabled), Toast.LENGTH_SHORT).show()
                        }
                    }
                    is AuthenticationResult.Error -> {
                        Toast.makeText(context, result.errString, Toast.LENGTH_SHORT).show()
                    }
                    is AuthenticationResult.CustomFallbackSelected -> { }
                }
                enrollCipher.value = null
                pendingEnableBiometric = false
            }

            override fun onAuthAttemptFailed() { }
        }
    )

    // Handle pending biometric enrollment
    LaunchedEffect(pendingEnableBiometric) {
        if (!pendingEnableBiometric) return@LaunchedEffect
        if (!biometricManager.isBiometricAvailable()) {
            Toast.makeText(context, context.getString(R.string.biometric_not_available), Toast.LENGTH_SHORT).show()
            pendingEnableBiometric = false
            return@LaunchedEffect
        }
        try {
            biometricManager.createKey(AppConstants.BIOMETRIC_KEY_ALIAS)
            val cipher = biometricManager.getEncryptCipher(AppConstants.BIOMETRIC_KEY_ALIAS)
            enrollCipher.value = cipher
            val request = biometricRequest(
                title = context.getString(R.string.biometric_enable_title),
                AuthenticationRequest.Biometric.Fallback.DeviceCredential,
            ) {
                setSubtitle(context.getString(R.string.biometric_enable_subtitle))
                setMinStrength(Strength.Class3(BiometricPrompt.CryptoObject(cipher)))
            }
            enrollLauncher.launch(request)
        } catch (e: Exception) {
            Log.e("PrivacySettings", "biometric enrollment failed", e)
            Toast.makeText(context, context.getString(R.string.biometric_key_error), Toast.LENGTH_LONG).show()
            enrollCipher.value = null
            pendingEnableBiometric = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopAppBar(
            title = {
              Text(
                        text = stringResource(R.string.title_lock_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
            },
            navigationIcon = {
              IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
              }
            },
            actions = {}
          )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Status
            item {
                SettingsSectionTitle(title = stringResource(R.string.label_e2e_encryption))
                SettingsCardGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconContainer(
                            icon = Icons.Default.Shield,
                            isSelected = isPasswordSet
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_e2e_encryption),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = if (isPasswordSet) stringResource(R.string.security_active) else stringResource(R.string.setup_credentials),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPasswordSet) Color(0xFF43A047) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Password setup or management
            item {
                if (!isPasswordSet) {
                    // ==============================
                    // SETUP PASSWORD
                    // ==============================
                    SettingsSectionTitle(title = stringResource(R.string.label_setup_password))
                    SettingsCardGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Password type selector
                            Text(
                                text = stringResource(R.string.password_type_label),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(modifier = Modifier.selectableGroup()) {
                                listOf(PasswordType.PIN, PasswordType.PASSWORD).forEach { type ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .selectable(
                                                selected = passwordType == type,
                                                onClick = { viewModel.setPasswordType(type) },
                                                role = Role.RadioButton
                                            )
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = passwordType == type,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(
                                                if (type == PasswordType.PIN) R.string.password_type_pin else R.string.password_type_password
                                            ),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = {
                                    Text(
                                        stringResource(
                                            if (passwordType == PasswordType.PIN) R.string.label_setup_pin else R.string.label_setup_password
                                        )
                                    )
                                },
                                visualTransformation = if (passwordType == PasswordType.PIN) PasswordVisualTransformation() else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // Strength indicator (only for PASSWORD type)
                            if (passwordType == PasswordType.PASSWORD && passwordInput.isNotEmpty()) {
                                val strength = remember(passwordInput) { checkPasswordStrength(passwordInput) }
                                val strengthColor = strength.toColor()
                                val strengthLabel = stringResource(strength.toLabelRes())

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.password_strength),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = strengthLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = strengthColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            when (strength) {
                                                PasswordStrength.WEAK -> 0.25f
                                                PasswordStrength.MEDIUM -> 0.5f
                                                PasswordStrength.STRONG -> 0.75f
                                                PasswordStrength.VERY_STRONG -> 1.0f
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = strengthColor,
                                        trackColor = strengthColor.copy(alpha = 0.2f),
                                    )
                                    if (strength == PasswordStrength.WEAK || strength == PasswordStrength.MEDIUM) {
                                        Text(
                                            text = stringResource(R.string.password_suggestion),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = passwordConfirm,
                                onValueChange = { passwordConfirm = it },
                                label = { Text(stringResource(R.string.confirm_master_password)) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Text(
                                text = stringResource(R.string.label_set_password_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    val minLen = if (passwordType == PasswordType.PIN) 4 else 6
                                    if (passwordInput.length < minLen) {
                                        Toast.makeText(context, context.getString(R.string.toast_password_too_short), Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (passwordInput != passwordConfirm) {
                                        Toast.makeText(context, context.getString(R.string.toast_passwords_do_not_match), Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.setMasterPassword(passwordInput)
                                    onBack()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_setup_password_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(stringResource(id = R.string.btn_save), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // ==============================
                    // ACTIVE SECURITY
                    // ==============================
                    SettingsSectionTitle(title = stringResource(R.string.security_active))
                    SettingsCardGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF43A047),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.security_active),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF43A047)
                                    )
                                )
                            }

                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                text = stringResource(
                                    if (passwordType == PasswordType.PIN) R.string.security_pin_type else R.string.security_password_type
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Biometric toggle
                            if (biometricManager.isBiometricAvailable()) {
                                SettingsSwitchTile(
                                    icon = Icons.Default.Fingerprint,
                                    title = stringResource(R.string.biometric_unlock_label),
                                    subtitle = stringResource(
                                        if (isBiometricEnabled) R.string.biometric_enabled_desc else R.string.biometric_disabled_desc
                                    ),
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            pendingEnableBiometric = true
                                        } else {
                                            viewModel.setBiometricEnabled(false)
                                        }
                                    }
                                )
                            }

                            // Screenshot / Recents toggle
                            SettingsSwitchTile(
                                icon = Icons.Default.PhotoCamera,
                                title = stringResource(R.string.screenshot_toggle_label),
                                subtitle = stringResource(R.string.screenshot_toggle_desc),
                                checked = screenshotEnabled,
                                onCheckedChange = { viewModel.setScreenshotEnabled(it) }
                            )

                            // Auto-lock timeout
                            val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
                            var showAutoLockDialog by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SettingsIconContainer(
                                    icon = Icons.Default.Timer,
                                    isSelected = autoLockTimeout != 0L
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.auto_lock_title),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = when {
                                            autoLockTimeout < 0 -> stringResource(R.string.auto_lock_immediately)
                                            autoLockTimeout == 0L -> stringResource(R.string.auto_lock_disabled)
                                            else -> stringResource(R.string.auto_lock_timeout_desc, autoLockTimeout)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showAutoLockDialog = true }) {
                                    Text(
                                        text = if (autoLockTimeout == 0L) stringResource(R.string.btn_enable) else stringResource(R.string.btn_change),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (showAutoLockDialog) {
                                AlertDialog(
                                    onDismissRequest = { showAutoLockDialog = false },
                                    title = { Text(stringResource(R.string.auto_lock_title)) },
                                    text = {
                                        Column {
                                            AutoLockTimeout.entries.forEach { option ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .selectable(
                                                            selected = autoLockTimeout == option.minutes,
                                                            onClick = {
                                                                viewModel.setAutoLockTimeout(option.minutes)
                                                                showAutoLockDialog = false
                                                            },
                                                            role = Role.RadioButton
                                                        )
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = autoLockTimeout == option.minutes,
                                                        onClick = null
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = when (option) {
                                                        AutoLockTimeout.IMMEDIATELY -> stringResource(R.string.auto_lock_immediately)
                                                        AutoLockTimeout.DISABLED -> stringResource(R.string.auto_lock_off)
                                                        AutoLockTimeout.MINUTE_1 -> stringResource(R.string.auto_lock_1min)
                                                        AutoLockTimeout.MINUTES_5 -> stringResource(R.string.auto_lock_5min)
                                                        AutoLockTimeout.MINUTES_15 -> stringResource(R.string.auto_lock_15min)
                                                        AutoLockTimeout.MINUTES_30 -> stringResource(R.string.auto_lock_30min)
                                                    },
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showAutoLockDialog = false }) {
                                            Text(stringResource(R.string.btn_cancel))
                                        }
                                    }
                                )
                            }

                            // Change password dialog
                            var showChangePasswordDialog by remember { mutableStateOf(false) }
                            var changePasswordInput by remember { mutableStateOf("") }
                            var changeNewPasswordInput by remember { mutableStateOf("") }
                            var changeConfirmPasswordInput by remember { mutableStateOf("") }
                            var changePasswordError by remember { mutableStateOf<String?>(null) }

                            Button(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(horizontal = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.change_password))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.change_password), fontWeight = FontWeight.Bold)
                            }

                            if (showChangePasswordDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showChangePasswordDialog = false
                                        changePasswordError = null
                                    },
                                    title = { Text(stringResource(R.string.change_password_dialog_title)) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            OutlinedTextField(
                                                value = changePasswordInput,
                                                onValueChange = { changePasswordInput = it; changePasswordError = null },
                                                label = { Text(stringResource(R.string.change_password_current)) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = changeNewPasswordInput,
                                                onValueChange = { changeNewPasswordInput = it; changePasswordError = null },
                                                label = { Text(stringResource(R.string.change_password_new)) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (passwordType == PasswordType.PASSWORD && changeNewPasswordInput.isNotEmpty()) {
                                                val strength = remember(changeNewPasswordInput) { checkPasswordStrength(changeNewPasswordInput) }
                                                val strengthColor = strength.toColor()
                                                val strengthLabel = stringResource(strength.toLabelRes())
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.password_strength),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = strengthLabel,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = strengthColor
                                                    )
                                                }
                                            }
                                            OutlinedTextField(
                                                value = changeConfirmPasswordInput,
                                                onValueChange = { changeConfirmPasswordInput = it; changePasswordError = null },
                                                label = { Text(stringResource(R.string.change_password_confirm)) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = if (passwordType == PasswordType.PIN) KeyboardType.NumberPassword else KeyboardType.Password
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            changePasswordError?.let {
                                                Text(
                                                    text = it,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val minLen = if (passwordType == PasswordType.PIN) 4 else 6
                                            if (changePasswordInput.isEmpty()) {
                                                changePasswordError = context.getString(R.string.toast_password_too_short)
                                                return@TextButton
                                            }
                                            if (changeNewPasswordInput.length < minLen || changeNewPasswordInput != changeConfirmPasswordInput) {
                                                changePasswordError = if (changeNewPasswordInput.length < minLen) {
                                                    context.getString(R.string.toast_password_too_short)
                                                } else {
                                                    context.getString(R.string.toast_passwords_do_not_match)
                                                }
                                                return@TextButton
                                            }
                                            if (viewModel.changePassword(changePasswordInput, changeNewPasswordInput)) {
                                                Toast.makeText(context, context.getString(R.string.toast_password_changed), Toast.LENGTH_SHORT).show()
                                                showChangePasswordDialog = false
                                                changePasswordError = null
                                            } else {
                                                changePasswordError = context.getString(R.string.toast_wrong_current_password)
                                            }
                                        }) {
                                            Text(stringResource(R.string.btn_save))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showChangePasswordDialog = false
                                            changePasswordError = null
                                        }) {
                                            Text(stringResource(R.string.btn_cancel))
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.deletePassword()
                                    onBack()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .padding(horizontal = 12.dp)
                                    .testTag("remove_password_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_master_password))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remove_protection), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
