Software Requirements Specification: SMS Expense Tracker
Version 1.0 | Prepared by: Product Owner | Intended implementer: Jules (AI coding agent)
1. Introduction
1.1 Purpose
This document defines the requirements for an Android app that automatically reads bank, card and wallet SMS messages, extracts transaction details, separates income from expenses, and categorises each transaction with no manual entry.
1.2 Product Vision
Zero-effort money tracking. The user installs the app, grants SMS access once, and sees an accurate, categorised picture of where money comes from and where it goes. All processing happens on the device.
1.3 Scope
In scope: SMS ingestion, parsing, income/expense classification, auto-categorisation with learning, accounts, budgets, analytics, export and backup, app lock. Out of scope (v1): iOS (iOS does not allow apps to read SMS), bank account linking via APIs, bill payments, multi-user sharing, cloud sync, investment tracking.
1.4 Key Assumptions
• A1: Platform is Android 8.0 (API 26) and above. SMS reading is not possible on iOS.
• A2: Primary market is India (INR, UPI, bank alerts). Currency, sender formats and categories must be configurable so other regions can be added later.
• A3: Fully offline and on-device. No SMS content leaves the phone.
• A4: The app is distributed via direct APK first. Google Play restricts SMS permissions to approved use cases, so Play release needs a Permissions Declaration (see Section 9).
1.5 Definitions
• Transaction: one money movement parsed from an SMS.
• Debit/Expense: money leaving an account. Credit/Income: money entering an account.
• Transfer: money moved between the user's own accounts. Not income or expense.
• Confidence score: 0 to 1 value showing how sure the parser or categoriser is.
2. Users and Personas
• Salaried professional: wants monthly spend by category and salary tracking.
• Student or freelancer: irregular income, wants budgets and savings visibility.
• Household manager: many small UPI and cash-like payments, wants merchant-level insight.
3. Functional Requirements
Priority: M = Must (v1), S = Should (v1.1), C = Could (later).
3.1 Onboarding and Permissions
• FR-1.1 (M) Show a clear privacy explanation before requesting READ_SMS and RECEIVE_SMS permissions.
• FR-1.2 (M) Ask for POST_NOTIFICATIONS on Android 13+.
• FR-1.3 (M) If SMS permission is denied, allow manual entry mode and show a persistent banner to enable it.
• FR-1.4 (M) Let the user choose the default currency and optionally the SMS history window to import (30, 90, 180 days, All).
• FR-1.5 (S) Let the user pick which senders or accounts to track after the first scan.
3.2 SMS Ingestion
• FR-2.1 (M) Real-time capture: a BroadcastReceiver for incoming SMS triggers parsing within 5 seconds.
• FR-2.2 (M) Historical import: read the SMS inbox via ContentResolver in batches using a background worker, with a progress indicator. The app must stay responsive.
• FR-2.3 (M) Sender filtering: only process messages from financial senders (alphanumeric sender IDs such as XX-HDFCBK, VM-SBIUPI, and user-added senders). Ignore personal numbers.
• FR-2.4 (M) Deduplication: the same SMS must never create two transactions. Use a hash of sender + body + timestamp, plus the bank reference number when present.
• FR-2.5 (M) Store only the minimum needed: a reference to the SMS, parsed fields, and the raw body in encrypted local storage (user can disable raw body storage).
• FR-2.6 (M) Reliability: ingestion must survive reboot, app kill and Doze mode (use WorkManager for retries).
• FR-2.7 (S) Dual SIM support: record which SIM or subscription received the SMS.
3.3 SMS Parsing Engine
• FR-3.1 (M) Extract these fields where present: amount, currency, direction (debit or credit), transaction date and time, account or card last 4 digits, merchant or payee or payer name, payment channel (UPI, card, NEFT, IMPS, RTGS, ATM, net banking, wallet, cheque, auto-debit), available balance, bank reference or UTR number, UPI ID.
• FR-3.2 (M) Use a pluggable, per-bank template and regex system. Templates are data (JSON), not hard-coded, so new banks are added without code changes.
• FR-3.3 (M) Fallback generic parser using keyword and pattern heuristics when no template matches (keywords such as debited, credited, spent, sent, received, withdrawn, paid, deposited, Rs, INR).
• FR-3.4 (M) Must IGNORE and not create transactions for: OTPs, promotional offers, loan or credit offers, balance-only enquiries, statement or due-date reminders, minimum due alerts, failed or declined transactions, and mandate registration notices. Keep them in an Ignored log (with reason) for debugging.
• FR-3.5 (M) Handle failed and reversed cases: a debit followed by a reversal or refund must be linked and the refund shown as a credit tagged Refund, not as Income.
• FR-3.6 (M) Handle amount formats: Rs.1,234.50, INR 1234, Rs 1.2L-style should be rejected as low confidence, and negative or zero amounts rejected.
• FR-3.7 (M) Each parsed transaction carries a parse confidence. Below a threshold (default 0.6) it goes to the Review Queue.
• FR-3.8 (S) Pending-message handling: if an SMS has a date but no time, use SMS received time.
• FR-3.9 (C) Support regional language SMS and other countries via template packs.
3.4 Income vs Expense Classification
• FR-4.1 (M) Every transaction has a type: Expense, Income, Transfer, Refund, Investment, or Ignored.
• FR-4.2 (M) Default mapping: debit = Expense, credit = Income, with the overrides below.
• FR-4.3 (M) Own-account transfers: if a debit and a credit of the same amount occur within a configurable window (default 10 minutes) across two of the user's accounts, mark both as Transfer and exclude them from income and expense totals.
• FR-4.4 (M) Credit card bill payments: a debit from a bank account paid to a card, together with the matching card payment-received credit, is a Transfer and must not double count spending.
• FR-4.5 (M) Credit card spends count as Expense at the time of spend, not at bill payment.
• FR-4.6 (M) Salary detection: credits with keywords such as salary, SAL, payroll, or a recurring monthly credit of similar amount from the same payer are tagged Income > Salary.
• FR-4.7 (S) Cashback, interest and refunds from the same merchant within 90 days of a debit are labelled Cashback, Interest, or Refund respectively.
• FR-4.8 (M) The user can change the type of any transaction and choose to apply the same change to similar past and future transactions.
3.5 Smart Categorisation
Categorisation uses a layered pipeline, applied in this order. The first layer that produces a result above its confidence threshold wins.
1. User rules: explicit rules created by the user (for example, merchant contains Swiggy = Food Delivery).
2. Learned mappings: merchant-to-category pairs learned from the user's past corrections (same merchant normalised name or UPI ID).
3. Built-in merchant dictionary: a bundled, versioned JSON of well-known merchants (Swiggy, Zomato, Uber, Amazon, Netflix, IRCTC, electricity boards, fuel brands, etc.) mapped to categories.
4. Keyword and channel heuristics: for example ATM channel = Cash Withdrawal, words like EMI = Loans, insurance, recharge, electricity.
5. On-device ML classifier (TensorFlow Lite or equivalent) using merchant text, amount band and channel, used only when layers 1 to 4 fail.
6. Fallback: Uncategorised, placed in the Review Queue.
Requirements:
• FR-5.1 (M) Merchant name normalisation: strip prefixes and noise (UPI/, VPA, POS, numbers, city names) so that SWIGGY*ORDER 8432 and Swiggy map to one merchant.
• FR-5.2 (M) Every categorised transaction stores category, subcategory, source layer, and confidence.
• FR-5.3 (M) Default category tree (see Section 6), user-editable: add, rename, merge, hide, set icon and colour.
• FR-5.4 (M) Manual recategorisation with a prompt: Apply to all past transactions from this merchant? Apply to future ones too? The choice creates a user rule.
• FR-5.5 (M) Review Queue screen listing low-confidence and Uncategorised items, with one-tap category suggestions (top 3) and swipe actions.
• FR-5.6 (S) Bulk edit: select multiple transactions and set category, type or tags.
• FR-5.7 (S) Rule manager: list, edit, reorder and disable rules, with a counter of transactions each rule matched.
• FR-5.8 (S) Peer-to-peer UPI payments to individuals (no merchant): ask once and remember per payee. Suggest the last used category for that payee.
• FR-5.9 (C) Optional, opt-in LLM-assisted categorisation using only the merchant name (never the full SMS or amount). Off by default.
• FR-5.10 (M) Quality target: at least 85 percent of transactions auto-categorised correctly on the test corpus (Section 8), rising with user corrections.
3.6 Transactions
• FR-6.1 (M) Chronological list grouped by day with amount, merchant, category, account, and an income/expense colour cue.
• FR-6.2 (M) Search by merchant, note, amount or category. Filter by date range, type, category, account, amount range, and tag.
• FR-6.3 (M) Detail view showing all parsed fields, the original SMS (if stored), parse confidence and categorisation source, with an Edit option.
• FR-6.4 (M) Edit any field. Add a note and tags. Delete or mark as Ignored. Restore from Ignored.
• FR-6.5 (M) Manual entry for cash and non-SMS transactions.
• FR-6.6 (S) Split a transaction across multiple categories.
• FR-6.7 (S) Report a wrong parse: lets the user flag an SMS as incorrectly parsed. The sample is stored locally for export as an anonymised bug report (user-reviewed before sharing).
• FR-6.8 (S) Undo for deletes and bulk edits.
3.7 Accounts
• FR-7.1 (M) Accounts are auto-created from SMS (bank + last 4 digits). Types: Savings, Current, Credit Card, Wallet, Cash.
• FR-7.2 (M) Show the latest known balance per account from SMS balance fields, with a last-updated timestamp.
• FR-7.3 (M) Rename, merge, archive an account. Merging reassigns transactions.
• FR-7.4 (S) Credit card view: outstanding, available limit and due date when present in SMS.
• FR-7.5 (S) Net worth view: sum of balances minus card dues.
3.8 Dashboard and Analytics
• FR-8.1 (M) Home dashboard for the selected period (this month by default): total income, total expense, net savings, savings rate, top 5 categories.
• FR-8.2 (M) Period selector: day, week, month, year, custom range. Month boundaries configurable (for example start on salary day).
• FR-8.3 (M) Charts: category donut, income vs expense bar by month, daily spend trend line.
• FR-8.4 (M) Drill-down from any chart segment to the filtered transaction list.
• FR-8.5 (S) Top merchants, largest transactions, and spend by account and by payment channel.
• FR-8.6 (S) Insights: month-on-month change by category, unusual spend detection (for example a transaction more than 3x category average), and weekday spend pattern.
• FR-8.7 (C) Home screen widget showing the month's spend and budget status.
3.9 Budgets and Goals
• FR-9.1 (M) Set a monthly budget per category and an overall monthly budget.
• FR-9.2 (M) Progress bars and notifications at 80 percent and 100 percent of budget.
• FR-9.3 (S) Rollover of unused budget (optional per category).
• FR-9.4 (S) Savings goals with target amount and date.
• FR-9.5 (C) Suggested budgets based on the last 3 months of spending.
3.10 Recurring and Subscriptions
• FR-10.1 (S) Detect recurring payments (same merchant, similar amount, regular interval) and list them as Subscriptions and Bills.
• FR-10.2 (S) Upcoming bill reminders and an alert when a subscription amount changes.
• FR-10.3 (S) Monthly total of recurring commitments.
3.11 Notifications
• FR-11.1 (M) Optional per-transaction notification (for example: Spent 450 at Swiggy, categorised as Food Delivery) with quick actions: Change category and Not an expense.
• FR-11.2 (S) Daily or weekly summary notification.
• FR-11.3 (M) All notification types individually toggleable.
3.12 Data Management, Security and Settings
• FR-12.1 (M) App lock with biometric or PIN. Hide balances in the Recents view.
• FR-12.2 (M) Local database encrypted at rest (SQLCipher or equivalent, key in Android Keystore).
• FR-12.3 (M) Export to CSV and JSON, with date range and filters.
• FR-12.4 (M) Encrypted local backup and restore to a user-chosen file or location (Storage Access Framework).
• FR-12.5 (S) Optional encrypted backup to the user's own Google Drive.
• FR-12.6 (M) Delete all data from the settings screen. Include a clear data-retention explanation.
• FR-12.7 (M) Settings: currency, number format, month start day, theme (light, dark, system), tracked senders, ignore list, confidence threshold, raw SMS storage toggle.
• FR-12.8 (M) Zero network permissions required for core features. If optional features add network access, they must be opt-in and listed in the privacy screen.
4. Non-Functional Requirements
• NFR-1 Performance: parse one SMS in under 100 ms. Import 10,000 messages in under 2 minutes on a mid-range device without UI jank. Cold start under 2 seconds.
• NFR-2 Accuracy: parser precision of at least 95 percent and recall of at least 90 percent for amount and direction on supported banks. Zero tolerance for creating a transaction from an OTP.
• NFR-3 Privacy: all processing on device. No analytics that include SMS content. No third-party trackers.
• NFR-4 Reliability: no lost or duplicated transactions across reboot, app update, or permission changes. Database migrations are tested.
• NFR-5 Battery: ingestion is event-driven, with no polling or long-running foreground service.
• NFR-6 Usability: core flows reachable in 3 taps. Supports dark mode, font scaling and TalkBack accessibility labels.
• NFR-7 Compatibility: Android 8.0 to current. Phones and tablets, portrait first.
• NFR-8 Maintainability: parser templates and merchant dictionary live in versioned JSON assets. Modules are unit-testable in isolation.
• NFR-9 Localisation: strings externalised, English in v1, Hindi as an early follow-up.
• NFR-10 Test coverage: at least 80 percent unit test coverage on the parsing, classification and categorisation modules.
5. Data Model (Room entities)
• Transaction: id, smsHash (unique), accountId, type, amount (stored as minor units, long), currency, dateTime, merchantRaw, merchantNormalised, categoryId, subcategoryId, categorySource, categoryConfidence, parseConfidence, channel, refNumber, upiId, balanceAfter, note, isReviewed, linkedTransactionId, createdAt, updatedAt.
• Account: id, name, bankName, last4, type, currentBalance, balanceUpdatedAt, isArchived.
• Category: id, name, parentId, kind (expense or income), icon, colour, isDefault, isHidden.
• Rule: id, matchField, matchOperator, matchValue, targetCategoryId, targetType, priority, isEnabled, hitCount.
• MerchantMapping: merchantNormalised, categoryId, confirmedCount.
• Budget: id, categoryId (null for overall), amount, period, rolloverEnabled.
• Goal: id, name, targetAmount, targetDate, savedAmount.
• SmsSender: id, senderId, bankName, isEnabled, isUserAdded.
• IgnoredMessage: id, smsHash, reason, receivedAt.
• Tag and TransactionTag: many-to-many.
Rule: store money as integer minor units to avoid floating-point errors.
6. Default Categories
Expense: Food and Dining (Groceries, Restaurants, Food Delivery, Coffee), Transport (Fuel, Cab and Auto, Public Transit, Parking and Toll), Shopping (Online, Clothing, Electronics, Home), Bills and Utilities (Electricity, Water, Gas, Mobile and Internet, DTH), Rent and Housing, Health (Pharmacy, Doctor, Insurance), Education, Entertainment (Movies, Streaming, Games), Travel (Flights, Hotels, Trains), Personal Care, Subscriptions, Loans and EMI, Insurance, Investments, Gifts and Donations, Fees and Charges, Cash Withdrawal, Family and Friends (UPI transfers to individuals), Taxes, Others. Income: Salary, Business, Freelance, Interest, Dividends, Refund, Cashback, Rent Received, Gift Received, Other Income. Special types (not categories): Transfer, Ignored.
7. SMS Parsing Examples (expected behaviour)
These are synthetic examples and must be turned into unit tests.
1. Rs 450.00 debited from A/c XX1234 on 22-10-26 to VPA swiggy@icici (UPI Ref 6128...). -> Expense, 450.00, account ending 1234, channel UPI, merchant Swiggy, category Food Delivery.
2. INR 85,000.00 credited to A/c XX1234 on 01-10-26 by NEFT from ACME TECH PVT LTD SAL. -> Income, 85000.00, channel NEFT, category Salary.
3. Your OTP for transaction is 482913. Do not share. -> Ignored (OTP).
4. Rs 1,299 spent on Credit Card XX9876 at AMAZON on 20-Oct. Avl limit Rs 48,701. -> Expense, card account, category Shopping, available limit captured.
5. Rs 20,000 debited from A/c XX1234 towards Credit Card XX9876 payment, and Rs 20,000 received on Card XX9876. -> Both Transfer, excluded from totals.
6. Transaction of Rs 600 declined due to insufficient funds. -> Ignored (failed).
7. Get 5 percent cashback on your next recharge! Use code SAVE5. -> Ignored (promotional).
8. Rs 2,000 withdrawn at ATM on 18-10-26. Avl Bal Rs 12,340. -> Expense, channel ATM, category Cash Withdrawal, balance captured.
8. Quality, Testing and Acceptance
• A corpus of at least 300 anonymised or synthetic SMS across at least 10 major banks, wallets and UPI apps, with expected outputs, is the test oracle. Include edge cases: missing time, different date formats, multi-line SMS, lakh or comma formats, and regional senders.
• Unit tests for parser, classifier, categoriser pipeline, merchant normaliser and dedupe.
• Instrumented tests for the SMS receiver, import worker, Room migrations and the permission flows.
• UI tests for onboarding, the review queue, recategorise-and-apply-rule, and budget alerts.
• Release acceptance: all Must requirements pass, NFR-2 targets are met on the corpus, no crashes in a 48-hour soak test, and a clean install handles a 10,000 SMS import.
9. Compliance and Policy
• Google Play restricts SMS and Call Log permissions. Expense management by reading transactional SMS may be approved only through the Permissions Declaration Form, with a core-functionality justification. Plan for a rejection: keep a direct-APK and F-Droid path.
• Provide a clear in-app privacy policy and a Data Safety form that states SMS is processed on-device only.
• Do not read or store non-financial SMS. Filtering must happen before any storage.
• Handle user-requested deletion instantly and completely.
10. Suggested Technical Approach (for Jules)
• Language and UI: Kotlin, Jetpack Compose, Material 3.
• Architecture: MVVM with clean architecture layers (data, domain, presentation). Single-activity, Navigation Compose.
• Storage: Room with SQLCipher. DataStore for settings.
• DI: Hilt. Async: Coroutines and Flow. Background: WorkManager.
• Modules: app, core-model, core-database, feature-sms-ingest, feature-parser, feature-categorizer, feature-transactions, feature-dashboard, feature-budgets, feature-settings.
• Parser: template engine reading bank templates from assets/templates/*.json, each with sender patterns, regexes with named groups, and direction mapping.
• Categoriser: a CategorizationPipeline interface with ordered Layer implementations, so layers are individually testable and replaceable.
• Charts: Vico or MPAndroidChart.
• CI: GitHub Actions running lint (ktlint, detekt), unit tests and a debug build on every PR.
11. Delivery Plan and Jules Task Breakdown
Each task should be one small PR with tests, and each must reference requirement IDs.
• Milestone 0, Foundation: project setup, modules, DI, theme, Room schema and migrations, CI. (NFR-8)
• Milestone 1, Core Ingestion: permission onboarding, SMS receiver, inbox import worker, sender filter, dedupe, ignored log. (FR-1, FR-2)
• Milestone 2, Parsing: template engine, generic fallback parser, 10 bank templates, OTP and promo rejection, test corpus. (FR-3)
• Milestone 3, Classification: income, expense, transfer and refund logic, account auto-creation and balances. (FR-4, FR-7)
• Milestone 4, Categorisation: merchant normaliser, dictionary, rules, learned mappings, review queue, recategorise-and-apply. (FR-5)
• Milestone 5, Transactions UI: list, search and filters, detail, edit, manual add. (FR-6)
• Milestone 6, Dashboard: period selector, summary cards, charts, drill-down. (FR-8)
• Milestone 7, Budgets and Notifications: budgets, alerts, transaction notifications. (FR-9, FR-11)
• Milestone 8, Data and Security: app lock, encryption, export, backup and restore, delete all. (FR-12)
• Milestone 9, v1.1: recurring detection, insights, split transactions, goals, widget, on-device ML layer.
MVP = Milestones 0 to 6 plus FR-12.1 to 12.4.
12. Risks
• Bank SMS formats change often. Mitigation: data-driven templates, generic fallback, in-app report-a-wrong-parse flow.
• Play Store SMS policy rejection. Mitigation: direct APK distribution path.
• Mis-categorised data reduces trust. Mitigation: confidence-based review queue and a learn-from-correction loop.
• OEM battery optimisation may block receivers. Mitigation: onboarding guidance and a daily catch-up scan of the inbox for missed messages.
• Double counting of transfers and card payments. Mitigation: FR-4.3 to 4.5 with dedicated tests.
13. Open Questions
1. Which country and banks must be supported first? India
2. Is Google Play distribution mandatory for v1? no
3. Is the cloud backup to Google Drive wanted, given the privacy-first positioning? no
4. Should the app support multiple currencies and conversion? rupee is fine now
5. Is an optional LLM layer acceptable, or must the app stay strictly offline? offline now