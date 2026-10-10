<!-- SPDX-License-Identifier: MIT -->

# 25-person opt-in tester plan

This plan recruits genuine volunteers for a real closed test. It is not a
review campaign. Never buy installs, require a positive rating, or publish a
list of tester names, emails, phone numbers, or chat screenshots.

## Before sending invitations

- [ ] Confirm the account type and current requirement in Play Console.
- [ ] Identify 25 people who freely agree to try the app. Use anonymous slots
      `T01`–`T25` in the private tracker; keep contact details outside Git.
- [ ] Prepare a working test build, clear install/opt-in instructions, a private
      feedback route, and an honest description of the data the app handles.
- [ ] Explain the current gate: eligible newer personal accounts require at
      least 12 people opted in continuously for 14 days. 25 is a buffer target,
      not Google's required number; verify the live Console rule.
- [ ] Set a calendar reminder for the first day real users opt in. Do not start
      or claim the Play clock until the closed test is available and opt-ins are
      recorded.

## Find 25 genuine volunteers

Recruit in small, relevant groups. Invite only people likely to understand the
app and willing to try it; do not scrape or mass-message strangers.

| Group | Planning slots | Good fit |
| --- | --- | --- |
| Friends or family who match the audience | T01–T05 | Can install and give candid feedback |
| Work, study, or hobby peers | T06–T10 | Familiar with the real task |
| Local language/community contacts | T11–T15 | Can check language or local details |
| Relevant opt-in community | T16–T20 | Invite only where community rules allow |
| Backup volunteers | T21–T25 | Replacements if someone cannot continue |

The private tracker may hold slot, consent date, opt-in status, and a feedback
summary. Keep actual identity/contact data in a private place with limited
access and delete it when no longer needed. Never put it in this repository.

## During the test

- Confirm each volunteer can install and opt in; share bug-report instructions.
- Ask people to use the app naturally and report truthful problems, not to rate
  it positively. Give a quiet check-in midway through the period.
- Track opt-in continuity privately. If someone opts out, do not pressure them;
  check Play Console to see whether the eligible count still meets the rule.
- Record bugs without names, emails, phone numbers, or sensitive app data.
- Fix crashes and content errors. Keep evidence of real testing for the
  production-access questions.

## WhatsApp invitation draft

> Hi! I'm preparing a small Android app for a genuine Google Play closed test.
> Would you be willing to opt in, install it, and try it honestly for at least
> 14 days? This is not a request for a rating or a positive review. You can say
> no or leave the test at any time. I’ll send the opt-in link and a private way
> to report problems. Please tell me if you cannot continue; no pressure.

Before sending, add the app's plain-language purpose and disclose any relevant
privacy details. Send the invite only to people who have agreed to hear from
you. Do not forward anyone else's contact details or opt-in link without
permission.
