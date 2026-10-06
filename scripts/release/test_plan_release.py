import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from plan_release import classify, release_plan, select_targets, versions


def artifact(tag, android=True, windows=True):
    names = (["app-release.apk"] if android else []) + (["Qirato-Windows-x64-1.0.0.zip"] if windows else [])
    return {"tag_name": tag, "draft": False, "assets": [{"name": name, "state": "uploaded"} for name in names]}


class PolicyTests(unittest.TestCase):
    def test_platform_paths_and_shared_assets(self):
        self.assertEqual((False, True), classify(["desktop/src/main/Main.kt"]))
        self.assertEqual((True, False), classify(["app/src/main/AndroidManifest.xml"]))
        self.assertEqual((True, True), classify(["core/src/commonMain/Policy.kt"]))
        self.assertEqual((True, True), classify(["shared-ui/src/jvmMain/Fonts.kt"]))
        self.assertEqual((True, True), classify(["app/src/main/res/font/vazirmatn_regular.ttf"]))
        self.assertEqual((True, True), classify(["app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"]))
        self.assertEqual((False, False), classify(["README.md", ".agents/rules/platform-release-routing.md"]))

    def test_manual_recovery_can_skip_only_already_uploaded_platform(self):
        self.assertEqual((False, True), select_targets(["core/a.kt"], ["core/a.kt"], "windows", artifact("v1.0.0", windows=False)))
        with self.assertRaises(ValueError):
            select_targets(["core/a.kt"], ["core/a.kt"], "windows", {"assets": []})

    def test_missing_platform_baseline_is_conservative(self):
        self.assertEqual((False, True), select_targets([], None, "auto", {}))


class GitReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.old_cwd = os.getcwd()
        os.chdir(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.email", "release-test@example.invalid")
        self.git("config", "user.name", "Release policy test")
        self.write_versions("1.0.0", "1.0.0", 1)
        self.commit("v1.0.0")
        self.releases = [artifact("v1.0.0")]

    def tearDown(self):
        os.chdir(self.old_cwd)
        self.temp.cleanup()

    def git(self, *args):
        subprocess.run(["git", *args], check=True, capture_output=True)

    def write(self, path, text):
        target = Path(path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def write_versions(self, android, windows, code):
        self.write("app/build.gradle.kts", f'versionName = "{android}"\nversionCode = {code}\n')
        self.write("desktop/version.properties", f"version={windows}\n")

    def commit(self, tag):
        self.git("add", ".")
        self.git("commit", "-qm", tag)
        self.git("tag", tag)

    def test_windows_only_skips_android_and_keeps_its_version(self):
        self.write_versions("1.0.0", "1.0.1", 1)
        self.write("desktop/src/main/Main.kt", "Windows change")
        self.commit("windows-v1.0.1")
        plan = release_plan("windows-v1.0.1", "auto", self.releases)
        self.assertEqual("false", plan["android"])
        self.assertEqual("true", plan["windows"])

    def test_android_only_compares_each_platform_to_its_own_artifact(self):
        self.write_versions("1.0.0", "1.0.1", 1)
        self.write("desktop/src/main/Main.kt", "Windows change")
        self.commit("windows-v1.0.1")
        self.releases.insert(0, artifact("windows-v1.0.1", android=False))
        self.write_versions("1.0.1", "1.0.1", 2)
        self.write("app/src/main/Main.kt", "Android change")
        self.commit("v1.0.1")
        plan = release_plan("v1.0.1", "auto", self.releases)
        self.assertEqual("true", plan["android"])
        self.assertEqual("false", plan["windows"])
        self.assertEqual("windows-v1.0.1", plan["windows_base"])

    def test_shared_change_requires_both_and_matching_versions(self):
        self.write_versions("1.0.1", "1.0.1", 2)
        self.write("core/src/commonMain/Policy.kt", "shared change")
        self.commit("v1.0.1")
        plan = release_plan("v1.0.1", "auto", self.releases)
        self.assertEqual(("true", "true"), (plan["android"], plan["windows"]))

    def test_unpublished_windows_change_is_not_hidden_by_android_release(self):
        self.write_versions("1.0.1", "1.0.0", 2)
        self.write("desktop/src/main/Main.kt", "not yet released")
        self.commit("v1.0.1")
        with self.assertRaisesRegex(ValueError, "Bump windows"):
            release_plan("v1.0.1", "auto", self.releases)

    def test_windows_only_generic_tag_is_rejected(self):
        self.write_versions("1.0.0", "1.0.1", 1)
        self.commit("v1.0.1")
        with self.assertRaisesRegex(ValueError, "Windows-only"):
            release_plan("v1.0.1", "auto", self.releases)

    def test_legacy_android_tag_without_desktop_module_is_supported(self):
        Path("desktop/version.properties").unlink()
        self.commit("v0.9.0")
        self.assertEqual("1.0.0", versions("v0.9.0")["android"])
        self.assertEqual("0.0.0", versions("v0.9.0")["windows"])

    def test_recovery_of_partial_shared_release_builds_only_missing_windows_asset(self):
        self.write_versions("1.0.1", "1.0.1", 2)
        self.write("core/Policy.kt", "shared")
        self.commit("v1.0.1")
        self.releases.insert(0, artifact("v1.0.1", windows=False))
        plan = release_plan("v1.0.1", "windows", self.releases)
        self.assertEqual(("false", "true"), (plan["android"], plan["windows"]))

    def test_documents_do_not_require_any_release(self):
        self.write("README.md", "documentation")
        self.commit("v1.0.1")
        with self.assertRaisesRegex(ValueError, "No platform"):
            release_plan("v1.0.1", "auto", self.releases)


if __name__ == "__main__":
    unittest.main()
