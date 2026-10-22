Technical Design and Jules Task Plan: SMS Expense Tracker
Version 1.0 | Companion to the SRS and the UI/UX Specification
Purpose: give the coding agent (Jules) unambiguous architecture, contracts and a sequenced task list. Every task cites SRS requirement IDs and has acceptance criteria.
1. Technology Stack
• Language: Kotlin 2.x. Min SDK 26, target SDK latest stable.
• UI: Jetpack Compose, Material 3, Navigation Compose.
• Architecture: MVVM + clean architecture. Unidirectional data flow (UiState, UiEvent, ViewModel with StateFlow).
• DI: Hilt. Async: Coroutines and Flow. Background: WorkManager.
• Persistence: Room, encrypted with SQLCipher (key kept in Android Keystore). Settings in DataStore.
• Serialisation: kotlinx.serialization for JSON assets.
• ML (later milestone): TensorFlow Lite for the on-device classifier.
• Charts: Vico (Compose native).
• Testing: JUnit5, MockK, Turbine, Robolectric, Compose UI tests, Paparazzi or Roborazzi for screenshots.
• Quality: ktlint, detekt, Android Lint, GitHub Actions CI.
2. Module Structure
• app: Application class, MainActivity, navigation graph, Hilt setup.
• core:model: pure Kotlin domain models (Transaction, Account, Category, Rule, Budget).
• core:database: Room entities, DAOs, migrations, encryption setup.
• core:common: Money type, date utilities, dispatchers, Result wrapper.
• core:designsystem: theme, tokens, shared composables (AmountText, CategoryIcon, and so on).
• feature:onboarding
• feature:home
• feature:transactions
• feature:review
• feature:insights (analytics, budgets, recurring)
• feature:accounts
• feature:settings (including categories, rules, export and backup)
• engine:sms (receiver, inbox reader, import worker, dedupe)
• engine:parser (template engine, generic fallback)
• engine:classifier (income, expense, transfer and refund logic)
• engine:categorizer (pipeline, normaliser, dictionary, rules, ML)
• engine:recurring (detector)
Dependency rule: feature modules depend on core and engine interfaces, never on each other. Engine modules never depend on Android UI. Parser, classifier and categorizer are pure Kotlin so they are fast to unit test.
3. Processing Pipeline
The end-to-end flow for every SMS, whether live or from the inbox import:
1. SmsReceiver or ImportWorker produces RawSms(id, sender, body, receivedAt, subscriptionId).
2. SenderFilter: drops messages from non-financial senders.
3. Deduplicator: computes the hash and checks the database.
4. NoiseFilter: rejects OTP, promo, reminder, failed and enquiry messages and writes an IgnoredMessage with a reason.
5. Parser: template match first, then generic fallback, producing ParsedTxn with fields and a confidence.
6. AccountResolver: finds or creates the Account from bank and last 4.
7. Classifier: assigns TransactionType (Expense, Income, Transfer, Refund and so on) and links pairs.
8. Categorizer: runs the layered pipeline and sets category, source and confidence.
9. Repository saves the Transaction in a single database transaction.
10. Notifier posts an optional notification and the UI updates through Room Flow.
All steps 2 to 8 are pure functions over data, which makes them easy to test with the SMS corpus.
4. Key Contracts (Kotlin interfaces)
These signatures are the agreed contracts. Jules may add parameters but must not change the meaning.
data class RawSms(val id: Long, val sender: String, val body: String, val receivedAt: Long, val subId: Int?)

data class ParsedTxn(
    val amountMinor: Long,
    val currency: String,
    val direction: Direction,        // DEBIT or CREDIT
    val dateTime: Long?,
    val accountLast4: String?,
    val bankName: String?,
    val merchantRaw: String?,
    val channel: Channel?,           // UPI, CARD, NEFT, IMPS, ATM, WALLET, OTHER
    val balanceAfterMinor: Long?,
    val refNumber: String?,
    val upiId: String?,
    val confidence: Float,
    val templateId: String?
)

sealed interface ParseResult {
    data class Success(val txn: ParsedTxn) : ParseResult
    data class Ignored(val reason: IgnoreReason) : ParseResult
    object NoMatch : ParseResult
}

interface SmsParser { fun parse(sms: RawSms): ParseResult }

interface CategorizationLayer {
    val name: String
    fun categorize(input: CategorizationInput): CategoryResult?   // null means pass to next layer
}

