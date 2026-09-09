

### 2026-09-09 — Corano: supporto Mushaf Tajwid a colori — APPROVED / DATASET GATE REQUIRED

This Quran-only step starts from approved runtime `99ed8a0ae257f917443dffd6315057b967fdec9c` and extends the already-approved Ḥafṣ / Warsh Muṣḥaf architecture with a **real Tajwid-coloured Muṣḥaf presentation**. Prayer, Location, Qibla, Alarm behavior, GeoNames, package identity, SDK levels and signing policy are frozen. The existing Galaxy S25 Quran fixes remain mandatory: the non-immersive `Lettura` control must stay above and clickable while the preview card is visible; the immersive top bar must continue to apply `displayCutout ∪ statusBars`; and `android:windowLayoutInDisplayCutoutMode="shortEdges"` remains required.

#### Dataset gate precedes implementation

No Tajwid asset may be added merely by recolouring Arihna's existing Ḥafṣ text or SVGs. Before any implementation asset is committed, the candidate source must be verified against an exact upstream repository plus immutable commit/tag and must establish all of the following from primary upstream evidence: redistribution license/permission suitable for bundling in a free Android APK, exact riwāya, provenance, format, page count/completeness, metadata availability, first/last page presence and attribution requirements. Prefer a complete 604-page Ḥafṣ ʿan ʿĀṣim Madinah-style Muṣḥaf in SVG; high-resolution raster is acceptable only if the SVG objective is unavailable and measured rendering/size remains practical. Repository names, license terms, page counts, riwāya and provenance must never be inferred or invented.

Candidate investigation must start with `quranpedia/quran-svg`, King Fahd Complex resources, Tanzil and Quran.com / Quran Foundation open-source data, then other established Quran datasets if needed. If no candidate has unambiguous permission for digital redistribution inside Arihna, **stop before asset inclusion**. In that case the technical implementation is not authorized; document the licensing blocker and propose the best truthful alternative (for example verified Tajwid metadata rendered as text) without presenting it as a printed-Muṣḥaf-equivalent dataset.

#### Selection and terminology

The Quran header keeps the current coherent mode selector and adds a clearly identifiable Tajwid choice. If, as preferred, the validated dataset is Ḥafṣ ʿan ʿĀṣim with Tajwid colouring, the visible option is **`Ḥafṣ Tajwid`** (a compact `Tajwid` label is acceptable only where the surrounding Ḥafṣ context is unambiguous). It must **not** be called a new riwāya. The UI/domain must distinguish:

- **riwāya** — recitation/transmission identity such as Ḥafṣ or Warsh;
- **Muṣḥaf/page edition** — the concrete page artwork/layout dataset;
- **rendering variant** — standard versus Tajwid-coloured presentation where applicable.

`Facile` remains the existing verified Unicode/Tanzil reading mode and is not a page edition.

#### Edition-aware data model

The runtime must stop using `QuranRiwaya` as the sole identity for a rendered Muṣḥaf. Introduce an Arihna-owned stable edition model equivalent in semantics to:

```text
QuranRiwaya
- HAFS
- WARSH

QuranRenderingVariant
- STANDARD
- TAJWID

QuranEdition
- stableId
- riwaya
- renderingVariant
- asset/page dataset identity
- page count / verified metadata capabilities

stable editions
- HAFS          = (HAFS, STANDARD)
- HAFS_TAJWID   = (HAFS, TAJWID)
- WARSH         = (WARSH, STANDARD)
```

Exact Kotlin naming may differ if the semantics and stable persisted ids remain explicit. `HAFS_TAJWID` is an **edition key**, not a third riwāya. Rendering asset lookup, page cache keys, surah metadata, supported index capabilities, attribution and reading state must all take the explicit edition rather than deriving identity from riwāya alone.

#### Bookmarks, last position and recent history

