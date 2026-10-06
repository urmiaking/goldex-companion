---
trigger: always_on
description: Publish only affected platforms, preserving each platform's version and last successful artifact baseline.
---

# Platform-selective releases

The user's release policy is mandatory:

- Changes to shared infrastructure, `core`, `shared-ui`, Gradle root/toolchain, or assets consumed by both hosts build and publish **Android and Windows**.
- Changes only in `desktop` build and publish **Windows only**.
- Changes only in `app` build and publish **Android only**. Android font files and launcher PNGs used by desktop are shared assets; classify them as both.
- Documentation/rules alone do not require an artifact release.

`.github/workflows/build-and-release.yml` and `scripts/release/plan_release.py` implement the policy. Compare each platform against its own last **published release containing that platform's asset**. Do not compare only against the last tag: it may have released the other platform, or its build may have failed. Draft/prerelease assets and unrelated Git history are not successful baselines.

## Versions and tags

- Bump only affected platforms. Android's `versionName` and increasing `versionCode` live in `app/build.gradle.kts`. Windows's package/UI version lives in `desktop/version.properties`.
- Android-only: tag `vX.Y.Z` matching Android; keep the Windows version unchanged.
- Windows-only: tag `windows-vX.Y.Z` matching Windows; keep Android version/code unchanged and do not mark this release GitHub **latest**. Android's installed updater reads `/releases/latest`.
- Shared: bump both to the same `X.Y.Z`, tag `vX.Y.Z` and publish both artifacts. This Android-containing release may become latest.
- Add 3–5 concise Persian user-facing notes at `docs/releases/<tag>.md` before tagging. Never put technical jargon in them.

## Verification and recovery

Run the repository's local compile/test gates and `python -m unittest discover -s scripts/release -p "test_*.py"`; keep unit tests local. Validate workflow edits with actionlint. Windows changes also require desktop tests, visual review, packaging and `--verify-runtime`. Local APK assembly/signing remains forbidden.

CI selects platform jobs independently and uploads to a draft; publication requires success of all selected jobs and presence of their artifacts. Missing/unchanged version bumps fail before expensive builds. Recovery dispatches the workflow from main with the exact existing tag and `target=android` or `windows`; it may skip the other platform only if that tag already contains its required asset. Recovery does not promote an older already-published release to latest.

Close the task issue after **all selected platform artifacts** are published and verified. A Windows-only task does not need an unnecessary APK build/release. Failed shared builds remain open until the missing platform is recovered. Use native `gh run watch`, not polling loops.
