---
trigger: always_on
description: Mandatory Windows control geometry, icons, readable labels, independent filters and search consistency.
---

# Windows control contract

Before editing Windows UI, read [the detailed specification](../../docs/design/windows-controls.md). This rule supersedes older blanket pill-button and segmented-filter guidance **for Windows**. Preserve the Android host's existing implementation unless Android changes are explicitly in scope.

1. Use `desktop/ui/controls/DesktopControls.kt` for desktop action buttons, outlined inputs, search, filter/choice rows, icon buttons and quick-access tiles. Reuse shared Aurum colors, `ButtonShape`, typography and motion; never make a local lookalike or hide an icon to fit a label.
2. **Header**: action/date/connection/back capsules are 24dp radius and 48dp height. Identity and registration/back actions belong to `WorkspaceHeader`. At narrow widths, actions stay beside identity; status may occupy a second header row. **Navigation** keeps its existing rounded treatment. Do not use header capsules inside cards or dialogs.
3. **Body/dialog actions**: `DesktopActionButton`, 12dp `ButtonShape`, 48dp height (44dp minimum for dense tables), 14sp Vazirmatn label, 20dp meaningful icon, one line, 6dp icon gap, 12dp horizontal padding. An icon is a required argument. Secondary actions use elevated surface/hairline; primary actions use the existing gold gradient. Preserve disabled/loading/busy behavior. Cancel remains first/right in RTL.
4. **Filters**: `DesktopFilterRow`/`DesktopFilterChip`, independent selectable capsules based on Android `LedgerFilterCapsuleItem`; no enclosing segmented track/sliding pill. Use separate count badges, correct existing single selection behavior, 48dp target, 14sp text, keyboard selection/focus, overflow scrolling, 200ms color transitions (zero with reduced motion). Category, instrument, time horizon and record-status filters use this rule everywhere. **Form choices** use `DesktopChoiceRow` with 12dp body curvature. Genuine calculator calculation-mode segments are the explicit retained exception.
5. **Search**: `DesktopSearchField`, 12dp radius, 48dp height, search icon, one-line 14sp text, clear control when nonempty, visible focus outline. Form inputs use `DesktopOutlinedTextField` or `DesktopField`/`GoldInputField` which delegate to it, also 12dp. Keep floating labels transparent, numeric text LTR/ss01 and units at visual left. Dropdown fields use the same body shape and an expand icon.
6. **Dashboard shortcuts**: icon-above-label `DesktopQuickAccessTile` derived from Android banking quick access; retain destinations and keyboard interaction. Do not turn these into stretched primary/secondary buttons.
7. Audit every rendered action, including empty/error states, old holdings, forms, delete confirmations, settings, signature/image controls and updater. Record approved exceptions and changed coverage in the specification. Verify minimum window, wide window, light/dark, keyboard, filter semantics, labels without overflow and financial workflows. Add meaningful guard coverage so future features cannot silently reintroduce raw mismatched controls.
