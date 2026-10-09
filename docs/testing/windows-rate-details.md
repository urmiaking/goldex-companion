# Windows instrument detail verification (0.56.49)

Issue: #270. Baseline e6df212, integrated origin/main inventory changes through 9e70604. Feature is desktop-only; app/core/shared-ui/toolchain/signing files remain unchanged against their own published baselines.

## Local results

- compileDebugKotlin --no-build-cache --no-daemon -q: exit 0.
- testDebugUnitTest --no-daemon -q: 237 tests / 35 suites, zero failures.
- :desktop:test --no-daemon -q: 124 tests / 23 suites, zero failures.
- :desktop:createDistributable --no-daemon -q: exit 0.
- Packaged Qirato.exe --verify-runtime: exit 0, version 0.56.49; font/icon/shared codecs/financial runtime passed.
- Release planner: 14 tests, zero failures.

22 focused tests cover instrument and horizon navigation, all 11 visible symbols, late/cancelled response isolation, unavailable and offline states, original cache timestamps after restart, exact ounce cents and Iranian toman parsing, malformed/future/invalid OHLC rejection, unknown fields/default compatibility/corrupt cache protection, 30-day simple average and actual OHLC extrema, Persian-year baseline and existing karat conversion for nominal gold premium. UI tests preserve calculator weights/manual drafts, table scroll, global header/update controls and keyboard/pointer chart access.

Deterministic fixture screenshots were reviewed at 1440x1080, 1280x900/1000 and 940x700, light/dark themes, including chart, statistics and honest unavailable documents. No fake invoice data is consumed. The latest integrated header remains shared, with the new inventory actions kept intact. Android SDK XML-version/image-decoder warnings were present; all required exits and XML results passed.

Evidence stays in output/rate-details: gate-status.json, per-gate logs, test-totals.json, runtime log/exit, planner results, retained test reports and desktop screenshots. CI and release metadata are appended there after publication; installers/APKs are never downloaded again for verification.

## Discovery coverage and limits

Current worktree Graphify was built from an explicit safe source/architecture/ADR scope, then incrementally updated for the new feature and merged header/shell: 413 nodes / 872 edges. Kotlin DesktopWorkspace extraction is partial; integrity checks report 82 external/dangling endpoints, 5 self-loops and collapsed multi-relation edges. Graph edges guide ownership, not proof of directed execution. Semantic usage counters were unavailable and recorded as zero placeholders. Exact codebase-memory search/snippets and source/tests confirm behavior; index coverage reports no recorded gaps for touched feature paths but metadata_changed freshness, and the live index occasionally returns stale symbol ranges after a merge. Current source/compiler/tests are authoritative.

Stitch get_screen returned the exact project/screen metadata and URLs. curl -L downloads redirected to Google sign-in HTML, so those responses were not treated as valid images/code. The user's two continuous screenshots are the implemented design reference.

## Capability limits

TGJU may omit a symbol or interval; actual cached history remains labelled with original time, and missing data stays unavailable. No volume-weighted mean, support/resistance or trading-risk labels are fabricated. Recent documents cannot be filled until real Windows invoice persistence and stable instrument associations exist; sample rows in the existing invoice screen are intentionally not imported. Explicit gold calculation applies the selected available basis while preserving other inputs.
