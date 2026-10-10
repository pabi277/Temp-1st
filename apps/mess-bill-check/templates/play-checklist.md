<!-- SPDX-License-Identifier: MIT -->

# Google Play release checklist

Re-check live Play Console requirements before every submission. This list is
not a guarantee of approval. A complete checklist or green CI is not permission
to create a PR or submit a release; get explicit owner authorization for each.

## Build and policy

- [ ] Green CI: tests pass; signed release AAB and APK are available as an
      Actions artifact; the upload key is not in Git.
- [ ] New mobile app/update targets Android 16 / API 36 or higher as of
      2026-08-31. Verify the current target requirement and exceptions in the
      [official policy](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
      before release.
- [ ] App is genuinely distinct, functional, and accurately represented; no
      repetitive-content reskin, misleading metadata, or borrowed IP.
- [ ] Complete the current content-rating questionnaire honestly. Match the
      target audience, ads, in-app purchases, and actual app behavior.
- [ ] Review every permission and SDK; remove anything not needed for the
      approved core feature.

## Listing assets and text

Check the current [asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
in Play Console before uploading. Current common requirements:

- [ ] App icon: 512 × 512 px, 32-bit PNG with alpha, up to 1 MiB.
- [ ] Feature graphic: 1024 × 500 px, JPEG or 24-bit PNG without alpha.
- [ ] Screenshots: JPEG or 24-bit PNG without alpha; 320–3840 px, with the
      longest side no more than twice the shortest. Confirm required number,
      device types, and any device-specific rules in Console.
- [ ] Short description is within the current 80-character limit; long
      description and every screenshot describe the shipped app accurately.
- [ ] No rankings, fake testimonials, unsupported pass-rate claims, misleading
      official affiliation, or keyword stuffing.

## Privacy, safety, and ads

- [ ] Publish a working privacy-policy URL and link it in Play Console and the
      app. It describes the actual release, SDKs, data, consent, retention, and
      contact route.
- [ ] Complete Data Safety truthfully for the app **and every SDK**; update it
      whenever the release changes. Match it to the privacy policy.
- [ ] Declare AdMob and any other ad, analytics, billing, or crash SDK behavior
      accurately. Test consent and privacy options.
- [ ] Complete the content-rating and target-audience forms accurately; apply
      Families requirements if the app is directed to children.

## Testing gate — do not use the stale “20 testers” number as the rule

For personal developer accounts created after 2023-11-13, Google's current
page requires at least **12 testers continuously opted in for the preceding
14 days** before applying for production access. Account type matters; verify
the current [testing requirement](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
in the owner's Play Console. This playbook's 25-person plan is a recruiting
buffer; 20 is not the current minimum. Use genuine, voluntary testers only.

## Payments, tax, and support

- [ ] Complete the Play payments profile and current country-specific tax
      forms. Google's guidance says non-US merchants outside the US submit
      Form W-8BEN for the US tax profile; confirm the owner's situation in
      [Payments settings](https://support.google.com/googleplay/android-developer/answer/7163598?hl=en).
- [ ] For an ad-free digital feature, use the billing method required by the
      current [Payments policy](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en)
      for each target market; verify eligible program exceptions before use.
- [ ] Monitor reviews. Reply politely, protect user privacy, state only
      verified facts, and never ask for a positive rating or offer a reward.
- [ ] Keep a private record of test results and support requests; do not commit
      tester identities, tax records, account IDs, PINs, or credentials.
