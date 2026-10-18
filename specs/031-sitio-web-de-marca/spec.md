# Spec 031 — Sitio web estático de marca (Firebase Hosting)

Estado: **implementada**

## Contexto y objetivo

El proyecto no tiene presencia web: toda la información de marca vive en `README.md` (y en sus imágenes), pensada
para leerse dentro de GitHub. Se quiere una **página principal estática** que presente la marca de la app y que
pueda publicarse en Google Cloud sin servidor ni build: HTML y CSS planos servidos por **Firebase Hosting**.

El objetivo es que quien llega desde un enlace externo entienda en un pantallazo qué es Secure Notes, qué lo
diferencia y dónde descargarlo, sin depender de GitHub para renderizar el Markdown.

## Usuarios

- Quien recibe un enlace al sitio y quiere saber qué es la app antes de instalarla.
- Quien mantiene la marca y necesita un sitio cuyo contenido salga de una sola fuente (el repo).

## Historias de usuario

- **HU-1.** Como visitante, quiero entender la propuesta de valor y ver capturas reales, para decidir si la app
  me interesa sin abrir el repositorio.
- **HU-2.** Como visitante, quiero llegar a la release y al repositorio con un clic, para descargar la app.
- **HU-3.** Como mantenedor, quiero desplegar el sitio con `firebase deploy` sin build ni dependencias, para que
  publicar sea un solo comando.

## Requisitos funcionales

- **RF-201.** CUANDO el visitante abra la página principal, EL SISTEMA muestra el nombre de la marca, su lema y
  un resumen de la propuesta de valor (privacidad, editor, IA local) tomado de `README.md`.
- **RF-202.** CUANDO el visitante recorra la página, EL SISTEMA muestra las cuatro capturas alojadas en
  `public/assets/` dentro del propio sitio, sin cargarlas desde GitHub ni desde un CDN externo. Esas mismas
  imágenes son las que muestra el `README.md`: no hay una segunda copia que pueda quedar desfasada.
- **RF-203.** CUANDO el visitante pulse «Descargar» o «Ver en GitHub», EL SISTEMA abre la release y el repositorio
  (`https://github.com/ESTRIN217/secure-notes`) en una pestaña nueva.
- **RF-204.** CUANDO el visitante consulte la sección legal, EL SISTEMA resume términos, privacidad, IA y Drive
  con el mismo significado que la fuente legal de la app, y enlaza `SECURITY.md` para reportar vulnerabilidades.
- **RF-205.** CUANDO el visitante llegue a una ruta inexistente, EL SISTEMA sirve la página `404.html` del sitio
  con enlace de vuelta a la portada.
- **RF-206.** CUANDO el sitio se despliegue, EL SISTEMA publica únicamente el directorio `public/` (configuración en
  `firebase.json`) y no expone código ni artefactos del repositorio.

## Requisitos no funcionales

- **RNF-131.** El sitio es **estático**: HTML y CSS sin JavaScript, sin gestor de paquetes, sin paso de compilación
  y sin dependencias externas (ni fuentes, ni frameworks, ni CDN). Se abre con doble clic desde el disco.
- **RNF-132.** El contenido de marca sale de `README.md`: el sitio no introduce afirmaciones nuevas sobre el
  producto ni datos de contacto distintos de los que el repositorio ya publica.
- **RNF-133.** La página es legible y usable de 320 px a escritorio, con HTML semántico (`header`, `main`,
  `section`, `footer`), un solo `h1`, textos alternativos en todas las imágenes y contraste alto sobre fondo oscuro.
- **RNF-134.** El sitio no recoge datos del visitante: sin analítica, sin cookies, sin formularios y sin llamadas
  de red a terceros (coherente con el principio 5 de la constitución).

## Fuera de alcance

- Dominio propio o certificado (el proyecto de Hosting es `secure-notes-502213`, definido en `.firebaserc`).
- Blog, documentación navegable del editor o versiones en otros idiomas del sitio.
- Cualquier cambio en la app Android.

## Trazabilidad

| Requisito | Dónde se cumple |
|---|---|
| RF-201, RF-202, RF-204 | `public/index.html` (secciones hero, capturas, legal) |
| RF-203 | `public/index.html` (enlaces a release y repositorio) |
| RF-205 | `public/404.html` |
| RF-206 | `firebase.json` (`public: "site"`) |
| RNF-131…134 | `public/index.html` + `public/styles.css` (sin `<script>`, sin enlaces externos) |
