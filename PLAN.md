# Plan: username change, Telegram group, Support redesign

Branch `feat/support-community-links`, one commit per phase, debug build (`:app:assembleStandardDebug`) after each.

## Values

| Item | Value | Source |
|---|---|---|
| Old GitHub username | `Fahimofficial` | README links, `SupportConfig`, `UpdateManager`, CODEOWNERS (the git remote already points at the new name) |
| New GitHub username | `SimplyFahimOfficial` | Given |
| Telegram group | `https://t.me/Quantummpv` | Given |
| Instagram | `SimplyFahimofficial` | `index.html`, `fastlane/metadata/android/preview-site/index.html` |
| Email | `SimplyFahim@astermail.com` (only one found) | Same two pages |
| Personal Telegram | not found anywhere | Placeholder `TODO_PERSONAL_TELEGRAM_USERNAME` in `SupportConfig.kt`; the tile stays hidden until it is set |

The Instagram handle `SimplyFahimofficial` contains the old username as a substring. It is not a GitHub link and stays unchanged.

## Phases

1. **Username.** Replace `github.com/Fahimofficial`, `api.github.com/repos/Fahimofficial`, `fahimofficial.github.io`, `@Fahimofficial` (CODEOWNERS) and `Fahimofficial/QuantumMPV` in docs, `.github/`, fastlane, site pages, app code, all `values*/strings.xml`, tests and `tools/validate_quantummpv_identity.py`. Upstream and third-party links, display names, applicationId, package, signing and version codes stay as they are.
2. **Telegram in the repo.** A "Join the Telegram group" badge in the README header. `.github/ISSUE_TEMPLATE/config.yml` does not exist, so no contact link is added and no template is created.
3. **In-app Support screen.** Order: UPI, Ko-fi, Community (Telegram hero card), Personal contact (Instagram, email, personal Telegram if set), then the existing About links. Add one `SupportIntents.openLink()` with copy + toast fallback, `SupportLinks` builders with unit tests, a UPI fallback that copies the ID, long-press copy, a tap haptic, Simple Icons brand vectors, and a 250 ms entrance that is skipped under reduced motion.
4. **README Support section.** Self-hosted SVG banner and cards (light/dark), a UPI card with the QR, a Ko-fi card, a Telegram hero card, and small personal contact badges.

## Current README Support content (inventory before Phase 4)

`## Support` section (anchor `#support`):

1. Heading text: `Support`
2. Text: "Use the [issue tracker](…/QuantumMPV/issues) for reproducible bug reports and feature requests. Contributions, testing, documentation, and translations are welcome through pull requests."
3. Link: `https://github.com/<owner>/QuantumMPV/issues` (issue tracker)

Donation block at the end of the README (after Star History):

4. Image: `https://img.shields.io/badge/Support_on-Ko--fi-FF5E5B?style=for-the-badge`, alt "Support SimplyFahim on Ko-fi"
5. Link: `https://ko-fi.com/simplyfahim`
6. Heading: `` `SimplyFahim@sbi` `` (anchor `#simplyfahimsbi`)
7. Text: "**UPI ID · Select and copy**"

Existing image available for the UPI card: `docs/assets/support/upi-qr.png` (decodes to `upi://pay?pa=SimplyFahim@sbi&pn=SimplyFahim&cu=INR`).

Anchors to keep working: `#support`, `#simplyfahimsbi`, and `#support-development` (linked from `.github/FUNDING.yml` and `SUPPORT.md`, which currently has no target; Phase 4 adds one).

Out of scope: the `[!TIP] Support QuantumMPV` callout at the top and the separate `## Community` section stay unchanged, apart from the username replacement.