Bookmark, last-read and recent-page persistence becomes edition-aware. The existing Ḥafṣ and Warsh namespaces must migrate without loss. A Tajwid bookmark/page/history entry must not replace, corrupt or appear as a standard Ḥafṣ or Warsh entry merely because the numeric page matches.

Until the validated Tajwid source proves an exact authoritative page/ayah correspondence with Arihna's existing Ḥafṣ edition, `HAFS_TAJWID` uses a fully independent reading-state namespace, conceptually equivalent to `lastPage_HAFS_TAJWID`, independent bookmarks and independent recent pages. Do not infer shared state from both editions being called Ḥafṣ or both having 604 pages. Any future shared/conversion behavior requires an explicit verified mapping and a later SPEC decision.

#### Search, Sura/Juz/Hizb and jump semantics

Every search/index/jump operation must be driven by metadata authoritative for the selected edition. Surah search/index uses that edition's verified surah/page metadata. Juz and Hizb navigation are enabled for `HAFS_TAJWID` only when the chosen dataset or another pinned authoritative source proves the boundaries for that exact page edition; do not silently reuse standard-Ḥafṣ or Warsh page/ayah assumptions. If authoritative Juz/Hizb metadata is absent, expose the same kind of honest controlled unavailability already used for Warsh rather than fabricating boundaries.

Jump-to-page remains valid only inside the selected edition's verified page range. Ayah/page/surah identities must never be automatically converted across `HAFS`, `HAFS_TAJWID` and `WARSH` without a real mapping dataset. Full-text Warsh/Tajwid search is not invented by this step; the existing search scope remains metadata/index search unless an independently verified textual source is integrated and specified.

#### Rendering and reader behavior

The validated Tajwid artwork must preserve its source colours exactly enough to represent the upstream Tajwid rules; Arihna must not apply arbitrary rule colours. The existing page reader architecture remains the functional baseline:

- correct RTL `HorizontalPager` behavior;
- offline page rendering;
- normal page reader plus immersive fullscreen reader;
- pinch zoom up to the existing maximum and pan while zoomed;
- pager scrolling disabled while the zoom/pan gesture owns the page;
- bookmark action in normal and immersive reading;
- edition-specific last position and recent history;
- Surah/Juz/Hizb/index capabilities only where authoritative metadata exists;
- current page/surah context without fabricated metadata;
- exact preservation of the existing `Lettura` z-order/clickability fix and Galaxy S25 cutout-safe immersive top chrome.

If SVG is used, preserve real source fills/strokes and use an edition-aware bounded parsed-picture cache. If raster is the only legally usable option, decoding/downsampling/caching must avoid avoidable full-set memory residency. No network fetch is allowed at reading time; the delivered edition is fully offline.

#### Attribution and licensing

Credits remain separate by dataset and must state only verified rights/provenance:

- existing Ḥafṣ page artwork: current pinned `batoulapps/quran-svg` attribution/license;
- existing Warsh artwork/metadata: current KFQC/Quranpedia terms already recorded by Arihna;
- Tajwid edition: exact new upstream, commit/tag, rights statement/license and required notice/attribution from the dataset gate.

Do not label a Tajwid dataset MIT merely because Arihna or another repository containing metadata uses MIT elsewhere. Preserve any required upstream NOTICE/license file in generated assets or repository documentation as appropriate.

#### Gradle asset pipeline and integrity gate

Extend the existing deterministic Quran asset-preparation pipeline only after dataset approval. The Tajwid source must be pinned immutably and build preparation must fail closed if integrity does not match the approved contract. The definitive gate must verify at minimum:

- exact Tajwid upstream repository and pinned commit/tag encoded in build/repository provenance;
- exact expected page count (604 when the selected edition is a classic 604-page Madinah layout);
- numeric first and last page assets present and parseable/readable;
- no missing/duplicate page numbers across the expected range;
- exactly 114 surahs in the selected edition's authoritative surah metadata when such metadata is part of the approved dataset;
- expected ayah total only when authoritative for that riwāya/source, never copied from another edition by assumption;
- all index/boundary metadata actually exposed by the UI is present and structurally validated;
- required license/NOTICE/attribution files and expected identifying terms;
- pinned-source commit/tag has not drifted.

