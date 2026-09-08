

### 2026-09-08 — Corano: S25 toolbar/cutout + supporto multi-riwaya (Warsh) APPROVED

Physical review on the primary Galaxy S25 of runtime `23b9a5a0a7ea372ddb5d68e16a0027fd09a40231` identified two Quran presentation defects and authorizes one additional Quran-only feature step. This change is strictly scoped to Quran plus the minimum app-window theme flag needed for correct edge-to-edge cutout handling. **Prayer, Location and Qibla are frozen and must not be changed.**

#### STEP A — Corano S25: barra Lettura sempre raggiungibile + cutout immersivo

1. **Non-immersive Muṣḥaf toolbar/preview overlap.** The row containing current surah/page context, visual style (`Classico`/`Pulito`), `Lettura` and bookmark is a persistent interactive toolbar above the page preview. The preview/premium page card must never draw over, obscure or intercept this toolbar in any scroll/page state. The implementation must use explicit sibling z-order plus clipping/padding, or an equivalent Compose layout contract, so the toolbar remains visually above the preview and `quran-reading-fullscreen` remains clickable while `quran-premium-page-frame` is visible.
2. **Immersive top-bar safe area.** The immersive Quran top bar (close X, Arabic surah title, page number, bookmark) must honor both the display cutout and status-bar safe area. Apply `Modifier.windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars))`, or `safeDrawingPadding()` only if demonstrably equivalent in the current Compose setup. The layout must also remain correct on devices with no cutout.
3. **Edge-to-edge cutout mode.** `Theme.Arihna` must explicitly set `android:windowLayoutInDisplayCutoutMode="shortEdges"` so existing immersive edge-to-edge behavior is coherent with safe-area handling. No unrelated permission/manifest change is authorized.
4. **Regression coverage.** Android instrumentation must render a visible premium preview and then activate `quran-reading-fullscreen`, proving the visible preview does not block the reading control. The immersive top-bar safe-inset contract must be covered structurally/in instrumentation where injectable and remains a mandatory real-device validation on the Galaxy S25 camera-hole cutout.

#### STEP B — Corano: supporto multi-riwaya (Warsh)

Add **Warsh ʿan Nāfiʿ** as a second Muṣḥaf riwāya beside the existing **Ḥafṣ ʿan ʿĀṣim**. Selection is made directly in the Quran header with a dedicated `Muṣḥaf Warsh` chip adjacent to the existing `Muṣḥaf Ḥafṣ` / `Facile` controls; the selected riwāya persists through the Quran reading preferences. `Facile` remains the existing verified Unicode/Tanzil reading mode and must not be relabeled as Warsh.

**Pinned Warsh dataset.** Use `quranpedia/quran-svg` commit `b91d39e1065b57bdda3e94aca8ecf3575e50e1e6`, folder `mushafs/warsh/kfqc`. Acceptance requires exactly **604 numeric SVG pages**, **114 surahs**, and the Warsh per-surah ayah counts summing to **6214**. The Warsh page artwork is the King Fahd Glorious Qur'an Printing Complex (KFQC) digital Muṣḥaf; it is **not MIT**. Quranpedia's polygon/JSON metadata is CC0 1.0, while KFQC permits free digital/app/web/software/media use and restricts physical print-for-commercial-sale of muṣḥafs. Preserve the upstream NOTICE terms in generated assets and show separate in-app credits for Warsh. The existing Ḥafṣ visual set remains the pinned `batoulapps/quran-svg` MIT set with its existing separate credit.

**Riwāya-aware state/data contract.** Ḥafṣ uses the Kufan count (**6236**) and Warsh uses the Madani-last count (**6214**); therefore no ayah/page/surah identity may be silently treated as a 1:1 cross-riwāya mapping. Every Muṣḥaf lookup must take an explicit riwāya. In particular:

- surah index/search uses that riwāya's own `surah.json`, including its page starts and ayah counts;
- rendered page assets are selected by riwāya (`Ḥafṣ` vs `Warsh`) and page number;
- bookmarks are stored in separate per-riwāya namespaces. A Ḥafṣ bookmark never appears as a Warsh bookmark merely because the numeric page is the same, and vice versa;
- last-read page and recent-page history are stored separately per riwāya; existing v1 Ḥafṣ preferences must migrate/fallback transparently into the Ḥafṣ namespace so current users do not lose their state;
- changing riwāya restores that riwāya's own last position and own bookmark/recent set;
- no automatic bookmark/ayah conversion between Ḥafṣ and Warsh is authorized in this step. Any future equivalence needs an explicit verified mapping dataset rather than an invented fallback.

**Juz/Hizb honesty.** Existing Ḥafṣ Juz/Hizb navigation remains unchanged. Until an authoritative Warsh Juz/Hizb boundary index is integrated and gated, Warsh must not reuse Ḥafṣ ayah-boundary metadata as though it were Warsh. The Warsh index may expose surah/page/bookmark/recent navigation while omitting Juz/Hizb boundary navigation with a clear non-authoritative-avoidance message. The immersive Warsh chrome may show page/surah context without fabricated Juz/Hizb values.

**Search scope.** The current Quran search is the surah/index search and must search the selected riwāya's own metadata. This step does not invent a Unicode Warsh full-text search corpus. Any future ayah-text search for Warsh requires a separately verified Warsh textual dataset.

**Instrumentation acceptance.** Tests must verify: (1) switching from Ḥafṣ to Warsh actually renders Warsh and preserves the existing Ḥafṣ flow; (2) a bookmark created in one riwāya is absent in the other and reappears when switching back; (3) each riwāya restores its own last page/recent state; (4) the non-immersive `Lettura` overlap regression stays fixed with preview visible; and (5) the immersive top chrome uses the display-cutout/status-bar safe-inset contract. Existing RTL paging, pinch zoom/pan, premium style selection and 604-page behavior remain required.

**Gate/release.** Implementation must be one clean candidate whose direct single parent is this specification commit. Before promotion, the exact candidate SHA must pass the full host/JVM/build gate, Android 9/API28 complete instrumentation with zero failures/errors/skips, the existing API36 location-permission matrix unchanged, frozen GeoNames/signing/package/SDK/Quran-Hafs integrity checks, and new Warsh dataset/licensing checks. Only an all-green exact-SHA candidate may fast-forward `main`. Publish a persistent-signed GitHub prerelease and stop for real Galaxy S25 validation of the reading toolbar, camera-hole cutout and Warsh switching/bookmarks.
