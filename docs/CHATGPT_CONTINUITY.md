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
- Exact-SHA gate: run `35110928396`, completed/success; static `104844112728`, API28 `104844112681`, API36 `104844112243` all success.
- Corrective prerelease: `quran-tajwid-crashfix-be4af47d-20260916`
- Release workflow run: `35112140220`, completed/success, including published-APK redownload verification.
- APK: `arihna-quran-tajwid-crashfix.apk`
- APK size: `386929193` bytes
- APK SHA-256: `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Persistent signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Package: `com.archimedeprojects.arihna`; minSdk 28, targetSdk 37, compileSdk 37.

## Physical Galaxy S25 validation

The Tajwid paging crash was traced to overlapping Tajwid spans for shadda+tanwin/contextual rules. The promoted fix makes the contextual rule win and adds regression coverage across all 6,236 pinned Hafs Uthmani ayat.

User validation status on 2026-09-17:

- Quran paging after the Tajwid fix: PASS on physical Galaxy S25.
- User additionally confirms the full stabilization smoke set previously requested has already been exercised and appears all good: Hafs/Tajwid/Warsh navigation, bookmarks/fullscreen, app reopen/cold-start behavior, Prayer, Location, Qibla, alarms and permission flows.
- Treat the current S25 smoke-validation milestone as complete based on the user's physical testing.
- No currently reported physical regression is blocking stabilization.

## Preserved shipped scope

- Quran: Hafs/Tajwid/Warsh, bookmarks/fullscreen and existing page behavior.
- Home/Hijri and daily inspiration; daily-inspiration corpus/compact preview feature remains preserved.
- Prayer times, location, Qibla, alarms and permission flows.
- Frozen GeoNames/persistent signer workflow remains part of release discipline.

## Next objectives — prioritized

1. **Stable release milestone**: prepare/publish a stable non-prerelease release from the unchanged verified runtime `be4af47d...`, preserving the signer and published-APK redownload/digest verification. Do not add unrelated feature work into this release step.
2. **Post-stable hardening**: expand regressions around Quran rapid paging, Hafs/Tajwid/Warsh switching, state restoration/bookmarks/fullscreen, long-session memory/performance, alarms/background/reboot, location changes, prayer recalculation and Qibla transitions.
3. **Future product improvements**: after the stable baseline is secured, choose one narrow SPEC-driven improvement at a time. Candidate areas remain Quran usability, Home/daily inspiration polish, settings/accessibility or other user-prioritized enhancements. Avoid broad refactors.

## Latest user request / what was done

2026-09-17: User clarified that the full point-1 S25 smoke pass had already been completed and everything appears to be working. User also confirmed interest in future app improvements, but only after stabilization/release work.

Actually done this turn:
- Re-read continuity state.
- Rechecked live `main`; it remains `be4af47d4f5b267e47eac09d761775a2d99bf0db`, direct child of SPEC `5b6f77bf1a7ee8634e74f46918b7f7263142316b`.
- Rechecked the corrective prerelease still targets the same runtime and still exposes the same single APK with SHA-256 `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`.
- Recorded the user's full physical Galaxy S25 smoke validation as complete.
- No runtime/app code, release, tag or `main` ref was changed in this turn.

## Current state / exact next action

- Current runtime remains the verified Tajwid crash-fix SHA above.
- CI/release verification is green and the user has now completed the requested physical S25 smoke validation with no reported blocker.
- Exact next action: prepare and publish the stable non-prerelease release from this unchanged runtime using the existing release discipline. Future feature improvements come after that stable baseline is secured.
