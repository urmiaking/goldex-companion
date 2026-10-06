# GoldEx Companion Architecture

## 1. Purpose and architectural stance

GoldEx Companion has an Android `app` host, a Windows `desktop` workspace, a Kotlin Multiplatform `core` (Android/JVM), and a `shared-ui` module (Android/JVM). Android retains its existing feature state, navigation, Room/local preferences and HTTP integrations. Windows provides a dashboard, gold calculator, provider/manual rates, locally persisted personal holdings and settings/backup; business ledgers/inventory/invoices and cloud remain Android-owned.

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
       -> Dialogs (CustomerPickerDialog, TaxProfitModal, PriceSourceModal, JewelerProfileModal, UpdateDialog, AddInventoryItemModal, AdjustStockModal)

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
- Activity Result compatibility: `MainActivity` retains `FragmentActivity` for biometric authentication. The Android host declares Fragment 1.6.2 directly, compatible with Activity 1.8.2. Biometric 1.1.0 otherwise brings in Fragment 1.2.5, whose 16-bit request-code restriction prevents registry-backed logo pickers from launching. `BrandImagePickerHostTest` exercises a real FragmentActivity registry launch and selected-logo result delivery with the resolved app dependencies.
- Shared ownership: paths below in `domain/` and `model/` refer to `core/src/commonMain/kotlin/com/goldex/companion/`. Repository ports, AppSettings, PortfolioModels, MarketRates and LicenseModels also live in commonMain with their existing package names. Android feature screens/state remain in `app`; theme and the selected reusable components below live in `shared-ui`, retaining their existing packages.
- Windows workspace (0.56.37): `desktop/state/DesktopWorkspace` owns immutable state, event-to-store behavior, navigation, retained drafts and calculator defaults. `DesktopPortfolioPolicy` validates form input and delegates valuation to the existing shared policies. `desktop/data/DesktopDataStore` implements shared portfolio/settings ports over a version-1, atomically replaced local document, exclusive session lock and previous-document backup. Strict desktop validation prevents silent record loss; unknown fields survive updates. `DesktopMarketRepository` supplies bounded HTTP/provider adapters and ONLINE/CACHED/MANUAL snapshots; missing quotes remain unavailable and stale snapshots keep their original timestamp. `desktop/ui` renders the right-side shell and pages with shared tokens/fonts, responsive layout and keyboard focus. The window and launcher use the official existing icon. No Android storage is opened or implicitly imported. See ADR 0011.
- Shared JSON serialization: `core/jvmSharedMain/data/PersistenceJsonCodecs.kt` retains the existing Android codecs/package/callers and adds portable settings serialization. Android resolves platform JSON; JVM supplies org.json. No persisted Android record shape or database migration changes.
- Shared presentation primitives: Color/Shape/Theme/Type, AnimatedPriceTicker, GoldButton, GoldOutlinedTextField, LuxuryCard, LuxurySegmentedControl and ThousandsSeparatorVisualTransformation belong to `shared-ui/commonMain`. Font resolution has Android/JVM actuals; the existing `app/src/main/res/font` files supply generated Android library resources and JVM resources. No font/token values or Android `R.font` callers change. Segmented controls expose tab selection semantics and a visible hardware-keyboard focus border.
- Invoice market inputs: AndroidAppContainer exposes the existing read-only quote StateFlow. MainActivity collects it and passes immutable MarketRates through MainScreen to the invoice editor, coin form and rate dialog. These forms do not access market repositories or own a second quote stream; existing invoice/manual input values remain local to their current owners.
- Platform services: `core/commonMain platform/` defines time, ID, decimal and local-calendar boundaries; `core/jvmSharedMain` provides the existing Java behavior to Android/JVM.
- Opening inventory: `domain/onboarding/OpeningInventory.kt` owns the unchanged seed policy; `CompleteOnboardingUseCase.kt` owns completion, the shared transaction and repeat protection. `data/local/RoomOnboardingCheckpoint.kt` adapts the existing marker without a schema change. A restored account bypasses all seed/profile writes; a repeated wizard edits settings without duplicating stock.
- App shell & coordinator: `ui/main/MainViewModel.kt` & `ui/main/MainScreen.kt` (with backward compatibility bridges in `ui/calculator/`)
- Feature ViewModels & Components: `ui/invoices/`, `ui/portfolio/`, `ui/settings/`, `ui/update/`, `ui/calculator/`, `ui/wizard/`, `ui/license/`
- App identity in the More hub footer comes from `R.string.app_name` and the installed build's `BuildConfig.VERSION_NAME`, rendered with Persian digits. `UpdateDialog` uses the same identity sources, one bounded scroll area for its header/version/release notes, and a fixed RTL action row (later on the right, download on the left). Update checking remains owned by `UpdateViewModel`/`AppUpdateChecker`; direct APK and release-page fallback behavior is retained.
- Barter Invoicing Screen: `ui/invoices/BarterInvoiceScreen.kt` & `ui/invoices/modals/AddInvoiceItemModal.kt`
- Invoice payment entry uses the single action beside the settlement section title; the former lower duplicate is removed. Cloud settings retain `CloudSyncViewModel` as the durable state owner. `CloudAccountActions` in `ui/sync/CloudSyncComponents.kt` renders existing intents, keeps recovery actions visible, and collapses maintenance actions behind “گزینه‌های بیشتر” using local saveable UI state. Restore, detach, logout and writer-transfer confirmations remain in `CloudSettingsContent`; the bottom sheet uses its header close control.
- Customer settlement: `core/domain/customers/CustomerSettlement.kt` owns target-debt allocation, gold/cash conversion, explicit offsetting of opposing balances, excess payments, and immutable ledger effects. `ui/customers/CustomerSettlementViewModel.kt` owns the form and quote snapshot; `CustomerSettlementModal` renders state and emits intents using the Persian Sovereign Aurum design (deep dark modal backdrop, full-width expandable basis selector, 2-row gold/cash balance summary card with debt-red and credit-green colors, spring-animated `LuxurySegmentedControl` sliding pills, quick calculate shortcut, live calculation preview card directly beneath payment inputs, and RTL footer with cancel on the right and submit on the left). The ledger and pending invoice cards open this flow; independent manual receipts remain available. Live quotes with an unknown timestamp or offline status cannot be presented as a current settlement rate.
- Settlement persistence: Room 2→3 only adds `settlementJson` to ledger entries. Legacy entries retain their one-unit effects. New entries retain the physical payment, target unit, agreed rate/source/time, and signed gold/cash effects. JSON and cloud codecs preserve this metadata and unknown settlement fields; malformed metadata fails before importing a financial record. No historical balances are automatically converted.
- Outstanding invoices: for ledger-connected invoices, the stored ledger effects determine the remaining balance and list status. Dated settlements are linked to the original invoice without repricing the sale; their read-only display rows retain the ledger receipt ID, date and recorded settlement effects. Invoice editing preserves settlement entries unless explicitly removed; removing a settlement payment in the invoice editor tracks pending deletions and only reverses the ledger transaction and customer account effect upon explicit final invoice save. Exiting without saving leaves the customer ledger and persisted invoice untouched. Deletion reverses their stored effects exactly once. Overall-account settlements without an invoice allocation do not mark a particular invoice settled. See ADR 0007 for rounding, offset, rollback and older-client restrictions.
- Invoice deletion: `domain/invoice/InvoiceDeletionUseCase.kt` owns preflight checks, reversal of the actual stored invoice-linked ledger entries grouped by customer, and removal of those entries before deleting the barter invoice. Settlement payment rows are reversed through their ledger entries exactly once; unrelated manual entries and other invoices remain intact. Missing ledger access, missing entry owners, or inbound third-party transfer references block deletion. The current invoice customer snapshot and `syncWithLedger` flag do not determine reversal ownership.
- Invoice card layout: the top status badge remains a pure status indicator; the middle elevated container houses items summary, weight/wage details, total invoice amount, and remainder row (when present). The bottom action row places the delete icon on the far right (start in RTL), and action buttons on the left with the settlement button positioned between print and view details for unsettled invoices. Both saved-invoice entry points open `CustomerSettlementModal` with the invoice allocation and its recorded balance. `AddInvoicePaymentModal` is for initial issue payments and legacy unlinked invoices; it cannot reprice a later ledger-connected receipt.
- Invoice contractual debt: `core/domain/invoice/InvoiceDebtPolicy.kt` owns explicit `InvoiceDebtBasis.GOLD/CASH`, independent of retail/wholesaler role. New gold contracts convert the net agreed invoice value once at issue; subsequent cash/coin-value settlements use their own recorded rate and gold receipts their net weight/fineness. Legacy null basis retains the old role policy. `SaveBarterInvoiceUseCase` rebuilds generated entries and retains only non-generated dated settlements; snapshot payment rows are excluded from regeneration. `CustomerManagerViewModel` atomically removes the matching snapshot row when reversing a receipt. Room 3→4 adds only the empty-default basis column. See ADR 0008 for conversion, overflow, compatibility and rollback details.
- Deletion UI: the invoice list and legacy archive use `InvoiceDeletionConfirmationDialog` with Persian RTL confirmation. `BarterInvoiceViewModel` updates list/editor state only after the deletion result and `MainScreen` refreshes customer balances and displays success or failure. Transfer target cards are derived from saved invoice records on load, save, and deletion so removing a source restores its deduction while retaining other transfers.
- Deletion failure handling: production Room repositories share `RoomSyncUnitOfWork`; invoice deletion, ledger reversal, balances and outbox roll back together. Existing compensation remains for nontransactional repository implementations used by compatibility tests.
- Official invoice export: `domain/invoice/OfficialInvoiceDocument.kt` is the shared, pure projection of a barter invoice and gallery settings; `ui/util/OfficialInvoicePdfGenerator.kt` renders that projection as a premium A5 landscape PDF (1190x840 pt at 2x base resolution) through one Vazirmatn/RTL typography path with unhinted subpixel text rendering, complete Persian digits, BiDi directional isolation, official Shamsi dates, and direct digital signature rendering. `ui/invoices/InvoicePdfPreviewModal.kt` renders the generated file in-app with adaptive high-resolution `PdfRenderer` before the same cached file is shared.
- Business identity & signature: `ui/hub/JewelerProfileModal.kt` and `ui/wizard/WizardProfileStep.kt` own the registered jeweler details plus logo selection and virtual digital signature capture via `ui/components/SignaturePadDialog.kt`. Commercial stamp uploads are discontinued. Persistent URIs live beside the profile in `AppSettings` (`invoiceLogoUri`, `invoiceSignatureUri`); the PDF renderer consumes the same profile-owned values.
- Brand image loading: `ui/components/BrandAssetComponents.kt` shares bounded PNG decoding between profile previews, logo imports, and official invoice export. Bounds-only decoding validates dimensions before a second read of the pixels. Existing file/content URIs and absolute paths remain supported. `BrandImagePicker.kt` opens the standard document picker first through one Activity Result launcher, then tries content, photo, and OEM gallery providers; only unavailable/denied providers trigger fallbacks. Cancelled selections and failed image imports preserve the current logo, and successful imports retain a private local copy.
- License & Subscription Management: `ui/license/LicenseActivationModal.kt` (bottom-sheet modal with RTL layout) and `data/license/LicenseRepository.kt` (integrates with `api.qirato.ir` for 14-day trials and lifetime code activation).
- Domain Calculation Policies: `domain/calculator/`, `domain/invoice/BarterCalculationUseCases.kt`, `domain/portfolio/`
- Dashboard: `ui/dashboard/DashboardScreen.kt`
- Financial models and formatters: `model/`
- Report detail projections: `domain/reporting/ReportingDetailsUseCase.kt` builds period sales categories, current and previous-period profit buckets, and stock movement totals, plus current customer and inventory-location snapshots. `MainScreen` opens the four report pages over the reports gateway with the same screen-push transition. `ui/reporting/ReportingChrome.kt` owns their shared header and date filters; `ReportingShareText.kt` formats user-initiated aggregate summaries. All pages consume the same `ReportingUiState`; no second data owner or persisted shape was introduced.
- Integrations: `data/`
- Design tokens: `shared-ui/src/commonMain/kotlin/com/goldex/companion/ui/theme/`. Dark-mode custom colors and Material 3 roles share the Stitch charcoal/slate/champagne palette; `docs/stitch/dark-mode/` stores the source dashboard HTML, screenshot, and role mapping. Dashboard vault gradients and market-gain labels select theme-aware tokens without changing financial state or the light palette.
- Floating input labels: `ui/components/GoldOutlinedTextField.kt` wraps the Material outlined field for `GoldInputField`, customer forms, and legacy settings. Labels are passed directly to Material with no painted background; the native outline cutout remains in use. Keyboard, formatting, validation, and field colors stay owned by the callers.
- Tests: `core/src/commonTest/` for portable compatibility/onboarding checks, `core/src/jvmSharedTest/` for existing domain tests executed on both Android and JVM, and `app/src/test/` for Android repository, migration, rollback and ViewModel checks; `app/src/androidTest/` includes the floating-label transparency pixel regression for light/dark themes and focus/error/disabled states (requires an Android device).
- Release workflow: `.github/workflows/build-and-release.yml` uses `scripts/release/plan_release.py` to select Android/Windows from each host's last published artifact, validate independent versions and prepare a Persian-noted draft. Selected jobs build independently; publication requires their successful verified uploads. Windows-only `windows-vX.Y.Z` releases do not become latest, preserving Android update discovery. Shared `vX.Y.Z` releases publish both; Android-only publishes APK alone. Exact-tag recovery skips only already-uploaded required assets. Signing credentials remain Android CI-only; local desktop packaging is permitted and local APK assembly/signing prohibited. See ADR 0012.

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
core presentation/         Platform-free form state and validation for extracted slices
shared-ui/                 Shared Compose tokens/primitives and font actuals
desktop/                   JVM window, desktop rendering and portable packaging
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

