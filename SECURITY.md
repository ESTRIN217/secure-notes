# Seguridad — Secure Notes

## Reportar una vulnerabilidad

**No abras issues públicos** para vulnerabilidades. Escríbeme por privado vía mi
perfil de GitHub ([ESTRIN217](https://github.com/ESTRIN217)) indicando versión de
la app, pasos de reproducción e impacto estimado. Respondo y publico la corrección
con crédito si lo deseas.

## Lo que está (y no está) cubierto

* Cifrado client-side AES-256-GCM + PBKDF2 (200 000 iteraciones), salt e IV por nota.
* **Sin tu contraseña maestra es matemáticamente imposible descifrar.** Perderla no
  es un bug: nadie —incluido el desarrollador— puede recuperar tus notas.
* Los respaldos de Drive viajan ya cifrados; el token OAuth vive en
  `EncryptedSharedPreferences`.
* La IA es local o LAN por diseño; no hay endpoints cloud que auditar.

## Superficie a vigilar

`CipherService`/`KeyDerivation`, `GoogleDriveSyncService` (scopes, refresh de
token), `FileImporter` (límite de tamaño, intents `VIEW`), `BiometricAuthManager`
y el aislamiento del motor JNI (`LlamaCppEngine`: jamás `destroy()`).
