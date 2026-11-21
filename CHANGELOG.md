# Changelog
## 2.0.0

### Highlights

- **Pick up where you left off**: Home features your latest video with its thumbnail, title, playback position and progress. Tap to resume; dismiss it to hide it until you play something else.
- **Make Home yours**: Pin folders into a dedicated section, browse cleaner video titles, and see resolution/codec and resume details in the list. Selection, playlist reordering, pull-to-refresh and list updates now have clearer motion and haptic feedback.
- **More player controls**: New installs get the full portrait and landscape button layout, with rows that scroll when space is limited. Adjust button-background opacity, use the controls drawer, or change playback speed with a two-finger vertical swipe (0.5×–4×); pinch-to-zoom and panning remain available.
- **Undo and menu customization**: Undo removals from Recents and playlists (permanent file deletions cannot be undone). Customize optional More-menu rows and choose whether to show its menu button.
- **Music and playback tools**: Edit lyrics in the app, apply AutoEq headphone correction with presets fetched from cdn.jsdelivr.net, choose additional Ambient effects, view on-device watching statistics, and optionally keep a status line visible over video.
- **Dolby Vision direct output (FongMi build)**: Opt-in direct output is available; player-control glass is unavailable while it is active because Dolby Vision frames must use the native SurfaceView.
- **Help and support**: Find the Telegram community, bug reporting, project site and developer contact in Help & Support. Support My Work has a device-generated UPI QR code, copy/share options and payment links.
- **Quiet Geometry polish**: A documented design system brings consistent player-panel headers, sheet drag handles, touch feedback, empty states and calmer motion. Reduced motion and Android's animation setting are respected throughout.
- **Experimental Liquid Glass**: Opt-in glass for the navigation dock, menu and mini player requires Android 12+; lens refraction requires Android 13+. The separate player-control glass option also requires Android 12+. Glass is off by default and may use more battery or affect video output; turn it off if playback looks wrong.

> The visual changes in this release were not visually reviewed on a device. This is separate from the automated and instrumented CI checks.

### Changes

- **Appearance settings simplified**: Removed the old surface-style picker, glass-opacity control, Cinema mode and Haze-based surfaces. Regular app surfaces are solid; Liquid Glass remains optional for supported floating controls. Older saved glass styles now fall back to solid surfaces.
- **Player layout migration**: New installs use the full button layout. Existing installs saved with the earlier compact default move to the full layout; other custom layouts are preserved. Reset to default restores the full layout, while Use recommended layout selects the compact preset.
- **Thumbnail source**: Choose Native (default), Auto (MediaStore then Native) or MediaStore only. Switching sources clears the thumbnail cache, including existing Native thumbnails; they are regenerated as needed.
- **Audio focus**: The optional Play with other apps setting lets other apps keep audio focus while QuantumMPV plays. It is off by default; unplugging headphones still pauses playback.
- **Player controls**: Button-background opacity defaults to 55%. Two-finger speed adjustment is on by default and can be disabled in Gestures.

### Fixes and refinements

- **Opening and playing media**: Fixed filenames containing `#`, additional video/audio MIME types shared by other apps, external playback positions, track state, yt-dlp quality choices and inherited `mpv.conf` HTTP headers. Aspect-ratio changes now reset manual zoom and pan, while restoring a saved ratio preserves them.
- **Library and storage**: Incomplete metadata probes no longer leave a missing duration cached as final; entries already stored without a duration are probed again. Duplicate storage volumes no longer cause picker key collisions, and Music avoids rescanning on every tab visit. Large artwork is decoded at reduced size, remote images are capped, and interrupted downloads no longer cache broken thumbnails.
- **Network and casting**: Local casting advertises the address reachable by the Cast device without listening on other interfaces. Network playback now handles decomposed Unicode filenames, common on macOS.
- **Player reliability**: Fixed PiP window hints, screenshot-write timing, runtime application of player options, saved-subtitle selection, secondary-subtitle styling and ambient vignette rendering. TheIntroDB requests now use its v4 API.
- **Rotation and opening-video stability**: Music playback's automatic orientation now respects Android's rotation lock. Initial video-orientation metadata is read off the main thread, reducing freezes and ANRs with large files and cloud/SAF providers.
- **Performance and privacy**: Reduced unnecessary playback/UI work, improved music artwork caching and startup preparation, and corrected the bundled Baseline Profile package. Release builds omit debug logs; sensitive tokens and keys are no longer included in relevant error logs.
- **Accessibility and motion**: Added TalkBack labels to key settings and player actions, restored the system navigation contrast scrim, improved touch targets, and made player/screen transitions honor Reduced motion and disabled system animations.
- **Website**: Fixed missing showcase images and their layout, improved keyboard-focus and touch behavior, and added standard security headers. Navigation and theme switching are more reliable, including when browser storage is unavailable.

