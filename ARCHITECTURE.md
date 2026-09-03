# GoldEx Companion — Technical Architecture (`ARCHITECTURE.md`)

## 1. System Philosophy & Design Patterns

GoldEx Companion is built on modern Android development standards using **Jetpack Compose**, **Unidirectional Data Flow (UDF)**, and **Clean MVVM Architecture**.

### State Flow & Immutable UI State
All computational screens observe a single immutable state stream from `GoldCalculatorViewModel`:
- `uiState: StateFlow<GoldCalculatorUiState>`
Every user interaction (typing weight, selecting karats, locking spot price, adding invoice items) dispatches an event method on `GoldCalculatorViewModel`, which updates state and computes derivations instantaneously.

### Domain Separation
- **`model/GoldCalculation.kt`**: Pure mathematical functions for Iranian gold formulas (Jewelry pricing, Mesghal-gram conversion, Karat equivalence, and Emami coin bubble). Pure, unit-testable, with no Android dependencies.
- **`model/Invoice.kt`**: Multi-item invoice aggregation logic computing total gross weight, net gold weight, total wages, total profit, and legal VAT.
- **`data/` Layer**: Repositories for live rates (HTTP scraping via OkHttp), local customer storage (SharedPreferences JSON serialization), and portfolio holdings.

## 2. Component Design System (Stitch Sovereign Aurum)

The UI adheres strictly to the **Google Stitch Sovereign Aurum** design system:
- **`LuxuryCard`**: Hairline border (`0.6.dp` gold/translucent border), 16.dp rounded corners, top gradient accent, and dual-layer subtle elevation.
- **`GlassmorphicDock`**: iOS 26 / Telegram glassmorphism floating bottom bar with frosted blur background, specular hairline highlight, and spring-animated sliding indicator.
- **`ModalNavigationDrawer`**: Persian RTL drawer keeping the main trading interface distraction-free while providing direct access to Customer Directory, App Settings, and Live Updates.

## 3. In-App Update Engine

- **`AppUpdateChecker`**: Non-blocking background worker checking GitHub Releases API.
- **`UpdateDialog`**: Direct modal dialog notifying users when a newer release is published with release notes and instant APK download intent.