`GoldexDatabase` is Room version 4 with explicit additive migrations and exported version 1/2/3/4 schemas. Business settings, sync metadata/outbox/checkpoint/conflicts, private asset metadata and staging share the financial database. `SettingsStore` remains compatible; biometric settings, theme, tokens, onboarding and opt-in remain device-owned. Legacy JSON is retained and malformed partial imports fail safely.

`SaveBarterInvoiceUseCase` and `InvoiceDeletionUseCase` share a transaction across invoice, ledger, customer balances and outbox. Manual ledger mutations and stock count/movement are also grouped. Classic invoices retain their existing behavior (no new ledger effects are invented). Local repository methods remain synchronous, matching the existing app; `allowMainThreadQueries` is a known inherited compromise. Network and snapshot work run on IO.

Create/full payload, top-level patch, tombstones, per-record version, device sequence and durable receipts implement protocol 1. Only diffs leave normal sync; local full-version conflict snapshots are stripped from wire requests. Server ownership/version/revision are authoritative; financial calculations remain local and historical records never recalculate on download. Exact Long strings, plain-string decimals (preventing scientific notation like 2.99E+8 in large amounts), and decimal-string legacy values are tested and enforced in SyncJson. Read the identity, protocol and recovery ADRs in `docs/adr/`.

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
- Cloud onboarding synchronization invariant: During the onboarding wizard, cloud sync is deferred (`deferOnboarding(isWizardActive)`) so that initial shop settings and opening inventory are committed to Room before initial cloud upload. `restoredGeneration` is exclusively reserved for full backup restorations (`restore()`) rather than normal syncs, preventing premature or empty initial cloud snapshots from bypassing user-entered wizard setup data.

