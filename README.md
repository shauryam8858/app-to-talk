# Echo

A tiny Android app that repeats exactly what you type (or say). No AI, no internet,
no accounts, no tracking. It uses only the standard Android SDK (no extra libraries).

## What the app does

- Type text, press **Echo**: the same text appears in the Output box (Hindi, English, emojis,
  numbers, punctuation, spaces and line breaks are copied unchanged).
- **Copy** copies the output. **Clear** empties both boxes.
- **Mic** (optional): works only on Android 13 or newer AND only if your phone has an
  offline speech recognizer installed. If not, the app tells you and never falls back to
  an online service. Microphone permission is asked only when you tap Mic.
- **Speak** (optional): reads the output with your phone's own text-to-speech, only with an
  already-installed offline voice. The app never downloads voices.
- The app has no INTERNET permission. Your text is never saved or sent anywhere.

## Build the APK for free (no Android Studio needed)

### 1. Make a GitHub account
Go to https://github.com , tap **Sign up**, and follow the steps (free plan).

### 2. Make a repository
Tap **+** (top right) > **New repository**. Name it `Echo`, choose **Public**, tick
**Add a README file**, then **Create repository**.

### 3. Upload the files (keep the folders!)
The folder layout must be exactly like this:

    .github/workflows/build.yml
    .gitignore
    build.gradle.kts
    gradle.properties
    settings.gradle.kts
    app/build.gradle.kts
    app/src/main/AndroidManifest.xml
    app/src/main/java/com/example/echo/MainActivity.kt
    app/src/main/res/... (layout, values, values-night, drawable, mipmap-anydpi-v26)

**Option A (computer, easiest):** unzip `Echo-project.zip`. Open the repository on GitHub >
**Add file > Upload files**. Open the unzipped `Echo` folder and drag everything INSIDE it
(including the `app` folder and the hidden `.github` folder) into the page, then **Commit changes**.
If your computer hides the `.github` folder, use Option B just for `build.yml`.

**Option B (phone or no folder upload):** in the repository tap **Add file > Create new file**.
In the name box type the full path, for example `app/src/main/AndroidManifest.xml`
(typing `/` makes folders automatically). Paste the file's contents, tap **Commit changes**.
Repeat for every file in `ALL_FILES.md`. Do `.github/workflows/build.yml` last, because
saving it starts the build.

### 4. Run the build
Open the **Actions** tab. If asked, tap **I understand my workflows, enable them**.
The build starts by itself after each upload. To start it by hand: Actions >
**Build Echo debug APK** > **Run workflow**.

### 5. Is it successful?
A green tick means success (first build takes about 3 to 6 minutes). A red cross means failure.

### 6. Download the APK
Tap the finished run, scroll to **Artifacts**, tap **Echo-debug-apk**. You get a ZIP;
unzip it to get `app-debug.apk`. (You must be logged in to GitHub to download.)

### 7. Install on your phone
Copy `app-debug.apk` to the phone (or download it there directly), tap it, and allow
"Install unknown apps" for your browser or file manager when asked. Then install.
Only install APKs you built yourself in your own repository. Turn "Install unknown apps"
off again afterwards. Play Protect may warn about an unknown app; debug builds are unsigned
by a store, so that is normal.

## Troubleshooting

- **No Actions tab / nothing runs:** make sure `.github/workflows/build.yml` exists with exactly
  that path (a dot at the start of `.github`), and that you committed to the `main` branch.
- **"Could not find or load main class" / "Directory does not contain a Gradle build":**
  files were uploaded into an extra folder. `settings.gradle.kts` must be at the TOP of the repository.
- **"Unresolved reference: R" or resource errors:** a file is in the wrong folder or has a typo
  in its name. Compare your repository with the list above.
- **A file shows `<` `>` as `&lt;` `&gt;`:** you pasted from a web page that changed the text.
  Use the downloaded files instead.
- **Build fails on SDK / licence messages:** press Re-run jobs; if it repeats, copy the red error
  lines and ask for help.
- **Mic says unavailable:** expected on Android 12 or older, or without an offline speech pack.
- **Speak says no offline voice:** install a voice in Settings > text-to-speech output.
