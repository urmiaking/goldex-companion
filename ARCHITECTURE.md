# GoldEx Companion Architecture

## 1. Purpose and architectural stance

GoldEx Companion is a production-oriented Android application implemented incrementally by vertical feature slices. It has an Android `app` host and a Kotlin Multiplatform `core` module with Android and JVM targets. The host uses Jetpack Compose, Kotlin coroutines, `StateFlow`, Room/local preferences, and HTTP market-rate integrations.

The architecture must support two truths:

1. Existing features and the Persian Sovereign Aurum UI must keep working unchanged.
2. Each new feature must be isolated enough to migrate from local-only MVP behavior to durable local storage, cloud synchronization, and production operations later.

Do not perform a broad rewrite to satisfy this document. Migrations are incremental and must preserve behavior at every step.

## 2. Current runtime architecture

```text
GoldexApplication -> AndroidAppContainer (process-lifetime adapters and ViewModel factory)
MainActivity
  -> injected ViewModels
  -> GoldExCompanionTheme
  -> AppLockScreen (when locked)
  -> MainViewModel (App Shell, Market Rates & Calculator Core)
  -> MainScreen
       -> DashboardScreen (DashboardUiState)
       -> LiveRatesScreen (MarketRatesUiState)
       -> MarketRateDetailScreen (MarketRateDetailState)
       -> JewelryTab (JewelryUiState & JewelryActions)
       -> KaratConvertScreen (KaratConvertViewModel)
       -> CoinBubbleScreen (MarketRates & explicit callbacks)
       -> MeltCalcScreen (MeltUiState & explicit callbacks)
       -> CustomerLedgerScreen (CustomerManagerViewModel)
       -> CustomerStatementScreen (CustomerManagerViewModel)
       -> InventoryScreen (InventoryViewModel & explicit callbacks)
       -> ReportingScreen (ReportingViewModel & explicit callbacks)
       -> MoreHubScreen (AppSettings & explicit callbacks)
       -> InvoicesManagementScreen & BarterInvoiceScreen (BarterInvoiceViewModel)
       -> OnboardingWizardScreen (WizardUiState & OnboardingViewModel events)
       -> Dialogs (CustomerPickerDialog, InvoiceManagerDialog, TaxProfitModal, PriceSourceModal, JewelerProfileModal, UpdateDialog, AddInventoryItemModal, AdjustStockModal)

Feature ViewModels & State Holders:
  -> AppLockViewModel (SettingsStore, BiometricAuthManager)
  -> ReportingViewModel (InvoiceStore, CustomerStore, InventoryStore, SettingsStore)
  -> InventoryViewModel (InventoryStore)
  -> CustomerManagerViewModel (CustomerStore)
  -> InvoiceManagerViewModel (InvoiceStore)
  -> BarterInvoiceViewModel (InvoiceStore, CustomerStore, BarterCalculationUseCases, InvoiceLedgerSyncUseCase)
  -> PortfolioManagerViewModel (PortfolioStore)
  -> SettingsViewModel (SettingsStore)
  -> UpdateViewModel (AppUpdateChecker)
  -> KaratConvertViewModel (GoldCalculationUseCases)
  -> MainViewModel (market/cache/source/connectivity ports, SettingsStore, injected work dispatcher)
  -> LicenseViewModel (LicenseStore)
  -> CloudSyncViewModel (Android SyncCoordinator)
  -> OnboardingViewModel (CompleteOnboardingUseCase)

core/commonMain model/ -> domain data types, calculations, formatting, market history & candlestick models, invoice aggregation
core/commonMain domain/ -> calculation policies (GoldCalculationUseCases, BarterCalculationUseCases, InvoiceLedgerSyncUseCase, PortfolioValuation, ReportingUseCases) and security contracts (BiometricAuthManager, BiometricStatus, BiometricAuthResult, AppLockState)
app data/ -> HTTP integrations, AndroidBiometricAuthManager, multi-provider market history (iSignal/TGJU fallback), 2-tier MarketRatesCache & MarketHistoryCache (in-memory + SharedPreferences disk persistence), Room database (GoldexDatabase, DAOs, Entities, Mappers) with automatic zero-data-loss SharedPreferences JSON migration (DataMigrationManager), and multiplatform-ready Repository delegation contracts.
```

