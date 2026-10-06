# Windows dashboard validation

The desktop-owned dashboard retains mobile Aurum/Vazirmatn/RTL primitives and renders actual desktop personal holdings. It does not equate those with mobile business inventory.

Automated coverage:

- Historical JSON: integer rial/toman conversion, chronological timestamps, malformed/negative/future data rejection and insufficient-data state.
- In-memory state: cache reuse, failed refresh retains real snapshot/error, Tehran day rollover rejects yesterday's intraday series, obsolete canceled request cannot replace the current selection or failure.
- Large historical series: drawing selects actual extrema and endpoints into at most 402 points; the full source remains available to the selection slider.
- Optional motion preference: defaults on old documents and survives reopen without losing holdings.
- Compose desktop: 1400×980 light/dark and 940×700 compact screenshots, all requested dashboard sections, keyboard-accessible point selection, horizon change, scroll to chart/invoices, interrupted navigation with retained settings draft, reduced page-motion navigation.
- Existing desktop tests exercise calculator inputs across destinations, settings save, asset editing/search/deletion, updater prompts and helper behavior.

Snapshots contain synthetic fixtures only. Source uses TGJU intraday today and dated closes for week/month; it never substitutes yesterday's closes for an intraday chart. Network smoke must report only counts/timestamp ranges, not full provider payloads. History survives only this process, not restart. Invoice writer/import/sync are outside this slice; the invoice section explicitly describes this capability rather than displaying a fabricated empty account.

Required delivery gates: local Android Kotlin compile and unit tests, desktop tests, release-plan tests, desktop distributable and packaged `--verify-runtime`; local APK assembly/signing remains forbidden. This desktop-only feature publishes a Windows artifact with Android jobs skipped.

Verified locally for 0.56.39: Android compile passed, 237 Android unit tests and 53 desktop tests passed, 11 release-planning tests passed, distributable/runtime verification passed. The packaged historical adapter fetched 4,590 actual intraday points (401 drawing points), seven daily-close points and 30 daily-close points for the three horizons. Compact-window large-balance coverage checks all rendered characters and actual glyph width; desktop paragraph-width metadata can retain maximum constraints despite a smaller intrinsic text size. No device/emulator run or local APK assembly was performed.

Graph coverage is the current worktree's desktop Kotlin source. Structural extraction is partial for existing workspace/updater StateFlow constructs; their source and direct callers are inspected separately. Documentation is inspected directly rather than inferred from structural edges. No exhaustive call coverage or manual Android-device test is claimed.
