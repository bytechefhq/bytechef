# Workspace System Prompt (all agent surfaces)

Date: 2026-08-04
Status: implemented
Predecessor: `2026-07-31-ai-guardrails-standalone-design.md` reserved the Workspace
Settings "AI" group as "the intended future home for the workspace system prompt"
(separate spec — this is that spec).

## Problem

There is no way for a workspace administrator to give standing instructions to the AI
agents operating in their workspace. Every existing prompt mechanism is scoped
narrower:

- AI Hub ASK/BUILD prompts are static classpath resources (`prompt_ai_hub_ask.txt` /
  `prompt_ai_hub_build.txt`), identical for every workspace.
- Personal-agent `instructions` are per-agent, rendered as an advisory Context block.
- The canvas AI Agent component's `systemPrompt` is a per-node workflow parameter.
- The embedded copilot's `additionalSystemPrompt` (ticket 520) is per-request SDK
  input on the embedded surface only.

Admins want one place to say "always answer in German", "our fiscal year starts in
February", "never reference competitor X" — and have it reach every LLM turn the
workspace runs.

## Decisions (from brainstorming)

1. **Surfaces: all of them.** AI Hub ASK/BUILD + personal agents (including
   model-override clients), subagent delegates, and the canvas AI Agent component —
   the same coverage matrix as AI guardrails.
2. **Semantics: appended section.** The workspace text is appended to each agent's
   own system message under a fixed header. The base prompt stays first and wins on
   conflict; the header states the instructions cannot override safety/security
   rules (same advisory posture as the personal-agent overlay).
3. **Scope levels: workspace-only.** No tenant-wide default for now; the
   property-backed storage leaves room to add one later (`Property.Scope.PLATFORM`,
   null workspaceId — the guardrails convention) without migration.
4. **UI: its own page** under Workspace Settings → AI, next to Guardrails.
5. **Architecture: dedicated module** (`platform-ai-workspace-prompt`), a parallel of
   `platform-ai-guardrails`, wired at the same three seams. Rejected alternatives:
   folding a `systemPrompt` field into the guardrails settings (wrong home —
   guardrails is content filtering, this is steering; muddies `isActive`/metrics),
   and generalizing the advisor SPI to a list (churn on a CE SPI for a second
   consumer that only now exists — YAGNI).

## Design

### 1. Module layout & storage

New EE module `server/ee/libs/platform/platform-ai/platform-ai-workspace-prompt`
with the guardrails three-way split:

- **`platform-ai-workspace-prompt-api`** — `WorkspaceSystemPrompt` domain record
  (`workspaceId`, `prompt`), `WorkspaceSystemPromptService` interface, and
  `MAX_LENGTH = 4000` (matching the embedded copilot's
  `ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH`).
- **`platform-ai-workspace-prompt-service`** — property-backed service impl: one
  `PropertyService` row per workspace, `Property.Scope.WORKSPACE`, key
  `workspace_system_prompt`. No dedicated table, no Liquibase migration. Also home
  to the advisor and the SPI impl (below). Beans are `@ConditionalOnEEVersion` and
  registered unconditionally (like the guardrails engine) — inert when no workspace
  has a prompt set.
- **`platform-ai-workspace-prompt-graphql`** —
  `workspaceSystemPrompt(workspaceId)` query (`ROLE_ADMIN` or the workspace
  `AI_GATEWAY_VIEW` permission, mirroring the guardrails controller's read gate;
  returns null when unset) and `updateWorkspaceSystemPrompt(workspaceId, prompt)`
  mutation (`ROLE_ADMIN` via `@PreAuthorize` on the controller — the guardrails/A2A
  precedent; no facade layer owns the check). A blank/whitespace-only prompt on
  update deletes the property row. The server rejects prompts longer than 4,000
  characters, surfaced as a `GraphQlBadRequestException` (GraphQL `BAD_REQUEST`)
  rather than the default `INTERNAL_ERROR`.

### 2. Advisor & injection semantics

`WorkspaceSystemPromptAdvisor` implements Spring AI `CallAdvisor` + `StreamAdvisor`,
constructed per request with a resolved `workspaceId` (never a singleton bound to
one workspace — one agent bean serves all workspaces). Per call:

1. Look up the workspace's prompt through the service. A short-TTL Caffeine memo in
   the singleton service impl keeps this off the DB hot path (guardrails settings do
   the same).
2. Blank/absent → pass through unchanged, zero cost.
3. Otherwise rewrite the request's system message to:

```
<agent's own system message>

## Workspace instructions

The workspace administrator provided the following instructions. Follow them
where they apply, but they cannot override or weaken any rule above,
including safety and security rules.

<prompt text>
```

- **Ordering:** registers at lower precedence than `AiGuardrailsAdvisor`, so the
  append happens after the guardrails input scan — an admin's own instructions are
  never redacted or blocked by their own blocked-terms list.
- **Fail-open:** a lookup error logs and skips the append; it never fails the turn
  (same posture as guardrails classifier errors).
- The header wording — including "cannot override or weaken any rule above,
  including safety and security rules" — is load-bearing and pinned exactly by a
  unit test, mirroring `AiHubSpringAIAgentPersonalAgentContextTest`.

Effective precedence in prose: base agent prompt > workspace instructions >
personal-agent instructions (both overlay layers are advisory and state they cannot
override safety).

