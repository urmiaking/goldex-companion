# ADR 0012: Per-platform release selection and baselines

## Status

Accepted at the user's explicit request. Supersedes the unconditional Android-then-Windows release chain in ADR 0010.

## Context

Building both hosts for a desktop-only change wastes Actions quota. Comparing only to the previous tag can hide changes when that tag published only the other platform. The Android updater reads GitHub's latest release, so promoting a Windows-only ZIP would advertise the wrong update.

## Decision

`scripts/release/plan_release.py` computes each platform's changes from the latest ancestor published release containing its artifact. Exclude drafts/prereleases and tags without that artifact. Classify app-only, desktop-only and shared modules/toolchain/workflow/resources according to the always-on platform routing rule. Validate affected version bumps before starting build runners.

Keep versions independent: Android versionName/code in app Gradle; Windows version in `desktop/version.properties`. Windows-only uses `windows-vX.Y.Z` and never becomes latest. Android-only/shared uses `vX.Y.Z`; shared releases bump both to that version. Checked-in Persian notes accompany the exact immutable tag.

The planning job prepares a draft. Android and Windows build jobs depend only on the plan, so shared builds run independently. Jobs upload their own assets; the final job verifies selected assets and publishes only when selected jobs succeed. A manual recovery can rebuild only a missing platform when the other required asset already exists on the same release. Published-release recovery preserves latest selection.

Unit tests and compile gates stay local. Android signing remains exclusively in its selected CI job; unrelated Windows jobs need no release credentials. Signing identities, application IDs and Android update parsing remain unchanged.

## Consequences

Windows-only tasks can complete without an APK. Shared changes still validate/release both. A failed platform leaves a recoverable draft rather than claiming a complete shared release. API metadata/tag history is needed for planning; a planning failure reports an issue and starts no expensive build jobs. Documentation-only work needs no release artifact.

## Revisit

Revisit for a third host, Windows automatic update discovery, branch-specific release channels or a different artifact store. Any new shared resource must be added to classifier tests.
