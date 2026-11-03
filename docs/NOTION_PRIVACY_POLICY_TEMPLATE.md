# Privacy Policy for Kharcha

**Last updated:** November 3, 2026  
**Developer:** Evertrail  
**App Name:** Kharcha — Personal Expense Tracker  
**Package Name:** `com.evertrail.kharcha`  
**Contact:** support@evertrail.com (or your contact email)  

---

> **Privacy at a Glance:**  
> Kharcha is built with an absolute offline-first, privacy-by-design architecture. **The app has zero internet permission (`android.permission.INTERNET` is omitted from the application manifest)**. Your financial data, messages, accounts, and budgets **physically cannot leave your device**. We do not collect, transmit, sell, or analyze your personal information.

---

## 1. Introduction

Evertrail ("we", "our", or "us") develops **Kharcha** as a privacy-focused, offline personal financial management application for Android. 

This Privacy Policy explains how Kharcha handles your data, what device permissions the app requests, and how we protect your privacy when you use our application.

By installing and using Kharcha, you acknowledge and agree to the data handling practices described in this policy.

---

## 2. Zero Network Transmission (100% Offline Architecture)

Kharcha is designed to operate completely independently of any cloud infrastructure, remote server, or third-party web service.

- **No Internet Permission:** Kharcha does not request Android’s `android.permission.INTERNET`. The operating system prevents the application from making any network requests or establishing outbound connections.
- **No Remote Servers:** We do not own, maintain, or connect to any remote server or database that receives your data.
- **No Third-Party Analytics or SDKs:** Kharcha contains zero tracking SDKs, zero advertising networks, zero diagnostic trackers, and zero telemetry services.

---

## 3. Information Handled & Device Permissions

To deliver automated personal finance management, Kharcha requests access to specific device hardware and system features. All data accessed remains strictly inside the app's sandboxed local storage on your device.

### 3.1 SMS Permissions (`READ_SMS` & `RECEIVE_SMS`)
- **Purpose:** Kharcha uses SMS access solely to automatically detect and log financial transactions (debits, credits, UPI payments, and account balances) sent by Indian banks and financial institutions.
- **On-Device Parsing Only:** Incoming and historical SMS messages are parsed entirely on your device using local regex and template-matching engines.
- **Ignored Messages & OTP Protection:** 
  - Non-transactional SMS messages (personal messages from friends/family, marketing spam, operator notices) are filtered and discarded.
  - **One-Time Passwords (OTPs)**, authentication codes, and security credentials are systematically detected and rejected. They are never recorded, saved, or included in any database entry.
- **No Cloud Upload:** Neither the raw text of your SMS messages nor parsed financial transactions are ever transmitted off your phone.

### 3.2 Notification Permission (`POST_NOTIFICATIONS`)
- **Purpose:** Required on Android 13 (API level 33) and above to display local system notifications.
- **Usage:** Kharcha posts notifications strictly to alert you when a transaction has been automatically detected, or when a transaction requires category review. These notifications are generated locally on your phone and do not use push messaging servers (such as Firebase Cloud Messaging).

### 3.3 Location Permissions (`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`) (Optional)
- **Purpose:** If enabled by the user, Kharcha can optionally capture your geographic coordinates at the time a transaction is created or recorded to help you remember where a purchase was made.
- **Usage:** Location data is attached solely as local metadata to the transaction entry stored in your on-device database. It is never transmitted off your device.

---

## 4. Data Storage and Security

- **On-Device Storage:** All financial ledgers, account balances, transaction records, custom categories, and user preferences are stored in a local SQLite database powered by Android Room.
- **Database Encryption:** User data is encrypted at rest using **SQLCipher (256-bit AES encryption)**, with encryption keys securely handled via the Android Keystore system.
- **Local Device Isolation:** Android’s application sandbox ensures that no other installed application on your device can access Kharcha's private database files.

---

## 5. Third-Party Sharing and Disclosure

Because Kharcha operates completely offline without internet connectivity:
- We **do not sell, trade, rent, or share** your personal or financial data with any third party, financial institution, government body, or advertiser.
- We do not use third-party cloud analytics (e.g. Google Analytics, Firebase, Mixpanel).
- We do not embed third-party advertising SDKs (e.g. Google AdMob, Unity, AppLovin).

---

## 6. Data Retention and Deletion (Your Rights)

You have absolute control over all data stored within Kharcha:

1. **In-App Data Purge:** You can permanently delete all transactions, accounts, rules, and categories at any time from within the app:
   - Go to **More / Settings > Clear Data / Reset Database**.
2. **App Uninstallation:** Uninstalling Kharcha from your device immediately and permanently deletes the local SQLite/SQLCipher database and all associated application cache and settings files.
3. **Data Export:** You can export your data at any time to a local CSV or JSON file directly on your device storage for your own records or offline backup.

---

## 7. Children's Privacy

Kharcha is designed for individuals aged 18 and older for the purpose of managing personal banking and debit/credit expenses. We do not knowingly collect or solicit information from anyone under the age of 18.

---

## 8. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in legal requirements or application features. Any updates will be published with a revised "Last updated" date at the top of this document. We encourage you to review this Privacy Policy periodically.

---

## 9. Contact Us

If you have any questions, concerns, or feedback regarding this Privacy Policy or Kharcha's data protection practices, please contact us at:

- **Developer / Publisher:** Evertrail
- **Email:** support@evertrail.com
- **Website:** https://evertrail.com (or your project URL)

---

### Instructions for Notion:
1. Create a new Notion page titled **"Kharcha — Privacy Policy"**.
2. Copy and paste the markdown text above into the page.
3. In the top-right corner of Notion, click **Share** $\rightarrow$ switch on **"Publish to web"**.
4. Copy the public link provided by Notion (e.g., `https://evertrail.notion.site/Kharcha-Privacy-Policy-...`).
5. Paste this URL into the **Privacy Policy** field under **Policy and programs > App content** in the Google Play Console.
