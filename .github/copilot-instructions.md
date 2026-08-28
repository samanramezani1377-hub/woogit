# WooGit — AI Agent Instructions

## Instruction priority — USER IS THE SOURCE OF TRUTH

For AI agents operating in this repository, apply this precedence order:

1. Higher-priority platform/system/developer and safety rules.
2. **The current user's explicit instructions.**
3. Repository documentation and agent instructions (`AGENTS.md`, this file, `CLAUDE.md`, and other project docs).
4. Agent defaults and assumptions.

When the current user's explicit request conflicts with repository documentation, **follow the user's request** unless a higher-priority rule prevents it. Never use repository documentation to override, reject, reinterpret, or silently replace the user's instruction.

If the request is ambiguous, ask rather than inventing a conflicting repository requirement. If a higher-priority rule prevents following the request, follow the higher-priority rule.

## Engineering resources

Use applicable repository engineering resources and skills, including:

- **Superpowers**: https://github.com/obra/superpowers
- **Addy Osmani Agent Skills**: https://github.com/addyosmani/agent-skills
- **Official Kotlin Agent Skills**: https://github.com/Kotlin/kotlin-agent-skills
- **Android Agent Skills**: https://github.com/new-silvermoon/awesome-android-agent-skills

These are project defaults and are subordinate to the current user's explicit instructions.

## CI verification

After substantive changes, the normal workflow is to run and inspect the project's CI gates:
- Debug build
- Release build
- Unit tests
- Android lint
- APK verification
- Final CI gate

However, the user's explicit request controls the requested workflow and timing. Never remove tests, weaken gates, use `continue-on-error` to hide failures, fake results, or claim verification passed when it did not.

The complete repository policy is in `AGENTS.md`.
