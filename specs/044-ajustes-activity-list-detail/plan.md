# Plan — Spec 044

## Archivos y responsabilidades

| Archivo | Responsabilidad | RF |
|---|---|---|
| `ui/settings/SettingsDestination.kt` | Enum puro de categorías/destinos + conversión desde el extra del `Intent`. Sin `android`. | RF-316, RF-324, RF-325, RNF-210 |
| `ui/settings/SettingsLauncher.kt` | `launchSettings(context, destination)`: único `Intent` hacia la Activity. | RF-323 |
| `SettingsActivity.kt` | Host: locale, `FLAG_SECURE`, tema, tasa de refresco, ViewModels, candado y contenido del scaffold. | RF-321, RF-322, RNF-206, RNF-208 |
| `ui/settings/SettingsApp.kt` | `NavigableListDetailPaneScaffold` con la lista de ocho categorías y el panel de detalle. | RF-317…RF-320, RF-322 |
| `ui/settings/SettingsCategoryDetail.kt` | Filas en línea de cada categoría, extraídas del `LazyColumn` actual. | RF-319 |
| `ui/viewmodel/ViewModelFactories.kt` | Factories compartidos por `MainActivity` y `SettingsActivity`. | RNF-208 |
| `ui/Screen.kt` | Redirige los accesos a `launchSettings`; quita los `Screen` de ajustes y sus rutas del `ScreenSaver`. | RF-316, RF-323 |
| `ui/ShortcutDestinations.kt` | El atajo de ajustes usa `launchSettings`. | RF-316, RF-323 |
| `MainActivity.kt` | El extra de actualización lanza ajustes; usa los factories compartidos. | RF-324, RNF-208 |
| `AndroidManifest.xml` | actividad no exportada, sin intent-filters. | RNF-206 |
| `libs.versions.toml`, `app/build.gradle.kts`, `oss-licenses.json` | Dependencia adaptativa y licencia. | RNF-207 |

## Funciones puras
- `SettingsDestination.fromExtra(value: String?): SettingsDestination` — valor desconocido o vacío
  cae a `HUB`. Sin `Context`, sin `Intent`.
- `SettingsDestination.extraValue: String` — valor estable que viaja en el `Intent`.

## Algoritmo (pseudocódigo)
```
Activity.onCreate:
  applicar locale, FLAG_SECURE, tema, requestPeak
  destino = SettingsDestination.fromExtra(intent.getStringExtra(EXTRA))
  setContent {
    VMs = fabricsCompartits(application)
    if (isPasswordSet && !isUnlocked) LockScreen(notesVM)
    else SettingsApp(destino, VMs, onClose = { finish() })
  }

SettingsApp:
  navigator = rememberListDetailPaneScaffoldNavigator<SettingsDestination>(initial = destino)
  NavigableListDetailPaneScaffold(
    listPane = { AnimatedPane { SettingsCategoryList(onSelect = { navigateTo(Detail, it) }) } },
    detailPane = { AnimatedPane {
        when (dest = navigator.currentDestination?.contentKey) {
          is Category -> SettingsCategoryDetail(dest, onNavigate = { navigateTo(Detail, it) })
          is SubScreen -> thatScreen(onBack = { paneBack() })
        }
    } }
  )
  BackHandler { if (navigator.canNavigateBack()) navigateBack() else onClose() }
```

## Decisiones
- **D1. Activity no exportada y sin intent-filters.** Igual que `CrashReportActivity`: solo la abre
  esta app. Alternativa descartada: poner un intent-filter para abrir ajustes desde fuera; ningún
  caso de uso lo pide y ampliaría la superficie de ataque.
- **D2. `NavigableListDetailPaneScaffold` en vez de `ListDetailPaneScaffold` a secas.** El primero
  trae pila y back dentro del scaffold, que es justo lo que necesita el teléfono (RF-318). El
  segundo obligaría a gestionar la pila a mano.