### Current source of truth

- Entry points: `GoldexApplication.kt` and `MainActivity.kt` under `app/src/main/java/com/goldex/companion/`; `app/AndroidAppContainer.kt` is the composition root.
- Shared ownership: paths below in `domain/` and `model/` refer to `core/src/commonMain/kotlin/com/goldex/companion/`. Repository ports, AppSettings, PortfolioModels, MarketRates and LicenseModels also live in commonMain with their existing package names. Android implementations and all `ui/` paths remain under the Android source root.
- Invoice market inputs: AndroidAppContainer exposes the existing read-only quote StateFlow. MainActivity collects it and passes immutable MarketRates through MainScreen to the invoice editor, coin form and rate dialog. These forms do not access market repositories or own a second quote stream; existing invoice/manual input values remain local to their current owners.
- Platform services: `core/commonMain platform/` defines time, ID, decimal and local-calendar boundaries; `core/jvmSharedMain` provides the existing Java behavior to Android/JVM.
- Opening inventory: `domain/onboarding/OpeningInventory.kt` owns the unchanged seed policy; `CompleteOnboardingUseCase.kt` owns completion, the shared transaction and repeat protection. `data/local/RoomOnboardingCheckpoint.kt` adapts the existing marker without a schema change. A restored account bypasses all seed/profile writes; a repeated wizard edits settings without duplicating stock.
- App shell & coordinator: `ui/main/MainViewModel.kt` & `ui/main/MainScreen.kt` (with backward compatibility bridges in `ui/calculator/`)
- Feature ViewModels & Components: `ui/invoices/`, `ui/portfolio/`, `ui/settings/`, `ui/update/`, `ui/calculator/`, `ui/wizard/`, `ui/license/`
- Barter Invoicing Screen: `ui/invoices/BarterInvoiceScreen.kt` & `ui/invoices/modals/AddInvoiceItemModal.kt`
- Invoice deletion: `domain/invoice/InvoiceDeletionUseCase.kt` owns preflight checks, reversal of the actual stored invoice-linked ledger entries grouped by customer, and removal of those entries before deleting the barter invoice. Settlement payment rows are reversed through their ledger entries exactly once; unrelated manual entries and other invoices remain intact. Missing ledger access, missing entry owners, or inbound third-party transfer references block deletion. The current invoice customer snapshot and `syncWithLedger` flag do not determine reversal ownership.
- Invoice card footer: a single RTL row places the total price on the right, flexible spacing, then delete, PDF, and details. The delete icon is the rightmost control within the actions group and keeps its 48dp touch target.
- Deletion UI: the invoice list and legacy archive use `InvoiceDeletionConfirmationDialog` with Persian RTL confirmation. `BarterInvoiceViewModel` updates list/editor state only after the deletion result and `MainScreen` refreshes customer balances and displays success or failure. Transfer target cards are derived from saved invoice records on load, save, and deletion so removing a source restores its deduction while retaining other transfers.
- Deletion failure handling: production Room repositories share `RoomSyncUnitOfWork`; invoice deletion, ledger reversal, balances and outbox roll back together. Existing compensation remains for nontransactional repository implementations used by compatibility tests.
- Official invoice export: `domain/invoice/OfficialInvoiceDocument.kt` is the shared, pure projection of a barter invoice and gallery settings; `ui/util/OfficialInvoicePdfGenerator.kt` renders that projection as a premium A5 landscape PDF through one Vazirmatn/RTL typography path with complete Persian digits, official Shamsi dates, and direct digital signature rendering. `ui/invoices/InvoicePdfPreviewModal.kt` renders the generated file in-app with high-resolution `PdfRenderer` (3x scale) before the same cached file is shared.
- Business identity & signature: `ui/hub/JewelerProfileModal.kt` and `ui/wizard/WizardProfileStep.kt` own the registered jeweler details plus logo selection and virtual digital signature capture via `ui/components/SignaturePadDialog.kt`. Commercial stamp uploads are discontinued. Persistent URIs live beside the profile in `AppSettings` (`invoiceLogoUri`, `invoiceSignatureUri`); the PDF renderer consumes the same profile-owned values.
- License & Subscription Management: `ui/license/LicenseActivationModal.kt` (bottom-sheet modal with RTL layout) and `data/license/LicenseRepository.kt` (integrates with `api.qirato.ir` for 14-day trials and lifetime code activation).
- Domain Calculation Policies: `domain/calculator/`, `domain/invoice/BarterCalculationUseCases.kt`, `domain/portfolio/`
- Dashboard: `ui/dashboard/DashboardScreen.kt`
- Financial models and formatters: `model/`
- Report detail projections: `domain/reporting/ReportingDetailsUseCase.kt` builds period sales categories, current and previous-period profit buckets, and stock movement totals, plus current customer and inventory-location snapshots. `MainScreen` opens the four report pages over the reports gateway with the same screen-push transition. `ui/reporting/ReportingChrome.kt` owns their shared header and date filters; `ReportingShareText.kt` formats user-initiated aggregate summaries. All pages consume the same `ReportingUiState`; no second data owner or persisted shape was introduced.
- Integrations: `data/`
- Design tokens: `ui/theme/`. Dark-mode custom colors and Material 3 roles share the Stitch charcoal/slate/champagne palette; `docs/stitch/dark-mode/` stores the source dashboard HTML, screenshot, and role mapping. Dashboard vault gradients and market-gain labels select theme-aware tokens without changing financial state or the light palette.
- Floating input labels: `ui/components/GoldOutlinedTextField.kt` wraps the Material outlined field for `GoldInputField`, customer forms, and legacy settings. Labels are passed directly to Material with no painted background; the native outline cutout remains in use. Keyboard, formatting, validation, and field colors stay owned by the callers.
- Tests: `core/src/commonTest/` for portable compatibility/onboarding checks, `core/src/jvmSharedTest/` for existing domain tests executed on both Android and JVM, and `app/src/test/` for Android repository, migration, rollback and ViewModel checks; `app/src/androidTest/` includes the floating-label transparency pixel regression for light/dark themes and focus/error/disabled states (requires an Android device).
- Release workflow: `.github/workflows/build-and-release.yml`

