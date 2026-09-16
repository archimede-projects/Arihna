# Tajwid overlap crash — approved corrective SPEC — 2026-09-16

## Trigger

Physical Galaxy S25 validation of prerelease `home-daily-inspiration-variety-c07663fc-20260916` reported an application crash while paging through Hafs Tajwid Beta.

Inspection of the pinned Uthmani corpus and runtime matcher found a deterministic failure mode: a token may carry shadda plus tanwin and also satisfy a context-sensitive tanwin transition rule. Examples present in the pinned corpus include `جَآنٌّ وَلَّىٰ` and `مُطْمَئِنٌّۢ بِٱلْإِيمَـٰنِ`. The current matcher emits both generic GHUNNAH and the context-sensitive rule on the same character range, while its validator rejects overlapping spans; page composition therefore can throw while paging.

## Approved correction

- Keep the pinned Quran corpus, page mapping, Hafs/Warsh artwork, reading modes, navigation, bookmarks, fullscreen behavior and Tajwid Beta disclaimer unchanged unless required by this crash fix.
- Preserve the invariant that Tajwid output spans are deterministic, valid, in bounds and non-overlapping.
- When a shadda/tanwin token qualifies both for generic GHUNNAH and for a more specific context-sensitive tanwin rule (IQLAB, IKHFA or IDGHAM), emit the context-sensitive display rule for that range instead of an overlapping generic GHUNNAH span.
- Add JVM regression coverage using exact pinned-corpus fixtures that previously create overlapping same-range rules.
- Add Android regression coverage that loads all 6,236 pinned Hafs Uthmani ayat and runs the Tajwid matcher across the entire corpus without exceptions, asserting valid non-overlapping spans.
- Do not change Prayer, Location, Qibla, alarms, Home, daily inspiration, permissions or Warsh behavior.

## Lineage and release gate

- The runtime candidate must be exactly one direct child of this SPEC commit.
- A failed candidate replacement must be a sibling from this same SPEC, not a child of the failed candidate.
- Before any promotion to `main`, run an exact-SHA gate containing JVM/unit/build checks plus the existing API 28 full Android regression and API 36 Quran/permission regression; the API 36 Quran coverage must include the new all-corpus Tajwid crash regression.
- Promote `main` only by non-forced fast-forward after all required jobs are green.
- Any Galaxy S25 validation APK remains a prerelease, uses the persistent Arihna signer, and must be redownloaded after publication with SHA-256, size and signer reverified before handoff.
