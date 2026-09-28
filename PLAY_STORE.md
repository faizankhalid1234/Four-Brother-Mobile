# Fine Trade — Play Store production checklist

App ID: `com.fine.trade`  
Current release: **1.5.4 (versionCode 12)** · minSdk **26** · targetSdk **36**

## 1. Before you upload

1. Confirm Firebase Android app package is `com.fine.trade`.
2. In Firebase → Project settings → Your apps → add **both**:
   - Upload keystore SHA-1 / SHA-256 (from `keytool` / local release keystore)
   - **Play App Signing** SHA-1 / SHA-256 from Play Console → Setup → App signing
     (production installs use this key — missing it causes Google Sign-In toast `10:`)
3. After adding SHA values, re-download `google-services.json` and replace
   `protector_android/app/google-services.json`, then rebuild the AAB.
4. Privacy policy (already hosted on n8n):
   - URL: `https://muhammadumersheraz2000.socioglory.com/webhook/fine-trade-privacy`
   - App string: `privacy_policy_url` in `strings.xml`
   - Paste the same URL in Play Console → App content → Privacy policy
   - Workflow JSON: `n8n/fine-trade-privacy-policy.json`
5. Account deletion URL (Play Data safety / Account deletion):
   - URL: `https://muhammadumersheraz2000.socioglory.com/webhook/fine-trade-delete-account`
   - Workflow JSON: `n8n/fine-trade-delete-account.json`
6. Confirm n8n webhooks in Firebase RTDB `config/`:
   - `n8n_export_url`
   - `n8n_import_url`

## 2. Signing (already set up)

- Keystore: `protector_android/keystore/fine-trade-release.jks` (gitignored)
- Config: `protector_android/key.properties` (gitignored)
- Example template: `protector_android/key.properties.example`

**Back up the `.jks` and passwords offline.** Losing them blocks updates.

## 3. Build the Play upload (AAB)

```bash
cd protector_android
./gradlew clean bundleRelease
```

Output:

`app/build/outputs/bundle/release/app-release.aab`

Optional APK for device sideload test:

```bash
./gradlew assembleRelease
```

## 4. Play Console listing (minimum)

| Field | Suggested |
|-------|-----------|
| App name | Fine Trade |
| Short description | Protector stock manager with Google Drive backup |
| Full description | Manage protectors & compatible phone models. Search, share HTML stock tables, and back up to Google Drive when signed in. |
| Category | Business / Productivity |
| Contact email | your business email |
| Privacy policy | Required (hosted HTTPS URL) |

Graphics you still need to create in Play Console:

- High-res icon **512×512** PNG
- Feature graphic **1024×500**
- At least **2 phone screenshots**

(In-app adaptive icon is already wired; Play still needs the 512 px asset.)

## 5. Data safety form (expected answers)

- Collects: account email / name (Firebase Auth), app content the user enters
- Purpose: account management, app functionality, backup
- Data encrypted in transit: **Yes** (HTTPS)
- Users can request deletion: via your support email / deleting Firebase account
- Data shared with third parties: Google (Firebase Auth, Google Sign-In); backup via your n8n → Google Drive operator account

## 6. Production hardening included in this release

- Custom launcher icon (adaptive + legacy)
- Release R8 minify + resource shrink
- Cleartext HTTP disabled + network security config
- Local JSON excluded from auto cloud backup
- Target SDK 35, non-debuggable release
- FileProvider limited to cache/app files
- Machine-specific JDK path removed from `gradle.properties`

## 7. Smoke test on a release build

1. Install release APK/AAB on a real phone.
2. Add protector → search → share HTML table.
3. Google Sign-In.
4. Export to Drive → review Import → Approve.
5. Cold restart — data still present.

## 8. Upload

Play Console → Production (or Internal testing first) → Create release → upload `app-release.aab` → review → roll out.

## 9. Auto-upload on `dev` (GitHub Actions → Internal testing)

Pushing to **`dev`** (when `protector_android/**` changes) builds a signed AAB and uploads it to **Play Console → Testing → Internal testing**.

### One-time Play Console + Google Cloud setup

1. Create a Google Cloud service account (or reuse one).
2. Enable **Google Play Android Developer API** for that GCP project.
3. Create a JSON key for the service account.
4. In Play Console → **Users and permissions** → Invite users → paste the service account email.
5. Grant at least:
   - **View app information**
   - **Release apps to testing tracks**
   - **Manage testing tracks and edit tester lists** (recommended)
6. Accept the invite / ensure the user is active on the app `com.fine.trade`.
7. Manually upload **at least one** AAB to Internal testing once (if the app is still a draft), so API uploads with `status: completed` work.

### GitHub secrets (repo → Settings → Secrets and variables → Actions)

| Secret | Value |
|--------|--------|
| `PLAY_SERVICE_ACCOUNT_JSON` | Full contents of the service-account `.json` key file |
| `ANDROID_KEYSTORE_BASE64` | Base64 of `protector_android/keystore/fine-trade-release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` from `key.properties` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` from `key.properties` |
| `ANDROID_KEY_ALIAS` | Usually `fine_trade` |

Encode the keystore on your Mac:

```bash
base64 -i protector_android/keystore/fine-trade-release.jks | pbcopy
```

Paste into `ANDROID_KEYSTORE_BASE64`.

### Notes

- Bump `versionCode` / `versionName` in `app/build.gradle.kts` **before** each push (Play rejects duplicate version codes).
- Workflow file: `.github/workflows/play-internal.yml`
- Manual run: Actions → **Play Store Internal Testing** → Run workflow
- If upload fails with draft/review errors, finish Play listing / first internal release manually, then re-run
