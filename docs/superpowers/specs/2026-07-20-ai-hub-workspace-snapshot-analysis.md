# AI Hub: auto-injected workspace snapshot vs on-demand discovery

Date: 2026-07-20
Status: Analysis — recommendation: keep on-demand discovery; do not build an auto-injected snapshot now

## Question

Sim.ai's Mothership injects "a snapshot of your entire workspace with every message — all workflows,
tables, knowledge bases, files, credentials, jobs, and integrations — which is why you can refer to
things by name without specifying IDs or paths." Should ByteChef's AI Hub do the same?

## What AI Hub does today

The system message (`AiHubSpringAIAgent.createSystemMessage`) carries only *session-scoped* context:
Active File, Open Tabs, Active Tab, Referenced Resources (the user's @-mentions), the personal-agent
overlay, and a memory index. Everything else is **tool-driven, on demand**: the agent calls
`listProjects` / `listWorkflows` / `listDataTables` / `listKnowledgeBases` / `listAssetFiles` when it
needs an inventory, and both mode prompts instruct it to discover before acting.

## Why "refer to things by name" mostly already works

The name-resolution UX the snapshot buys is largely served by two existing mechanisms:

1. **@-mentions** — the client resolves names to typed Referenced Resources *before* the model sees
   the message, with UI autocomplete. This is strictly better than a snapshot for precision: the user
   picks the exact resource; the model never guesses between two similarly-named tables.
2. **One cheap list call** — when the user types a bare name ("the leads table"), the agent resolves
   it with a single `listDataTables` call. Cost: one tool round trip (~1–3 s, a few hundred tokens),
   paid only on the turns that need it.

## Cost model of a per-message snapshot

- **Token floor per turn.** Names + ids + types for a realistic workspace (200 workflows, 50 tables,
  20 KBs, 500 files) is ~3–8k tokens. Injected every turn of every conversation, it dwarfs the cost
  of occasional list calls. Small workspaces pay little — but small workspaces also least need it.
- **Prompt-cache busting.** The snapshot sits in the system message and *changes whenever any
  resource changes* (or whenever ordering/recency metadata ticks). Every change invalidates the
  provider-side prompt-cache prefix for all subsequent turns, so the real cost is not just the
  snapshot tokens but re-billing the full system prompt at uncached rates. The AI Hub system prompt
  (mode prompt + pinned tool schemas) is large; keeping it cache-stable is worth real money.
- **Staleness or freshness — pick one.** A per-conversation snapshot goes stale mid-conversation
  (the agent itself creates tables/files as it works — this session's tools mutate the workspace
  constantly). A per-message snapshot is fresh but maximizes the two costs above.
- **Multi-tenant variance.** Inventory size is unbounded across tenants; any snapshot needs
  truncation policy (top-K by recency?), which reintroduces "the thing I meant isn't in the
  snapshot" — and then the agent must fall back to list tools anyway, now with a misleading partial
  inventory in context.

## What Mothership's choice implies

Mothership's snapshot is coherent with its design (one flat workspace, resource counts implicitly
bounded, heavy emphasis on zero-friction naming). ByteChef's AI Hub instead invested in:
tool-search (pgvector catalog) to keep pinned schemas small, `PinnedToolSearchToolCallingAdvisor`,
and prompt-cache-friendly stable system prompts. An auto-snapshot cuts against all three.

## Middle grounds considered (and when to revisit)

1. **Counts-only summary** ("12 projects, 5 data tables, 3 knowledge bases, 41 files"), injected
   once per conversation: ~30 tokens, cache-stable within a conversation. Cheap, but low value —
   counts rarely change agent behavior; the prompts already mandate list-before-act.
2. **Top-K recent names per domain** (~300 tokens, once per conversation): the highest-value
   variant. Would let "the leads table" resolve without a tool call *when it's recent*. Still
   cache-busts across conversations only (acceptable). Worth building **only if telemetry shows
   name-resolution friction**.
3. **Semantic resource search tool** (`searchResource`, pgvector over resource names/descriptions):
   keeps the on-demand model but collapses five list tools into one targeted lookup. More useful for
   very large workspaces than any snapshot.

**Decision gate:** the tool-invocation logging shipped in the platform-tool-execution work records
every `list*` call. Before building any snapshot, measure: (a) average `list*` calls per
conversation, (b) conversations with ≥2 list calls for the *same* domain (the actual waste a
snapshot would eliminate). If (b) is rare — which the prompts' "discover once, then act" guidance
makes likely — the snapshot buys nothing measurable.

## Recommendation

Keep on-demand discovery. @-mentions plus a single list call cover the naming UX; a per-message
snapshot has an unbounded token floor and breaks prompt caching for marginal benefit. If telemetry
later shows repeated same-domain list calls per conversation, build middle-ground #2 (top-K recent
names, once per conversation, hard token cap) — not the full snapshot.
