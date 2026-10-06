# RAKHNA — local security & machine notes (DO NOT PUBLISH)

This file stays only on this PC. It is listed in `.gitignore` and must never be committed or pushed to GitHub.

## Why this file exists

All security-sensitive, private, or machine-specific details live here in one place.
Public repo should only have app source code — never keys, tokens, device IDs, or local tool paths that you want kept private.

## Never publish / never commit

- Keystore / signing files: `*.jks`, `*.keystore`, passwords
- `.env`, API tokens, Firebase/Google services JSON with secrets
- ADB wireless pairing codes
- Phone serial / personal device IDs
- This file: `RAKHNA.md`
- Debug APKs with private stock data (user data is on-device SharedPreferences)
- Any screenshot of customer stock

## App identity (local reference)

- Brand / launcher name: **FOUR BROTHERS**
- Application ID: `com.fourbrothers.protector`
- Main activity: `com.fourbrothers.protector/.MainActivity`
- Local storage prefs: `protector_app` (legacy migrate-only)
- Phone data file: `four_brothers_data.json`
  - Internal: `filesDir/four_brothers_data.json` (primary, survives app updates)
  - Mirror: `Android/data/com.fourbrothers.protector/files/four_brothers_data.json`
- FileProvider authority: `com.fourbrothers.protector.fileprovider`
- Share cache folder: app `cacheDir/shares/` (PNG cards)
- Note: uninstalling the app deletes phone-stored entries (Android rule)

## This PC — tools (private)

- JDK: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`
- Android SDK: `C:\AndroidTools\sdk`
- Platform-tools / ADB: `C:\AndroidTools\platform-tools`
- External Gradle build dir (outside OneDrive locks): `C:\AndroidTools\builds\protector_android`
- Debug APK: `C:\AndroidTools\builds\protector_android\app\outputs\apk\debug\app-debug.apk`
- Desktop copy: `Desktop\mobile accesories\ProtectorApp.apk`

## Install / update on phone (ADB)

```powershell
$env:PATH = "C:\AndroidTools\platform-tools;$env:PATH"
adb devices -l
adb install -r "C:\AndroidTools\builds\protector_android\app\outputs\apk\debug\app-debug.apk"
adb shell am force-stop com.fourbrothers.protector
adb shell am start -n com.fourbrothers.protector/.MainActivity
```

If launcher still shows old name:

```powershell
adb uninstall com.fourbrothers.protector
adb install "C:\AndroidTools\builds\protector_android\app\outputs\apk\debug\app-debug.apk"
```

Note: uninstall **deletes** local protector data on that phone.

## Security practices for this app

1. Data is local-only (SharedPreferences). No cloud sync unless you add it later.
2. Share uses FileProvider — only grant temporary read URI to the chosen app.
3. Do not enable `allowBackup` for production release if stock data must not leave the device via USB backup (currently `allowBackup=true` in debug; turn off for Play Store release if needed).
4. Use a release keystore for publishing; keep keystore + passwords only in this file / password manager — never in Git.
5. GitHub repo: https://github.com/faizankhalid1234/Four-Brother-Mobile — public. Assume anything pushed is visible to everyone.

## Release checklist (before publishing APK elsewhere)

- [ ] `allowBackup` decision
- [ ] Release signing keystore created (store path + alias noted below)
- [ ] `versionCode` / `versionName` bumped
- [ ] No `RAKHNA.md`, keystores, or `.env` in the commit
- [ ] Test Share + search on a real phone

### Release keystore (fill when you create it — still never commit the file)

- Path:
- Alias:
- Store password: (password manager only)
- Key password: (password manager only)