- **D3. La lista son las ocho categorías, no las filas.** Fue confirmado por el usuario. Cada
  categoría agrupa sus filas en el detalle.
- **D4. Las sub-pantallas se pintan directamente en el detalle, sin tocarlas.** Cada una ya trae su
  `Scaffold` y su barra; envolverlas añadiría una segunda barra (RF-320). Alternativa descartada:
  añadir un parámetro `inPane` a las ocho pantallas, que habría tocado ocho archivos sin ganar nada.
- **D5. Factories compartidos en un archivo nuevo.** Duplicar el factory de `AiViewModel` (que
  construye el grafo completo) sería una fuente de divergencia. El motor sigue pasando por
  `AiModelHost`.
- **D6. La Activity crea su propio `NotesViewModel` y muestra `LockScreen`.** Cumple RF-321 sin
  compartir estado desbloqueado. Consecuencia asumida y documentada en RNF-209.
- **D7. Un único `launchSettings`.** Igual que `launchCodeEditor`: si cada acceso construyera su
  `Intent`, uno acabaría sin destino.
- **D8. Dependencias `androidx.compose.material3.adaptive:adaptive` y `:adaptive-navigation`.**
  El alias `material3-adaptive-navigation-suite` es el `NavigationSuiteScaffold`, no el scaffold
  lista/detalle (comprobado en el AAR), así que se descarta. Las versiones las fija el Compose BOM
  2026.09.00 (1.3.0) y se añaden a `oss-licenses.json`.

## Enmienda de interfaz (RF-326…RF-333, RNF-211…RNF-212)
- `ui/settings/SettingsCategoryCatalog.kt`: datos de las ocho categorías (destino, etiqueta,
  subtítulo, icono, grupo) y la función pura `matchesQuery(titulo, subtitulo, consulta)`.
- `ui/settings/LanguageOptions.kt`: lista pura de idiomas (`locale`, `labelRes`, `flagRes`) y un
  composable de opciones reutilizado por el bottom sheet y por el detalle de la categoría Idioma.
  Alternativa descartada: duplicar los diez idiomas en el detalle; dos listas acabarían divergiendo.
- `SettingsApp.kt`: `SearchBar` con estado `rememberSaveable`, filtrado sobre el catálogo, filas de
  dos líneas agrupadas, selección con `primaryContainer` + check, y selección automática de la
  primera categoría solo cuando hay dos paneles (en compacto la lista sigue siendo la entrada, D9).
- `SettingsCategoryDetail.kt`: título grande + `IconButton` de cierre en lugar de `TopAppBar`,
  secciones con cabecera en `primary` y una `SettingsCardGroup` por sección; la categoría Idioma
  pinta directamente `LanguageOptionsColumn`.
- **D9. Primero seleccionado solo en dos paneles.** En teléfono, abrir directamente el detalle
  ocultaría la lista, que es el punto de entrada del patrón list-detail compacto (M3 adaptativo).
- **D10. El título del detalle no va en una barra.** Las capturas muestran un encabezado simple con
  X; `TopAppBar` añadiría una franja y una flecha redundantes con el botón del sistema.
- **D11. Los diálogos de tema siguen siendo modales.** Solo el selector de idioma se pide en el
  detalle; ampliar la enmienda a tema/backend sería un cambio de alcance no pedido.

## Estrategia de tests
- `SettingsDestinationTest` (JUnit 4 puro): valores conocidos, vacío y desconocido → `HUB`.
- `SettingsCategoryCatalogTest` (JUnit 4 puro): ocho categorías, destinos únicos y `matchesQuery`.
- Invariante de fuentes: todo acceso a ajustes llama a `launchSettings` y el manifest declara la
  actividad con `exported="false"` y sin `intent-filter`.
- Las pruebas puras existentes no se tocan; `MarkdownSurfacesInvariantsTest` lee rutas de archivo que
  no cambian.
- Verificación: `./gradlew assembleDebug` + pruebas puras; en dispositivo queda pendiente la
  comprobación visual de los dos anchos y del candado.
