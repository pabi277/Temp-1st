<!-- SPDX-License-Identifier: MIT -->

# AdMob setup checklist

This is a release checklist, not a source of account or ad identifiers. Keep
real IDs out of this playbook. Use placeholders in public code and protected
configuration in each app repository.

## Account and app setup

- [ ] Create and verify the AdMob account and payments profile using the
      owner's real legal and tax information; never put those details in Git.
- [ ] Add the app in AdMob and create only the ad units the approved product
      plan needs.
- [ ] Use separate, clearly named development and production configuration.
- [ ] Store production values outside the repository (for example, protected
      Actions configuration); never print values in CI logs.
- [ ] Keep the app's privacy policy, Play Data Safety answers, consent settings,
      and actual SDK behavior in agreement.

## App ID in the manifest

The **app ID** and **ad unit IDs** are different values. Add the app ID as the
Google Mobile Ads manifest metadata, using a placeholder rather than a real ID:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobAppId}" />
```

Wire `admobAppId` from a local/protected build configuration. Replace the
placeholder `YOUR_ADMOB_APP_ID` only in the owner's private configuration.
Check the merged release manifest before upload.

## Test IDs → production IDs

- [ ] Use Google's current official demo/test ad units during development.
      Copy them from the [Android test ads guide](https://developers.google.com/admob/android/test-ads);
      this playbook intentionally contains no literal AdMob IDs.
- [ ] Use emulator/test-device mode for development. Never click a live ad or
      ask a tester to click one.
- [ ] Before release, replace every demo/test value with the intended app's
      production app ID and ad-unit IDs from protected configuration.
- [ ] Review the merged manifest and release configuration so test units cannot
      ship accidentally and production units are not used during manual tests.
- [ ] Exercise the release path with test devices and verify ads are not loaded
      when consent or the app's `Policy` object says no.

## Consent and privacy choices

- [ ] Configure the current UMP privacy messages (or another eligible CMP) for
      the regions and audiences the app serves.
- [ ] Request a consent-information update on every app launch.
- [ ] Show a required consent form and a privacy-options entry point when the
      current configuration requires them.
- [ ] Request ads only when the consent SDK says ads may be requested; prevent
      duplicate requests while consent checks complete.
- [ ] Test first launch, returning user, no-network, consent change, and privacy
      options. Do not treat a cached answer as current without checking the SDK.
- [ ] Re-check the chosen SDK's release notes and support status. Google
      documentation has distinguished the legacy Mobile Ads SDK from newer
      integrations; do not copy stale setup code or pin an unreviewed version.

Official starting points: [UMP for Android](https://developers.google.com/admob/android/privacy)
and [UMP release notes](https://developers.google.com/admob/android/privacy/release-notes).

## Placements

- [ ] Rewarded ads are user-initiated and optional; explain the reward before
      the user chooses to watch. Grant it only after the SDK confirms completion.
- [ ] Interstitials, if used, appear only at a natural break, never during a
      task, answer, navigation action, or app launch.
- [ ] Add a frequency cap and never place an ad where it can be mistaken for a
      control. Keep core offline features useful without an ad.
- [ ] The ad-free purchase is visible and works; test purchase state and restore.
- [ ] Verify behavior with an accessibility pass and a slow/no-network test.

## PIN by post and payments

- [ ] If AdMob requests address verification, confirm the private payments
      address and handle any mailed PIN only in the AdMob account.
- [ ] Never store or send the PIN, account number, tax identity, or payout
      details in this repository, a prompt, a review reply, or a screenshot.
- [ ] Check the payment page for current verification steps, holds, and currency
      thresholds; estimated earnings are not the same as finalized earnings.