The code is authoritative when this document and implementation disagree. Update this document when a structural decision changes.

## 3. Feature ownership model

New work must be organized around a feature owner. Pure behavior belongs in `core`; platform adapters and presentation currently belong in `app`.

```text
app ui/<feature>/          Screens, components, feature state
core domain/<feature>/     Use cases, policies, pure business calculations
core data/                 Existing repository ports and shared data models
app data/<feature>/        Repository implementations, local/remote sources
app app/                   Android composition root
core platform/             expect services + platform actual adapters
```

The current `model/` and `data/` packages remain valid during migration. Do not move files merely for aesthetic consistency. Move a file when ownership is unclear, isolated testing is needed, or a feature is being extracted.

Recommended feature areas:

- `dashboard`: portfolio summary, recent activity, market overview, trend history
- `market`: live rates, source selection, fallback policy, freshness
- `calculator`: jewelry, melt, coin bubble, and karat conversion
- `invoices`: invoice lifecycle, customer association, PDF/export
- `portfolio`: holdings, valuation, profit/loss
- `inventory`: showcase items, stock kardex ledger, weight conversions, vault valuation
- `reporting`: financial performance analytics, gross profit, vault weight balance, customer counterparties, and official VAT reports
- `settings`: user preferences and jeweler profile

## 4. State and UI contracts

