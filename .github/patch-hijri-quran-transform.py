from pathlib import Path

# Fix the Home transform ambiguity: the route signature is the first of two
# intentionally similar callback signatures in the baseline source.
p = Path('/tmp/revision.py')
text = p.read_text(encoding='utf-8')
old = "text = replace_once(text, '    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 'Home route signature Quran')"
new = "text = text.replace('    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 1)"
if text.count(old) != 1:
    raise SystemExit(f'expected one Home transform source target, found {text.count(old)}')
text = text.replace(old, new, 1)

# Avoid JVM setter collision: Kotlin property `language` already generates a
# setLanguage signature even with a private setter.
old = '    fun setLanguage(value: AppLanguage) {'
new = '    fun updateLanguage(value: AppLanguage) {'
if text.count(old) != 1:
    raise SystemExit(f'expected one language mutator declaration, found {text.count(old)}')
text = text.replace(old, new, 1)
old = '            onSelect = languageController::setLanguage,'
new = '            onSelect = languageController::updateLanguage,'
if text.count(old) != 1:
    raise SystemExit(f'expected one language mutator reference, found {text.count(old)}')
text = text.replace(old, new, 1)

# The project already carries JUnit 4 for host tests; generated focused tests
# must use that dependency instead of kotlin.test, which is not on the classpath.
old = 'import kotlin.test.Test'
new = 'import org.junit.Test'
if text.count(old) != 2:
    raise SystemExit(f'expected two kotlin.test.Test imports, found {text.count(old)}')
text = text.replace(old, new)
old = 'import kotlin.test.assertTrue'
new = 'import org.junit.Assert.assertTrue'
if text.count(old) != 1:
    raise SystemExit(f'expected one kotlin.test.assertTrue import, found {text.count(old)}')
text = text.replace(old, new, 1)
old = 'import kotlin.test.assertEquals'
new = 'import org.junit.Assert.assertEquals'
if text.count(old) != 1:
    raise SystemExit(f'expected one kotlin.test.assertEquals import, found {text.count(old)}')
text = text.replace(old, new, 1)
p.write_text(text, encoding='utf-8')

# Patch the build-assets transform before it generates app/build.gradle.kts.
p = Path('/tmp/quran-buildassets.py')
text = p.read_text(encoding='utf-8')

# In Gradle Kotlin DSL `java` can resolve to the Android Java extension rather
# than the Java package root. Project.uri() is unambiguous.
old = 'val url = java.net.URI(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
new = 'val url = project.uri(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
if text.count(old) != 1:
    raise SystemExit(f'expected one Quran URI transform source target, found {text.count(old)}')
text = text.replace(old, new, 1)

# AGP 9 rejects Provider instances passed to the legacy SourceSet API. Resolve
# this project-owned build directory to a concrete File and keep the explicit
# mergeAssets task dependency already installed by the transform.
old = 'val generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets")'
new = 'val generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets").get().asFile'
if text.count(old) != 1:
    raise SystemExit(f'expected one generatedQuranAssets declaration, found {text.count(old)}')
text = text.replace(old, new, 1)
old = 'val root = generatedQuranAssets.get().asFile'
new = 'val root = generatedQuranAssets'
if text.count(old) != 1:
    raise SystemExit(f'expected one generated Quran root expression, found {text.count(old)}')
text = text.replace(old, new, 1)

p.write_text(text, encoding='utf-8')
print('patched Home ambiguity, language JVM name, unit-test imports and Gradle Quran asset generation')
