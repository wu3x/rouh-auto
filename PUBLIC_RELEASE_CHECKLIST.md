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
