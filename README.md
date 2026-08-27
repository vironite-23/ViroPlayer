# ViroPlayer

ViroPlayer is an Android video gallery/player foundation designed around two complementary
storage paths:

1. **MediaStore** for the normal device video library.
2. **Storage Access Framework (SAF)** for user-selected directory trees, including
   dot-prefixed/hidden folders and video files.

The UI is intentionally small. It is a foundation for adding the gallery, menu,
navigation, sorting, filtering, and advanced player features later.

## Important hidden-file behavior

Android's MediaStore is an optimized media index and is not a general-purpose
recursive filesystem index. For a gallery that must deliberately include hidden
directories/files, this project adds an `ACTION_OPEN_DOCUMENT_TREE` flow.

When the user grants a folder:

- the URI permission is persisted;
- the scanner recursively walks all child documents;
- directories beginning with `.` are **not** filtered out;
- files beginning with `.` are **not** filtered out;
- video MIME types and common video extensions are recognized;
- SAF results are merged with MediaStore results and de-duplicated by URI.

This means the user explicitly granting the parent directory is the key mechanism
for hidden content.

## Current architecture

```text
app/
  data/
    FolderStore.kt
    SafVideoScanner.kt
    VideoItem.kt
    VideoRepository.kt
  player/
    PlayerActivity.kt
  ui/
    AppTheme.kt
    VideoThumbnail.kt
  MainActivity.kt
  ViroGalleryApp.kt
```

### Gallery

`MainActivity` currently provides:

- MediaStore video permission request
- Add-folder button using Android's directory picker
- persistent SAF folder access
- recursive video scan
- simple adaptive grid
- hidden-content indicator
- thumbnail extraction through `MediaMetadataRetriever`
- opening a selected video in the player

### Player

`PlayerActivity` currently uses Jetpack Media3 ExoPlayer and a `PlayerView`.
It is deliberately kept close to the default Media3 player so it can later be
replaced or wrapped with the requested custom player UI.

## Build environment

The project is configured for the current Android toolchain used by the August
2026 Android documentation:

- Android Gradle Plugin 9.3.0
- Gradle 9.5
- Kotlin 2.3.21
- Compose BOM 2026.08.00
- Media3 1.11.0
- compileSdk 37
- minSdk 26

Use JDK 17.

The Gradle wrapper JAR is not included in this initial source-only repository.
Generate/update it with Android Studio or a local Gradle 9.5 installation.

## Next expansion points

The code intentionally leaves these areas open:

- gallery tabs and navigation
- folder browser UI
- grid/list/detailed view modes
- sorting and grouping
- search
- favorites
- playlists
- watched/progress state
- subtitle selection
- audio track selection
- playback speed
- gestures
- orientation/fullscreen policy
- picture-in-picture
- resume position
- external player support
- SMB/WebDAV/network sources
- per-folder scan settings
- scan exclusions
- hidden-content toggle
- background indexing and incremental updates
- thumbnail cache
- database-backed media index
- Android TV/tablet layouts

## Integrated gallery UI
- Main Video page uses the supplied dark gallery style with search, sort, view mode, folder picker, hidden-video toggle and settings navigation.
- Folder page uses a 3-column video grid by default, date badges, duration labels, selection highlight and long-press selection.
- Folder page supports Grid/List modes and sorting by newest, oldest, name and duration.
- Settings contains library, playback, display and UI Colors sections. Music is intentionally not a separate feature/tab.
- UI Colors persist in `viro_settings`, including background, card/panel, selected/highlight, accent and bottom navigation bar colors.

## Build

The repository includes a GitHub Actions workflow at `.github/workflows/android-build.yml`.
It uses JDK 21 and Gradle 9.3, builds the debug APK and release AAB, and publishes both as workflow artifacts.

For a local build, install JDK 21 and Gradle 9.3, then run:

```bash
gradle clean assembleDebug bundleRelease
```

The generated app icon is `app/src/main/res/drawable-nodpi/virogallery_icon.png` and is registered as both the launcher icon and round icon.
