# Arihna premium Quran / daily inspiration / Hijri redesign

Baseline runtime: `584238d8800550cc96287d070744c55b6797b77c`.

## User-visible target
Implement the previously approved four-screen mockup direction (not the later darker variant): calm premium emerald/cream/gold visual system, realistic Mushaf reader, improved Surah navigation/bookmarks, uplifting Arabic daily content, refined alarm-volume control, and a calendar that can explicitly switch between Gregorian and Hijri.

## Frozen visual reference
Timzguida repository commit: `e1670c8ccca52d46daee20e1192a1fe8a6b79aa7`.
Palette tokens copied from Timzguida V3 theme:
- cream `#FCF8F0`
- emerald `#0E654B`
- emerald dark `#064A37`
- sage `#E8F0EA`
- gold `#D4AA46`
- ink `#202824`

## Quran / Mushaf
Existing immutable Tanzil/Tarteel text source remains pinned to `a5284b17034d36567e4a4bac982a17ba56837448` and remains the source used by Arihna for textual corpus acceptance.

Add a second pinned visual Mushaf source for page rendering only:
- repository: `batoulapps/quran-svg`
- commit: `78d97544bfdc57e9f04bc97ace3f857ed972d772`
- license: MIT
- acceptance: exactly 604 SVG Mushaf pages plus `surah.json` and license packaged offline.

The classic/Hafs reader must:
- look and behave like a real Mushaf page rather than a list of cards;
- support horizontal page swiping with a book/page-turn feel;
- retain offline behavior;
- expose current page and Surah context;
- support persistent page bookmarks;
- offer improved Surah selection with search plus Surah/Juz/Hizb navigation affordances and bookmark/recent shortcuts;
- keep the existing EASY reader available as an alternate readable-text mode.

## Home
Use the Timzguida palette across top-level surfaces while preserving prayer/location/alarm behavior.
Add Arabic-first daily content with explicit source labels:
- Quran verse or authenticated hadith / Islamic wisdom card;
- positive daily action card (small achievable action intended to improve character and brighten the day).
Daily content must be deterministic by date and must not claim a Quran/hadith source for unattributed motivational text.

## Alarm volume
Keep the same underlying system alarm-volume controller and test tag contract, but redesign the control as a slim, elegant, continuous-looking pill slider with a compact thumb and clear percentage.

## Calendar
The calendar UI must have an explicit Gregorian/Hijri mode selector. Hijri mode must display Hijri month/year and Hijri day numbers; Gregorian mode must remain available. Home's date entry continues to open the calendar without breaking the existing `home-hijri-date` and `home-calendar-dialog` contracts.

## Regression contracts
Preserve:
- applicationId `com.archimedeprojects.arihna`;
- minSdk 28, compileSdk 37, targetSdk 37;
- location behavior and GeoNames frozen bytes;
- prayer schedule behavior;
- alarm scheduling/permissions/signing contracts;
- Arabic RTL support;
- existing exact-SHA gate expectations.

## Acceptance gates
1. Candidate is a direct single-parent child of this SPEC commit.
2. Build + JVM tests green.
3. Existing Android instrumentation suites green on API 28 and API 36.
4. New static assertions verify Timzguida palette, 604 pinned Mushaf pages, bookmark UI, refined alarm slider tag, Arabic daily content tags, and Gregorian/Hijri calendar selector.
5. Only after exact-SHA gate is fully green may `main` fast-forward non-forced to the candidate.
