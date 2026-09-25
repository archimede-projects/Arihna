# SPEC — Makkah two-takbir alert

Date: 2026-09-25

Base runtime:
`ab1051c43514b4595b92ae3533a3ee644b41dd29`

## User correction
The current dedicated short Takbīr alert is rejected on physical Galaxy S25 because the voice/noise quality sounds amateurish.

Replace only that dedicated short alert with an authentic Makkah-source recording that plays:

`الله أكبر`
`الله أكبر`

and then stops.

It must NOT continue into the remainder of the Adhan and must NOT be a sped-up full Adhan.

## Source
Preferred verified source:
- Wikimedia Commons: `Adhan, Great Mosque of Mecca - Jan 21, 2013.webm`
- Description: Adhan at the Great Mosque of Mecca / Makkah
- Author attribution on Commons: Seyfula Islam
- License: CC BY 3.0 Unported
- Source page:
  https://commons.wikimedia.org/wiki/File:Adhan,_Great_Mosque_of_Mecca_-_Jan_21,_2013.webm

Arihna may make a derivative clip under CC BY 3.0 provided attribution, license link and modification notice are preserved.

## Required behavior
1. `TAKBIR_X2` uses a clean derived clip from the verified Makkah source.
2. Audible content is exactly the first two takbīr phrases and then silence/stop.
3. No shahada, `حي على الصلاة`, or any later Adhan phrase may be audible.
4. Do not time-compress/speed up the recitation.
5. Trim leading/trailing dead air conservatively without clipping speech.
6. Preserve per-prayer local playback gain 0–100%.
7. Preserve all other Adhan variants unchanged.
8. Preserve daily inspiration notification behavior unchanged.
9. Preserve prayer scheduling, alarms, Quran, Location and Qibla unchanged.
10. Update `docs/audio/ADHAN_SOURCES.md` with source, author, CC BY 3.0, derivative-clip notice, source URL, exact source/derived SHA-256 and byte size.

## Validation
- Build must reproduce the exact derived clip deterministically from a pinned source byte hash or consume a separately pinned verified derived asset.
- Static gate verifies source/derived hashes and provenance.
- Unit tests verify `TAKBIR_X2` is one playback of the derived two-phrase clip (not repeat of an amateur one-phrase sample).
- API28 and API36 exact-SHA gates must pass.
- Publish S25 prerelease only after green gates.
- Physical Galaxy S25 validation must confirm audio quality and exactly two takbīr before stable publication.
