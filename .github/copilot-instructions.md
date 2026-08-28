# WooGit — Mandatory Agent Engineering Stack

Every AI coding agent operating on this repository MUST follow ALL applicable required resources:

- **Superpowers**: https://github.com/obra/superpowers
- **Addy Osmani Agent Skills**: https://github.com/addyosmani/agent-skills
- **Official Kotlin Agent Skills**: https://github.com/Kotlin/kotlin-agent-skills
- **Android Agent Skills**: https://github.com/new-silvermoon/awesome-android-agent-skills
- **WooGit CI Verification** as the mandatory acceptance gate.

For every Kotlin/Android/Gradle task, the agent MUST check and use applicable skills from the official Kotlin and Android collections before implementation, in addition to the relevant Superpowers and Addy Osmani workflows.

Use the applicable resources for planning, implementation, debugging, review, testing, performance, accessibility, Android/Compose engineering, and verification. Never invent that a skill was used when it was unavailable or inapplicable.

## Mandatory CI Verification

After substantive changes:
1. Push the implementation.
2. Wait for the GitHub Actions run.
3. Inspect the actual result and logs.
4. If it fails, diagnose and fix the real failure, push again, and repeat.
5. Completion is allowed only when the required CI gates are green, unless a genuine external blocker prevents verification.

Required CI gates must not be removed, skipped, weakened, or hidden:
- Debug build
- Release build
- Unit tests
- Android lint
- APK verification
- Final CI gate

Never use test/check removal, `continue-on-error`, fake results, or other bypasses to obtain green CI.

The complete repository policy is in `AGENTS.md`.