### Documentation

- Updated the README, privacy policy and project citations to reflect the current app, its permissions and online features, build steps, bundled components and support options.

## 1.4.0
### What's New
- **Two surface styles**: The surface picker now offers Classic and Clear Glass, and Classic is the default for new installs and for anyone who never chose a style. Saved legacy styles are mapped automatically: Soft, Frosted, Liquid and AMOLED Glass become Clear Glass, while Minimal and Cinema become Classic. Cinema keeps its no-tint/no-highlight behavior until it is migrated.
- **Library Insights**: A new screen, opened from the new floating menu, summarizes what you have played using only data already stored on this device: videos played, completed, unfinished, played in the last 7 days, plus "Continue watching" and "Recently played" lists. Videos that no longer exist are ignored, and the screen follows the Recently Played setting, showing nothing when it is off. Total watch time is not shown because it is not tracked yet.
- **Responsive floating dock**: The overflow control is pinned inside the navigation pill and the separate circular Search action sits immediately beside it. On narrow screens, tab labels collapse and the 48 dp tab targets scroll inside the pill instead of colliding with Search or hiding destinations.
- **Contextual Search**: The floating Search action now opens the active tab’s existing search and keyboard for Home, Music, Recents, Playlists, Network, and Jellyfin. The top-bar Search action is suppressed only while the floating dock owns it, avoiding duplicates; nested screens retain their normal action.
- **Glass menu**: The Settings and Library Insights panel now renders in-window through the shared lightweight Haze glass treatment, expands above its overflow trigger, dismisses on outside tap or Back, and follows Off/Reduced motion settings.
- **Library search screen**: Home (album view) search is now its own screen. The dock's Search circle morphs into the search bar, and the dock, mini player and quick-play FAB step aside so results and the keyboard get the full height. Recent searches are kept on the device and shown while the field is empty.
- **Morphing play/pause**: Player, audio player and mini-player play/pause buttons now morph between the triangle and the two bars.
- **Dock and menu motion**: The selected-tab pill slides to the new tab on a spring, including jumps across several tabs, and overflow menu tiles shrink slightly on press.

