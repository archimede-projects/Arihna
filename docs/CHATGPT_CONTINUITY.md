# Arihna — ChatGPT continuity state

> Persistent cross-chat handoff file on dedicated branch `chat-context`. It must never be merged into `main`.

## Mandatory protocol

1. At the start of every Arihna chat, read this file first from `archimede-projects/Arihna`, branch `chat-context`.
2. Verify live GitHub state before claiming current SHAs, PASS, releases or APK verification.
3. After every user prompt, update/replace this file on `chat-context` before the final answer with: latest request, what was actually done, verified identifiers/results, failures/problems, current state and exact next action.
4. Never move `main` for continuity updates. Never merge `chat-context` into `main`.
5. Never store passwords, tokens, private keys or sensitive data here.
6. If write access is unavailable, say so explicitly.

## User working style

- Language: Italian; operational and concise.
- Proceed directly when context is sufficient; avoid unnecessary questions.
- During long GitHub/CI work, give brief progress updates.
- Never claim PASS/release/APK verification without evidence.
- Final physical Android validation is performed by the user on a Galaxy S25.
- Be transparent about incomplete or failed steps.

## Engineering / release discipline

- SPEC-first for runtime/code changes.
- Candidate = exactly one commit, direct child of its SPEC; replacement candidates after failure must be siblings from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer for user APKs.
- S25 validation builds are GitHub prereleases; stable release may reuse byte-identical physically validated APK bytes when production runtime is unchanged.
- Before APK handoff, redownload the published APK and verify SHA-256 + signer.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Current repository state

- Repo: `archimede-projects/Arihna`
- `main` live-verified after post-stable hardening: `4588088ae459b7ac8811fed359e83192bb0681bd`
- `main` head message: `test(quran): add post-stable hardening regressions`
- Parent SPEC: `50471062331499be5364cbebbfe4dc6f9b084610`
- Previous production runtime / stable APK code baseline remains `be4af47d4f5b267e47eac09d761775a2d99bf0db`.
- The two commits added after `be4af47...` are SPEC documentation + Android instrumentation tests only; no production source/assets/manifest/Gradle runtime behavior changed.

## Stable user baseline

- Stable tag: `arihna-stable-be4af47d-20260917`
- Title: `Arihna — Stable — S25 validated`
- Target production runtime: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- `draft=false`, `prerelease=false`
- Stable workflow `35188236936` / job `105094843337`: success, including published-APK redownload verification.
- APK: `arihna.apk`
- Size: `386929193` bytes
- SHA-256: `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- User physically validated broader smoke on Galaxy S25 and then installed/opened the stable APK cleanly.
- No new APK is required for the post-stable test-only hardening because AndroidTest/spec files are not packaged into the app.

## Tajwid correction history

The prior S25 Tajwid paging crash was caused by overlapping shadda+tanwin/contextual Tajwid spans. Runtime `be4af47...` makes the contextual rule win and includes regression coverage across all 6,236 pinned Hafs Uthmani ayat. User confirmed the crash is resolved.

## Post-stable Quran hardening — completed

User request on 2026-09-17: `Ok andiamo avanti` — proceed with the planned post-stable hardening.

SPEC:
- branch `spec/quran-post-stable-hardening-20260917`
- SHA `50471062331499be5364cbebbfe4dc6f9b084610`
- parent `be4af47d4f5b267e47eac09d761775a2d99bf0db`

Candidate:
- branch `candidate/quran-post-stable-hardening-v1-20260917`
- SHA `4588088ae459b7ac8811fed359e83192bb0681bd`
- direct child of SPEC, exactly one candidate commit
- only changed candidate file: `app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranPostStableHardeningAndroidTest.kt`
- no production code changed

Added regressions:
1. `longSessionHistoriesStayBoundedSeparatedAndClamped`: 64 repeated Hafs/Warsh/Tajwid history writes; validates 8-entry MRU, uniqueness, bounds and clamping.
2. `repeatedModeSwitchingRestoresIndependentPagesAndBookmarks`: repeated Hafs → Tajwid → Warsh → Hafs switching; validates per-mode page restoration and bookmark separation.
3. `repeatedFullscreenCyclesResetImmersiveAndKeepPerModePageState`: repeated fullscreen open/close for Hafs/Tajwid/Warsh; validates immersive reset and page-state preservation.

Exact-SHA gate:
- run `35210107445`: completed/success
- static/build job `105165336825`: success
- API28 full-suite job `105165337102`: success
- API36 Quran-hardening + permission-matrix job `105165337134`: success
- exact lineage/scope, frozen GeoNames, unit/build/AndroidTest compile, Quran corpus/assets, API28 full suite, API36 Quran hardening/Tajwid regression and permission matrix all passed their configured invariants.
- No raw exact connected-test count was separately recorded; do not invent one.

Promotion:
- `main` was live-rechecked at `be4af47...` after the gate.
- Promoted to `4588088ae459b7ac8811fed359e83192bb0681bd` using `force=false`.
- Live recheck confirmed `main=4588088...` and parent SPEC `5047106...`.
- Stress tests exposed no deterministic production defect, so no corrective runtime candidate was needed.

## Current state / exact next action

- Stable user-facing milestone is closed and remains the physically validated `arihna-stable-be4af47d-20260917` APK.
- Quran post-stable hardening is also closed and integrated in `main`; it improves repository regression protection only, not app bytes/features.
- No known blocker is open.
- Exact next product step: choose one narrow **user-visible improvement**, then execute it SPEC-first. Good candidate areas are Quran usability, Home/daily-inspiration polish, or Settings/accessibility. Do not combine multiple product areas into one candidate.
