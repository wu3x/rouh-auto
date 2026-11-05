# Car Launcher Next

Development repository for a custom Android car head-unit launcher (Android 13, 1280×720, 4 GB RAM).

## Project goals

- Keep the media player on the left and the map on the right.
- Preserve the current launcher/home and media playback behavior.
- Improve the map with a premium dark 2D style.
- Keep GPS location tracking independent of internet connectivity.
- Support downloading/caching map regions for offline display.
- Remove trip logging and other unnecessary features.
- Review Android 13 compatibility, permissions, stability and memory usage.

## One-time source import

This repository was initialized with a GitHub Actions workflow. Upload your **existing working project ZIP** to the root of this repository, named `car-launcher-modified.zip`, using **Add file → Upload files → Commit changes**.

On push, the workflow automatically:
1. Extracts `car-launcher-main/` into normal source files at the repository root.
2. Deletes the uploaded ZIP and commits the imported files.
3. Attempts to build a debug APK and uploads it under **Actions → Artifacts**.

The source import is a one-time step; after that, updates edit individual project files.

## Build locally

```bash
bash gradlew assembleDebug
```

Debug APK path: `app/build/outputs/apk/debug/app-debug.apk`.

> This is a development build. The APK and new maps must be tested on the physical Android 13 head unit before replacing the stable launcher. The old repository remains untouched.
