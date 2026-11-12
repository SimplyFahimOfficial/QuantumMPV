#!/usr/bin/env python3
"""Guard QuantumMPV-specific UI and player features during upstream integrations."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def read(relative_path: str) -> str:
    path = ROOT / relative_path
    if not path.is_file():
        raise AssertionError(f"Required feature file is missing: {relative_path}")
    return path.read_text(encoding="utf-8")


def require(pattern: str, text: str, description: str) -> None:
    if not re.search(pattern, text, flags=re.MULTILINE | re.DOTALL):
        raise AssertionError(f"Missing protected feature behavior: {description}")


def main() -> int:
    """Check protected UI and player features, returning zero or raising AssertionError."""
    appearance_model = read("app/src/main/java/com/quantummpv/app/preferences/AppearancePreferences.kt")
    design_tokens = read("app/src/main/java/com/quantummpv/app/ui/theme/DesignTokens.kt")
    appearance_screen = read("app/src/main/java/com/quantummpv/app/ui/preferences/AppearancePreferencesScreen.kt")
    searchable_preferences = read("app/src/main/java/com/quantummpv/app/ui/preferences/SearchablePreference.kt")
    search_navigation = read("app/src/main/java/com/quantummpv/app/ui/preferences/SettingsSearchNavigation.kt")
    main_screen = read("app/src/main/java/com/quantummpv/app/ui/browser/MainScreen.kt")
    video_card = read("app/src/main/java/com/quantummpv/app/ui/browser/cards/VideoCard.kt")
    glass_components = read("app/src/main/java/com/quantummpv/app/ui/theme/GlassComponents.kt")
    audio_properties_sheet = read("app/src/main/java/com/quantummpv/app/ui/player/controls/components/sheets/AudioPropertiesSheet.kt")
    equalizer_sheet = read("app/src/main/java/com/quantummpv/app/ui/player/controls/components/sheets/EqualizerSheet.kt")
    strings = read("app/src/main/res/values/strings.xml")

    # Surface styles, glass opacity/blur sliders, tint/edge modes and Cinema mode were removed on purpose.
    # Every surface is solid except the floating dock under the experimental Liquid Glass switch.
    for removed_key in (
        "surface_style", "glass_bottom_navigation", "glass_fallback_style", "glass_intensity", "glass_blur_level",
        "dynamic_tint_mode", "edge_highlight_mode", "refraction_mode", "cinema_mode",
    ):
        if f'"{removed_key}"' in appearance_model:
            raise AssertionError(f"removed surface preference is registered again: {removed_key}")
    require(
        r'fun rememberGlassConfig\(\): GlassConfig \{[\s\S]*?return GlassConfig\(motionStyle = motionStyle, performanceMode = performanceMode\)',
        design_tokens,
        "resolved glass config is always Classic and only carries motion and performance",
    )
    if re.search(
        r'pref_section_glass_surface|pref_glass_surface_style_title|pref_glass_intensity_title|pref_cinema_mode_title|'
        r'pref_dynamic_tint_mode_title|pref_edge_highlight_mode_title|surfaceStyle|cinemaMode|dynamicTintMode by|edgeHighlightMode by',
        appearance_screen,
    ):
        raise AssertionError("Surface Appearance, surface style, opacity, Cinema, tint and edge controls must stay out of Appearance settings")
    for removed_title in (
        "pref_glass_surface_style_title", "pref_glass_intensity_title", "pref_cinema_mode_title",
        "pref_dynamic_tint_mode_title", "pref_edge_highlight_mode_title",
    ):
        if removed_title in searchable_preferences or removed_title in search_navigation or f'name="{removed_title}"' in strings:
            raise AssertionError(f"removed Appearance control remains searchable, anchored or translated: {removed_title}")
    require(
        r'PreferenceCard \{[^\n]*\n\s*//[^\n]*\n\s*Preference\(\s*modifier = Modifier\.settingsSearchTarget\(R\.string\.pref_liquid_glass_title\),'
        r'[\s\S]*?onClick = \{ backstack\.navigateTo\(LiquidGlassPreferencesScreen\) \},\s*\)\s*PreferenceDivider\(\)\s*'
        r'Row\([\s\S]{0,200}isThemeSectionExpanded = !isThemeSectionExpanded',
        appearance_screen,
        "Liquid Glass is the first row of the Theme card, above the collapsible header",
    )
    if "Haze" in glass_components or "haze" in glass_components:
        raise AssertionError("GlassComponents must not depend on Haze again")
    if re.search(r'(?i)\bhaze\b', read("gradle/libs.versions.toml") + read("app/build.gradle.kts")):
        raise AssertionError("the Haze dependency must stay removed")
    require(
        r'val liquidGlassCapture = rememberLiquidGlassCapture\(\)\s+val glassDock = liquidGlassCapture != null',
        main_screen,
        "experimental Liquid Glass is the only thing that turns the dock into glass",
    )
    if len(re.findall(r'glass\s*=\s*glassDock', main_screen)) != 4:
        raise AssertionError("Liquid Glass must reach every navigation dock renderer")
    if "glass = glassBottomNavigation" in main_screen or "LocalGlassConfig" in main_screen:
        raise AssertionError("a dock renderer bypasses the Liquid Glass override")
    # Glass may sample the page content only from siblings of it: never provide the backdrop to the content.
    require(r'\.captureLiquidGlass\(liquidGlassCapture\)', main_screen, "page content is recorded for the dock to refract")
    require(r'ProvideLiquidGlassBackdrop\(liquidGlassCapture\) \{\s+com\.quantummpv\.app\.ui\.browser\.components\.FloatingGlassMenuPanel',
            main_screen, "overflow menu is a sibling of the recorded content")
    # The Surface Appearance card is gone, so every card below Theme moved up by two list items.
    require(r'anchorItemIndex = 3,[\s\S]{0,250}pref_tree_flatten_depth_title', searchable_preferences,
            "file-browser search results target the shifted card")
    require(r'anchorItemIndex = 5,[\s\S]{0,250}pref_appearance_thumbnail_position_title', searchable_preferences,
            "thumbnail search results target the shifted card")
    require(r'anchorItemIndex = 7,[\s\S]{0,250}pref_nav_music_title', searchable_preferences,
            "navigation search results target the shifted card")
    require(r'anchorItemIndex = 11,[\s\S]{0,250}pref_anim_controls_style_title', searchable_preferences,
            "animation search results target the shifted card")
    require(r'pref_appearance_unlimited_name_lines_title, itemIndex = 3', search_navigation,
            "file-browser fallback anchors use the current list index")
    require(r'pref_appearance_show_video_thumbnails_title, itemIndex = 5', search_navigation,
            "thumbnail fallback anchors use the current list index")
    print("PASS: surface styles are gone, Liquid Glass leads Theme, and the dock follows only Liquid Glass.")

    player_controls_screen = read("app/src/main/java/com/quantummpv/app/ui/preferences/PlayerControlsPreferencesScreen.kt")
    require(r'appearancePrefs\.restoreClassicPlayerLayout\(\)[\s\S]{0,200}playerPrefs\.showControlsDrawer\.set\(true\)',
            player_controls_screen, "Restore classic layout brings back the full legacy button set and the controls drawer")
    require(r'pref_layout_restore_classic', searchable_preferences, "Restore classic layout is searchable")
    require(r'pref_layout_restore_classic', search_navigation, "Restore classic layout has a search anchor")
    print("PASS: the classic player layout can be restored from Player controls settings.")

    require(r'val useGlassCardSurface = surfaceStyle != SurfaceStyle\.CLASSIC && surfaceStyle != SurfaceStyle\.MINIMAL',
            video_card, "glass card surfaces are enabled only for glass-capable styles")
    require(r'if \(isSelected\)\s*\{[\s\S]{0,150}tertiaryContainer', video_card,
            "selected cards retain their existing selection color instead of using the glass surface")
    require(r'if \(appTheme == AppTheme\.Aurora && !isSelected\)', video_card,
            "every unselected Aurora card keeps its gradient independently of glass style")
    require(
        r'fun GlassMediaCardSurface\([\s\S]*?shape: Shape = [^\n]+[\s\S]*?GlassContainer\(modifier = modifier, shape = shape, content = content\)',
        glass_components,
        "media-card surface forwards the caller-provided shape",
    )
    for sheet in (audio_properties_sheet, equalizer_sheet):
        require(r'contentWindowInsets = \{[\s\S]*?WindowInsetsSides\.Top \+ WindowInsetsSides\.Horizontal', sheet,
                "modal sheet leaves bottom inset available to its background")
        require(r'windowInsetsPadding\(WindowInsets\.navigationBars\.only\(WindowInsetsSides\.Bottom\)\)', sheet,
                "sheet background paints behind the navigation-bar inset")
    print("PASS: VideoCard keeps selection, Aurora gradients, and card shape.")

    subtitle_installer = read("app/src/main/java/com/quantummpv/app/ui/player/SubtitleFontInstaller.kt")
    mpv_view = read("app/src/main/java/com/quantummpv/app/ui/player/MPVView.kt")
    native_checks = read("tools/test_native_artifacts.py")
    require(r'FONT_SHA256\s*=\s*"[0-9a-f]{64}"', subtitle_installer, "pinned Unicode fallback font checksum")
    require(r'SubtitleFontInstaller\.install\(context\.applicationContext\)', mpv_view,
            "Unicode fallback installed before mpv font discovery")
    require(r'GoNotoCurrent-Regular\.ttf', native_checks, "subtitle-font artifact regression test")
    print("PASS: multilingual subtitle fallback remains pinned, installed, and checked by CI.")

    require(r'forceRightToLeftSubtitles\s*=\s*preferenceStore\.getBoolean\("sub_force_rtl",\s*false\)',
            read("app/src/main/java/com/quantummpv/app/preferences/SubtitlesPreferences.kt"),
            "persisted RTL bidi-compatibility subtitle setting")
    require(r'sub-vsfilter-bidi-compat', mpv_view, "mpv bidi compatibility option remains wired")
    require(r'player_sheets_sub_force_rtl_title', strings, "RTL subtitle compatibility label remains available")
    print("PASS: RTL bidi compatibility remains available for right-to-left/script edge cases.")

    osd_font = read("app/src/main/java/com/quantummpv/app/ui/player/MpvOsdFont.kt")
    require(r'MpvOsdFont\.ensureInstalled\(context\.applicationContext\)', mpv_view,
            "bundled OSD font is installed before player initialization")
    require(r'configuredDirectory\s*=\s*configuredFontsDirectory[\s\S]{0,500}fileName\s*=\s*MpvOsdFont\.FONT_FILE_NAME',
            mpv_view, "OSD font is mirrored into mpv.conf-selected font directories")
    require(r'FONT_SHA256\s*=\s*"[0-9a-f]{64}"', osd_font, "bundled OSD font content verification")
    require(r'"osd-font"', read("app/src/main/java/com/quantummpv/app/preferences/MpvConfigOverride.kt"),
            "mpv.conf remains authoritative for OSD font configuration")
    print("PASS: OSD font choice is installed with integrity verification and respects mpv.conf ownership.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
