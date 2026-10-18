# Spec 028 — Plan

> Adjunto a `spec.md` (spec 028, **aprobada**). Los RF/RNF se citan tal cual. El plan describe **cómo**, con su
> alternativa descartada; los nombres de fichero son los reales del repo.

## Alcance de ficheros

### `:app` — nuevo

- **`app/src/main/java/com/example/data/backup/BackupSettings.kt`** — la definición compartida (RF-191). Es un
  paquete nuevo y diminuto, como `data/theme/` (spec 025) que también es un solo fichero puro. Contiene **cuatro**
  cosas:
  - `enum class SettingType { TEXT, FLAG, NUMBER }` — cómo se lee y se escribe cada ajuste.
  - `val BACKUP_SETTINGS: Map<String, SettingType>` — **la lista**, 14 claves, declaradas **una vez**. Es lo que
    serializan los tres sitios que crean la copia y lo que restoration leen los dos que la restauran.
  - `val DEFAULT_SETTINGS: Map<String, Any>` — el valor por defecto de cada clave, **los mismos de hoy**, para que
    el refactor no pueda cambiar de paso lo que viaja. Vive en una lista porque tiene que ser **comprobable sin
    `SharedPreferences`**, que aquí no se puede levantar.
  - `sealed class BackupSetting` con `Text`, `Flag` y `Number`, y las cuatro funciones de la tabla de abajo.
- **Alternativa descartada:** una `data class BackupSettings` con catorce campos tipados. Es más explícita y el
  compilador te obliga a tocar el campo al añadir una clave, pero necesita **tres** declaraciones por clave —la
  del campo, la del constructor que la lee y la de la escritura— y el objetivo es que no haya tres. Con el mapa,
  cada clave se declara **una** vez.

### `:app` — modificado

Los cinco sitios. En los tres primeros desaparece un `JSONObject().apply { … }` de 13 líneas; en los dos últimos
desaparece una cadena de 26 líneas de `if (settings.has(...))`. Los tres tienen ya un `SharedPreferences` a mano,
así que ninguno necesita más cables.

- **`app/src/main/java/com/example/ui/viewmodel/BackupViewModel.kt`**
  - `buildBackupJson` (`:250`): las 13 líneas del bloque `settings` → `backupSettingsJson(readBackupSettings(sharedPrefs))`.
  - `restoreFromJson` (`:378`): la cadena de 26 líneas → `applyRestoredSettings(sharedPrefs, restorableSettings(settings))`.
    El `if (container.has("settings"))` de fuera se queda: es lo que hoy evita restaurar ajustes en una copia que
    no los trae.
- **`app/src/main/java/com/example/ui/viewmodel/NotesViewModel.kt`**
  - `forceSyncCloud` (`:1075`): 13 líneas → una, en `settingsObj`.
  - `finalizeRestore` (`:1370`): 26 líneas → una.
  - **No se toca** el estado de la app que usa esas mismas claves por su cuenta (`autoUpdateCheck` `:104`,
    `isBiometricEnabled` `:121`, `encryptBackups` `:123`, `setBiometric` `:503`, `setEncryptBackups` `:512`…): son
    ajustes vivos, no la copia.
- **`app/src/main/java/com/example/data/sync/SyncWorker.kt`**
  - `doWork` (`:53`): 13 líneas → una. **No se toca** el `ENCRYPT_BACKUPS_KEY` de `:77`, que decide si esta copia
    va cifrada.
- **No se toca**: `BackupAttachmentHelper`, el ZIP, el cifrado del sobre, `SharedPreferencesRepository`,
  `ThemePreference.kt`, `backup_rules.xml`, `data_extraction_rules.xml`, ningún `strings.xml` y ningún
  `build.gradle.kts` (RNF-121). El formato sigue siendo `version: 4` (RNF-122).

### `:app` — pruebas

- **`app/src/test/java/com/example/data/backup/BackupSettingsTest.kt`** — nuevo; **puro**, sin Robolectric (RNF-120).
  El paquete de la prueba **mira** el del código que prueba, como `DraftWriteTest` y
  `app/src/test/java/com/example/data/storage/`. Las dos listas, las dos funciones puras y las dos adaptadoras.
- **`app/src/test/java/com/example/BackupSettingsInvariantsTest.kt`** — nuevo; **puro por lectura de fuente**.
  RF-191 no se puede ver en las pruebas de comportamiento: las otras trece claves ya se leen igual, así que el
  fallo que se quiere vigilar —volver a escribir la lista en un sexto sitio— no cambia ninguna salida. Solo se ve
  en el texto.