A directory-exists check alone is not acceptance evidence.

#### Performance / APK-size acceptance

Measure rather than guess the cost of the Tajwid edition. The gate/release report records at least generated Tajwid asset bytes, signed APK byte size and delta versus the approved baseline where practical. Exercise representative first-open and adjacent-page navigation so the chosen parser/cache path does not eagerly retain the whole Muṣḥaf. API28 instrumentation/build must remain free of OOM. Galaxy S25 validation must include normal and immersive page open, several page turns, zoom/pan and edition switching. Correct religious rendering and source integrity take priority over compression tricks; if the validated dataset makes the APK unusually large, measure the real result before proposing a different packaging strategy.

#### Instrumentation acceptance

Add/update Android instrumentation so the exact technical candidate verifies at least:

A. `Ḥafṣ Tajwid`/`Tajwid` selector is visible and clickable.
B. `Ḥafṣ → Ḥafṣ Tajwid → Warsh → Ḥafṣ` completes without crash or state bleed.
C. A real Tajwid page opens and the test can distinguish its Tajwid edition asset path/state from standard Ḥafṣ.
D. Tajwid opens in immersive fullscreen while preserving safe top inset handling.
E. Tajwid bookmarks are edition-aware and isolated from standard Ḥafṣ/Warsh unless an explicitly verified mapping has separately authorized sharing.
F. Tajwid last position persists/restores independently.
G. Tajwid recent history persists/restores independently.
H. Existing standard Ḥafṣ continues to render/navigate.
I. Existing Warsh continues to render/navigate.
J. `Lettura` remains clickable while `quran-premium-page-frame` is visible.
K. Immersive top chrome continues to honor injected/real `displayCutout + statusBars` safe area.
L. RTL pager direction/identity does not regress across editions.
M. Existing zoom/pan gesture ownership and pager-disable behavior does not regress.

No Quran instrumentation test may be hidden with `@Ignore` or accepted as skipped in the definitive gate.

#### Exact-SHA gate, promotion and prerelease

This documentation-only specification commit is the **single specification parent** for the Tajwid technical candidate. The implementation candidate must be exactly one commit whose direct single parent is this SPEC SHA. If any candidate fails, every replacement candidate must be rebuilt as a sibling whose direct single parent is this same SPEC SHA; a failed candidate may never become the parent of its replacement.

Before promotion, the exact candidate SHA must pass the complete Arihna gate: static/policy checks, unfiltered JVM/unit regression, exact candidate debug build, full Android 9/API28 instrumentation with zero failures/errors/skips, and the existing modern API36 capability/permission matrix without regressions. The gate additionally enforces the Tajwid dataset integrity/licensing/provenance contract above, existing Ḥafṣ and Warsh integrity, frozen GeoNames/Adhan invariants, `applicationId = com.archimedeprojects.arihna`, `minSdk = 28`, `compileSdk = 37`, `targetSdk = 37`, and the established permission/dependency scope. Prayer, Location and Qibla files/behavior remain out of scope.

Only an all-green exact-SHA candidate may move `main`, and `main` must advance by a **non-forced fast-forward**. Build the validation APK with Arihna's persistent signer, verify package/SDK identity and signer, publish a GitHub **prerelease**, download the published APK again, and verify that the downloaded bytes have the same SHA-256 and signer as the locally gated artifact. Only after those checks exist may the APK be described as verified or its direct link be handed off. The final report records SPEC SHA, candidate/runtime SHA, exact gate run and static/API28/API36 job results, prerelease tag, direct APK link, APK SHA-256, verified signer fingerprint and APK byte size.
