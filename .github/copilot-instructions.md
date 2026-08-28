# WooGit — Mandatory Superpowers Workflow

Any AI agent operating on this repository MUST use the **Superpowers** methodology before and during engineering work:
https://github.com/obra/superpowers

Use the relevant Superpowers skills for planning, implementation, debugging, review, and verification. In particular use `systematic-debugging` for failures and `verification-before-completion` before declaring success.

UNLAZY: continue through implementation, build/test, review, and verification until the requested acceptance criteria are actually satisfied or a genuine external blocker prevents progress. Never fake or weaken checks to obtain green results.

If Superpowers is not active in the agent environment, report that prerequisite instead of silently replacing it with an ad-hoc workflow.

The complete repository policy is in `AGENTS.md`.