class CategorizationPipeline(private val layers: List<CategorizationLayer>) {
    fun run(input: CategorizationInput): CategoryResult
}
Money is always Long minor units (paise) in the domain and database. Never use Float or Double for money.
5. Parser Template Format
Templates live in assets/templates/<bank>.json and are loaded at startup. Example:
{
  "id": "hdfc_upi_debit_v1",
  "bank": "HDFC",
  "senderPatterns": ["^[A-Z]{2}-HDFCBK$"],
  "direction": "DEBIT",
  "channel": "UPI",
  "regex": "Rs\\.?\\s?(?<amount>[\\d,]+\\.?\\d*)\\s+debited from a/c \\*\\*(?<last4>\\d{4}).*?VPA\\s+(?<upi>\\S+)",
  "dateFormats": ["dd-MM-yy", "dd-MMM-yy"],
  "confidence": 0.95
}
Rules for the engine:
• Named groups map to ParsedTxn fields: amount, last4, merchant, upi, date, time, balance, ref.
• Templates are tried in priority order per sender. The first full match wins.
• A template must carry at least 3 sample SMS and expected results in assets/templates/tests/<id>.json, used by an automatic parameterised test.
• The generic fallback assigns confidence 0.5 to 0.7 depending on how many fields were found.
6. Database Schema Notes
Entities follow SRS Section 5. Additional rules:
• Unique index on Transaction.smsHash. Index on (dateTime), (accountId, dateTime), (categoryId, dateTime), (merchantNormalised).
• Use Room type converters for enums. Store dateTime as epoch milliseconds (UTC) and convert for display.
• Foreign keys with ON DELETE rules: deleting an Account is blocked if it has transactions (archive instead). Deleting a Category requires reassignment.
• Every schema change ships with a tested migration (no destructive fallbacks).
• Seed data (default categories, SmsSender list) is inserted by a first-run initialiser, versioned so updates add new defaults without overwriting user edits.
7. Categorisation Pipeline Detail
Layer order and confidence defaults:
1. UserRuleLayer (confidence 1.0): evaluated by priority, matches on merchant, amount range, account, channel or SMS text.
2. LearnedMappingLayer (0.9): MerchantMapping keyed on the normalised merchant or UPI ID, with a minimum confirmedCount of 1.
3. DictionaryLayer (0.85): bundled merchants.json with names, aliases and regex hints mapped to a category id.
4. HeuristicLayer (0.7): keyword and channel rules (ATM = Cash Withdrawal, EMI = Loans and EMI, and similar).
5. MlLayer (model dependent, accept at or above 0.75): optional, behind a feature flag.
6. Fallback: Uncategorised, confidence 0, flagged for the Review Queue.
Merchant normaliser steps: lowercase, strip prefixes (upi/, vpa, pos, ecom), remove digits and order ids, remove city and bank suffixes, collapse whitespace, then alias lookup.
Learning: when the user recategorises, write a MerchantMapping and optionally a Rule (if the user chose apply to all), increment confirmedCount, and re-run categorisation over matching uncorrected transactions in a background worker.
8. Classification Logic Notes
• Transfer detection runs after every insert: search for an opposite-direction transaction with equal amount, different own account, within the window. If found, set both to Transfer and set linkedTransactionId.
• Card payment detection: the debit narrative contains card keywords and the last 4 matches a known Credit Card account, or a card credit with payment received appears within the window.
• Refund linking: a credit from the same merchant (normalised) within 90 days with amount less than or equal to the original debit.
• All classification decisions record a reason string for the detail screen and for debugging.
9. Background Work
• SmsReceiver: lightweight; it only enqueues a OneTimeWork request with the SMS data. No parsing in the receiver.
• ImportWorker: chunked (500 messages per chunk), progress reported through setProgress, resumable via a checkpoint of the last processed SMS id.
• CatchUpWorker: periodic (daily) scan for messages newer than the last seen id, to cover OEM battery killers.
• RecurringDetectWorker: weekly.
• BackupWorker: optional scheduled.
10. Security
• SQLCipher passphrase generated randomly, wrapped by an Android Keystore key, stored in encrypted prefs.
• FLAG_SECURE on screens with balances when hide balances is enabled.
• android:allowBackup=false; exports and backups use the app's own encrypted format.
• No INTERNET permission in the manifest for v1 (enforced by a CI check). Any later network feature must add it behind a clear opt-in.
• Never log SMS bodies or amounts in release builds. A lint rule or detekt custom rule should forbid logging raw SMS.
11. Testing Strategy
• Corpus tests: assets in src/test/resources/sms_corpus/*.json with sms, expected type, amount, merchant and category. A single parameterised test reports precision and recall per bank and fails CI if below the NFR-2 targets.
• Property tests for the Money type and date parsing.
• Pipeline tests with fakes for each layer.
• Room migration tests for every schema version.
• Compose tests for critical flows: onboarding permission branches, recategorise with apply, review queue, budget alert display.
• Screenshot tests for core screens in light, dark and large font.
• CI gates: build, lint, detekt, unit tests, coverage at or above 80 percent on engine modules.
12. Jules Task Backlog
Each task is one PR. Jules should read AGENTS.md (Section 13) first. Format: ID, goal, requirements, acceptance criteria.
T0.1 Project scaffold. Create the multi-module Gradle project (Kotlin DSL, version catalog), Hilt, theme and navigation shell, CI workflow, ktlint and detekt. Acceptance: app builds, empty Home screen launches, CI is green.
T0.2 Database foundation. Room entities, DAOs, converters, SQLCipher integration, seed initialiser, migration test harness. Requirements: SRS section 5, FR-12.2. Acceptance: DAO tests pass; the database file is not readable without the key.
T1.1 Onboarding and permissions. Screens per the UI spec 5.1, permission flows including denial paths. Requirements: FR-1. Acceptance: Compose tests for granted and denied branches.
T1.2 SMS ingestion. SmsReceiver, inbox reader, SenderFilter, Deduplicator, ImportWorker with checkpointing, IgnoredMessage log. Requirements: FR-2. Acceptance: importing 10,000 fake messages completes under 2 minutes on a Robolectric benchmark profile; duplicates are never inserted.
T2.1 Parser engine. Template loader, regex engine with named groups, date and amount parsers, generic fallback. Requirements: FR-3.1 to 3.3, 3.6. Acceptance: all sample SMS in SRS section 7 pass.
T2.2 Noise filter and bank templates. OTP, promo, reminder, failed and enquiry detection; add templates for at least 10 banks and wallets with tests. Requirements: FR-3.4, 3.7. Acceptance: zero OTP false positives in the corpus.
T3.1 Account resolver and classifier. Auto account creation, balances, income or expense typing, transfer, card payment and refund linking, salary detection. Requirements: FR-4, FR-7.1 to 7.2. Acceptance: transfer and card payment scenarios do not affect income or expense totals.
T4.1 Categorisation pipeline. Normaliser, dictionary (at least 300 merchants), heuristics, rules, learned mappings, fallback. Requirements: FR-5.1 to 5.4, 5.10. Acceptance: at least 85 percent accuracy on the corpus.
T4.2 Review Queue and learning loop. UI per 5.7, apply-to-similar flow, background recategorisation. Requirements: FR-5.4, 5.5. Acceptance: correcting one merchant updates all matching past transactions.
T5.1 Transactions UI. List, filters, search, detail, edit, manual add, multi-select. Requirements: FR-6. Acceptance: filter combinations produce correct totals; Compose tests pass.
T6.1 Dashboard. Summary, period selector with custom month start, charts and drill-down. Requirements: FR-8.1 to 8.4. Acceptance: totals match the database queries in tests.
T7.1 Budgets and notifications. Budget CRUD, progress, threshold alerts, transaction notifications with actions. Requirements: FR-9.1, 9.2, FR-11. Acceptance: alerts fire once per threshold per period.
T8.1 Security and data. App lock, export (CSV and JSON), encrypted backup and restore, delete all. Requirements: FR-12. Acceptance: backup and restore round-trips identical data.
T9.x Post-MVP. Recurring detection, insights cards, split transactions, goals, widget, TFLite layer.
13. AGENTS.md (place in the repository root)
The following content is intended to be committed as AGENTS.md so Jules follows project rules on every task:
• Read the SRS, UI/UX spec and this design doc in /docs before starting. Cite requirement IDs in the PR description.
• Work on one task per PR. Keep PRs under about 600 changed lines where possible.
• Always add or update tests with the code. Never reduce coverage on the engine modules.
• Run ./gradlew ktlintCheck detekt test before finishing; fix all failures.
• Money is Long minor units. Never use Float or Double for currency.
• Never log SMS bodies, amounts or account numbers. Never add the INTERNET permission.
• No hard-coded user-facing strings; use string resources. No hard-coded colours; use theme tokens.
• Parser templates and merchant data go in JSON assets, not Kotlin code.
• Follow the module dependency rules in Section 2. Do not create feature-to-feature dependencies.
• Database changes require a Room migration and a migration test.
• When requirements are ambiguous, choose the simplest option that satisfies the acceptance criteria and note the assumption in the PR description.
• Do not add new third-party libraries without stating the reason in the PR.
14. Definition of Done (per task)
1. Acceptance criteria met and demonstrated by tests.
2. CI green (build, lint, detekt, tests).
3. Previews for new composables in light, dark and large font.
4. No new warnings; no TODOs without a linked backlog ID.
5. PR description lists requirement IDs covered and any assumptions.
15. Suggested Prompt Template for Jules
Use this for each task you assign:
Task: <ID and title>. Context: read AGENTS.md and the docs folder. Goal: <one sentence>. Requirements: <FR IDs>. Acceptance criteria: <copied from Section 12>. Constraints: <modules to touch, anything to avoid>. Deliverable: one PR with tests passing.