# Geography, eCPM, and search

Do not treat “Tier-1 pays X” or “India pays Y” as fixed facts. For planning,
compare exact countries, not an assumed tier multiplier. AdMob says eCPM can
fluctuate with market trends and monetization strategy; its reporting can be
compared by country and ad format. Seasonality can matter. Total earnings also
depend on impressions and match rate. Use the app's own reports after real
traffic exists; never promise an income.

## The math

AdMob eCPM is estimated earnings per thousand impressions. For a rough
revenue model, use the actual reported values:

```text
estimated earnings ≈ Σ(country impressions ÷ 1,000 × that country's eCPM)
```

Example with variables, not benchmarks:

```text
India:     I_IN ÷ 1,000 × E_IN
Other geo: I_GEO ÷ 1,000 × E_GEO
```

`E_IN` and `E_GEO` must come from the same reporting period, ad format, and
currency. Do not compare a rewarded-ad eCPM in one market with a banner eCPM in
another. Use the impression counts actually reported and track requests and
match rate separately; do not assume every request fills. Taxes, refunds,
payment holds, and currency conversion mean this is not take-home income.

If the hypothetical eCPM in one country were twice the other, the same number
of impressions would produce twice the estimated ad earnings before other
factors. That is arithmetic only—not a claim that any country always pays
more. Build a simple spreadsheet from actual reports and include zero/low-fill
scenarios.

## Product strategy by geography

- Research English search phrases that users actually type in each intended
  market; record exact wording, search results, ratings, and freshness.
- Build for global-English search only when the feature and content genuinely
  work for those users. A state-specific RTO guide is not global just because
  its interface is English.
- Keep language and local data in assets so the owner can review and update
  them without rewriting core logic.
- Launch with one audience and one clear phrase. Expand language or geography
  only after evidence; do not keyword-stuff or imply official affiliation.
- Check country-level impressions, retention, and useful actions before
  changing placements. Higher eCPM is not a reason to annoy or mislead users.

AdMob's payment thresholds depend on the account's payment currency. Check the
current [payment-threshold table](https://support.google.com/admob/answer/2772208?hl=en)
and payment profile; a USD threshold is not automatically the threshold for an
INR account. See [AdMob's eCPM definition and fluctuation guidance](https://support.google.com/admob/answer/15337570?hl=en).
