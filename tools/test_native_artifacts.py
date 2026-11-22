#!/usr/bin/env python3
"""Regression checks for deterministic native artifact packaging and subtitle fonts."""
from hashlib import sha256
import importlib.util
import json
from pathlib import Path
import re
import subprocess
from tempfile import TemporaryDirectory
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'native-artifacts.json').read_text())
app_gradle = (root / 'app/build.gradle.kts').read_text()
cmake = (root / 'app/src/main/cpp/CMakeLists.txt').read_text()
assert 'mpvlib-no-vulkan.aar' in app_gradle
assert 'mpvlib-no-vulkun.aar' not in app_gradle
assert 'set(MPVRX_NATIVE_LINK_OPTIONS' in cmake
assert '"-Wl,-z,max-page-size=16384"' in cmake
assert '$<$<CONFIG:Release>:-Wl,-z,max-page-size=16384>' not in cmake
assert 'target_link_options(${target} PRIVATE ${MPVRX_NATIVE_LINK_OPTIONS})' in cmake
# AGP 9.4.1 is the floor that moved Android tooling off vulnerable Bouncy Castle 1.79
# (see NATIVE_ARTIFACT_AUDIT.md). Newer AGP releases are accepted so routine
# Dependabot bumps do not fail this check; downgrades below the floor still do.
agp_match = re.search(r'^agp = "(\d+)\.(\d+)\.(\d+)"$', (root / 'gradle/libs.versions.toml').read_text(), re.MULTILINE)
assert agp_match, 'gradle/libs.versions.toml must pin agp to a stable MAJOR.MINOR.PATCH version'
assert tuple(map(int, agp_match.groups())) >= (9, 4, 1), f'AGP {".".join(agp_match.groups())} is below the 9.4.1 security floor'
wrapper_properties = (root / 'gradle/wrapper/gradle-wrapper.properties').read_text()
wrapper_match = re.search(r'distributionUrl=.*/gradle-(\d+)\.(\d+)(?:\.(\d+))?-bin\.zip$', wrapper_properties, re.MULTILINE)
assert wrapper_match, 'Gradle wrapper must use a stable -bin distribution'
assert tuple(int(part or 0) for part in wrapper_match.groups()) >= (9, 6, 0), 'Gradle wrapper is below the 9.6.0 floor'
assert 'org.bouncycastle:bcprov-jdk18on:1.86' in app_gradle
assert 'org.bouncycastle:bcpkix-jdk18on:1.86' in app_gradle
assert 'requested.group == "org.bouncycastle" -> useVersion("1.85")' in (root / 'build.gradle.kts').read_text()
assert 'configurations.classpath' in (root / 'build.gradle.kts').read_text()
assert 'standard.artifact_revision=mpvlib-v1.0.10' in (root / 'app/src/main/assets/native-build-metadata.properties').read_text()
assert (root / 'native-artifacts.properties').read_text() == (root / 'app/src/main/assets/native-build-metadata.properties').read_text()
for flavor, spec in manifest['artifacts'].items():
    assert len(spec['sha256']) == 64
    assert len(spec['source_revision']) == 40
    assert spec['file'].endswith('.aar')
assert not subprocess.check_output(['git', 'ls-files', 'app/libs'], cwd=root, text=True).strip(), 'checked-in AARs bypass the pinned fetch gate'
for workflow in ('build.yml', 'ci.yml', 'pre-release.yml', 'preview.yml', 'release.yml'):
    text = (root / '.github/workflows' / workflow).read_text()
    assert 'verify_native_artifacts.py --download' in text, workflow

release_code = int(re.search(r'^val releaseVersionCode = (\d+)$', app_gradle, re.MULTILINE).group(1))
band_size = int(re.search(r'^val versionCodeBandSize = ([\d_]+)$', app_gradle, re.MULTILINE).group(1).replace('_', ''))
stable_base = release_code * band_size + band_size - 1
apk_codes = [stable_base * 10 + abi for abi in range(5)]
aab_code = stable_base * 10 + 9
preview_min = ((release_code + 1) * band_size + 1) * 10
next_stable_min = ((release_code + 1) * band_size + band_size - 1) * 10
assert max(apk_codes) < aab_code < preview_min < next_stable_min
assert 'val stableBundleVersionCode = stableVersionCode * 10 + 9' in app_gradle
assert 'versionCode = if (bundleWithoutAbiSplits) stableBundleVersionCode else stableVersionCode' in app_gradle
release_workflow = (root / '.github/workflows/release.yml').read_text()
assert 'verify_native_artifacts.py --aab "$play_bundle"' in release_workflow
assert 'verify_native_artifacts.py --aab "$aab"' in release_workflow
assert 'bundletool-all-1.18.3.jar' in release_workflow
assert 'dump_value /manifest/@android:versionName' in release_workflow
assert 'dump_value /manifest/@android:versionCode' in release_workflow
assert 'expected_bundle_code=$(( stable_code * 10 + 9 ))' in release_workflow

