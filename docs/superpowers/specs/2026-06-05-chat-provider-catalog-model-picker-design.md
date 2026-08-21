# Chat-Provider Catalog Model Picker — Design

**Date:** 2026-06-05
**Status:** Draft (awaiting review)
**Surfaces:** AI Hub thread composer, Copilot thread composer (shared `ModelPicker`)

## Summary

Replace the composer's AI-Gateway-driven LLM picker with a **catalog-driven** picker. The picker
lists **all chat-capable AI providers** from the platform AI Providers catalog (the `Provider`
enum behind the `/automation/settings/ai-providers` page), each with its **icon**. A provider that
is **active** (`enabled == true`) expands into its model list (plus a "Choose model by ID" entry);
an **inactive** provider shows a single **"Configure credentials"** action that navigates to the AI
Providers settings page. The trigger button shows the **exact selected provider + model** (with
icon), never the generic "Workspace default" label, and seeds new conversations from the user's
**last-used** selection.

To make selections actually take effect at runtime, a **new catalog-based ChatModel resolver** is
wired into both the AI Hub and Copilot chat-client resolvers; it builds the Spring-AI `ChatModel`
from the catalog provider's platform API key and the selected model name.

## Background: two decoupled "AI provider" systems

ByteChef has two unrelated provider concepts. Understanding both is required to read this spec.

