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
- Runtime candidate = exactly one commit, direct child of its SPEC; replacement candidates after failure must be siblings from the same SPEC.
- Full exact-SHA gates include API28 and API36.
- Promote `main` only after required gates are green, via non-forced fast-forward.
- Use the persistent Arihna signer.
- S25 validation builds are GitHub prereleases; stable release may reuse byte-identical physically validated APK bytes when runtime is unchanged.
- Before handoff, redownload the published APK and verify SHA-256 + signer.
- Preserve unrelated features unless the SPEC explicitly changes them.

## Repository / stable verified baseline

- Repo: `archimede-projects/Arihna`
- Runtime branch: `main`
- Continuity branch: `chat-context`
- Live `main` rechecked 2026-09-17 after stable publication: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- Runtime: `fix(quran): prevent Tajwid overlap crash`
- Parent SPEC: `5b6f77bf1a7ee8634e74f46918b7f7263142316b`
- Exact-SHA gate `35110928396`: completed/success; static `104844112728`, API28 `104844112681`, API36 `104844112243` all success.
- Physical Galaxy S25 stabilization smoke: user reports PASS/no blocker for Hafs/Tajwid/Warsh navigation, bookmarks/fullscreen, app reopen/cold-start behavior, Prayer, Location, Qibla, alarms and permissions.

### Stable release

- Tag: `arihna-stable-be4af47d-20260917`
- Title: `Arihna — Stable — S25 validated`
- Target runtime: `be4af47d4f5b267e47eac09d761775a2d99bf0db`
- `draft=false`, `prerelease=false`
- Stable release workflow run: `35188236936`, completed/success
- Stable job: `105094843337`, success
- Successful critical steps: verify exact runtime/lineage/gate/source prerelease; verify physically validated APK bytes/signature; publish stable; redownload and verify published stable release.
- Exactly one stable asset: `arihna.apk`
- Size: `386929193` bytes
- SHA-256: `a0c91c24846480037d17d493481795c7cb7c0a441c484845ef61c24206b6f5ed`
- Signer certificate SHA-256: `1397008c1f962dbbd36dd8a8ea0216afdd06e4b2b3e08bc0f6d4b54344d7b0fa`
- Download URL: `https://github.com/archimede-projects/Arihna/releases/download/arihna-stable-be4af47d-20260917/arihna.apk`
- Release URL: `https://github.com/archimede-projects/Arihna/releases/tag/arihna-stable-be4af47d-20260917`
- Stable APK is byte-identical to the physically S25-validated prerelease asset `arihna-quran-tajwid-crashfix.apk`.

## Relevant correction history

The prior physical S25 Tajwid paging crash was caused by overlapping Tajwid spans for shadda+tanwin/contextual rules. Runtime `be4af47d...` makes the contextual rule win and includes regression coverage across all 6,236 pinned Hafs Uthmani ayat. User subsequently confirmed the crash is resolved and completed the wider smoke pass.

Corrective prerelease retained as validation history:
- tag `quran-tajwid-crashfix-be4af47d-20260916`
- workflow run `35112140220` success
- asset `arihna-quran-tajwid-crashfix.apk`
- same SHA-256/size/signer as the stable APK.

## Latest user request / what was done

2026-09-17: User authorized proceeding with the stable release after completing the physical S25 smoke validation.

Actually done this turn:
- Reverified live `main` and source prerelease before release work.
- Created release-only branch `driver/stable-be4af47d-release-20260917` from exact runtime; `main` was not moved.
- Chose to publish the exact bytes already physically tested rather than rebuild a new APK.
- First stable workflow run `35188163125` / job `105094624760` failed before APK download/publication because `android-actions/setup-android@v3` defaulted to obsolete SDK package `tools`; no stable release was published by this failed attempt.
- Read the job log, identified `Failed to find package 'tools'`, and corrected only the release workflow to request `platform-tools`; workflow-fix commit `9cee4b2d1739068d2aa4204e27b26918703262d4` on the release branch.
- Second run `35188236936` / job `105094843337` completed successfully.
- The source APK was verified for exact size, SHA-256 and persistent signer before publication.
- Published stable release `arihna-stable-be4af47d-20260917` as non-prerelease.
- Redownloaded the published stable APK in CI and successfully reverified size, SHA-256 and signer.
- Rechecked live release metadata and rechecked that `main` remains unchanged at `be4af47d...`.
- No runtime/app code change was made for stable publication.

## Current state / exact next action

- Stable baseline is now secured and physically S25-validated.
- `main` remains `be4af47d4f5b267e47eac09d761775a2d99bf0db`; stable APK is verified and published.
- No release blocker is currently known.
- Exact next action: begin future product improvement planning with one narrow user-prioritized objective at a time, SPEC-first, preserving this stable baseline. Candidate areas include Quran usability, Home/daily inspiration polish, settings/accessibility, or another user-selected enhancement.
