# Ramingo / Google Play listing kit

Prepared 19 October 2026. EN primary, RU localized. The original listing assets are in Play Console; the crop-safe EN-US and RU feature-graphic replacements were submitted for review on 19 October 2026 and are not published yet. App resources and production trip data were not changed.

## Start here

- `preview-en.png`, `preview-ru.png`: seven screenshots in order.
- `feature-graphic/final/feature-graphic.png`: selected crop-safe trip-planning feature graphic; Russian version in `final/ru/`. The latest center-crop previews and comparison are in `feature-graphic/crop-safe-v2/`.
- `release-package.zip`: curated English/Russian upload files and the existing approved 512×512 Play icon, ready for manual Console entry.
- `concepts-overview.png`: three feature concepts and three screenshot directions.
- `copy/en/` and `copy/ru/`: paste-ready text. Recommended title **Ramingo: Road Trip Planner**; short **Plan road trips and city walks. Keep stops and useful places together.**
- `aso/recommendations.md`: choice rationale, concept scores, limitations and manual upload instructions.

## Assets

`screenshots/final/01-road-trip.png` through `07-share.png` are 1240×2208 RGB/sRGB PNG. RU counterparts are in `screenshots/final/ru/`. Exact 9:16 supplementary 1080×1920 exports are in `screenshots/optional-1080x1920/en/` and `/ru/`. Feature concepts and finals are 1024×500 RGB/sRGB PNG. Do not upload contact sheets or draft directions as screenshots.

All app fragments are native captures. Layout decorations and headings are marketing composition, not app UI. No captured UI text was translated or fabricated. Only whole-rectangle crops, uniform resizing and rounded presentation masks were applied. The places screenshot shows the route map and two restaurant cards from the same city; the pet-services screenshot presents the actual catalogue without implying that distant catalogue listings are already saved to the route. On-screen map attribution remains visible. Personal data is excluded by crop. The feature graphic uses an ImageGen-created editorial illustration of the Italian coast, with one winding route linking three stops beside a separate brand panel and product headline; it contains no app UI.

## Rebuild

Python 3 + Pillow. From the repository: `python design-explorations/ramingo-google-play/build_listing.py`, then `python design-explorations/ramingo-google-play/build_copy.py`. Manrope fonts and the approved icon are referenced from existing repository assets. These scripts write only inside this kit. `requirements.txt` records the image dependency.

Recreate the curated Console package with `python design-explorations/ramingo-google-play/package_release.py`. It copies only the selected locale text, screenshots, feature graphics and the approved Play icon. It does not create adaptive icons or contact sheets in the upload package.

`screenshots/sources/ui/` contains only privacy-reviewed crops. `provenance.json` records raw capture filename, SHA-256 and crop rectangle; raw originals with private information remain outside this deliverable under local `tmp/ramingo-listing-capture/` and must not be published. `--prepare-crops` requires those local originals; normal rebuilds do not. `capture.ps1` can recapture a manually prepared device state; it is not an unattended data-entry script. Do not run its tap/section actions without confirming the current device UI.

## Checks and scope

`validation-images.json`: dimensions, mode, ICC and file sizes. `validation-copy.json`: exact character counts. `aso/positioning.md`: code, UI, localization, baseline and capture audit. `aso/keyword-research.md`: qualitative competitor/intent research, no fabricated volumes. `aso/ab-testing.md`: three proposed controlled experiments. `copy/marketing-posts.md`: EN/RU launch and social drafts, not sent.

The Play Console submission changed only the EN-US and RU feature graphics. The approved app icon and seven screenshots per language remain as before. Pet graphics show the real catalog because adding saved stops would change production. An album frame is deliberately omitted. No app logic, Android resources, schema, trip records or invitations changed. No conversion test has run. Google review and storefront publication are pending. Read the limitations in recommendations before publishing.
