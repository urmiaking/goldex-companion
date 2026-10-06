"""Select release jobs against each platform's last published artifact, not the last tag."""
import argparse
import json
import os
import re
import subprocess
from pathlib import Path

TAG = re.compile(r"^(windows-)?v(\d+\.\d+\.\d+)$")
SHARED = ("core/", "shared-ui/", "gradle/", ".github/workflows/", ".github/actions/", "scripts/release/")
ROOT_BUILD = {"build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat"}


def command(*args, required=True):
    result = subprocess.run(args, text=True, encoding="utf-8", capture_output=True)
    if required and result.returncode:
        raise RuntimeError(f"Command failed: {args[0]} {args[1]}")
    return result


def version_tuple(version):
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("Expected a three-part version")
    return tuple(int(part) for part in version.split("."))


def classify(paths):
    android = windows = False
    for path in paths:
        if path.startswith(SHARED) or path in ROOT_BUILD or path.startswith("app/src/main/res/font/") or re.match(r"app/src/main/res/mipmap-[^/]+/ic_launcher\.png$", path):
            android = windows = True
        elif path.startswith("app/"):
            android = True
        elif path.startswith("desktop/"):
            windows = True
        elif path.endswith(".gradle.kts"):
            android = windows = True
    return android, windows


def versions(ref):
    app = command("git", "show", f"{ref}:app/build.gradle.kts").stdout
    android = re.search(r'versionName\s*=\s*"([\d.]+)"', app).group(1)
    code = int(re.search(r"versionCode\s*=\s*(\d+)", app).group(1))
    props = command("git", "show", f"{ref}:desktop/version.properties", required=False)
    if props.returncode == 0:
        windows = re.search(r"^version=([\d.]+)$", props.stdout, re.M).group(1)
    else:
        desktop = command("git", "show", f"{ref}:desktop/build.gradle.kts", required=False)
        match = re.search(r'packageVersion\s*=\s*"([\d.]+)"', desktop.stdout)
        windows = match.group(1) if match else "0.0.0"
    version_tuple(android)
    version_tuple(windows)
    return {"android": android, "android_code": code, "windows": windows}


def has_asset(release, platform):
    return any(asset.get("state", "uploaded") == "uploaded" and
               (asset["name"] == "app-release.apk" if platform == "android" else
                bool(re.fullmatch(r"Qirato-Windows-x64-[\d.]+\.zip", asset["name"])))
               for asset in release.get("assets", []))


def baseline(releases, platform, target):
    candidates = []
    for release in releases:
        ref = release["tag_name"]
        if ref == target or release.get("draft") or release.get("prerelease") or not TAG.fullmatch(ref) or not has_asset(release, platform):
            continue
        # A published artifact from an unrelated branch is not a source baseline.
        if command("git", "merge-base", "--is-ancestor", ref, target, required=False).returncode:
            continue
        candidates.append((version_tuple(TAG.fullmatch(ref).group(2)), ref))
    # All supported release tags encode the published platform version. Read only
    # the newest matching ancestor; legacy Android-only tags need no desktop module.
    return max(candidates)[1] if candidates else None


def changed_paths(base, target):
    return command("git", "diff", "--name-only", "-z", base, target, "--").stdout.split("\0") if base else None


def select_targets(android_changes, windows_changes, requested, current_release):
    android = android_changes is None or classify(android_changes)[0]
    windows = windows_changes is None or classify(windows_changes)[1]
    if requested != "auto":
        selected_android = requested in ("android", "both")
        selected_windows = requested in ("windows", "both")
        if android and not selected_android and not has_asset(current_release, "android"):
            raise ValueError("Android changes remain unpublished; select both platforms")
        if windows and not selected_windows and not has_asset(current_release, "windows"):
            raise ValueError("Windows changes remain unpublished; select both platforms")
        return selected_android, selected_windows
    return android, windows


