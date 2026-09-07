from pathlib import Path

p = Path('/tmp/revision.py')
text = p.read_text(encoding='utf-8')
old = "text = replace_once(text, '    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 'Home route signature Quran')"
new = "text = text.replace('    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 1)"
if text.count(old) != 1:
    raise SystemExit(f'expected one transform source target, found {text.count(old)}')
p.write_text(text.replace(old, new, 1), encoding='utf-8')
print('patched Home route transform to replace first occurrence only')
