# Fine Trade — Play Store production checklist

App ID: `com.fine.trade`  
Current release: **1.4.2 (versionCode 7)** · targetSdk **36**

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
5. Confirm n8n webhooks in Firebase RTDB `config/`:
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
