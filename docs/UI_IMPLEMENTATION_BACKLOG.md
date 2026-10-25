# Kharcha UI Implementation Backlog & Task Breakdown

**Version:** 1.0  
**Status:** Ready for Implementation  
**Governing Rules:** `AGENTS.md` (≤600 lines/PR, 1 task/PR, tests included, zero hardcoded colors/strings, 100% offline)  
**Companion Docs:** `UI_REDESIGN_PLAN.md`, `UI_ADVANCED_FILTERS_SPEC.md`, `UI_SCREEN_LAYOUTS_AND_WIREFRAMES.md`  

---

## 1. Overview & Synthesis of Research Reports

This backlog operationalizes the 10 core recommendations from the UI/UX Research Report and resolves the architectural gaps identified in the Codebase Audit:

| Recommendation | Target Module | Task ID |
|---|---|---|
| **1. Channel / Payment Mode Filter** | `feature:transactions` | `UI-3.1` |
| **2. Saved Filter Presets** | `core:common`, `feature:transactions` | `UI-3.2` |
| **3. Amount Range Filter & Dual Slider** | `core:designsystem`, `feature:transactions` | `UI-3.1` |
| **4. Live Result Counter on Apply CTA** | `feature:transactions` | `UI-3.1` |
| **5. Merchant Search Autocomplete** | `feature:transactions` | `UI-2.2` |
| **6. Micro-Sparklines in Budget Cards** | `core:designsystem`, `feature:insights` | `UI-6.2` |
| **7. Privacy Eye Shutter (Balance Blur)** | `core:designsystem`, `feature:home` | `UI-1.2` |
| **8. Spend by Payment Mode Chart** | `feature:insights` | `UI-6.1` |
| **9. Skeleton Shimmer Loaders** | `core:designsystem` | `UI-1.3` |
| **10. Swipe Gesture Discovery Hints** | `core:designsystem`, `feature:transactions` | `UI-7.1` |

---

## 2. Sequenced PR Task Backlog

Every task is designed to be **<500 lines of code change**, completely self-contained, unit-tested, and verifiable with `./gradlew ktlintCheck detekt test`.

```
Phase 1: Foundation & Polish (UI-1.1 → UI-1.3)
   │
Phase 2: Clean Data Presentation & Row Anatomy (UI-2.1 → UI-2.2)
   │
Phase 3: Advanced Two-Tier Filters (UI-3.1 → UI-3.2)
   │
Phase 4: Dashboard & Charts (UI-4.1 → UI-4.2)
   │
Phase 5: Full Screen Transaction Detail (UI-5.1)
   │
Phase 6: 3-Tab Insights Architecture (UI-6.1 → UI-6.2)
   │
Phase 7: Swipe Actions & Multi-Select Batch Mode (UI-7.1 → UI-7.2)
   │
Phase 8: Settings, Review Queue & Polish (UI-8.1 → UI-8.2)
```

---

### Phase 1: Foundation Tokens & Polish

#### Task `UI-1.1`: Design Token Cleanup & Tabular Typography
- **Goal:** Eliminate hard-coded colors in `InsightsScreen.kt`, introduce tabular numbers (`tnum`) in typography, and create the 16-color category palette.
- **Requirements:** `UI_SPEC.md` §2.1, §2.2, `AGENTS.md`
- **Scope:**
  - Replace `Color(0xFF2E7D32)` and `Color(0xFFED6C02)` in `feature:insights` with `MaterialTheme.financialColors.income` and `.expense`.
  - Update `core:designsystem/theme/Theme.kt` to set `fontFeatureSettings = "tnum"` for numbers to align vertically.
  - Add `CategoryColors.kt` palette (16 distinguishable, color-blind safe colors).
- **Lines of code:** ~180 lines.

#### Task `UI-1.2`: Privacy Shutter (Balance Masking)
- **Goal:** Add an instant eye icon toggle (`👁`) in the top app bar across Home and Transactions to blur/mask balances (`₹ ••••••`).
- **Requirements:** `UI_SPEC.md` §5.11, Research Recommendation #7.
- **Scope:**
  - Add `isPrivacyModeEnabled` state in `UserPreferences`.
  - Update `AmountText` to render masked dots when privacy mode is active.
  - Add eye action icon to `TopAppBar` in `HomeScreen` and `TransactionsScreen`.
- **Lines of code:** ~220 lines.

