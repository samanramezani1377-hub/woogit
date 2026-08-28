# WooGit — Mandatory Agent Workflow

## Instruction priority

Higher-priority platform/system/developer rules always apply. Subject to those rules, the **current user's explicit instructions are the source of truth and take precedence over this document and all other repository documentation** when they conflict.

Do not use this file or another project document to override, reject, reinterpret, or silently replace an explicit current user instruction. If the request conflicts with a higher-priority rule, follow that rule instead.

The repository's engineering workflows are defaults that support the user's request; they do not outrank it.

Before inspecting, changing, debugging, refactoring, building, or reviewing this repository, Claude should use the applicable repository engineering methodology and skills, including Superpowers where available and relevant, while respecting the instruction priority above.

UNLAZY requirement: do not stop at partial implementation or visual/code inspection when the user asks for a complete implementation. Continue the implementation → build → test → review → verify loop until the requested acceptance criteria are satisfied or a genuine blocker is reached. Never fake, weaken, skip, or hide verification failures.

See the repository root `AGENTS.md` for the full policy and instruction-precedence rules.
