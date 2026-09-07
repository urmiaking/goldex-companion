# GoldEx Companion Agent Rules

This file governs autonomous changes to GoldEx Companion. Agents must read it before inspecting or modifying the repository. The implementation and `ARCHITECTURE.md` are the source of truth for current behavior; this file defines safe engineering behavior and migration constraints.

## 1. Non-negotiable safety rules

### 1.1 Preserve user data and signing identity

- Never delete, overwrite, regenerate, or replace `app/keystore/goldex-release.keystore`.
- Never change the application ID or release signing identity without an explicit migration plan.
- Never use destructive Git commands such as `reset --hard` or `checkout --` to discard user changes.
- Never delete or rewrite persisted customer, invoice, portfolio, or settings data without an import and rollback plan.

### 1.2 Protect secrets

- Never add credentials, API tokens, passwords, or private keys to source control.
- Do not copy existing secrets into new files or documentation.
- New release signing configuration must use protected CI secrets or environment variables.
- Do not log customer data, financial records, tokens, or complete external payloads.

### 1.3 Preserve product contracts

- Preserve Persian RTL behavior and Vazirmatn typography.
- Preserve the Persian Sovereign Aurum tokens and existing visual language.
- Preserve financial formulas, precision, and VAT policy unless the user explicitly requests a domain change.
- Preserve existing navigation and persisted data compatibility.
- Do not replace a working UI with a generic screen while migrating architecture.

## 2. Build and verification policy

- Before triggering GitHub Actions CI, run a local compile-only check using the fastest available Gradle task (e.g., `./gradlew compileDebugKotlin`) to catch syntax and compilation errors early.
- Do not run full Gradle builds, test suites, or APK assembly on the local machine. Those remain CI-only.
- Compilation, unit tests, instrumentation tests, APK assembly, signing, and release verification run in GitHub Actions.
- The workflow source of truth is `.github/workflows/build-and-release.yml`.
- Do not use arbitrary polling loops for GitHub Actions. Use `gh run watch` or `gh run view --watch`.
- Before a change is complete, run the narrowest available non-Gradle validation and inspect diagnostics. For code changes, CI must execute the relevant tests and build.
- Do not claim a build or test passed unless its output is available.

## 3. Current technology constraints

These are current implementation constraints, not permanent bans on future architecture:

- One Android `app` module.
- Jetpack Compose and Material 3.
- Kotlin coroutines and `StateFlow`.
- `HttpURLConnection` and `org.json` for the current network layer.
- SharedPreferences/JSON for current small local datasets.
- No Hilt, Room, Retrofit, OkHttp, Ktor, or navigation-compose is currently installed.

Agents may propose or introduce a replacement only when the feature's scale justifies it, the migration is explicit, and existing behavior/data remain compatible. Do not add a framework merely for fashion or to satisfy a generic clean-architecture template.

## 4. Feature implementation rules

Implement features vertically:

```text
UI -> feature state -> use case/domain policy -> repository contract -> data source
```

For every feature:

1. Identify the owning feature package.
2. Keep composables focused on rendering and event emission.
3. Keep business calculations out of composables.
4. Keep Android and network details out of pure domain code.
5. Add tests for behavior that can affect money, user data, or navigation.
6. Update `ARCHITECTURE.md` when a boundary or source of truth changes.

The current global `GoldCalculatorViewModel` is a migration state. New behavior must not make it larger unless there is no reasonable feature owner. Prefer extracting a feature state holder or use case at the next natural change point.

Already-established migration seams must be reused:

- Calculation policies belong in `domain/calculator/GoldCalculationUseCases.kt`.
- Portfolio valuation belongs in `domain/portfolio/PortfolioValuation.kt`.
- Repository consumers should depend on contracts from `data/RepositoryContracts.kt`.
- Dashboard rendering consumes `DashboardUiState` rather than the global ViewModel.
- Portfolio UI emits explicit add/delete callbacks rather than creating its own ViewModel.
- Karat conversion state belongs in `ui/calculator/KaratConvertViewModel.kt`; the global ViewModel must not regain those fields or events.
- JSON persistence changes must use and extend `data/PersistenceJsonCodecs.kt` with compatibility tests.

## 5. State and event rules

- Expose immutable state to the UI.
- Use `StateFlow` for durable screen state and explicit event methods for user intents.
- Do not expose mutable repositories to composables.
- Do not pass `Context` into public ViewModel methods.
- Use application context only inside Android-owned repositories or an `AndroidViewModel` boundary.
- Keep transient UI state local when it has no business meaning.
- Do not duplicate ownership of the same state between screens or ViewModels.
- Dashboard data must be represented by state, not hidden constants in the composable.

## 6. Financial and localization rules

The official domain rules are:

