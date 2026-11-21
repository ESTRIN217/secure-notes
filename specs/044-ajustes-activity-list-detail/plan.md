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
- **D12. La categoría IA se resuelve como sub-pantalla (RF-334).** En vez de una página intermedia
  con el interruptor y una fila que navegaba a `AI_SETTINGS`, el destino `AI` se trata como
  sub-pantalla en `SettingsDetailPane` y pinta `AiSettingsScreen`. El destino `AI_SETTINGS` se
  conserva para accesos directos; el interruptor no se pierde porque ya vive en esa pantalla.
- **D13. Privacidad se resuelve igual que IA (RF-335).** Su categoría solo contenía una fila que
  navegaba a `PRIVACY_SETTINGS`, así que se trata también como sub-pantalla. El destino
  `PRIVACY_SETTINGS` sigue existiendo porque `MainList` lo usa desde `launchSettings`.
- **D14. Almacenamiento pasa a ser una sección de la lista (RF-336).** Desaparece la fila de
  categoría «Almacenamiento y datos» y su página con dos filas; en su lugar hay una cabecera de
  grupo con las dos entradas (Copia y Administrador), que ya son destinos de sub-pantalla. No hacen
  falta strings nuevos: se reutilizan las etiquetas y descripciones existentes.
- **D15. Legal se resuelve igual (RF-337).** Desaparece la fila categoría «Legal»; la nueva sección
  contiene Términos y privacidad y Licencias, que ya son destinos de sub-pantalla. Se reutilizan
  `settings_terms_privacy`, `settings_legal_desc`, `settings_licenses` y `about_licenses_desc`.
- **D16. Información pasa a sección de lista y el changelog a detalle (RF-338).** Se añade el
  destino `CHANGELOG` y se extrae el contenido de `ChangelogBottomSheet` a `ChangelogContent`, para
  que la hoja modal desaparezca y el mismo contenido se pinte en el detalle. El motor de Markdown
  sigue proveyéndose una sola vez por encima de la lista de releases.
- **D17. Índice de búsqueda estático por destino (RF-339).** No se intenta leer el árbol de
  composición de las pantallas (no es fiable ni barato); se mantiene un mapa puro de recursos por
  destino con los títulos de los ajustes interiores. Se reutilizan strings existentes: cero claves
  nuevas. Alternativa descartada: generar el índice desde el contenido renderizado.
- **D18. Sin autoscroll al resultado.** Buscar abre la pantalla correspondiente; resaltar la fila
  concreta dentro del detalle exigiría pasar el término como estado del scaffold y está fuera de
  esta enmienda.
- **D19. Una sola barra en móvil (RF-340/RF-341).** `MediumTopAppBar` con
  `exitUntilCollapsedScrollBehavior` en el `Scaffold` global de `SettingsApp`, conectada por
  `nestedScroll` al contenido, y botón back que reutiliza `goBack()`. Las sub-pantallas conservan su
  `Scaffold` solo para insets y snackbar, con `contentWindowInsets = WindowInsets(0)` y sin `topBar`,
  `BackHandler` ni `onBack`. En tablet/PC no hay barra global: el `SettingsDetailHeader` (título + X)
  se aplica a todas las pantallas del detalle.
- **D20. La búsqueda se limpia sola y no lleva título encima (RF-342).** `OutlinedTextField` con
  `trailingIcon` condicional y `clear_search` (ya traducido) como nombre accesible; se elimina el
  parámetro `showTitle` y el `Text` superior de la lista.
- **D21. Altura acotada para que haya scroll (RF-343).** El `Scaffold` global y el `Box` de su
  contenido llevan `fillMaxSize`, y el `LazyColumn` de la lista lleva `weight(1f)` dentro de su
  `Column`. Sin esos límites, Compose mide la lista con altura infinita y el gesto no desplaza.
- **D22. Chevron solo en móvil (RF-344).** `SettingsCategoryList(showChevron = !hasTwoPanes)` pasa
  el flags a cada fila, que cambia el check por `Icons.Default.ChevronRight`. El parámetro
  `selected` de `ListItem` mantiene la semántica accesible en tablet, aunque no haya marca visual.
- **D24. Atajo directo vía activity-alias (RF-352).** Los atajos estáticos los lanza el launcher (otra app),
  que no puede iniciar una Activity no exportada. En vez de exportar `SettingsActivity` (relajaría RNF-206),
  se declara un `<activity-alias>` exportado `SettingsShortcutAlias` con `targetActivity=.SettingsActivity` y
  el atajo de ajustes apunta a él. `SettingsActivity` sigue `exported=false` con su candado. Se mantiene
  `FLAG_ACTIVITY_SINGLE_TOP` en `launchSettings` (el usuario eligió conservarlo) y se quita el destino
  `SETTINGS` del enum `ShortcutDestination` y del `when` en `ShortcutDestinations.kt`, actualizando los
  invariantes de atajos (4 en el enum, 5 en el XML) y añadiendo uno para el alias.
- **D23. Barra sin Scaffold global (RF-345).** Se descarta el `Scaffold` envolvente (doble consumo
  de insets con el scaffold adaptativo y sus propios Scaffolds de sub-pantalla). La barra pasa a ser
  el primer hijo de un `Column(fillMaxSize)` y el `NavigableListDetailPaneScaffold` vive en un
  `Box(weight(1f).consumeWindowInsets(systemBars))`. El `nestedScroll` se pasa hasta el `LazyColumn`
  real, y los `AnimatedPane` llevan `fillMaxSize()` para que midan con altura finita.}

## Investigación del scroll (2026-11-21)
Se descartó que la altura acotada (RF-343) bastara. La causa probable es estructural: el patrón
oficial de Material 3 aplica `nestedScroll` al `Scaffold` y da `innerPadding` al `LazyColumn`;
aquí se aplicaba a un `Box` que envuelve `NavigableListDetailPaneScaffold`, cuyos `AnimatedPane`
gestionan sus propios insets y gestos. Además faltaba `consumeWindowInsets`. D23 corrige ambas
cosas. Plan B si el gesto sigue fallando en dispositivo: sustituir temporalmente `AnimatedPane` del
`listPane` por el `LazyColumn` directo para aislar si el culpable es el pane adaptativo.

**RF-346**: con la barra ya reestructurada, el dispositivo confirmó que el **detail sí scrollea**
pero el **list no**. Única diferencia: el `LazyColumn` del list llevaba el `nestedScroll` connection
directo y el detail no. El connection consume el `pre-scroll` para contraer la barra y se quedaba
el gesto. Se mueve `.nestedScroll(scrollBehavior.nestedScrollConnection)` al `Box` contenedor
común (patrón oficial M3) y se quita el parámetro `scrollConnection` de `SettingsCategoryList`.

## Estrategia de tests
- `SettingsDestinationTest` (JUnit 4 puro): valores conocidos, vacío y desconocido → `HUB`.
- `SettingsCategoryCatalogTest` (JUnit 4 puro): ocho categorías, destinos únicos y `matchesQuery`.
- Invariante de fuentes: todo acceso a ajustes llama a `launchSettings` y el manifest declara la
  actividad con `exported="false"` y sin `intent-filter`.
- Las pruebas puras existentes no se tocan; `MarkdownSurfacesInvariantsTest` lee rutas de archivo que
  no cambian.
- Verificación: `./gradlew assembleDebug` + pruebas puras; en dispositivo queda pendiente la
  comprobación visual de los dos anchos y del candado.
