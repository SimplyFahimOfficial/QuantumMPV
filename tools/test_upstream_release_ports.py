#!/usr/bin/env python3
"""Regression checks for selected, architecture-adapted mpvRx 2.7.x ports."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]

cmake = (root / "app/src/main/cpp/CMakeLists.txt").read_text()
assert '"-Wl,-z,max-page-size=16384"' in cmake
assert "$<$<CONFIG:Release>:-Wl,-z,max-page-size=16384>" not in cmake

servers = (root / "app/src/main/java/com/quantummpv/app/ui/preferences/MediaServersPreferencesScreen.kt").read_text()
assert "if (existingId == null)" in servers
assert "appearancePreferences.showJellyfinTab.set(true)" in servers

network_screen = (root / "app/src/main/java/com/quantummpv/app/ui/browser/networkstreaming/NetworkBrowserScreen.kt").read_text()
network_adapter = (root / "app/src/main/java/com/quantummpv/app/ui/browser/networkstreaming/NetworkPlaylistVideo.kt").read_text()
assert "AddToPlaylistDialog(" in network_screen
assert "onLongClick = {" in network_screen
assert "isPlayableNetworkMedia(includeAudio)" in network_screen
assert "NetworkPlaybackUri.create(connectionId, path)" in network_adapter

mini_player = (root / "app/src/main/java/com/quantummpv/app/ui/browser/components/MiniPlayer.kt").read_text()
assert "progressColor.copy(alpha = 0.22f)" in mini_player
assert "progressColor.copy(alpha = 0.85f)" in mini_player
assert "val edgeWidth = 2.dp.toPx().coerceAtMost(playedWidth)" in mini_player

subtitle_preferences = (root / "app/src/main/java/com/quantummpv/app/preferences/SubtitlesPreferences.kt").read_text()
subtitle_positioning = (root / "app/src/main/java/com/quantummpv/app/ui/player/SubtitlePositioning.kt").read_text()
subtitle_panel = (root / "app/src/main/java/com/quantummpv/app/ui/player/controls/components/panels/SubtitleSettingsMiscellaneousCard.kt").read_text()
assert 'secondary_sub_scale' in subtitle_preferences and 'secondary_sub_pos", -1' in subtitle_preferences
assert "preferredSecondaryPosition >= MIN_SUBTITLE_POSITION" in subtitle_positioning
assert "resolveSecondarySubtitlePosition" in subtitle_panel
assert "player_sheets_sub_force_rtl_title" in subtitle_panel

seekbar = (root / "app/src/main/java/com/quantummpv/app/ui/player/controls/components/Seekbar.kt").read_text()
assert "DeviceFormFactor.isTelevision(LocalContext.current)" in seekbar
assert "Key.DirectionLeft, Key.DirectionRight" in seekbar
assert "animatedPosition.snapTo(finalPosition)" in seekbar
assert "onValueChangeFinished(finalPosition)" in seekbar
assert ".focusProperties { canFocus = false }" in seekbar

# Selected mpvRx 3.0.x ports (docs/UPSTREAM_3_0_SYNC_REVIEW.md).
playback_session = (root / "app/src/main/java/com/quantummpv/app/ui/player/PlaybackSession.kt").read_text()
assert "if (nativeCoreReady) {\n        MPVLib.setPropertyString(name, value)" in playback_session

network_path = (root / "app/src/main/java/com/quantummpv/app/domain/network/NetworkPath.kt").read_text()
network_proxy = (root / "app/src/main/java/com/quantummpv/app/data/network/proxy/NetworkStreamingProxy.kt").read_text()
assert 'fun encodeNetworkPathForUrl(path: String): String = Uri.encode(path, "/")' in network_path
assert "URI(SCHEME, connectionId.toString()" not in network_path
assert "encodeNetworkPathForUrl(route)" in network_proxy

assert 'val secondaryOverride = if (forceAssOverride || isSecondarySubtitleActive()) "force" else "scale"' in subtitle_positioning

intro_db = (root / "app/src/main/java/com/quantummpv/app/repository/IntroDbRepository.kt").read_text()
assert '"https://api.theintrodb.org/v4/media"' in intro_db
assert "api.theintrodb.org/v1" not in intro_db and "api.theintrodb.org/v2" not in intro_db
# The URL alone can't show markers still parse; the v4 response fixtures live in this unit test.
intro_db_test = (root / "app/src/test/java/com/quantummpv/app/repository/TheIntroDbV4Test.kt").read_text()
assert '"tvdb_id": 169' in intro_db_test and "movieResponseYieldsEverySegmentTypeWithItsTimes" in intro_db_test

main_activity = (root / "app/src/main/java/com/quantummpv/app/MainActivity.kt").read_text()
assert "window.isNavigationBarContrastEnforced = true" in main_activity

# mpvRx 3.0.x mpv-integration fixes (docs/UPSTREAM_3_0_SYNC_REVIEW.md, "Second batch").
import re

main_sources = "\n".join(p.read_text() for p in (root / "app/src/main/java").rglob("*.kt"))
# duration is a double property; an int64 read is not valid for every mpv build.
assert 'getPropertyInt("duration")' not in main_sources
# vid / sid can be "auto" or "no", so they are read as strings.
assert not re.search(r'getPropertyInt\("(vid|secondary-sid)"\)', main_sources)
player_activity = (root / "app/src/main/java/com/quantummpv/app/ui/player/PlayerActivity.kt").read_text()
assert 'PlaybackSession.setPropertyDouble("time-pos", it / MILLISECONDS_TO_SECONDS.toDouble())' in player_activity
assert 'if (requiresYtdlp && !MpvConfigOverridePolicy.isOwnedByMpvConf("ytdl-format"))' in player_activity
assert "ytdlFormat ?: YtdlpOptionsBuilder.configuredFormat(ytdlPreferences)" in player_activity
assert 'PlaybackSession.setPropertyString("ytdl-format", effectiveFormat)' in player_activity
assert 'ytdlFormat.orEmpty())' not in player_activity
assert 'MPVLib.getPropertyNode("http-header-fields")?.toObject<List<String>>(json).orEmpty()' in playback_session
assert "PlaybackHttpHeaders.forPlaybackItem(" in playback_session
assert "defaultHeaders = PlaybackHttpHeaders.fromMpvHeaderFields(defaultHttpHeaderFields)" in playback_session
header_tests = (root / "app/src/test/java/com/quantummpv/app/ui/player/PlaybackHttpHeadersTest.kt").read_text()
assert "fun preservesNativeMpvHeaderEntries()" in header_tests
screenshots = (root / "app/src/main/java/com/quantummpv/app/ui/player/screenshot/ScreenshotSaver.kt").read_text()
assert "delay(250)" not in screenshots and screenshots.count("awaitScreenshotFile(") >= 3

# mpvRx 3.0.x player screen (docs/UPSTREAM_3_0_SYNC_REVIEW.md, "Player screen").
app_src = root / "app/src/main/java/com/quantummpv/app"
assert '"player_buttons_background_opacity", 0.55f' in (app_src / "preferences/AppearancePreferences.kt").read_text()
buttons = (app_src / "ui/player/controls/components/ControlsButton.kt").read_text()
# Fill and border both go through the level; a hard-coded alpha would make the slider skip them.
assert buttons.count("PlayerButtonAlpha.background(") == 2
assert "LocalPlayerButtonsOpacity provides PlayerButtonAlpha.level(" in (app_src / "ui/player/controls/PlayerControls.kt").read_text()
for path in (
    "ui/preferences/PlayerControlsPreferencesScreen.kt",
    "ui/preferences/SearchablePreference.kt",
    "ui/preferences/SettingsSearchNavigation.kt",
):
    assert "pref_player_buttons_opacity" in (app_src / path).read_text(), path
gestures = (app_src / "ui/player/controls/GestureHandler.kt").read_text()
assert 'preferenceStore.getBoolean("two_finger_speed_swipe", true)' in (app_src / "preferences/GesturePreferences.kt").read_text()
# Speed swipe waits for the first movement so pinch zoom and two-finger panning keep working.
assert "speedSwipeRuledOut =" in gestures and "if (!speedSwipeRuledOut)" in gestures
assert re.search(r"\} finally \{\s*if \(isSubZoomMode && gestureStarted\)", gestures)
assert "pref_player_gestures_two_finger_speed_swipe" in (app_src / "ui/preferences/GesturePreferencesScreen.kt").read_text()

print("selected upstream 2.7.x and 3.0.x release-port regression checks passed")
