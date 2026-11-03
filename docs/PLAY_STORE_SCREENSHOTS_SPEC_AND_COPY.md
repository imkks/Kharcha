# Google Play Phone Screenshots: Requirements, Copy & Visual Blueprint

Google Play requires **at least 2** phone screenshots (maximum 8). For personal finance and expense tracker apps, a **5 to 6 screenshot narrative** yields the highest store conversion rate.

---

## 1. Google Play Screenshot Specifications

| Parameter | Specification |
| :--- | :--- |
| **Minimum Count** | 2 screenshots |
| **Recommended Count** | 5 to 6 screenshots |
| **Maximum Count** | 8 screenshots |
| **Format** | **JPEG** or **24-bit PNG** (no alpha channel / no transparency) |
| **Aspect Ratio** | 9:16 or 1:2 (standard phone portrait) |
| **Recommended Resolutions** | • **`1080 x 2400 px`** (20:9 — standard modern Android)<br>• **`1080 x 2340 px`** (19.5:9)<br>• **`1080 x 1920 px`** (16:9) |
| **File Size Limit** | Up to 8 MB per image |

---

## 2. The 5-to-6 Screenshot Narrative Blueprint

Instead of uploading plain raw app screenshots, high-converting apps use a **Headline Banner + Framed Phone Screen** layout.

```
+──────────────────────────────────────────────+
| [Headline Banner]                            |
| EFFORTLESS EXPENSE TRACKING                  |
| Smart dashboard with monthly budget & pacing |
|                                              |
|            ┌────────────────────┐            |
|            │   Phone Mockup     │            |
|            │   with real UI     │            |
|            │                    │            |
|            │   [Dashboard]      │            |
|            │   ₹32,450 Spent    │            |
|            │   Food, Shopping   │            |
|            │                    │            |
|            └────────────────────┘            |
+──────────────────────────────────────────────+
```

---

### Screenshot 1: The Hook — Dashboard & Monthly Spend
* **Goal:** Capture immediate attention with a clean, modern Material 3 interface.
* **Top Headline:** `Effortless Expense Tracking`
* **Subheading:** `Clean dashboard with monthly spend & daily safe-to-spend pacing`
* **App Screen to Capture:**
  * The **Home Screen** displaying:
    * Total monthly spend card (e.g. `₹32,450`).
    * Category breakdown donut/bar chart.
    * Recent bank transactions list with logos/icons.

---

### Screenshot 2: Core Feature — Automated SMS Parsing
* **Goal:** Solve the biggest pain point in expense apps: manual logging fatigue.
* **Top Headline:** `Zero Manual Entry`
* **Subheading:** `Instantly logs transactions from Indian bank SMS & UPI alerts`
* **App Screen to Capture:**
  * The **Transactions Tab** or an expanded **Transaction Detail Card**:
    * Showing incoming bank SMS (e.g. HDFC / SBI / ICICI / Axis).
    * Auto-extracted merchant name (e.g., *Swiggy*, *Amazon*, *Zomato*).
    * Transaction amount, timestamp, and account tag (`A/c ...1234`).

---

### Screenshot 3: The Privacy Superpower — 100% Offline
* **Goal:** Build trust. Contrast Kharcha with apps that upload bank messages to cloud servers.
* **Top Headline:** `100% Offline • Complete Privacy`
* **Subheading:** `Zero internet permission. Your financial data never leaves your device.`
* **App Screen to Capture:**
  * The **Onboarding Permission Screen** or **More / Settings Screen**:
    * Displaying the *"100% Offline • Zero Internet • On-Device Encrypted"* trust badge.
    * Highlighting local SQLCipher 256-bit AES encryption.

---

### Screenshot 4: Multi-Account & Credit Card Management
* **Goal:** Show that Kharcha handles complex finances (multiple accounts, cards, UPI).
* **Top Headline:** `All Accounts In One Place`
* **Subheading:** `Auto-groups transactions across savings accounts, credit cards & wallets`
* **App Screen to Capture:**
  * The **Accounts Screen** (or Accounts filter sheet):
    * Showing multiple accounts (e.g., *HDFC Bank Savings*, *SBI SimplyCLICK Card*, *ICICI Salary Account*).
    * Account balances and monthly debit/credit totals.

---

### Screenshot 5: Visual Insights & Category Budgets
* **Goal:** Demonstrate actionable analytics and financial control.
* **Top Headline:** `Smart Category Budgets & Insights`
* **Subheading:** `Set spending limits and visualize where your money goes`
* **App Screen to Capture:**
  * The **Insights / Analytics Screen**:
    * Monthly category distribution (Food & Dining, Groceries, Shopping, Fuel, Bills).
    * Budget progress bar (e.g., *72% of ₹40,000 budget used*).

---

### Screenshot 6: Search, Filters & Data Export (Optional but Recommended)
* **Goal:** Appeal to power users who want flexibility and full ownership of their data.
* **Top Headline:** `Powerful Search & CSV Export`
* **Subheading:** `Filter by merchant, bank or tag — export your ledger anytime`
* **App Screen to Capture:**
  * The **Search Screen** with active filter chips (e.g. `UPI`, `> ₹1,000`, `This Month`), or the **Export CSV** dialog.

---

## 3. How to Capture & Assemble Screenshots Quickly

### Step 1: Capture Raw Screenshots from Android Device or Emulator
* **On Physical Phone:** Press **Power + Volume Down** on each screen in the app.
* **In Android Studio Emulator:** Click the **Camera icon (Screenshot)** on the emulator toolbar (or `Cmd + S` / `Ctrl + S`).

### Step 2: Assemble into Framed App Store Screenshots
Use any of these free/quick online tools or templates:
1. **AppMockup (app-mockup.com):**
   * Free, no watermark.
   * Select Android device preset (`1080 x 2400`).
   * Choose dark theme background (`#0B1315` or `#0F1E22`).
   * Paste the headlines from Section 2 above and upload your raw app screenshots.
2. **Canva (canva.com):**
   * Create custom size: `1080 x 2400 px`.
   * Add text at the top (32–42px font, bold white and neon mint `#00D09C`).
   * Insert the phone frame (under Elements > Frames > Phone) and drop your screenshot inside.
3. **Figma / Previewed.app:**
   * Use community "Google Play Screenshot Template" file.

---

## 4. Google Play Policy Rules for Screenshots

* ✅ **Do:** Ensure all mock data in screenshots looks realistic (e.g. realistic Indian rupee amounts like ₹450, ₹1,299).
* ❌ **Do NOT:** Show real bank account numbers (keep it masked like `A/c **1234`).
* ❌ **Do NOT:** Include sensitive personal customer data or real phone numbers in SMS bodies.
* ❌ **Do NOT:** Include star ratings, awards, or download claims (*"5 Stars"*, *"Best App"*).
