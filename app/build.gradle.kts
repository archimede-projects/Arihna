import java.net.URI
import java.util.zip.ZipInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val quranSourceCommit = "a5284b17034d36567e4a4bac982a17ba56837448"
val quranSvgCommit = "78d97544bfdc57e9f04bc97ace3f857ed972d772"
val quranWarshSvgCommit = "b91d39e1065b57bdda3e94aca8ecf3575e50e1e6"
val quranMetadataCommit = "052b515f3a24dfacbe4cafc3b89f0681a447f462"
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

        // Pinned Tanzil metadata supplies canonical Page/Juz/Hizb start coordinates.
        val metadataTarget = quranDir.resolve("quran-data.js")
        URI(
            "https://raw.githubusercontent.com/acfatah/tanzil/$quranMetadataCommit/data/quran-data.js",
        ).toURL().openStream().use { input ->
            metadataTarget.outputStream().use { output -> input.copyTo(output) }
        }
        check(metadataTarget.isFile && metadataTarget.length() > 0L) { "Missing pinned Quran page metadata" }

        // Visual Muṣḥaf pages only. The immutable Tarteel/Tanzil corpus above remains
        // Arihna's textual Quran source; these pinned MIT SVGs reproduce a printed page.
        val mushafDir = root.resolve("mushaf")
        if (mushafDir.exists()) mushafDir.deleteRecursively()
        mushafDir.mkdirs()
        val surahTarget = root.resolve("mushaf-surah.json")
        val licenseTarget = root.resolve("QURAN_SVG_LICENSE.txt")
        surahTarget.delete()
        licenseTarget.delete()

        val archiveUrl = URI(
            "https://codeload.github.com/batoulapps/quran-svg/zip/$quranSvgCommit",
        ).toURL()
        ZipInputStream(archiveUrl.openStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    when {
                        Regex(".*/svg/[0-9]{3}\\.svg").matches(name) -> {
                            val target = mushafDir.resolve(name.substringAfterLast('/'))
                            target.outputStream().buffered().use { output -> zip.copyTo(output) }
                        }
                        name.endsWith("/surah.json") -> {
                            surahTarget.outputStream().buffered().use { output -> zip.copyTo(output) }
                        }
                        name.endsWith("/LICENSE") -> {
                            licenseTarget.outputStream().buffered().use { output -> zip.copyTo(output) }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val mushafPages = mushafDir.listFiles { file ->
            file.isFile && Regex("[0-9]{3}\\.svg").matches(file.name)
        }?.size ?: 0
        check(mushafPages == 604) { "Expected 604 pinned Mushaf SVG pages, found $mushafPages" }
        check(surahTarget.isFile && surahTarget.length() > 0L) { "Missing pinned Mushaf surah metadata" }
        check(licenseTarget.isFile && licenseTarget.length() > 0L) { "Missing pinned Mushaf MIT license" }


        // Warsh ʿan Nāfiʿ: Quranpedia metadata is CC0; the page artwork is the
        // King Fahd Complex digital Muṣḥaf and is NOT covered by the Hafs MIT licence.
        val warshDir = root.resolve("mushaf-warsh")
        if (warshDir.exists()) warshDir.deleteRecursively()
        warshDir.mkdirs()
        val warshSurahTarget = root.resolve("mushaf-warsh-surah.json")
        val warshNoticeTarget = root.resolve("WARSH_KFQC_NOTICE.md")
        warshSurahTarget.delete()
        warshNoticeTarget.delete()
        val warshCheckout = layout.buildDirectory.dir("tmp/quranpedia-warsh").get().asFile
        if (warshCheckout.exists()) warshCheckout.deleteRecursively()

        fun git(vararg args: String) {
            val command = mutableListOf("git")
            command.addAll(args)
            val process = ProcessBuilder(command).inheritIO().start()
            check(process.waitFor() == 0) { "git command failed: ${command.joinToString(" ")}" }
        }
        git(
            "clone", "--filter=blob:none", "--no-checkout",
            "https://github.com/quranpedia/quran-svg.git", warshCheckout.absolutePath,
        )
        git("-C", warshCheckout.absolutePath, "sparse-checkout", "init", "--no-cone")
        git(
            "-C", warshCheckout.absolutePath, "sparse-checkout", "set", "--no-cone",
            "mushafs/warsh/kfqc/svg/[0-9][0-9][0-9].svg",
            "mushafs/warsh/kfqc/json/surah.json",
            "NOTICE.md",
        )
        git("-C", warshCheckout.absolutePath, "fetch", "--depth=1", "origin", quranWarshSvgCommit)
        git("-C", warshCheckout.absolutePath, "checkout", "--detach", quranWarshSvgCommit)

        val warshSvgSource = warshCheckout.resolve("mushafs/warsh/kfqc/svg")
        warshSvgSource.listFiles { file -> file.isFile && Regex("[0-9]{3}\\.svg").matches(file.name) }
            .orEmpty()
            .forEach { file -> file.copyTo(warshDir.resolve(file.name), overwrite = true) }
        warshCheckout.resolve("mushafs/warsh/kfqc/json/surah.json")
            .copyTo(warshSurahTarget, overwrite = true)
        warshCheckout.resolve("NOTICE.md").copyTo(warshNoticeTarget, overwrite = true)
        warshCheckout.deleteRecursively()

        val warshPages = warshDir.listFiles { file ->
            file.isFile && Regex("[0-9]{3}\\.svg").matches(file.name)
        }?.size ?: 0
        check(warshPages == 604) { "Expected 604 pinned Warsh SVG pages, found $warshPages" }
        val warshSurahRaw = warshSurahTarget.readText(Charsets.UTF_8)
        check(Regex("\\\"number\\\"\\s*:").findAll(warshSurahRaw).count() == 114) {
            "Expected 114 Warsh surahs"
        }
        val warshAyahCount = Regex("\\\"ayahCount\\\"\\s*:\\s*(\\d+)")
            .findAll(warshSurahRaw).sumOf { it.groupValues[1].toInt() }
        check(warshAyahCount == 6214) { "Expected 6214 Warsh ayat, found $warshAyahCount" }
        val notice = warshNoticeTarget.readText(Charsets.UTF_8)
        check("King Fahd" in notice && "digital publishing" in notice && "CC0 1.0" in notice) {
            "Warsh KFQC/Quranpedia usage notice is incomplete"
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
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("com.caverock:androidsvg:1.4")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
