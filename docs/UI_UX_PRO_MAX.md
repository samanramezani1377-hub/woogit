# WooGit UI/UX Pro Max Reference

This document is the project-level reference for AI agents making UI/UX changes in WooGit.

## Mandatory UI/UX reference

For any UI/UX redesign, review, bug fix, visual refinement, accessibility improvement, animation change, or design-system change, agents SHOULD consult **UI/UX Pro Max** before implementing the change.

Official repository:
https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

The skill provides searchable design intelligence, design-system generation, UX guidelines, style guidance, typography, color systems, and stack-specific guidance including **Android Jetpack Compose**.

## WooGit-specific rule

UI/UX Pro Max is a **reference and design authority**, not a reason to invent unrelated features.

When fixing an existing WooGit UI problem:

1. Identify the existing screen, component, state, and user flow.
2. Treat the request as a bug/quality/UX correction unless a genuinely new capability is required.
3. Inspect the current WooGit design system before changing it.
4. Consult UI/UX Pro Max for the appropriate pattern and Compose guidance.
5. Preserve existing architecture and behavior unless the requested correction requires a change.
6. Prefer a reusable design-system/component fix over duplicated page-specific styling.
7. Verify accessibility, touch targets, RTL/Farsi typography, contrast, state feedback, scrolling, loading, empty/error states, and performance.
8. Do not replace real data, API behavior, tests, or verification with visual mocks.

## WooGit visual direction

WooGit should use a coherent modern **Liquid/Glass-inspired design system** rather than isolated glass-looking cards. Glass effects must remain readable, accessible, performant, and consistent across Products, Orders, Dashboard, forms, dialogs, navigation, status badges, and lists.

Do not call a UI "Liquid Glass" merely because it has transparency. Use a consistent system of translucency, layered surfaces, subtle borders/highlights, depth, hierarchy, motion, and appropriate background treatment while avoiding excessive blur or contrast loss.

## Jetpack Compose

WooGit is an Android/Compose project. Prefer Compose-native solutions and the project's existing components/tokens. UI/UX Pro Max stack guidance should be interpreted through the existing WooGit architecture rather than introducing an unrelated UI framework.

## Design-system source of truth

If a persisted `design-system/MASTER.md` exists, read it before making UI changes. Page-specific files under `design-system/pages/` override the master only for that page.

If UI/UX Pro Max is installed in the active AI environment, use its search/design-system tooling for the relevant domain and Jetpack Compose stack. If it is not installed, use the official repository as the reference and do not fabricate results from the skill.

## Suggested design-system workflow

For an installed UI/UX Pro Max skill, generate or inspect the design system before broad UI changes. Example:

```bash
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "mobile ecommerce admin dashboard liquid glass RTL Persian" --design-system -p "WooGit"
```

For Compose-specific guidance, use the skill's stack-aware search facilities and apply the result to the existing WooGit components.

## AI agent completion checklist

Before considering a UI/UX task complete:

- [ ] Existing behavior was understood before redesigning it.
- [ ] UI/UX Pro Max guidance was consulted when available.
- [ ] Existing WooGit architecture was preserved.
- [ ] RTL/Farsi presentation was checked.
- [ ] Loading, empty, error, success, and disabled states were checked.
- [ ] Scrolling and content overflow were checked.
- [ ] Touch targets and accessibility semantics were checked.
- [ ] Visual changes are consistent with the shared design system.
- [ ] No fake data or visual-only workaround hides a functional problem.
- [ ] Relevant tests/build/lint were actually run.

## Important

This file does not vendor the UI/UX Pro Max skill itself. It establishes the project-level contract so AI agents know which external design reference to consult. The official skill should be installed through its supported CLI/agent integration when the development environment permits it.