## 14. Known current compromises

- The market layer contains provider-specific HTTP and parsing code.
- Android feature ViewModels, Room 2.6.1, WorkManager, cloud coordinator/JSON form state, PDF/files and biometrics remain Android-owned. The initial Windows manual calculator shares core and UI primitives; it does not yet implement those financial application adapters.
- Repository ports remain synchronous. Invoice, customer/statement, inventory and settlement feature ViewModels execute reads/writes and snapshot projections through a serial coroutine queue on an injected dispatcher (IO by default). Reports load on opening, rather than during shell construction. Room still permits main-thread queries for retained settings/onboarding/portfolio compatibility paths; this is not permission for new list operations to run on Main.
- Native iOS actual services, full shared application UI and other platform signing are not implemented. Windows packaging currently publishes a portable manual calculator; the full Android financial application has not been ported.
- Existing Double money fields and large-number formatting semantics remain unchanged for compatibility.
- Dashboard visual content is partly static while the feature is being migrated from Stitch designs.

These are tracked migration items, not reasons to break existing features through a broad rewrite.

### Large financial lists (0.56.35)

Invoice, customer, customer-statement and inventory screens expose individual records as keyed LazyColumn items with reusable content types. A list is never wrapped inside one animated item containing all cards. Each animated main destination owns its scroll container, so outgoing invoice lists retain finite height while switching tabs. Existing cards, RTL, navigation and floating actions are retained.

