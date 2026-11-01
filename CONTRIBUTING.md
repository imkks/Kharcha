# Contributing to Kharcha

Thanks for helping improve Kharcha. For a small fix, open a pull request. For
larger changes, start with an issue so the product and architecture impact can
be agreed before implementation.

## Before coding

1. Read the [SRS](docs/SRS.md), [UI specification](docs/UI_SPEC.md), and
   [technical design](docs/TechDoc.md) sections related to the change.
2. Check [open issues](https://github.com/imkks/Kharcha/issues) and the
   [documentation index](docs/README.md) for existing plans.
3. Keep a pull request focused on one task. Cite relevant requirement IDs in
   the pull request description and state any assumptions.

## Development rules

- Keep money in `Long` minor units. Do not use `Float` or `Double` for currency.
- Never log or commit SMS bodies, transaction amounts, account numbers, or
  unredacted personal data. Use synthetic examples in tests and issues.
- Keep SMS processing on-device. Do not add the Android `INTERNET` permission
  without an approved product and privacy requirement.
- Use Android string resources for user-visible text and theme tokens for
  colors.
- Put parser templates and merchant data in JSON assets, not Kotlin source.
- Preserve module boundaries in [the technical design](docs/TechDoc.md):
  features must not depend on other feature modules.
- Database changes need a Room migration and a migration test.
- Do not add a third-party dependency without explaining why in the PR.
- Add or update tests with behavior changes. Do not reduce engine-module test
  coverage.

## Build and quality checks

Use JDK 17 and the Android SDK required by the project. Run these checks before
requesting review:

```sh
./gradlew ktlintCheck detekt test
```

To build a local debug APK:

```sh
./gradlew assembleDebug
```

## Pull requests

- Explain the change and user impact in plain language.
- Link the issue and requirement IDs, when applicable.
- List checks run and their results.
- Include screenshots for visible UI changes, with personal and financial data
  removed.
- Call out database migrations, permission changes, and new dependencies.

The [pull request template](.github/pull_request_template.md) provides a
checklist.
