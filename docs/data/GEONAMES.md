# GeoNames city database pipeline

Arihna's manual-city database is generated offline from the official GeoNames `cities500` dump plus selected alternate names. Runtime city search does not call an online geocoder.

## Populated-place feature filter

The raw `cities500.txt` file is an upstream candidate source, not the final Arihna city set. GeoNames includes several feature-code semantics under class `P`; some are sections of larger cities, historical/abandoned/destroyed places, minor localities, or aggregate records that should not appear as independent manual-city choices.

Arihna therefore requires `feature class = P` and applies an explicit deny-by-default whitelist to column 7 (`feature code`). Any current or future code not listed below is excluded until separately reviewed.

Included codes:

- `PPL` — populated place.
- `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5` — seats of administrative divisions.
- `PPLC` — capital of a political entity.
- `PPLG` — seat of government of a political entity.
- `PPLF` — farm village. GeoNames defines this as a current populated place; the occupational character does not make it historical or non-inhabited.
- `PPLR` — religious populated place. GeoNames likewise defines it as a current populated place.
- `STLMT` — current inhabited settlement with a GeoNames-specific classification. Arihna preserves GeoNames-provided country and timezone fields and does not reinterpret geopolitical status.

Explicitly excluded examples:

- `PPLX` — section of a larger populated place; a district/neighborhood is not an independent city choice.
- `PPLH`, `PPLCH` — historical populated place / historical capital.
- `PPLQ` — abandoned populated place.
- `PPLW` — destroyed populated place.
- `PPLL` — minor populated locality rather than a city/town/village center for Arihna's manual-city UX.
- `PPLS` — aggregate/plural populated places rather than one independent center.

Official GeoNames feature-code reference: https://www.geonames.org/export/codes.html

The generator writes auditable filter evidence into metadata: raw row count, included/excluded row counts, raw feature-class and feature-code distributions, and per-code included/excluded counts. This makes future upstream classification changes visible instead of silently changing the bundled dataset.

## FTS4 contentless integrity validation

`city_search` intentionally uses FTS4 with `content=''` and `docid = city_alias.id`. This keeps only the inverted index needed for lookup and does not duplicate the alias text inside the FTS virtual table.

Do **not** replace the validation below with a global `PRAGMA integrity_check` while this schema remains contentless. Starting with SQLite 3.44.0, global `PRAGMA integrity_check` invokes virtual-table `xIntegrity` for FTS3/FTS4/FTS5 and other virtual tables. FTS4's integrity path attempts to validate the inverted index against source content; with `content=''`, that source content is intentionally absent, so a valid index can fail with `unable to validate the inverted index ... SQL logic error`. SQLite 3.45.1 release notes also document follow-up fixes for side effects introduced by the new virtual-table integrity checking behavior.

Official references:

- SQLite 3.44.0 release notes: https://sqlite.org/releaselog/3_44_0.html
- SQLite 3.45.1 release notes: https://sqlite.org/releaselog/3_45_1.html
- SQLite FTS3/FTS4 documentation: https://sqlite.org/fts3.html

The generator therefore uses two complementary gates:

1. Table-scoped `PRAGMA integrity_check('<table>')` for Arihna-owned non-FTS tables: `country`, `admin1`, `city`, and `city_alias`. This preserves SQLite's structural B-tree/index integrity checking for the ordinary database.
2. Functional validation for `city_search`:
   - `city_search_docsize` must contain exactly one document row for every `city_alias` row;
   - no indexed docid may be orphaned from `city_alias`;
   - no `city_alias.id` may be missing from the FTS document roster;
   - representative `MATCH` queries must return the exact expected `city_alias.id` for GeoNames city ids: Roma/Rome `3169070`, Makkah and Mecca `104515`, New York `5128581`, and Sydney `2147714`.

`city_search_docsize` is used only to enumerate/count indexed docids because an unfiltered scan of an FTS4 contentless virtual table itself requires unavailable stored content. The golden `MATCH` queries exercise the actual inverted index.

The generator writes the validation results and the Python SQLite version into its metadata JSON so each pipeline run retains auditable evidence.

## APK-size gate

The workflow builds a baseline debug APK without the generated database, then builds another APK with `assets/geonames/cities.db` and measures the actual APK increment. If the increment is greater than 20 MiB, the Location STEP 4 pipeline must stop for explicit review. It must never silently switch datasets or change the approved feature-code policy merely to pass the size gate.
