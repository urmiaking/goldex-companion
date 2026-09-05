---
trigger: always_on
description: Invariant requiring simple, non-technical Persian changelogs on GitHub releases for in-app display.
---

# User-Facing Release Changelog Invariant

## 1. Core Principle
All GitHub releases must include a simple, concise, and non-technical changelog in **Persian** explaining what has changed from the end-user's perspective (the goldsmith/jeweler).

## 2. Prohibited Content
- **NO Developer Jargon**: Do not mention PR numbers (`#12`), commit SHAs (`6dc61c4`), source code file names (`DashboardScreen.kt`), internal refactors, or technical dependencies.
- **NO Auto-Generated Diffs**: Never leave the release body solely to GitHub's auto-generated compare links (e.g. `**Full Changelog**: https://...`).

## 3. Required Format & Tone
- Written in polite, professional, and accessible Persian (زبان ساده و روان برای صنف طلا و جواهر).
- Categorized using clean visual bullet points or badges:
  - `✨ قابلیت‌های جدید` (New features)
  - `⚡ بهبودها و رفع اشکالات` (Improvements & bug fixes)
- Keep to **3 to 5 concise bullet points** so it fits cleanly inside the in-app `UpdateDialog` without scrolling clutter.

### Standard Template:
```markdown
نسخه جدید قیراط با تغییرات و بهبودهای زیر آماده استفاده است:

✨ **تغییرات و قابلیت‌های جدید:**
- بهبود عملکرد و پایداری بخش‌های مختلف برنامه
- رفع اشکالات گزارش شده و بهینه‌سازی تجربه کاربری
```

## 4. Release Publishing Protocol
Whenever publishing or finalizing a release:
1. Before or immediately after the cloud CI workflow publishes the release, update the release body using GitHub CLI:
   ```bash
   gh release edit <tag> --notes "<Persian changelog>"
   ```
2. Verify with `gh release view <tag>` to ensure the release body is cleanly formatted and contains the Persian text.
