# ProtectorUtility

Mobile protector stock management app for Android.

## App

- **Brand:** ProtectorUtility
- **Package:** `com.protector.utility`
- **Active project:** `protector_android` (native Kotlin / Material 3)

## Features

- Add / edit / delete protectors
- Mobile model master list with autocomplete
- Home search with filter chips (Both / Protector / Mobile model)
- Styled search suggestions
- Share designed protector card image + caption (WhatsApp, Instagram, Facebook, etc.)
- Local JSON storage on phone (`protector_utility_data.json`)
- Firebase login/signup + manual Google Drive import/export via n8n (see `CLOUD_BACKUP.md`)

## Build

```bash
cd protector_android
./gradlew assembleDebug
```

## Note

`protector_app` contains an earlier Flutter prototype. The shipping app is **protector_android**.

Private machine/security notes are kept locally in `RAKHNA.md` (not published).
