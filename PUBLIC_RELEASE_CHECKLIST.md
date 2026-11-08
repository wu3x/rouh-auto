# Public release review

ROUH Auto is being prepared for public source distribution.

## Verified
- [x] Historical MapTiler key revoked by its owner.
- [x] Private font release removed from the current GitHub Releases list.
- [x] Retired map provider removed from current source.
- [x] Legacy APK compatibility filename removed from the build workflow.
- [x] Current offline map package is distributed separately as a GitHub Release asset.

## Before switching visibility
- [ ] Review historical Git commits, tags, branches, forks, and GitHub Actions logs/artifacts. Removing a file from the current branch does not remove it from history.
- [ ] Review commit author email addresses and any public-facing social links.
- [ ] Check the latest CI build and test Jazan map installation on a device.
- [ ] Confirm that every bundled image, icon, and other asset can be publicly redistributed.

The old MapTiler key has been revoked; the historical commits may still contain its text. If removing historical content is required, use GitHub's documented history-rewrite process or publish a fresh clean repository. Do not rewrite this repository's history without preserving backups and coordinating any existing clones.
