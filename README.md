# Afterglow

Phase 1 of the messaging app: accounts with unique usernames, username
search, and real-time 1:1 text messaging. Kotlin + Jetpack Compose +
Firebase (Auth + Firestore).

## What's in this phase

- Registration with a unique, enforced username ("Username already taken.
  Please choose another one." shown on collision)
- "Continue with Google" as an alternative to username/password (via
  Credential Manager) — first-time Google sign-ins are prompted to pick a
  username right after, since Google doesn't provide one
- Automatic sign-in after registration; sessions persist until logout
  (handled by Firebase Auth)
- Search for other users by username
- Start a 1:1 conversation instantly
- Real-time message delivery via Firestore listeners (no manual refresh)
- minSdk 28 (Android 9 / Pie) through the current API level

## Not in this phase yet

Stickers, voice messages, and photo/video sharing all layer onto the
existing `Message`/`MessageType` model — mostly UI + Firebase Storage
upload work, no schema changes needed. Live video calling is a separate
system (a WebRTC-based call SDK, e.g. Agora or Twilio, plus call
signaling) and hasn't been started.

## One-time setup

1. **Open in Android Studio** (a recent stable release — anything that
   bundles AGP 9.x / Kotlin 2.4+ will do). Let it sync once; it may prompt
   an Upgrade Assistant for the exact AGP/Gradle patch version on your
   machine — accepting that prompt is fine.

2. **Create a Firebase project** at [console.firebase.google.com](https://console.firebase.google.com).

3. **Add an Android app** to that project with package name
   `com.afterglow.messenger`. Download the generated `google-services.json`
   and place it at `app/google-services.json`. (Not included here — it's
   tied to your specific Firebase project.)

4. **Enable Authentication → Sign-in method → Email/Password.** (The app
   never shows an email field to the user — it derives an internal email
   from their username. See the comment at the top of `AuthRepository.kt`
   if you want to understand or change that.)

5. **Enable Cloud Firestore** (start in production mode — the rules below
   handle access control), then deploy `firestore.rules`:
   ```
   firebase deploy --only firestore:rules
   ```
   or paste its contents into the Rules tab in the console.

6. Run on a device or emulator running Android 9+.

## Setting up "Continue with Google"

Google Sign-In needs two more things done in the Firebase console, plus a
fresh `google-services.json`:

1. Firebase console → **Authentication → Sign-in method → Add new provider
   → Google** → enable it.
2. **Project settings** (gear icon, top of the left sidebar) → scroll to
   **Your apps** → select the Android app → **SHA certificate
   fingerprints → Add fingerprint**. Add both of these (they belong to the
   debug keystore already committed at `app/debug.keystore` — see note
   below):
   ```
   SHA-1:   EF:AB:F8:5C:76:63:AA:98:A8:A2:00:A7:29:8A:45:91:3E:A8:40:48
   SHA-256: 47:57:A2:C6:8B:50:1D:2A:83:9C:52:21:23:B1:72:DE:3E:FB:81:47:DD:D6:5B:8D:02:2A:B6:2F:5F:07:34:64
   ```
3. Back on the Project settings page, **download `google-services.json`
   again** and replace the one in `app/` with it. This step is easy to
   miss and the build *will* fail without it — enabling Google sign-in
   adds an OAuth web client entry to that file, and the app reads its
   client ID from a string resource (`default_web_client_id`) that only
   exists once you're using the updated file.

**About `app/debug.keystore`:** normally Android tooling auto-generates a
debug signing key per machine, which is fine until something (like Google
Sign-In) needs to check a fixed SHA-1 fingerprint — a GitHub Actions build
would then get a different, unregistered fingerprint on every run and
Google sign-in would fail with no clear error. So this project commits one
fixed debug keystore instead (wired up in `app/build.gradle.kts`) and the
fingerprints above are permanent as long as that file doesn't change. This
is a debug-only convenience — a real Play Store release would need its own
separate, properly secured release keystore (not committed to the repo),
with its own SHA-1 added here too.

## Building without installing Android Studio (GitHub Actions)

If the machine you have available can't run Android Studio (it needs a
64-bit OS), you can compile this into an installable APK entirely in the
browser using GitHub Actions — no local Android tooling required. The
workflow file is already included at
`.github/workflows/build-debug-apk.yml`.

1. Do steps 2-5 above first (Firebase project + `google-services.json`) —
   the build will fail without a real `google-services.json` in `app/`,
   same as it would locally. Add that file into the `app/` folder on your
   computer before uploading.
2. Create a free account at [github.com](https://github.com) if you don't
   have one.
3. Create a new repository (private is fine — Actions works on private
   repos too, with generous free monthly minutes).
4. On the repo page, use **Add file → Upload files** and drag the entire
   contents of this project folder in (including the `.github` folder —
   some browsers hide dot-folders in a plain file picker, so dragging the
   whole folder tends to work better than picking files one by one).
   Commit the upload.
5. Go to the **Actions** tab. The build should start automatically; if not,
   select the workflow and click **Run workflow**.
6. When it finishes (a few minutes), open the run and download the
   `afterglow-debug-apk` artifact — that's a zip containing `app-debug.apk`.
7. Get that APK onto your phone (Google Drive, email attachment, a USB
   cable, however's easiest), tap it, and allow "install from this source"
   when Android asks. That's it — no Play Store needed for a debug build.

Note: this project intentionally doesn't include `gradle-wrapper.jar` (a
small binary file this environment couldn't download). GitHub Actions
doesn't need it — the workflow installs Gradle directly. If you later open
the project in real Android Studio, it will offer to regenerate the
wrapper automatically on first sync; just accept that prompt.

## Notable design decisions

- **No Hilt/dependency-injection framework.** ViewModels construct their
  repositories directly instead. This is partly simplicity, partly
  deliberate: DI annotation-processor setups are one of the more common
  sources of "works on my machine" build breakage, and this code hasn't
  been compiled in a real toolchain before reaching you. Swapping in Hilt
  later is a contained change if you want it.
- **Client-side timestamps**, not `FieldValue.serverTimestamp()`. Simpler
  and fine for a v1; if you want to be resistant to a sender's device clock
  being wrong, that's a natural hardening step in `ChatRepository`.
- **No Firebase KTX artifacts** — those were removed from the Firebase
  Android BoM; this project uses the main modules directly, which now
  include the Kotlin-friendly APIs.