The current application uses one `CalculatorUiState`. This is an accepted migration state, not a requirement that all future features share one state object.

Rules for new UI:

- Composables render immutable state and emit events.
- Composables must not call repositories or perform business calculations.
- Screen-local transient state is allowed for purely visual concerns such as selected timeframe or scroll position.
- Business state belongs to a ViewModel or feature state holder.
- Dashboard values, deltas, chart points, and recent transactions must come from state; demo values must be explicitly marked as placeholders and must not be presented as live data in production.
- Prefer feature-specific state projections over passing the entire `CalculatorUiState` to new screens.
- Navigation callbacks may remain simple lambdas until navigation becomes a real multi-screen requirement.

Target direction:

```kotlin
data class DashboardUiState(
    val portfolioSummary: PortfolioSummary?,
    val marketQuotes: List<MarketQuote>,
    val trend: List<PricePoint>,
    val recentInvoices: List<InvoiceSummary>,
    val isLoading: Boolean,
    val error: UiError?
)
```

Extract feature ViewModels incrementally. Do not duplicate state between the global ViewModel and a feature ViewModel without a defined owner.

## 5. Domain and financial correctness

Financial rules belong in pure Kotlin code with no Compose or Android dependencies. Use cases should own policies; ViewModels should translate input, invoke use cases, and publish state.

Required rules:

- Raw gold value, wage, profit, and tax remain separately represented.
- VAT is calculated only on wage plus profit according to the applicable Iranian regulation.
- Weight precision supports 0.001 grams.
- Karat and mesghal conversion constants are named and tested.
- Every financial change needs deterministic unit tests, including rounding and boundary cases.
- New production contracts must not use `Double` as an unexamined monetary representation. Prefer `Long` for whole toman amounts or an explicit money type; use `BigDecimal` only where fractional precision is required.
- Rounding is explicit at the business boundary and covered by tests.

Existing public models may continue using current types during migration. Do not change persisted or invoice-visible values without a compatibility plan.

## 6. Data and repository boundaries

UI and domain code must depend on repository contracts, not HTTP or `SharedPreferences` details.

Example target boundary:

```kotlin
interface MarketRatesRepository {
    val rates: kotlinx.coroutines.flow.Flow<MarketRates>
    suspend fun refresh(): Result<MarketRates>
}
```

Current implementations may remain:

- `HttpURLConnection` plus `org.json` for market providers
- Room version 2 for business records/settings and transactional cloud outbox; the existing migration retains old JSON backups
- `SharedPreferences` for device preferences and market caches; `PersistenceJsonCodecs` remains Android-owned while JSON contracts are tested

For every new repository:

- Define ownership of cached, pending, and failed data.
- Return structured success/error results rather than silently hiding all failures.
- Keep source-specific parsing separate from repository policy when practical.
- Ensure network connections close in `finally`.
- Make freshness and source information explicit in market data.
- Keep writes atomic from the user's perspective.

### Persistence migration

The business-record migration to Room is already implemented. Legacy SharedPreferences JSON remains a compatibility/import source and backup. It rewrites complete collections and must not become a new primary store for growing financial datasets.

Introduce Room or another durable local database when:

- History is large enough that collection rewrites affect UX.
- Partial queries, indexes, relationships, or migrations are required.
- Cloud sync needs stable local identifiers and change tracking.

The migration must include an import path, schema version, backup/rollback behavior, and tests against existing JSON data. No existing user data may be discarded.

## 7. Cloud synchronization

`ui/sync/CloudSyncViewModel` owns account forms and delegates to the independent `data/sync/SyncCoordinator`. `AccountRepository`, `CloudSyncRepository` and `SyncUnitOfWork` define the identity, transport and transaction seams. The coordinator owns cloud opt-in, StateFlow status, mutex, debounce, connectivity/foreground triggers and WorkManager retries. Business ViewModels do not own networking.