def release_plan(tag, requested, releases):
    if not TAG.fullmatch(tag):
        raise ValueError("Expected vX.Y.Z or windows-vX.Y.Z")
    command("git", "rev-parse", "--verify", f"{tag}^{{commit}}")
    current = next((release for release in releases if release["tag_name"] == tag), {"assets": []})
    bases = {platform: baseline(releases, platform, tag) for platform in ("android", "windows")}
    android, windows = select_targets(changed_paths(bases["android"], tag), changed_paths(bases["windows"], tag), requested, current)
    if not android and not windows:
        raise ValueError("No platform build changes since published artifacts; no release needed")
    new = versions(tag)
    if requested == "auto":
        for platform, selected in (("android", android), ("windows", windows)):
            if selected and bases[platform]:
                old = versions(bases[platform])
                if version_tuple(new[platform]) <= version_tuple(old[platform]):
                    raise ValueError(f"Bump {platform} version before publishing its changes")
                if platform == "android" and new["android_code"] <= old["android_code"]:
                    raise ValueError("Bump Android versionCode")
        if not android and not tag.startswith("windows-v"):
            raise ValueError("Windows-only releases use windows-vX.Y.Z to preserve Android update discovery")
    if android and tag != "v" + new["android"]:
        raise ValueError("Android tag must match versionName")
    if windows and TAG.fullmatch(tag).group(2) != new["windows"]:
        raise ValueError("Release tag must match desktop package version")
    return {"android": str(android).lower(), "windows": str(windows).lower(), "windows_version": new["windows"],
            "android_version": new["android"], "tag": tag, "android_base": bases["android"] or "initial", "windows_base": bases["windows"] or "initial"}


def github_releases(repo):
    pages = json.loads(command("gh", "api", f"repos/{repo}/releases", "--paginate", "--slurp").stdout)
    return [release for page in pages for release in page]


def prepare(tag, requested, repo):
    releases = github_releases(repo)
    plan = release_plan(tag, requested, releases)
    notes = command("git", "show", f"{tag}:docs/releases/{tag}.md", required=False)
    existing = next((release for release in releases if release["tag_name"] == tag), None)
    body = notes.stdout if notes.returncode == 0 else (existing or {}).get("body")
    if not body:
        raise ValueError("Add concise Persian release notes in docs/releases/<tag>.md")
    Path("release-notes.md").write_text(body, encoding="utf-8")
    if existing is None:
        command("gh", "release", "create", tag, "--draft", "--verify-tag", "--latest=false", "--title", f"قیراط {tag}", "--notes-file", "release-notes.md", "--repo", repo)
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as stream:
            for key, value in plan.items():
                stream.write(f"{key}={value}\n")
    print(json.dumps(plan, ensure_ascii=False))


def finalize(tag, repo, windows_version, android, windows):
    release = json.loads(command("gh", "release", "view", tag, "--repo", repo, "--json", "isDraft,assets").stdout)
    names = {asset["name"] for asset in release["assets"]}
    if android and "app-release.apk" not in names:
        raise ValueError("Android asset missing")
    if windows and f"Qirato-Windows-x64-{windows_version}.zip" not in names:
        raise ValueError("Windows asset missing")
    args = ["gh", "release", "edit", tag, "--repo", repo, "--draft=false"]
    # Recovery of an already-published release leaves GitHub latest selection untouched.
    if release["isDraft"]:
        latest = command("gh", "api", f"repos/{repo}/releases/latest", "--jq", ".tag_name", required=False)
        latest_tag = latest.stdout.strip()
        publish_latest = "app-release.apk" in names and tag.startswith("v") and (
            not TAG.fullmatch(latest_tag) or version_tuple(TAG.fullmatch(tag).group(2)) >= version_tuple(TAG.fullmatch(latest_tag).group(2)))
        args.append("--latest=" + str(publish_latest).lower())
    command(*args)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", required=True)
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", "urmiaking/goldex-companion"))
    parser.add_argument("--target", choices=["auto", "android", "windows", "both"], default="auto")
    parser.add_argument("--finalize", action="store_true")
    parser.add_argument("--windows-version", default="")
    parser.add_argument("--android", default="false")
    parser.add_argument("--windows", default="false")
    args = parser.parse_args()
    if not TAG.fullmatch(args.tag):
        raise SystemExit("Invalid release tag")
    if args.finalize:
        finalize(args.tag, args.repo, args.windows_version, args.android == "true", args.windows == "true")
    else:
        prepare(args.tag, args.target, args.repo)
