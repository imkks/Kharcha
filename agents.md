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