font = root / 'app/src/main/assets/subtitle-fonts/GoNotoCurrent-Regular.ttf'
font_license = root / 'app/src/main/assets/subtitle-fonts/OFL.txt'
installer = root / 'app/src/main/java/com/quantummpv/app/ui/player/SubtitleFontInstaller.kt'
player = root / 'app/src/main/java/com/quantummpv/app/ui/player/MPVView.kt'
installer_text = installer.read_text()
player_text = player.read_text()
assert font.is_file(), 'bundled Unicode subtitle fallback font is missing'
assert font.stat().st_size == 14_700_060, 'unexpected bundled subtitle font size'
assert sha256(font.read_bytes()).hexdigest() == '882afbab965608c2d2bc627fd8016b962aa5a6be2d358f9de24a7b5967c5632e', 'bundled subtitle font checksum mismatch'
assert font_license.is_file() and 'SIL OPEN FONT LICENSE Version 1.1' in font_license.read_text()
assert 'ASSET_PATH = "subtitle-fonts/GoNotoCurrent-Regular.ttf"' in installer_text
assert 'SubtitleFontInstaller.install(context.applicationContext)' in player_text
assert 'SHA-256' in installer_text and 'sub-fonts-dir' in player_text

# Exercise the AAB verifier with a synthetic App Bundle and pinned AAR fixture.
module_spec = importlib.util.spec_from_file_location('verify_native_artifacts', root / 'tools/verify_native_artifacts.py')
assert module_spec and module_spec.loader
verifier = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(verifier)
artifact_spec = {
    'file': 'standard.aar',
    'artifact_revision': 'fixture-artifact',
    'source_revision': 'fixture-source',
    'mpv_revision': 'fixture-mpv',
    'ffmpeg_revision': 'fixture-ffmpeg',
    'yt_dlp_revision': 'fixture-yt-dlp',
    'quickjs_revision': 'fixture-quickjs',
}
fixture_manifest = {'artifacts': {'standard': artifact_spec}}
metadata = '\n'.join(f'standard.{key}={value}' for key, value in artifact_spec.items() if key != 'file')

with TemporaryDirectory() as temporary:
    fixture_dir = Path(temporary)
    verifier.LIBS = fixture_dir
    aar_path = fixture_dir / artifact_spec['file']
    with ZipFile(aar_path, 'w') as aar:
        for abi in verifier.ABI_DIRS:
            for library in verifier.REQUIRED_NATIVE:
                aar.writestr(f'jni/{abi}/{library}', f'{abi}/{library}'.encode())

    def write_package(path: Path, bundle: bool, omit_abi: str | None = None, corrupt: tuple[str, str] | None = None) -> None:
        prefix = 'base/' if bundle else ''
        with ZipFile(path, 'w') as package:
            package.writestr(f'{prefix}assets/native-build-metadata.properties', metadata)
            abis = verifier.ABI_DIRS - ({omit_abi} if omit_abi else set())
            for abi in abis:
                for library in verifier.REQUIRED_NATIVE:
                    contents = f'{abi}/{library}'.encode()
                    if corrupt == (abi, library):
                        contents = b'not-the-pinned-native-library'
                    package.writestr(f'{prefix}lib/{abi}/{library}', contents)

    valid_aab = fixture_dir / 'QuantumMPV-standard-fixture.aab'
    write_package(valid_aab, bundle=True)
    verifier.verify_native_package(fixture_manifest, valid_aab, is_bundle=True)

    incomplete_aab = fixture_dir / 'QuantumMPV-standard-incomplete.aab'
    write_package(incomplete_aab, bundle=True, omit_abi='x86_64')
    try:
        verifier.verify_native_package(fixture_manifest, incomplete_aab, is_bundle=True)
    except SystemExit as error:
        assert 'AAB ABI set' in str(error)
    else:
        raise AssertionError('AAB verifier accepted a missing ABI')

    altered_aab = fixture_dir / 'QuantumMPV-standard-altered.aab'
    write_package(altered_aab, bundle=True, corrupt=('arm64-v8a', 'libmpv.so'))
    try:
        verifier.verify_native_package(fixture_manifest, altered_aab, is_bundle=True)
    except SystemExit as error:
        assert 'differs from pinned' in str(error)
    else:
        raise AssertionError('AAB verifier accepted a changed native library')

    apk = fixture_dir / 'QuantumMPV-arm64-v8a-fixture.apk'
    write_package(apk, bundle=False, omit_abi='armeabi-v7a')
    verifier.verify_native_package(fixture_manifest, apk, is_bundle=False)

print('native artifact and subtitle font regression checks passed')
