# Android release checklist

The Android application is published as a signed Android App Bundle (AAB). Do not commit the keystore, `supabase.properties`, or any service-role key.

## Local release build

1. Copy `mobile/supabase.properties.example` to `mobile/supabase.properties` and fill it with the public Supabase, Mapbox, and Google client configuration.
2. Set the release signing variables in the shell:

```text
ANDROID_KEYSTORE_PATH=<absolute path to the release .jks file>
ANDROID_KEYSTORE_PASSWORD=<keystore password>
ANDROID_KEY_ALIAS=<key alias>
ANDROID_KEY_PASSWORD=<key password>
ANDROID_VERSION_CODE=1
ANDROID_VERSION_NAME=0.1.0
```

3. Build the bundle from `mobile/`:

```text
./gradlew :app:bundleRelease
```

The build fails deliberately when release signing is not configured. The bundle is written to `mobile/app/build/outputs/bundle/release/app-release.aab`.

## GitHub Actions

Run `.github/workflows/android-release.yml` manually or push a `v*` tag. Configure these repository secrets first:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `SUPABASE_URL`
- `SUPABASE_PUBLISHABLE_KEY`
- `MAPBOX_ACCESS_TOKEN`
- `MAPBOX_DOWNLOADS_TOKEN`
- `GOOGLE_WEB_CLIENT_ID`

The workflow only produces a signed AAB artifact. Uploading to Google Play still requires the Play Console app, package registration, Data safety answers, content rating, screenshots, store listing, and a Play service-account secret owned by the publisher.

## Account deletion and privacy

The Android account menu includes an in-app account deletion action. The external deletion page is:

`https://travelplanner.muntim.ru/#/delete-account`

Deploy the web application and the `delete-account` Supabase Edge Function before submitting the Android app. The function must keep JWT verification enabled and must have its server-side `SUPABASE_SERVICE_ROLE_KEY` available only in Supabase.

Before submission, replace the placeholder legal/support information in the public privacy policy and confirm the Data safety declaration against the production data flows.

## Store submission gate

- A signed release AAB installs on a clean Android device.
- Sign-up, sign-in, Google sign-in, password change, sign-out, and account deletion work against production Supabase.
- Create/edit/delete flows persist after relaunch and preserve existing trip data.
- Mapbox, email invites, photos, localization, dark theme, system bars, and offline/error states have been checked on the target Android versions.
- Privacy policy URL, support contact, account deletion URL, screenshots, app icon, description, age/content rating, Data safety, and release notes are filled in in Play Console.
- The production Supabase security advisor has been reviewed; currently it reports that leaked-password protection is disabled, which should be enabled before release.
