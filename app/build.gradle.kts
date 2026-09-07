import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val quranSourceCommit = "a5284b17034d36567e4a4bac982a17ba56837448"
val generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets").get().asFile
val prepareQuranAssets by tasks.registering {
    outputs.dir(generatedQuranAssets)
    doLast {
        val root = generatedQuranAssets
        val quranDir = root.resolve("quran").apply { mkdirs() }
        val files = mapOf(
            "quran-uthmani.txt" to "text/quran-uthmani.txt",
            "juz-info.json" to "metadata/juz-info.json",
            "hizb-info.json" to "metadata/hizb-info.json",
            "TANZIL_TEXT_README.md" to "text/README.md",
        )
        files.forEach { (name, remotePath) ->
            val target = quranDir.resolve(name)
            val url = project.uri(
                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",
            ).toURL()
            url.openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            check(target.isFile && target.length() > 0L) { "Missing Quran asset: $name" }
        }
    }
}

android {
    namespace = "com.archimedeprojects.arihna"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.archimedeprojects.arihna"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-bootstrap"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("main").assets.srcDir(generatedQuranAssets)
}

tasks.matching { task ->
    task.name.startsWith("merge") && task.name.endsWith("Assets")
}.configureEach {
    dependsOn(prepareQuranAssets)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.adhan)
    implementation(libs.play.services.location)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
