# Google Play Console: App Content & Internal Testing Answers

This document provides exact, click-by-click answers for every question Google Play Console asks before launching **Internal Testing** and completing the **App Content** policy declarations for **Kharcha** (`com.evertrail.kharcha`).

---

## Part 1: "App Content" Policy Declarations (Step-by-Step)

In Google Play Console, go to the left sidebar $\rightarrow$ **Policy and programs** $\rightarrow$ **App content**. Complete each of the declaration tasks below:

---

### 1. Privacy Policy
* **Prompt:** *Enter your privacy policy URL*
* **Answer:** Paste your published Notion web link (e.g. `https://evertrail.notion.site/Kharcha-Privacy-Policy-...`).
* **Note:** The template for this page is in [`docs/NOTION_PRIVACY_POLICY_TEMPLATE.md`](file:///Users/krish/Developer/Kharcha/docs/NOTION_PRIVACY_POLICY_TEMPLATE.md).

---

### 2. App Access
* **Prompt:** *Can users access all parts of your app without credentials?*
* **Selection:** **All functionality is available without special access**
* **Reason:** Kharcha has no login, password, OTP server sign-in, or paywall. Everything works offline upon installation.

---

### 3. Ads
* **Prompt:** *Does your app contain ads?*
* **Selection:** **No, my app does not contain ads**

---

### 4. Content Ratings (IARC Questionnaire)
Click **Start questionnaire** and answer the prompts:
* **Category:** Utility, Productivity, Communication, or Other (Select **Utility / Productivity**)
* **Violence:** No
* **Sexuality:** No
* **Language:** No
* **Controlled Substance:** No
* **Promotion of Age-Restricted Products (Alcohol/Tobacco):** No
* **Does the app allow users to interact or exchange content through voice, text, or sharing photos?** $\rightarrow$ **No**
* **Does the app share the user's current physical location with other users?** $\rightarrow$ **No**
* **Does the app allow users to purchase digital goods?** $\rightarrow$ **No**
* **Summary result:** Rating will be **Everyone** (PEGI 3, USK 0, ESRB Everyone). Click **Save** $\rightarrow$ **Next** $\rightarrow$ **Submit**.

---

### 5. Target Audience and Content
* **Target age groups:** Select **18 and over** only.
* **Could your store listing unintentionally appeal to children?** $\rightarrow$ Select **No**.
* **Reason:** Selecting 18+ exempts the app from Google’s strict "Designed for Families" policy requirements.

---

### 6. News Apps
* **Prompt:** *Is your app a news app?*
* **Selection:** **No**

---

### 7. COVID-19 Contact Tracing and Status Apps
* **Selection:** **My app is not a publicly available COVID-19 contact tracing or status app**

---

### 8. Data Safety (Most Critical Section)

Because Kharcha contains **zero internet permission** (`android.permission.INTERNET` is not declared in `AndroidManifest.xml`) and **all data remains on the device**:

#### Page 1: Data Collection and Security
* **Does your app collect or share any of the required user data types?**
  * Select: **No**
  * *(Google's official definition: "Collect" means transmitting data off the user's device. Since Kharcha processes everything 100% on-device and has no internet capability, answer **No**).*
* **Is all of the user data collected by your app encrypted in transit?**
  * Select: **N/A** (or greyed out because no data is collected off-device).
* **Do you provide a way for users to request that their data be deleted?**
  * Select: **Yes**
* **Explain how users can request data deletion:**
  * Enter URL or explanation: *"Users can permanently delete all local data, transactions, and accounts at any time inside the app by going to More > Clear Data, or by uninstalling the app."*

*(If prompted to declare specific data types because of permissions, declare that Financial Data and SMS are strictly processed on-device and NOT collected/transmitted off device).*

---

### 9. Government Apps
* **Prompt:** *Is your app developed by or on behalf of a government?*
* **Selection:** **No**

---

### 10. Financial Features Declaration
* **Prompt:** *Does your app provide financial features?*
* **Selection:** **Yes**
* **What type of financial features does your app provide?**
  * Select: **Personal finance management (PFM)** / **Expense tracking**
  * **Important:** Ensure you do **NOT** select "Personal loans", "Banking services", "Cryptocurrency exchange", or "Investment advice".
* **Do you provide personal loans?** $\rightarrow$ **No**

---

### 11. SMS and Call Log Permissions Declaration Form (Mandatory for `READ_SMS` & `RECEIVE_SMS`)

Google restricts SMS permissions to apps that declare an approved core use case.

* **Select Core Functionality:**
  * Choose: **Personal Financial Management (PFM) - SMS-based transactional financial management**
* **Provide an explanation of why your app requires these permissions:**
  > *"Kharcha is an offline personal financial management app designed specifically for Indian users. It reads incoming and existing bank transaction alerts (from Indian banks such as HDFC, SBI, ICICI, Axis, Kotak, and UPI providers) to automatically log debit and credit expenses without requiring manual data entry. Kharcha operates 100% offline with zero internet permission, meaning financial SMS content physically cannot leave the user's device."*
* **Prominent In-App Disclosure:**
  * Confirm that Kharcha displays an upfront explanation dialog before prompting the user with Android's system SMS permission dialog.
* **Demonstration Video Link (YouTube unlisted link):**
  * Record a short (1 to 2 minute) screen recording and upload to YouTube as **Unlisted**.
  * The video must show:
    1. App opening to the Onboarding / Permission disclosure screen.
    2. The dialog explaining why SMS access is needed.
    3. Granting the system SMS runtime permission.
    4. An SMS transactional alert being parsed into a transaction in the ledger.
    5. Navigating to device App Info / Settings showing that Kharcha has no network/internet permissions.

---

## Part 2: "Store Settings" (Category, Tags, Contact & Marketing)

In Google Play Console, go to the left sidebar $\rightarrow$ **Grow users** (or **Store presence**) $\rightarrow$ **Store settings**.

---

### 1. App Category
* **App or Game:** Select **App**
* **Category:** Select **Finance**
* **Tags (Click "Manage tags"):** Select up to 5 relevant tags:
  1. **Finance**
  2. **Personal finance**
  3. **Productivity**
  4. **Budgeting** (or **Accounting**)
  5. **Utilities**
  *(Click **Apply**)*

---

### 2. Store Listing Contact Details
These contact details are displayed publicly on your Google Play Store listing so users can reach out for support:

* **Email address (Mandatory):** Enter your public support email (e.g. `support@evertrail.com` or your developer contact email).
* **Phone number (Optional):** Leave blank (recommended to avoid unsolicited calls), or enter with country code if you maintain a support hotline.
* **Website (Optional):** Enter your website or Notion landing page URL (e.g. `https://evertrail.com` or your published Notion Privacy Policy domain).

---

### 3. External Marketing
* **Prompt:** *Advertise my app outside Google Play*
* **Selection:** **Leave checked (Yes)**
* **Reason:** This allows Google to showcase Kharcha in search engine results, Google ad networks, and Android feature roundups at zero cost to you.

---

### 4. Main Store Listing Summary (Assets & Descriptions)
Navigate to **Store presence** $\rightarrow$ **Main store listing**:

* **App name:** `Kharcha` (Max 30 characters)
* **Short description (Max 80 characters):**
  > `Privacy-first offline expense tracker that automatically logs bank SMS.`
* **Full description (Max 4000 characters):** Use the complete copy template from [Section 3.2 of the Release Guide](file:///Users/krish/Developer/Kharcha/docs/PLAY_STORE_RELEASE_GUIDE.md#32-store-listing-copy).
* **Graphics:**
  * **App icon:** `512 x 512 px`, PNG with alpha (Max 1024 KB).
  * **Feature graphic:** `1024 x 500 px`, JPG/PNG without transparency (Max 15 MB).
  * **Phone screenshots:** Upload at least 2 screenshots (recommended 4–8 in `1080 x 2400 px` or `1080 x 1920 px`).

---

## Part 3: Setting Up the "Internal Testing" Track

In the left sidebar, navigate to **Testing** $\rightarrow$ **Internal testing**.

---

### Step 1: Manage Testers (Testers Tab)
1. Click the **Testers** tab at the top.
2. Under **Email lists**, click **Create email list**.
   * **List name:** `Kharcha Internal Testers`
   * **Add email addresses:** Enter your own Gmail address and any colleagues/friends who will test on their physical Android devices.
   * Click **Save changes**.
3. Under **Feedback URL or email address**, enter your support email (e.g. `support@evertrail.com`).
4. Save the page.

---

### Step 2: Create the Release (Releases Tab)
1. Click the **Releases** tab $\rightarrow$ click **Create new release** (top-right).
2. **App bundles:**
   * Click **Upload** and choose:
     ```
     app/build/outputs/bundle/release/app-release.aab
     ```
   * Wait until the upload shows a green checkmark indicating:
     * App name: `Kharcha`
     * Package name: `com.evertrail.kharcha`
     * Version: `1.0.0 (1)`
3. **Release name:**
   * Enter: `1.0.0 (1) - Internal Test Build`
4. **Release notes:**
   * Enter:
     ```text
     Initial internal test build of Kharcha:
     - 100% offline personal finance tracking with zero internet permission.
     - Automatic on-device bank transaction logging from SMS.
     - SQLCipher encrypted local database.
     - Material 3 dashboard, analytics, and category management.
     ```
5. Click **Next** / **Save and review release**.
6. Click **Start rollout to Internal testing**.

> **Note on Review Time:** Internal testing does **NOT** require Google policy review. The release is published immediately within 5–15 minutes.

---

### Step 3: How to Download & Test on Physical Devices

Once rolled out:
1. Return to the **Testers** tab under **Internal testing**.
2. Scroll to the bottom to **How testers join your test**.
3. Click **Copy link** next to **Join on the web** or **Join on Android** (the URL looks like `https://play.google.com/apps/testing/com.evertrail.kharcha`).
4. Send this link to your test device (make sure the device is logged into one of the Google accounts you added to the email list).
5. Open the link on the test device $\rightarrow$ click **"Become a tester"** / **"Accept Invite"**.
6. Click **"Download it on Google Play"**.
7. Install the app directly from the Google Play Store!
