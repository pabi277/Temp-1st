<!-- SPDX-License-Identifier: MIT -->
# DECISIONS

Short log of decisions that shape this app, per AppFlow `RULES.md` law 12. Newest last.

## D-001 · 2026-10-11 · exception to RULES.md law 3 (timeline)
Wall-clock window extended from 4 weeks to 7–8 weeks because the owner's capacity is
< 5 h/week and v1 includes three locales (EN/HI/BN) plus PDF and CSV export. Scope was
**not** enlarged beyond the approved plan (`research/bprime-v1-plan-2026-10-11.md` §2);
the ~26–39 h phase budget is unchanged, only the calendar it sits on. Owner approved in
chat 2026-10-10. The day-28 kill gate stays binding.

## D-002 · 2026-10-11 · no Kotlin Gradle plugin (AGP built-in Kotlin)
`org.jetbrains.kotlin.android` is **not** applied. CI failed with a fatal KGP diagnostic:
"no longer required for Kotlin support since AGP 9.0 … remove the plugin", and AGP 9.0's
new DSL interfaces are incompatible with that plugin. Kotlin compiles via AGP's built-in
support (`:app:compileDebugKotlin` is green on AGP 9.4.0 + Gradle 9.8.1 + JDK 17).
Source of truth: AGP 9.0 release notes, "built-in Kotlin" section, checked 2026-10-11.
Consequence: no `kotlinOptions`/`jvmTarget` block; if a future Kotlin config need cannot
be expressed through the AGP DSL, revisit here rather than re-adding KGP.

## D-003 · 2026-10-11 · frozen CI template is stale on runner pins
`templates/ci-build.yml` (frozen snapshot of AppFlow `2963192`) pins
`actions/checkout@v4`, `actions/setup-java@v4` and `android-actions/setup-android@v3`.
`setup-android@v3` **fails** on current `ubuntu-latest`; upstream is `v4.0.4`
(2026-09-17), and GitHub annotates v4 pins as deprecated (setup-java v6, checkout v7,
upload-artifact v7). The staging workflow runs current pins. The frozen copy here is left
unchanged per START.md ("keep the copied snapshot unchanged; update it only through a
deliberate, documented refresh commit") — refreshing it is an AppFlow decision, and the
owner declined playbook edits on 2026-10-11.

## D-004 · 2026-10-11 · offline by construction, enforced by test
No `INTERNET` or any other permission in the manifest, no third-party runtime dependency,
and `PrivacyContractTest` fails the build if a permission string appears. Chosen so the
Data Safety "no data collected" claim is true by machinery, after observing an incumbent
declare "No data shared with third parties" while its listing says "Contains ads".

## D-005 · 2026-10-11 · repo location (transitional)
The app's own repository could not be created: the session token lacks repository
Administration write (`gh repo create` → "Resource not accessible by integration").
Commits `4b05b1c..` are held locally and mirrored into the scratch repo's
`arena/4ebedb37-temp-1st` branch under `apps/mess-bill-check/` purely so CI can build.
Everything moves to `pabi277/mess-bill-check` intact once it exists; nothing is on GitHub
for this app yet.
