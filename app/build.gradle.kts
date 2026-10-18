import java.util.Properties
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.gradle.api.tasks.testing.Test

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
	keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
  namespace = "com.example"
  buildToolsVersion = "36.1.0"
  compileSdk { version = release(37) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.estrin217.securenotes"
    minSdk = 33
    targetSdk = 36
    versionCode = 2
    versionName = "3.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      storeFile = keystoreProperties.getProperty("storeFile")?.let{ rootProject.file(it) } 
      storePassword = keystoreProperties.getProperty("storePassword")
      keyAlias = keystoreProperties.getProperty("keyAlias")
      keyPassword = keystoreProperties.getProperty("keyPassword")
    }
    
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("release")
      versionNameSuffix = "-Beta.${LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))}"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.withType<Test>().configureEach {
  jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

androidComponents {
  onVariants { variant ->
    val appName = "Secure-Notes"
    val buildType = variant.buildType ?: "unknown"
    val architecture = "arm64-v8a"
    val newFileName = "${appName}-${buildType}-${architecture}.apk"

    variant.outputs.forEach { output ->
      if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
        output.outputFileName.set(newFileName)
      }
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(project(":lib"))
  implementation(project(":visor-pdf"))
  // El catálogo de temas que la app y el editor de código comparten (spec 025). Tres módulos y una sola
  // dirección: `:app` -> `:editor-de-codigo` -> `:tema`.
  implementation(project(":tema"))
  implementation(project(":editor-de-codigo"))
  implementation(project(":visor-media"))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)
  implementation(libs.coil.network.okhttp)
  implementation(libs.coil.svg)
  // El motor de GFM de TODA superficie de Markdown de la app: spec 017 lo construyó para el chat, la 018
  // lo extió a la pantalla legal y a las dos de novedades, y la 019 a la vista previa del editor de código.
  // Vive en `:markdown` porque lo necesitan dos módulos y entre ellos la dependencia va **en un solo
  // sentido**. Sus seis piezas ya venian en el APK como dependencia `runtime` de compose-markdown, que la
  // 018 retira: se declaran en el módulo porque `runtime` no llega al classpath de compilacion.
  // Cero jars nuevos (RNF-48, RNF-53, RNF-57).
  implementation(project(":markdown"))
  implementation(libs.latex.base)
  implementation(libs.latex.parser)
  implementation(libs.latex.renderer)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.work.ktx)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.biometric.compose)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.play.services.auth)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.org.json)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
