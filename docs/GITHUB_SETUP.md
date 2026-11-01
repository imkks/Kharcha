# GitHub repository setup

This checklist describes repository settings that cannot be configured by files
alone. It is for the repository owner or an administrator.

## Actions

1. Set `main` as the default branch.
2. In **Settings → Actions → General**, allow the repository's GitHub Actions
   workflows to run.
3. Confirm pull requests run **Android CI**.

## Protect `main`

In **Settings → Rules → Rulesets** (or **Branches → Branch protection rules**),
create a rule targeting `main`:

- Require a pull request before merging.
- Require the `Android CI / checks` status check.
- Require conversation resolution.
- Block force pushes and branch deletion.

## Security and maintenance

- Enable private vulnerability reporting in **Settings → Security → Code
  security and analysis**, if available for the repository.
- Keep Dependabot alerts and security updates enabled. The checked-in
  `.github/dependabot.yml` requests monthly Gradle and GitHub Actions updates.
- Review dependency updates before merging.
- Add a `LICENSE` only after the project owner chooses the reuse terms. No
  license is included by default.
