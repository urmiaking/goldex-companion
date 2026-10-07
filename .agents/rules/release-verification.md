---
trigger: always_on
description: Verify Android and Windows delivery using existing evidence and release metadata, without downloading artifacts again.
---

# Release verification without artifact re-downloads

The user's policy is mandatory for both Android and Windows:

- After implementing a feature or publishing a release, do not download the Windows MSI/ZIP or Android APK again solely to check correctness, size, hash, or publication.
- Do not use `gh release download`, `gh run download`, browser downloads, or HTTP downloads for this redundant verification.
- Use the available local compile/test results and local desktop runtime/package checks, together with the relevant CI job results and logs.
- Confirm publication through `gh release view` or the GitHub API: the expected tag, published status, and required platform assets must be present. Inspect reported asset names, sizes, and digests when available; do not fetch artifact bytes to recompute them. Missing digest metadata alone does not justify downloading an artifact.
- Report exactly what the evidence establishes. Do not claim a downloaded package, local installation, or end-to-end runtime check occurred unless it actually did.

Existing required tests and CI packaging/installer checks remain mandatory. This rule governs post-implementation/post-publication verification; it does not disable the app's updater downloads or downloads explicitly requested by the user for another task.
