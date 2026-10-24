# Kharcha UI Screen Layouts & Wireframes Specification

**Version:** 1.0  
**Companion Documents:** `docs/UI_REDESIGN_PLAN.md`, `docs/UI_ADVANCED_FILTERS_SPEC.md`, `docs/UI_SPEC.md`  
**Platform:** Android Jetpack Compose, Material 3 (Tonal elevation, MaterialTheme tokens)  

---

## 1. Information Architecture & Navigation

The app uses a 4-destination bottom navigation bar with a single centered FAB:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               App Shell                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                            [ Dynamic Screen Body ]                          │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                         [ + Floating Action Button ]                        │
│   [ 🏠 Home ]      [ 💳 Transactions ]      [ 📊 Insights ]     [ ⚙ More ]  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Screen Wireframes & Component Trees

### 2.1 Screen 1: Home Dashboard (`feature:home`)

#### Populated State Wireframe:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Kharcha                                                     [ ◀ Oct 2026 ▶ ]│
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  Net Savings                                                            │ │
│ │  ₹ 42,500.00                                               (50% rate)   │ │
│ │                                                                         │ │
│ │    ↑ Income (Credits)                        ↓ Expenses (Debits)        │ │
│ │    +₹ 85,000.00                              -₹ 42,500.00               │ │
│ │    ▲ 12% vs last month                       ▼ 8% vs last month         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  🛡️ Safe to Spend                                           On Track   │ │
│ │  ₹ 1,420.00 / day                          9 days remaining in month   │ │
│ │  ████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  67% of budget│ │
│ │  Spent: ₹ 42,500.00                   Budget: ₹ 60,000.00               │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  ⚠️  12 unreviewed transactions                                         │ │
│ │  Review low-confidence and unclassified SMS records        [ Review → ] │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  SPENDING BY CATEGORY                                                   │ │
│ │                     ┌──────────┐                                        │ │
│ │                     │  DONUT   │   🍕 Food & Dining   ₹14,200  (33%)    │ │
│ │                     │  CHART   │   🛒 Shopping        ₹10,500  (25%)    │ │
│ │                     │ ₹42,500  │   🚗 Transport       ₹ 6,400  (15%)    │ │
│ │                     └──────────┘   📱 Utilities       ₹ 4,200  (10%)    │ │
│ │                                    📦 Others          ₹ 7,200  (17%)    │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  INCOME VS EXPENSE (LAST 6 MONTHS)                                      │ │
│ │    ₹100k ┤                                                              │ │
│ │     ₹75k ┤     █ ░       █ ░       █ ░       █ ░       █ ░       █ ░    │ │
│ │     ₹50k ┤     █ ░       █ ░       █ ░       █ ░       █ ░       █ ░    │ │
│ │     ₹25k ┤     █ ░       █ ░       █ ░       █ ░       █ ░       █ ░    │ │
│ │        0 └─────┴─┴───────┴─┴───────┴─┴───────┴─┴───────┴─┴───────┴─┴─── │ │
│ │               May       Jun       Jul       Aug       Sep       Oct     │ │
│ │               (█ Income: Green      ░ Expense: Orange)                  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  Recent Activity                                              See all (89) →│
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ [ 🍕 ] Swiggy • Food & Dining • UPI • ••1234       -₹ 450.00    12:08 PM│ │
│ │ [ 🛒 ] Amazon • Shopping • Card • ••5625         -₹ 2,399.00   Yesterday│ │
│ │ [ 💰 ] Acme Corp • Salary • NEFT • ••1234       +₹ 85,000.00      01 Oct│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Interactions & Drill-Down:
- **Tapping Period Arrow (`◀` / `▶`)**: Updates state to the previous/next month; recalculates Net Savings, Safe-to-Spend, Category Donut, and Recent list in-place.
- **Tapping Donut Segment or Category Row**: Navigates to `TransactionsScreen` with pre-filled filters: `category = "Food & Dining"` and `dateRange = SelectedMonth`.
- **Tapping 6-Month Bar**: Navigates to `TransactionsScreen` filtered to that specific month.
- **Tapping `[ Review → ]`**: Opens `ReviewQueue` modal workflow.

---

### 2.2 Screen 2: Clean Transactions List (`feature:transactions`)

#### Normal Populated & Filter Active View:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Transactions                                                [ 🔍 ]  [ ⫶ ]   │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ 🔍  Search merchants, amounts, or type...                          [ ✕ ]│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│  [ ⫶ Filters (2) ]  [ All ]  [ ✔ Expense ]  [ Income ]  [ Needs Review ● ]  │
│ ─────────────────────────────────────────────────────────────────────────── │
│  Active: Oct 2026  •  Food & Dining                                  [ ✕ ]  │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ Filtered: 34 items   •   Total Expenses: -₹ 14,250.00                   │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  TODAY • -₹ 1,245.00                                                        │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ [ 🍕 ] Swiggy                                                 -₹ 450.00 │ │
│ │        Food & Dining • UPI • ••1234                    12:08 PM         │ │
│ ├─────────────────────────────────────────────────────────────────────────┤ │
│ │ [ ☕ ] Blue Tokai Coffee                                      -₹ 280.00 │ │
│ │        Food & Dining • Card • ••5625                   09:15 AM         │ │
│ ├─────────────────────────────────────────────────────────────────────────┤ │
│ │ [ 🍔 ] McDonald's                                             -₹ 515.00 │ │
│ │        Food & Dining • UPI • ••1234                   01:30 AM         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  YESTERDAY • -₹ 890.00                                                      │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ [ 🍕 ] Zomato                                                 -₹ 890.00 │ │
│ │        Food & Dining • UPI • ••1234                Yesterday • Needs Rev●│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Multi-Select Action Mode View (Triggered by Long-Press):

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [ ✕ ]  3 Selected                                           [ Select All ]  │
├─────────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ [☑]  [ 🍕 ] Swiggy                            -₹ 450.00   Food & Dining │ │
│ │ [☑]  [ ☕ ] Blue Tokai Coffee                 -₹ 280.00   Food & Dining │ │
│ │ [☑]  [ 🍔 ] McDonald's                        -₹ 515.00   Food & Dining │ │
│ │ [☐]  [ 🚕 ] Uber                              -₹ 320.00   Transport     │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ [ 🏷 Change Category ]   [ 🔄 Change Type ]   [ 👁 Mark Reviewed ]  [ 🗑 ]│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Swipe Gestures on Transaction Rows:
- **Swipe Right (`→`)**: Quick-categorize trigger with subtle green background and check icon. Reveals 3 top recommended category chips inline.
- **Swipe Left (`←`)**: Delete / Exclude transaction with red background and trash icon. Triggers undo snackbar: `"Transaction removed • [ Undo ]"` (5-second grace period).

