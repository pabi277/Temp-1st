# Per-app maintenance cadence

A release is not finished when the first AAB is uploaded. Give every app a
small, repeatable maintenance budget and keep the work in its own repository.

## Each quarter

- Check Play Console notices, reviews, crashes, store-policy status, and
  current target API deadlines.
- Re-test the core offline flow, current consent behavior, ads, ad-free
  entitlement, restore, and any important links.
- Review Data Safety, privacy policy, permissions, audience/rating answers, and
  SDK inventory against the exact shipped build.
- Reply to reviews with verified facts; record recurring complaints in
  `DECISIONS` or the next-app spec.

## Once a year — target API and dependency touch-up

- Re-read the live [Google Play target API policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
  and set a calendar reminder before the deadline. As checked on 2026-10-10,
  mobile new apps and updates require API 36 or higher; this is not a permanent
  value.
- Start a target API / Android Gradle Plugin / Gradle / JDK review early enough
  to test behavior changes before the applicable Play deadline.
- Update libraries deliberately. Check SDK support status and security
  advisories; remove abandoned or unnecessary dependencies.
- Run unit tests, a minified release build where enabled, and real-device
  checks for offline, permissions, consent, ads, and billing.
- Rebuild the signed AAB, inspect the artifact, and update store disclosures
  only when the shipped behavior changes.
- Verify the signing-key recovery plan and GitHub secret access without
  committing keys or credentials.
- Update the app's short `DECISIONS` log with date, evidence, owner, and outcome.

## Portfolio chores

Keep a private, non-public tracker with one row per app: repository, pinned
playbook SHA, version, last release date, target API, next review date, and
ship/iterate/kill decision. Do not put account IDs, signing details, tester
identities, or payout data in it unless a secure private system is appropriate.

At the yearly portfolio review, archive dead apps safely, check that each live
app is genuinely distinct, and compare effort with actual user value. Do not
keep an app alive solely because it has already consumed time or money.