`GoldexDatabase` is Room version 2 with explicit additive migration and exported version 1/2 schemas. Business settings, sync metadata/outbox/checkpoint/conflicts, private asset metadata and staging share the financial database. `SettingsStore` remains compatible; biometric settings, theme, tokens, onboarding and opt-in remain device-owned. Legacy JSON is retained and malformed partial imports fail safely.

`SaveBarterInvoiceUseCase` and `InvoiceDeletionUseCase` share a transaction across invoice, ledger, customer balances and outbox. Manual ledger mutations and stock count/movement are also grouped. Classic invoices retain their existing behavior (no new ledger effects are invented). Local repository methods remain synchronous, matching the existing app; `allowMainThreadQueries` is a known inherited compromise. Network and snapshot work run on IO.

Create/full payload, top-level patch, tombstones, per-record version, device sequence and durable receipts implement protocol 1. Only diffs leave normal sync; local full-version conflict snapshots are stripped from wire requests. Server ownership/version/revision are authoritative; financial calculations remain local and historical records never recalculate on download. Exact Long strings and decimal-string legacy values are tested. Read the identity, protocol and recovery ADRs in `docs/adr/`.

Wizard includes an opt-in cloud step after the introduction, default off. Restored accounts bypass opening-inventory seeding. More Hub/settings expose account, queue depth, last success, manual sync, transfers, review, backup export and explicit account detach. Enabled header uses a 48dp cloud status control; green only after actual completion, rotating blue only during exchange, offline slash, login/lock/warning/pending badges. Only syncing-to-synced animates (~250ms); reduced motion disables rotation/transitions. Known retired writers require restore or detach before edits.

Server public activation is disabled by default. Real SMS, protected secrets, production backup/restore checks and emulator/device UI validation must pass before release. Local Robolectric migration/atomicity tests are not a replacement for device validation; instrumented migration and Compose state tests are included for a provisioned test device. No local APK assembly/signing is part of this work.

## 8. Dashboard contract

`DashboardScreen` is a presentation surface. It must not become a second business layer.

The current visual structure and Stitch design may remain while production data progressively replaces hardcoded content:

1. Live market values from market state.
2. Portfolio totals from portfolio domain calculations.
3. Recent invoices from invoice storage.
4. Trend points from a defined market history source.
5. Real deltas with an explicit comparison interval.

Hardcoded demo content must be named as demo content in code and removed or disabled for production releases.

## 9. Design system stability

The Persian Sovereign Aurum design system is a product contract.

