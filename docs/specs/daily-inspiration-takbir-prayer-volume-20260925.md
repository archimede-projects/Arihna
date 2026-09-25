# SPEC — Daily inspiration notification + two-takbir alert + per-prayer volume — 2026-09-25

## Baseline

- Stable runtime baseline: `0d7d12c2e89a8012446bde6967d366f31ca8e77e`.
- Stable release: `arihna-stable-0d7d12c2-20260925`.
- This baseline includes Quran Imlāʾī and was physically validated PASS by the user on Galaxy S25.
- Preserve all unrelated stable behavior.

## User intent

The user requested three additions:

1. Send the existing **daily inspiration / phrase of the day** as an Android notification.
2. Add a genuinely short prayer-call option that pronounces **only**:
   - `الله أكبر`
   - `الله أكبر`
   and then stops.
3. Allow an independent playback-volume percentage for each prayer alarm, e.g. Fajr/Isha at 100% and Dhuhr/Asr at 40%.

## Terminology: two-takbir alert

The existing `AdhanVariant.SHORT` is a full adhan recording presented in a shorter/faster form. It does **not** satisfy this request.

Add a distinct option with an unambiguous name such as:

- Italian: `Takbīr breve · Allahu Akbar ×2`
- Arabic: `تكبير قصير · الله أكبر ×٢`

Requirements:

- audio content must be exactly two natural utterances of `الله أكبر`, then silence/stop;
- must not continue into the shahada, hayya phrases, or any other adhan phrase;
- must not be implemented by merely speeding up a complete adhan;
- existing `SHORT` and all existing adhan variants remain migration-compatible and unchanged;
- source/provenance/license of the dedicated audio must be documented;
- if derived from an existing permissively licensed recording, the exact source asset and deterministic trim boundary must be documented and the final asset SHA-256 pinned;
- preview and actual alarm playback must use the same bytes.

## Per-prayer volume

Add a persisted 0–100% playback level for each `AlarmDefinition.PrayerLinked` rule.

Behavior:

- each of Fajr, Dhuhr, Asr, Maghrib and Isha can have its own percentage;
- existing/migrated prayer rules default to 100% so upgrades do not become unexpectedly quieter;
- volume is a **local MediaPlayer gain multiplier** over Android's existing alarm-stream volume;
- do not permanently change `AudioManager.STREAM_ALARM` when a prayer fires;
- therefore the existing global alarm-volume control remains the system baseline, while per-prayer volume scales that baseline;
- 100% = full current Android alarm-stream level; 40% = 40% of that level;
- silent profile remains silent regardless of stored percentage;
- system ringtone and bundled adhan playback both honor the prayer percentage when played through Arihna's `MediaPlayer`;
- custom alarms keep their current behavior unless explicitly extended by a later SPEC.

UI:

- expose the prayer-specific volume slider in the existing per-prayer sound dialog;
- show the numeric percentage;
- preview selected adhan using the currently selected prayer percentage;
- stable test tags for slider/value.

Persistence:

- version the alarm-rule codec migration safely;
- V1/V2 persisted rules must still decode;
- missing prayer volume migrates to 100%;
- updating sound or enable state must not reset stored prayer volume.

## Daily inspiration notification

Reuse the exact existing `dailyInspirationFor(date)` source so Home and notification cannot diverge for the same date.

Required behavior:

- user-controllable setting to enable/disable the daily notification;
- default is OFF for existing users until explicitly enabled, to avoid adding unsolicited daily notifications on upgrade;
- default delivery time when enabled: 08:00 device local time;
- user can choose the daily delivery time;
- notification includes the daily Arabic inspiration, its Italian translation when present, and reference;
- no duplicate notification for the same local calendar date;
- tapping the notification opens Arihna;
- use a dedicated notification channel separate from alarm channels;
- notification must never start alarm/full-screen ringing behavior;
- respect Android `POST_NOTIFICATIONS` permission;
- use inexact daily scheduling: this feature must not require exact-alarm permission;
- reschedule after boot, app replacement, manual time change and timezone change;
- changing enable/time updates the next scheduled occurrence deterministically.

## Scope preservation

Must not change:

- Quran corpus, Imlāʾī/Hafs/Tajwid/Warsh behavior;
- prayer-time calculation;
- alarm occurrence calculation or snooze semantics;
- location/Qibla;
- Home daily-inspiration selection algorithm;
- existing custom alarms;
- Android permission requests beyond already declared notification permission;
- existing bundled adhan variants or their storage IDs.

## Tests / gates

Candidate must remain exactly one commit and a direct child of this SPEC.

Add deterministic coverage for at least:

- existing alarm V1/V2 persistence -> prayer volume = 100%;
- new persistence round-trip retains independent prayer volumes;
- sound/profile changes preserve prayer volume;
- ringing payload carries prayer volume;
- local playback gain conversion clamps 0..100%;
- two-takbir variant has a unique stable storage ID and exact dedicated resource mapping;
- two-takbir asset checksum/provenance contract;
- daily inspiration scheduler enable/disable/time behavior;
- no same-date duplicate delivery;
- notification content comes from `dailyInspirationFor(date)`;
- boot/time/timezone/app-replacement rescheduling path;
- notification permission denial is handled without crash;
- prayer-screen slider and two-takbir selection UI;
- API28 full suite;
- API36 alarm/notification/permission matrix and relevant Quran hardening unchanged.

## Promotion / device validation

- Run full exact-SHA static/build + API28 + API36 gates.
- Promote to `main` only after all required gates are green, via non-forced fast-forward.
- Publish a signed Galaxy S25 prerelease and redownload/verify APK SHA-256 + signer.
- Physical S25 validation must include:
  - daily inspiration notification enable/time/delivery/tap;
  - two-takbir preview and real prayer test: exactly `الله أكبر` twice, then stop;
  - Fajr/Isha at 100% and Dhuhr/Asr at 40% (or other visibly distinct test values);
  - verify global Android alarm volume is unchanged after playback;
  - existing full adhan variants still play normally;
  - no regressions in prayer scheduling, Quran, location/Qibla or custom alarms.
