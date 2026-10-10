<!-- SPDX-License-Identifier: MIT -->

# [YOUR_APP_NAME]

Bootstrapped from AppFlow @ <sha> on <date>

[One-sentence, factual description of the app and who it helps.]

## What it does

- [Verified feature 1]
- [Verified feature 2]
- [Verified offline or network behavior]

## Screenshots

[Add current screenshots from the released app. Do not show features or claims
that are not present in the current build.]

## Privacy and ads

- Privacy policy: [YOUR_PUBLIC_PRIVACY_POLICY_URL]
- Data Safety: see the Google Play listing; it reflects this exact build and
  all included SDKs.
- Ads: [state the actual formats and placements, or “No ads” if true].
- Ad-free purchase: [state the actual offer and restore method, or “Not offered”].
- Accounts and backend: [state accurately; do not assume “none” if an SDK or
  service provides one].

## Build and test

Requirements: JDK 17, Android SDK, and the checked-in Gradle wrapper.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Release artifacts are built by GitHub Actions. Signing credentials are stored
in protected Actions secrets, not in this repository. See
`.github/workflows/ci-build.yml` for the actual workflow.

## Permissions and data

[Explain each sensitive permission and its user-facing purpose. Link to the
privacy policy and describe the offline data path. Remove this section only if
it would make the app less transparent.]

## License and credits

- **App license:** [MIT or GPL-3.0; replace this placeholder].
- **Third-party code and assets:** [link to shipped notices and attribution].

## Store listing

- Google Play: [YOUR_PUBLIC_PLAY_LISTING_URL]
- Support contact: [YOUR_PUBLIC_SUPPORT_EMAIL]
- Target API: [CURRENT_REQUIRED_API_LEVEL, re-check before release]

Do not leave bracketed placeholders in a public release. Never add signing
keys, service-account JSON, account IDs, private tester data, or real AdMob IDs.
