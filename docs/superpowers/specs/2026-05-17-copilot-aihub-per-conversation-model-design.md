# Copilot + AI Hub + Personal Agents — unified `ModelPicker`

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-17 | **Last updated:** 2026-05-17

## Why

The [2026-05-13 personal-agent model-selection spec](2026-05-13-ai-hub-personal-agent-model-selection-design.md) shipped per-agent overrides (columns `llmProvider` / `llmModel`, routing-agent overlay, `PersonalAgentChatClientResolver`). §6 of that spec explicitly punted **per-task model selection for COPILOT** to a follow-up — this spec is that follow-up, **broadened**.

Today:

- **Copilot** (workflow editor, code editor, cluster element) always uses the workspace's `@Primary ChatModel` bean. One global pick per workspace; no per-conversation choice.
- **AI Hub** (`COPILOT` kind, the standard chat) also runs on the workspace default. Only personal-agent conversations honor an override (and even that override isn't user-pickable in the chat — it's only configurable from the personal-agent edit form).
- **Personal Agent edit form** ([AiHubPersonalAgentForm.tsx:434-507](../../../client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx:434)) uses **two side-by-side dropdowns** (Provider + Model). With the provider list growing to ~15 entries (OpenAI, Anthropic, Google, Azure ×2, Ollama, xAI Grok, Groq, DeepSeek, Cohere, Mistral, AWS Bedrock, Vercel AI Gateway, OpenRouter…), two flat Selects don't scale visually or interaction-wise.

We want **one** model picker, used in three places, that mirrors the n8n / Claude.ai / ChatGPT pattern: a compact trigger button showing the current selection, and a popover with cascading provider menus + search.

The reference UX (screenshot supplied by user):

```
┌─────────────────────────────┐
│ [icon] gpt-5-mini       ▼   │   ← trigger: icon + model name + ▼
│        n8n free OpenAI ...  │   ← optional subtitle (source/credit pool)
└─────────────────────────────┘
        │ click
        ▼
┌─────────────────────────────┐
│ 🔍 Search…                  │
├─────────────────────────────┤
│ 💬 Personal agents      >   │   ← v2 only; cascades to your agents
│ 🤖 Workflow chats       >   │   ← v2 only; cascades to your workflow chats
├─────────────────────────────┤
│ ⓞ  OpenAI               >   │   ← cascades to OpenAI's models
│ A  Anthropic            >   │
│ ◆  Google               >   │
│ 🅐  Azure (API Key)      >   │
│ 🅐  Azure (Entra ID)     >   │
│ 🦙 Ollama              >   │
│ 𝕏  xAI Grok             >   │
│ ⓖ  Groq                 >   │
│ 🐳 DeepSeek             >   │
│ …                           │
└─────────────────────────────┘
```

The list of providers + models is workspace-scoped and already lives behind the GraphQL pair `workspaceAiGatewayProviders` + `workspaceAiGatewayModels` — same source the Personal Agent edit form uses today.

## What ships

### v1 (this spec)

**A shared `ModelPicker` React component** that replaces:

1. The Personal Agent edit form's two-dropdown setup (one component instead of two `Select`s).
2. Nothing else exists today — it's added fresh to:
3. **Copilot panel** chat input toolbar (per-conversation override; resets on new conversation).
4. **AI Hub chat panel** input toolbar (per-conversation override; persists for the life of the conversation row).

Trigger displays the selected provider's icon + selected model name. When nothing is overridden, displays the workspace default model name with a muted "Workspace default" label. When a Personal Agent has an `llmModel` override and a user is in that agent's conversation, displays the agent's model with an "Agent default" sentinel.

Popover contents (v1): **Providers only** — the alphabetized provider list, each cascading into its model list. Search filters provider names; typing falls through to surface matching models inline.

### Server changes (v1)

- AI Hub: add `USER_SELECTED_LLM_PROVIDER_KEY` / `USER_SELECTED_LLM_MODEL_KEY` to `AiHubStateKeys`; update [PersonalAgentChatClientResolver](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/PersonalAgentChatClientResolver.java) to read user-selected first, fall back to personal-agent override, then to workspace default. **Rename deferred:** the original plan was to rename this to `AiHubChatClientResolver`, but the class also holds a `validate(workspaceId, llmProvider, llmModel)` method that's personal-agent-specific (invoked at save time from `AiHubPersonalAgentServiceImpl.create/update`). A pure rename would make the class semantically incoherent; a proper split (resolver vs. validator) is its own cleanup PR. v1 keeps the class name + updates the Javadoc to explain the dual-precedence role.
- Copilot: introduce `CopilotChatClientResolver` (sibling of the AI Hub one) and add an `overrideChatClientResolver` hook to each Copilot AG-UI agent. Wire via `ObjectProvider` so absence falls back to `@Primary ChatModel`.

