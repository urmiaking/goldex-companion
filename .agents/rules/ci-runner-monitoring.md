---
trigger: model_decision
description: Invariant for monitoring GitHub Actions CI/CD runs and release builds
---

# GitHub Actions CI Monitoring Invariant

When checking or monitoring GitHub Actions workflow runs, runners, or release builds:
1. **NEVER use the `schedule` tool or sleep loops** to poll build statuses at arbitrary intervals.
2. **ALWAYS use native streaming watch commands**:
   - `gh run watch <run-id> --repo <owner/repo>`
   - or `gh run view <run-id> --watch --repo <owner/repo>`
3. If run asynchronously as a background task, wait for the task completion notification instead of setting external timers.
4. Once completed, verify the release with `gh release view <tag>` or inspect the failed job log with `gh run view --log-failed`.
