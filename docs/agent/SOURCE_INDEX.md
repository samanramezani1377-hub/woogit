# WooGit App — Agent Source Index

## Purpose
This is the source-of-truth locator for agent work. The contract maps in this directory describe behavior; this index identifies where executable behavior lives.

## Mandatory lookup order
1. `app/src/main/kotlin/com/samanramezani1377/woogit/` — application bootstrap, workers, security and app-level behavior.
2. `data/src/main/kotlin/com/samanramezani1377/woogit/` — network, REST transport, repositories, persistence and WooCommerce adapters.
3. `domain/src/main/kotlin/com/samanramezani1377/woogit/` — domain models/contracts/use cases.
4. `presentation/src/main/kotlin/com/samanramezani1377/woogit/` — screens, navigation, ViewModels, AI execution and UI state.
5. `core/` — shared infrastructure if present.
6. Tests under the corresponding `src/test` trees are validation, not runtime source.

## Critical source anchors
| Contract area | Source anchor |
|---|---|
| AI agent orchestration | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiAgent.kt` |
| AI batch/recovery state | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiWorkingMemoryStore.kt` |
| AI provider contract | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiProvider.kt` |
| AI tool execution | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/WooGitToolExecutor.kt` |
| AI confirmation UI | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiScreen.kt` |
| AI state and confirmation dispatch | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiViewModel.kt` |
| AI runtime prompt | `presentation/src/main/kotlin/com/samanramezani1377/woogit/presentation/ai/AiAgentPrompt.kt` |
| AI regression tests | `presentation/src/androidTest/kotlin/com/samanramezani1377/woogit/presentation/ai/AiWorkingMemoryStoreTest.kt` |
| App composition / dependency graph | `app/src/main/kotlin/com/samanramezani1377/woogit/AppComposition.kt` |
| Backend HTTP contract | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/BackendClient.kt` |
| Shared HTTP transport | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/NetworkClient.kt` |
| WooCommerce transport facade | `data/src/main/kotlin/com/samanramezani1377/woogit/data/network/WooCommerceApi.kt` |

## AI batch execution contract

`AiAgent` owns the execution lifecycle. `AiScreen` owns selection only. `AiWorkingMemoryStore` persists per-item status/result and recovery checkpoints. `WooGitToolExecutor` is the store-side write executor and requires reread/verification before a write is reported as successful.

After batch confirmation, the Agent injects a complete internal per-item outcome into the model context. The model, not the UI, produces the final user-facing report. Every batch item must be reported as `VERIFIED`, `FAILED`, or `REJECTED_BY_USER` according to persisted execution state.

## Line-addressable rule
Agents must cite the exact file and current line range when making a source-level claim. If a source file changes, refresh the relevant line ranges in the contract map. Do not copy entire source files into documentation.

## Direction of authority
`source code -> agent map`. The map never overrides executable code.

<!-- one-time-ai-cleanup-trigger -->
