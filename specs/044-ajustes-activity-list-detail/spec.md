# Spec 044 — Ajustes como Activity con lista y detalle adaptativa
Estado: **implementada y verificada en dispositivo** (2026-11-22)

## Contexto y objetivo
Los ajustes viven hoy como un `Screen` más del `Navigator` de `MainActivity`, con ocho sub-pantallas
también en ese `Navigator`. En tablet y PC la interfaz desperdicia el ancho: no hay una lista de
categorías persistente junto al contenido. Además, los ajustes están acoplados al ciclo de vida de
`MainActivity`, así que no pueden abrirse como una superficie independiente.

Esta spec saca todo el árbol de ajustes a una `Activity` propia y organiza el hub con un scaffold
lista/detalle: en pantallas grandes la lista de categorías queda siempre visible y el detalle cambia
a su lado; en teléfono el comportamiento sigue siendo de una sola pantalla con atrás.

## Usuarios
- Usuario de teléfono que quiere los mismos ajustes de siempre, ahora en una Activity propia.
- Usuario de tablet o PC que quiere ver las categorías de ajustes y el contenido de la seleccionada
  a la vez, sin tener que volver atrás para cambiar de categoría.
- Usuario que abre ajustes desde el rail, un atajo, la lista de notas o una notificación de
  actualización, y espera llegar directamente al destino pedido.

## Historias de usuario
- HU-1. Como usuario, quiero abrir ajustes desde cualquier acceso de la app y llegar a la sección
  que pedí, para no tener que navegar desde el hub.
- HU-2. Como usuario de tablet, quiero ver la lista de categorías a la izquierda y el detalle a la
  derecha, para cambiar de sección sin perder el sitio.
- HU-3. Como usuario de teléfono, quiero que al elegir una categoría se me muestre su detalle y que
  atrás me devuelva a la lista, para no quedar atrapado.
- HU-4. Como usuario con contraseña maestra, quiero que los ajustes sigan detrás del candado, para
  que copia y privacidad no se puedan ver con la bóveda cerrada.

## Definiciones
- **Categoría**: una de las ocho secciones del hub: Apariencia, Idioma, Privacidad y seguridad,
  Almacenamiento y datos, IA, Herramientas, Legal, Información.
- **Sub-pantalla**: una pantalla de ajustes con `Scaffold` y barra superior propios (Privacidad,
  Copia, Almacenamiento, Ajustes de IA, Legal, Licencias, Actualización, Acerca de).
- **Destino**: valor que viaja en el `Intent` y dice qué categoría o sub-pantalla se abre primero.
- **Panel de detalle**: contenido seleccionado del scaffold; puede ser el grupo de filas de una
  categoría o una sub-pantalla completa.

## Requisitos funcionales
- RF-316: CUANDO el usuario pulsa cualquier acceso a ajustes, EL SISTEMA abre la Activity de
  ajustes con el destino correspondiente, sin pasar por un `Screen` del `Navigator`.
- RF-317: MIENTRAS el ancho disponible es 600 dp o más, EL SISTEMA mantiene visibles la lista de
  categorías y el panel de detalle a la vez.
- RF-318: CUANDO el ancho disponible es menor de 600 dp, EL SISTEMA muestra un solo panel; al
  seleccionar una categoría muestra su detalle y al pulsar atrás vuelve a la lista.
- RF-319: CUANDO se selecciona una categoría cuyo contenido son filas en línea, EL SISTEMA muestra
  esas filas en el panel de detalle con un título y una flecha de vuelta.
- RF-320: CUANDO una fila abre una sub-pantalla, EL SISTEMA la muestra en el panel de detalle con su
  propia barra superior, sin añadir una segunda barra por encima.
- RF-321: SI existe contraseña maestra y la bóveda está bloqueada, ENTONCES EL SISTEMA muestra la
  pantalla de bloqueo antes de cualquier contenido de ajustes.
- RF-322: CUANDO se pulsa atrás en el panel de detalle, EL SISTEMA retrocede en la pila del scaffold;
  si no hay más pila, cierra la Activity.
- RF-323: EL SISTEMA tiene un único punto de entrada para abrir ajustes, usado por el rail, el atajo,
  los enlaces de la lista de notas y el Intent de actualización.
- RF-324: CUANDO un Intent trae el extra de apertura de actualización, EL SISTEMA abre ajustes en el
  destino de información/actualización.
- RF-325: CUANDO la Activity se crea con un destino inicial, EL SISTEMA selecciona ese destino en el
  scaffold en lugar de empezar por el hub.

