# GeoNames city database pipeline

Arihna's manual-city database is generated offline from GeoNames `cities500` plus selected alternate names. Runtime city search never calls an online geocoder.

## Populated-place feature filter

`cities500.txt` is an upstream candidate source, not Arihna's final city set. Arihna requires feature class `P` and applies a deny-by-default whitelist to feature code (column 7):

- `PPL`
- `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5`
- `PPLC`, `PPLG`
- `PPLF`, `PPLR`, `STLMT`

Explicitly excluded examples include `PPLX` (section of a larger populated place), `PPLH`/`PPLCH` (historical), `PPLQ` (abandoned), `PPLW` (destroyed), `PPLL` (minor locality) and `PPLS` (aggregate populated places). Unknown/future codes remain excluded until reviewed. Official reference: https://www.geonames.org/export/codes.html

The generator metadata retains raw/included/excluded counts and per-code distributions so upstream classification changes are auditable.

## Lossless runtime-minimal storage profile

No city or alias is removed for footprint optimization.

The final runtime schema uses:

- country/region display names in the existing `country` and `admin1` lookup tables; city keeps their short text codes because replacing those codes with numeric ids saved only about 34 KB after compression in the pre-benchmark;
- `latitude_e6` / `longitude_e6` signed SQLite INTEGER microdegrees instead of REAL; the E6 staging generator rejects coordinates that would require silent rounding and validates exact E6 round-trip for every row;
- a small `timezone(id, name, api28_compat_name)` lookup, with `city.timezone_id` numeric. `timezone.name` always remains the modern GeoNames/IANA identifier; `api28_compat_name` is nullable and only contains an explicitly reviewed compatibility id;
- `city.api28_time_zone_supported` as a 0/1 marker for the approved Android 9/API28 baseline. Runtime still tries the modern id first, so this baseline marker does not make a city permanently unsupported on newer tzdata;
- `population` as SQLite INTEGER for ranking/disambiguation;
- only `city_lat_lon_idx(latitude_e6, longitude_e6)` as an explicit runtime secondary index. `findById` uses the city INTEGER PRIMARY KEY; `search` uses FTS4 then INTEGER PRIMARY KEY joins; `nearest` uses the retained coordinate index;
- final `city_alias` without preprocessing-only UNIQUE/secondary indexes. Alias uniqueness is enforced in validated build-time staging, then every already-deduplicated alias row (including its id) is copied losslessly into the read-only runtime database;
- `city_search` unchanged as FTS4 `content=''` with `docid = city_alias.id`.

The runtime-minimal optimizer performs a full logical row comparison against the validated E6 staging database after joining numeric timezone ids back to IANA text, verifies city/alias/timezone counts, checks for duplicate alias pairs and orphan timezone ids, and exercises representative SQL for `findById`, FTS `search`, and `nearest`. `EXPLAIN QUERY PLAN` must show FTS usage for search and `city_lat_lon_idx` for nearest.

## Android 9 / API28 timezone compatibility

The frozen STEP 4 catalog has 391 distinct modern IANA timezone ids. The Android 9/API28 gate found five that `ZoneId.of(modernId)` cannot resolve on the baseline tzdata 2018e. Each was reviewed individually against IANA tzdb; Arihna does not use a generic alias map, fixed UTC offsets, longitude inference, or a custom timezone-rule engine.

Runtime resolution is capability-first: try the modern `timezone.name` first. Only when the platform does not recognize it may Arihna try the row's explicitly reviewed `api28_compat_name`.

### Category A — direct reviewed compatibility

- `Europe/Kyiv → Europe/Kiev`: official IANA rename in tzdb 2022b; `backward` retains `Europe/Kiev` as a Link to `Europe/Kyiv`, with identical rules. Sources: https://data.iana.org/time-zones/tzdb-2022b/NEWS and https://data.iana.org/time-zones/tzdb-2022b/backward
- `America/Ciudad_Juarez → America/Ojinaga`: **not an IANA Link**. `America/Ciudad_Juarez` was split from `America/Ojinaga` in tzdb 2022g. The mapping is safe specifically for the API28/2018e baseline because its `America/Ojinaga` still covered Juárez and projected Mountain/US-DST (`-07/-06`), matching modern Ciudad Juárez. Sources: https://data.iana.org/time-zones/tzdb-2022g/NEWS and https://data.iana.org/time-zones/tzdb-2018e/northamerica

### Category B — peer equivalence, not IANA Links

On 2026-08-30 the following pairs were compared automatically every six hours over the half-open UTC interval `2026-01-01T00:00:00Z` → `2101-01-01T00:00:00Z`, 109,572 samples per pair. Both comparisons produced zero offset discrepancies.

- `America/Coyhaique → America/Punta_Arenas`: modern Coyhaique is permanent UTC-03 after the Aysén change represented by tzdb 2025b. `America/Punta_Arenas` in tzdata 2018e is already permanent UTC-03 from 2016-12-04. Sources: https://data.iana.org/time-zones/tzdb-2025b/NEWS and https://data.iana.org/time-zones/tzdb-2018e/southamerica
- `Asia/Qostanay → Asia/Aqtobe`: modern Qostanay is UTC+05 after Kazakhstan unified on UTC+05 in 2024. The API28/2018e `Asia/Aqtobe` rules are UTC+05 without DST over the verified horizon. Sources: https://data.iana.org/time-zones/tzdb-2024a/NEWS and https://data.iana.org/time-zones/tzdb-2018e/asia

