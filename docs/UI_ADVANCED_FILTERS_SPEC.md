# Advanced Filters & Clean Transaction Data Display Specification

**Document Version:** 1.0  
**Companion to:** `docs/UI_REDESIGN_PLAN.md`, `docs/SRS.md` (FR-6.1, FR-6.2), `docs/UI_SPEC.md` (§5.3)  
**Target Platform:** Jetpack Compose, Material 3, Android 8.0+  

---

## 1. Objectives & Design Vision

1. **Clean Visual Hierarchy**: Turn noisy, raw SMS strings into scannable, human-readable records (merchant brand, category pill, payment method, formatted Indian currency with tabular figures).
2. **Instant, Zero-Friction Filtering**: Let users slice financial transactions across any dimension (date, type, category, amount, channel, account, review status) within 1-2 taps.
3. **Live Feedback**: In both quick-filter chips and bottom sheet modal, always show the live counter and total monetary sum matching the filter state (e.g., `"Show 24 transactions (-₹14,250.00)"`).
4. **Smart Search Bar**: Enable natural text search + smart token parsing (`>500`, `upi`, `amazon`, `card`) while preserving exact string matching.

---

## 2. Clean Data Presentation Model

### 2.1 Raw vs Cleaned Field Mapping

| Raw SMS Fragment | Normalized Domain Field | Display Representation |
|---|---|---|
| `spent on your IDFC FIRST Bank Credit Card ending XX5625 at RAZ*Optick Labs Inc` | `merchantRaw = "RAZ*Optick Labs Inc"`<br>`merchantNormalised = "Raz Optick Labs"` | **Raz Optick Labs** (Title)<br>Card ending ••5625 (Subtitle) |
| `VPA swiggy@icici UPI Ref 6128...` | `merchantNormalised = "Swiggy"`<br>`channel = UPI`<br>`upiId = "swiggy@icici"` | **Swiggy** (Title)<br>Food & Dining • UPI (Subtitle) |
| `INR 18596.70` | `amount = 1859670L` (paise) | **-₹18,596.70** (Tabular figures, Expense color) |
| `1790447880000L` | `dateTime` (epoch ms) | **12:08 AM** (if today)<br>**Yesterday** (if -1 day)<br>**27 Sep** (if within year) |
| `parseConfidence = 0.55f` | Low confidence indicator | Amber/Red warning dot (`●`) alerting review needed |

### 2.2 Visual Anatomy of a Clean Transaction Row

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [ 🍕 ]  Swiggy                                                 -₹ 450.00   │
│ (40dp)   Food & Dining  •  UPI  •  ••1234               Today, 12:08 PM  ●  │
└─────────────────────────────────────────────────────────────────────────────┘
  │        │                 │        │                   │             │  │
  │        │                 │        │                   │             │  └ Low-confidence dot
  │        │                 │        │                   │             └ Reviewed check (✓)
  │        │                 │        │                   └ Relative date/time
  │        │                 │        └ Account last-4 (subtle text)
  │        │                 └ Payment Channel chip/bullet
  │        └ Normalized Category Name
  └ Category Icon in 40dp colored container (from 16-color theme palette)
```

#### Typography & Colors:
- **Title (Merchant)**: `MaterialTheme.typography.titleMedium` (16sp, FontWeight.SemiBold, `onSurface`). Max 1 line, ellipsize end.
- **Subtitle (Category & Metadata)**: `MaterialTheme.typography.bodySmall` (12sp, `onSurfaceVariant`). Clean bullets ` • ` separator.
- **Amount (Hero Right)**: `AmountText` with `fontFeatureSettings = "tnum"`, `fontWeight = SemiBold`, size 16sp.
  - Expense: `-₹X,XX,XXX.XX` in `financialColors.expense`
  - Income: `+₹X,XX,XXX.XX` in `financialColors.income`
  - Transfer: `₹X,XX,XXX.XX` in `financialColors.transfer` (no +/- sign)
- **Status Dot**:
  - `parseConfidence < 0.70f` or `!isReviewed`: 6dp circle in `financialColors.warning` or primary color.

---

## 3. Advanced Filter Architecture

### 3.1 Filter Data Domain Model

```kotlin
package com.kharcha.core.model

import java.time.LocalDate

data class TransactionFilter(
    val query: String = "",
    val types: Set<TransactionType> = emptySet(),       // Empty = all types
    val dateRange: DateRangeSelection = DateRangeSelection.AllTime,
    val categories: Set<String> = emptySet(),           // Empty = all categories
    val amountRange: AmountRangeFilter = AmountRangeFilter.Any,
    val channels: Set<Channel> = emptySet(),            // Empty = all channels
    val accountIds: Set<Long> = emptySet(),             // Empty = all accounts
    val reviewStatus: ReviewStatusFilter = ReviewStatusFilter.All,
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
            types.isNotEmpty() ||
            dateRange !is DateRangeSelection.AllTime ||
            categories.isNotEmpty() ||
            amountRange !is AmountRangeFilter.Any ||
            channels.isNotEmpty() ||
            accountIds.isNotEmpty() ||
            reviewStatus != ReviewStatusFilter.All

    val activeFilterCount: Int
        get() {
            var count = 0
            if (query.isNotBlank()) count++
            if (types.isNotEmpty()) count++
            if (dateRange !is DateRangeSelection.AllTime) count++
            if (categories.isNotEmpty()) count += categories.size
            if (amountRange !is AmountRangeFilter.Any) count++
            if (channels.isNotEmpty()) count += channels.size
            if (accountIds.isNotEmpty()) count += accountIds.size
            if (reviewStatus != ReviewStatusFilter.All) count++
            return count
        }
}

