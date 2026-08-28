# WooGit — Mandatory Superpowers + Kotlin Agent Skills Workflow

Any AI coding agent operating on this repository MUST use both required engineering methodologies:

- **Superpowers**: https://github.com/obra/superpowers
- **Official Kotlin Agent Skills**: https://github.com/Kotlin/kotlin-agent-skills

For every Kotlin/Android/Gradle task, the agent MUST check and use any applicable official Kotlin Agent Skill before implementation, in addition to the relevant Superpowers workflow. Do not replace an applicable official Kotlin skill with ad-hoc Kotlin guidance.

Use Superpowers for planning, implementation, systematic debugging, review, testing, and verification. In particular use `systematic-debugging` for failures and `verification-before-completion` before declaring success.

Never fake, weaken, skip, or bypass checks to obtain green results.

If a required methodology or an applicable skill is unavailable in the current agent environment, report that prerequisite instead of silently substituting an ad-hoc workflow.

The complete repository policy is in `AGENTS.md`.
