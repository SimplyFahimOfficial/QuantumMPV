#!/usr/bin/env python3
"""Fetch pinned mpvlib AARs and verify their provenance and APK/AAB native contents."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "native-artifacts.json"
LIBS = ROOT / "app" / "libs"
ABI_DIRS = {"armeabi-v7a", "arm64-v8a", "x86", "x86_64"}
REQUIRED_NATIVE = {"libmpv.so", "libplayer.so", "libavcodec.so", "libavformat.so", "libavutil.so"}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_manifest() -> dict:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if data.get("schema_version") != 1 or set(data.get("artifacts", {})) != {"standard", "noVulkan", "fongmi"}:
        raise SystemExit("native-artifacts.json has an unsupported or incomplete schema")
    return data


def fetch_artifacts(manifest: dict) -> None:
    LIBS.mkdir(parents=True, exist_ok=True)
    for flavor, spec in manifest["artifacts"].items():
        destination = LIBS / spec["file"]
        with tempfile.NamedTemporaryFile(dir=LIBS, delete=False) as temp:
            temporary = Path(temp.name)
        try:
            print(f"Fetching pinned {flavor} artifact {spec['download']}")
            urllib.request.urlretrieve(spec["download"], temporary)
            actual = sha256(temporary)
            if actual != spec["sha256"]:
                raise SystemExit(f"SHA-256 mismatch for {flavor}: expected {spec['sha256']}, got {actual}")
            temporary.replace(destination)
        finally:
            temporary.unlink(missing_ok=True)


def verify_aars(manifest: dict) -> None:
    for flavor, spec in manifest["artifacts"].items():
        path = LIBS / spec["file"]
        if not path.is_file():
            raise SystemExit(f"Missing {path}; run tools/verify_native_artifacts.py --download")
        actual = sha256(path)
        if actual != spec["sha256"]:
            raise SystemExit(f"AAR checksum mismatch for {flavor}: expected {spec['sha256']}, got {actual}")
        with zipfile.ZipFile(path) as aar:
            names = set(aar.namelist())
            actual_abis = {name.split("/")[1] for name in names if name.startswith("jni/") and name.count("/") == 2}
            if actual_abis != ABI_DIRS:
                raise SystemExit(f"{flavor} AAR ABI set {sorted(actual_abis)} != {sorted(ABI_DIRS)}")
            for abi in ABI_DIRS:
                for library in REQUIRED_NATIVE:
                    if f"jni/{abi}/{library}" not in names:
                        raise SystemExit(f"{flavor} AAR is missing jni/{abi}/{library}")
        print(f"PASS AAR {flavor}: {spec['artifact_revision']} {actual}")


def expected_flavor(package_path: Path) -> str:
    name = package_path.name.lower()
    if "fongmi" in name:
        return "fongmi"
    if "novulkan" in name or "no-vulkan" in name:
        return "noVulkan"
    return "standard"


def verify_native_package(manifest: dict, package_path: Path, is_bundle: bool) -> None:
    if not package_path.is_file():
        raise SystemExit(f"Missing {'AAB' if is_bundle else 'APK'}: {package_path}")
    flavor = expected_flavor(package_path)
    if is_bundle and flavor != "standard":
        raise SystemExit(f"{package_path}: only the standard flavor is published as an AAB")
    spec = manifest["artifacts"][flavor]
    module_prefix = "base/" if is_bundle else ""
    lib_root = f"{module_prefix}lib/"
    metadata_name = f"{module_prefix}assets/native-build-metadata.properties"

    with zipfile.ZipFile(package_path) as package:
        names = set(package.namelist())
        if metadata_name not in names:
            raise SystemExit(f"{package_path}: missing {metadata_name}")
        metadata = package.read(metadata_name).decode("utf-8")
        required_metadata = {
            "artifact_revision": spec["artifact_revision"],
            "source_revision": spec["source_revision"],
            "mpv_revision": spec["mpv_revision"],
            "ffmpeg_revision": spec["ffmpeg_revision"],
            "yt_dlp_revision": spec["yt_dlp_revision"],
            "quickjs_revision": spec["quickjs_revision"],
        }
        for key, value in required_metadata.items():
            if f"{flavor}.{key}={value}" not in metadata:
                raise SystemExit(f"{package_path}: metadata does not prove {flavor}.{key}={value}")

        abi_index = 2 if is_bundle else 1
        minimum_slashes = 3 if is_bundle else 2
        package_abis = {
            name.split("/")[abi_index]
            for name in names
            if name.startswith(lib_root) and name.endswith(".so") and name.count("/") >= minimum_slashes
        }
        if is_bundle and package_abis != ABI_DIRS:
            raise SystemExit(f"{package_path}: AAB ABI set {sorted(package_abis)} != {sorted(ABI_DIRS)}")
        if not package_abis or not package_abis <= ABI_DIRS:
            raise SystemExit(f"{package_path}: unexpected native ABI entries {sorted(package_abis)}")

        aar_path = LIBS / spec["file"]
        with zipfile.ZipFile(aar_path) as aar:
            for abi in package_abis:
                for library in REQUIRED_NATIVE:
                    package_name = f"{lib_root}{abi}/{library}"
                    if package_name not in names:
                        raise SystemExit(f"{package_path}: missing {package_name}")
                    package_bytes = package.read(package_name)
                    aar_bytes = aar.read(f"jni/{abi}/{library}")
                    if hashlib.sha256(package_bytes).digest() != hashlib.sha256(aar_bytes).digest():
                        raise SystemExit(f"{package_path}: {abi}/{library} differs from pinned {spec['file']}")

    kind = "AAB" if is_bundle else "APK"
    print(f"PASS {kind} {package_path}: {flavor} {spec['artifact_revision']} ABIs={','.join(sorted(package_abis))}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--download", action="store_true", help="download exact manifest artifacts")
    parser.add_argument("--apk", action="append", type=Path, help="verify a final APK; may be repeated")
    parser.add_argument("--aab", action="append", type=Path, help="verify a standard App Bundle; may be repeated")
    args = parser.parse_args()
    manifest = load_manifest()
    if args.download:
        fetch_artifacts(manifest)
    verify_aars(manifest)
    for apk in args.apk or []:
        verify_native_package(manifest, apk if apk.is_absolute() else ROOT / apk, is_bundle=False)
    for aab in args.aab or []:
        verify_native_package(manifest, aab if aab.is_absolute() else ROOT / aab, is_bundle=True)


if __name__ == "__main__":
    try:
        main()
    except (OSError, urllib.error.URLError, zipfile.BadZipFile) as error:
        print(f"native artifact verification failed: {error}", file=sys.stderr)
        sys.exit(1)
