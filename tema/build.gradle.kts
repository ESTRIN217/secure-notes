plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.estrin217.tema"
  compileSdk { version = release(37) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 33
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  // El catálogo de temas de la app y del editor de código (spec 025). Solo necesita los tipos de color y el
  // esquema de Material 3, que ya estaban en el APK: **cero artefactos nuevos** (RNF-99).
  //
  // No lleva `androidx.compose.runtime` declarado aparte porque el propio BOM lo trae como dependencia transitiva
  // de `material3`, y aquí no hay nada `runtime` sin declarar que alguien intente importar.
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)

  // Pruebas **puras**, sin Robolectric: por eso el JUnit 4 se declara aquí en vez de llegar de forma transitiva
  // desde el `robolectric` que declaran los otros módulos. Y sin él sí corren en este entorno.
  testImplementation(libs.junit4)
}