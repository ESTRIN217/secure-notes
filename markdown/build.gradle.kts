plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.estrin217.markdown"
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
  // El motor de GFM que pintan las superficies de Markdown de la app: chat con IA, texto legal, changelog,
  // pantalla de actualización y vista previa del editor de código (specs 017, 018 y 019). Vive aquí porque lo
  // necesitan dos módulos y la dependencia entre ellos va **en un solo sentido**.
  //
  // Las seis piezas ya estaban en el APK como dependencia `runtime` de `compose-markdown`, que la spec 018
  // retiró; se declaran explícitas porque `runtime` no llega al classpath de compilación (spec 017, RNF-48).
  //
  // **Cero artefactos nuevos** (RNF-57): todo esto ya estaba dentro. Y en concreto **no** hay ningún cargador
  // de imágenes de Coil ni ninguna librería de red: el cargador de la vista previa decodifica con `BitmapFactory`
  // y no tiene forma de pedir nada por la red (ver `LocalFileImageLoader`).
  implementation(libs.markwon.core)
  implementation(libs.markwon.ext.tables)
  implementation(libs.markwon.ext.strikethrough)
  implementation(libs.markwon.ext.tasklist)
  implementation(libs.markwon.html)
  implementation(libs.markwon.linkify)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.org.json)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}