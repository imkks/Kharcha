# Kharcha UI Redesign Plan

**Version 1.0 • October 2026**
**Status: Planning (No code yet)**
**Branch: `feature-ui-dashboard`**

---

## Table of Contents

1. [Current State Assessment](#1-current-state-assessment)
2. [Redesign Goals](#2-redesign-goals)
3. [Navigation & Architecture](#3-navigation--architecture)
4. [Screen Redesigns](#4-screen-redesigns)
   - 4.1 [Home / Dashboard](#41-home--dashboard)
   - 4.2 [Transactions](#42-transactions)
   - 4.3 [Insights](#43-insights)
   - 4.4 [More / Settings](#44-more--settings)
5. [Advanced Filters System](#5-advanced-filters-system)
6. [Transaction Detail Screen](#6-transaction-detail-screen)
7. [Component Library](#7-component-library)
8. [Design Tokens & Theme Fixes](#8-design-tokens--theme-fixes)
9. [Implementation Phases](#9-implementation-phases)

---

## 1. Current State Assessment

### What works well ✅

| Area | Details |
|------|---------|
| Onboarding | All 5 screens implemented, matches spec |
| Financial color tokens | Income (green), Expense (red-orange), Transfer (blue-grey) with light/dark |
| Summary card | Net amount hero, income/expense sub-tiles with icons |
| Safe-to-spend gauge | Daily allowance, pacing status, budget progress bar |
| Review card | Conditional "X need review" banner with action button |
| Transaction row | Merchant, category subtitle, amount with sign/color, confidence dot |
| Search | Text search across merchant, ref number, channel, category |
| Type filter chips | All / Expense / Income / Transfer / Needs Review |
| Date grouping | Sticky headers with Today / Yesterday / date, daily expense totals |
| Spend calendar heatmap | 35-day grid with 5 intensity levels, clickable cells |
| Recurring detection | Auto-detected subscriptions with cadence, next charge, price alerts |
| Category/channel breakdown | Top categories with badges, progress bars, percentages |
| CSV + LLM export | Full transaction export and batch classification JSON |
| Review queue dialog | Card-stack with 6 category chips, skip/ignore, progress |

### What's missing or broken ❌

| # | Gap | Severity | SRS Ref |
|---|-----|----------|---------|
| 1 | **Home period selector is cosmetic** — doesn't actually filter data | 🔴 Critical | FR-8.2 |
| 2 | **No date range filter** on Transactions | 🔴 Critical | FR-6.2 |
| 3 | **No category filter** on Transactions | 🔴 Critical | FR-6.2 |
| 4 | **No amount range filter** | 🟡 High | FR-6.2 |
| 5 | **No channel/payment mode filter** | 🟡 High | FR-6.2 |
| 6 | **No donut chart** on Home | 🟡 High | FR-8.3 |
| 7 | **No bar chart** (income vs expense by month) | 🟡 High | FR-8.3 |
| 8 | **No chart drill-down** to filtered transactions | 🟡 High | FR-8.4 |
| 9 | **Insights is flat**, not tabbed (Overview/Budgets/Recurring) | 🟡 High | UI Spec 5.8 |
| 10 | **No Transaction Detail screen** — only a dialog | 🟡 High | FR-6.3 |
| 11 | **No multi-select / bulk edit** | 🟢 Medium | FR-5.6 |
| 12 | **No swipe actions** (categorise/delete) | 🟢 Medium | UI Spec 5.3 |
| 13 | **No Accounts screen** (module exists, empty) | 🟢 Medium | FR-7 |
| 14 | **No Settings screen** (module exists, empty) | 🟢 Medium | FR-12.7 |
| 15 | **No custom typography** (no tabular figures) | 🟢 Medium | UI Spec 2.2 |
| 16 | **Hard-coded colors** in InsightsScreen | 🟢 Medium | AGENTS.md |
| 17 | **Categories are hardcoded strings** — no entity, no hierarchy | 🟢 Medium | FR-5.3 |
| 18 | **No skeleton loaders** for loading states | 🟢 Medium | UI Spec 5.2 |
| 19 | **No Room database layer** — `core:database` is empty | 🔴 Critical | SRS §5 |
| 20 | **No Navigation Compose** — enum state switching | 🟢 Medium | TechDoc §1 |

---

## 2. Redesign Goals

### Principles

1. **Glanceable**: Monthly totals visible without scrolling. One-glance budget health.
2. **Filterable**: Any dimension (date, category, amount, channel, merchant, review status) can filter transactions in ≤2 taps.
3. **Trust-building**: Clean data, clear sources. Show where each number comes from. "Parsed from SMS" vs "You entered" vs "Auto-categorised".
4. **Progressive disclosure**: Simple on the surface, powerful when drilled into. Advanced filters are behind a sheet, not cluttering the default view.
5. **Consistent components**: Every transaction row, amount text, and chart follows the same visual language across all screens.

### Non-goals for this plan

- No cloud sync or accounts system (offline-only)
- No Navigation Compose migration (separate PR)
- No Room database migration (separate PR)
- No new third-party libraries besides Vico for charts

---

## 3. Navigation & Architecture

### Current: 4-tab bottom nav

```
[ Home ] [ Transactions ] [ Insights ] [ More ]
                    + FAB (Add Transaction)
```

### Proposed: Same 4 tabs, richer content

```
┌─────────────────────────────────────────────────┐
│ Tab Bar                                         │
│ [ Home ] [ Transactions ] [ Insights ] [ More ] │
└─────────────────────────────────────────────────┘

Home ─────────── Dashboard with summary, charts, review card, recent
Transactions ─── Full list with advanced filters, search, grouping
Insights ─────── Tabbed: Overview | Budgets | Recurring
More ──────────── Accounts, Categories & Rules, Settings, Export

FAB visible on: Home, Transactions
```

### Screen flow from each tab

```
Home
 ├─ Period selector (actually filters data!)
 ├─ Donut chart → tap segment → Transactions (filtered by category)
 ├─ Bar chart → tap month bar → Transactions (filtered by month)
 ├─ Review card → Review Queue (full screen, not dialog)
 └─ Recent txn row → Transaction Detail (full screen)

Transactions
 ├─ Search → results update live
 ├─ Filter chips → quick type filter
 ├─ Filter icon → Advanced Filter Sheet (date, category, amount, channel)
 ├─ Tap row → Transaction Detail
 ├─ Long-press → Multi-select mode → Bottom action bar
 └─ Swipe right → Quick categorise | Swipe left → Delete with undo

Insights
 ├─ Overview tab: category donut, monthly bars, top merchants, heatmap
 ├─ Budgets tab: per-category budgets with progress bars
 └─ Recurring tab: detected subscriptions with next charge dates

More
 ├─ Accounts → Account list → Account detail (filtered txns)
 ├─ Categories & Rules → Category manager, Rule manager
 ├─ Settings → General, SMS, Notifications, Security, Data
 └─ Export → CSV / JSON export with date range and filters
```

---

## 4. Screen Redesigns

### 4.1 Home / Dashboard

**Layout (top to bottom in a scrolling list):**

```
┌──────────────────────────────────────┐
│  Kharcha            [◀ Oct 2026 ▶]  │  ← Top bar with WORKING period selector
├──────────────────────────────────────┤
│                                      │
│          Net Balance                 │
│         ₹1,23,456                    │  ← Display Large, hero number
│                                      │
│   ↑ Income          ↓ Expense        │
│   +₹85,000          -₹42,500        │  ← Green / Red-orange with icons
│   ▲12% vs last      ▼8% vs last     │  ← Percentage change vs previous period
│                                      │
├──────────────────────────────────────┤
│  🟡 Safe to Spend                   │
│  ₹1,420/day • 9 days left           │
│  ████████████░░░░ 67%               │  ← Budget progress bar
│  Burn rate: ₹1,580/day • On Track   │
├──────────────────────────────────────┤
│  ⚠ 12 transactions need review      │  ← Only if count > 0
│                         [Review →]   │
├──────────────────────────────────────┤
│                                      │
│      [  Category Donut Chart  ]      │  ← Top 5 + Others
│                                      │
│  Food & Dining     ₹12,400   29%    │  ← Legend with tappable rows
│  Transport         ₹8,200    19%    │     Tap row → Transactions filtered
│  Shopping          ₹6,800    16%    │
│  Bills             ₹5,500    13%    │
│  Entertainment     ₹4,200    10%    │
│  Others            ₹5,400    13%    │
│                                      │
├──────────────────────────────────────┤
│  Income vs Expense (6 months)        │
│                                      │
│  ┃██ ██ ██ ██ ██ ██┃                │  ← Grouped bar chart
│  ┃May Jun Jul Aug Sep Oct┃          │     Green = income, orange = expense
│                                      │     Tap bar → that month's transactions
├──────────────────────────────────────┤
│  Recent Transactions       See all → │
│                                      │
│  🍕 Swiggy         -₹450   12:08pm  │
│  🛒 Amazon         -₹2,399 Yesterday │
│  💰 ACME Tech     +₹85,000 1 Oct    │
│  🚕 Uber          -₹320    30 Sep   │
│  ⛽ HP Fuel        -₹2,100  29 Sep   │
│                                      │
└──────────────────────────────────────┘
```

**Key changes from current:**
1. **Period selector actually filters** — prev/next arrows cycle months, data recalculates
2. **% change vs previous period** — shows trend direction on income/expense tiles
3. **Donut chart** — interactive, top 5 + Others, tap to drill into filtered transactions
4. **Bar chart** — last 6 months income vs expense, tap to drill
5. **Recent transactions** — show 5 (not 10), with relative timestamps and category icons

**Component breakdown:**

| Component | Status | Notes |
|-----------|--------|-------|
| `PeriodSelector` | **New** | Chip with ◀ ▶ arrows, cycles months. Returns date range. |
| `SummaryCard` | **Update** | Add % change badges vs previous period |
| `SafeToSpendGaugeCard` | **Keep** | Already good. Minor polish. |
| `ReviewCard` | **Keep** | Works well. |
| `DonutChart` | **New** | Vico or custom Canvas. Top 5 + Others. Interactive. |
| `CategoryLegendRow` | **New** | Icon + name + amount + percentage + progress bar |
| `IncomeExpenseBarChart` | **New** | Vico grouped bar chart, 6 months |
| `RecentTransactionRow` | **Update** | Add category icon, relative time, compact layout |

---

### 4.2 Transactions

**Layout:**

```
┌──────────────────────────────────────┐
│  Transactions               [🔍] [⫶] │  ← Search icon + Filter icon
├──────────────────────────────────────┤
│  🔍 Search merchants, amounts...     │  ← Expands on tap
├──────────────────────────────────────┤
│ [All] [Expense] [Income] [Transfer]  │  ← Quick type filter chips
│ [Needs Review]                       │     (scrollable row)
├──────────────────────────────────────┤
│  Active: Oct 2026 • Food • UPI  [✕]  │  ← Active filter summary (if any)
├──────────────────────────────────────┤
│  Income ₹85,000  Expense ₹42,500    │
│  Net ₹42,500                         │  ← Summary strip for filtered set
├──────────────────────────────────────┤
│  Today • -₹1,245                     │  ← Sticky date header with day total
│ ─────────────────────────────────── │
│  🍕 Swiggy                   -₹450  │
│     Food & Dining • UPI • ••1234     │
│  🛒 Amazon                 -₹2,399  │
│     Shopping • Card • ••5625         │
│  💰 ACME Tech             +₹85,000  │
│     Salary • NEFT • ••1234      ✓   │  ← ✓ = reviewed
├──────────────────────────────────────┤
│  Yesterday • -₹870                   │
│ ─────────────────────────────────── │
│  🚕 Uber                    -₹320  │
│     Transport • UPI • ••1234    ●   │  ← ● = low confidence
│  ⛽ HP Fuel                 -₹550   │
│     Transport • Card • ••5625       │
└──────────────────────────────────────┘
```

**Key changes from current:**
1. **Filter icon** (⫶) in top bar opens Advanced Filter Bottom Sheet
2. **Active filter summary strip** shows what's active with clear-all (✕)
3. **Category icons** in transaction rows (colored circles)
4. **Review status indicator** — ✓ for reviewed, ● for low confidence
5. **Swipe actions** — right to categorise, left to delete with undo
6. **Long-press multi-select** with bottom action bar
7. **Search includes amount** — type "450" to find ₹450 transactions

**Transaction Row redesign:**

```
┌────────────────────────────────────────────────┐
│ [🍕]  Swiggy                          -₹450   │
│       Food & Dining • UPI • ••1234     12:08   │
│                                           ●    │
└────────────────────────────────────────────────┘
  40dp   ← body fills →              ← amount →
 circle                               right-aligned
```

- **Leading**: 40dp category icon in colored circle (from `CategoryIconHelper`)
- **Title**: Merchant name (bold), single line, ellipsize end
- **Subtitle**: Category • Channel • Account last 4 — separated by bullets
- **Trailing top**: Signed amount with semantic color, right-aligned
- **Trailing bottom**: Relative time (12:08, Yesterday, 25 Oct)
- **Indicators**: ● red dot for low confidence (<0.7), ✓ for reviewed

---

### 4.3 Insights

**Change from flat scroll to 3 tabs:**

```
┌──────────────────────────────────────┐
│  Insights & Analytics   [Oct 2026 ▼] │
├──────────────────────────────────────┤
│  [ Overview ]  [ Budgets ]  [ Sub ]  │  ← Tab row (fixed, not scrolling)
├──────────────────────────────────────┤
│  (Tab content below)                 │
└──────────────────────────────────────┘
```

#### Overview Tab

```
1. Monthly Flow Card
   ┌──────────────────────────────────┐
   │  Income    ₹85,000    ▲12%      │
   │  Expenses  ₹42,500    ▼8%       │
   │  Savings   ₹42,500    50% rate  │
   └──────────────────────────────────┘

2. Category Breakdown
   ┌──────────────────────────────────┐
   │       [Donut Chart]              │
   │  (same as Home, interactive)     │
   │  + ranked list with % bars       │
   └──────────────────────────────────┘

3. Spend Calendar Heatmap (already exists — keep)

4. Top Merchants
   ┌──────────────────────────────────┐
   │  1. Swiggy         ₹4,200  10%  │
   │  2. Amazon          ₹3,800   9%  │
   │  3. Uber            ₹2,100   5%  │
   │  4. HP Fuel         ₹1,800   4%  │
   │  5. Netflix           ₹649   2%  │
   └──────────────────────────────────┘

5. Payment Channel Breakdown (already exists — keep)

6. Insight Cards (new)
   ┌──────────────────────────────────┐
   │  📈 Food Delivery is up 32%     │
   │     vs last month                │
   │  💡 You spent ₹2,400 more on    │
   │     shopping than usual          │
   └──────────────────────────────────┘
```

#### Budgets Tab

```
┌──────────────────────────────────────┐
│  Overall Budget                      │
│  ████████████░░░░░░ ₹42,500/₹60,000 │
│  ₹17,500 left • 9 days              │
├──────────────────────────────────────┤
│  Food & Dining                       │
│  ██████████████░░░ ₹12,400/₹15,000  │  ← Green (83%)
│  ₹2,600 left • Pacing high          │
├──────────────────────────────────────┤
│  Transport                           │
│  ████████░░░░░░░░░ ₹8,200/₹20,000   │  ← Green (41%)
│  ₹11,800 left • On track            │
├──────────────────────────────────────┤
│  Shopping                            │
│  ██████████████████ ₹6,800/₹5,000   │  ← Red (136%!)
│  Exceeded by ₹1,800                 │
├──────────────────────────────────────┤
│                           [+ Budget] │  ← FAB to add new budget
└──────────────────────────────────────┘
```

Color thresholds:
- **Green**: 0–79% of budget used
- **Amber**: 80–99%
- **Red**: 100%+

#### Recurring / Subscriptions Tab

```
┌──────────────────────────────────────┐
│  Monthly commitment: ₹4,647         │
├──────────────────────────────────────┤
│  Netflix          ₹649/mo   Monthly  │
│  Next: 30 Oct 2026                   │
├──────────────────────────────────────┤
│  Spotify          ₹119/mo   Monthly  │
│  Next: 1 Nov 2026                    │
├──────────────────────────────────────┤
│  Amazon Prime    ₹1,499/yr  Yearly   │
│  ⚠ Price changed from ₹999          │  ← Price diff alert
│  Next: 15 Jan 2027                   │
├──────────────────────────────────────┤
│  Gym Membership  ₹2,500/mo  Monthly  │
│  Next: 5 Nov 2026                    │
│  [Confirm] [Not recurring]           │  ← Unconfirmed items have actions
└──────────────────────────────────────┘
```

---

### 4.4 More / Settings

**Replace inline `MoreSettingsScreen` with a proper grouped list:**

```
┌──────────────────────────────────────┐
│  More                                │
├──────────────────────────────────────┤
│  🔒 100% Offline • On-Device Only   │  ← Privacy badge (keep)
├──────────────────────────────────────┤
│  ACCOUNTS                            │
│  🏦 Accounts                    →   │
├──────────────────────────────────────┤
│  ORGANIZATION                        │
│  📂 Categories & Rules           →   │
│  🏷️ Merchant Rules (12)          →   │
├──────────────────────────────────────┤
│  DATA                                │
│  📤 Export Transactions (CSV)    →   │
│  🤖 LLM Batch Export (JSON)     →   │
│  🔄 Re-scan SMS Inbox            →   │
├──────────────────────────────────────┤
│  SETTINGS                            │
│  ⚙ General                      →   │
│  📱 SMS & Parsing                →   │
│  🔔 Notifications                →   │
│  🔐 Security                     →   │
├──────────────────────────────────────┤
│  ABOUT                               │
│  ℹ Version 1.0.0                     │
│  📋 Privacy Policy               →   │
│  ⚖ Open Source Licences          →   │
└──────────────────────────────────────┘
```

---

## 5. Advanced Filters System

### Filter Architecture

Two levels of filtering:

1. **Quick filters** — always visible as chips on the Transactions screen
2. **Advanced filters** — full bottom sheet with all filter dimensions

### Quick Filter Chips (always visible)

```
[ All ] [ Expense ↓ ] [ Income ↑ ] [ Transfer ⇄ ] [ Needs Review ● ]
```

Single-select. Matches current behavior but with better icons.

### Advanced Filter Bottom Sheet

Triggered by the filter icon (⫶) in the Transactions top bar.

```
┌──────────────────────────────────────┐
│  ─── (drag handle) ───              │
│                                      │
│  Filters                   [Reset]   │
│                                      │
│  DATE RANGE                          │
│  [ This Month ] [ Last Month ]       │  ← Preset chips
│  [ Last 30 days ] [ Custom... ]      │
│                                      │
│  📅 15 Oct 2026 → 25 Oct 2026       │  ← Selected range display
│                                      │
│  CATEGORY                            │
│  [ Food ] [ Transport ] [ Shopping ] │  ← Multi-select chips
│  [ Bills ] [ Health ] [ +5 more ]    │     +N more opens full picker
│                                      │
│  AMOUNT RANGE                        │
│  [ <₹100 ] [ ₹100-500 ] [ ₹500-2K ] │  ← Preset brackets
│  [ ₹2K-10K ] [ >₹10K ] [ Custom ]   │
│                                      │
│     ₹200 ──────●────●────── ₹5,000   │  ← Dual-handle range slider
│                                      │
│  PAYMENT CHANNEL                     │
│  [ UPI ] [ Card ] [ NEFT/IMPS ]      │  ← Multi-select chips
│  [ ATM ] [ Wallet ] [ Cash ]         │
│                                      │
│  ACCOUNT                             │
│  [ HDFC ••1234 ] [ ICICI ••5678 ]    │  ← Multi-select chips
│  [ SBI ••9012 ]                      │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ Show 24 transactions (₹1,420)│    │  ← Live result counter!
│  └──────────────────────────────┘    │
└──────────────────────────────────────┘
```

### Filter State Model

```kotlin
data class TransactionFilter(
    val types: Set<TransactionType>,        // empty = all
    val dateRange: DateRange?,               // null = all time
    val categories: Set<String>,             // empty = all
    val amountRange: AmountRange?,           // null = any amount
    val channels: Set<Channel>,              // empty = all
    val accounts: Set<Long>,                 // empty = all (account IDs)
    val reviewStatus: ReviewFilter,          // ALL, REVIEWED, NEEDS_REVIEW
    val searchQuery: String,                 // empty = no search
)

sealed class DateRange {
    object ThisMonth : DateRange()
    object LastMonth : DateRange()
    data class LastNDays(val days: Int) : DateRange()
    data class Custom(val start: Long, val end: Long) : DateRange()
}

data class AmountRange(
    val minPaise: Long?,  // null = no lower bound
    val maxPaise: Long?,  // null = no upper bound
)

enum class ReviewFilter { ALL, REVIEWED, NEEDS_REVIEW }
```

### Active Filters Display

When filters are active, show a summary strip above the transaction list:

```
┌──────────────────────────────────────┐
│  Oct 2026 • Food, Transport • UPI [✕]│
└──────────────────────────────────────┘
```

- Compact text summary of active filters
- ✕ button clears all filters
- Tapping the strip re-opens the filter sheet

### Search Enhancements

Current search matches: merchantRaw, merchantNormalised, refNumber, channel, categorySource.

**Add:**
- **Amount search**: typing "450" matches transactions with amount 450.00
- **Autocomplete**: show matching merchant names as user types
- **Recent searches**: last 5 searches shown when search field is focused and empty

---

## 6. Transaction Detail Screen

**Replace the current `TransactionDetailDialog` with a full screen:**

```
┌──────────────────────────────────────┐
│  ←  Transaction Detail        [⋮]   │  ← Back + overflow menu
├──────────────────────────────────────┤
│                                      │
│           -₹18,596.70                │  ← Display Large, expense color
│         RAZ Optick Labs              │  ← Headline, merchant name
│    27 Sep 2026, 12:08 AM            │  ← Body, date/time
│                                      │
├──────────────────────────────────────┤
│  Type        [ Expense ▼ ]           │  ← Editable chip
│  Category    [ Shopping ▼ ]          │  ← Editable chip → Category Picker
│  Account     IDFC ••5625             │
│  Tags        [ + Add tag ]           │
├──────────────────────────────────────┤
│  DETAILS                             │
│  Channel       Credit Card           │
│  Reference     REF12345              │
│  UPI ID        —                     │
│  Balance After ₹3,89,646.50         │
│  Parse Conf.   95%  ████████████░░   │
│  Category By   Dictionary            │
├──────────────────────────────────────┤
│  NOTE                                │
│  ┌──────────────────────────────┐    │
│  │ Tissot watch purchase        │    │  ← Inline editable
│  └──────────────────────────────┘    │
├──────────────────────────────────────┤
│  ▶ Original SMS                      │  ← Expandable card
│  ┌──────────────────────────────┐    │
│  │ [VM-IDFCFB-S] Transaction   │    │
│  │ Successful! INR 18596.70    │    │
│  │ spent on your IDFC FIRST    │    │
│  │ Bank Credit Card ending...  │    │
│  │              [Report wrong] │    │
│  └──────────────────────────────┘    │
├──────────────────────────────────────┤
│  🔗 Linked: Refund of ₹18,596.70   │  ← If linked transaction exists
│     from RAZ Optick Labs, 5 Oct →   │     Tappable to jump
└──────────────────────────────────────┘

Overflow menu (⋮):
  • Split transaction
  • Mark as transfer
  • Mark as ignored
  • Delete
```

**Category Picker** (shown as bottom sheet when category chip is tapped):

```
┌──────────────────────────────────────┐
│  ─── (drag handle) ───              │
│                                      │
│  Pick a Category                     │
│  🔍 Search categories...             │
│                                      │
│  SUGGESTED                           │
│  [ 🛒 Shopping ] [ 🎁 Gifts ] [ 🏷 Other ]│
│                                      │
│  EXPENSE                             │
│  🍕 Food & Dining              →    │  ← Expandable to subcategories
│  🛒 Shopping                   →    │
│  🚗 Transport                  →    │
│  📱 Bills & Utilities          →    │
│  🏠 Rent & Housing                  │
│  🏥 Health                     →    │
│  🎬 Entertainment              →    │
│  ✈ Travel                     →    │
│  💳 Loans & EMI                     │
│  📦 Subscriptions                   │
│  ...                                 │
│                                      │
│  INCOME                              │
│  💰 Salary                          │
│  💼 Business                        │
│  ...                                 │
│                                      │
│  [ + New Category ]                  │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ Apply to all from this       │    │ ← After selection
│  │ merchant?                    │    │
│  │ ○ This one only              │    │
│  │ ● All past & future          │    │
│  │            [Cancel] [Apply]  │    │
│  └──────────────────────────────┘    │
└──────────────────────────────────────┘
```

---

## 7. Component Library

### New Components to Build

| Component | Module | Description |
|-----------|--------|-------------|
| `PeriodSelector` | `core:designsystem` | Chip with ◀ ▶ arrows, returns `DateRange`. Used on Home and Insights. |
| `DonutChart` | `core:designsystem` | Vico-based or custom Canvas. Top N + Others. Tap to select segment. Center shows total or selected amount. |
| `BarChart` | `core:designsystem` | Vico-based grouped bar chart. Income (green) vs expense (orange). Tap bar for details. |
| `BudgetProgressBar` | `core:designsystem` | Linear progress with rounded caps. Green/amber/red thresholds. Shows "₹X left for Y days". |
| `FilterChipRow` | `core:designsystem` | Horizontally scrollable chip row with consistent styling. |
| `AdvancedFilterSheet` | `feature:transactions` | Full bottom sheet with all filter dimensions, live result counter, reset. |
| `ActiveFilterStrip` | `feature:transactions` | Compact summary of active filters with clear-all. |
| `AmountRangeSlider` | `core:designsystem` | Dual-handle Material 3 range slider with formatted currency labels. |
| `DateRangePicker` | `core:designsystem` | Preset pills + custom calendar range. Uses M3 `DateRangePicker`. |
| `CategoryPickerSheet` | `core:designsystem` | Bottom sheet with search, suggestions, grouped categories, subcategory expansion. |
| `InsightCard` | `feature:insights` | Card with icon, headline insight text, and optional action. |
| `SkeletonLoader` | `core:designsystem` | Shimmer loading placeholder matching `TransactionRow` and card shapes. |
| `SwipeableTransactionRow` | `core:designsystem` | Wraps `TransactionRow` with swipe-to-categorise (right) and swipe-to-delete (left). |
| `MultiSelectBottomBar` | `feature:transactions` | Action bar shown during multi-select: Categorise, Change Type, Tag, Delete. |
| `PercentChangeBadge` | `core:designsystem` | Small badge showing ▲12% (green) or ▼8% (red) for period-over-period changes. |
| `AccountCard` | `feature:accounts` | Bank logo/initial, name, last 4, type, balance, last updated. |
| `SubscriptionRow` | `feature:insights` | Merchant, amount, cadence, next charge, price change alert, confirm/reject actions. |

### Components to Update

| Component | Changes |
|-----------|---------|
| `TransactionRow` | Add category icon (colored circle), review status indicator (✓/●), relative time display |
| `AmountText` | Ensure tabular figures font feature, add optional abbreviated format (1.2K, 1.2L) |
| `SummaryCard` | Add % change vs previous period badges |
| `ReviewQueueDialog` | Migrate to full screen `ReviewQueueScreen` in `feature:review` |
| `TransactionDetailDialog` | Migrate to full screen `TransactionDetailScreen` |
| `CategoryBadge` | Add per-category color assignment (16-color palette) |

---

## 8. Design Tokens & Theme Fixes

### Typography

Add custom typography with tabular figures:

```kotlin
// In Theme.kt
val KharchaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        fontFeatureSettings = "tnum",  // tabular figures
    ),
    // ... all text styles with tnum for number alignment
)
```

### Hard-coded Color Fixes

Replace all hard-coded colors in `InsightsScreen.kt`:

| Current | Replace with |
|---------|-------------|
| `Color(0xFF2E7D32)` | `MaterialTheme.financialColors.income` |
| `Color(0xFFED6C02)` | `MaterialTheme.financialColors.expense` |
| `Color(0xFFB45309)` | `MaterialTheme.financialColors.warning` |

### Category Colors

Add a fixed palette of 16 distinguishable, color-blind safe colors:

```kotlin
object CategoryColors {
    val palette = listOf(
        Color(0xFFE53935),  // Red
        Color(0xFF1E88E5),  // Blue
        Color(0xFF43A047),  // Green
        Color(0xFFFB8C00),  // Orange
        Color(0xFF8E24AA),  // Purple
        Color(0xFF00ACC1),  // Cyan
        Color(0xFF6D4C41),  // Brown
        Color(0xFFD81B60),  // Pink
        Color(0xFF3949AB),  // Indigo
        Color(0xFF00897B),  // Teal
        Color(0xFFC0CA33),  // Lime
        Color(0xFF546E7A),  // Blue Grey
        Color(0xFFFF7043),  // Deep Orange
        Color(0xFF7CB342),  // Light Green
        Color(0xFFAB47BC),  // Medium Purple
        Color(0xFF78909C),  // Grey
    )

    fun forCategory(categoryName: String): Color {
        val index = abs(categoryName.hashCode()) % palette.size
        return palette[index]
    }
}
```

### Spacing Constants

```kotlin
object KharchaSpacing {
    val screenPadding = 16.dp
    val cardPadding = 16.dp
    val cardCornerRadius = 16.dp
    val chipCornerRadius = 8.dp
    val listRowMinHeight = 64.dp
    val iconSize = 40.dp
    val smallIconSize = 16.dp
    val touchTarget = 48.dp
    val sectionSpacing = 16.dp
    val itemSpacing = 8.dp
}
```

---

## 9. Implementation Phases

### Phase 1: Foundation Fixes (1 PR, ~300 lines)
> *Unblock everything else. No visual change.*

- [ ] Make Home `PeriodSelector` actually filter by date range
- [ ] Fix hard-coded colors in `InsightsScreen.kt` → use theme tokens
- [ ] Add tabular figures (`tnum`) to typography
- [ ] Add `CategoryColors` palette to design system
- [ ] Add spacing constants

**Acceptance:** Period selector on Home changes displayed data. No hard-coded colors. CI green.

---

### Phase 2: Transaction Row Upgrade (1 PR, ~400 lines)
> *Every screen showing transactions gets better immediately.*

- [ ] Add category icon (colored circle) to `TransactionRow`
- [ ] Add review status indicator (✓ reviewed, ● low confidence)
- [ ] Add relative time display (12:08, Yesterday, 25 Oct)
- [ ] Update `CategoryBadge` to use `CategoryColors` palette
- [ ] Update `CategoryIconHelper` with subcategory icons

**Acceptance:** Transaction rows across Home, Transactions, Insights show category icons and status. CI green.

---

### Phase 3: Advanced Filters (1 PR, ~500 lines)
> *The biggest usability jump. Users can finally find what they need.*

- [ ] Create `TransactionFilter` data class
- [ ] Build `AdvancedFilterSheet` bottom sheet
- [ ] Add date range presets and custom calendar picker
- [ ] Add category multi-select filter
- [ ] Add amount range slider with preset brackets
- [ ] Add channel multi-select filter
- [ ] Add `ActiveFilterStrip` with clear-all
- [ ] Add live result counter on Apply button
- [ ] Integrate with `TransactionsScreen`

**Acceptance:** All filter dimensions work. Filters combine correctly. Live count updates as filters change. CI green.

---

### Phase 4: Charts & Dashboard (1–2 PRs, ~500 lines)
> *Home becomes a real dashboard.*

- [ ] Add Vico library dependency (state reason in PR)
- [ ] Build `DonutChart` component with tap interaction
- [ ] Build `IncomeExpenseBarChart` component with tap interaction
- [ ] Add category legend with tappable rows → drill to filtered Transactions
- [ ] Add `PercentChangeBadge` to `SummaryCard`
- [ ] Wire chart taps to navigate to Transactions with pre-applied filters

**Acceptance:** Home shows donut + bar chart. Tapping chart elements opens filtered Transactions. CI green.

---

### Phase 5: Transaction Detail Full Screen (1 PR, ~400 lines)
> *Replace dialog with a proper screen. Enable deep editing.*

- [ ] Create `TransactionDetailScreen` in `feature:transactions`
- [ ] Move from dialog to full-screen composable
- [ ] Add editable type/category/tags chips
- [ ] Add `CategoryPickerSheet` with search, suggestions, and apply-to-similar prompt
- [ ] Add expandable "Original SMS" card
- [ ] Add linked transaction banner
- [ ] Add overflow menu (split, mark as transfer, mark as ignored, delete)

**Acceptance:** Tapping a transaction anywhere opens full detail screen. Category picker with apply prompt works. CI green.

---

### Phase 6: Insights Tabs (1 PR, ~400 lines)
> *Organise the analytics into digestible sections.*

- [ ] Add tab row: Overview | Budgets | Recurring
- [ ] Move existing content into Overview tab
- [ ] Create Budgets tab with per-category budget cards and progress bars
- [ ] Create Recurring tab (already exists as `RecurringSubscriptionsCard` — extract and enhance)
- [ ] Add `InsightCard` component for generated insights
- [ ] Add month-over-month category comparison

**Acceptance:** Three tabs work. Content correctly separated. Budget progress bars use green/amber/red thresholds. CI green.

---

### Phase 7: Swipe Actions & Multi-Select (1 PR, ~350 lines)
> *Power-user features. Fast categorisation without opening detail.*

- [ ] Build `SwipeableTransactionRow` with Material 3 SwipeToDismiss
- [ ] Swipe right → quick category picker (top 3 chips inline)
- [ ] Swipe left → delete with Undo snackbar (5 seconds)
- [ ] Long-press → multi-select mode with checkboxes
- [ ] Build `MultiSelectBottomBar` (Categorise, Change Type, Delete)
- [ ] Haptic feedback on swipe threshold

**Acceptance:** Swipe works on Transactions screen. Multi-select with bulk categorise works. Undo works. CI green.

---

### Phase 8: More Screen & Polish (1 PR, ~300 lines)
> *Replace inline settings with proper grouped list.*

- [ ] Build proper `MoreScreen` with grouped sections
- [ ] Add section headers (Accounts, Organization, Data, Settings, About)
- [ ] Move export buttons from inline card to proper rows
- [ ] Add `SkeletonLoader` for all loading states (Home, Transactions, Insights)
- [ ] Add empty state illustrations for each screen
- [ ] Add swipe discovery hint animation (first use only)

**Acceptance:** More screen has all items from spec. Loading states show skeletons. CI green.

---

### Future Phases (separate PRs, post-redesign)

| Phase | Scope |
|-------|-------|
| Accounts Screen | Full `feature:accounts` implementation (FR-7) |
| Settings Screen | Full `feature:settings` with all groups (FR-12.7) |
| Room Database | Migrate from current repo to proper Room DAOs |
| Navigation Compose | Replace enum state switching with NavHost |
| App Lock | Biometric + PIN lock screen (FR-12.1) |
| Export Redesign | Full export with filters, date range, format picker (FR-12.3) |
| Review Full Screen | Migrate review queue from dialog to `feature:review` |

---

## Appendix: File Map

Key files that will be modified or created:

```
core/designsystem/
  component/
    TransactionRow.kt          ← UPDATE: category icon, status indicator
    AmountText.kt              ← UPDATE: tabular figures, abbreviation
    CategoryBadge.kt           ← UPDATE: use CategoryColors
    DonutChart.kt              ← NEW
    BarChart.kt                ← NEW
    PeriodSelector.kt          ← NEW
    BudgetProgressBar.kt       ← NEW
    SkeletonLoader.kt          ← NEW
    AmountRangeSlider.kt       ← NEW
    PercentChangeBadge.kt      ← NEW
    SwipeableTransactionRow.kt ← NEW
  theme/
    Color.kt                   ← UPDATE: add CategoryColors
    Theme.kt                   ← UPDATE: add custom typography with tnum
    Spacing.kt                 ← NEW

feature/home/
  ui/HomeScreen.kt             ← UPDATE: working period selector, charts

feature/transactions/
  ui/TransactionsScreen.kt     ← UPDATE: advanced filters, swipe, multi-select
  ui/TransactionDetailScreen.kt ← NEW (replaces dialog)
  ui/AdvancedFilterSheet.kt    ← NEW
  ui/CategoryPickerSheet.kt    ← NEW
  model/TransactionFilter.kt   ← NEW

feature/insights/
  InsightsScreen.kt            ← UPDATE: tabbed, fix colors
  BudgetsTab.kt                ← NEW
  RecurringTab.kt              ← NEW (extract from existing)
  InsightCardGenerator.kt      ← NEW

feature/accounts/
  AccountsScreen.kt            ← NEW (future)

feature/settings/
  SettingsScreen.kt            ← NEW (future)

app/
  MainActivity.kt              ← UPDATE: MoreScreen, navigation wiring
```