| | **AI Providers catalog** (this spec's source) | **AI Gateway** (current picker source) |
|---|---|---|
| Definition | `com.bytechef.component.ai.llm.Provider` enum (11 active entries) | `AiGatewayProviderType` enum (8) + workspace rows |
| Scope | Platform / environment | Workspace |
| Surfaced as | `AiProviderDTO {id, name, icon, apiKey, enabled}` via REST `getAiProviders(environment)` (ADMIN-only) | `workspaceAiGatewayProviders`/`Models` GraphQL (USER) |
| API keys | `PropertyService`, `Scope.PLATFORM`, keyed by `Provider.getKey()` | per-workspace `ai_gateway_provider.apiKey` |
| Icons | yes — `ComponentDefinition.getIcon()` per provider | none |
| Models | none on DTO — but reachable via the chat action's `model` property options on the `ComponentDefinition` | `workspaceAiGatewayModels` rows |
| Drives runtime today | no | yes (`AiHubChatClientResolver`, `CopilotChatClientResolver`) |

Key consequence: the catalog has icons + the full provider list but does **not** drive runtime, and
its REST endpoint is admin-only. This spec adds (1) a USER-safe read path and (2) a runtime resolver
so the catalog can both render the picker and resolve selections.

### Chat-capable providers

From `Provider.java`, the catalog offers this chat-capable subset (8 providers):

`ANTHROPIC (anthropic)`, `GROQ (groq)`, `MISTRAL (mistral)`, `NVIDIA (nvidia)`,
`OPEN_AI (openAi)`, `VERTEX_GEMINI (vertexGemini)`, `PERPLEXITY (perplexity)`, `DEEPSEEK (deepseek)`.

**Excluded** from the catalog (curated `CHAT_PROVIDERS` set):
- `STABILITY` — image-only.
- `HUGGING_FACE` — Spring AI no longer supports the Hugging Face chat model.
- `AZURE_OPEN_AI` — its chat model requires a per-deployment **endpoint** that the platform catalog does
  not store (only an `apiKey`), so it can't be resolved from catalog credentials; listing it would be a
  silent fall-back to the workspace default. (Supporting it later means adding an endpoint field to AI
  Providers settings + the property store.)

### Model lists per provider

Providers with an enumerable model list (the chat action's `model` property has `options`):
`OPEN_AI`, `ANTHROPIC`, `MISTRAL`, `VERTEX_GEMINI`. These expose `[{name, label}]`.

Providers that are OpenAI-compatible and accept a **free-form** model id (no `options` on the `model`
property): `GROQ`, `PERPLEXITY`, `NVIDIA`, `DEEPSEEK`. These expose an empty `models` list and
`supportsModelById: true`; their active submenu shows **only** "Choose model by ID".

## Requirements

1. List all chat-capable catalog providers in the composer picker, alphabetized, each with its icon.
2. Show a provider's models only when the provider is active (`enabled == true`).
3. Inactive providers show a "Configure credentials" action → `/automation/settings/ai-providers`.
4. The trigger shows the exact selected provider + model (with icon), not "Workspace default".
5. New conversations/tasks seed from the user's last-used selection; first-ever use falls back to
   the first active provider's first model.
6. A pick actually changes the model used at runtime, for both AI Hub and Copilot.
7. No `apiKey` is ever exposed to non-admin clients.
8. Applies to both the AI Hub composer and the Copilot composer.

## Design

### Piece 1 — Server: USER-safe chat-provider catalog query

Add a new **GraphQL** query (consistent with the picker's existing GraphQL data fetching),
environment-scoped, `@PreAuthorize("hasAuthority('USER')")`:

```graphql
query aiProviderCatalog($environment: ID!) {
    aiProviderCatalog(environment: $environment) {
        key            # Provider.getKey(), e.g. "openAi" — the value sent as userSelectedLlmProvider
        name           # display label, e.g. "OpenAI"
        icon           # SVG string (inline or path:-resolved), same as settings page
        enabled        # active flag
        supportsModelById
        models { name label }
    }
}
```

Resolver/facade behavior (extends the existing `AiProviderFacade` path):

- Iterate `Provider.values()`, filter to chat-capable (component has a chat action).
- For each: resolve `name`, `icon` (existing `ComponentDefinition.getIcon()` + `IconUtils`),
  `enabled` (existing `PropertyService` lookup by `Provider.getKey()`).
- `models`: read the chat action's `model` property `options` from the `ComponentDefinition`
  (`{name = option.value, label = option.label}`). Empty when the property has no options.
- `supportsModelById`: true when `models` is empty OR the model property is free-form text.
- **Never** include `apiKey`.

The DTO is a new USER-safe projection (e.g. `AiProviderCatalogItem`), distinct from the
admin-only `AiProviderDTO`, so the API key field cannot leak by construction.

Module placement (finalize in plan): the catalog facade lives in
`platform-configuration-service` (EE); the GraphQL controller in the corresponding graphql module
alongside the existing AI-gateway GraphQL controllers. Both gated EE.

### Piece 2 — Server: catalog-based ChatModel resolver

New shared component (EE), e.g. `CatalogChatClientResolver`:

```
ChatClient resolve(int environment, String providerKey, String modelName)
```

- Look up `Provider` by `key`; if absent/non-chat → return null (fall back).
- Read the platform API key: `PropertyService.getProperty(provider.getKey(), Scope.PLATFORM, null, environment)`.
  If absent or provider not enabled → return null (fall back).
- Build the Spring-AI `ChatModel` by reusing the per-provider
  `com.bytechef.component.ai.llm.ChatModel.createChatModel(...)` factory (same code the components
  use), passing the API key + `modelName`.
- Return `ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().model(modelName).build()).build()`.

Wiring — both `AiHubChatClientResolver` and `CopilotChatClientResolver` gain a new precedence tier:

1. **User catalog selection** (`userSelectedLlmProvider` is a catalog `key` + `userSelectedLlmModel`)
   → `CatalogChatClientResolver`. *(new, highest among user selections)*
2. Personal-agent override (AI Hub only) — unchanged.
3. Existing AI-Gateway / workspace-default fallback — unchanged.

If the catalog resolver returns null (no key, disabled, build failure), resolution falls through to
the existing path, then to the workspace default. The bridge from EE resolver to the CE component
`ChatModel` factory is resolved in the plan (the factory is `@AutoService`/ServiceLoader, not a
Spring bean — likely a small lookup-by-provider adapter).

Note: `providerKey` over the wire is `Provider.getKey()` (e.g. `"openAi"`), not the gateway type
name. The catalog resolver matches on `key`; the gateway fallback matches on its own type name, so
the two namespaces don't collide.

### Piece 3 — Client: `ModelPicker` rewrite

`client/src/shared/components/ai/model-picker/ModelPicker.tsx`:

- Data source: new `useAiProviderCatalogQuery({environment})` (replaces the two AI-Gateway
  queries for the provider/model sections). `environment` from `useEnvironmentStore`.
- Render every provider (alphabetized) with its icon via `react-inlinesvg` `InlineSVG`.
  - **Active** (`enabled`): submenu lists `models` as items; append a **"Choose model by ID"** row
    (free-text input that calls `onChange(key, typedModel)`). Free-form providers show only that row.
  - **Inactive**: submenu (or row) shows a single **"Configure credentials"** item →
    `navigate('/automation/settings/ai-providers')`, closing the menu.
- **Trigger**: provider icon + selected model label. Resolved from the catalog item for the current
  `selectedProvider`/`selectedModel`. The "Workspace default" sentinel display is removed; a
  concrete selection is always shown (see last-used seeding).
- Keep the personal-agents and workflow-chats cascades unchanged (AI Hub only; opt-in via callbacks).
- Wire format unchanged: `onChange(providerKey, modelName)` → existing stores → existing
  runtime-provider `buildStateToSend` → `userSelectedLlmProvider`/`userSelectedLlmModel`.

**Last-used seeding** (requirement 5): persist the last `(providerKey, modelName)` per workspace in
`localStorage` (e.g. key `bytechef.modelPicker.lastUsed.<workspaceId>`). On a new conversation/task
or the AI Hub draft slot, initialize the selection from last-used; if none, pick the first active
provider's first model (deterministic order). This only changes the *initial* value — the per-task
(`useAiHubTasksStore`) and per-conversation (`useCopilotStore`) override mechanics are untouched.
Updating last-used happens in `ModelPicker.onChange`.

Both composers already render the same `ModelPicker` (`AiHubChatComposer` via a `modelPicker`
ReactNode; `CopilotPanel` via `leadingComposerActions`), so the rewrite lands in both at once. The
AI Hub home/draft and per-task wiring and the Copilot store wiring are updated to seed from
last-used.

## Edge cases

- **No active providers:** picker still lists all providers (all show "Configure credentials");
  trigger shows a neutral "Select model" prompt; nothing is sent (no override → server default).
- **Selected provider later disabled:** trigger still shows the stored selection; on next open the
  provider appears inactive. Runtime resolver returns null → server default. (Acceptable; no forced
  reset.)
- **Model-by-id for a provider that also has a list:** allowed — the free-text row coexists with the
  listed models so users can run a model newer than the bundled list.
- **Environment switch:** the query is environment-scoped; switching environments refetches and may
  change active state. Last-used is keyed by workspace, not environment (revisit if it causes
  cross-environment confusion).
- **CE / gateway-disabled:** the picker and resolver are EE-gated like the existing AI Hub/Copilot
  features. When the catalog resolver bean is absent, both chat resolvers behave exactly as today.

## Out of scope

- Changing the AI Gateway settings pages or the gateway runtime path (kept as fallback).
- Per-workspace catalog API keys (catalog keys remain environment-scoped, admin-set in Settings).
- Curated model lists for free-form providers (decided: "Choose model by ID" only).
- Image/embedding/non-chat providers (Stability et al.) in the picker.

## Testing

**Server**
- Facade: chat-capable filter excludes Stability; `models` populated from chat action options;
  free-form providers yield empty `models` + `supportsModelById = true`; `apiKey` never present.
- GraphQL: USER role authorized; ADMIN authorized; anonymous rejected.
- `CatalogChatClientResolver`: builds correct ChatModel per provider key from a stored platform key;
  returns null on missing key / disabled / unknown key; both AI Hub and Copilot resolvers prefer the
  catalog tier and fall back correctly.

**Client (Vitest)**
- Active provider renders models + "Choose model by ID"; inactive renders "Configure credentials"
  → navigates to the settings route.
- Trigger shows the exact provider icon + model label; never "Workspace default".
- Last-used seeding: new conversation initializes from `localStorage`; first-ever use picks
  first-active; `onChange` updates `localStorage`.
- Wire format: `onChange` produces the catalog `key` + model; the runtime provider sends
  `userSelectedLlmProvider`/`userSelectedLlmModel` only when both are set.

## Implementation phases (for the plan)

1. Server: USER-safe `aiProviderCatalog` query (facade projection + GraphQL controller + tests).
2. Server: `CatalogChatClientResolver` + precedence wiring in both chat resolvers + tests.
3. Client: `ModelPicker` rewrite (catalog query, icons, active/inactive, model-by-id, exact trigger)
   + last-used seeding + both-composer wiring + tests.
```
