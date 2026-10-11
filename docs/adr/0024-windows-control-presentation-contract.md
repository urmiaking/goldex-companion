# ADR 0024: Context-specific Windows controls

## Context

Windows customer cards reused the shell's 24dp capsule/compact 11sp control while other screens used shared 12dp buttons or Material defaults. Rule documents simultaneously required 24dp primary buttons and universal 12dp curvature; all mutually exclusive choices were treated as segmented switches. The user selected existing calculator body controls, Android customer filter options, Android banking quick access and the desktop header as distinct references.

## Decision

`desktop/ui/controls/DesktopControls.kt` owns Windows presentation adapters using shared Aurum tokens. Body actions/outlined fields/search use 12dp, header/nav controls 24dp, independent record/time/category filters use Android's reference capsules and counts. Body actions require an explicit meaningful icon and 14sp one-line label. Form choices use independent 12dp options; approved calculator mode controls remain local. Dashboard shortcuts use icon-over-label tiles. WorkspaceHeader owns rate-detail identity/back, as it already owns customer statement identity/back/registration.

The mandatory rule and detailed specification are `.agents/rules/windows-design-system.md` and `docs/design/windows-controls.md`. Existing generic button/selector/input rules explicitly route Windows to this contract; historical architecture statements no longer claim all controls share one radius. Guard tests prevent raw action/filter/input variants from reappearing without a reviewed exception.

## Consequences

- Android UI and shared tokens remain unchanged; this is a Windows-only release.
- Feature state holders, domain policies, navigation destinations, persisted shape, calculation precision and transactional delete/write guards remain unchanged.
- All action icons are compile-time required, including optional/empty/error/dialog actions. Disabled invoice placeholders stay disabled.
- Overflow uses scrolling or responsive reflow, not compact text or hidden icons. Reduced-motion preference is supplied to the Windows control boundary.
- These host-owned wrappers intentionally avoid a shared token migration affecting Android. Shared components can adopt a common context API later if both hosts require the same change.

## Revisit

Revisit when Android and Windows jointly change control geometry or accessibility APIs, or when meaningful behavior needs another control role. Any exception must be recorded in the detailed contract and verified at minimum width in both themes.
