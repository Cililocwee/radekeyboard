---
name: prepare-to-deploy
user-invocable: true
description: Walk the user step-by-step through bundling, signing, and shipping Rade Keyboard to the Google Play Store — spelling out every manual click/navigation for the parts an agent can't do. Use when the user invokes /prepare-to-deploy or asks how to build, bundle, sign, release, or publish the app to Google Play.
---

# Prepare to Deploy — Rade Keyboard → Google Play

Walk the user through a release. They context-switch between many projects, so the
job of this skill is to **remove all recall burden**: do the mechanical repo work
yourself, and for anything you can't do (keystore secrets, keytool prompts, Play
Console clicks), spell out the *exact* steps — what to type, where to navigate, what
to click — inline. Do NOT just point at `docs/deploy-to-google-play.md`; reproduce
the steps here so the user never has to leave the conversation.

`docs/deploy-to-google-play.md` is the deeper reference, but this skill must stand on
its own.

## How to run it

Go through the phases below **in order**. After each phase that needs the user to do
something manual, pause and wait for them to confirm before continuing. Reflect the
repo's ACTUAL current state (read the files — don't assume).

---

### Phase 1 — Version (agent does this)

1. Read `app/build.gradle`; report the current `versionCode` (integer) and
   `versionName` (string).
2. State plainly: **`versionCode` must strictly increase for every Play upload** — even
   a re-upload of the "same" version needs a higher number.
3. Ask whether to bump now and to what. If yes, edit `app/build.gradle`.

### Phase 2 — Signing setup (MANUAL — walk them through it click by click)

**Rade Keyboard has already shipped to the Play Store**, so there is an existing upload
key that MUST be reused. Do NOT create a new keystore — a new key won't match what Play
has registered and every upload will be rejected ("signed with the wrong key").

**Known signing facts for this app** (recovered from the original project's IDE config;
the password is NOT recorded anywhere and only the user has it):
- **Upload keystore:** `~/Desktop/Important Documents/radekeyboard-keystore`
- **Format:** PKCS12 → the store password and key password are the **same single value**.
- **Key alias:** `radekeyboard-key`
- **There IS a store password** (empty was verified incorrect). Almost certainly already
  enrolled in Play App Signing (the app is live).

⚠️ **PKCS12 listing trap:** `keytool -list` will show the `radekeyboard-key` alias even
with a blank/wrong password — it just prints an "integrity has NOT been verified"
WARNING and lists anyway. That does NOT mean the keystore is passwordless. *Signing*
(the release build) requires the correct password even though *listing* did not. Do not
assume "it listed fine" == "no password".

First **detect state**: look for a `signingConfigs` block in `app/build.gradle` and a
`keystore.properties` file at the repo root. Confirm `keystore.properties`, `*.jks`,
`*.keystore` are gitignored.

