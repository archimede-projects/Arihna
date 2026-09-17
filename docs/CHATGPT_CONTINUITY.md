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

- SPEC-first.
- Runtime candidate = exactly one commit, direct child of its SPEC.
- Replacement candidates after failure must be siblings from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer.
- S25 validation builds are GitHub prereleases.
- Before handoff, redownload the published APK and verify SHA-256 + signer.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Repository / current verified baseline

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Continuity file: `docs/CHATGPT_CONTINUITY.md`
- Live `main` rechecked 2026-09-17: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- Runtime: `fix(quran): prevent Tajwid overlap crash`
- Parent SPEC: `5b6f77bf1a7ee8634e74f46918b7f7263142316b`
- Exact-SHA gate previously live-verified: run `35110928396`, completed/success; static `104844112728`, API28 `104844112681`, API36 `104844112243` all success.
- Corrective prerelease previously live-verified: `quran-tajwid-crashfix-be4af47d-20260916`
- Release workflow run: `35112140220`, completed/success, including published-APK redownload verification.
- APK: `arihna-quran-tajwid-crashfix.apk`
- APK size: `386929193` bytes
- APK SHA-256: `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Persistent signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Package: `com.archimedeprojects.arihna`; minSdk 28, targetSdk 37, compileSdk 37.
- Open GitHub issues rechecked 2026-09-17: none.

## Tajwid crash status

The previous physical S25 crash while paging Tajwid was traced to overlapping Tajwid spans for shadda+tanwin/contextual rules. The promoted fix makes the contextual rule win and adds regression coverage across all 6,236 pinned Hafs Uthmani ayat.

Physical revalidation update from user on 2026-09-17: user reports that the Quran can now be paged successfully on the Galaxy S25. Treat the specific previously reported paging crash as physically resolved for the observed path. Do not generalize this to every Arihna feature without separate smoke validation.

## Preserved shipped scope

- Quran: Hafs/Tajwid/Warsh, bookmarks/fullscreen and existing page behavior.
- Home/Hijri and daily inspiration; daily-inspiration corpus/compact preview feature remains preserved.
- Prayer times, location, Qibla, alarms and permission flows.
- Frozen GeoNames/persistent signer workflow remains part of release discipline.

## Next objectives — prioritized

1. **S25 stabilization smoke pass**: exercise Hafs/Tajwid/Warsh navigation, bookmarks/fullscreen, cold start/resume, Prayer, Location, Qibla, alarms and permission flows on the physical S25. Record any failure before new feature work.
2. **Stable release milestone**: if the smoke pass is green and no code changes are needed, publish a stable/non-prerelease release from the same verified runtime `be4af47d...`, preserving signer and redownload verification. If any defect appears, fix SPEC-first before stable release.
3. **Quran quality hardening**: add/expand regressions around rapid paging, mode switches Hafs/Tajwid/Warsh, state restoration/bookmarks/fullscreen and memory/performance on long sessions; no visual/behavioral rewrite unless a SPEC asks for it.
4. **Core reliability hardening**: physical S25 checks for alarm delivery, reboot/background behavior, location changes, prayer recalculation and Qibla sensor/location transitions, with API28/API36 gates for any fixes.
5. **Next product feature**: after stabilization, choose one narrow SPEC-driven improvement rather than broad refactoring. Candidate areas: Quran usability, Home polish/daily inspiration, or settings/accessibility. Do not start one until the stable baseline is secured or the user explicitly reprioritizes.

## Latest user request / what was done

2026-09-17: User reported the corrected build now allows paging through the Quran and asked for the next project objectives.

Actually done this turn:
- Recorded the positive physical S25 result for the previously failing paging path.
- Re-read continuity state.
- Rechecked live `main`; it remains `be4af47d4f5b267e47eac09d761775a2d99bf0db` with parent SPEC `5b6f77bf1a7ee8634e74f46918b7f7263142316b`.
- Checked open GitHub issues; none are open.
- No runtime/app code, release, tag or `main` ref was changed.
- Defined the prioritized roadmap above.

## Current state / exact next action

- Tajwid/Quran paging crash: physically resolved for the user-observed S25 path.
- Current runtime remains the verified crash-fix SHA above.
- Exact next action: run a short physical S25 stabilization smoke pass across the remaining core areas. If green, prepare a stable release from the unchanged verified runtime; if any failure appears, stop and create a narrow SPEC-first correction.
