# Deploy Rade Keyboard to the Google Play Store

A start-to-finish playbook for bundling and shipping this app. It exists because
release steps are easy to forget when you've been away from a project — run
`/prepare-to-deploy` for a version-aware, interactive walk-through, or follow this
document by hand.

> **App:** `com.corriestroup.radekeyboard` · **Artifact:** Android App Bundle (`.aab`)
> · **Build:** Gradle (`./gradlew`) · Release track uploads happen in the
> [Google Play Console](https://play.google.com/console).

---

## 0. One-time setup (only the first time, per machine / per app)

You do NOT repeat these for every release.

### a. Google Play developer account
- A Google Play Console account ($25 one-time fee) with the app already created
  under the package name `com.corriestroup.radekeyboard`.

### b. Upload keystore (signing key)
Play requires every release to be signed. Create an **upload keystore** once and
keep it safe — losing it (without Play App Signing enrolled) means you can never
update the app.

```bash
keytool -genkey -v \
  -keystore rade-upload-key.jks \
  -keyalias rade-upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Store `rade-upload-key.jks` **outside the repo** (it must never be committed) and
record the keystore password, key alias, and key password in a password manager.

### c. Enroll in Play App Signing
Strongly recommended. Google holds the real app-signing key; you sign uploads with
the upload key above. If the upload key is ever lost, Google can reset it. Enroll
when you create the app (or under **Release → Setup → App signing**).

### d. Wire signing into Gradle (kept out of git)
Put credentials in `keystore.properties` (gitignored) at the repo root:

```properties
storeFile=/absolute/path/to/rade-upload-key.jks
storePassword=********
keyAlias=rade-upload
keyPassword=********
```

Then reference it from `app/build.gradle` (add a `signingConfigs` block and point
`buildTypes.release.signingConfig` at it, loading values from
`keystore.properties`). Verify `keystore.properties` and `*.jks` are in
`.gitignore`.

---

## 1. Pre-flight checklist (every release)

- [ ] **Bump the version** in `app/build.gradle`:
  - `versionCode` — integer, **must strictly increase** for every Play upload.
  - `versionName` — human-readable string (e.g. `1.8`).
- [ ] Working tree is clean and on the intended branch; changes merged to `main`.
- [ ] `make test` passes.
- [ ] `make lint` is clean (or reviewed).
- [ ] Manually verified on a device: keyboard enables, sets as default, types,
      tone marks + long-press work, onboarding completes.
- [ ] Release notes / "What's new" text drafted (per supported language).
- [ ] Store listing still accurate (screenshots, description, data-safety form,
      target-audience/content rating). Google periodically requires re-confirmation.

---

## 2. Build the release bundle

```bash
make release          # → ./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

Notes:
- Upload **`.aab`** (App Bundle), not `.apk`. Google generates per-device APKs from it.
- The bundle must be **signed** with your release/upload key (via the `signingConfig`
  from step 0d). An unsigned or debug-signed bundle will be rejected.
- The bundle is a build artifact — do **not** commit it. `app/release/` and `build/`
  are gitignored.

Sanity-check the artifact before uploading:

```bash
# Confirm the versionCode/versionName baked into the bundle
$ANDROID_HOME/build-tools/<version>/aapt2 dump badging app/build/outputs/bundle/release/app-release.aab 2>/dev/null | grep versionName || true
```

(Or just trust the `app/build.gradle` values you bumped in step 1.)

---

## 3. Upload to the Play Console

1. Go to the [Play Console](https://play.google.com/console) → select **Rade Keyboard**.
2. Pick a track under **Release**:
   - **Internal testing** — fastest, up to 100 testers, near-instant. Best for a first
     smoke test.
   - **Closed / Open testing** — wider beta.
   - **Production** — public rollout.
3. **Create new release** → upload `app-release.aab`.
4. Enter the **release name** (often the `versionName`) and **release notes**.
5. Resolve any warnings the console surfaces (permissions, data safety, target API
   level — must meet Google's current minimum, which is why `targetSdk` is 35).
6. **Review release** → **Start rollout**. For production you can use a **staged
   rollout** (e.g. 20% → 100%).

---

## 4. After rollout

- Internal-testing builds are available to testers within minutes; production review
  can take hours to a few days.
- Monitor **Release → Track** for review status, and **Quality → Android vitals** for
  crashes/ANRs once live.
- Tag the release in git (e.g. `git tag v1.8 && git push --tags`) so the shipped
  commit is recoverable.
- Bump `versionCode`/`versionName` again for the next cycle.

---

## Common rejections & gotchas

| Symptom | Cause / fix |
|---------|-------------|
| "Version code N has already been used" | `versionCode` not incremented — bump it in `app/build.gradle`. |
| "Your APK/AAB is not signed" / signed with debug key | Release `signingConfig` not wired up (step 0d) or built with `assembleDebug` instead of `bundleRelease`. |
| Target API level too low | Google raises the minimum `targetSdk` yearly; this repo tracks it (currently 35). |
| Data safety / permissions questions | Keep the app permission-light. Every declared permission in `AndroidManifest.xml` must map to a real feature, or the data-safety form gets complicated. |
| Lost upload key | Only recoverable if enrolled in Play App Signing (step 0c). Otherwise you cannot update the app. |
| IME-specific policy | Keyboards must disclose in the listing that they can capture input; do not transmit keystrokes off-device. This app has no network by design — keep it that way. |