Both endpoints use AG-UI `State` as the wire format — no new DTOs or REST/GraphQL endpoints needed.

### v2 (out of scope here; tracked for the next spec)

Adds **Personal agents** and **Workflow chats** sections to the popover.

- Picking a personal agent: navigates to / opens a conversation with that agent (changes the AI Hub task's `kind` to `PERSONAL_AGENT`, sets `aiHubPersonalAgentId`). Not a within-conversation switch — each agent gets its own conversation row.
- Picking a workflow chat: same shape, `kind = WORKFLOW_CHAT`, scoped to that workflow's existing chat thread (or a new one if none exists yet).
- These sections are only meaningful in the **AI Hub chat panel** context. They're hidden in Copilot (which has its own purpose-built agents) and hidden in the Personal Agent edit form (an agent can't reference another agent or a workflow chat as its "model").

v1 lays the component scaffolding so v2 is purely "add two more `DropdownMenuSub` entries above the providers and wire navigation."

## Precedence (server-side resolver)

```
user-selected (per conversation, sent in AG-UI state)   ⇽ highest
  ↓ if absent
personal-agent override (AI Hub PERSONAL_AGENT kind only, from agent row)
  ↓ if absent
workspace @Primary ChatModel (from AiModelConfiguration)  ⇽ lowest
```

For Copilot the middle layer is skipped (no personal-agent layer).

## Component design — `ModelPicker`

**Location:** `client/src/shared/components/ai/model-picker/ModelPicker.tsx`

**Props:**

```ts
interface ModelPickerPropsI {
    // Sentinel sources. When `selectedProvider` / `selectedModel` are both null,
    // the trigger renders one of these:
    //   1. agentDefaultProvider+Model → "Agent default" sentinel (AI Hub personal-agent conversations)
    //   2. workspaceDefaultLabel      → "Workspace default" sentinel (everywhere else)
    agentDefaultModel?: string | null;
    agentDefaultProvider?: string | null;

    /** Which optional sections to render in the popover. v1 only supports providers; v2 adds agents + chats. */
    enabledSections?: ('personal-agents' | 'workflow-chats')[];

    /** Compact trigger (icon + short model name) vs full trigger (icon + name + subtitle). */
    layout?: 'compact' | 'full';

    /**
     * (provider, model) → both null = revert to the highest-precedence default available
     *   (agent-default if provided, else workspace-default).
     */
    onChange: (provider: string | null, model: string | null) => void;

    selectedModel: string | null;
    selectedProvider: string | null;

    /** Used by the GraphQL queries. */
    workspaceId: number;

    workspaceDefaultLabel?: string;
}
```

**Internal data sources:**

- [`useWorkspaceAiGatewayProvidersQuery({workspaceId})`](../../../server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/workspace-ai-gateway-provider.graphqls) → `[AiGatewayProvider]` with `{id, type, name, enabled}`.
- [`useWorkspaceAiGatewayModelsQuery({workspaceId})`](../../../server/ee/libs/automation/automation-ai/automation-ai-gateway/automation-ai-gateway-graphql/src/main/resources/graphql/workspace-ai-gateway-model.graphqls) → `[AiGatewayModel]` with `{id, name, alias, providerId, capabilities, contextWindow}`.

Both already merge `application.yml` + AI Providers admin page on the server side (the same merge `AiTextUtils.getProviderOptions` does for Universal AI components). The client just consumes the resolved list — no merging in the browser.

**Internal structure (right-cascading submenus — `DropdownMenu` + `DropdownMenuSub`):**

- Root: shadcn `DropdownMenu` (built on Radix). Trigger = the compact button; content = `DropdownMenuContent`.
- **Search input** at the top of the content: a controlled `<input>` wrapped in a `DropdownMenuLabel`-style row. Stops `onKeyDown` propagation so the user can type freely without Radix intercepting arrow/letter keys for menu navigation. Query filters the outer rows (provider names) in place; matching providers expand a per-row "Matches" preview inline (one line per matching model).
- **Providers list** below the search: one `DropdownMenuSub` per provider.
  - `DropdownMenuSubTrigger` renders the provider icon + name + auto-rendered right chevron (Radix injects the chevron).
  - `DropdownMenuSubContent` renders right-side, anchored to the trigger row. Contains the provider's models as `DropdownMenuItem`s (each shows `alias || name`).