**Precedentes que se siguen**: `org.json` funciona en pruebas puras de JVM porque está `testImplementation`
(`libs.org.json`, `app/build.gradle.kts:151`) y `UpdaterViewModelTest` lo usa sin `@RunWith`; el recorrido de
invariantes con umbrales y suelos de `EditorInvariantsTest`; el paquete puro propio de `data/theme`.

## Funciones puras

| Función | Módulo | Entrada | Salida | Puro | RF/RNF |
|---|---|---|---|---|---|
| `backupSettingsJson` | `:app` | `Map<String, Any>` de lo que hay en preferencias | `JSONObject` | sí | RF-187 |
| `restorableSettings` | `:app` | `JSONObject` de una copia | `List<BackupSetting>` | sí | RF-188, RF-189, RF-190 |
| `readBackupSettings` | `:app` | `SharedPreferences` | `Map<String, Any>` | no | RF-187 |
| `applyRestoredSettings` | `:app` | `SharedPreferences`, `List<BackupSetting>` | — | no | RF-192 |

Las dos puras son **la decisión** y se prueban enteras. Las dos que no lo son sonFontantería: leen trece claves con
su tipo y su defecto, y escriben lo que ya viene escrito y tipado. Aun así **se prueban**, con un doble escrito a
mano de `SharedPreferences` en el fichero de prueba (es una interfaz: unas 25 líneas y ningún comportamiento de
Android), porque es donde un error de tipos se colaría sin que ninguna prueba de comportamiento lo notase.

## Algoritmo en pseudocódigo

```
BACKUP_SETTINGS = { las 13 de hoy + AppConstants.THEME_ID_KEY, todas TEXT salvo cuatro FLAG y un NUMBER }
DEFAULT_SETTINGS = { los mismos valores por defecto que lee hoy cada sitio }

# RF-187 — Puro: qué se copia. Solo lo que está en la lista, y solo con el tipo declarado.
backupSettingsJson(valores):
    salida = {}
    para (clave, tipo) en BACKUP_SETTINGS:
        v = valores[clave] ?: seguir
        si v es del tipo declarado: salida[clave] = v
    return salida

# RF-187 — Adapter: de dónde sale. El tema se RESUELVE, no se copia en crudo.
readBackupSettings(prefs):
    mapa = { para cada clave salvo el tema: leer(prefs, clave, tipo, DEFAULT_SETTINGS[clave]) }
    mapa[THEME_ID_KEY] = resolveStoredTheme(
        prefs.getString(THEME_ID_KEY, null),
        si prefs.contains(DYNAMIC_COLORS_KEY): prefs.getBoolean(DYNAMIC_COLORS_KEY, true) sino null
    ).id
    return mapa

# RF-188/RF-189/RF-190 — Puro: qué se aplica de ESTA copia.
restorableSettings(json):
    salida = []
    para (clave, tipo) en BACKUP_SETTINGS:
        si !json.has(clave): seguir                          # copia antigua: no se toca (RF-190)
        v = leerTipado(json, clave, tipo) ?: seguir           # si el tipo no cuadra, se salta
        si clave == THEME_ID_KEY y !existeElTema(v): seguir   # RF-189
        salida += BackupSetting.de(clave, tipo, v)
    return salida

existeElTema(id) = AppThemes.all.any { it.id == id }          # en blanco tampoco existe

# RF-192 — Adapter: la escritura, en un solo apply.
applyRestoredSettings(prefs, ajustes):
    editor = prefs.edit()
    para s en ajustes:
        cuando s es Text/Flag/Number: editor.putString/putBoolean/putLong(s.key, s.value)
    editor.apply()
```

## Decisiones justificadas (con su alternativa descartada)

1. **Una lista `Map<String, SettingType>` y no catorce campos**. Ver «alternativa descartada» del alcance: el mapa
   declara cada clave **una** vez, y esa vez es la que leen los tres serializadores y los dos restauradores.
2. **Al copiar, el tema se resuelve; al restaurar, se valida.** Son dos mitades distintas de la misma decisión y
   por eso van en dos funciones. Resolver al copiar (con `resolveStoredTheme`, pura y ya probada) es lo que hace
   que la copia describa **la app como se ve**: quien nunca abrió el selector de tema —tiene `dynamic_colors`
   apagado y ningún `theme_id`, o sea toda la gente que lleva años usando la app— si no, su copia no llevaría
   tema y al restaurar saldría el dinámico, que es exactamente lo que esta spec viene a arreglar. La alternativa
   descartada es copiar `prefs.getString(THEME_ID_KEY, "")` en crudo y omitirla si viene vacía: menos código, y
   deja fuera justo a quien más le afecta.
