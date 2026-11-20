package com.example.ui.settings

import android.content.Context
import android.content.Intent
import com.example.SettingsActivity

/** Spec 044, RF-323: el extra viaja como String estable, no como enum parcelado. */
internal const val EXTRA_SETTINGS_DESTINATION = "com.example.EXTRA_SETTINGS_DESTINATION"

/**
 * Único punto de entrada a los ajustes (RF-323). Igual que `launchCodeEditor`: si cada acceso
 * construyera su propio `Intent`, uno acabaría sin destino.
 */
internal fun launchSettings(
    context: Context,
    destination: SettingsDestination = SettingsDestination.HUB
) {
    val intent = Intent(context, SettingsActivity::class.java).apply {
        putExtra(EXTRA_SETTINGS_DESTINATION, destination.extraValue)
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    context.startActivity(intent)
}