## Requisitos no funcionales
- RNF-206: La Activity de ajustes no está exportada y no declara filtros de intents.
- RNF-207: Las dependencias de layout y navegación adaptativas se declaran con la versión que fija
  el Compose BOM y se añaden a la lista de licencias del APK.
- RNF-208: Los factories de ViewModels se comparten entre `MainActivity` y la Activity de ajustes;
  el motor de IA sigue materializándose solo a través de `AiModelHost`.
- RNF-209: Con el ajuste «bloquear al salir», al cruzar a la Activity de ajustes se pide de nuevo la
  contraseña: la Activity no recibe el estado desbloqueado de `MainActivity`.
- RNF-210: La traducción del destino del `Intent` es una función pura y se prueba sin Robolectric.

## Casos límite
- El destino del `Intent` es desconocido o está vacío: se abre el hub.
- La Activity se recrea tras girar: el destino y la pila del scaffold sobreviven al giro.
- El proceso muere con la Activity en primer plano: al reabrir, la bóveda está bloqueada y se pide
  contraseña.
- Hay una sub-pantalla abierta en tablet y se pulsa atrás: se vuelve a la categoría, no se cierra la
  Activity mientras la lista siga visible.
- Ya hay una Activity de ajustes abierta y llega otro acceso: se reutiliza con el destino nuevo.

## Fuera de alcance
- Cambiar el contenido, el aspecto o las acciones de las ocho sub-pantallas.
- Añadir categorías nuevas o reordenar las ocho existentes.
- Cambiar el sistema de bloqueo o el temporizador de bloqueo automático.
- Llevar el editor de código, el visor de PDF o el chat a esta Activity.

## Criterios de finalización
- Todos los accesos a ajustes abren la Activity con el destino correcto.
- En 600 dp o más, lista y detalle son visibles simultáneamente; por debajo, atrás vuelve a la lista.
- Con contraseña y bóveda cerrada, la Activity muestra el bloqueo.
- `assembleDebug` y las pruebas puras pasan; ✅ verificación en dispositivo completada (2026-11-22).

## Enmienda (2026-11-20, RF-326…RF-333 / RNF-211…RNF-212) — interfaz de la Activity
Tras ver la aplicación en dispositivo, la Activity cumple su función pero su interfaz no sigue los
patrones de las referencias (Chrome, Android Studio, Metrolist): falta búsqueda, agrupación, detalle
con título grande y contenido del selector de idioma en el propio detalle. Se enmienda la spec sin
cambiar el alcance, el candado ni los accesos.

- RF-326: EL SISTEMA muestra un campo de búsqueda en el panel izquierdo que filtra **solo las ocho
  categorías** por título y subtítulo, sin abrir un modal.
- RF-327: CUANDO la búsqueda no tiene coincidencias, EL SISTEMA muestra un mensaje de estado vacío
  en lugar de una lista en blanco.
- RF-328: EL SISTEMA muestra las categorías agrupadas bajo cabeceras de sección, con icono, título y
  subtítulo en cada fila, y resalta la seleccionada con color **y** una marca que no depende del color.
- RF-329: CUANDO se abre ajustes sin un destino explícito, EL SISTEMA selecciona la primera
  categoría: en dos paneles muestra su detalle y en teléfono la deja resaltada en la lista.
- RF-330: EL SISTEMA muestra el detalle de una categoría con un título grande y un botón de cierre,
  sin una barra superior con flecha; el botón del sistema sigue retrocediendo.
- RF-331: EL SISTEMA organiza el detalle en secciones con cabeceras en color de acento y tarjetas
  separadas, con título, subtítulo y control en cada fila.
- RF-332: CUANDO se selecciona la categoría Idioma, EL SISTEMA muestra en el panel de detalle las
  opciones del selector de idioma (el contenido del bottom sheet), sin abrir un modal.
- RF-333: CUANDO se elige un idioma desde el detalle, EL SISTEMA aplica el cambio y recrea la
  Activity, igual que lo hacía el bottom sheet.
- RF-334: CUANDO se selecciona la categoría IA, EL SISTEMA muestra `AiSettingsScreen` directamente
  en el panel de detalle, sin una página intermedia con el interruptor; el listado conserva la
  categoría y su estado seleccionado.
- RF-335: CUANDO se selecciona la categoría Privacidad y seguridad, EL SISTEMA muestra
  `PrivacySettingsScreen` directamente en el panel de detalle, sin una página intermedia con una
  sola fila; el listado conserva la categoría y su estado seleccionado.
- RF-336: EL SISTEMA muestra en la lista una sección «Almacenamiento y datos» con dos entradas —
  Copia de seguridad y restauración y Administrador de almacenamiento— que abren sus pantallas
  directamente en el panel de detalle, sin la categoría ni la sección intermedia anteriores.
