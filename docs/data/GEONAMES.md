# GeoNames city database pipeline

Arihna's manual-city database is generated offline from the approved GeoNames `cities500` snapshot plus selected alternate names. Runtime city search does not call an online geocoder.

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

The workflow builds a baseline debug APK without the generated database, then builds another APK with `assets/geonames/cities.db` and measures the actual APK increment. If the increment is greater than 20 MiB, the Location STEP 4 pipeline must stop for explicit review; it must never silently switch from `cities500` to a smaller dataset.
