# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file on dedicated branch `chat-context`. It must never be merged into `main`.

## Mandatory protocol
1. Read this file first for every Arihna prompt.
2. Live-verify GitHub before claiming current SHA/PASS/release/APK state.
3. After every user prompt, replace/update this file with request, actual work, verified identifiers, failures, current state, exact next action.
4. Never move `main` for continuity; never merge `chat-context` into `main`.
5. Never store secrets.
6. If GitHub write access is unavailable, say so.

## User working style
- Italian, concise, operational.
- Proceed when context is sufficient; avoid unnecessary questions.
- Give brief progress updates during long CI/release work.
- Never claim PASS/release/APK verification without evidence.
- Physical Android validation is performed by the user on Galaxy S25.
- For consultant handoff, user wants assistant replies/reports in **one single copyable block** (one code block / one Copy button) containing everything actually done and the current next step.

## Engineering / release discipline
- SPEC-first for runtime/code changes.
- Candidate = exactly one commit, direct child of SPEC; failed replacements are siblings from same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after green gates via non-forced fast-forward.
- User-facing changed runtimes go to S25 prerelease first.
- Use persistent Arihna signer and redownload published APK to verify SHA-256 + signer before handoff.
- Preserve unrelated features unless SPEC changes them.

## Prior stable baseline
- Stable tag `arihna-stable-be4af47d-20260917`
- Runtime `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- APK `arihna.apk`, size `386929193`
- SHA-256 `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Signer cert SHA-256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- User previously passed broad Galaxy S25 smoke and clean stable install/open.
- This remains last physically validated stable baseline until the new page-jump runtime is validated.

## Post-stable Quran hardening — completed
- SPEC `50471062331499be5364cbebbfe4dc6f9b084610`
- test-only main lineage `4588088ae459b7ac8811fed359e83192bb0681bd`
- gate `35210107445`: completed/success
- Added regressions for long histories, repeated Hafs/Tajwid/Warsh switching, bookmark separation, fullscreen cycles and state restoration.
- No production behavior changed in that cycle.

## Quran direct page jump — promoted, awaiting S25 validation

### SPEC
- branch `spec/quran-direct-page-jump-20260918`
- SHA `e680659976010d8e8bfe2e62da862567230b5554`
- parent/base `4588088ae459b7ac8811fed359e83192bb0681bd`
- scope: direct exact-page navigation 1..604 from Quran explorer; preserve all existing navigation and unrelated app features.

### Candidate / current main
- branch `candidate/quran-direct-page-jump-v1-20260918`
- SHA `977fbde989facc3b392f6af5a65449a5322d58c6`
- direct child of SPEC, exactly one candidate commit
- changed files:
  1. `app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt`
  2. `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt`
- UI: bilingual `Vai a pagina` control, numeric input, valid domain 1..604, explicit `Vai` action.
- Regression `directPageJumpWorksAcrossHafsTajwidAndWarsh`: pages 321 Hafs, 77 Tajwid, 604 Warsh + persisted state.
- No corpus/artwork/Tajwid rule/bookmark schema/fullscreen/Prayer/Location/Qibla/alarm/Home/Settings/permission changes.
- Live rechecked 2026-09-21: `main = 977fbde989facc3b392f6af5a65449a5322d58c6`.

### Exact-SHA gate
- driver `driver/quran-direct-page-jump-v1-gate-20260918`
- driver commit `e130718e3ca444dcae687a94423b9e5b9bfeb8ee`
- run `35325258945`: completed/success
- static/build `105536706783`: success
- API28 full suite `105536706741`: success
- API36 Quran + hardening + Tajwid + permission matrix `105536706685`: success
- Do not invent raw connected-test counts beyond configured invariants.

### S25 validation prerelease
- release driver `driver/quran-direct-page-jump-v1-release-20260918`
- driver commit `d3ebba79c6d2c28db9fb0b75df7affd6aa6e8390`
- release run `35326339850`, job `105540154182`: completed/success
- tag `quran-page-jump-977fbde9-20260918`
- title `Arihna — Quran page jump — S25 validation`
- release id `391334028`
- target `977fbde989facc3b392f6af5a65449a5322d58c6`
- `draft=false`, `prerelease=true`
- asset `arihna-quran-page-jump.apk`, id `572225953`
- size `386945577`
- SHA-256 `002d640a734eadf46b141b4489ae3c26d2803d9d49b4bacc772f7c678ab20813`
- signer cert verified `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- direct download: https://github.com/archimede-projects/Arihna/releases/download/quran-page-jump-977fbde9-20260918/arihna-quran-page-jump.apk
- release page: https://github.com/archimede-projects/Arihna/releases/tag/quran-page-jump-977fbde9-20260918
- Live rechecked 2026-09-21: release metadata still matches; run/job remain success.

## Latest user prompt / what was done
2026-09-21 user clarified the intended Easy Quran presentation: `Un arabo facile da leggere per chi sta imparando la lingua ataba` (Arabic).

Actually done:
- Read continuity from `chat-context`.
- Live-verified current `main = 977fbde989facc3b392f6af5a65449a5322d58c6`.
- Re-inspected `QuranCorpus.kt` and `EasyQuranReader`.
- Confirmed current Easy reader loads the same `quran-uthmani.txt` corpus as the other text-based Quran path, so it does not currently provide a genuinely learner-friendly Arabic orthography; it merely renders Uthmani text as verse cards in a separate reader.
- Refined product intent: “Facile” should be a learner-readable Arabic writing presentation for people learning Arabic, while preserving the exact Quran wording and normal pageable navigation.
- No runtime code, `main`, CI, tag, release, or APK state changed this turn.

## Corrected product intent for “Facile”
- “Facile” means **learner-friendly Arabic writing**, not simplified Quran content and not a separate reading/navigation mode.
- Preserve the Quran wording, verse order and meaning exactly; do not rewrite/paraphrase verses.
- Preferred presentation target: a clear standard Arabic / imla'i-style orthographic rendering with full/appropriate vowel marks, clean readable Naskh-style typography, generous line spacing and readable word spacing, avoiding the visual complexity of Uthmani/Mushaf orthography where an authoritative equivalent source is available.
- Normal reading behavior must remain pageable/swipeable with index, page jump, bookmarks, fullscreen and persisted page state.
- Hafs/Tajwid/Warsh remain distinct reading identities; “Facile” is a writing/display option.
- Do not invent or algorithmically “simplify” Quran text. Any learner-friendly orthographic corpus must come from a verified authoritative source or a deterministic, validated mapping with exact verse-level equivalence checks.

## Current state / exact next action
- Page-jump runtime `977fbde9...` is physically validated PASS on Galaxy S25.
- Last stable tag is still `arihna-stable-be4af47d-20260917`; secure/promote the validated page-jump runtime as stable independently.
- After stable publication, create a narrow SPEC for **learner-friendly Arabic writing mode**:
  1. source/verify an authoritative learner-readable Arabic orthography corpus;
  2. assert 114 surahs / 6,236 Hafs ayat and verse-by-verse identity/equivalence requirements;
  3. integrate it as a writing/display style over normal pageable reading, not as a separate single-surah reader;
  4. add API28/API36 regressions for paging, mode switching, bookmarks/fullscreen/page jump and text-source integrity;
  5. publish signed S25 prerelease only after green exact-SHA gates.
