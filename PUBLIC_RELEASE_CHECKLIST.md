# Public release readiness

**Status: HOLD — do not make this repository public yet.**

## Completed
- [x] Historical MapTiler key revoked by its owner.
- [x] Font distribution release removed.
- [x] Current source uses Android system fonts; legacy font symbol renamed.
- [x] Current map provider no longer requires MapTiler.
- [x] Obsolete debug APK upload removed from CI; old release asset will be deleted on the next successful build.
- [x] Offline Jazan map is a separate release asset.

## Publication blockers
- [ ] **Commit author privacy:** Existing Git commits contain a personal Gmail address. Configure GitHub's private noreply commit email and publish a clean history before switching visibility, if that address should not be public.
- [ ] **Historical content:** Earlier commits contain the revoked MapTiler key and previous project files. Deleting from the latest tree does not erase history. Review tags, PRs, CI logs and artifacts. Consider a new public repository with a clean initial commit instead of exposing the private history.
- [ ] **Asset provenance:** Verify redistribution rights for the project banner JPEG, preview GIF, cover images, launcher artwork/icons and website image. No license/ownership proof is included in this repository. Do not claim third-party images are original.
- [ ] **OSM attribution:** Preserve © OpenStreetMap contributors wherever OSM-based map tiles are shown, including offline Jazan MBTiles; verify applicable ODbL/produced-work obligations for public map downloads.
- [ ] **Legacy domain:** `docs/CNAME` references `carlauncher.app`. Confirm domain ownership and whether the website should remain before removing or changing it.
- [ ] **CI/device testing:** Confirm a successful Android build and test map import, offline display, and GPS on target hardware.
- [ ] **Release audit:** Remove the obsolete `car-launcher-next-debug.apk` from the current `latest-test` release, and verify all remaining release assets are intended for public distribution.

## Publication approach
For a clean public identity, keep this repository private as the development archive and publish a new repository from the reviewed working tree with a fresh initial commit and GitHub noreply author email. If the current URL and release links must remain unchanged, a deliberate Git history rewrite and coordinated tag/release cleanup is required. Do not force-push or rewrite history without a backup.


## Public-link troubleshooting (2026-11-08)
- The GitHub repository API confirms `visibility=public`, `private=false`, and `disabled=false`. A 504 Gateway Timeout in Instagram's in-app browser is a gateway/network response, **not evidence of a copyright restriction**.
- Test `https://github.com/wu3x/rouh-auto` in Safari/Chrome and on another network; compare with `https://api.github.com/repos/wu3x/rouh-auto`. If only the in-app browser fails, use its **Open in browser** option.
- GitHub Pages is not enabled (`has_pages=false`). Do not advertise a Pages URL until deployment is configured.
- The APK download link depends on GitHub Releases. Check access without logging in and keep release assets published.
- The owner reports the artwork is AI-generated. See `ASSETS.md` for provenance and OpenStreetMap attribution. A public repository is not automatically blocked because of an artwork license.

## UI quality pass (seven incremental commits)
1. Navigation rail: selected-state outline and accessible tap label.
2. Icon buttons: improved 56dp targets, background and visual weight.
3. App grid: consistent cards, spacing and boundaries.
4. Settings rows: consistent surfaces and minimum touch height.
5. Search: readable input, alignment and full-sized trailing target.
6. Theme: unified high-contrast dark palette.
7. Publication and troubleshooting notes: this audit update.

These are code-level improvements, not a substitute for testing on multiple head-unit screen sizes and hardware. Check the latest Actions run and install the APK on a real device before release.