### Fixes
- **Search**: Searching videos finds them again: matching is case-insensitive on file name and path and reads the library directly instead of a media index that was never filled. An empty search box no longer shows "No results found".
- **Upgrade crash**: Upgrading from v1.0/v1.1 (database version 23) or a preview build (version 24) no longer fails to open the library database. The new migration creates every table and column added since, and is covered by upgrade-path tests.
- **Secure folder**: Other apps can no longer ask the player to open files in QuantumMPV's private storage, which previously allowed hidden secure-folder videos to play without the PIN.
- **Updates**: Update APKs download to a temporary file and are installed only after their size and published SHA-256 match. An interrupted download is no longer offered as ready to install.
- **Saved secrets**: Seerr, AI-provider and subtitle-provider API keys and Audiobookshelf tokens are now encrypted at rest; existing values are migrated automatically. Backups skip secure-folder media and regenerable thumbnails.
- **Resume position**: Playback progress is saved every 15 seconds while playing, so a crash or low-memory kill loses at most 15 seconds of progress.
- **Floating dock**: The Search circle now matches the navigation pill's height and surface, and the Settings / Library Insights menu uses a solid surface in Classic appearance instead of glass.
- **Surface opacity**: The glass "Surface opacity" slider now visibly changes how see-through glass surfaces are.
- **Library errors**: A failed library scan shows an error with a "Try again" button instead of looking like an empty library.
- **Sharing**: Sharing Media Info and Lua scripts works again (the shared files were outside the provider's allowed folders).
- **Accessibility and motion**: Selection-bar buttons keep a 48 dp touch target and have translated labels; cards announce their selected state; PiP controls are translated; idle pulses and shimmers stop under Reduced/Off motion, and the system "Remove animations" setting now applies immediately.
- **Themed icon**: The monochrome launcher and notification icon now show the QuantumMPV ring-and-play mark.

## 1.3.1-preview.3
This preview builds on v1.3.1-preview.2 with smoother navigation, floating app search, and refinements to glass surfaces and player controls.
### What's New
- **Surface style choices**: Keep four distinct styles—Classic, Frosted Glass, Clear Glass, and Liquid Glass—while mapping saved legacy styles to the closest retained option and preserving Cinema's no-tint/no-highlight behavior.
- **Smoother navigation**: Reduce jitter when switching between distant tabs, defer navigation geometry updates until transitions settle, and use lighter glass rendering.
- **Floating app search**: Open Settings Search from a quick-access floating button that hides when space is limited or selection mode is active.
- **Player and browser polish**: Improve HUD contrast while controls are visible, keep multi-column card titles consistent while honoring the full-name preference, and constrain audio/equalizer sheets to 460 dp in landscape.

## 1.3.1-preview.2
This preview builds on v1.3.1-preview.1 with the completed Glass & Surface Style integration.
### What's New
- **Consistent surface styling**: Apply the selected appearance across browser video cards, navigation, player controls, the audio mini-player, and audio/equalizer sheets.
- **Appearance choices**: Make Liquid Glass the default while retaining Classic and Minimal styles; navigation presentation now follows the selected surface style.
- **Style compatibility**: Preserve selected-card styling and legacy navigation behavior, and normalize imported tint settings for supported styles.

## 1.3.0
Preview release containing the compatible mpvRx 2.7.0, 2.7.1, and 2.7.2 improvements selected for QuantumMPV without replacing its custom playback, subtitle, configuration, or release architecture.
### What's New
- **Playback and playlist reliability**: Improved network playlist persistence and refresh behavior, media counting, subtitle layout handling, and Android TV D-pad seeking.
- **Subtitle improvements**: Added verified Unicode fallback-font support, independent secondary-subtitle scale and position controls, RTL compatibility, and safer subtitle option handling.
- **Player and appearance improvements**: Added settings-search coverage, OSD font preference handling across reused player cores, mini-player refinements, and compatible visual polish.
- **Build and compatibility updates**: Preserved 16-KB native page alignment, pinned native artifacts, and updated compatible build and dependency infrastructure.
### Fixes
- **D-pad seeking**: Avoid seeking to zero for unknown-duration media and ignore key-up events that did not start a scrub.
- **Network playlists**: Preserve persisted network references during playlist refreshes and include them in playlist totals.
- **Subtitle positioning**: Correct automatic secondary-subtitle compensation during zoom and pan.
- **Configuration ownership**: Continue honoring user-owned `mpv.conf` options while avoiding unsupported secondary subtitle writes.
- **Security and integrity**: Verify the bundled subtitle font using its expected size and SHA-256 checksum before installation.

## 1.2.0
The second stable QuantumMPV release, bringing the 1.2.0 preview cycle's media-server integrations, network settings redesign, secure-folder hardening, and playback reliability work together in one public build.
### What's New
- **Audiobookshelf support**: Media Server settings now include Audiobookshelf connection and full server management alongside Jellyfin, Seerr, and Navidrome.
- **Dedicated Network section**: Media Servers and Network destinations are grouped under a new Network section, with P2P streaming, HLS proxy, and the yt-dlp Manager moved out of Advanced.
- **Theme customizer and wallpapers**: Ported the custom theme customizer and wallpaper selection screens for deeper personalization.
- **Advanced gestures**: Restored configurable swipe action zones and nested tab gesture support.
- **Server music playback**: Navidrome and Subsonic server playback is fully integrated into the QuantumMPV media pipeline.
### Fixes
- **Folder discovery**: Videos now appear even when Android MediaStore has not indexed them, with a direct filesystem scan covering Movies, external storage, and USB/OTG volumes.
- **Stale index handling**: Folder contents are read directly and reconciled with MediaStore every time a folder opens, so folders are never visibly empty.
- **Video startup and surfaces**: Video tracks stay enabled while mpv initializes, and playback survives Activity, PiP, and background Surface transitions without opening to a blank screen.
- **Secure Folder protection**: Biometric unlock is now cryptographically bound to an Android Keystore-backed cipher, with device-credential fallback preserved.
- **Build reliability**: Pinned native artifacts, hardened dependency resolution, and corrected instrumentation-test APK handling across CI.
## 1.2.0-preview.8
This hotfix fixes the remaining case where folders were visible but their videos did not appear.
### Hotfix
- **Folder contents**: Read the selected folder directly instead of relying only on the asynchronous media index.
- **Stale MediaStore/index data**: Reconcile actual filesystem files with MediaStore every time a folder opens.
- **Folder discovery**: Keep filesystem discovery for Movies, external storage, and USB/OTG volumes.
## 1.2.0-preview.7
This hotfix makes videos appear in QuantumMPV folders even when Android MediaStore has not indexed them yet.
### Hotfix
- **Folder discovery**: Supplement MediaStore with a direct filesystem scan.
- **Movies and external storage**: Cover the standard Movies directory and external/OTG volumes.
- **Existing customizations**: Keep the current QuantumMPV browser and playback behavior intact.
## 1.2.0-preview.6
This hotfix restores video playback when the player Surface is created, recreated, or temporarily lost.
### Hotfix
- **Video loading**: Keep video tracks enabled while mpv initializes so videos do not open to a blank screen.
- **Surface transitions**: Preserve mpv demuxer buffering across Activity, PiP, and background Surface changes.
- **Renderer startup**: Defer only the renderer attachment until Android provides a valid Surface.
- **Orientation**: Retain the launch orientation until valid video aspect metadata is available.

## 1.2.0-preview.5
This preview republishes the completed Network settings and Audiobookshelf integration to the public preview channel.
### What's New
- **Network section**: Media Servers and Network are grouped under a dedicated Network section.
- **Audiobookshelf**: Media Server settings include Audiobookshelf connection and server management.
- **Network tools**: P2P streaming, HLS proxy, and yt-dlp Manager are grouped under Network instead of Advanced.

## 1.2.0-preview.4
This preview reorganizes network settings and completes Audiobookshelf server management.
### What's New
- **Network settings**: Added a dedicated Network section with separate Media Servers and Network destinations.
- **Audiobookshelf settings**: Added connect, edit, switch, delete, and add-another-server controls alongside Jellyfin, Seerr, and Navidrome.
- **Simpler navigation**: Moved P2P streaming, HLS proxy, and the yt-dlp manager out of Advanced into Network.

## 1.2.0-preview.3
This preview includes the secure-folder authentication hardening and Android CI reliability fixes.
### What's New
- **Secure Folder protection**: Biometric unlock is now cryptographically bound to an Android Keystore-backed cipher, while device-credential fallback remains available.
- **Android CI reliability**: Instrumentation-test APKs are no longer incorrectly treated as application APKs during native artifact verification.
- **Dependency and native hardening**: Pinned native artifacts and patched dependency resolution remain enforced across build and release workflows.


These notes are written in plain English and focus on changes that matter in everyday use.

## 1.2.0-preview.2

This preview brings the highly anticipated missing features from mpvRx 2.6.0 into QuantumMPV, resolving all compatibility barriers.

### What's New

- **Audiobooks & Servers**: Fully ported the Audiobookshelf integration and Navidrome/Subsonic server playback support into the new QuantumMPV media pipeline.
- **Themes & Wallpapers**: Ported the custom theme customizer and wallpaper selection screens.
- **Advanced Gestures**: Restored and integrated the configurable swipe action zones and nested tab gesture support.
- **Core Reliability**: Resolved 100+ architectural merge conflicts and compile errors introduced by the deep codebase rename and refactor, aligning the upstream features with the modern SQLite-based FTS library approach.


## 1.2.0-preview.1

This preview brings the latest media-library, download, playback, and build reliability work together for testing before the next stable release.

### What's New

- **Unified media library**: Added a Room-backed media index with SQLite full-text search and a WorkManager-powered filesystem scan.
- **More reliable playback**: Improved external playlist handling, network lifecycle cleanup, pause latency, and video-output safeguards.
- **Safer downloads**: Made yt-dlp output names collision-safe and kept temporary download files isolated during cleanup.
- **Build reliability**: Restored shared duration formatting, completed the media scanner dependencies, and verified the Android CI, release build, static-analysis, and preview pipelines.

### APK variants

Preview packages use the same variant layout as the stable release: Standard Universal and architecture-specific APKs, FongMi Universal, and Non-Vulkan Universal.

## 1.1.0

The first stable QuantumMPV 1.1.0 release, bringing the current playback, library, streaming, download, appearance, and branding work together in one public build.

### What's New

- **Liquid Glass appearance option**: Added an opt-in appearance switch for translucent glass treatments while keeping the solid, battery-friendly default experience.
- **Appearance polish**: Refined settings cards, bottom navigation, Home accents, motion behavior, and contrast across light and dark themes.
- **Share-to-Quick-Download**: Send supported web links to QuantumMPV from another Android app and choose Best available, up to 1080p, up to 720p, up to 480p, or Audio only.
- **Durable download queue**: yt-dlp jobs are persisted in Room, recover safely after process recreation, and use atomic claims to prevent duplicate execution.
- **Download quality persistence**: The selected quick-download format is stored with the queue job and survives app restarts.
- **Library Insights**: Added an on-device diagnostics view for history entries, unique items, recent activity, missing local files, and network items without uploading playback data.
- **Branding refresh**: Applied the QuantumMPV logo and stable app labels across the launcher, fallback assets, landing page, favicon metadata, and repository presentation assets.
- **Support and credits**: Updated developer support information and expanded project credits and links in the app and project documentation.
- **Release reliability**: Improved Room migration coverage, structured diagnostics, release APK validation, and CI artifact handling. Made both detekt and ktlint blocking while fixing existing formatting findings.

### APK variants

All variants contain the same QuantumMPV app version and features, with bundled native mpv backends. Choose the package that best matches your device:

- **Standard** (`QuantumMPV-<architecture>-<version>.apk`): Recommended for most devices. Available as Universal and architecture-specific APKs with the standard Vulkan-capable playback backend.
- **FongMi** (`QuantumMPV-fongmi-<version>.apk`): Universal package using the FongMi native profile for Vulkan, direct MediaCodec, and Dolby Vision playback.
- **Non-Vulkan** (`QuantumMPV-no-vulkan-<version>.apk`): Universal OpenGL-only package for devices without compatible Vulkan support or with unstable legacy GPU drivers.

## Initial release 1.0.0

The first public QuantumMPV release, bringing the core playback, library, streaming, download, customization, and branding work together in one stable build.

### Highlights

- Share supported links for quick yt-dlp downloads.
- Browse playback history and local media from the library.
- Use the supplied QuantumMPV branding across the app and project materials.
- Support the developer through the documented UPI destination `simplyfahim@axl`.

### Credits

QuantumMPV builds on open-source projects including mpvRx, MpvRex, MpvInfinity, Mpvex, and mpv-android. See [CITATION.md](CITATION.md) for the project links and full attribution details.