- **If signing is already wired up:** say so (✅) and skip to Phase 3.
- **If it's not wired up yet** (the current state): walk the user through reusing the
  existing key. The agent must NOT run keytool or handle passwords.

  **2a. Confirm the keystore is present.** Check that
  `~/Desktop/Important Documents/radekeyboard-keystore` exists (agent can `ls` it). If it
  has moved, search for it: `find "$HOME" -iname "radekeyboard-keystore*"`. If it truly
  can't be found anywhere, jump to **2d (lost key)**.

  **2b. User verifies the passwords** (only they have these). Have them run:
  ```bash
  keytool -list -v -keystore ~/Desktop/Important\ Documents/radekeyboard-keystore
  ```
  It prompts for the **keystore password**; success + a listed `radekeyboard-key` alias
  confirms both the file and the store password. (If the key has a separate password,
  they'll need that too for the build.)

  **2c. Agent wires Gradle to the existing key.** The agent will:
  - Write a gitignored `keystore.properties` at the repo root, pre-filled with the known
    path + alias and `CHANGE_ME` password placeholders:
    ```properties
    storeFile=/Users/<you>/Desktop/Important Documents/radekeyboard-keystore
    storePassword=CHANGE_ME
    keyAlias=radekeyboard-key
    keyPassword=CHANGE_ME
    ```
  - Add a `signingConfigs` block to `app/build.gradle` that loads `keystore.properties`
    and wire `buildTypes.release.signingConfig` to it.
  - Verify `.gitignore` covers `keystore.properties`, `*.jks`, `*.keystore`.

  Then tell the user: **open `keystore.properties` and replace both `CHANGE_ME` values
  with the passwords from step 2b.** (The agent never sees these.)

  **2d. Lost key fallback** (only if the keystore genuinely can't be found): if enrolled
  in Play App Signing, you can register a new upload key — Play Console → **Test and
  release → Setup → App signing → Request upload key reset** (Google emails a process;
  takes ~2 days). Only then create a new keystore with `keytool -genkey`. If NOT enrolled
  and the key is lost, the app can't be updated under the same listing.

### Phase 3 — Pre-flight checks (agent runs these, reports pass/fail)

- `git status` — clean working tree, on the intended branch, changes merged to `main`.
- `make test` — must pass (report the count).
- `make lint` — offer to run it; report result.
- **Ask the user to confirm manually** (agent can't): they've verified on a real device
  that the keyboard enables, sets as default, types, tone marks + long-press work, and
  onboarding completes.

### Phase 4 — Build the signed bundle (agent does this)

- Run `make release` (`./gradlew bundleRelease`).
- Report the output path: `app/build/outputs/bundle/release/app-release.aab`.
- Confirm the build was **signed** (if Phase 2 was skipped because signing wasn't wired
  up, STOP and tell the user the bundle is unsigned and will be rejected).
- Remind: the `.aab` is a build artifact — never commit it.

### Phase 5 — Upload to Play Console (MANUAL — spell out every click)

The agent cannot log into the Play Console. Give the user this exact path:

1. Go to https://play.google.com/console → select **Rade Keyboard**.
2. Left sidebar: **Test and release**, then pick a track:
   - **Testing → Internal testing** ← recommended for the first upload (up to 100
     testers, available in minutes).
   - or **Closed testing** / **Open testing** for wider beta, or **Production** to go live.
3. Click the **Create new release** button (top right).
4. Under **App bundles**, click **Upload** and select
   `app/build/outputs/bundle/release/app-release.aab`.
5. Fill in **Release name** (default is the `versionName`) and **Release notes** (put
   your "what's new" text between the `<en-US>`/language tags shown).
6. Click **Next** / **Save**, then resolve any errors or warnings the console flags
   (permissions, data safety, target API level, content rating).
7. Click **Review release**, then **Start rollout to <track>** and confirm.
   - Internal testing is near-instant; Production review can take hours to a few days.
8. First time only: the console may require the **Store listing**, **Data safety**, and
   **Content rating** sections to be complete before it lets you roll out — fill those
   under **Grow → Store presence** / **Policy → App content**.

### Phase 6 — After rollout (agent can help)

- Offer to tag the release in git: `git tag v<versionName> && git push --tags`.
- Remind the user to bump `versionCode`/`versionName` for the next cycle.
- Point to **Test and release → App quality → Android vitals** to watch crashes/ANRs
  once live.

---

## Output Format

Lead with a compact status checklist reflecting the ACTUAL current state, e.g.:

```
Release readiness — Rade Keyboard
  ✅ versionCode 8 / versionName 1.8
  ⚠️  signing NOT wired up — one-time setup needed (Phase 2)
  ✅ tests passing (8)
  ⬜ device smoke-test — needs your confirmation
  ⬜ Play Console upload — manual (Phase 5)
```

Then walk the user through whichever phase they're on. **Do** the mechanical steps they
approve (version bump, writing `keystore.properties`/`signingConfigs` scaffolding,
running build/test/lint, git tagging). **Never** run `keytool`, type or read signing
passwords, or attempt the Play Console upload — for those, present the exact
click/type/navigate steps and let the user act.
