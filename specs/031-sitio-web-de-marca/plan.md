# Plan — Spec 031 (sitio web estático de marca)

## Enfoque

Un directorio `public/` autocontenido: es la raíz que publica Firebase Hosting y también lo que se abre con doble
clic desde el disco. Sin build: es el mismo HTML en los dos casos.

## Decisiones

- **`public/` como raíz publicada, no el repo entero.** `public: "."` obligaría a mantener una lista de
  exclusiones que crece con cada módulo nuevo y expone `app/`, `specs/` y el build. Un directorio propio hace
  que lo publicado sea exactamente lo que se ve. Es además el directorio que crea `firebase init hosting`, así que
  no hay dos raíces candidatas que reconciliar.
- **Una sola copia de cada imagen, en `public/assets/`.** Las capturas y el banner están allí y el `README.md`
  apunta a esa misma ruta. Antes esas imágenes eran copia de una carpeta `assets/` en la raíz: el mismo PNG en dos
  sitios obliga a recordar cuál manda, y el README puede acabar mostrando una versión distinta de la que se
  acaba de publicar. La carpeta raíz ya no existe.
- **Sin reescritura `** → `/index.html`.** La que pone `firebase init` es de SPA: haría que *toda* ruta
  inexistente devolviera la portada con HTTP 200 y `404.html` no se serviría nunca (RF-205). Aquí no hay SPA.
- **CSS en un solo fichero, sin framework.** El sitio son ~700 líneas de HTML: una dependencia de CSS no
  aporta nada y RNF-131 la prohíbe.
- **Sin JavaScript.** No hay nada dinámico que mostrar; el año del pie va escrito.
- **Paleta tomada de `public/assets/banner.png`** (índigo/violeta sobre `#0b0d14` y oro `#EDC22E` del icono), para
  que la página y el banner parezcan la misma marca.

## Ficheros

| Fichero | Propósito |
|---|---|
| `public/index.html` | Página principal (hero, tres pilares, capturas, características, idiomas, legal, descarga, pie). |
| `public/styles.css` | Todo el estilo, tokens en `:root`, responsive desde 320 px. |
| `public/404.html` | Página de ruta inexistente (RF-205). |
| `public/robots.txt` | Permitir indexación. |
| `public/favicon.svg` | Candado oro sobre cuadrado redondeado, sin PNG. |
| `public/assets/*.png` | Banner y cuatro capturas: **la única copia**, también es lo que muestra el `README.md`. |
| `firebase.json` | Hosting: `public: "public"`, `cleanUrls`, caché por tipo de fichero, sin reescritura SPA. |
| `.firebaserc` | Id de proyecto: `secure-notes-502213`. |
| `.gitignore` | Añade `.firebase/` y `firebase-debug.log`. |

## Riesgos

- **Proyecto de Firebase**: `.firebaserc` apunta a `secure-notes-502213`; si la cuenta que despliega no tiene
  acceso a ese proyecto, `firebase deploy` fallará al resolverlo.
- **Divergencia con el README**: si el README cambia y el sitio no, la marca dice dos cosas distintas. Mitigado
  dejando `README.md` como fuente citada en `spec.md` (RNF-132).