- RF-337: EL SISTEMA muestra en la lista una sección «Legal» con dos entradas —Términos y privacidad
  y Licencias de código abierto— que abren sus pantallas directamente en el panel de detalle, sin
  la categoría ni la sección intermedia anteriores.
- RF-338: EL SISTEMA muestra en la lista una sección «Aplicación e información» con tres entradas —
  Buscar actualizaciones, Registro de cambios y Acerca de— que abren sus pantallas directamente en
  el panel de detalle, sin la categoría ni la sección intermedia anteriores. El registro de cambios
  deja de ser una hoja modal y se convierte en contenido del detalle.
- RF-339: EL SISTEMA incluye en la búsqueda de la lista los términos del contenido de las pantallas
  de detalle, además del título y subtítulo de cada entrada; buscar «backend» encuentra Ajustes de
  IA y buscar «contraseña» encuentra Privacidad y seguridad.
- RF-340: MIENTRAS el ancho es menor de 600 dp, EL SISTEMA muestra una única `MediumTopAppBar` en
  ajustes con botón de vuelta y un título que pasa de grande a mediano al desplazarse la lista; en
  600 dp o más no muestra esa barra.
- RF-341: EL SISTEMA elimina la barra superior y el botón de vuelta propios de las pantallas de
  detalle; en móvil la vuelta la da la barra global y en tablet/PC el encabezado del detalle con X.
- RF-342: EL SISTEMA muestra una acción X en la barra de búsqueda de ajustes cuando hay texto
  escrito, que lo borra de una sola pulsación, y no muestra ningún título por encima de la barra.
- RF-343: EL SISTEMA mantiene el desplazamiento de la lista de ajustes y del panel de detalle tras
  introducir la barra global, dando altura acotada al contenido del `Scaffold`.
- RF-344: EL SISTEMA muestra un chevron al final de cada fila de la lista solo cuando el ancho es
  menor de 600 dp, en lugar de la marca de check; en tablet/PC la fila no lleva icono final porque
  el detalle ya está visible. El estado seleccionado se sigue comunicando por semántica accesible.
- RF-345: EL SISTEMA coloca la barra superior de ajustes como primer hijo de un `Column` raíz, con
  el scaffold adaptativo en el espacio restante, y conecta el desplazamiento directamente a la
  lista scrolleable, de modo que la lista se desplace y la barra colapse sin interferencias.
- RF-346: EL SISTEMA conecta el desplazamiento de la barra superior al contenedor común de los
  paneles, no al `LazyColumn` del listado, para que la lista scrollee y la barra colapse a la vez.
- RF-347: EL SISTEMA cambia el estilo del título de la barra de grande a pequeño cuando la lista se
  desplaza, observando el desplazamiento en solo lectura, sin consumir el gesto de scroll.
- RF-348: EL SISTEMA aplica el padding de la barra de estado al contenido de ajustes de modo que la
  barra de búsqueda no quede tapada: con dos paneles se aplica el padding completo del sistema y con
  un solo panel solo los laterales e inferiores, porque la barra superior ya cubre la parte superior.
- RF-349: EL SISTEMA contrae y expande físicamente la altura de la barra superior a medida que se
  desplaza la lista, y desvanece el título cuando el contenido pasa por debajo, igual que el patrón
  de la referencia, sin consumir el gesto de scroll.
- RF-350: EL SISTEMA contrae la barra de forma estable al desplazarse hacia abajo y la expande solo
  al volver arriba (histéresis), sin vaivén, y el título pasa de grande a pequeño y se desvanece con
  el desplazamiento, sin consumir el gesto de scroll.
- RF-351: CUANDO se abre ajustes sin un destino explícito, EL SISTEMA resalta la primera categoría
  solo si hay dos paneles; con un solo panel ninguna fila queda activa.
- RF-352: EL SISTEMA abre ajustes desde el atajo del lanzador directamente en su Activity, sin pasar
  por `MainActivity`; para ello usa un `activity-alias` exportado que apunta a la Activity de ajustes,
  que sigue sin exportar. El destino `SETTINGS` desaparece del enrutado de atajos de la app.
- RNF-211: Todo texto nuevo visible sale de `strings.xml` en los nueve locales.
- RNF-212: Las filas tienen un objetivo táctil de al menos 48 dp, nombre accesible, foco visible y
  textos que escalan sin recortarse; la selección no se comunica solo con color.

## Dudas abiertas
Ninguna: el alcance, el candado, la granularidad de la lista, los accesos, la creación de la spec y
los detalles de la enmienda de interfaz fueron confirmados por el usuario.