- **"Use default" sentinel row** at the top of the outer content (above the search input or just below it; design choice for the first PR — first below the search). Sentinel label = "Use agent default" if `agentDefault*` props are passed, else "Use workspace default" / `workspaceDefaultLabel`. Selecting it fires `onChange(null, null)`.
- Selecting a model row fires `onChange(provider, model)` (using the model's `name`, not `alias`) and closes the entire menu (root + any open sub).
- Hover / arrow-key navigation to a provider opens its sub-menu automatically (Radix default behavior). Click outside or `Escape` closes the entire menu.

**Why this primitive set over a cmdk page-stack:** the screenshot shows side-cascading submenus with `>` chevrons — `DropdownMenuSub` is the native primitive for that exact UX. Page-stack swaps content in place; the user asked for cascade. The tradeoff is that search-filter interaction is slightly more custom (the `<input>` lives inside the menu and has to stop key propagation), but the chassis matches the target UX without contortions.

**Provider iconography:** ByteChef ships SVGs per provider already (used in the AI Providers admin page). Reuse via a small `<ProviderIcon type="OPEN_AI" />` lookup. Missing icon → fall back to a circle with the provider's first letter.

**v2 expansion hooks (deliberately scaffolded, not wired):**

- `enabledSections` prop accepts `'personal-agents' | 'workflow-agents'` but v1 always treats it as `[]`. Adding these in v2 plugs in two more pages on top of the providers page.

## Call sites (v1)

### Copilot panel — `client/src/shared/components/copilot/CopilotPanel.tsx`

- Render `<ModelPicker layout="compact" workspaceDefaultLabel="Workspace default">` in the input toolbar (above or beside the send button).
- Selection stored in [useCopilotStore](../../../client/src/shared/components/copilot/stores/useCopilotStore.ts) as `selectedProvider` / `selectedModel`, both nullable.
- Reset on new conversation in the existing pathname/conversation-reset hook.
- [CopilotRuntimeProvider.tsx:20-62](../../../client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx:20) — extend `agent.runAgent()` to inject `state.userSelectedLlmProvider` + `state.userSelectedLlmModel` from the store.

### AI Hub chat panel

- Same component, same toolbar slot.
- Selection scoped to the **conversation** (AI Hub already has per-conversation state stores). Persist across messages in the same conversation; resets when user opens a different conversation row.
- Hide entirely when `task.kind === 'WORKFLOW_CHAT'` (those route through `WebhookBridgeAgent`, no LLM call to override).
- When `task.kind === 'PERSONAL_AGENT'`, pass `agentDefaultProvider={agent.llmProvider}` and `agentDefaultModel={agent.llmModel}` so the "default" sentinel reads as "Agent default" and reverts to the agent's pinned model.

### Personal Agent edit form — `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`

- Replace the existing two-dropdown block ([lines 434-507](../../../client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx:434)) with a single `<ModelPicker layout="full" workspaceDefaultLabel="Use workspace default">`.
- `selectedProvider` / `selectedModel` from local state (already tracked).
- `onChange` writes both into local state simultaneously (preserving the "both set or both null" invariant the server enforces).
- The `enabledSections={[]}` default keeps the agent-context picker free of recursive "pick an agent" sections.

## Server implementation map

### 1. AI Hub state keys

Add to [AiHubStateKeys.java:27-76](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/util/AiHubStateKeys.java):

```java
public static final String USER_SELECTED_LLM_PROVIDER_KEY = "userSelectedLlmProvider";
public static final String USER_SELECTED_LLM_MODEL_KEY = "userSelectedLlmModel";
```

### 2. Widen `PersonalAgentChatClientResolver` (rename deferred — see "Server changes" above)

Update `resolve(State)` to check user-selected keys first. Class name stays for v1 (the rename to a generic name would clash with the personal-agent-specific `validate()` method this class also holds — see deferred cleanup note above):

```java
String llmProvider = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
String llmModel    = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

if (llmProvider == null || llmModel == null) {
    // Half-set user state → fall through with a single warning log.
    if ((llmProvider == null) != (llmModel == null)) {
        logger.warn("User-selected LLM half-set (provider={}, model={}); falling through", llmProvider, llmModel);
    }
    // Fall back to the personal-agent override (already injected by AiHubRoutingAgent).
    llmProvider = asString(state.get(AiHubStateKeys.PERSONAL_AGENT_LLM_PROVIDER_KEY));
    llmModel    = asString(state.get(AiHubStateKeys.PERSONAL_AGENT_LLM_MODEL_KEY));
}

if (llmProvider == null || llmModel == null) {
    return null; // → AiHubSpringAIAgent falls back to @Primary ChatModel
}
// rest unchanged: resolve provider via AI Gateway, build ChatClient
```

Half-set states (one of provider/model null) at any precedence level → fall through to next level; emit a single warning log per request, no 400.

### 3. Copilot — override hook + resolver

Three Copilot agents (`CodeEditorSpringAIAgent`, `WorkflowEditorSpringAIAgent`, `ClusterElementSpringAIAgent`) need the same `protected ChatClient resolveChatClient(RunAgentInput input)` override pattern that [AiHubSpringAIAgent:165-187](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubSpringAIAgent.java) has.

**Implementation strategy:** lift the override mechanism out of `AiHubSpringAIAgent` into a small `OverridableChatClientAgent` mixin/interface in `server/ee/libs/platform/platform-ai-*` (so it doesn't have to be duplicated 3× in Copilot agents). Fallback if the lift creates ripple beyond one or two files: just copy the pattern inline into each Copilot agent. Choose at implementation time based on what slots in cleanly.

Add `CopilotChatClientResolver` in `server/ee/libs/ai/ai-copilot/ai-copilot-service/` — same shape as `AiHubChatClientResolver`, reads:

```java
String llmProvider = asString(state.get("userSelectedLlmProvider"));
String llmModel    = asString(state.get("userSelectedLlmModel"));
```

(Copilot state keys aren't typed in an enum-style holder today; we can either add a `CopilotStateKeys` constants class or use raw strings. Raw strings is fine for v1.)

### 4. Wire via `ObjectProvider`

In [CopilotConfiguration.java](../../../server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/config/CopilotConfiguration.java): inject `ObjectProvider<CopilotChatClientResolver>` and call `.ifAvailable(builder::overrideChatClientResolver)` on each Copilot agent builder. Absent → falls back to `@Primary ChatModel`. Exact mirror of [AiHubConfiguration.java:206-324](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/config/AiHubConfiguration.java).

## Out of scope

- **v2 sections** (Personal agents, Workflow agents) — separate spec; v1 ships the picker chassis.
- **Per-message temperature / top-p / max-tokens overrides.** v1 is provider + model only.
- **Workflow-chat per-conversation overrides.** Workflow chat routes through the workflow's own LLM component, not the AI Hub LLM agent. No-op.
- **Pinning a user-default different from the workspace default.** Future spec if requested; today every conversation starts at workspace default and the user adjusts per conversation.
- **A new REST/GraphQL endpoint to fetch available (provider, model) pairs.** The existing `workspaceAiGatewayProviders` + `workspaceAiGatewayModels` queries already serve this. Reuse, don't duplicate.

## Threat model

- **Model exfiltration / probing.** The picker only shows providers/models the workspace already has enabled — same disclosure surface as the Personal Agent form today. No new attack surface.
- **Bypassing workspace guardrails.** The resolver constrains the `(provider, model)` pair to the workspace's enabled set (`resolveProvider` returns `null` for unknown pairs → falls back to workspace default with a warning log). A malicious client passing a non-enabled pair via state cannot escape the workspace's allowed set.
- **Cost attribution.** `ai_llm_usage` rows record `model` per turn — no change. User-selected models surface in usage analytics the same way agent-overridden models do today.

## Sequencing

Ship as one PR; each piece is small and there's no cross-feature gating. Total ~2 days.

1. AI Hub server: state keys + resolver precedence + class rename (~0.25 day).
2. Copilot server: agent override hook + resolver + wiring (~0.5 day).
3. Client: `ModelPicker` component + tests (~0.5 day).
4. Client: Personal Agent form refactor to use `ModelPicker` (~0.1 day).
5. Client: Copilot wiring + store changes (~0.25 day).
6. Client: AI Hub wiring (~0.25 day).
7. Tests + docs (~0.25 day).

The riskiest step is **#2 — Copilot doesn't have the resolver hook today** and the override mechanism needs to slot into the AG-UI agent base classes cleanly. Lifting from AI Hub may pull in agent-base-class refactors; copying inline is the safe fallback.

## Acceptance

- A user opens the Copilot panel, picks "Claude Opus 4.7" from the new picker, sends a message. Server trace shows the request routed through Anthropic with model `claude-opus-4-7`, regardless of workspace default.
- The user opens AI Hub, starts a new conversation, picks "GPT-4o" — message routes through OpenAI. New conversation = selection resets to workspace default.
- Opening a personal agent with `llmModel = "claude-3-5-sonnet"` preselects Sonnet in the picker (showing "Agent default" sentinel). User overrides to GPT-4o for one message, picks "Use agent default", next message goes back through Sonnet.
- An admin disables the OpenAI provider in the AI Providers admin page. The picker no longer offers OpenAI models. Any conversation that had GPT-4o selected falls back to workspace default on next message with a warning log.
- Workflow-chat conversations show no picker.
- Personal Agent edit form uses the same `ModelPicker` (one trigger, popover with provider list) instead of two side-by-side Selects; save round-trip persists `(llmProvider, llmModel)` exactly as before.