sealed interface DateRangeSelection {
    object AllTime : DateRangeSelection
    object Today : DateRangeSelection
    object Yesterday : DateRangeSelection
    object ThisWeek : DateRangeSelection
    object ThisMonth : DateRangeSelection
    object LastMonth : DateRangeSelection
    data class LastNDays(val days: Int) : DateRangeSelection // 30, 90, 180
    data class Custom(val startEpochMs: Long, val endEpochMs: Long) : DateRangeSelection
}

sealed interface AmountRangeFilter {
    object Any : AmountRangeFilter
    object Under100 : AmountRangeFilter                         // < ₹100
    object Between100And500 : AmountRangeFilter                 // ₹100 - ₹500
    object Between500And2000 : AmountRangeFilter                // ₹500 - ₹2,000
    object Between2000And10000 : AmountRangeFilter              // ₹2,000 - ₹10,000
    object Above10000 : AmountRangeFilter                       // > ₹10,000
    data class Custom(val minPaise: Long?, val maxPaise: Long?) : AmountRangeFilter
}

enum class ReviewStatusFilter {
    All,
    ReviewedOnly,
    NeedsReviewOnly
}
```

---

## 4. Filter UI Components & Interactions

### 4.1 Two-Tier Filtering System

1. **Tier 1: Sticky Quick-Filter Strip (Always on Transactions Screen)**
   - Search text field with instant debounce (150ms).
   - Horizontal scrollable filter chips:
     - `[ ⫶ All Filters (3) ]` (Badge button opening bottom sheet)
     - `[ All ]` / `[ Expense ↓ ]` / `[ Income ↑ ]` / `[ Transfer ⇄ ]`
     - `[ Needs Review ● ]`
     - `[ This Month ▾ ]` (Quick dropdown for common periods)

2. **Tier 2: Comprehensive Bottom Sheet (`AdvancedFilterSheet`)**
   - Modal bottom sheet with drag handle and full keyboard/accessibility support.
   - Contains 6 distinct filter sections (Date, Category, Amount, Channel, Account, Status).
   - Sticky bottom footer with:
     - **Reset All** text button.
     - **Primary Action**: Dynamic CTA showing count and net total:
       `"Show 34 Transactions (-₹8,450.00)"`.

### 4.2 Layout Wireframe: `AdvancedFilterSheet`

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 ───                                         │
│  Filters                                                  Reset All         │
├─────────────────────────────────────────────────────────────────────────────┤
│  DATE RANGE                                                                 │
│  [ This Month ]  [ Last Month ]  [ Last 30 Days ]  [ Last 90 Days ]         │
│  [ Custom Range... ]                                                        │
│                                                                             │
│  Selected: 01 Oct 2026 – 25 Oct 2026 (25 days)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  TRANSACTION TYPE                                                           │
│  [ ✔ Expense ]    [ Income ]    [ Transfer ]    [ Refund ]                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  CATEGORIES (Multi-Select)                                                  │
│  [ ✔ 🍕 Food & Dining ]  [ ✔ 🛒 Shopping ]  [ 🚗 Transport ]                │
│  [ 📱 Bills & Utilities ]  [ 🏥 Health ]      [ +7 More... ]                │
├─────────────────────────────────────────────────────────────────────────────┤
│  AMOUNT RANGE                                                               │
│  [ Any ]  [ < ₹100 ]  [ ₹100–500 ]  [ ₹500–2K ]  [ ₹2K–10K ]  [ > ₹10K ]   │
│                                                                             │
│  Min: ₹200.00                                            Max: ₹5,000.00     │
│  ────●───────────────────────────────────────●─────────────────────         │
│     (Dual Range Slider with Haptic Step Snapping)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  PAYMENT CHANNEL                                                            │
│  [ ✔ UPI ]  [ Card (Debit/Credit) ]  [ NetBanking ]  [ ATM ]  [ Cash ]      │
├─────────────────────────────────────────────────────────────────────────────┤
│  ACCOUNT                                                                    │
│  [ HDFC Bank (••1234) ]  [ IDFC First (••5625) ]  [ SBI (••9012) ]          │
├─────────────────────────────────────────────────────────────────────────────┤
│  REVIEW STATUS                                                              │
│  [ All ]  [ ✔ Needs Review (Low Confidence) ]  [ Reviewed ]                 │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │               Show 18 Transactions (-₹6,280.00)                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Smart Search Syntax Grammar

In addition to full-text search against `merchantRaw`, `merchantNormalised`, and `refNumber`, the search bar parses inline token qualifiers:

| Syntax | Example | Behavior |
|---|---|---|
| `>amount` or `<amount` | `>1000` | Filters transactions where amount > ₹1,000.00 |
| `#category` | `#food` | Filters transactions belonging to Food & Dining |
| `@channel` | `@upi` | Filters transactions done via UPI |
| `card:last4` | `card:5625` | Filters transactions on account ending in 5625 |
| `is:review` | `is:review` | Filters unreviewed / low confidence transactions |

If no qualifier prefix is supplied, it executes fuzzy token matching across merchant, reference number, UPI ID, and user notes.

---

## 6. Performance & Clean Querying Guidelines

1. **In-Memory Filtering with Memoization**:
   - Filter state changes trigger `derivedStateOf` / `remember(transactions, filter)` without recalculating strings.
   - Amount calculations run on integer `Long` minor units (paise) using `.sumOf { it.amount }` without any floating-point conversions.
2. **Date Boundaries**:
   - Dates convert to epoch boundaries using the device's local timezone (`ZoneId.systemDefault()`).
   - Grouping runs single-pass into a sorted linked map (`Map<Long, List<Transaction>>`).
3. **Empty Filter State Resilience**:
   - When no transactions match:
     - Show friendly empty graphic.
     - Display `"No transactions match your current filters"`.
     - Prominent button: `[ Reset All Filters ]` returning to default state in 1 tap.