#### Task `UI-1.3`: Skeleton Shimmer Loaders
- **Goal:** Create reusable shimmer loading placeholders for cards, transaction rows, and charts.
- **Requirements:** `UI_SPEC.md` §6, Research Recommendation #9.
- **Scope:**
  - Create `SkeletonLoader.kt` composable modifier with subtle tonal shimmer animation.
  - Add `TransactionRowSkeleton`, `SummaryCardSkeleton`, and `ChartSkeleton`.
- **Lines of code:** ~190 lines.

---

### Phase 2: Clean Data Presentation & Row Anatomy

#### Task `UI-2.1`: Upgraded `TransactionRow` with Icons and Verification Status
- **Goal:** Redesign `TransactionRow` to show 40dp colored circular category icon, normalized merchant name, bullet metadata, relative timestamp, and confidence/review badges.
- **Requirements:** `SRS.md` FR-6.1, `UI_SPEC.md` §5.3.
- **Scope:**
  - Update `TransactionRow.kt` to render leading category icon in tinted circle using `CategoryColors`.
  - Add subtitle with `Category • Channel • Account last 4`.
  - Add relative timestamp (`Today 12:08 PM`, `Yesterday`, `27 Sep`).
  - Add `parseConfidence < 0.70f` warning dot (`●`) and reviewed checkmark (`✓`).
- **Lines of code:** ~250 lines.

#### Task `UI-2.2`: Smart Search with Token Parsing & Merchant Autocomplete
- **Goal:** Enhance search bar to handle qualifiers (`>500`, `#food`, `@upi`, `card:5625`) and suggest merchant autocompletes.
- **Requirements:** `SRS.md` FR-6.2, Research Recommendation #5.
- **Scope:**
  - Implement `TransactionSearchParser` in `core:common` to parse query strings into tokens.
  - Add drop-down autocomplete popup for known merchants when typing.
- **Lines of code:** ~320 lines.

---

### Phase 3: Advanced Two-Tier Filters

#### Task `UI-3.1`: Advanced Filter Bottom Sheet & Live Counter CTA
- **Goal:** Build modal bottom sheet with date presets, multi-select categories, amount range slider, payment channels, and live match counter.
- **Requirements:** `SRS.md` FR-6.2, `UI_ADVANCED_FILTERS_SPEC.md`.
- **Scope:**
  - Implement `TransactionFilter` model.
  - Build `AdvancedFilterSheet.kt` with M3 FilterChips and range sliders.
  - Implement dynamic footer CTA: `"Show X Transactions (₹Total)"`.
  - Wire to `TransactionsScreen`.
- **Lines of code:** ~480 lines.

#### Task `UI-3.2`: Saved Filter Presets & Active Filter Strip
- **Goal:** Allow users to save named filter combinations (e.g. "UPI this month", "Large Expenses") and render an active filter bar with 1-tap clear.
- **Requirements:** Research Recommendation #2, `UI_SPEC.md` §5.3.
- **Scope:**
  - Build `ActiveFilterStrip.kt` showing summary of active filters with `[ ✕ ]` reset.
  - Persist saved filter presets in `UserPreferences`.
  - Add saved filter chips at start of horizontal chip row.
- **Lines of code:** ~350 lines.

---

### Phase 4: Dashboard & Charts

#### Task `UI-4.1`: Interactive Category Donut Chart & Drill-Down
- **Goal:** Add interactive category donut chart on Home displaying top 5 categories + others, with drill-down navigation to filtered transactions on tap.
- **Requirements:** `SRS.md` FR-8.3, FR-8.4, `UI_SPEC.md` §5.2.
- **Scope:**
  - Implement lightweight Canvas-based `DonutChart.kt` in `core:designsystem` (no external dependency needed).
  - Tapping slice or legend row triggers navigation to `TransactionsScreen` with category filter pre-selected.
- **Lines of code:** ~380 lines.

#### Task `UI-4.2`: 6-Month Income vs Expense Bar Chart & Working Period Selector
- **Goal:** Make Home period selector actually filter data and add 6-month comparative bar chart.
- **Requirements:** `SRS.md` FR-8.2, FR-8.3.
- **Scope:**
  - Refactor `PeriodSelector` to cycle date boundaries and feed filtered transactions to Home ViewModel.
  - Implement `IncomeExpenseBarChart.kt` (6 months bars with green income / orange expense).
  - Tapping a month drills into that month's transactions.
- **Lines of code:** ~420 lines.

---

