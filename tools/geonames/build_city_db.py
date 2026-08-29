#!/usr/bin/env python3
"""Build Arihna's deterministic offline GeoNames city database.

Generator version: 2
Inputs are local snapshot files downloaded from GeoNames. The caller is
responsible for checksum verification; this script records input and output
metadata and never fetches network resources itself.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import unicodedata
import zipfile
from pathlib import Path

GENERATOR_VERSION = 2
ALLOWED_LANGUAGES = {"it", "en", "ar"}
KIND_CANONICAL = 0
KIND_ASCII = 1
KIND_ALTERNATE = 2
NON_FTS_INTEGRITY_TABLES = ("country", "admin1", "city", "city_alias")
GOLDEN_SEARCH_ALIASES = (
    ("Roma", 3169070, "roma"),
    ("Makkah", 104515, "makkah"),
    ("Mecca", 104515, "mecca"),
    ("New York", 5128581, "new york"),
    ("Sydney", 2147714, "sydney"),
)


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def normalize_name(value: str) -> str:
    value = unicodedata.normalize("NFKD", value).casefold()
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    value = re.sub(r"[^\w\u0600-\u06ff]+", " ", value, flags=re.UNICODE)
    return " ".join(value.split())


def load_country_names(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            if not line or line.startswith("#"):
                continue
            cols = line.rstrip("\n").split("\t")
            if len(cols) >= 5 and cols[0] and cols[4]:
                result[cols[0]] = cols[4]
    return result


def load_admin1_names(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            cols = line.rstrip("\n").split("\t")
            if len(cols) >= 2 and cols[0] and cols[1]:
                result[cols[0]] = cols[1]
    return result


def open_text_member(zip_path: Path, member_name: str):
    zf = zipfile.ZipFile(zip_path)
    members = [n for n in zf.namelist() if not n.endswith("/")]
    if member_name not in members:
        zf.close()
        raise RuntimeError(f"Expected {member_name} in {zip_path}, found {members}")
    raw = zf.open(member_name, "r")
    import io
    text = io.TextIOWrapper(raw, encoding="utf-8", newline="")
    return zf, text


def validate_non_fts_integrity(db: sqlite3.Connection) -> dict[str, str]:
    """Run SQLite structural integrity checks on Arihna-owned non-FTS tables.

    Do not replace these with a global PRAGMA integrity_check while city_search
    remains FTS4 contentless. SQLite 3.44+ invokes virtual-table xIntegrity from
    the global pragma, and FTS4 cannot validate an inverted index against source
    content that content='' intentionally does not store.
    """
    results: dict[str, str] = {}
    for table in NON_FTS_INTEGRITY_TABLES:
        messages = [row[0] for row in db.execute(f"PRAGMA integrity_check('{table}')")]
        if messages != ["ok"]:
            raise RuntimeError(f"SQLite integrity check failed for {table}: {messages}")
        results[table] = "ok"
    return results


def validate_city_search(db: sqlite3.Connection, alias_count: int) -> dict[str, object]:
    """Functionally validate the FTS4 contentless search index.

    A contentless FTS4 virtual table cannot be reliably full-scanned to count or
    enumerate rows because the original column content is absent by design.
    FTS4's city_search_docsize shadow table is therefore used as the document
    roster: it contains exactly one row per indexed docid with this schema. The
    golden MATCH checks below exercise the actual inverted index and confirm that
    each expected city_alias.id is retrievable for representative aliases.
    """
    search_doc_count = db.execute("SELECT COUNT(*) FROM city_search_docsize").fetchone()[0]
    orphan_docids = db.execute(
        """
        SELECT COUNT(*)
        FROM city_search_docsize AS search_doc
        LEFT JOIN city_alias AS alias ON alias.id = search_doc.docid
        WHERE alias.id IS NULL
        """
    ).fetchone()[0]
    missing_docids = db.execute(
        """
        SELECT COUNT(*)
        FROM city_alias AS alias
        LEFT JOIN city_search_docsize AS search_doc ON search_doc.docid = alias.id
        WHERE search_doc.docid IS NULL
        """
    ).fetchone()[0]

    if search_doc_count != alias_count:
        raise RuntimeError(
            f"city_search document count mismatch: FTS={search_doc_count}, city_alias={alias_count}"
        )
    if orphan_docids:
        raise RuntimeError(f"city_search has {orphan_docids} orphan docids")
    if missing_docids:
        raise RuntimeError(f"city_search is missing {missing_docids} city_alias docids")

    golden_matches: list[dict[str, object]] = []
    for label, city_id, normalized_alias in GOLDEN_SEARCH_ALIASES:
        alias_rows = db.execute(
            "SELECT id FROM city_alias WHERE city_id=? AND normalized_alias=?",
            (city_id, normalized_alias),
        ).fetchall()
        if len(alias_rows) != 1:
            raise RuntimeError(
                f"Golden alias {label!r} expected exactly one city_alias row for "
                f"GeoNames id {city_id}, found {len(alias_rows)}"
            )
        expected_docid = alias_rows[0][0]
        matched_docids = {
            row[0]
            for row in db.execute(
                "SELECT docid FROM city_search WHERE city_search MATCH ?",
                (normalized_alias,),
            )
        }
        if expected_docid not in matched_docids:
            raise RuntimeError(
                f"Golden MATCH failed for {label!r}: expected docid {expected_docid} "
                f"for GeoNames id {city_id}"
            )
        golden_matches.append(
            {
                "label": label,
                "geonames_id": city_id,
                "normalized_alias": normalized_alias,
                "expected_docid": expected_docid,
            }
        )

    return {
        "alias_rows": alias_count,
        "fts_documents": search_doc_count,
        "orphan_docids": orphan_docids,
        "missing_docids": missing_docids,
        "golden_matches": golden_matches,
    }


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--cities", type=Path, required=True)
    p.add_argument("--alternate-names", type=Path, required=True)
    p.add_argument("--country-info", type=Path, required=True)
    p.add_argument("--admin1", type=Path, required=True)
    p.add_argument("--snapshot-date", required=True)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--metadata", type=Path, required=True)
    args = p.parse_args()

    for path in (args.cities, args.alternate_names, args.country_info, args.admin1):
        if not path.is_file():
            raise FileNotFoundError(path)

    country_names = load_country_names(args.country_info)
    admin1_names = load_admin1_names(args.admin1)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()

    db = sqlite3.connect(args.output)
    db.execute("PRAGMA page_size=4096")
    db.execute("PRAGMA journal_mode=OFF")
    db.execute("PRAGMA synchronous=OFF")
    db.execute("PRAGMA temp_store=MEMORY")
    db.executescript(
        """
        CREATE TABLE country(
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL
        ) WITHOUT ROWID;

        CREATE TABLE admin1(
            code TEXT PRIMARY KEY NOT NULL,
            country_code TEXT NOT NULL,
            name TEXT NOT NULL
        ) WITHOUT ROWID;

        CREATE TABLE city(
            id INTEGER PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            country_code TEXT NOT NULL,
            admin1_code TEXT,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            timezone_id TEXT NOT NULL,
            population INTEGER NOT NULL
        );

        CREATE INDEX city_country_idx ON city(country_code);
        CREATE INDEX city_admin1_idx ON city(admin1_code);
        CREATE INDEX city_lat_lon_idx ON city(latitude, longitude);
        CREATE INDEX city_population_idx ON city(population DESC);

        CREATE TABLE city_alias(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            city_id INTEGER NOT NULL,
            normalized_alias TEXT NOT NULL,
            source_kind INTEGER NOT NULL,
            language TEXT,
            preferred INTEGER NOT NULL DEFAULT 0,
            short_name INTEGER NOT NULL DEFAULT 0,
            UNIQUE(city_id, normalized_alias)
        );

        CREATE INDEX city_alias_normalized_idx ON city_alias(normalized_alias);
        CREATE INDEX city_alias_city_idx ON city_alias(city_id);
        """
    )

    city_ids: set[int] = set()
    referenced_countries: set[str] = set()
    referenced_admin1: set[str] = set()
    city_count = 0

    zf, cities_text = open_text_member(args.cities, "cities500.txt")
    try:
        with db:
            for line in cities_text:
                cols = line.rstrip("\n").split("\t")
                if len(cols) < 19:
                    raise RuntimeError(f"Malformed cities500 row with {len(cols)} columns")
                city_id = int(cols[0])
                name = cols[1].strip()
                ascii_name = cols[2].strip()
                latitude = float(cols[4])
                longitude = float(cols[5])
                country_code = cols[8].strip()
                admin1_raw = cols[10].strip()
                admin1_code = f"{country_code}.{admin1_raw}" if admin1_raw else None
                population = int(cols[14] or 0)
                timezone_id = cols[17].strip()
                if not name or not country_code or not timezone_id:
                    raise RuntimeError(f"Missing required city data for geonameId={city_id}")
                if not (-90.0 <= latitude <= 90.0 and -180.0 <= longitude <= 180.0):
                    raise RuntimeError(f"Invalid coordinates for geonameId={city_id}")

                db.execute(
                    "INSERT INTO city(id,name,country_code,admin1_code,latitude,longitude,timezone_id,population) VALUES(?,?,?,?,?,?,?,?)",
                    (city_id, name, country_code, admin1_code, latitude, longitude, timezone_id, population),
                )
                city_ids.add(city_id)
                referenced_countries.add(country_code)
                if admin1_code:
                    referenced_admin1.add(admin1_code)

                canonical = normalize_name(name)
                if canonical:
                    db.execute(
                        "INSERT OR IGNORE INTO city_alias(city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,1,0)",
                        (city_id, canonical, KIND_CANONICAL, None),
                    )
                ascii_norm = normalize_name(ascii_name)
                if ascii_norm:
                    db.execute(
                        "INSERT OR IGNORE INTO city_alias(city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,0,0)",
                        (city_id, ascii_norm, KIND_ASCII, None),
                    )
                city_count += 1
    finally:
        cities_text.close()
        zf.close()

    missing_countries = sorted(code for code in referenced_countries if code not in country_names)
    if missing_countries:
        raise RuntimeError(f"Missing country names for {missing_countries}")

    with db:
        db.executemany(
            "INSERT INTO country(code,name) VALUES(?,?)",
            ((code, country_names[code]) for code in sorted(referenced_countries)),
        )
        db.executemany(
            "INSERT INTO admin1(code,country_code,name) VALUES(?,?,?)",
            (
                (code, code.split(".", 1)[0], admin1_names[code])
                for code in sorted(referenced_admin1)
                if code in admin1_names
            ),
        )

    alt_seen = 0
    alt_inserted_before = db.execute("SELECT COUNT(*) FROM city_alias").fetchone()[0]
    zf, alt_text = open_text_member(args.alternate_names, "alternateNamesV2.txt")
    try:
        batch: list[tuple[int, str, int, str, int, int]] = []
        for line in alt_text:
            cols = line.rstrip("\n").split("\t")
            if len(cols) < 8:
                continue
            try:
                city_id = int(cols[1])
            except ValueError:
                continue
            if city_id not in city_ids:
                continue
            language = cols[2].strip()
            if language not in ALLOWED_LANGUAGES:
                continue
            alias = cols[3].strip()
            if not alias:
                continue
            is_historic = cols[7].strip() == "1"
            if is_historic:
                continue
            normalized = normalize_name(alias)
            if not normalized:
                continue
            preferred = 1 if len(cols) > 4 and cols[4].strip() == "1" else 0
            short_name = 1 if len(cols) > 5 and cols[5].strip() == "1" else 0
            batch.append((city_id, normalized, KIND_ALTERNATE, language, preferred, short_name))
            alt_seen += 1
            if len(batch) >= 5000:
                with db:
                    db.executemany(
                        "INSERT OR IGNORE INTO city_alias(city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?)",
                        batch,
                    )
                batch.clear()
        if batch:
            with db:
                db.executemany(
                    "INSERT OR IGNORE INTO city_alias(city_id,normalized_alias,source_kind,language,preferred,short_name) VALUES(?,?,?,?,?,?)",
                    batch,
                )
    finally:
        alt_text.close()
        zf.close()

    alias_count = db.execute("SELECT COUNT(*) FROM city_alias").fetchone()[0]
    alt_inserted = alias_count - alt_inserted_before

    with db:
        db.execute("CREATE VIRTUAL TABLE city_search USING fts4(normalized_alias, content='')")
        db.execute("INSERT INTO city_search(docid, normalized_alias) SELECT id, normalized_alias FROM city_alias ORDER BY id")
        db.execute("ANALYZE")
    db.execute("VACUUM")

    # Do not run global PRAGMA integrity_check here. On SQLite 3.44+ it calls
    # xIntegrity for FTS4, which cannot validate a contentless (content='')
    # index against source text that is intentionally not stored. That produces
    # "unable to validate the inverted index ... SQL logic error" even for a
    # valid, queryable index. Keep structural checks on all non-FTS tables and
    # validate city_search functionally instead.
    non_fts_integrity = validate_non_fts_integrity(db)
    city_search_validation = validate_city_search(db, alias_count)

    timezone_count = db.execute("SELECT COUNT(DISTINCT timezone_id) FROM city").fetchone()[0]
    blank_timezone_count = db.execute("SELECT COUNT(*) FROM city WHERE timezone_id='' OR timezone_id IS NULL").fetchone()[0]
    invalid_id_count = db.execute("SELECT COUNT(*) FROM city WHERE id<=0").fetchone()[0]
    invalid_coordinate_count = db.execute(
        "SELECT COUNT(*) FROM city WHERE latitude < -90 OR latitude > 90 OR longitude < -180 OR longitude > 180"
    ).fetchone()[0]
    db.close()

    metadata = {
        "generator_version": GENERATOR_VERSION,
        "snapshot_date": args.snapshot_date,
        "sqlite_version": sqlite3.sqlite_version,
        "inputs": {
            "cities500.zip": {"sha256": sha256(args.cities), "bytes": args.cities.stat().st_size},
            "alternateNamesV2.zip": {"sha256": sha256(args.alternate_names), "bytes": args.alternate_names.stat().st_size},
            "countryInfo.txt": {"sha256": sha256(args.country_info), "bytes": args.country_info.stat().st_size},
            "admin1CodesASCII.txt": {"sha256": sha256(args.admin1), "bytes": args.admin1.stat().st_size},
        },
        "output": {"sha256": sha256(args.output), "bytes": args.output.stat().st_size},
        "counts": {
            "cities": city_count,
            "aliases_total": alias_count,
            "alternate_candidates_it_en_ar": alt_seen,
            "alternate_inserted_unique": alt_inserted,
            "timezones_distinct": timezone_count,
            "invalid_coordinates": invalid_coordinate_count,
            "blank_timezones": blank_timezone_count,
            "invalid_city_ids": invalid_id_count,
        },
        "validation": {
            "non_fts_integrity": non_fts_integrity,
            "city_search": city_search_validation,
        },
    }
    args.metadata.parent.mkdir(parents=True, exist_ok=True)
    args.metadata.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
