package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.estrin217.tema.AppTheme
import com.estrin217.tema.AppThemes
import com.estrin217.tema.LocalAppTheme

/**
 * El tema de toda la app (spec 025, RF-162).
 *
 * Se acabó el `dynamicColor: Boolean`: ya no es un interruptor sino **qué tema** de los seis, y el color sale del
 * catálogo. Cuando el tema es el dinámico —el de por defecto— los colores se los sigue pidiendo Android, que es lo
 * que hacía esta función antes de que existiera la spec; cuando no, salen de [AppThemes].
 *
 * También es donde se publica [LocalAppTheme], que es lo que hace que el editor de código y los bloques de nota
 * pinten con el tema sin saber nada de la preferencia. Una sola línea aquí y el resto de la app lo hereda.
 *
 * El `Context` se sigue leyendo solo en la rama dinámica: un tema fijo no lo necesita, y las pruebas de captura no
 * tienen por quémountar uno.
 */
@Composable
fun MyApplicationTheme(
  appTheme: AppTheme = AppThemes.dynamic,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalAppTheme provides appTheme) {
    MaterialTheme(
      colorScheme = AppThemes.schemeFor(appTheme, darkTheme) ?: dynamicScheme(darkTheme),
      typography = Typography,
      content = content,
    )
  }
}

/** Los colores que le pide al sistema, que es lo único que aquí necesita un `Context`. */
@Composable
private fun dynamicScheme(darkTheme: Boolean): ColorScheme {
  val context = LocalContext.current
  return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}