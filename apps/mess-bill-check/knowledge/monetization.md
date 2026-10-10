# Monetization that respects the user

Choose one primary model per app and prove the app is useful before tuning
revenue. The default is offline-first and free to use for its core task.

## Rewarded-first ads

- Offer a rewarded ad only when the user chooses it and understands the reward.
- Grant the promised reward only after the SDK confirms completion.
- Do not require an ad to reach a core answer, escape a screen, or use the
  offline content.
- Use interstitials only at a natural break, with conservative frequency; no
  app-open surprises, mid-task interruptions, or ad-shaped controls.
- Add consent checks and a host-testable `Policy` object so core tests do not
  depend on the ad SDK.
- Do not click live ads, induce clicks, or ask testers to click ads.

## Ad-free purchase

Rule 9 requires an ad-free IAP as an opt-out. Keep it simple: one clear,
one-time non-consumable purchase that removes every ad, with purchase restore
and a stable local entitlement state. Do not use a subscription unless the
product truly provides ongoing value and the owner approves that different
model.

The ₹49–99 range is an initial India price experiment, not a market fact or
profit promise. Choose the actual price in Play Console and review localized
prices, taxes, refunds, and support obligations. A digital ad-free feature
usually requires Google Play Billing unless a current, applicable program
exception applies; check the [Payments policy](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en)
for each market. Explain exactly what the purchase changes.

Interpret “one monetization model per app” as one primary revenue model; the
required ad-free purchase is the one permitted opt-out path, not permission to
add subscriptions, multiple ad loops, or unrelated paywalls.

## Revenue math and thresholds

A simple estimate is:

```text
estimated ad earnings ≈ impressions ÷ 1,000 × reported eCPM
```

Use the app's own country- and format-specific reports. Do not use store
downloads as ad impressions or assume every session produces an impression.

AdMob's payment thresholds depend on the account's payment currency. For a
USD account the payment threshold is USD 100; other currencies have their own
threshold values. Threshold eligibility is based on finalized earnings, and
payment setup or holds can delay payout. This is an AdMob payment threshold,
not a universal Google Play payout rule. Check the [current AdMob threshold
table](https://support.google.com/admob/answer/2772208?hl=en) and the actual
payment profile.

## One small experiment

Before launch, write down the primary model, the exact placement, what the user
gets, the ad-free behavior, and the aggregate metric that would justify keeping
it. Review retention, useful task completion, crash rate, complaints, and
revenue together. If ads harm the core task, remove or move them; do not
optimize clicks at the user's expense.
