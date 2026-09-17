# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file on dedicated branch `chat-context`. It must never be merged into `main`.

## Mandatory protocol

1. At the start of every Arihna chat, read this file first from `archimede-projects/Arihna`, branch `chat-context`.
2. Treat it as handoff context, but verify live GitHub state before claiming current SHAs, PASS, releases or APK verification.
3. After every user prompt, replace/update this file on `chat-context` before the final answer. Record: latest request, what was actually done, verified identifiers/results, failures/problems, current state and exact next action.
4. Never move `main` merely for continuity updates. Never merge `chat-context` into `main`.
5. Never store passwords, tokens, private keys or sensitive data here.
6. If write access is unavailable, say so explicitly instead of pretending this file was updated.

## User working style

- Language: Italian.
- Execute directly and keep status concise.
- During long GitHub/CI work, give brief progress updates while working.
- Do not claim PASS, release readiness or APK verification without real evidence.
- User performs final physical Android validation on a Galaxy S25.
- When context is sufficient, proceed without unnecessary questions.
- User explicitly values transparent status reporting; if any required continuity/update step is incomplete, say so rather than implying success.

## Engineering / release discipline

- SPEC-first.
- Runtime candidate = exactly one commit, direct child of its SPEC.
- Failed replacement candidates must be siblings from the same SPEC, not descendants of a failed candidate.
- Full exact-SHA gates include API 28 and API 36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer.
- S25 validation builds are GitHub prereleases.
- Before handoff: redownload published APK and verify SHA-256 and signer.
- Preserve unrelated features unless SPEC explicitly changes them.

## Repository

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Continuity file: `docs/CHATGPT_CONTINUITY.md`

## Current live-verified runtime/release state

Rechecked live on GitHub on 2026-09-17 after the Tajwid crash correction:

- `main`: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- Runtime message: `fix(quran): prevent Tajwid overlap crash`
- Parent SPEC: `5b6f77bf1a7ee8634e74f46918b7f7263142316b`
- SPEC message: `spec(quran): prevent Tajwid overlap crash`
- Runtime is exactly one direct child of the SPEC.
- Exact-SHA gate run: `35110928396`
  - live status: `completed`
  - live conclusion: `success`
  - static/JVM/build job: `104844112728` = `success`
  - API28 full-suite job: `104844112681` = `success`
  - API36 Quran/permission-matrix job: `104844112243` = `success`
- Gate coverage added by this correction includes an Android regression that loads all 6,236 pinned Hafs Uthmani ayat and runs the Tajwid matcher over the complete corpus, requiring valid non-overlapping spans and no exception.
- Release workflow run: `35112140220`
  - live status: `completed`
  - live conclusion: `success`
  - release job: `104848314994` = `success`
  - successful release steps include exact promoted-runtime verification, definitive gate verification, persistent signer restoration, APK build, package/SDK/assets/signer/digest verification, prerelease publication, and redownload/reverification of the published APK.
- Release tag: `quran-tajwid-crashfix-be4af47d-20260916`
- Release title: `Arihna — Tajwid crash fix — S25 validation`
- Release targets runtime `be4af47d4f5b267e47eac09d761775a2d99bf0db`, `draft=false`, `prerelease=true`.
- Exactly one release asset:
  - `arihna-quran-tajwid-crashfix.apk`
  - bytes: `386929193`
  - live GitHub asset digest: `sha256:a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
  - URL: `https://github.com/archimede-projects/Arihna/releases/download/quran-tajwid-crashfix-be4af47d-20260916/arihna-quran-tajwid-crashfix.apk`
- Persistent signer certificate SHA-256 verified by the release workflow, including after redownload: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Persistent signer source commit: `ce23a7f78695be95c7f8dfd2bcedbe22544da94c`
- Keystore SHA-256 checked by the release workflow: `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`
- Package: `com.archimedeprojects.arihna`; minSdk 28, targetSdk 37, compileSdk 37.
- The corrected APK is CI/release-verified, but final physical Galaxy S25 revalidation of the corrected build is still user-side and has not yet been reported.

## Tajwid crash report and correction

### User report

On 2026-09-16 the user installed the previous daily-inspiration prerelease on a physical Galaxy S25 and reported that Arihna crashed while paging through Tajwid. Android displayed: `Si è verificato un problema con Arihna` / `Applicazione Arihna chiusa a causa di un bug.`

This physical result invalidated the previous prerelease for Tajwid validation even though its prior CI was green.

### Root cause identified

Inspection of the pinned Hafs Uthmani corpus and `UthmaniTajwidEngine` found a deterministic overlap failure:

- Some tokens contain shadda plus tanwin and also qualify for a context-sensitive tanwin transition rule.
- Real pinned-corpus examples include `جَآنٌّ وَلَّىٰ` and `مُطْمَئِنٌّۢ بِٱلْإِيمَـٰنِ`.
- The old matcher could emit generic `GHUNNAH` and a contextual rule such as `IDGHAM_WITH_GHUNNAH` or `IQLAB` on the same character range.
- The engine's validator requires Tajwid spans to be non-overlapping, so page composition could throw while scrolling to an affected ayah.

