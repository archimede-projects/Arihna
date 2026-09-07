from pathlib import Path

p = Path('app/build.gradle.kts')
text = p.read_text(encoding='utf-8')
needle = '''android {\n    namespace = "com.archimedeprojects.arihna"\n    compileSdk = 37\n'''
if text.count(needle) != 1:
    raise SystemExit('android block anchor not unique')
text = text.replace(needle, '''val quranSourceCommit = "a5284b17034d36567e4a4bac982a17ba56837448"\nval generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets")\nval prepareQuranAssets by tasks.registering {\n    outputs.dir(generatedQuranAssets)\n    doLast {\n        val root = generatedQuranAssets.get().asFile\n        val quranDir = root.resolve("quran").apply { mkdirs() }\n        val files = mapOf(\n            "quran-uthmani.txt" to "text/quran-uthmani.txt",\n            "juz-info.json" to "metadata/juz-info.json",\n            "hizb-info.json" to "metadata/hizb-info.json",\n            "TANZIL_TEXT_README.md" to "text/README.md",\n        )\n        files.forEach { (name, remotePath) ->\n            val target = quranDir.resolve(name)\n            val url = java.net.URI(\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\n            ).toURL()\n            url.openStream().use { input ->\n                target.outputStream().use { output -> input.copyTo(output) }\n            }\n            check(target.isFile && target.length() > 0L) { "Missing Quran asset: $name" }\n        }\n    }\n}\n\nandroid {\n    namespace = "com.archimedeprojects.arihna"\n    compileSdk = 37\n''', 1)
anchor = '''    compileOptions {\n        sourceCompatibility = JavaVersion.VERSION_17\n        targetCompatibility = JavaVersion.VERSION_17\n    }\n}'''
if text.count(anchor) != 1:
    raise SystemExit('android close anchor not unique')
text = text.replace(anchor, '''    compileOptions {\n        sourceCompatibility = JavaVersion.VERSION_17\n        targetCompatibility = JavaVersion.VERSION_17\n    }\n\n    sourceSets.getByName("main").assets.srcDir(generatedQuranAssets)\n}\n\ntasks.matching { task ->\n    task.name.startsWith("merge") && task.name.endsWith("Assets")\n}.configureEach {\n    dependsOn(prepareQuranAssets)\n}''', 1)
p.write_text(text, encoding='utf-8')

Path('docs/quran').mkdir(parents=True, exist_ok=True)
Path('docs/quran/SOURCES.md').write_text('''# Quran source provenance\n\nArihna bundles the Quran reader assets at build time from the immutable public source `TarteelAI/quran-assets@a5284b17034d36567e4a4bac982a17ba56837448`.\n\n- `text/quran-uthmani.txt`: Tanzil Quran Text Uthmani v1.1, copied verbatim.\n- `metadata/juz-info.json`: Juz boundaries.\n- `metadata/hizb-info.json`: Hizb boundaries.\n- `text/README.md`: Tanzil attribution and CC BY 3.0 notice, bundled as `TANZIL_TEXT_README.md`.\n\nThe exact-SHA gate independently downloads the same pinned files and compares them byte-for-byte with the assets merged into the APK build inputs. Runtime Quran reading is fully offline.\n''', encoding='utf-8')
print('Pinned Quran build assets configured')
