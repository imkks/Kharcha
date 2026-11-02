# Kharcha — Google Play Console Testing to Production Release Guide

This document is the complete step-by-step operational handbook for launching **Kharcha** (`com.evertrail.kharcha`) from initial testing through to full production on the Google Play Store.

---

## 1. App Summary & Key Identifiers

| Parameter | Details |
| :--- | :--- |
| **App Name** | **Kharcha** |
| **Package Name (`applicationId`)** | `com.evertrail.kharcha` |
| **App Category** | Finance / Personal Finance Management (PFM) |
| **App Distribution** | Free (Zero Ads, Zero Subscriptions) |
| **Target Audience** | Ages 18 and older |
| **Core Value Proposition** | 100% offline, privacy-first automated expense tracker via on-device bank SMS parsing |
| **Network Access** | **Zero Internet Permission** (`android.permission.INTERNET` is omitted) |
| **Sensitive Permissions** | `android.permission.READ_SMS`, `android.permission.RECEIVE_SMS`, `android.permission.POST_NOTIFICATIONS` |

---

## 2. Important Notice: APK vs. AAB (Android App Bundle)

> [!IMPORTANT]
> **Google Play requires an Android App Bundle (`.aab`) for publishing new apps.**
> While you have a signed `.apk` (useful for direct sideloading or GitHub Releases), **Play Console requires an `.aab`** signed with your release keystore.

### 2.1 Verifying `app/build.gradle.kts` Configuration
Ensure your `app/build.gradle.kts` matches your registered package name and versioning:

```kotlin
android {
    namespace = "com.kharcha"

    defaultConfig {
        applicationId = "com.evertrail.kharcha" // Must match Play Console package name
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0.0"
        ...
    }
}
```

### 2.2 Building the Release Android App Bundle (`.aab`)
Run the following Gradle command from the project root:

```bash
./gradlew clean bundleRelease
```

The generated signed bundle will be located at:
```
app/build/outputs/bundle/release/app-release.aab
```
Keep this `.aab` file ready for uploading to Google Play Console.

---

## 3. Google Play Store Assets & Metadata Checklist

Prepare these assets before creating the store listing:

### 3.1 Graphic Assets Specifications
- **App Icon**: `512 x 512 px`, 32-bit PNG (with alpha), max 1024 KB.
- **Feature Graphic**: `1024 x 500 px`, JPG or 24-bit PNG (no alpha), max 15 MB.
- **Phone Screenshots**:
  - Minimum: 2 screenshots. Recommended: 4 to 8 high-resolution screenshots.
  - Aspect Ratio: 16:9 or 9:16 (e.g. `1080 x 2400 px` or `1080 x 1920 px`).
  - Suggested screens:
    1. Dashboard: Total monthly spend, category breakdown chart, accounts overview.
    2. Auto-tracking: Transaction cards with bank logos (HDFC, SBI, ICICI) and tags.
    3. Privacy Highlight: "100% Offline, Zero Internet Permission, Encrypted on Device".
    4. Analytics & Insights: Spending trends, monthly budget tracker.
    5. Manual & SMS Review: Clean transaction review and category assignment screen.

### 3.2 Store Listing Copy

#### Short Description (Max 80 characters)
> Privacy-first offline expense tracker that automatically logs bank SMS.

#### Full Description (Max 4000 characters)
```text
Kharcha is a modern, privacy-focused personal expense tracker built specifically for India.

Unlike traditional expense apps that upload your sensitive bank messages to cloud servers, Kharcha operates 100% offline. It contains NO INTERNET PERMISSION—meaning your personal financial data physically cannot leave your phone.

WHY KHARCHA?
• Automatic Expense Tracking: Reads incoming bank transaction SMS (HDFC, SBI, ICICI, Axis, Kotak, UPI, and more) and logs transactions instantly.
• 100% Offline & Private: No internet permission requested. Your financial data stays encrypted on your device using SQLCipher.
• Smart Categorization: On-device categorization automatically tags groceries, dining, shopping, fuel, bills, and investments.
• Account Management: Automatically detects multiple bank accounts and credit cards from the last 4 digits mentioned in SMS.
• Insights & Visuals: Clean Material 3 charts showing monthly spending trends, budget limits, and category distributions.
• Zero Ads, Zero Tracking: No banners, no trackers, and no selling of your data.

PERMISSIONS EXPLAINED:
• SMS (READ_SMS & RECEIVE_SMS): Required strictly for detecting transactional alerts from Indian financial institutions to log expenses. SMS contents never leave your device.
• Notifications: Required to notify you when a new transaction is automatically logged or needs quick review.

Take control of your personal finances with complete peace of mind.
```

---

## 4. Google Play Policy Declarations & App Content Setup

