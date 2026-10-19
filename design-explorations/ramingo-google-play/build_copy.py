"""Generate editable EN/RU copy, analysis and character-count checks. No network."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parent
def put(p,s):
 p=ROOT/p; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s.strip()+'\n',encoding='utf-8')
titles=['Ramingo: Road Trip Planner','Ramingo: Travel Planner','Ramingo: Trip Organizer','Ramingo: Routes and City Walks','Ramingo: Plan Your Trips']
shorts=[
 'Plan road trips and city walks. Keep stops and useful places together.',
 'Keep your routes, stays, restaurants and city walks in one trip plan',
 'Organize road trips, save useful places and share the plan with your companions',
 'Plan the drive, explore on foot and keep your travel stops together',
 'Build your travel itinerary with routes, stays, restaurants and useful pet stops']
en='''Plan the drive. Explore the city. Keep the details together.

Ramingo is a road trip planner and travel organizer for people who like to shape their own journeys. Bring your route, city walks, stops and useful places into one trip plan, then invite your travel companions to view it.

PLAN YOUR ROAD TRIP
Create a trip and organize the places you want to visit along the way. Plan the journey between cities and keep your next destination in sight, whether you are preparing a weekend away or a longer road trip.

EXPLORE CITIES ON FOOT
Build walking routes for your city days. Bring sights and places you want to visit into a walking plan, so the drive and the time you spend exploring fit into the same journey.

KEEP YOUR STOPS AND PLACES TOGETHER
Add interesting stops, save places to stay and keep restaurants you want to try with the rest of your travel itinerary. Organize the details in the trip they belong to, rather than keeping the plan in separate lists.

MAKE ROOM FOR YOUR PET
Travelling with a pet means thinking about their needs too. Add veterinary clinics and pet shops to your trip, so useful places are part of your preparations. They sit alongside your other travel plans without taking over the journey.

ADD YOUR TRAVEL PHOTOS
Keep photos in your trip album and bring memories back to the journey they came from.

LET YOUR COMPANIONS SEE THE PLAN
Invite other people by email and give them viewer access to your trip. Let family, friends or travel companions see the prepared plan and know what is coming next.

From the first drive to the last city walk, give your next journey a place to come together. Start planning with Ramingo.'''
ru='''Продумайте дорогу, прогулки и всё, что пригодится в пути.

Ramingo — планировщик автопутешествий для тех, кто любит организовывать поездки самостоятельно. Соберите маршрут, остановки, городские прогулки и полезные места в одном приложении, а затем покажите готовый план попутчикам.

ПЛАНИРУЙТЕ АВТОПУТЕШЕСТВИЯ
Создайте поездку и добавьте места, которые хотите посетить. Продумайте переезды между городами и следующие остановки — для коротких выходных или большого путешествия на автомобиле.

ИССЛЕДУЙТЕ ГОРОДА ПЕШКОМ
Составляйте маршруты прогулок по достопримечательностям и интересным местам. Автомобильная часть путешествия и прогулки по городам останутся в одном плане.

СОХРАНЯЙТЕ ВАЖНЫЕ МЕСТА
Добавляйте остановки, жильё и рестораны, которые хочется посетить. Держите детали рядом с маршрутом, чтобы не собирать план заново из разрозненных списков.

ПОДГОТОВЬТЕСЬ К ПОЕЗДКЕ С ПИТОМЦЕМ
Добавьте ветеринарные клиники и зоомагазины, которые могут пригодиться в дороге. Потребности питомца станут частью общего плана путешествия.

СОБИРАЙТЕ ФОТОГРАФИИ
Добавляйте снимки в альбом поездки и возвращайтесь к воспоминаниям о путешествии.

ПОКАЖИТЕ ПЛАН БЛИЗКИМ
Приглашайте других людей по электронной почте и предоставляйте им доступ на просмотр. Семья, друзья и попутчики смогут увидеть подготовленный план и узнать, что ждёт дальше.

От первого переезда до последней прогулки — соберите следующее путешествие в Ramingo.'''
selected={
 'en':{'app-title':titles[0],'short-description':shorts[0],'full-description':en,
       'alternative-opening':'A great road trip is more than the drive. Plan the cities, walks and useful stops that make it yours, and keep them together in Ramingo.'},
 'ru':{'app-title':'Ramingo: планировщик поездок','short-description':'Планируйте автопутешествия и прогулки, сохраняйте остановки и полезные места','full-description':ru,
       'alternative-opening':'Путешествие начинается с идеи, а складывается из деталей. Соберите переезды, прогулки и нужные места в Ramingo — от первой остановки до возвращения домой.'}}
heads={
 'en':['Plan your next road trip','Drive there. Explore on foot.','Every stop. One clear plan.','Make room for great places.','Travel better prepared with pets.','A place for every detail.','Let them see the whole plan.'],
 'ru':['Спланируйте своё автопутешествие','Между городами — на авто. По городу — пешком.','Все остановки. Один понятный план.','Сохраните места, куда хочется попасть.','Продумайте поездку с питомцем.','Каждая деталь на своём месте.','Покажите близким весь план поездки.']}
counts={}
for lang,items in selected.items():
 for name,txt in items.items():
  put(f'copy/{lang}/{name}.txt',txt)
  if name in ['app-title','short-description','full-description']:
   limit={'app-title':30,'short-description':80,'full-description':4000}[name]
   assert len(txt)<=limit,(lang,name,len(txt)); counts[f'{lang}/{name}']={'characters':len(txt),'limit':limit}
 put(f'copy/{lang}/screenshot-headlines.txt','\n'.join(f'{i+1:02d}. {t}' for i,t in enumerate(heads[lang])))
put('copy/en/title-candidates.md','# App title candidates\n\n'+ '\n'.join(f'{i+1}. {s} — {len(s)}/30'+(' — SELECTED' if i==0 else '') for i,s in enumerate(titles))+ '\n\nAll retain the brand. The selected title leads with a concrete planning intent; broader alternatives suit later positioning tests. No keyword-volume evidence is available.')
put('copy/en/short-description-candidates.md','# Short description candidates\n\n'+'\n'.join(f'{i+1}. {s} — {len(s)}/80'+(' — SELECTED' if i==0 else '') for i,s in enumerate(shorts))+'\n\nSelected: pairs the primary road-trip intent with city walks and keeping useful stops together. Two direct sentences scan naturally. No install CTA or ranking claim. Candidate 4 is a shorter benefit-led test alternative.')
for s in titles: assert len(s)<=30
for s in shorts: assert len(s)<=80
put('validation-copy.json',json.dumps(counts,ensure_ascii=False,indent=2))

put('copy/marketing-posts.md','''# Marketing copy / EN + RU

Drafts for manual posting. No messages have been sent. Check each community’s promotion rules before use. Public destination: https://play.google.com/store/apps/details?id=com.odyssey.travelplanner

## Launch / EN
Meet Ramingo: a place to bring your next road trip together. Plan the drive, map out city walks, keep useful stops, stays and restaurants with your trip, and invite your companions to view the plan. Travelling with a pet? Include vets and pet shops too. Explore Ramingo on Google Play.

## Анонс / RU
Знакомьтесь, Ramingo — приложение, в котором можно собрать план автопутешествия. Продумайте переезды и прогулки по городам, сохраните жильё, рестораны и полезные остановки, а затем покажите план попутчикам. Едете с питомцем? Добавьте ветеринаров и зоомагазины. Ramingo доступен в Google Play.

## Reddit / EN
Title: I’m building a trip planner that keeps the drive and the city walks together

Hi everyone — I’m the developer of Ramingo, an Android travel planner. The focus is preparing a journey: routes between cities, walking routes once you arrive, and the stops, stays and restaurants you want to keep with the trip. There is also space for useful pet stops, and you can invite other people to view your plan.

I’d like feedback from people who plan their own road trips. Which part of preparing a multi-city journey is hardest to keep organized? If this fits the group’s rules, here’s the app: https://play.google.com/store/apps/details?id=com.odyssey.travelplanner

Note for publisher: the developer voice assumes the author is the developer. No invented biography, development duration, download totals or user stories are included.

## Reddit / RU
Заголовок: Делаю планировщик, который объединяет автопоездку и прогулки по городам

Привет! Я разрабатываю Ramingo — Android-приложение для подготовки путешествий. В нём можно собрать переезды между городами, пешие маршруты, остановки, жильё и рестораны. В поездку также можно добавить полезные места для питомца и пригласить других людей на просмотр плана.

Хочу услышать мнение тех, кто самостоятельно планирует автопутешествия: какие детали сложнее всего держать в порядке? Если правила сообщества разрешают, вот приложение: https://play.google.com/store/apps/details?id=com.odyssey.travelplanner

## Facebook travel group / EN
Planning a road trip with a few city days along the way? Ramingo keeps driving routes, walking plans, stays, restaurants and useful stops in the same trip. You can also invite your companions to view it. I’m sharing the app as its developer and would welcome feedback from independent travellers: what would make your next trip easier to organize?

## Facebook / RU
Планируете автопутешествие с прогулками по городам? В Ramingo можно собрать переезды, пешие маршруты, жильё, рестораны и полезные остановки в одном плане, а затем открыть его для просмотра попутчикам. Я разработчик приложения и буду рад обратной связи: каких возможностей вам не хватает при подготовке поездки?

## Instagram / EN
The drive. The city walks. That little place you want to stop at.
Bring the journey together with Ramingo — a planner for road trips and days spent exploring on foot.
Find Ramingo on Google Play via the profile link.
#Ramingo #RoadTripPlanning #CityWalks

## Instagram / RU
Дорога. Прогулки по городам. Места, ради которых хочется остановиться.
Соберите своё путешествие в Ramingo: переезды, пешие маршруты и полезные детали поездки.
Приложение в Google Play — по ссылке в профиле.
#Ramingo #Автопутешествия #ПланированиеПоездки

## Product Hunt / EN
Name: Ramingo
Tagline: Road trips, city walks and useful stops in one plan
Description: Plan driving routes and city walks, save stays and restaurants, add useful pet stops and keep travel photos with your trip. Invite companions to view the plan, so everyone can see what is coming next.
Maker comment: Hi Product Hunt! I’m the developer of Ramingo. It brings the practical parts of preparing a road trip into one place, including the walking days between drives. I’d love to hear how you organize multi-city trips and which details you tend to lose track of.

## Product Hunt / RU adaptation
Слоган: Автопутешествия, прогулки и полезные места в одном плане
Описание: Планируйте переезды и пешие маршруты, сохраняйте жильё и рестораны, добавляйте полезные места для питомца и фотографии поездки. Приглашайте попутчиков на просмотр, чтобы все могли увидеть готовый план.

## CTA options / EN
1. Plan your next trip with Ramingo
2. Explore Ramingo on Google Play
3. Bring your travel plans together

## CTA / RU
1. Спланируйте следующую поездку с Ramingo
2. Посмотрите Ramingo в Google Play
3. Соберите путешествие в одном приложении

These CTAs are for posts and campaign links, not the Play short description. Profile-link wording assumes the publisher adds the actual link before posting.
''')

put('aso/keyword-research.md','''# Qualitative keyword research — 19 October 2026

No Play search-volume, paid ASO database, keyword difficulty or ranking measurements were available. Priorities below are editorial judgments about product fit, not measured demand. English spellings target an international audience; validate country-level acquisition queries in Console after release.

| Intent | Phrases | Priority / rationale | Placement |
|---|---|---|---|
| Prepare a driving holiday | road trip planner; plan a road trip; road trip organizer | Primary: specific to the core scenario | Title; opening; driving section |
| Organize a journey | trip planner; travel planner; travel organizer; plan a trip | High relevance, broad intent; actual competition is unmeasured | Natural full description |
| Build an itinerary | travel itinerary; trip itinerary planner; multi-city trip planner | Strong task match; last phrase is inferred from multi-city routes | Full description; screenshots |
| Explore a city | walking route planner; city walk planner; city sightseeing route | Secondary; qualified city-walk context prevents an outdoor navigation promise | Short description; screenshot 2 |
| Keep places | save travel stops; organize travel plans; road trip stops | Relevant benefit language; query frequency unknown | Screenshots 3–4; body |
| Prepare with a pet | travel with pets; pet travel planning; vets along a route | Differentiator, narrower audience; no emergency-service guarantee | Screenshot 5; body |
| Show the plan | share trip itinerary; share travel plans | Useful secondary intent; frame as invited viewing | Screenshot 7; body |
| Russian planning | планировщик поездок; планировщик автопутешествий; маршрут прогулки; план путешествия | Localized equivalents, not proven rankings | RU title/body |

Exclude GPS navigator, live traffic, offline maps, hotel booking, AI trip generator, automatic route optimization, flight tracker and pet-friendly hotel search: not verified promises in this brief. Google Play has no separate keyword field here; do not paste this list into the description.

## Competitor observations

[Wanderlog on Google Play](https://play.google.com/store/apps/details?id=com.wanderlog.android&hl=en) presents a broad trip-planning product, including map-based itineraries and collaboration. Ramingo should demonstrate its own workflow instead of borrowing reservation-import or optimization claims.

[Roadtrippers on Google Play](https://play.google.com/store/apps/details?id=com.roadtrippers&hl=en) leads with the road-trip category and a road-travel ecosystem. Ramingo can pair that clear category signal with city walking days and useful pet stops. These are positioning observations, not a claim that competitors lack any feature.

Visual direction: large task-led headlines, one main benefit per frame, authentic UI, few brand colors. Avoid tiny feature collages, generic travel-stock backgrounds and awards/rating badges. These are design judgments; no conversion advantage has been measured. Competitor text, logos and screenshots are not reused in the kit.
''')
put('aso/positioning.md','''# Positioning and product evidence

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
''')

put('aso/ab-testing.md','''# Three controlled tests

These are proposals; no experiments have run and no uplift is claimed. Test one variable at a time after a stable baseline. Use one challenger plus control to avoid fragmenting limited traffic.

| Test | Control / challenger | Hypothesis | Metric |
|---|---|---|---|
| A / First screenshot | Current exported first frame / same frame with alternative “The drive. The walks. One trip plan.” heading | Naming both travel modes may improve relevance for mixed road/city journeys | Console target “Unique user install clicks”; compare per-exposure experiment performance and confidence interval |
| B / Short description | Selected short / candidate 4 in copy/en/short-description-candidates.md | Shorter benefit language may be easier to understand at a glance | Same install target in localized EN experiment; diagnostic store-listing acquisition rate by comparable source |
| C / Feature graphic | Selected three-stop itinerary illustration / Explore concept, all other assets fixed | A continuous route connecting three visible stops beside explicit planning copy may improve product understanding; abstract travel art may attract more exploration-led visitors | Same install target; interpret only where the asset receives exposure |

Prepare A as a headline-only variant before launching; it is a proposed experiment, not an additional final frame. B and C challenger content already exists. Do not compare A with a different whole screenshot sequence: that would test several variables.

Current official Console guidance supports default graphics experiments and localized text/graphics experiments. It currently names install, open and pre-registration click targets; choose the install target for acquisition, not open clicks. Use the setup calculator for sample size, duration, detectable effect and confidence. Source: [Google Play experiment instructions](https://support.google.com/googleplay/android-developer/answer/12053285?hl=en), checked 19 October 2026.

Record baseline dates, listing language, countries, traffic sources, visitors and acquisitions. Hold acquisition campaigns and app-release messaging stable where possible. Check the same weekday mix; wait for the planned evidence rather than stopping when a variant briefly leads. More data needed is inconclusive, not a draw or a loss.

With low traffic, first run moderated comprehension checks on the first three frames, then one adequately powered test. A scheduled 7- or 14-day window is not proof of enough data. Do not infer causality from a simultaneous title, screenshot and description rollout. Title changes can be evaluated sequentially with explicit confounders; they are not one of the native text experiments proposed here.

Track listing conversion and search-query relevance separately. Watch onboarding completion and trip creation only if existing, consent-appropriate analytics already measure them; this task adds no tracking. Check downstream retention as a guardrail if available, without claiming listing experiments directly establish retention gains.
''')
put('aso/recommendations.md','''# Release recommendations and upload guide

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
''')

alts={
 'en':['Map of a 13-leg, 11-city European trip and the next driving leg.','A driving leg and a numbered city walking route in Verona.','Four saved driving legs between Italian cities and San Marino.','A route map and two saved restaurant cards in Chioggia, Italy.','Vet and pet-shop catalog entries with add controls.','Trip sections for routes, sights, restaurants, lodging, pets, budget, members and photos.','Empty invitation form with Viewer access selected.'],
 'ru':['Карта путешествия по Европе: 13 переездов, 11 городов и следующий участок пути.','Автомобильный переезд и пеший маршрут по Вероне.','Четыре сохранённых переезда между городами Италии и Сан-Марино.','Карта маршрута и две карточки ресторанов в Кьодже, Италия.','Записи ветеринара и зоомагазина из каталога с кнопками добавления.','Разделы поездки: маршрут, достопримечательности, рестораны, жильё, питомцы, бюджет, участники и фото.','Пустая форма приглашения с выбранным доступом на просмотр.']}
for lang,arr in alts.items():
 assert all(len(s)<=140 for s in arr)
 put(f'copy/{lang}/image-alt-text.txt','\n'.join(f'{i+1:02d}. {s}' for i,s in enumerate(arr))+'\nFeature graphic: '+('Illustrated Italian coast with one winding route connecting three stops, beside Ramingo and “Every stop. One clear plan.”' if lang=='en' else 'Иллюстрированное побережье Италии: дорога связывает три остановки рядом с Ramingo и заголовком «Все остановки — в одном плане».'))
put('README.md','''# Ramingo / Google Play listing kit

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
''')
put('requirements.txt','Pillow>=10,<13')
print(json.dumps(counts,ensure_ascii=False,indent=2))