These peer mappings are compatibility assertions for the approved baseline and verified horizon, not claims that the IANA zone identities are aliases. Re-run the equivalence test if the minSdk/tzdata baseline or relevant IANA rules change.

### Category C — controlled unsupported baseline

`America/Nuuk` has no safe API28 compatibility mapping. Although `America/Nuuk` is the modern name of former `America/Godthab`, Android 9's tzdata 2018e projects `America/Godthab` as `-03/-02`; modern Nuuk uses `-02/-01` after Greenland's 2023 rule change. Mapping to Godthab on API28 would therefore be wrong by one hour. Sources: https://data.iana.org/time-zones/tzdb-2018e/europe and current IANA Greenland rules under https://data.iana.org/time-zones/

After applying the four approved Category A/B mappings, the exhaustive frozen dataset contains exactly one residual unsupported timezone id: `America/Nuuk`, affecting 17 of 224,330 cities. These rows are **not removed**. They remain searchable and nearest-discoverable, but on a runtime that cannot resolve the modern id they cannot materialize as a successful selectable `ManualCity`; selection produces `LocationFailure.UNSUPPORTED_TIME_ZONE`.

Exact frozen STEP 4 rows:

- `3424901` Aasiaat — Qeqertalik — Greenland
- `3423146` Ilulissat — Avannaata — Greenland
- `3422683` Kangaatsiaq — Greenland
- `3419714` Kangerlussuaq — Qeqqata — Greenland
- `3421982` Maniitsoq — Qeqqata — Greenland
- `3421765` Nanortalik — Kujalleq — Greenland
- `3421719` Narsaq — Kujalleq — Greenland
- `3421319` Nuuk — Sermersooq — Greenland
- `3421193` Paamiut — Sermersooq — Greenland
- `3420846` Qaqortoq — Kujalleq — Greenland
- `3420768` Qasigiannguit — Qeqertalik — Greenland
- `3420635` Qeqertarsuaq — Qeqertalik — Greenland
- `3420636` Qeqertarsuaq — Sermersooq — Greenland
- `3419842` Sisimiut — Qeqqata — Greenland
- `3424607` Tasiilaq — Sermersooq — Greenland
- `3418910` Upernavik — Avannaata — Greenland
- `3426193` Uummannaq — Avannaata — Greenland

Arihna preserves GeoNames' `America/Nuuk` assignment for these records; it does not reinterpret Greenland geography to manufacture another zone. On a future platform whose tzdata recognizes `America/Nuuk`, capability-first resolution succeeds natively and the API28 marker no longer blocks that city.

## Integrity validation

Do **not** use a global `PRAGMA integrity_check` as the FTS4 contentless gate. Since SQLite 3.44, global integrity checking invokes virtual-table `xIntegrity`; FTS4 cannot validate a `content=''` index against original content that is intentionally absent and may emit `unable to validate the inverted index ... SQL logic error` for a valid database.

References:

- https://sqlite.org/releaselog/3_44_0.html
- https://sqlite.org/releaselog/3_45_1.html
- https://sqlite.org/fts3.html

The runtime-minimal build therefore requires:

1. table-scoped `PRAGMA integrity_check('<table>')` = `ok` for `country`, `admin1`, `timezone`, `city`, and `city_alias`;
2. FTS document count equal to `city_alias` count;
3. zero orphan FTS docids and zero aliases missing from FTS;
4. golden `MATCH` coverage for Roma (`3169070`), Makkah/Mecca (`104515`), New York (`5128581`) and Sydney (`2147714`);
5. lossless logical equality with staging for every city and alias row;
6. exact API28 compatibility rows for the four approved mappings and exactly 17 `America/Nuuk` city rows marked unsupported on the baseline.

## APK-size measurement and exact benchmark provenance

The filtered baseline run `33280106118` measured 224,327 cities / 258,681 aliases and a 27,459,231-byte APK increment. A later E6-only run (`33292976302`) used an updated GeoNames `latest` payload, so cross-run deltas must not be mixed blindly.

For the decisive runtime-minimal AAPT measurement, the workflow intentionally reuses the exact validated E6 staging database from run `33292976302`, rather than downloading `latest` again. It verifies:

- staging DB SHA-256 `e567b7eabb40994d5d9fb95209c050503cf5747a6cdd25ef73d195aeb4003877`;
- 224,330 cities / 258,685 aliases / 391 timezone strings;
- the recorded source-file SHA-256 values from that run.

This makes the runtime-minimal result directly comparable with the E6 measurement from the same semantic snapshot. The APK build additionally hashes the decompressed `assets/geonames/cities.db` entry and requires it to equal the generated runtime DB hash, preventing a stale Gradle/AAPT asset from contaminating the result.

The APK already stores `cities.db` using ZIP method 8 (DEFLATE). Separate `.gz`/`.zst` precompression is not used because Android platform SQLite cannot open such a stream directly; it would require extraction and extra on-device storage/complexity.

If the real runtime-minimal APK increment is greater than 20 MiB, STEP 4 stops for review. If it is 20 MiB or less, the compact schema can be finalized and CityRepository/API28 validation may proceed.
