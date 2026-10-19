# Positioning and product evidence

**Primary:** independent road-trip travellers who want the drive, city walks and useful places together in a prepared plan.

Promise: “Bring the journey together.” The product helps prepare a trip; walking days are part of the journey and pet needs are a useful supporting benefit.

| Audience | Need | Priority and proof |
|---|---|---|
| A / Road-trip travellers | Routes and stops between cities | Primary; real driving-leg list and trip map |
| B / Independent travellers | Keep practical details together | Primary context; stays, restaurants, trip sections |
| C / City explorers | Prepare walks after arriving | Secondary entry point; real numbered walking map |
| D / Pet owners | Include useful pet stops | Differentiator; vets/pet-shop catalog and add controls |
| E / Couples, families, groups | Show everyone a prepared plan | Supporting value; invitation form with Viewer selected |

## Why this title and short description

**Ramingo: Road Trip Planner** makes the brand and use case explicit in a short title. It is a recommendation based on fit, not search-volume evidence.

**Plan road trips and city walks. Keep stops and useful places together.** pairs the primary intent with an immediately clear organization benefit. The title carries the category; the short description adds walking and saved stops.

## Screen narrative

1. Whole-trip map + next road leg: understand the category immediately.
2. Driving leg + walking map: the journey continues after parking.
3. Four real route legs: a readable sequence of saved stops.
4. Restaurant map, restaurant entry and lodging name: places belong with the trip.
5. Pet catalog tabs and actual vet/shop entries: practical extra preparation, not the main identity.
6. Real trip-section drawer: organizing structure, visibly distinct from maps and stop lists.
7. Blank invitation form with Viewer selected: share the prepared plan without implying that viewing is editing.

No eighth photo frame: approved trips contain personal material and external place imagery with incomplete provenance. Seven stronger frames are preferable to an unverified album claim in graphics. Photo support is confirmed and described in copy.

## Project audit

Android source: `mobile/app/src/main/java/com/odyssey/travelplanner/ui/OdysseyApp.kt`; repository `TripRepository.kt`. Inspect symbols rather than relying on line numbers after code changes.

- Brand UI palette: primary #6C5CE7, light #F4F4F7, white surfaces, text #141419; dark primary #A79BFF. Marketing extends the approved icon with warm ivory/coral. Manrope fonts are the existing Android assets.
- Existing approved icon: `design-reference/ramingo-app-icon.svg`, Play PNG `design-reference/ramingo-google-play-icon-512.png`. Neither was changed for this task.
- Driving legs and external Maps opening; sights walking routes via Mapbox walking profile. No turn-by-turn claim.
- Stays and restaurants: save/manage trip entries. No in-app booking promise, even where external booking links exist.
- Pets: catalog, manual place entry and adding places. Catalog entries were captured; none added to production for this task.
- MembersContent and repository role handling distinguish owner/editor/reader. The kit advertises the narrower verified viewer scenario. Blank form only; no invitations sent.
- Photos: trip album supported; no promotional capture of personal photos.
- UI locales: RU, EN, ES, DE. Some user-entered text and proper names do not translate; final EN crops exclude mixed-language descriptions.

Capture environment: real Android device, 720×1640, installed debug variant 0.2.77, light theme, approved existing “Italy with family” trip. This is current native UI, not a browser mockup. No test-only UI, production edits, source changes or demo data insertion. User permitted “Italy with family” and “Christmas Italy”; only the former was needed. App language restored to Russian after capture.

## Baseline

Existing English Console listing observed in this conversation: title “Ramingo”; short description “Plan trips, routes, stays, sights, meals, budgets, and photos together.” Historical draft: `docs/google-play-aso-draft.md`. Public listing fetch was unavailable; the live Russian baseline was not re-verified. New text favors a clearer road-trip entry point and explicit viewer access. Existing icon submission from the earlier task is separate; this kit has not been uploaded.
