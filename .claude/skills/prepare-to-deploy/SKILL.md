---
name: prepare-to-deploy
user-invocable: true
description: Produce a version-aware, step-by-step checklist for bundling and shipping Rade Keyboard to the Google Play Store. Use when the user invokes /prepare-to-deploy or asks how to build, bundle, sign, release, or publish the app to Google Play.
---

# Prepare to Deploy — Rade Keyboard → Google Play

This skill walks the user through releasing the app. The canonical reference is
`docs/deploy-to-google-play.md`; this skill turns it into an actionable, *current-state*
checklist by inspecting the repo.

## Steps

1. **Read the current version.** Open `app/build.gradle` and report the current
   `versionCode` and `versionName`. Remind the user that `versionCode` MUST strictly
   increase for every Play upload, and ask whether to bump it now (and to what). If
   they say yes, edit `app/build.gradle`.

2. **Check signing is wired up.** Look for a `signingConfigs` block in
   `app/build.gradle` and a `keystore.properties` file (gitignored) at the repo root.
   - If missing, tell the user this is the one-time setup (keystore creation + Play App
     Signing + Gradle wiring) from section 0 of `docs/deploy-to-google-play.md`, and
     that release uploads will be rejected until it's done.
   - Confirm `keystore.properties` and `*.jks` are in `.gitignore`.

3. **Run pre-flight checks** (report pass/fail, don't just assert):
   - Clean working tree / on the right branch (`git status`).
   - `make test` passes.
   - `make lint` is clean (offer to run it).
   - Ask the user to confirm they've manually verified the keyboard on a device
     (enable → set default → type → tone marks → long-press → onboarding).

4. **Build the bundle.** Run `make release` (`./gradlew bundleRelease`). Report the
   output path: `app/build/outputs/bundle/release/app-release.aab`. Remind that the
   `.aab` is a build artifact and must not be committed.

5. **Print the Play Console steps** (these are manual — the agent cannot do them):
   choose a track (Internal testing first), create a new release, upload the `.aab`,
   add release notes, resolve console warnings, review, and roll out. Point to
   sections 3–4 of `docs/deploy-to-google-play.md`.

6. **Offer post-release follow-ups:** tag the release in git (`v<versionName>`) and
   remind them to bump the version again for the next cycle.

## Output Format

Present a concise, checkbox-style checklist reflecting the ACTUAL current state
(e.g. "✅ versionCode is 8", "⚠️ no signingConfig found — see one-time setup"), not a
generic copy of the doc. Only do the mechanical steps the user approves (version bump,
running builds/tests); never attempt the Play Console upload or touch signing secrets.
