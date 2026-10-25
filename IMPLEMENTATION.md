# EzMenu 2.0 implementation

Approved: offline private cookbook, warm editorial light/dark design, personal journal and immutable cooking snapshots.

- [x] Database v2 + migration + snapshots + honest seed
- [x] Persistent cooking sessions and alarms
- [x] Editorial adaptive UI: library, inspiration, journal, profile, detail
- [x] Autosaved editor, groups, photos, timers, batch preview
- [x] Sharing, Markdown/PDF, archive restore, trash
- [x] Tests, API 26 / 37.2, visual matrix, performance, portfolio

Baseline source archive: build/v2-baseline.zip. No Git repository exists. Preserve all v1 data.

2026-10-23: First complete API 37 run: 25 device tests PASS. Existing 19 unit tests PASS; 3 timer tests added pending final run. Root navigation bug fixed by explicit Main dispatcher after suspend Room calls. Final asset, theme and extended export changes require final rerun. API26 official image download in progress; 360/412/840dp and font 2.0 visual matrix pending.

Latest: API26 official image installed and emulator-5556 booted. Matrix exposed a real LazyGrid header/item-count race when resizing; library header consolidated into one stable keyed item, key(wide) resets span layout. New restore I/O rollback also retains active sessions. Latest debug/test builds must be regenerated and matrix rerun; final complete 27-device-test suite pending.

Final full candidate verification: 22 unit tests PASS; 27 instrumentation tests on API26 and 27 on API37 PASS; eight visual matrix configurations PASS; lint 0 errors, 8 existing version hints; Debug and Release PASS. Video portfolio/ezmenu-demo.mp4 recorded. API26 force-stop probe PASS with v2 session/timer/editor draft. Last test-only teardown cleanup improved to delete its journal/session rows; recompile test APK and run UI regression class once. No further product code changes planned.

Completed: final 28 tests/device PASS on API26 and API37 (56 total); 22 unit tests; eight matrix configurations PASS; both actual force-stop probes PASS. Added actual OS alarm/permission fallback regression. Last lint-only recheck is running. dist contains preview and unsigned Release APKs with SHA-256.

FINAL: Added one actual background alarm / denied-permission fallback regression. Both API26 and API37 now pass all 28 device tests (56 total). Final lint-only rerun PASS, still eight version hints. Test cleanup removes its independent journal/session records. Both restart probes pass. Product code and dist artifacts complete.

Removed eight unused prototype UI/navigation files after confirming all references belonged to the obsolete NavHost. Source copies retained under build/validation/obsolete-ui plus full baseline ZIP. Actual studio UI is unchanged; final build/lint/device check being regenerated, then recopy dist checksums.

DELIVERED FINAL CANDIDATE: obsolete prototype pages removed; local no-backup random nonce replaces hardware ID use. Final 22 JVM tests, 28 device tests on each API26/API37, lint zero errors/eight retained version hints all PASS. Final Release assemble PASS; both final nonce process-restart probes PASS. dist APKs and SHA256SUMS refreshed from these exact builds. No required implementation or validation work remains.

2026-10-24 VISUAL REFINEMENT: Removed promotional headings and redundant identity/explainer copy. Restored two-column photographic recipe cards; refined detail, editor, journal, profile, navigation, placeholders and export card. Added 14 generated offline WebP assets (3.3MB), replaced procedural art, retained legacy resource references. Fixed icon clipping at 200% font scale without reducing text scaling. Final 22 JVM tests PASS; API26 and API37 each pass all 28 instrumentation tests; all eight visual matrix cases PASS; lint 0 errors / 8 retained version hints; Debug and unsigned Release rebuilt and copied to dist with refreshed SHA-256. Final screenshots: shots/refinement/matrix and portfolio/.

2026-10-24 BRAND / TYPE / MOTION: Restored the 旬食 brand heading and a native line-art culinary mark. Rebuilt bilingual serif and sans fonts at true 400/500/600 weights, complete GB2312 plus project glyphs; removed synthetic bold and unwanted font padding. Added shared cover transitions, press/favorite/filter/navigation/step animations and unobtrusive save feedback. Fixed Snackbar clearance for recipe/new and recipe/{recipeId}/edit so primary actions remain usable. Final 22 unit tests and 30 instrumentation tests on each API26/API37 PASS; eight visual configurations PASS; lint zero errors and eight retained version hints. Export fonts and palette synchronized. Final artifacts refreshed in dist.
