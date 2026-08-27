# ViroGallery

A Kotlin Android video-only gallery/player built from the supplied Android APK builder template.

## Included
- Video-only library; no Music tab/library feature.
- Three-column video grid by default, with a 2/3-column toggle.
- MediaStore video scanning and a configurable Storage Access Framework root folder.
- Folder navigation restricted to the selected root and its subfolders.
- Video thumbnails, filename and duration.
- Portrait/landscape player UI based on the supplied layouts.
- Player overlay menu, subtitle sheet, playback queue, speed/audio actions, share/delete and repeat/shuffle controls.
- Dark UI matching the supplied screenshots.
- Settings screen for root folder and grid behavior.
- ViroGallery application namespace/applicationId.
- Supplied Viro icon installed as density-specific launcher assets: mdpi 48px, hdpi 72px, xhdpi 96px, xxhdpi 144px, xxxhdpi 192px.

## Build
GitHub Actions uses Gradle 8.0.2 directly through `gradle/actions/setup-gradle@v4`, so the template's placeholder/broken wrapper JAR is not required.

```bash
gradle --no-daemon clean assembleDebug bundleRelease
```