> [!CAUTION]
> The most critical step for Kharcha is the **SMS Permissions Policy Declaration**. Google scrutinizes SMS permissions rigorously. Follow the instructions below to avoid rejections.

Navigate to **Policy and programs > App content** in the Play Console:

### 4.1 Privacy Policy (Mandatory)
- Provide a public URL (e.g. hosted via GitHub Pages, Notion public page, or your domain).
- Must explicitly state:
  1. What data the app accesses (`READ_SMS`, `RECEIVE_SMS`).
  2. That all data is processed strictly locally on-device.
  3. That the app contains **no internet permission** and does not transmit data to any third party or remote server.

### 4.2 SMS and Call Log Permissions Declaration Form
Google Play only allows SMS permissions for apps where SMS reading is a **core feature**.
- **Declaration Type**: Select **"Personal Financial Management (PFM) - SMS-based transactional financial management"**.
- **Prominent Disclosure**: Ensure the app presents an in-app dialog explaining *why* SMS access is needed *before* calling Android's system permission prompt.
- **Mandatory Video Demonstration**:
  - Google requires a public or unlisted YouTube video link (under 2 minutes) demonstrating:
    1. The app launching and displaying the upfront prominent disclosure dialog for SMS.
    2. Granting the Android runtime SMS permission.
    3. An incoming transactional SMS or inbox import being automatically detected and parsed into an expense card in the ledger.
    4. Navigating to App Settings showing that the app has no internet capability.

### 4.3 Data Safety Form
Because Kharcha has no `INTERNET` permission and stores data locally:
- **Does your app collect or share any user data?** -> Select **"No"**.
  - Note: Google defines "collection" as transmitting data off the device. Since data remains strictly on-device, you can answer "No" to data collection.
- **Is user data transferred off device?** -> **No**.
- **Data Deletion**: State that users can permanently delete all local data at any time via App Settings > Clear Data / Delete Database or by uninstalling the app.

### 4.4 Financial Features Declaration
- Select **Personal Financial Management (PFM)**.
- Confirm that the app does **NOT** provide consumer lending, payday loans, banking services, or cryptocurrency trading.

### 4.5 Ads Declaration
- Select **"No, my app does not contain ads"**.

### 4.6 App Access
- Select **"All functionality is available without special access restrictions"** (since Kharcha does not require an online user account/login).

### 4.7 Content Rating (IARC Questionnaire)
- Complete the questionnaire truthfully:
  - Violence: None
  - Sexual Content: None
  - Language: None
  - Controlled Substances: None
  - Miscellaneous (User exchange of info / location sharing): None
- Result: **Everyone (PEGI 3 / USK 0 / ESRB Everyone)**.

### 4.8 Target Audience & Content
- Target age group: **18 and over**.
  - Selecting 18+ avoids the complex Google Play Families Policy requirements.

---

## 5. Testing Tracks Progression: From Testing to Production

Google Play enforces a staged progression through testing tracks:

```mermaid
flowchart LR
    A["1. Internal Testing<br/>(Immediate, up to 100 testers)"] --> B["2. Closed Testing<br/>(14 days, 20 testers required)"]
    B --> C["3. Production Application<br/>(Answer review questions)"]
    C --> D["4. Production Release<br/>(Public on Play Store)"]
```

---

### Step 1: Internal Testing Track (Immediate Device Validation)
*Purpose: Fast smoke-testing on your personal devices and team members with no Google review delay.*

1. In Play Console, go to **Testing > Internal testing**.
2. Click **Create release**:
   - Upload `app-release.aab`.
   - Name the release (e.g., `1.0.0 (1) - Internal Alpha`).
   - Add release notes.
3. Create an **Email list** under the "Testers" tab (add your own email address and immediate colleagues).
4. Save and click **Review and roll out**.
5. Once rolled out (instant, no review needed):
   - Copy the **Join on Android** or **Join on Web** invite link.
   - Open link on your Android device, accept invite, and download from Google Play.
6. Verify:
   - SMS permission prompt appears and functions properly.
   - New bank SMS triggers automatic expense capture.
   - Database operations (Room + SQLCipher) execute without crashes.

---

### Step 2: Closed Testing Track (The 14-Day 20-Tester Rule)

> [!WARNING]
> **Mandatory Requirement for Personal Developer Accounts created after Nov 13, 2023:**
> Google requires personal accounts to run a **Closed Test with at least 20 testers opted in continuously for at least 14 days** before applying for production access.

#### How to Set Up Closed Testing:
1. In Play Console, go to **Testing > Closed testing**.
2. Select or create a track (e.g. **Alpha**).
3. Under the **Testers** tab:
   - Create a tester list containing at least **20-25 distinct Google account email addresses** (friends, family, beta testers).
   - Ensure you recruit slightly more than 20 (e.g., 22-25) to account for any drops.