```text
RawGoldValue = NetWeight * Spot18k * Karat / 750
WageAmount = RawGoldValue * WagePercent / 100
             or NetWeight * WagePerGram
ProfitAmount = (RawGoldValue + WageAmount) * ProfitPercent / 100
TaxAmount = (WageAmount + ProfitAmount) * TaxPercent / 100
TotalPayable = RawGoldValue + WageAmount + ProfitAmount + TaxAmount
```

- VAT must not be calculated on raw gold value.
- Weight supports 0.001g precision.
- New monetary contracts should use `Long` whole-toman values or an explicit money type instead of unexamined `Double` arithmetic.
- Rounding must be explicit and tested.
- Use `PersianNumberFormatter` for user-facing amounts, weights, percentages, dates, and identifiers where appropriate.
- Use `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` at the screen boundary.
- Keep numeric input semantics LTR where required for decimal entry, while labels and layout remain RTL.

## 7. Repository and network rules

- UI and domain code should depend on repository contracts as new features are extracted.
- Keep provider-specific parsing separate from market policy when practical.
- Preserve source, freshness, and live/offline status in market data.
- Use structured error states; do not silently convert every failure into fake live data.
- Set an explicit User-Agent and Accept header for APIs that require them.
- Always close readers/streams and call `disconnect()` in `finally` for HTTP connections.
- Network work belongs on `Dispatchers.IO` or an equivalent injected dispatcher.
- Validate remote values before using them in financial calculations.

## 8. Persistence and migration rules

SharedPreferences JSON is allowed for current MVP datasets. It is not a reason to block production preparation.

When adding or changing persisted data:

- Define a stable identifier.
- Define default values for older records.
- Make parsing tolerant of unknown fields.
- Add compatibility tests for existing JSON.
- Never silently discard malformed or unknown records.
- Document a migration path before changing the persisted shape.

Introduce Room or another durable store when query volume, relationships, migrations, or sync requirements justify it. The migration must import existing JSON, preserve IDs, support rollback/backup, and be tested before old storage is removed.

## 9. Cloud-sync readiness rules

Do not build a generic sync abstraction prematurely. Before implementing sync for a feature, document:

- identity and authentication
- local ownership
- stable IDs
- timestamps and deletion markers
- retry and offline behavior
- conflict resolution
- server and client authority

Different entities may use different conflict policies. Invoice sync must not automatically inherit portfolio sync rules.

## 10. Design-system rules

- Reuse `ui/theme/Color.kt`, `Type.kt`, and `Theme.kt`.
- Reuse existing components before creating variants.
- Use semantic colors for market positive, negative, warning, and neutral states.
- Keep card, border, typography, spacing, and motion decisions consistent with the existing Stitch design.
- Do not introduce raw one-off colors or typography in feature screens unless the design decision is documented.
- A design migration must not alter domain calculations or persistence behavior.
- In two-action dialogs/modals (RTL), the secondary/cancel action must always be on the right (first child in Row) and the primary/save action on the left (second child in Row) per `.agents/rules/dialog-button-layout.md`.

## 11. Testing rules

Required test priority:

1. Pure calculations, rounding, formatters, and financial policies.
2. Persistence serialization and migration.
3. Repository fallback and error behavior.
4. ViewModel event-to-state transitions.
5. Critical Compose workflows.

Tests must verify behavior, not implementation details. Every production feature should include the smallest meaningful test slice in the same change.

## 12. Documentation rules

### `ARCHITECTURE.md`

Documents current runtime structure, ownership, migration targets, known compromises, and architectural decisions. Update it when package ownership, persistence, state ownership, money representation, navigation, or sync changes.

### `README.md`

Remains user/developer onboarding documentation: purpose, current capabilities, setup, and release usage. Do not put agent workflow rules there.

### Architecture Decision Records

Use `docs/adr/` for durable decisions involving:

- persistence or database migration
- money representation and rounding
- cloud sync and conflict resolution
- authentication and security
- navigation or module boundaries

Each ADR contains context, decision, consequences, and revisit conditions.

## 13. Change protocol

Before editing:

1. Find the concrete behavior owner and nearby tests/call sites.
2. Check for uncommitted user work.
3. State one falsifiable hypothesis and one focused validation.
4. Make the smallest compatible edit.

After editing:

1. Run focused validation immediately.
2. Repair only the touched behavior before widening scope.
3. Confirm no persisted data, signing identity, or design token was unintentionally changed.
4. Report changed files, validation performed, and any remaining CI-only verification.

## 14. Forensic release checklist

- [ ] Existing behavior and persisted data remain compatible.
- [ ] No secrets or private keys were added.
- [ ] No local Gradle build was run.
- [ ] Relevant tests and diagnostics were checked.
- [ ] Financial formulas and rounding are covered.
- [ ] Persian digits and RTL behavior remain intact.
- [ ] Design tokens remain centralized.
- [ ] Dashboard values are not falsely presented as live data.
- [ ] Documentation matches the implementation.
- [ ] Cloud CI is ready to build, sign, and publish the artifact.
- [ ] Release signing credentials are supplied through protected CI secrets before broad public distribution.
