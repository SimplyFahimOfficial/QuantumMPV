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

Existing image available for the UPI card: `docs/assets/support/upi-qr.png` (decodes to `upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR`, payee Md Fahim).

Anchors to keep working: `#support`, `#simplyfahimsbi`, and the legacy `#support-development` alias. `.github/FUNDING.yml` and `SUPPORT.md` link to the current `#support` heading.

The `[!TIP] Support QuantumMPV` callout stays at the top with the verified Md Fahim payee, and the separate `## Community` section remains unchanged.

## Inventory check after Phase 4

Checked against GitHub's own Markdown renderer (`POST /markdown`, gfm mode):

| # | Item | Still present |
|---|---|---|
| 1 | `Support` heading (anchor `#support`) | Yes, unchanged |
| 2 | Issue tracker sentence, word for word | Yes |
| 3 | Issue tracker link | Yes (new owner name) |
| 4 | Ko-fi badge image, same URL and alt text | Yes, now in the Ko-fi card |
| 5 | `https://ko-fi.com/simplyfahim` | Yes |
| 6 | `` `SimplyFahim@sbi` `` heading (anchor `#simplyfahimsbi`) | Yes, now an H3 outside the HTML table so GitHub can generate the permalink. |
| 7 | "UPI ID · Select and copy" | Yes |

Added: `#support-development` anchor, banner and card SVGs (light/dark), the QR image, the Telegram hero card and badge, and Instagram and email badges. The personal Telegram badge is left commented out until `TODO_PERSONAL_TELEGRAM_USERNAME` is filled in.