### Phase 5: Full Screen Transaction Detail

#### Task `UI-5.1`: Dedicated Transaction Detail Screen & Category Sheet
- **Goal:** Replace `TransactionDetailDialog` with full-screen composable featuring full metadata, editable category sheet, note field, and expandable raw SMS card.
- **Requirements:** `SRS.md` FR-6.3, FR-6.4, `UI_SPEC.md` §5.4, §5.6.
- **Scope:**
  - Create `TransactionDetailScreen.kt` in `feature:transactions`.
  - Add editable chips for Type, Category, Account.
  - Add expandable raw SMS card with "Report wrong parse" button.
  - Add category picker sheet with "Apply to all past & future" rule prompt.
- **Lines of code:** ~490 lines.

---

### Phase 6: 3-Tab Insights Architecture

#### Task `UI-6.1`: Insights Tabbed Structure & Payment Channel Analytics
- **Goal:** Split `InsightsScreen` into 3 clean tabs (Overview, Budgets, Recurring) and add dedicated Payment Channel breakdown chart.
- **Requirements:** `UI_SPEC.md` §5.8, Research Recommendation #8.
- **Scope:**
  - Add M3 `TabRow` (Overview, Budgets, Recurring).
  - Move cash flow, heatmap, and merchants into `OverviewTab`.
  - Add `PaymentChannelChart` showing UPI vs Card vs NetBanking breakdown.
- **Lines of code:** ~380 lines.

#### Task `UI-6.2`: Budgets Tab with Traffic-Light Progress & Sparklines
- **Goal:** Build the Budgets tab featuring category budget cards with green/amber/red thresholds and pacing sparklines.
- **Requirements:** `SRS.md` FR-9.1, FR-9.2, Research Recommendation #6.
- **Scope:**
  - Create `BudgetsTab.kt`.
  - Add `BudgetProgressBar.kt` with rounded ends and pacing alerts ("₹X left for Y days").
  - Add micro-sparkline showing spend burn-rate.
- **Lines of code:** ~360 lines.

---

### Phase 7: Gestures & Multi-Select

#### Task `UI-7.1`: Swipe-to-Categorize & Swipe-to-Delete with Undo
- **Goal:** Implement swipe gestures on transaction rows with haptic feedback and first-time discovery hints.
- **Requirements:** `UI_SPEC.md` §5.3, Research Recommendation #10.
- **Scope:**
  - Implement `SwipeableTransactionRow.kt` using M3 `SwipeToDismissBox`.
  - Swipe right (`→`): reveals 3 top category chips for 1-tap categorisation.
  - Swipe left (`←`): removes item with 5-second `Snackbar` undo action.
  - Add subtle peek animation on first app launch to teach the gesture.
- **Lines of code:** ~340 lines.

#### Task `UI-7.2`: Multi-Select Action Mode & Bulk Categorization
- **Goal:** Support long-press selection of multiple transactions with a floating bottom action bar.
- **Requirements:** `SRS.md` FR-5.6, `UI_SPEC.md` §5.3.
- **Scope:**
  - Enable selection state in `TransactionsScreen`.
  - Render bottom action bar: `[ Categorize ]`, `[ Change Type ]`, `[ Mark Reviewed ]`, `[ Delete ]`.
  - Propagate bulk updates through repository.
- **Lines of code:** ~390 lines.

---

### Phase 8: Settings, Review Queue & Polish

#### Task `UI-8.1`: Full-Screen Review Queue
- **Goal:** Move review workflow from dialog to dedicated full-screen experience with card swipe and completion celebrations.
- **Requirements:** `SRS.md` FR-5.5, `UI_SPEC.md` §5.7.
- **Scope:**
  - Create `ReviewQueueScreen.kt` in `feature:review`.
  - Card-stack layout showing SMS snippet, amount, and top 3 suggested category chips.
  - Completion state displaying count of reviewed items and new merchant rules learned.
- **Lines of code:** ~410 lines.

#### Task `UI-8.2`: Structured More/Settings Screen
- **Goal:** Replace inline settings in `MainActivity.kt` with a structured settings list grouped into Accounts, Organization, Data, and Settings.
- **Requirements:** `SRS.md` FR-12.7, `UI_SPEC.md` §5.11.
- **Scope:**
  - Create `MoreScreen.kt` in `feature:settings`.
  - Grouped sections: General, Data & Export, Notifications, Security, About.
- **Lines of code:** ~320 lines.