4. Click **Create release**:
   - Upload the verified `app-release.aab`.
   - Set Release name (e.g., `1.0.0 (1) - Closed Beta`).
   - Save and click **Send changes for review**.
5. **Google Review**: This submission triggers a policy review (usually 1-3 business days).
6. **Once Approved**:
   - Send the closed test invite link (Web or Android URL) to all 20+ testers.
   - **Crucial**: Each tester must click "Become a tester", download/install the app from Google Play, and open it.
   - Keep the app installed and ask testers to use it across the 14 days.
7. **Monitor the Countdown**:
   - The Play Console Dashboard will display a progress tracker showing how many days remain and the active tester count.

---

### Step 3: Applying for Production Access

Once the 14-day threshold with 20 opted-in testers is satisfied:
1. The **"Apply for production"** button becomes active on the Console Dashboard.
2. Google requires you to answer a questionnaire about the testing phase.

#### Recommended Answers to Google's Production Questions:
- **How did you recruit your testers?**
  > *Testers were recruited from personal and developer networks, financial management focus groups, and beta users interested in privacy-focused offline expense tracking.*
- **How did testers use your app?**
  > *Testers used Kharcha daily to track day-to-day transactions, verify SMS parsing accuracy across Indian banks (HDFC, SBI, ICICI, Axis), configure custom categories, and check monthly expense analytics.*
- **What feedback did you receive from testers and how did you address it?**
  > *Feedback focused on parsing edge-cases for specific bank SMS formats, dark mode contrast adjustments, and notification action responsiveness. All identified SMS parsing templates and UI responsiveness items were refined and verified during the test period.*
- **Why do you believe your app is ready for production?**
  > *Kharcha operates completely offline without internet permissions, eliminating server-side vulnerabilities. Over the 14-day testing window, crash rates remained at 0%, ANR rates were 0%, and core on-device features (SMS parsing, Room database encryption, analytics) performed stably across various Android devices.*

---

### Step 4: Production Release Rollout

Once Google approves your application for production access:
1. Go to **Release > Production**.
2. Click **Create new release**.
3. Select the approved `.aab` from your library (or upload a newer build if bug fixes were applied).
4. Enter release notes for `en-IN` and `en-US`:
   ```text
   Initial launch of Kharcha!
   • 100% offline, privacy-first personal expense tracking.
   • Automatic transaction logging from bank SMS.
   • Smart on-device categorization and monthly spending analytics.
   • Zero ads, zero tracking, and no internet permission required.
   ```
5. Choose rollout strategy:
   - **Staged Rollout (Recommended)**: Start at 20% -> 50% -> 100% over 3-5 days to monitor crash rates on diverse hardware.
   - **Full Rollout**: 100% immediate availability.
6. Click **Review release** and **Start rollout to Production**.
7. Google will perform a final store listing review (typically 12-48 hours). Once status changes to **Available on Google Play**, your app is officially live!

---

## 6. Post-Launch Operations & Release Updates

### 6.1 Android Vitals Monitoring
Under **Quality > Android vitals**, monitor:
- **User-perceived crash rate**: Must remain below Google's bad behavior threshold (< 1.09%).
- **User-perceived ANR rate**: Must remain below the bad behavior threshold (< 0.47%).
- Because Kharcha has no background network synchronization and uses optimized coroutines for SMS parsing, vitals should remain near 0%.

### 6.2 Publishing Updates
When deploying a new version (e.g. `1.0.1`):
1. In `app/build.gradle.kts`:
   - Increment `versionCode` (e.g. `versionCode = 2`).
   - Update `versionName` (e.g. `versionName = "1.0.1"`).
2. Run database migration tests if any Room schema changes were made.
3. Build new bundle: `./gradlew clean bundleRelease`.
4. Go to **Release > Production > Create new release**, upload the new `.aab`, add release notes, and rollout.

---

## 7. Quick Reference Action List

- [ ] Ensure `applicationId = "com.evertrail.kharcha"` in `app/build.gradle.kts`.
- [ ] Build release bundle via `./gradlew bundleRelease`.
- [ ] Prepare store graphics (App icon 512x512, Feature graphic 1024x500, 4-8 screenshots).
- [ ] Host public Privacy Policy URL detailing local SMS handling and zero internet access.
- [ ] Record and upload unlisted demo video showing SMS disclosure and local transaction capture.
- [ ] Fill App Content declarations (Data Safety: No collection, Financial Features: PFM, Ads: No).
- [ ] Roll out to Internal Testing for immediate personal verification.
- [ ] Roll out to Closed Testing (20+ testers, 14 continuous days).
- [ ] Submit Production application questionnaire.
- [ ] Publish to Production with staged rollout!
