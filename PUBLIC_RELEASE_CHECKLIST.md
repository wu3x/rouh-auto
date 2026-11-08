# ROUH Auto — public release safety checklist

**Status: NOT CLEARED FOR PUBLIC VISIBILITY.** Keep the existing repository private until all blocking items below are resolved.

## Blocking items

- [ ] **Rotate/revoke the historical MapTiler API key.** A real key value existed in an earlier version of `app/src/main/java/org/wpfy/carlauncher/feature/map/MapStyle.kt` and was removed by commit `5a65124`. Removing it from HEAD does **not** remove it from Git history. Revoke it in the MapTiler account, check usage/billing, and ensure no other historical credentials remain.
- [ ] **Remove the private font distribution from releases.** Release `font-assets` currently includes `Thmanyah-Font-Family.zip`. Changing the repository to public would expose that archive. Remove the release asset before publishing unless you have explicit rights to redistribute the font publicly. Do not copy licensed fonts into the repository.
- [ ] **Review and sanitize full Git history**, including old commits, tags, branches, PR references and Actions logs/artifacts. Current-source scanning alone is not sufficient. If rewriting history, follow GitHub's official `git-filter-repo` guidance and coordinate all clones. Prefer a new clean public repository if preserving the original private history is important.
- [ ] **Review repository metadata and personal information**, including commit author emails, README social links, website analytics and old screenshots, before making them public.
- [ ] **Confirm release build and map download** on an Android emulator and the actual head unit. Validate map checksum, GPS, offline behavior, and no unexpected network dependency.

## Current-source precautions

- Release signing credentials are read from GitHub Actions secrets; no keystore is tracked in the current tree.
- MapTiler API key in current `MapStyle.kt` is empty.
- `.gitignore` excludes local credential files and offline map packages.
- Jazan's automatic downloader checks the expected SHA-256 before installation.

This checklist is not a certification that the repository's historical content is safe to publish.
