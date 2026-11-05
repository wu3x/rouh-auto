# Car Launcher

Car Launcher is a landscape-first Android home screen for aftermarket car head units. It is designed for Android 9 (API 28) and newer, with a large-touch-target Compose UI suitable for dashboard use.

## Current features

- Home dashboard with status, navigation-preview, and media surfaces
- App library with search, list/grid views, pinning, and quick launch
- Multiple active media sessions with sticky source selection
- Swipeable media sources, artwork backgrounds, progress, and playback controls
- Expanded player with supported shuffle, repeat, and favorite controls
- Driving-oriented notification list
- Dark/light appearance, text sizing, startup-screen, and launcher settings
- Optional launch-at-boot behavior

## Build

Requirements:

- JDK 17
- Android SDK with API 36 installed

Build the public debug APK:

```bash
./gradlew assembleDebug
```

The APK is generated under `app/build/outputs/apk/debug/`. Build artifacts, signing keys, research APKs, private planning, and paid-edition work are intentionally excluded from this repository.

## Status

The app is under active development. The standard public build does not include privileged system-app embedding or paid navigation code. Hardware-specific work for YT5760D/AC8257 head units is tracked privately until it is ready for public release.

## Attribution

Car Launcher started from [OpenLauncher](https://github.com/vickoc911/openlauncher) and has since substantially diverged.

## License

Licensing is still under provenance review. No license is granted by this repository at this time.
