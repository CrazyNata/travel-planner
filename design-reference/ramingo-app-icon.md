# Ramingo app icon

Approved artwork: the coastal road without a sun. Keep the composition and flat colors unchanged when exporting it.

- Editable master: `ramingo-app-icon.svg`.
- Google Play listing: `ramingo-google-play-icon-512.png`, an opaque 512×512 PNG without baked corner rounding or shadows.
- Presentation preview: `../mobile/ramingo-app-icon-preview.png`.
- Android launcher: `../mobile/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`; API 33 adds the monochrome layer through its qualified resource.
- Shared in-app and splash mark: `../mobile/app/src/main/res/drawable/ic_ramingo_mark.xml`.

The adaptive foreground uses a 384-unit viewport at 108 dp. The approved 256-unit scene is translated by 64 units, preserving the central 72 dp composition; surrounding artwork extends into the parallax area. The background is warm peach. Do not add a rounded mask to adaptive source layers: Android supplies the launcher mask. The legacy shared mark retains its rounded tile.

Palette: `#FFB77B`, `#F58E76`, `#6C5CE7`, `#41236B`, `#FFF3DA`. The themed icon uses the road silhouette alone.

Changing the listing PNG updates the store artwork. Updating the launcher icon for installed production clients requires publishing a new Android build.
