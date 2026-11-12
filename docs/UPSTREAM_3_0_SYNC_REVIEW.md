# mpvRx 3.0.0 – 3.0.2 synchronization review

Covers the upstream releases reported in issues #98 (v3.0.0), #104 (v3.0.1) and #105 (v3.0.2). Each release
contains the previous one, so this single review answers all three. Policy: [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)
and [UPSTREAM_INTEGRATION.md](UPSTREAM_INTEGRATION.md).

Sources: [v3.0.0](https://github.com/Riteshp2001/mpvRx/releases/tag/v3.0.0),
[v3.0.1](https://github.com/Riteshp2001/mpvRx/releases/tag/v3.0.1),
[v3.0.2](https://github.com/Riteshp2001/mpvRx/releases/tag/v3.0.2).

## Why the automatic merge fails

- The last shared commit is `a83c3493` (2026-08-21). Since then QuantumMPV has 1,774 commits and upstream has 1,018.
- Upstream code lives in `app.gyrolet.mpvrx`, QuantumMPV's in `com.quantummpv.app`, so every Kotlin file differs by path.
- From upstream v2.7.2 (the last selective port) to v3.0.2 there are 480 commits (442 non-merge) touching 769 files
  (+95,995 / −30,480).
- After mapping upstream paths onto QuantumMPV's, only **11 of the 442** commits apply cleanly to `master`.
  A three-way apply of the 125 small fix/perf commits (≤ 4 files, ≤ 150 lines) gives 13 clean, 78 conflicting
  and 34 that touch files QuantumMPV doesn't have.

A wholesale merge would replace QuantumMPV's identity, design system and security work, so the sync stays selective,
as the policy requires.

To list the commits again:

```sh
git remote add upstream https://github.com/Riteshp2001/mpvRx.git
git fetch upstream tag v2.7.2 tag v3.0.2
git log --no-merges --reverse --format='%h %s' v2.7.2..v3.0.2
```

## Ported in this branch

Every port was reviewed by hand against QuantumMPV's code; authorship and the upstream hash are kept in the commit.

| Upstream | Change | Notes |
|---|---|---|
| `d0d5355a8` | `setOptionString` uses `mpv_set_property` once the core is initialised | libmpv's option API is pre-init only. No caller reads the return value. |
| `9ced52e2f` | No integer fallback reads for `duration` / `time-pos` | These are floating-point properties. |
| `13bc2555e` | A secondary subtitle no longer forces the primary ASS override | The primary uses `scale` unless the user forces it. |
| `9602cd1d2` | NFD filenames keep their bytes in network and proxy URLs | `java.net.URI` NFC-normalised them, which gave HTTP 404. A regression test was added. |
| `529617be8` | Three-button navigation keeps Android's contrast scrim | |
| `82829422a`, `b3cb77969`, `f440f57ee` | Library and Tree cold loads show the Folders scanning state | Uses QuantumMPV's existing localised `loading_state_scanning_message` instead of upstream's hard-coded text. |
| `f5f616b5d` (vignette part, by hand) | Vignette fade uses `1.0 - smoothstep(0.1, 1.3, r)` | Reversed smoothstep edges are undefined in GLSL. QuantumMPV's Glow shader has no precision-qualified constructors, so the rest of that commit doesn't apply. |
| `afd38886a` (by hand) | TheIntroDB v4 API | QuantumMPV called v2 with a v1 fallback. v1 is removed and v2/v3 are frozen ([migration guide](https://github.com/TheIntroDB/theintrodb-npm/blob/main/docs/migration-v3-to-v4.md)). `TheIntroDbV4Test` parses the v4 examples from TheIntroDB's OpenAPI spec. |

### Player screen (3.0.x)

Requested explicitly, so the items marked "needs a product decision" below were revisited.

| Upstream | Change | Notes |
|---|---|---|
| `2c62f6048`, `79b7530bd` (opacity part, by hand) | Player button background opacity slider (Player controls > Appearance) | Key `player_buttons_background_opacity`, default 0.55 keeps the current look. `PlayerButtonAlpha` maps the level onto every button fill and border, including the hand-built pills. `PlayerButtonAlphaTest`. The placement options of `2c62f6048` are not ported. |
| 3.0.2 (two-finger speed part, by hand) | Two-finger swipe changes the speed in 0.25x steps, 0.5x to 4x | Key `two_finger_speed_swipe`, default on. Added to QuantumMPV's older pinch handler: pinch and pan wait until the first movement shows it is not a speed swipe, and zoomed-in video keeps two-finger panning. Cleanup moved into a `finally` so a cancelled gesture cannot leave the speed pill up. `SwipeSpeedTest`. Not checked on a device. |

The top and bottom scrims behind the controls already exist (`TopHudScrim`, `BottomHudScrim`), so upstream's single edge
vignette was not ported. Not ported from the 3.0 player: persistent status bar, Android TV style volume and brightness
capsule, redesigned clip editor, buttons beside play/pause in landscape, subtitle drag and pinch regions, and TV controls.
Each changes layout or gesture arbitration that cannot be checked without a device. The status bar in particular has no
safe position yet: it would overlap the title row, the lock button and the speed pill.

### Second batch: mpv integration fixes

Each was checked against QuantumMPV's code first; the bug exists here.

| Upstream | Change | Notes |
|---|---|---|
| `b3a12cceb` | A load with no per-video yt-dlp format keeps the quality chosen in settings | QuantumMPV wrote an empty string over it on every yt-dlp load. Known limit: a per-video override stays until the core restarts (`shortcut:` comment in `PlayerActivity`). The `vid=auto` half of that commit does not apply, because QuantumMPV does not defer video selection. |
| `b840fa11e` | A headerless item gets the `mpv.conf` HTTP headers back | QuantumMPV reset `http-header-fields` to an empty list, which dropped the user's default. The default is captured after init like `user-agent`. |
| `c4cae1f64`, `9ced52e2f` | `duration` is read as a double | Seek clamp, `resolveSeekMode` and the lyrics requests. A test guard fails if an int64 read of `duration` comes back. |
| `c4cae1f64`, `389f9509a` | `vid` and `secondary-sid` are read as strings | Both can be `auto` or `no`. |
| `5f655ccd8` | The `position` intent extra keeps millisecond precision | |
| `aa5ab1533` | Screenshots wait for the file instead of sleeping 250 ms | `ScreenshotSaverAwaitTest`. `screenshot-to-file` ignores `screenshot-format`, so that write is gone. |

### Liquid Glass (experimental)

Ported from 3.0.0–3.0.2, opt-in and off by default (Appearance > Theme > Liquid Glass). It is the Kyant
backdrop renderer (`io.github.kyant0:backdrop-android` 2.0.1 and `shapes-android` 1.2.1, Apache-2.0) with upstream's
settings page, live preview and tuning options.

- **Where it applies:** the navigation dock, its overflow menu and the mini player. `GlassContainer` picks the Kyant
  renderer only when a captured backdrop is provided, so every other surface is a solid Classic surface.
- **How it is wired:** `MainScreen` records the page content and `MainActivity` records the navigation display. A
  glass surface cannot sample a layer it is part of, so the backdrop goes only to siblings of the recorded content.
  `tools/test_quantummpv_custom_features.py` pins this.
- **Needs:** Android 12 for the blur. The lens refraction needs Android 13.
- **Not ported:** glass over the playing video. Upstream moves mpv's output to a `TextureView` so the glass can sample
  it, and a `TextureView` tone-maps HDR to SDR. That changes every playback path (HDR, PiP, external display) and
  cannot be checked without a device. Also not ported: glass sliders and seekbar thumb, player sheets, the
  "Liquid glass in HDR" option, and upstream's animated backgrounds (QuantumMPV has none). A custom wallpaper is
  recorded into its own layer, so the glass refracts it together with the page content.

Guards for these changes are in `tools/test_upstream_release_ports.py`.

## Applies cleanly but not ported

| Upstream | Reason |
|---|---|
| `50cf4a1c0`, `4aad98480` | Play Store build only. QuantumMPV has no scoped-storage flavour (`SCOPED_STORAGE_ONLY` is false everywhere). |
| `e42c3e58a`, `6bf0fc284` | Player glass-button inset and shadow tweaks. They belong to the player-video glass, which is not ported (see "Liquid Glass" below). |
| `86ba2c4f6` | Adds a closing brace for an upstream-only scope bug; QuantumMPV's `FoldersPreferencesScreen` doesn't have that bug. |

## Release features that need a product decision

These conflict with QuantumMPV's own direction and need a maintainer decision before anyone ports them:

- **Material 3 Expressive redesign, new icon, icon builder and splash** (3.0.0). QuantumMPV has its own identity and the
  "Quiet Geometry" design system (`docs/design`). Porting this would replace both.
- **Liquid Glass over the playing video** (3.0.0–3.0.2). See "Liquid Glass (experimental)" above for what was ported.
- **Profile hub, floating navigation rework.** QuantumMPV's navigation is planned separately (4 tabs, audit P2-4).
- **Play Store build.** It needs a new flavour, signing and asset split.
- **Animated Apple Music covers and the lrc.red lyrics provider.** These are new third-party services, so they need a
  privacy and terms review first. They are candidates for the planned Labs section.
- **Removal of the in-app HLS proxy** (3.0.2). QuantumMPV still uses its proxy (8 files), so removing it is a
  behaviour change in its own right.
- **Persistent status bar, live expanding music player, phone-to-TV remote.** (The two-finger speed swipe was ported, see "Player screen".) These are new
  features rather than fixes, and they interact with the gesture and HUD changes in PR #106.

## Fixes worth porting next (need manual adaptation)

These are bug fixes that matter to QuantumMPV but conflict with its code. A mechanical check
(`git apply --check` on every small upstream fix, with paths and packages mapped) finds one that applies, and that one
is in the "not ported" table above, so each of these needs a hand port:

1. **SMB stalls** (`14cb6290c`, `e59f3f82e`, `12186e541`): direct transport, bounded connects and listings, and safer
   reconnects. QuantumMPV's `SmbClient` differs; needs testing against a real SMB server.
2. **Shader commands only after libmpv initialisation** (`d8457c987`). QuantumMPV's `MPVView` init order differs.
3. **Rotation-correct startup from container headers** (3.0.1–3.0.2). QuantumMPV's orientation probe was reworked
   separately (off-main probe, CHANGELOG "Opening videos"), so compare behaviour rather than code.
4. **Buffering indicator accuracy and byte-range prefetch for slow servers** (3.0.1).
5. **Thumbnail work waits for scrolling to settle** (3.0.1).
6. **Lyrics no longer carry over to the next song** (3.0.1).
7. **Settings restore that skips damaged entries** (3.0.2). QuantumMPV's `SettingsBackupPolicy` (PR #106) already
   validates and skips entries; compare before porting the crash-revert part.
8. **mpvlib 1.1.4 native update.** QuantumMPV pins its own checksum-verified AARs (`mpvlib-v1.0.10`); updating them
   follows the native-artifact process, not a code port.

### Third batch: fixes after v3.0.2

Found by rewriting each upstream commit to QuantumMPV's package and dry-applying it to `master`: only 6 of 593 commits
since v2.7.2 apply mechanically, so these were ported by hand after checking that the bug exists here.

| Upstream | Change | Notes |
|---|---|---|
| `18470efc4` | `fileExtension()` keeps a `#` or `?` that is part of a file name | `MediaPathUtilsTest`. A scheme-less `a.mkv?t=1.5` still reads as `5` (`shortcut:` comment). |
| `5a5e8186a` | Failed metadata probes are not cached; cached rows without a duration are probed again | Upstream only stopped new rows. The refresh rule was `.ts` only here, so old rows would have stayed. |
| `4c4216c80` | Cast server advertises the interface on the receiver's subnet first | Only private IPv4 addresses qualify, because #106 binds the server to the advertised address. `CastMediaServerSubnetTest`. |
| `1407597a2` (part) | No source-rect hint while already in PiP | The orientation half builds on `usesAdaptiveOrientation()`, which QuantumMPV does not have. |
| `b27989cb0` | Choosing an aspect preset resets manual zoom; restoring a saved one does not | |
| `47720b192` (part) | Audio "Auto" orientation uses `SCREEN_ORIENTATION_USER` | QuantumMPV has no `Free` option or in-app music player. |

Checked and not needed: `66d1a4d68` (QuantumMPV already uses `hr-seek=no` with precise seeking under 120 s and during
A-B loops, and has no `scrubTo`), `155fd0b98` (`onStartCommand` already promotes first, which covers the Android 16
deadline), `171eb9066` (the legacy flags are already one assignment), `d9936e04e` (no clip subtitle loader here).

### Fourth batch: control overflow

| Upstream | Change | Notes |
|---|---|---|
| `1bf439137`, `eabbb3464` (part) | `fadingHorizontalScroll` on the landscape bottom-left group and the portrait bottom row | Edge alpha is a pure function (`FadingHorizontalScrollTest`). `shadowRoom` and `reverseScrolling` are not ported: QuantumMPV's buttons have no shadow and nothing here scrolls in reverse. The top-right and bottom-right groups keep their natural width, as upstream does. The layout-preview fade has no counterpart because QuantumMPV's preview wraps. |

### Sixth batch: Liquid Glass over the playing video (experimental)

Ported from 3.0.0-3.0.2 as an opt-in setting, off by default.

| Upstream | Change | Notes |
|---|---|---|
| `MpvSurfaceHost` | `MpvTextureHost` plus `PlayerGlassRenderer` | Upstream turned `MPVView` into a `FrameLayout` that swaps its child. Here `MPVView` stays a `SurfaceView` (`BaseMPVView` in the pinned mpvlib is one), and the texture host is a sibling that is only mounted while the option is on. With the option off nothing is added and no code path changes. |
| `shouldUseTextureRendering` | `PlayerGlassPolicy` | One pure rule used by the activity and the controls. Adds the Android 12 floor, the Ambient background exclusion and the `mpv.conf` HDR ownership check. `PlayerGlassPolicyTest`. |
| `VideoGlassBackdrop` | `VideoGlassBackdrop.kt` | Same layer-mapping approach: the texture's GPU layer is drawn into a backdrop in the controls' coordinates, so there is no `PixelCopy` or bitmap readback. |
| `PlayerGlassSurface` | reuses `liquidGlassSurface` | The dock and mini player already use it, so no second glass implementation was added. |

Renderer switching is safe because each renderer binds under its own owner token and `PlaybackSession.unbindSurface` ignores a non-owner, the same pattern the mini player and the external display use.

Not ported: glass sliders, seekbar thumb, sheets and panels, and the live-preview change for the player. Only the round buttons drawn by `ControlsButton` are glass; pills and the play/pause cluster draw their own containers and are unchanged.

Verification: compiled, ktlint, detekt, Android lint and all 341 unit tests were run locally on a JDK 17 and Android SDK 37 toolchain, and in CI. Nothing runs the feature itself: no device or emulator was available (the sandbox has no KVM), so the renderer switch, the first frame, PiP, background playback, HDR and frame pacing are unverified.

### Assessed in the second batch and left open

- **Native event callbacks can deadlock during startup** (`20238d284`). The pattern is present: `PlaybackSession`
  registers itself as the observer, and its handlers take `nativeLock`. The upstream fix queues callbacks onto a
  single-thread dispatcher, which re-plumbs the central event path. It needs a device to exercise, so it is left.
- **Shader commands before `MPVLib.init()`** (`d8457c987`). `MPVView.initOptions()` applies Anime4K and HDR Toys with
  property and `change-list` calls, which mpv rejects before init. QuantumMPV's own comment says they must stay there,
  so the two disagree, and GPU shader loading cannot be checked here. Left, with this note.
- **Recents hides playlists** (`5e2ade1a2`). A product change, not a bug fix.
- **Not applicable:** `1a421970e` (`numericWidthKey` does not exist here) and the `vid=auto` half of `b3a12cceb`.

## Note on tap timing

Upstream tried showing the controls on the first tap (`7009cc9e7`) and reverted it a day later (`340e1a644`), because
double-tap seeking on the sides then flashed the controls. PR #106 (CP-21) acts immediately only in zones whose double
tap does nothing, and keeps waiting in the left and right seek zones, so it avoids that regression.
