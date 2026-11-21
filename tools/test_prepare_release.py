#!/usr/bin/env python3
"""Regression tests for tools/prepare_release.py."""
from __future__ import annotations

import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import prepare_release as release  # noqa: E402


class PrepareReleaseTest(unittest.TestCase):
    def prepare(self, version: str) -> tuple[str, str]:
        with tempfile.TemporaryDirectory() as directory:
            build_file = Path(directory) / "app" / "build.gradle.kts"
            build_file.parent.mkdir()
            build_file.write_text(
                'val releaseVersionCode = 10405\nversionName = "1.4.5"\n',
                encoding="utf-8",
            )
            output = io.StringIO()
            with (
                mock.patch.object(release, "BUILD_FILE", build_file),
                mock.patch.object(sys, "argv", ["prepare_release.py", version]),
                contextlib.redirect_stdout(output),
            ):
                release.main()
            return build_file.read_text(encoding="utf-8"), output.getvalue()

    def test_prepares_v2_0_0_with_monotonic_release_code(self) -> None:
        contents, output = self.prepare("2.0.0")
        self.assertIn('versionName = "2.0.0"', contents)
        self.assertIn("val releaseVersionCode = 20000", contents)
        self.assertIn("Prepared QuantumMPV 2.0.0 with releaseVersionCode 20000", output)

    def test_keeps_existing_1_x_line_supported(self) -> None:
        contents, _ = self.prepare("1.4.5")
        self.assertIn('versionName = "1.4.5"', contents)
        self.assertIn("val releaseVersionCode = 10405", contents)

    def test_accepts_the_last_version_whose_final_version_codes_fit_google_play(self) -> None:
        contents, _ = self.prepare("2.9.98")
        self.assertIn("val releaseVersionCode = 20998", contents)
        self.assertLessEqual(release.max_emitted_version_code(20998), release.MAX_VERSION_CODE)

    def test_rejects_versions_whose_abi_encoded_codes_exceed_the_play_limit(self) -> None:
        for version in ("2.9.99", "2.10.0", "2.15.0"):
            with self.subTest(version=version), tempfile.TemporaryDirectory() as directory:
                build_file = Path(directory) / "build.gradle.kts"
                original = 'val releaseVersionCode = 10405\nversionName = "1.4.5"\n'
                build_file.write_text(original, encoding="utf-8")
                with (
                    mock.patch.object(release, "BUILD_FILE", build_file),
                    mock.patch.object(sys, "argv", ["prepare_release.py", version]),
                    self.assertRaisesRegex(SystemExit, "Google Play's limit"),
                ):
                    release.main()
                self.assertEqual(build_file.read_text(encoding="utf-8"), original)

    def test_bound_matches_the_gradle_version_code_formula(self) -> None:
        # Preview band for 2.0.0: (20001 * 10000 + 9998) * 10 + ABI digit 4, as in app/build.gradle.kts.
        self.assertEqual(release.max_emitted_version_code(20000), 2_000_199_984)

    def test_rejects_unconfigured_major_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            build_file = Path(directory) / "build.gradle.kts"
            original = 'val releaseVersionCode = 10405\nversionName = "1.4.5"\n'
            build_file.write_text(original, encoding="utf-8")
            with (
                mock.patch.object(release, "BUILD_FILE", build_file),
                mock.patch.object(sys, "argv", ["prepare_release.py", "3.0.0"]),
                self.assertRaisesRegex(SystemExit, "supports the 1.x and 2.x release lines"),
            ):
                release.main()
            self.assertEqual(build_file.read_text(encoding="utf-8"), original)


if __name__ == "__main__":
    unittest.main(verbosity=2)