---

### 2.3 Screen 3: Transaction Detail Screen (`feature:transactions`)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [ ← Back ]                     Transaction Detail                   [ ⋮ ]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                                 -₹ 18,596.70                                │
│                              RAZ Optick Labs Inc                            │
│                        27 Sep 2026 at 12:08 AM • Card XX5625                │
│                                                                             │
│         [ 🏷 Shopping ▾ ]      [ ✔ Expense ▾ ]      [ 💳 IDFC First ]       │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  TRANSACTION DETAILS                                                        │
│  Payment Channel               Credit Card (Swipe / Online)                 │
│  Bank Reference / UTR          REF839218392019                              │
│  UPI Identifier                —                                            │
│  Balance After Spend           ₹ 3,89,646.50 (Available Limit)             │
│  Parser Confidence             95% (High Reliability)                       │
│  Classification Source         Rule: "Optick Labs -> Shopping"              │
│  Review Status                 Reviewed by user                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  USER NOTE                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ Purchased Tissot Power Cash watch for gifting              [ ✎ Edit ]  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│  ORIGINAL BANK SMS                                             [ Collapse ] │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ [VM-IDFCFB-S] Transaction Successful! INR 18596.70 spent on your IDFC  │ │
│  │ FIRST Bank Credit Card ending XX5625 at RAZ*Optick Labs Inc on 27 SEP  │ │
│  │ 2026 at 12:08 AM Avbl Limit: INR 389646.5                             │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│  [ 🚩 Report Parsing Error ]                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.4 Screen 4: Insights & Analytics (`feature:insights`)

Organized into 3 structured tabs to prevent overwhelming single-page scrolling:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Insights & Analytics                                        [ Oct 2026 ▾ ]  │
├─────────────────────────────────────────────────────────────────────────────┤
│        [ ✔ 1. OVERVIEW ]         [ 2. BUDGETS ]        [ 3. RECURRING ]     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Monthly Cash Flow                                                          │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ Income: +₹85,000.00    Expenses: -₹42,500.00     Net Savings: ₹42,500.00│ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  Spending Intensity Heatmap (Calendar 35-Day Grid)                          │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │  M   T   W   T   F   S   S                                              │ │
│ │ [0] [1] [2] [0] [3] [4] [1]       Selected: Friday, 24 Oct              │ │
│ │ [2] [0] [1] [4] [2] [0] [1]       Spend: -₹ 2,450.00 (5 txns)           │ │
│ │ [1] [3] [0] [2] [1] [4] [0]                                             │ │
│ │ [0] [2] [1] [0] [3] [1] [2]       Intensity: [0: None] [1: <500]        │ │
│ │ [1] [0] [2] [4] [1] [0] [0]                  [2: 500-2K] [3: 2-5K] [4: >5K│
│ └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                             │
│  Top Merchants This Month                                                   │
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ 1. RAZ Optick Labs                           ₹ 18,596.70   (43.7% of spend)│
│ │ 2. Swiggy                                    ₹  4,250.00   (10.0% of spend)│
│ │ 3. Amazon India                              ₹  3,890.00   ( 9.1% of spend)│
│ │ 4. Shell Petrol Pump                         ₹  2,500.00   ( 5.8% of spend)│
│ │ 5. Netflix India                             ₹    649.00   ( 1.5% of spend)│
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.5 Screen 5: Review Queue Workflow (`feature:review`)

When users tap "12 transactions need review":

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [ ← Back ]             Review Transactions (1 of 12)        Progress: 10%  │
│ ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                                                                     │   │
│   │                        -₹ 2,000.00                                  │   │
│   │                    Tissot Power Cash                                │   │
│   │                 Today, 10:45 AM • Wallet                            │   │
│   │                                                                     │   │
│   │  Original SMS snippet:                                              │   │
│   │  "Rs.2000 has been credited to your Tissot Power Cash Wallet..."    │   │
│   │                                                                     │   │
│   │  Select Category:                                                   │   │
│   │  [ 🏷 Shopping ]  [ 🎁 Gift / Offer ]  [ 💼 Income ]  [ ⚙ Other ]   │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│     [ 🚫 Not an Expense (Ignore) ]                   [ ⏭ Skip for now ]     │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ ☑ Remember this rule for future messages from "Tissot Power Cash"     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```