3. **Ante un tema desconocido no se escribe nada** (RF-189, decidido con quien Decide). La alternativa
   descartada: escribir el identificador y dejar que `resolveStoredTheme` caiga al dinámico, que es lo que ya
   hace con un id desconocido. Sería automático, pero una copia vieja cambiaría el aspecto de la app sin que nadie
   lo pidiera, y «el tema que elegiste» no puede ser algo que la app no tiene.
4. **`restorableSettings` devuelve una lista y no escribe.** La alternativa descartada: que reciba el `Editor` y
   escriba. Sería una función menos, pero necesita `Context` y deja de ser pura: los tres casos que de verdad
   importan —copia sin tema, tema desconocido, tipo que no cuadra— se quedarían sin prueba en esta plataforma.
5. **El `dynamic_colors` heredado se sigue escribiendo y no se toca.** La alternativa descartada: borrarlo al
   restaurar, para que la copia no deje preferences que ya no significan nada. No: es la **entrada** de la regla de
   migración de la spec 025 para las copias antiguas, y sin él una copia vieja restaurada en una instalación
   limpia no llegaría al tema `default` que le corresponde.
6. **El formato sigue en `version: 4`** (RNF-122). La alternativa descartada: subirlo a 5. Subirlo sería lo
   correcto si una copia vieja dejara de poder leerse o si el significado de un campo cambiasse; aquí la clave es
   nueva y opcional y los dos lectores la preguntan con `has()`. Subirlo solo haría que toda copia existente
   pareciera vieja sin ganar nada.
7. **El tema se aplica al abrir la app, no en caliente** (RF-192, decidido con quien Decide). La alternativa
   descartada: un puente entre la restauración y el `ThemeViewModel` de cada `Activity`. Haría del tema el único
   ajuste que se ve al instante mientras los otros trece no, y el `ThemeViewModel` se crea por `Activity`, así que
   el puente tendría que vivir en otro sitio para no descolgarlo de la pantalla.

## Estrategia de tests (`./gradlew test`)

### 1. Pruebas **puras** de las listas y de las dos funciones puras (`BackupSettingsTest`, sí corren aquí)

- `BACKUP_SETTINGS` tiene **exactamente 14** claves e **incluye `THEME_ID_KEY`**: el olvido de esta spec es
  justamente una clave de menos, así que la lista se cuenta (RF-187).
- `DEFAULT_SETTINGS` tiene los **mismos catorce valores por defecto que lee hoy cada sitio**, uno a uno
  (`"SYSTEM"`, `true`, `""`, `true`, `true`, `""`, `PASSWORD`, `false`, `false`, `5`, `false`, `false`, `"6h"`):
  el refactor no puede cambiar de paso lo que viaja (RF-193).
- `backupSettingsJson` con los catorce valores → el bloque sale con los catorce.
- `backupSettingsJson` con una clave **de sobra** en el mapa → **no** sale en el bloque: lo que no está en la lista
  no viaja (RF-191).
- `backupSettingsJson` con un valor de **tipo que no cuadra** → esa clave no sale, y las otras trece sí.
- `restorableSettings` con los catorce → catorce, y el del tema con **su valor** (RF-188).
- `restorableSettings` **sin** `theme_id` → trece y ninguno de tema (RF-190).
- `restorableSettings` con `theme_id` **desconocido** → trece, **no** se escribe el tema y la lista no falla
  (RF-189).
- `restorableSettings` con `theme_id` **en blanco** → tampoco se escribe (RF-189): en blanco no es «sin elegir».
- `restorableSettings` con un `theme_id` que sí es del catálogo, incluido `dynamic` → se escribe (RF-188).
- `restorableSettings` con el bloque **vacío** → lista vacía, sin excepción.
- `restorableSettings` con un tipo que **no cuadra** en unajuste que no es el tema → se salta **solo** esa clave.
- Los tres tipos se conservan: `Text`, `Flag` y `Number` llegan con su tipo, no como texto.

### 2. Las dos adaptadoras, con un doble de `SharedPreferences` (`BackupSettingsTest`, sin Robolectric)

- `readBackupSettings` con las preferencias vacías → los catorce **defaults**, y el tema es `dynamic`, que es lo
  que una instalación nueva muestra de fábrica (RF-187).
- `readBackupSettings` con `dynamic_colors` apagado y **sin** `theme_id` → el tema es `default`: la copia
  describe el morado que esa persona estaba viendo (decisión 2).
- `readBackupSettings` con un `theme_id` guardado → sale ese, y el `dynamic_colors` apagado **no** lo pisa.
- `readBackupSettings` con **catorce** preferencias escritas → las catorce con su **tipo**, no convertidas a texto.
- `applyRestoredSettings` con los tres tipos → se escriben con `putString`, `putBoolean` y `putLong`, cada uno con
  su valor, en un `edit()` y un `apply()`.
