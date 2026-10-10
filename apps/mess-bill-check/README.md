<!-- SPDX-License-Identifier: MIT -->

# Mess Bill Check

Bootstrapped from AppFlow @ 296319233a85d226539222abea401b829c065dd7 on 2026-10-10

**Status: pre-release skeleton.** No published build exists, nothing is on Google Play, and the
feature list below is the approved v1 plan, not a description of a shipping app.

Offline mess-account ledger for hostel and PG students in English, Hindi and Bengali. It keeps a
student's own record of meals, leave days and deposits and compares the bill the app computes
against the bill the manager states, so the difference is visible before any money moves.

## What it does

- Planned v1 (approved 2026-10-11, plan in `research/bprime-v1-plan-2026-10-11.md`): daily meal log
  with half-days and leave days, charge lines with a configurable split rule, deposits and payments,
  a computed-vs-stated bill delta, immutable month-close snapshot, free CSV and PDF export, and JSON
  backup/restore.
- Offline by construction: v1 requests no `INTERNET` permission and ships no SDK that contacts a
  server. A unit test fails the build if a future change adds either.
- No account, no backend, no sync. Cross-device movement is by user-owned backup files only.

## Screenshots

None yet. Screenshots will come from a released build only.

## Privacy and ads

- Privacy policy: not published yet; required before the first release build. It will state the
  no-collection design in plain language.
- Data Safety: no published app yet; the declaration will reflect the exact shipping build and all
  included SDKs.
- Ads: none planned for v1.
- Ad-free purchase: not applicable while there are no ads; a one-time support purchase is under
  owner review.
- Accounts and backend: none.

## Build and test

Requirements: JDK 17, Android SDK, and the checked-in Gradle wrapper.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Not yet runnable: this repository currently contains the frozen playbook snapshot, the license, and
the CI workflow only. Release artifacts are built by GitHub Actions. Signing credentials are stored
in protected Actions secrets, not in this repository. See `.github/workflows/ci-build.yml` for the
actual workflow.

## Try it before a release exists

The debug APK is built by CI on every push and published as the `mess-bill-check-debug-apk`
artifact of the `Android CI` run — download it from the run page, unzip, install
(`adb install app-debug.apk`, or copy the APK to the phone and open it). Debug builds are signed
with Android's shared debug key, so this is not a distribution channel; it exists so a person can
hold the app before anything is published.

The APK declares no permissions at all — CI verifies that on the built artifact, not just in the
source, because "it never phoned home" should be a property of what you install.

## Permissions and data

No sensitive permissions are planned. All app data stays in the app's private storage on the device;
export and backup write files only when the user asks for them.

## License and credits

- **App license:** MIT (see `LICENSE`), copyright (c) 2026 Pabitra Chakrabortty.
- **Third-party code and assets:** none yet. Any library added later gets a version-matched
  attribution entry before release.

## Store listing

- Google Play: not published.
- Support contact: not published yet; a reachable address is required before any Play submission.
- Target API: 36 as required for new apps per the policy checkpoint in AppFlow `RULES.md`
  (checked 2026-10-10); re-check the live target API policy before every release.
