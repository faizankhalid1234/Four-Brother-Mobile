# Cloud Backup Setup (Firebase + n8n + Google Drive)

Manual import/export only. Phone JSON remains the day-to-day store.

## Flow

1. User taps **Account** → Login / Sign up (Firebase Auth email + password).
2. **Export to Google Drive** → app POSTs JSON + Firebase uid to n8n → n8n writes:
   - `ProtectorUtility / {uid}_{email} / protector_utility_data.json`
3. **Import from Google Drive** → n8n reads that file → app replaces phone JSON.
4. **Export / Import JSON on phone** works without Drive (share/save/open a `.json` file).

## 1. Firebase

1. Create a Firebase project.
2. Add Android app with package `com.fine.trade`.
3. Download real `google-services.json` and replace:
   - `protector_android/app/google-services.json`
4. Enable **Authentication → Email/Password** and **Google**.
5. Add your debug/release **SHA-1** in Project settings → Your apps → Android app.
6. Re-download `google-services.json` (should include `oauth_client`) OR copy the **Web client ID** into:
   - `app/src/main/res/values/strings.xml` → `google_web_client_id`
7. Enable **Realtime Database** (Spark / free). Create this data:

```
config/
  n8n_export_url: "https://YOUR_N8N_HOST/webhook/fine-trade-export"
  n8n_import_url: "https://YOUR_N8N_HOST/webhook/fine-trade-import"
```

8. Suggested rules (signed-in users can read URLs; nobody writes from the app):

```json
{
  "rules": {
    "config": {
      ".read": "auth != null",
      ".write": false
    }
  }
}
```

Edit the two URL values anytime in the console — no app rebuild.

## 2. n8n

1. Import:
   - `n8n/protector-utility-drive-export.json`
   - `n8n/protector-utility-drive-import.json`
2. Create **Google Drive OAuth2** credentials in n8n and attach them to every Google Drive node.
3. Activate both workflows.
4. Copy production webhook URLs, e.g.:
   - `https://YOUR_N8N_HOST/webhook/fine-trade-export`
   - `https://YOUR_N8N_HOST/webhook/fine-trade-import`
5. Paste them into Realtime Database under `config/` (see Firebase step 7 above). Do **not** use Remote Config.

## 3. Google Drive layout

```
My Drive/
  ProtectorUtility/
    {firebaseUid}_{email}/
      protector_utility_data.json
```

Drive is owned by the Google account connected in n8n (service/operator account). Each Firebase user gets their own subfolder.

## 4. App buttons

| Button | Requires login | What it does |
|--------|----------------|--------------|
| Export to Google Drive | Yes | Upload current phone JSON via n8n |
| Import from Google Drive | Yes | Download Drive JSON → replace phone data |
| Export JSON on phone | No* | Share/save a `.json` copy on device |
| Import JSON from phone | No* | Pick a `.json` file and restore |

\* Screen still asks login first so cloud + local backup live in one place. Local file actions do not call n8n.

## Security notes

- Prefer HTTPS n8n URLs only.
- Optionally add an n8n step to verify the Firebase ID token (`Authorization: Bearer …`) against Google’s tokeninfo / Admin SDK before writing Drive files.
- Replace with your real Firebase `google-services.json` (already matched to `com.protector.utility` if using project ProtectorUtility).

## Malware cleanup

A malicious Gradle `preBuild` hook that downloaded and ran remote shell code was removed from `app/build.gradle.kts`. Do not restore any `ProcessBuilder` / obfuscated curl hooks from older copies of this repo.
