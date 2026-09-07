from pathlib import Path

# Fix the Home transform ambiguity: the route signature is the first of two
# intentionally similar callback signatures in the baseline source.
p = Path('/tmp/revision.py')
text = p.read_text(encoding='utf-8')
old = "text = replace_once(text, '    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 'Home route signature Quran')"
new = "text = text.replace('    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 1)"
if text.count(old) != 1:
    raise SystemExit(f'expected one Home transform source target, found {text.count(old)}')
p.write_text(text.replace(old, new, 1), encoding='utf-8')

# In Gradle Kotlin DSL `java` can resolve to the Android Java extension rather
# than the Java package root. Project.uri() is unambiguous.
p = Path('/tmp/quran-buildassets.py')
text = p.read_text(encoding='utf-8')
old = 'val url = java.net.URI(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
new = 'val url = project.uri(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
if text.count(old) != 1:
    raise SystemExit(f'expected one Quran URI transform source target, found {text.count(old)}')
p.write_text(text.replace(old, new, 1), encoding='utf-8')

print('patched Home ambiguity and Gradle Quran URI generation')
