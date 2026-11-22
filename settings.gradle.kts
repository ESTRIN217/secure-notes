pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "secure-notes"

include(":app")
include(":visor-pdf")
include(":editor-de-codigo")
include(":visor-media")
// El motor de GFM que pintan las superficies de Markdown de la app. Existe como módulo porque lo necesitan
// `:app` y `:editor-de-codigo`, y entre ellos la dependencia va **en un solo sentido** (spec 019).
include(":markdown")
// El catálogo de temas (paletas de resaltado y esquemas de color) que comparten `:app` y `:editor-de-codigo`
// (spec 025). Existe por la misma razón que `:markdown`: la dependencia entre los dos va en un solo sentido, y
// un catálogo queviviera en `ui/theme/` de `:app` dejaría al editor sin él.
include(":tema")

// El binding de llama.cpp del chat on-device, vendorizado en el repo con las modificaciones
// de spec 039 (contadores de métricas y nkvPosition) — see lib/. El **motor** (los fuentes de
// llama.cpp que el binding compila con add_subdirectory) sigue externo en `/root/llama.cpp`;
// ver `lib/src/main/cpp/CMakeLists.txt` y AGENTS.md para el porqué.
//
// Solo se configura para tareas full o sin flavor explícito. Las tareas independent-only
// (CI: testIndependentDebugUnitTest, assembleIndependentDebug, etc.) no lo necesitan y así
// evitamos que AGP exija NDK/CMake en entornos sin ese SDK.
val requested = gradle.startParameter.taskNames
val independentOnly = requested.isNotEmpty() &&
    requested.any { it.contains("independent", ignoreCase = true) } &&
    requested.none { it.contains("full", ignoreCase = true) }

if (!independentOnly) {
    include(":lib")
}
