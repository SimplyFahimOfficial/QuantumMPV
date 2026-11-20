#!/usr/bin/env python3
"""Update the Android version for the manually dispatched release-preparation workflow."""
from __future__ import annotations

import argparse
import re
from pathlib import Path

BUILD_FILE = Path("app/build.gradle.kts")
VERSION_RE = re.compile(r"^(?P<prefix>\s*versionName\s*=\s*\").*?(?P<suffix>\"\s*)$", re.MULTILINE)
CODE_RE = re.compile(r"^(?P<prefix>\s*val releaseVersionCode\s*=\s*)\d+(?P<suffix>\s*)$", re.MULTILINE)
SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
SUPPORTED_MAJOR_VERSIONS = {1, 2}

# The Gradle build reserves a 10,000-code band for each stable release code.
# Fixed-width decimal slots keep semantic versions numerically ordered without
# allowing a later patch/minor release to produce a smaller Android versionCode.
MINOR_PATCH_RADIX = 100
MAJOR_RADIX = 10_000
VERSION_CODE_BAND = 10_000
MAX_ABI_DIGIT = 4
# Google Play rejects version codes above 2,100,000,000 (below Kotlin's Int maximum).
MAX_VERSION_CODE = 2_100_000_000


def max_emitted_version_code(release_code: int) -> int:
    """Largest versionCode app/build.gradle.kts can emit for a release code.

    Stable builds use release_code * band + (band - 1); preview builds use the next band, up to
    (release_code + 1) * band + (band - 2). Each is then multiplied by 10 and given an ABI digit.
    """
    preview = (release_code + 1) * VERSION_CODE_BAND + VERSION_CODE_BAND - 2
    return preview * 10 + MAX_ABI_DIGIT


def encode_version_code(major: int, minor: int, patch: int) -> int:
    if minor >= MINOR_PATCH_RADIX or patch >= MINOR_PATCH_RADIX:
        raise SystemExit(
            f"minor and patch components must each be below {MINOR_PATCH_RADIX}",
        )
    release_code = major * MAJOR_RADIX + minor * MINOR_PATCH_RADIX + patch
    if max_emitted_version_code(release_code) > MAX_VERSION_CODE:
        raise SystemExit(
            f"version {major}.{minor}.{patch} would produce Android versionCodes above Google Play's "
            f"limit of {MAX_VERSION_CODE}",
        )
    return release_code


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("version", help="QuantumMPV semantic version, for example 2.0.0")
    args = parser.parse_args()
    match = SEMVER_RE.fullmatch(args.version)
    if not match:
        raise SystemExit("version must have the form MAJOR.MINOR.PATCH, for example 2.0.0")

    major, minor, patch = map(int, match.groups())
    if major not in SUPPORTED_MAJOR_VERSIONS:
        raise SystemExit(
            f"QuantumMPV supports the 1.x and 2.x release lines; received {args.version!r}",
        )
    release_code = encode_version_code(major, minor, patch)
    text = BUILD_FILE.read_text(encoding="utf-8")
    updated, version_count = VERSION_RE.subn(rf"\g<prefix>{args.version}\g<suffix>", text, count=1)
    updated, code_count = CODE_RE.subn(rf"\g<prefix>{release_code}\g<suffix>", updated, count=1)
    if version_count != 1 or code_count != 1:
        raise SystemExit("could not update exactly one versionName and releaseVersionCode")
    BUILD_FILE.write_text(updated, encoding="utf-8")
    print(f"Prepared QuantumMPV {args.version} with releaseVersionCode {release_code}")


if __name__ == "__main__":
    main()
