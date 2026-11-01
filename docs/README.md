# Kharcha documentation

Use this page to find the product specs and supporting implementation notes.
The SRS and technical design describe requirements and architecture; the UI
spec is the main reference for user flows. Planning and research documents are
supporting material and may describe work that has changed since they were
written.

## Product and architecture

| Document | Purpose |
| --- | --- |
| [SRS](SRS.md) | Product scope, functional requirements and acceptance criteria |
| [UI/UX specification](UI_SPEC.md) | App screens, interaction patterns and visual requirements |
| [Technical design](TechDoc.md) | Module boundaries, processing pipeline and implementation plan |
| [Axio style UI spec and plan](AXIO_STYLE_UI_SPEC_AND_PLAN.md) | Visual direction and UI adaptation plan |

## UI plans

| Document | Purpose |
| --- | --- |
| [UI redesign plan](UI_REDESIGN_PLAN.md) | Proposed redesign work and sequencing |
| [Screen layouts and wireframes](UI_SCREEN_LAYOUTS_AND_WIREFRAMES.md) | Screen-level layout reference |
| [Advanced filters spec](UI_ADVANCED_FILTERS_SPEC.md) | Transaction filtering requirements |
| [UI implementation backlog](UI_IMPLEMENTATION_BACKLOG.md) | Outstanding interface tasks |

## Engineering research

| Document | Purpose |
| --- | --- |
| [Parser benchmark and Indian banks](PARSER_BENCHMARK_AND_INDIAN_BANKS_PLAN.md) | Parser evaluation and bank coverage plan |
| [ML categorizer plan](ML_CATEGORIZER_PLAN.md) | Categorization research and planned ML work |
| [PennyWise benchmark analysis](PENNYWISE_BENCHMARK_ANALYSIS.md) | Product benchmark and exploratory notes |

When a plan conflicts with the SRS or current implementation, confirm the
intended requirement before changing behavior. Keep SMS bodies, account
numbers and transaction amounts out of issues, screenshots, logs and test
fixtures unless the data is synthetic or fully redacted.
