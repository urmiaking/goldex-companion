# Segmented Switch & Selector Invariant (`segmented-switch-design-system.md`)

## 1. Core Principle
Distinguish calculation modes and chart time ranges from record/category filters. Android's existing calculation mode selectors and the user-approved Windows calculator mode controls may retain `LuxurySegmentedControl`. Retained Windows chart horizons and customer account type also use this shared sliding selector; record/category/instrument filters remain independent.

Windows MUST use independent `DesktopFilterRow`/`DesktopFilterChip` for customer/invoice/stock categories and rate instruments, matching Android customer-ledger reference options. Chart horizons and customer create/edit account type retain `LuxurySegmentedControl`; stock location is a dropdown. Other form choices use `DesktopChoiceRow`. Follow [windows-design-system.md](windows-design-system.md) and [the detailed contract](../../docs/design/windows-controls.md), including count badges, 14sp labels, keyboard selection, overflow and reduced motion. The following motion specifications apply only to retained segmented controls.

## 2. Inviolable Design & Motion Rules
- **Sliding Indicator Transition**: The active selection indicator must glide smoothly between options using a physical spring transition (`spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)`). Instant snapping without motion is prohibited.
- **No Harsh Dark Borders**: Outer containers and pills must NEVER use black or dark-gray solid borders (`colors.border` with dark alpha). Always use delicate golden borders: `BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f))`.
- **RTL Awareness**: The offset and sliding calculations must natively support Right-to-Left (RTL) layout direction, ensuring the pill moves correctly towards the selected Persian label. In Jetpack Compose, `Modifier.offset(x)` is layout-direction aware (positive `x` offsets away from Start/Right edge towards End/Left edge in RTL); hence `targetOffset = itemWidth * selectedIndex` is the correct formula for both RTL and LTR without manual inversion.
- **Typography**: Text inside all segmented options must use `VazirmatnFamily` with `fontFeatureSettings = "ss01"`, ensuring Persian numerals (۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹).
- **Colors**:
  - Container Background: `colors.surfaceElevated`
  - Active Pill: `colors.surface` with `BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.5f))` and soft elevation (`1.dp` to `1.5.dp`)
  - Active Text Color: `colors.goldPrimary` (Bold)
  - Inactive Text Color: `colors.textMuted` (Medium)

## Windows scope

Windows follows [its context-specific contract](windows-design-system.md). Retain `LuxurySegmentedControl` for dashboard/analysis chart horizons, create/edit customer account type, and calculator modes. Inventory location is a dropdown; record/category/instrument filters remain independent options. Do not apply the opening blanket rule to every Windows choice.
