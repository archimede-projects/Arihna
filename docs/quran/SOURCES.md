# Quran source provenance

Arihna bundles the Quran reader assets at build time from the immutable public source `TarteelAI/quran-assets@a5284b17034d36567e4a4bac982a17ba56837448`.

- `text/quran-uthmani.txt`: Tanzil Quran Text Uthmani v1.1, copied verbatim.
- `metadata/juz-info.json`: Juz boundaries.
- `metadata/hizb-info.json`: Hizb boundaries.
- `text/README.md`: Tanzil attribution and CC BY 3.0 notice, bundled as `TANZIL_TEXT_README.md`.

The exact-SHA gate independently downloads the same pinned files and compares them byte-for-byte with the assets merged into the APK build inputs. Runtime Quran reading is fully offline.
