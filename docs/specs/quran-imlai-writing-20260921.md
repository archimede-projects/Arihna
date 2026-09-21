# SPEC — Quran in الرسم الإملائي — 2026-09-21

## Baseline

- Stable runtime baseline: `977fbde989facc3b392f6af5a65449a5322d58c6`.
- Stable release: `arihna-stable-977fbde9-20260921`.
- The baseline has been physically validated by the user on Galaxy S25.
- This cycle must preserve all unrelated stable behavior.

## Product definition

The requested “Facile” experience means:

**القرآن بالرسم الإملائي**

It is a **writing / orthography presentation**, not a separate reading mode and not simplified or paraphrased Quran content.

The goal is to make the Arabic text easier to read for a learner of Arabic by presenting an authoritative imla'i orthography while preserving the Quran text, ayah identity, order and semantics.

## Non-goals

This cycle must NOT:

- rewrite, paraphrase, summarize or linguistically simplify Quran verses;
- invent an algorithmic “easy Quran” by ad-hoc character substitution;
- change Hafs/Tajwid/Warsh recitation identity;
- change the existing Quran page count contracts without an explicit verified mapping;
- modify Prayer, Location, Qibla, alarms, Home, Settings or Android permission behavior;
- weaken or remove existing page-jump, bookmark, history or fullscreen behavior.

## Source integrity requirements

Before a runtime candidate is created:

1. Select a traceable, authoritative Quran corpus in `الرسم الإملائي`.
2. Record source, version/revision and redistribution/license terms in repository documentation.
3. Verify complete Hafs structure:
   - 114 surahs;
   - 6,236 ayat;
   - no missing ayat;
   - no duplicate ayat keys;
   - strictly correct surah/ayah ordering.
4. Maintain a deterministic mapping from every imla'i ayah to the existing pinned Hafs `surah:ayah` identity.
5. Add integrity checks that fail the build/test gate if the corpus structure drifts.
6. Do not claim textual equivalence merely from normalized string equality; orthographic differences are expected. Structural identity and source provenance are mandatory.

## Reader architecture

“Facile” must cease to behave as an independent single-surah reader.

The implementation should model imla'i as a **text/orthography presentation option** over the normal Quran reading/navigation flow.

Required behavior:

- retain normal sequential pageable/swipeable navigation;
- retain the existing Quran index;
- retain direct `Vai a pagina` navigation;
- retain bookmarks;
- retain recent/history behavior;
- retain fullscreen;
- retain persisted current-page behavior;
- switching writing presentation must not unexpectedly move the reader to another logical location.

Hafs, Tajwid and Warsh remain distinct reading/riwaya identities. The imla'i presentation must not be silently applied to Tajwid or Warsh unless a future verified source/mapping explicitly supports that combination.

## UI naming

Avoid the ambiguous product label `Facile` as the only description.

Preferred user-facing terminology:

- Italian: `Scrittura imlāʾī`
- Arabic: `الرسم الإملائي`

A shorter secondary label may be used if necessary for compact UI, but the precise term must remain discoverable.

## Typography / readability

The imla'i presentation should prioritize learner readability:

- clear Arabic/Naskh-style rendering using fonts already legally distributable in the app or platform-safe typography;
- sufficient text size and line height;
- correct RTL layout;
- clear ayah markers;
- no clipping of Arabic combining marks;
- no font substitution that changes the underlying text content.

Do not bundle or redistribute a new font until its license is explicitly verified.

## Migration / compatibility

The existing `QuranReadingMode.EASY` behavior is considered legacy behavior to be refactored by the future candidate.

Migration must:

- preserve existing stored reading mode safely;
- avoid crashes for users whose saved preference is `EASY`;
- map legacy `EASY` state to the new imla'i presentation in a deterministic way;
- preserve the user's logical reading location where possible;
- keep Hafs/Tajwid/Warsh stored state namespaces intact.

## Required regression coverage

A runtime candidate must add automated coverage for at least:

1. imla'i corpus structural integrity: 114 surahs / 6,236 ayat;
2. deterministic surah/ayah mapping;
3. migration from legacy `EASY` preference;
4. entering and leaving imla'i presentation without losing logical page/location state;
5. page jump while imla'i is selected;
6. bookmarks and recent/history behavior;
7. fullscreen open/close;
8. repeated switching between Hafs/Tajwid/Warsh and imla'i presentation without state contamination;
9. representative Arabic rendering checks for combining marks / non-empty ayah text;
10. no regression in the existing Tajwid overlap-crash test and post-stable Quran hardening suite.

## Candidate discipline

- Candidate must be exactly one commit and a direct child of this SPEC.
- Any replacement candidate after a failed gate must be a sibling from this same SPEC.
- Do not promote `main` until the exact candidate SHA passes required gates.

## Verification gates

Before promotion:

- static/source contract checks;
- unit tests;
- AndroidTest compilation;
- debug APK build;
- Quran corpus integrity checks;
- exact-SHA API28 full suite;
- exact-SHA API36 Quran regression + permission matrix;
- zero required test failures/errors/skips under the configured gate contract.

## Release discipline

If the candidate changes production runtime/app bytes:

1. promote `main` only after all exact-SHA gates are green;
2. publish a signed prerelease for Galaxy S25 validation;
3. use the persistent Arihna signer;
4. redownload the published APK;
5. verify APK SHA-256, size, package/SDK contract and signer;
6. do not advance the imla'i runtime to stable until the user physically validates it on Galaxy S25.

## Physical S25 acceptance focus

The validation build should be checked for:

- readability of `الرسم الإملائي`;
- correct Arabic text rendering;
- smooth sequential navigation;
- correct page/index/page-jump behavior;
- bookmark/fullscreen behavior;
- stable switching back to Hafs/Tajwid/Warsh;
- no crashes, clipping or obvious state contamination.

## Out of scope for this SPEC

- translations;
- transliteration into Latin characters;
- Arabic-language lessons or vocabulary annotations;
- word-by-word morphology;
- audio recitation changes;
- Tajwid rule expansion;
- Warsh imla'i conversion without its own verified source and future SPEC.
