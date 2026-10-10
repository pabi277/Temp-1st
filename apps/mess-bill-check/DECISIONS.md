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

## D-006 — v1 uses framework Views, not Jetpack Compose

**Date**: 2026-10-11 · **Status**: open, needs the owner's nod before a Play release

`research/bprime-v1-plan-2026-10-11.md` §4 specifies Compose + Material 3. Compose needs the
Kotlin Gradle plugin, and D-002 established that AGP 9.4.0 rejects it in this build
configuration. Rather than freeze the build on a plugin I could not verify, v1 is written with
`android.widget` views and hand-built layouts. Consequences: no `MaterialTheme` tokens, no
`Room`/`DataStore` (see D-007, D-008), and screens are Activities rather than one Activity with
navigation. The bill maths, the ledger and every export format are UI-independent, so a Compose
rewrite replaces `ui/` only.

**Revisit when**: the app earns attention (kill-test thresholds met) or D-002's plugin problem is
solved on a machine that can run Gradle locally.

## D-007 — persistence is a hand-written JSON file, not Room

Room needs KSP, which needs the Kotlin plugin (D-002). `core/Json.kt` is a small strict parser
and `core/LedgerStore.kt` writes atomically to `filesDir` with a `.tmp` + rename, keeping the
previous file as `ledger.json.prev` and refusing a snapshot rewrite after a period is closed.
Chosen deliberately over a naive writer because the failure mode it protects against is the one a
student cannot recover from: losing a month of meal rows. Scale is irrelevant here — a period is
~31 rows, and parsing takes microseconds.

**Revisit when**: multi-period searching or per-row queries appear, i.e. when a real database would
buy something.

## D-008 — settings live in `SharedPreferences`, not `DataStore`

Same plugin constraint. The five settings (rate basis, rounding, rebate minimum, language, theme
later) are primitives, read once per screen, never streamed. `Prefs` is a typed accessor with no
business logic in it, and language is deliberately stored outside the ledger so an export stays
readable by another phone whose language differs.

## D-009 — v1 has no reminders, and export goes through `MediaStore` only

The plan §11 lists a daily meal-log reminder with `POST_NOTIFICATIONS`. A notification permission
request in week one contradicts "ask for nothing" and would need a runtime-permission path I can't
test from CI, so reminders are deferred to v1.1. Export writes through `MediaStore.Downloads` on
API 29+ and to the app-private external directory below that, which needs no permission at all —
the trade is that the user finds the file in `Download/` rather than in a shared-document provider.

## D-010 — verification is compile-and-unit-test only

There is no emulator or Android SDK in the environment that built this, so per RULES.md 11 the
only gate is CI: `testDebugUnitTest` (54 tests over the pure-Kotlin core) plus `assembleDebug`.
The five screens have therefore never been drawn, no tap has ever been simulated, and
localisation has not been read by a speaker. Before the first Play track, someone must walk
§「manual checklist」of the plan on a real device, including the permission dialog path for
`READ_MEDIA_IMAGES`-free export on API 24 and the language switch.