- Reuse tokens from `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, and `Theme.kt`.
- Button and interactive control curvature is standardized to `ButtonShape` (`ButtonCornerRadius = 12.dp` in `Shape.kt`), mirroring the header notification button and dashboard card curvature.
- Preserve RTL at the screen boundary.
- Keep Persian number formatting centralized.
- Do not introduce screen-specific colors for existing semantic states.
- Keep reusable components under `ui/components/` unless they are truly feature-specific.
- Visual refactors must not alter calculations, persistence, or navigation behavior.

## 10. Security and release readiness

- Never commit new credentials, API tokens, passwords, or private keys.
- Keep the release keystore identity stable; signing credentials must move to protected CI secrets before public production distribution.
- Do not log customer identity, financial records, tokens, or full network payloads in production.
- Validate external data before using it in financial calculations.
- Release builds must be reproducible by `.github/workflows/build-and-release.yml`.
- Tag releases use the original build/sign/publish workflow. Compile checks and unit tests run locally before merging or tagging. Instrumented tests are optional manual device checks and do not run in release CI.
- Every release must have human-readable Persian notes and a verified artifact.

## 11. Testing strategy

The test pyramid is:

1. Pure domain tests for calculations, formatting, rounding, and policies.
2. Repository tests for serialization, migration, fallback, and error behavior.
3. ViewModel tests for event-to-state transitions.
4. Compose tests for critical workflows and accessibility semantics.
5. Cloud release verification for signed artifacts.

Local gates are `./gradlew compileDebugKotlin --no-build-cache --no-daemon -q`, `./gradlew testDebugUnitTest --no-daemon -q`, and `./gradlew :core:verifyCoreBoundaries :core:jvmTest --no-daemon -q`. Common metadata compilation prevents importing host classes into shared business code; the boundary task additionally rejects platform imports. JVM artifacts/tests are never packaged in the Android APK.

Every vertical feature ships with its smallest meaningful test slice. Do not wait for a complete database or backend before testing production behavior.

## 12. Architectural change protocol

Before changing a shared contract, an agent must:

1. Identify the current owner and all call sites.
2. State the compatibility impact.
3. Make the smallest migration step that can be tested.
4. Preserve existing persisted data and UI behavior.
5. Update this document and the relevant ADR when the boundary changes.

Use an Architecture Decision Record for decisions involving persistence, money representation, navigation, cloud sync, authentication, or module boundaries.

## 13. Completed migration slices

- Calculation policies live in `domain/calculator/GoldCalculationUseCases.kt`; the ViewModel parses UI input and publishes results without owning the formulas.
- Portfolio aggregation lives in `domain/portfolio/PortfolioValuation.kt`.
- Existing concrete repositories implement contracts in `data/RepositoryContracts.kt` (`CustomerStore`, `InvoiceStore`, `PortfolioStore`, `SettingsStore`, `MarketRatesStore`).
- `DashboardScreen` consumes `DashboardUiState` instead of the global ViewModel.
- `LiveRatesScreen` consumes `MarketRatesUiState` instead of the aggregate calculator state.
- `PortfolioTab` receives explicit mutation callbacks and items/rates, decoupled from ViewModels.
- Karat conversion owns its input and event state in `ui/calculator/KaratConvertViewModel.kt`.
- Customer, portfolio, and invoice JSON compatibility is centralized in `data/PersistenceJsonCodecs.kt` and covered by pure compatibility tests.
- Independent feature ViewModels (`CustomerManagerViewModel`, `InvoiceManagerViewModel`, `PortfolioManagerViewModel`, `SettingsViewModel`, `UpdateViewModel`, `KaratConvertViewModel`) are fully wired into the UI and coordinate directly with dialogs and preview cards.
- The root composition shell lives in `MainViewModel` and `MainScreen`. MainViewModel retains shell/market/calculator state; it is a migration seam rather than a claim that all feature state has been separated.
- Calculator and tool composables (`JewelryTab`, `CoinBubbleScreen`, `MeltCalcScreen`, `MoreHubScreen`) are decoupled from concrete ViewModels, depending only on focused UI state data classes and callback interfaces (`JewelryActions`).
- Compatibility typealiases `GoldCalculatorViewModel` and `CalculatorUiState` remain. The legacy `GoldCalculatorScreen` bridge now requires the same injected ViewModel factory as MainScreen; both in-repository entry points are updated.
- Release signing credentials are securely configured using protected GitHub Actions repository secrets.
- Phase 2 formally completed: Dashboard, Live Rates, and dedicated Stitch calculator screens (`KaratConvertScreen`, `CoinBubbleScreen`, `MeltCalcScreen`, `StandardFormulasScreen`) are fully established.
- Standardized two-action dialog button layout across all modals and dialogs (Cancel on right, Save/Confirm on left in RTL) governed by `.agents/rules/dialog-button-layout.md`.
- Weight and price-based wage inputs unified to 18sp with strict LTR decimal entry semantics under RTL layouts.
- Unified corner radius across all buttons, icon buttons, filter capsules, and action surfaces to 12.dp (`ButtonShape` in `ui/theme/Shape.kt`), eliminating arbitrary radii (pill, 20.dp, 24.dp, 50%, CircleShape) in favor of the single source of truth defined by the header notification bell and primary dashboard cards.
- Onboarding Wizard module established in `ui/wizard/` (`OnboardingWizardScreen`, `WizardIntroSlides`, `WizardProfileStep`, `WizardFinancialStep`, `WizardInventoryStep`, `WizardCompletionStep`) featuring a fixed sticky stepper header, scrollable `AnimatedContent` horizontal slide/fade middle body, sticky fixed footer navigation with strict RTL button semantics, auto-advancing intro slider with progress-filling indicators, interactive store logo and commercial stamp asset uploading (`ui/components/BrandAssetComponents.kt`), RTL-native gesture direction, subtle hero-image zoom, `LuxurySegmentedControl` financial selectors, and animated confetti celebration.
- Market and dashboard motion is presentation-owned: Live Rates uses the shared filter enter/exit transition, while the dashboard keeps price and fluctuation counters outside the chart transition so their digits animate independently when the horizon changes.
- User-facing tax guidance derives its displayed percentage from `AppSettings`; legal article labels and fixed tax-rate claims are not UI sources of truth.
- The app-shell light/dark choice is owned by `MainViewModel` through `ThemePreference` and the `SettingsStore` theme preference methods. It is persisted independently from financial and jeweler-profile settings so stale feature state cannot overwrite the selected appearance.
- Gold Inventory & Showcase module established in `ui/inventory/` (`InventoryScreen`, `InventoryViewModel`, `AddInventoryItemModal`, `AdjustStockModal`) with official guild retail price formulas, automated profit by category (20% jewelry, 0% coins, 7% standard), dual wage modes (percentage and toman per gram), custom gold fineness, RFID/tray tracking, animated category capsule filters, and stock adjustment logging.
- The four specialized reporting entries open dedicated RTL pages with the gateway's header, date chips, screen transition, dark summary cards, and animated changing figures. Sales, VAT, and stock movements use the selected period, including a Persian-calendar current-quarter option; inventory and customer balance totals remain explicitly labeled as current recorded balances. The profit page labels wage plus recorded seller profit rather than claiming net operating profit, since expense records are not available. VAT shows recorded invoice tax and the configured default rate separately. Stitch source HTML and screenshots are kept under `docs/stitch/reporting/` as visual references.
- Production persistence migration to Room database (`GoldexDatabase`, Room DAOs, independent database entities, type converters, and mappers) fully established. Clean Architecture Ports & Adapters separation isolates domain models (`Customer`, `BarterInvoice`, `InventoryItem`, etc.) completely from database annotations. Zero-data-loss automated migration (`DataMigrationManager`) migrates existing SharedPreferences JSON on first launch inside an atomic SQLite transaction while preserving legacy files as immutable safety backups.

- Migration preparation in 0.56.14: the shared core compiles Kotlin common metadata and JVM tests, the Android host consumes it, ViewModel construction is centralized, and opening-inventory writes moved out of Compose. See ADR 0006.

## 14. Known current compromises

- The market layer contains provider-specific HTTP and parsing code.
- ViewModel/presentation code, Room 2.6.1, WorkManager, cloud coordinator/JSON form state, PDF/files and biometrics remain Android-owned. A JVM-tested core is not yet a Windows application.
- Repository methods remain synchronous and Room permits main-thread queries; changing threading is a separate migration.
- Native iOS actual services, common UI, desktop packaging and other platform signing are not implemented.
- Existing Double money fields and large-number formatting semantics remain unchanged for compatibility.
- Dashboard visual content is partly static while the feature is being migrated from Stitch designs.

These are tracked migration items, not reasons to break existing features through a broad rewrite.
Cloud settings now opens from the existing More Hub settings group in a bottom-anchored modal using the existing financial modal geometry, LuxuryMotion and GoldInputField. Login separates phone/code steps, displays only server-selected temporary code hints, and respects resend cooldown. The cloud modal is no longer forced open by read-only state; repository mutation guards remain active. See docs/adr/0005-cloud-settings-modal.md.
