# Common Play rejection risks and prevention

This is a practical, non-exhaustive checklist—not a ranked statistic or a
promise of approval. Read the exact Play Console notice and policy section for
every decision.

| Risk family | Prevention before submission |
| --- | --- |
| Target API below the current requirement | Check the official target API page on release day; set a modern `targetSdk` from the first build and test behavior changes. As checked 2026-10-10, mobile new apps and updates require API 36 or higher. |
| Data Safety omits an SDK or conflicts with the app | Inventory every SDK, permission, and network call. Inspect current SDK disclosures; make Play Data Safety, in-app disclosure, and the privacy policy agree. |
| Missing, inaccessible, or misleading privacy policy | Host a working public policy URL, link it in Play Console and in the app, and describe actual collection, sharing, retention, and deletion. |
| Broken, empty, or materially different app | Test the signed release on a real device and a clean install; verify every screen, offline path, links, purchase, and declared feature. Do not submit a placeholder or misleading screenshot. |
| Excessive or unexplained permissions | Remove permissions not needed for the core purpose. Request sensitive access only with a clear, user-visible need and required disclosure/consent. |
| Disruptive or deceptive ads | Use clearly separated, policy-compliant placements; keep ads away from controls; test consent and frequency; do not force clicks or surprise users. |
| Misleading title, description, screenshots, or affiliation | Describe only working features. Avoid false official RTO/government affiliation, fabricated outcomes, unsupported pass rates, and keyword stuffing. |
| Repetitive content or reskin | Ship genuinely different products with distinct user value, not a bundle of near-identical apps with only colors or text changed. |
| Copyright, trademark, or copied question-bank content | Use owned or properly licensed assets and content; record sources and permissions. Verify that publicly accessible rules can legally be reused in the chosen form. |
| Incorrect audience, content rating, or billing disclosure | Answer Play Console questionnaires for the shipped app; disclose ads and IAP; test the purchase and restore paths; re-check current market-specific policy. |
| Closed-test requirement not satisfied | Confirm account eligibility and current Console status. For eligible newer personal accounts, the current rule is 12 continuously opted-in testers for 14 days before applying for production access; do not fabricate participation. |

## Official references

- [Google Play Developer Program Policies](https://play.google.com/about/developer-content-policy/)
- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Data Safety form](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Testing requirements for newer personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