### Corrective implementation

SPEC: `5b6f77bf1a7ee8634e74f46918b7f7263142316b`

Runtime candidate/promoted fix: `be4af47d4f5b267e47eac09d761775a2d99bf0db`

Changed scope:

- `app/src/main/java/com/archimedeprojects/arihna/feature/quran/UthmaniTajwidEngine.kt`
  - computes the context-sensitive tanwin rule first;
  - when a token would otherwise produce both generic GHUNNAH and a contextual rule on the same range, the more specific contextual display rule wins;
  - preserves the deterministic, in-bounds, non-overlapping span invariant.
- `app/src/test/java/com/archimedeprojects/arihna/feature/quran/UthmaniTajwidEngineTest.kt`
  - adds exact pinned-corpus regression fixtures for shadda+tanwin IDGHAM and IQLAB cases.
- `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/TajwidCrashRegressionAndroidTest.kt`
  - loads the complete pinned Hafs corpus;
  - asserts exactly 6,236 ayat;
  - runs the Tajwid matcher over every ayah;
  - validates all spans are in bounds, non-empty and non-overlapping.

Narrow-scope intent preserved: no intentional behavior change to Prayer, Location, Qibla, alarms, Home, daily inspiration, permissions, Hafs page mapping/artwork, Warsh behavior, bookmarks or fullscreen behavior beyond what was required for this crash fix.

## Previous shipped Home feature preserved

The earlier Home daily-inspiration feature remains part of the runtime lineage and was not intentionally changed by the Tajwid correction:

- curated inspiration corpus expanded to at least 60 unique days;
- positive daily actions expanded to at least 30;
- Home preview limited to compact two-line text with ellipsis;
- full content remains available in the detail dialog.

Previous runtime before the Tajwid correction was `c07663fcc61101a75e2b78ba550ccc6f60e38b35` (`feat(home): expand daily inspiration variety compact preview`). Its prerelease `home-daily-inspiration-variety-c07663fc-20260916` is the build on which the user reported the physical Tajwid crash and should not be used for final Tajwid validation.

## Important preserved project history

Previously integrated work includes Quran/Hafs/Tajwid/Warsh, Home/Hijri, alarms, location, prayer and Qibla. Inspect GitHub history for exact implementation details; do not casually rewrite those areas during narrow tasks.

Persistent signer source historically/currently used for S25 validation builds:
- branch `location-step5-device-test`
- commit `ce23a7f78695be95c7f8dfd2bcedbe22544da94c`
- certificate SHA-256 `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- keystore SHA-256 `bc9057f26ad6de7efb70a5df06effb1ed259f1d015c3f062d0f757b3f0983b72`

Frozen GeoNames asset historically/currently used:
- release `settings-s25-premium-3f28b6f0-20260906`
- APK `arihna-settings-s25-premium.apk`
- `cities.db` SHA-256 `7bf32ed8845b293518880f00345406b5fc45e83b4c0e0555313c42472569c6bb`

## Latest user request / what was done

2026-09-17: The user asked to complete the continuity update that had been transparently reported as incomplete after the Tajwid crash-fix/release work.

Actually done this turn:

- Re-read the current continuity file from `chat-context` before modifying it.
- Reverified live GitHub `main` and confirmed it remains `be4af47d4f5b267e47eac09d761775a2d99bf0db`, direct child of SPEC `5b6f77bf1a7ee8634e74f46918b7f7263142316b`.
- Reverified exact-SHA gate run `35110928396` is still `completed/success`.
- Reverified release workflow run `35112140220` is still `completed/success`.
- Reverified prerelease `quran-tajwid-crashfix-be4af47d-20260916`, its target runtime, single APK asset, byte size and live GitHub SHA-256 digest.
- Re-read the release workflow definition and confirmed the persistent signer fingerprint, keystore hash, exact-runtime/gate checks, published-APK redownload check, downloaded SHA/size check and signer verification are encoded in the successful workflow.
- Replaced this continuity file on `chat-context` with the complete Tajwid crash/fix/release state.
- No app/runtime code and no `main` ref were changed in this continuity-only turn.

## Current state / exact next action

- `main` is the verified Tajwid crash-fix runtime `be4af47d4f5b267e47eac09d761775a2d99bf0db`.
- Corrective S25 prerelease is `quran-tajwid-crashfix-be4af47d-20260916` with APK `arihna-quran-tajwid-crashfix.apk` and SHA-256 `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`.
- CI/release verification is complete and green.
- Remaining validation: user installs the corrected APK on the physical Galaxy S25 and stress-pages through Tajwid, especially across many pages. Record the physical result when reported.
- If the corrected build still crashes, treat it as a new physical validation failure: do not amend the promoted runtime blindly; gather the new failing page/ayah or Android crash evidence, create a new SPEC-first correction, and preserve the same exact-SHA/release discipline.
