# SPEC — Reduce APK DEX by removing Material Icons Extended — 2026-10-16

Base stable/runtime: `3492add5cb3f74b5ffe4e477055be7aa676fb6c4`

## Motivation / measured baseline
Current physically validated stable APK is 387,885,070 bytes.
Fine APK audit shows:
- Hafs offline Mushaf: 161,214,746 compressed bytes;
- Warsh offline Mushaf: 135,524,170 compressed bytes;
- DEX: 68,830,988 bytes across 20 dex files;
- GeoNames: 15,033,136 compressed bytes;
- raw audio: ~5.1 MB;
- Quran text: ~0.9 MB.

Hafs, Warsh, GeoNames, Quran text and audio are product content and MUST remain intact.
The current app depends on `androidx.compose.material:material-icons-extended` while Arihna uses only a small finite set of Rounded icons.

## Goal
Reduce APK/DEX size without changing user-visible behavior or offline content by removing the full Material Icons Extended dependency and retaining only the exact Material icon definitions used by Arihna.

## Requirements
1. Remove `material-icons-extended` from runtime dependencies.
2. Preserve every currently used icon's exact Material Rounded appearance.
3. Use source copied from the exact official AndroidX Material Icons artifact/version resolved by the current Compose BOM (1.7.8), with provenance documented.
4. Keep only icon definitions actually referenced by Arihna; no bulk icon package.
5. No navigation/UI layout/text/behavior changes.
6. Preserve all offline assets byte-for-byte in content: 604 Hafs pages, 604 Warsh pages, GeoNames DB, Quran corpora/metadata and approved alarm audio.
7. Preserve all prayer, Adhan, Makkah x2, volume, vintage alarm, Quran, Qibla, location, permission and alarm behavior.
8. Debug APK remains the validation build format for this optimization; do not introduce release/R8 migration in this SPEC.
9. Candidate must demonstrate a material APK/DEX reduction relative to stable baseline. Report exact before/after bytes and DEX totals.
10. If exact local icon source replacement causes visual or compilation ambiguity, stop rather than substituting approximate icons.

## Required icon inventory
Inventory all main-source imports from `androidx.compose.material.icons` and pin the complete unique list before candidate creation. Known current uses include navigation, prayer, Quran, location and calendar icons.

## Validation
- Candidate exactly one commit direct child of this SPEC.
- Static contract: no `material-icons-extended` runtime dependency and no unresolved external extended icon definitions.
- Unit + AndroidTest compile + debug APK build.
- APK audit verifies Hafs/Warsh/GeoNames/Quran/vintage content remains present.
- Compare APK size and total compressed/raw DEX against stable baseline.
- Full exact-SHA API28 and API36 gates.
- Promote only after all gates pass.
- Publish verified S25 prerelease with persistent signer and redownload verification.
- Physical S25 validation required before replacing current stable.

## Explicit non-goals
- Do not remove/compress/alter Mushaf pages.
- Do not remove Warsh.
- Do not alter GeoNames coverage.
- Do not switch to online/on-demand content.
- Do not change R8/release build pipeline in this SPEC.
