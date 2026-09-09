from pathlib import Path

path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt')
text = path.read_text()
old = '''                    val pageLabel = appText(\n                        "pag. ${currentPage + 1}",\n                        "صفحة ${toArabicIndic(currentPage + 1)}",\n                    )'''
new = '''                    val pageLabel = if (mode == QuranReadingMode.HAFS_TAJWID) {\n                        appText("Tajwid Beta · regole principali", "تجويد تجريبي · القواعد الرئيسية")\n                    } else {\n                        appText(\n                            "pag. ${currentPage + 1}",\n                            "صفحة ${toArabicIndic(currentPage + 1)}",\n                        )\n                    }'''
if text.count(old) != 1:
    raise SystemExit(f'expected one header page label, got {text.count(old)}')
path.write_text(text.replace(old, new, 1))