FeatureWorkQueue serializes feature operations, surfaces failures and supports retry. Synchronous financial transactions run without suspension inside one worker invocation, preserving RoomSyncUnitOfWork's thread-local transaction/outbox context. Success callbacks follow completed persistence; write guards reject repeat submissions while saving. Statement and settlement reads check the current selection/request before publication, including failures, to prevent late results reopening or replacing another customer's screen.

CustomerStore exposes a compatible bulk invoice-ledger read. Room implements it with distinct IDs in batches of 500, using the existing invoiceId index; no entity, schema version or persisted shape changes. Invoice cards use a ledger snapshot and ID-indexed transfer projection instead of per-invoice reads and repeated full-list rewrites. Snapshot totals are built outside composition, filters are cached per immutable state, and historical statement balances belong to core domain/customers/CustomerStatementBalances.kt.

All record snapshots are still loaded into memory. This change bounds composed UI rows and removes Main-thread database work in these features; it does not introduce database paging. See ADR 0009 and docs/testing/large-record-lists.md for coverage and device profiling limits.
Cloud settings opens from the existing More Hub settings group in a bottom-anchored modal with Stitch luxury styling, featuring top gold accent hairline, animated pulse status indicator, auto-sync master toggle card with shop name, central vault server metrics with latency and AES-256 encryption status, and storage quota progress bar (50 MB trial quota, 5 GB permanent license quota). Sync preferences cover invoices, ledgers, inventory balance, and photo assets with a Wi-Fi-only toggle. CloudAccountActions provides manual sync, offline backup exports, and collapsed maintenance options. See docs/adr/0005-cloud-settings-modal.md.
