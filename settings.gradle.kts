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

val llamaLibDir = file("/root/llama.cpp/examples/llama.android/lib")
if (llamaLibDir.exists()) {
    include(":lib")
    project(":lib").projectDir = llamaLibDir
}
