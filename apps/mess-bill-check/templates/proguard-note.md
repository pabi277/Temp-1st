<!-- SPDX-License-Identifier: MIT -->

# R8 / ProGuard keep-rules starter

Use this file as the app's `app/proguard-rules.pro` only when release minification
is enabled. Start with no broad keep rules. R8 keep rules can preserve code,
mask useful warnings, or break a release if copied from the wrong library
version.

## The one-section-per-library law

Add **one section per library** that actually needs app-level keep rules. Use
the library's official, version-matched documentation; do not copy a random
snippet from an old forum post. Many Android libraries publish consumer rules
that Gradle applies automatically.

For each required section, record:

- Library and exact version.
- Official source URL and date checked.
- Why reflection, serialization, JNI, or another behavior needs the rule.
- The smallest verified rule and the release test that covers it.

```proguard
# ---- Library: YOUR_LIBRARY_NAME @ YOUR_VERSION ----
# Source: YOUR_OFFICIAL_RULES_URL (checked YYYY-MM-DD)
# Reason: explain the exact reflective/JNI/serialization use.
# Replace this comment with only the minimum verified rules, for example:
# -keep class YOUR_LIBRARY_PACKAGE.YourRequiredType { *; }
# Release test: name the test or manual flow that proves minification works.
```

Do not ship the example rule above literally. Avoid broad rules such as
`-keep class ** { *; }`. Build and exercise the minified release after every
rule change; remove rules that are no longer needed.
