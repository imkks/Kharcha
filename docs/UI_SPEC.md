UI/UX Design Specification: SMS Expense Tracker
Version 1.0 | Companion to the SRS | Platform: Android, Jetpack Compose, Material 3
Requirement IDs (FR-x) refer to the SRS. Every screen below lists the requirements it satisfies, so a coding agent can trace UI to behaviour.
1. Design Principles
1. Glanceable first: the answer to how much did I spend and earn this month must be visible without scrolling.
2. Zero-effort, but never silent: automation does the work, and low-confidence items are surfaced gently in a Review Queue instead of being hidden.
3. Income and expense are instantly distinguishable using colour, sign and icon, never colour alone.
4. Corrections must be 2 taps or fewer, and every correction offers to apply to similar transactions.
5. Calm and trustworthy: a finance app should feel private, not noisy. No ads, no dark patterns, minimal animation.
6. Offline by default: no spinners waiting on a network. Only on-device progress (import, export, backup).
2. Design Tokens
2.1 Colour (Material 3 roles; provide light and dark)
• Primary: deep teal (#0F766E light, #5EEAD4 dark). Used for primary actions and selected states.
• Income: green (#15803D light, #4ADE80 dark). Always paired with a plus sign and an arrow-down-left icon.
• Expense: red-orange (#C2410C light, #FB923C dark). Always paired with a minus sign and an arrow-up-right icon.
• Transfer: neutral blue-grey (#475569 light, #94A3B8 dark), paired with a swap icon.
• Warning (budget 80 percent): amber (#B45309 light, #FBBF24 dark). Danger (budget exceeded): red (#B91C1C light, #F87171 dark).
• Surfaces: use Material 3 surface, surfaceVariant and surfaceContainer roles. No hard-coded greys.
• Category colours: a fixed palette of 16 distinguishable colours, colour-blind safe, assigned per category and user editable.
2.2 Typography
• Font: Roboto Flex or the system default. Numbers use tabular figures so amounts align.
• Scale: Display (hero totals) 36sp bold; Headline 24sp; Title 18sp medium; Body 16sp; Label 12sp. All sizes scale with system font size up to 200 percent.
2.3 Spacing, shape and elevation
• 4dp grid; screen padding 16dp; card padding 16dp; list row height at least 64dp.
• Corner radius: cards 16dp, chips 8dp, buttons full pill, bottom sheets 28dp top corners.
• Minimum touch target 48 by 48dp.
• Elevation by tonal colour, not heavy shadows.
2.4 Money formatting
• Default locale format for INR: Rs 1,23,456.50 (Indian digit grouping), configurable.
• Large totals on dashboard may abbreviate (12.4K, 1.2L) with the full value on tap or long-press.
• Income shows +, expense shows a minus sign, transfers show no sign and a neutral colour.
3. Information Architecture
Bottom navigation with 4 destinations plus a floating action button:
1. Home (dashboard)
2. Transactions
3. Insights (analytics, budgets, recurring)
4. More (accounts, categories and rules, settings, export and backup)
FAB (global): Add transaction (manual entry). Hidden on Insights and More. Top-bar actions: search (Transactions), period selector (Home, Insights), notification bell is not used. Review Queue is reached from a badge card on Home and from the Transactions filter row. It is not a tab.
4. Key User Flows
4.1 First run
Splash -> Welcome (value proposition) -> Privacy explainer -> SMS permission request -> Notification permission -> Choose import window and currency -> Import progress -> Review summary -> Home.
4.2 New SMS arrives
SMS received -> parsed and categorised silently -> optional notification with Change category and Not an expense actions -> transaction appears at the top of Transactions and updates Home totals.
4.3 Correct a category
Tap transaction -> Detail -> tap category chip -> category picker sheet -> select -> sheet asks Apply to all from this merchant? (This one only / All past and future) -> snackbar with Undo.
4.4 Clear the Review Queue
Home card (12 need review) -> Review Queue -> swipe-card or list with top 3 suggested categories as chips -> tap chip to accept and move to next -> completion state.
5. Screen Specifications
5.1 Onboarding (FR-1)
• Screen 1 Welcome: illustration, headline Track every rupee automatically, subtext about reading bank SMS on your phone only. Primary button Get started.
• Screen 2 Privacy: three icon rows (Stays on your device, Only bank messages are read, You can delete everything anytime). Link to full policy. Button Continue.
• Screen 3 Permission rationale: explains why READ_SMS and RECEIVE_SMS are needed, then triggers the system dialog. If denied: shows Use manual mode and Open settings options, never blocks the app.
• Screen 4 Setup: currency dropdown, import window segmented buttons (30 days, 90 days, 6 months, All), toggle Store original SMS text.
• Screen 5 Import progress: determinate progress bar, live counter (Found 214 transactions), animated list of the latest 3 parsed items, Skip to app option as the import continues in the background.
5.2 Home / Dashboard (FR-8, FR-9)
Layout, top to bottom:
1. Top bar: app name left, period selector chip (October 2026) right with prev and next arrows.
2. Summary card: large Net amount (Income minus Expense) as the hero number; two sub-tiles below, Income (green) and Expense (orange), each with the percentage change versus the previous period.
3. Review card (only if the queue is not empty): X transactions need a quick look, with a button Review.
4. Budget card: overall budget progress bar with amount left and days left; tap goes to Budgets.
5. Category donut chart with the top 5 categories as a legend; tap a segment to open the filtered Transactions list.
6. Income vs Expense mini bar chart for the last 6 months.
7. Recent transactions (5 rows) with See all. States: Empty (no data yet) shows an illustration and Import SMS now or Add manually. Loading uses skeleton shimmer cards.
5.3 Transactions list (FR-6)
• Sticky top bar with search icon (expands into a search field) and filter icon.
• Filter chip row: Period, Type (All, Expense, Income, Transfer), Category, Account, Needs review. Active filters show as filled chips with a clear-all control.
• Summary strip under the filters: Income, Expense and Net for the filtered set.
• List grouped by day with sticky date headers showing the day total.
• Row anatomy: leading category icon in a coloured circle; title (merchant); subtitle (category, account last 4, channel); trailing signed amount; a small warning dot if low confidence.
• Interactions: tap opens detail; long-press enters multi-select with a bottom action bar (Categorise, Change type, Tag, Delete); swipe right to categorise, swipe left to delete with Undo snackbar.
• Empty filter result: No transactions match. Button Clear filters.
5.4 Transaction Detail (FR-6.3, FR-6.4)
• Header: big amount with type colour, merchant name, date and time.
• Editable chips: Type, Category, Account, Tags.
• Details list: Channel, UPI ID or reference number, Balance after, Parse confidence, Category source (for example Learned from your rule).
• Note field (inline editable).
• Expandable card: Original SMS (hidden if raw storage is off), with a button Report wrong parse.
• Actions in the overflow: Split, Mark as transfer, Mark as ignored, Delete.
• Linked item banner when a refund or transfer pair exists, tappable to jump to the counterpart.
5.5 Add / Edit Transaction (FR-6.5)
Bottom sheet or full screen with: segmented control (Expense, Income, Transfer), large amount input with numeric keypad, category selector (grid of icons), account selector, date and time picker (defaults to now), note, tags. Primary button Save. Transfer mode shows From and To accounts instead of a category.
5.6 Category Picker (FR-5.3, FR-5.4)
Modal bottom sheet. Top: search field. Then a Suggested row (top 3 predicted categories as chips), then a grid of all categories grouped by parent, expandable to subcategories. Bottom: New category. After a selection in an edit flow, show the Apply to similar transactions prompt as a second small sheet with two radio options and a confirm button.
5.7 Review Queue (FR-5.5)
• Header: X remaining with a progress indicator.
• Card-stack or list layout; each item shows merchant, amount, date, the original SMS snippet, and the three suggested category chips plus Other.
• Actions: tap chip = accept; Not an expense = mark Ignored; Skip.
• Completion state: a friendly all-done illustration with the number of items reviewed and a count of new rules learned.
5.8 Insights (FR-8.5, 8.6, FR-9, FR-10)
Tabs at the top: Overview, Budgets, Recurring.
• Overview: category breakdown (donut plus ranked list with percentage bars), month-over-month comparison per category, top merchants, spend by weekday heatmap, spend by account, and generated insight cards (for example Food Delivery is up 32 percent vs last month).
• Budgets: list of budget cards with progress bars (green under 80 percent, amber 80 to 100, red over). Add budget FAB. Editing sets the amount and rollover toggle.
• Recurring: list of detected subscriptions and bills with next due date, amount, and a monthly total at the top. Each row has Confirm, Not recurring actions.
5.9 Accounts (FR-7)
• List of account cards: bank logo or initial, name, last 4, type, latest balance with a last updated label.
• Credit card cards additionally show used versus limit as a bar, and the due date.
• Header summary: Total balance and Net worth.
• Account detail: filtered transactions, rename, merge, archive.
5.10 Categories and Rules (FR-5.3, FR-5.7)
• Categories: two tabs, Expense and Income. Reorder by drag; tap to edit name, icon, colour, parent; hide toggle.
• Rules: list with a plain-language summary (If merchant contains Swiggy then category Food Delivery), hit count, and an enable switch. Add rule form with field, operator, value and target.
5.11 Settings (FR-12)
Grouped list: General (currency, number format, month start day, theme); SMS (tracked senders, ignored messages log, confidence threshold, store raw SMS toggle, re-scan inbox); Notifications (per-transaction, budget alerts, daily summary); Security (app lock, hide balances in recents); Data (export CSV or JSON, backup, restore, delete all data); About (version, privacy policy, open source licences).
5.12 Export and Backup (FR-12.3, 12.4)
Form with format, date range and include-filters options, then a Save to location action using the system file picker. A progress dialog is shown for large exports and a snackbar confirms completion. Backup shows the last backup date and a warning if it is older than 30 days.
5.13 App Lock
Full-screen lock with biometric prompt and PIN fallback, shown on cold start and after a configurable timeout (immediately, 1, 5 minutes).
6. Reusable Components
• TransactionRow, AmountText (applies sign, colour and tabular figures), CategoryIcon, CategoryChip, TypeBadge, PeriodSelector, SummaryCard, BudgetProgressBar, DonutChart, BarChart, EmptyState, ErrorState, SkeletonLoader, ConfirmApplySheet, CategoryPickerSheet, AccountCard, InsightCard, PermissionBanner. Each component is built as a stateless composable with a preview for light, dark, large-font and RTL.
7. Notifications
• Transaction notification: title Spent Rs 450 at Swiggy; text Food Delivery, HDFC ending 1234; actions Change category and Not an expense. Grouped per day, silent by default (no sound).
• Budget alert: title Food budget at 80 percent; text Rs 1,200 left for 9 days.
• Daily summary (optional): Today you spent Rs 830 across 4 transactions.
• Notification channels: Transactions, Budgets, Summaries, each individually controllable.
• Notifications must not show the full amount on the lock screen if hide balances is on.
8. Interaction, Motion and Feedback
• Transitions: shared-element transition from list row to detail; 200 to 300 ms standard easing. Respect the system reduce-motion setting.
• Every destructive action uses a snackbar with Undo for at least 5 seconds.
• Haptic feedback on category acceptance in the Review Queue and on budget threshold crossing.
• Pull to refresh on Transactions triggers an inbox re-scan for missed messages.
9. Accessibility
• WCAG AA colour contrast in both themes.
• All icons have content descriptions; amounts are read as Expense, 450 rupees, Swiggy (not symbols).
• Charts provide a text or table alternative and a summary description.
• Supports TalkBack, Switch Access, font scaling to 200 percent and a high-contrast mode.
• Do not rely on colour alone: income and expense use signs and icons as well.
10. Content and Tone
• Friendly, short, never judgemental about spending. Use You and plain language.
• Error copy states what happened and what to do next (for example We could not read some messages. Tap to review them).
• Avoid jargon such as parse; use read or detected in user-facing text.
• All strings go in resource files for localisation (English v1, Hindi next).
11. Screen States Checklist
For every screen, implement and preview: loading, empty, populated, error, permission denied, offline-irrelevant (not applicable), large font, dark mode and landscape or tablet width.
12. Wireframe Deliverables for the Coding Agent
The agent should implement these as Compose screens in this order, each with previews and screenshot tests: Onboarding set, Home, Transactions list, Transaction detail, Category picker with apply sheet, Review Queue, Add transaction, Insights tabs, Accounts, Settings, Categories and Rules, Export and Backup, App lock.
13. Open Design Questions
1. Should the app default to dark mode or follow the system?
2. Do you want a bottom-sheet or full-screen layout for manual entry?
3. Should net savings or total expense be the hero number on Home?