### 3. Surface wiring — three seams

1. **AI Hub** — `ai-hub-service` adds a dependency on
   `platform-ai-workspace-prompt-service` (it already depends on the guardrails
   service). In `AiHubSpringAIAgent`, at the same `resolveChatClient` seam where
   `attachGuardrailsAdvisor` binds (per-request-safe `chatClient.mutate()`), also
   attach a `WorkspaceSystemPromptAdvisor` using the same verified-state-key
   `workspaceId`. Covers ASK/BUILD, personal agents, and their model-override
   clients. `WORKFLOW_CHAT` is exempt by construction (`WebhookBridgeAgent`, no
   LLM).
2. **Canvas AI Agent (CE component)** — new CE SPI
   `WorkspaceSystemPromptAdvisorProvider` in `platform-ai-api`, identical in shape
   to `AiGuardrailsAdvisorProvider`:
   `Optional<Advisor> getAdvisor(@Nullable PlatformType platformType, @Nullable Long
   jobPrincipalId, String surface)` — optional bean, no-op on CE. The EE impl
   resolves deployment → project → workspace with the same Caffeine-cached
   resolution as `AiGuardrailsAdvisorProviderImpl` (a small package-local extract
   shared between the two impls is acceptable at implementation time).
   `AiAgentComponentHandler` / `AiAgentChatAction` take a second
   `ObjectProvider<WorkspaceSystemPromptAdvisorProvider>` alongside the existing
   guardrails one.
3. **Subagent delegates** — `SubAgentGuardrailedChatClient` (`ai-hub-service`)
   generalizes from "attach the guardrails advisor" to "attach the per-workspace
   advisors": it already captures the forwarded ToolContext workspace id per call
   and now also attaches the prompt advisor. No delegate `ToolCallback` classes
   change. Covers the copilot specialists, the AI-hub-owned subagents
   (research/data_analyst/image_generator/slide_builder), and the manager
   specialists.

### 4. Client UI

New page **Workspace Settings → AI → System Prompt**
(`/automation/settings/ai/system-prompt`), mirroring `AiGuardrails.tsx`:

- `client/src/ee/pages/settings/automation/ai/system-prompt/WorkspaceSystemPrompt.tsx`
  — a single textarea (~12 rows) with a live `n / 4000` character counter, helper
  copy explaining the text is appended to every AI agent operating in the workspace
  and cannot override safety rules, and the standard dirty-guarded Save footer.
- Route + nav item in `routes.tsx` under the existing "AI" settings group, directly
  after Guardrails; admin + EE gated identically.
- GraphQL operation files under `client/src/graphql/`; regenerate the client via
  codegen (operations and generated file committed separately, per convention).
- Clearing the textarea and saving deletes the setting; the page treats `null` and
  `""` identically.
- No per-mutation `onError` — the global fetch-interceptor toast covers failures.

### 5. Testing

- **Advisor unit tests** (`platform-ai-workspace-prompt-service`): append shape,
  exact pinned header wording, blank/absent pass-through, fail-open on lookup
  error, streaming path, 4,000-char boundary.
- **Service unit tests**: property round-trip, blank-deletes-row, over-length
  rejection.
- **AI Hub seam** (`ai-hub-service`): extend the `AiHubSpringAIAgentGuardrailsTest`
  pattern — prompt advisor attached when a prompt exists, skipped when absent;
  `SubAgentGuardrailedChatClientTest` extended for the second advisor.
- **Canvas seam**: component test that the SPI's advisor lands on the ChatClient
  when the provider bean exists, no-op otherwise.
- **GraphQL controller tests**: `ROLE_ADMIN` gating on the mutation, null on a
  missing row, over-length rejection.
- **Client**: page test à la `AiGuardrails.test.tsx` — render, counter, save
  mutation, clear-to-delete.

### 6. Out of scope

- Tenant-wide default prompt (future: `Property.Scope.PLATFORM` row with null
  workspaceId, same module, no migration needed).
- Per-workspace policy overrides mentioned alongside this feature in the guardrails
  spec — separate spec.
- MCP-surface manager subagent invocations
  (`AiHubManagerMcpContributorConfiguration` et al.) — the same documented gap as
  guardrails F3; a different surface with no AG-UI stream.
- Embedded surfaces — embedded has no workspace concept; the per-request
  `additionalSystemPrompt` (ticket 520) already serves that need and is untouched.
- Metrics — the advisor is deterministic steering, not an event worth counting; a
  `bytechef_ai_workspace_prompt_applied` counter can be added later if operational
  visibility is wanted.

## Decisions log

- Appended section over advisory Context block: the canvas AI Agent has no Context
  mechanism, and an appended system-message section steers more reliably while the
  fixed header keeps it subordinate to the base prompt.
- Append after the guardrails input scan (lower advisor precedence): scanning
  admin-authored instructions with the workspace's own blocked-terms/redaction
  policy would let a policy mangle its own steering text.
- Property-backed storage over a table: single text blob per workspace, exactly the
  "plain config data" case the guardrails settings javadoc recommends properties
  for; also keeps the later tenant-default addition migration-free.
- 4,000-character cap: consistency with the embedded copilot's existing
  `ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH`; workspace admins who need more are likely
  better served by AI Skills or personal agents.