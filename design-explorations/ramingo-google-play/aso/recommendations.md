# Release recommendations and upload guide

## Recommended package

Use the EN title and descriptions in `copy/en/`; select matching RU text and graphics for Russian. Keep the seven-image order. Screens explain product value before pet support and sharing, so a first-time visitor understands road-trip planning immediately.

Screenshot direction selected: **Ivory**. It provides a warm travel character, strong dark text and a neutral frame around the actual pale-violet UI. Violet header is more assertive but competes with the UI; Peach carries the icon palette but feels heavier across seven screens. This is a design assessment, not conversion evidence. Samples: `screenshots/drafts/`.

## Feature graphic concept review

Editorial scores, 1–10; not user-test results.

| Concept | Brand recognition | Professional | Legibility | Product fit | Visual quality | Reason |
|---|---:|---:|---:|---:|---:|---|
| Three-stop itinerary — selected | 9 | 9 | 8 | 9 | 9 | Ramingo-colored route art connects three stops inside one coastal scene; the headline explains the planning benefit, while screenshots below show the real interface |
| Explore | 8 | 8 | 9 | 7 | 8 | Extends the coastal palette and feels inviting; the planning function is less explicit |
| Minimal | 8 | 9 | 10 | 7 | 8 | Strong typographic hierarchy; less proof of what the app does |

## Technical checks

Requested screenshots: 1240×2208, RGB PNG with embedded sRGB. Ratio 1.7806 is within the general limit of 2:1, but not exactly 9:16. Supplemental `screenshots/optional-1080x1920/` exports meet exact 9:16 without stretching UI. Use that pack if prioritizing the recommended portrait ratio. Feature graphic: 1024×500, RGB PNG without alpha, embedded sRGB. Seven screenshots per language.

Verified against [Google Play preview asset guidance](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en) and [listing text guidance](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en-EN) on 19 October 2026. Title ≤30, short ≤80, full ≤4000. The script records exact counts. Recheck Console validation when uploading; no acceptance or conversion guarantee is implied.

The first map and walking maps retain the on-screen Mapbox logo and information control. No personal photos, third-party place photos, booking references, prices of stays, member identities or addresses of private individuals enter the kit. Catalog business addresses are public place data, not traveller addresses. Place ratings are native catalog content, not fabricated Ramingo reviews.

## Manual upload

1. Open the Ramingo app in Play Console → Grow users → Store presence → Main store listing (the exact label may differ by Console language).
2. Select the existing English locale. Paste title, short and full text from the three plain-text files; do not include candidate lists or analysis.
3. Keep the approved icon. Replace phone screenshots with 01–07 in order. Use one size pack consistently. Upload `feature-graphic/final/feature-graphic.png`.
4. Add matching Russian text and `screenshots/final/ru/` graphics plus `feature-graphic/final/ru/feature-graphic.png` to the Russian locale.
5. Use the localized alt text in `copy/*/image-alt-text.txt`. Check order, crops, preview and all validation notices. The illustrated coastal itinerary was created with ImageGen and contains no app UI. Answer accurately if Play Console asks about asset provenance; the previously approved app icon is unchanged.
6. Save a draft, review the publishing overview and submit only after the owner’s approval. The listing assets were uploaded in Play Console. On 19 October 2026, only the EN-US and RU feature graphics were replaced after correcting the crop margin, and those two changes were submitted for review. The approved icon and seven screenshots per language were not changed in this revision.

## Next languages

Prioritize German and Spanish because actual UI translations already exist; review them natively before promoting. Consider Italian and Czech only after product-localization review, as European road-trip hypotheses, not measured acquisition demand. Country/query conversion data should decide the final order. Avoid advertising a fully translated UI in a language that currently lacks one.

## Limitations

- Existing-trip catalog rather than newly saved pet stops: production data was preserved. Marketing says find/add, not already saved.
- No eighth album screenshot. The feature-graphic illustration was created with ImageGen rather than taken from a user trip or third-party listing.
- Captures are 720 px wide on the available device; crops are resampled to the requested export. A future higher-resolution native capture can improve fine UI text without changing the layout.
- Store acceptance, thumbnail legibility and actual conversion are different checks. Dimensions/profile/counts are automated; the exported boards are visually inspected. ASO hypotheses need real traffic.
- The EN-US and RU banner correction is awaiting Google review and storefront publication. No A/B experiment or measured results exist. No app code or icon changed in this revision.
