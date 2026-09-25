# Quran Imlāʾī source provenance

## Selected source

Arihna's `الرسم الإملائي` presentation uses **Tanzil Quran Text — Simple, Version 1.1**.

Tanzil documents its **Simple** text as Quran text in **Imla'ei script**, the modern Arabic writing style. Tanzil states that its Quran text is carefully produced, highly verified and continuously monitored.

Primary documentation:

- https://tanzil.net/docs/Quran_Text_Types
- https://tanzil.net/docs/Text_License
- https://tanzil.net/download/
- updates: http://tanzil.net/updates/

## Reproducible build retrieval

Tanzil's official download is form/POST based. For deterministic CI retrieval, Arihna pins a verbatim mirror of the Tanzil source file:

- repository: `dotquran/corpus`
- commit: `c23f5cec2e95e253dc450bd0f34d09e37ba40fac`
- path: `src/resources/simple.txt`
- Git blob: `b7b0b3db111cf183d1439ff76dc38d61d743592d`

The mirrored file itself carries the Tanzil copyright/license block and identifies itself as:

`Tanzil Quran Text (Simple, Version 1.1)`

Arihna copies that file verbatim into the generated APK asset `quran/quran-imlai.txt`; application parsing ignores comment/license lines but does not rewrite verse text.

## License / redistribution

Tanzil Quran Text is licensed under **Creative Commons Attribution 3.0**. Tanzil permits copying and distributing verbatim copies, prohibits changing the Quran text, requires clear attribution to Tanzil Project and a link to Tanzil so users can track changes, and requires the copyright notice to remain with copies containing substantial text.

Arihna therefore:

- does not algorithmically rewrite or normalize the Imlāʾī verses;
- preserves the embedded Tanzil notice in the bundled asset;
- displays Tanzil attribution in the Imlāʾī reader;
- exposes a user-facing link to https://tanzil.net.

## Structural verification

Verified before candidate creation and rechecked by the Gradle asset task:

- 114 surahs;
- 6,236 ayat;
- 6,236 unique `surah:ayah` keys;
- first key `1:1`;
- last key `114:6`;
- no duplicate keys;
- key sequence exactly matches Arihna's pinned Hafs Uthmani corpus.

Orthographic string equality with Uthmani is neither expected nor used as a correctness test. Structural identity, pinned provenance and verbatim source preservation are the integrity contract.