- `applyRestoredSettings` con una lista **vacía** → no escribe nada.

### 3. Invariantes de `:app` (`BackupSettingsInvariantsTest`, puros por lectura de fuente)

Con las reglas del proyecto: se **quitan comentarios y cadenas** antes de buscar y cada recorrido lleva **suelo**,
para que un recorrido roto se ponga rojo en vez de pasar por vacío.

- **RF-191, serializadores** — en `BackupViewModel.kt`, `NotesViewModel.kt` y `SyncWorker.kt` hay **0** apariciones
  de `put(AppConstants.<CLAVE>_KEY,` y **≥ 1** de `backupSettingsJson(`. El patrón `put(` a secas no hace falta:
  los ajustes vivos se escriben con `putString(`/`putBoolean(` y con `setStringPref(`.
- **RF-191, restauradores** — en `BackupViewModel.kt` y `NotesViewModel.kt` hay **0** apariciones de
  `settings.get<tipo>(AppConstants.` y **≥ 1** de `applyRestoredSettings(`. El patrón es del bloque de
  restauración, no del fichero entero: `NotesViewModel` sigue usando esas claves para su estado vivo, y lo que no
  puede es reescribirlas.
- **RF-191, un solo sitio** — se cuentan las **declaraciones** de `val BACKUP_SETTINGS` y `val DEFAULT_SETTINGS`
  en todo el árbol de `:app`: exactamente **dos**, y las dos en `BackupSettings.kt`. El plan llegaba hasta aquí
  con otro invariante —«el único fichero que menciona `THEME_ID_KEY` junto a `DYNAMIC_COLORS_KEY`»— y era
  **falso**: `SharedPreferencesRepository.getAppTheme()` menciona los dos legítimamente (es el otro lector de la
  migración heredada) y `Constants.kt` los declara. Lo único que tiene que ser único es la **declaración**, no la
  mención.
- **RNF-120** — `BackupSettings.kt` **no** importa `androidx.compose`, y las dos funciones puras están en él.
- **Decisión 2** — `BackupSettings.kt` llama a `resolveStoredTheme` en vez de rederivar la caída heredada.
- Cada recorrido comprueba antes que el fichero existe, para que el inventario no se quede viejo en silencio.
- **Autocomprobación del recorrido**: una prueba le pasa un trozo de código inventado con un comentario de
  línea, uno de bloque, un texto con un `//` de URL dentro y una llamada real, y exige que solo sobreviva la
  llamada. Sin ella, un `Regex` mal escrito devuelve 0, la comprobación «no hay ni una copia de la lista» pasa
  en verde y la prueba no está mirando nada.

### 4. Honestidad sobre lo que **no** se prueba aquí

- Que un ZIP restaurado **pinte** el tema en pantalla necesita la app corriendo: va al guion de dispositivo.
- `git diff --stat` en las tareas es lo que prueba que no cambió ningún `build.gradle.kts`, ningún
  `strings.xml` ni la versión del formato (RNF-121, RNF-122).
- Las dos cadenas de 26 líneas que desaparecen las ve `git diff --stat`; su comportamiento no cambia, y eso lo
  fija el punto 2 de la estrategia 1 (los valores y los tipos no cambian).

### 5. Compilación y suite

`./gradlew :app:compileDebugKotlin` y `./gradlew :app:test --continue` contra la línea base **816/41** de `:app`
(los 41 son los de plataforma, de siempre): ninguna clase roja nueva (RNF-123).

## Guion de verificación en dispositivo (RF-187…RF-191)

Ocho pasos, con la copia local (que es la que se puede editar a mano antes de restaurarla):

1. Ajustes → **Tema**: elegir uno que no sea el de fábrica. La app lo aplica al instante.
2. Ajustes → Copia de seguridad → **Crear copia de seguridad** y guardar el ZIP.
3. Descomprimir el ZIP y abrir `backup.json`: `settings.theme_id` es **el identificador del tema elegido**
   (RF-187). Los otros trece siguen ahí con sus valores de hoy (RF-193).
4. **Borrar los datos** de la app (o instalarla limpia) → Ajustes → **Restaurar copia de seguridad** → elegir el
   ZIP. La restauración termina con éxito.
5. Reabrir la app: **el tema es el de la copia** (RF-188).
6. Editar el `backup.json` de un ZIP copiado y poner `theme_id: "no_existe"`, restaurarlo: el resto de ajustes
   **sí** se aplican y **el tema no cambia** (RF-189).
7. Borrar `theme_id` de un ZIP copiado y restaurarlo: **el tema no cambia** (RF-190).
8. Con Drive vinculado: copia **manual** y **automática**; el `backup.json` descargado trae el mismo `theme_id`
   (RF-187, RNF-124).