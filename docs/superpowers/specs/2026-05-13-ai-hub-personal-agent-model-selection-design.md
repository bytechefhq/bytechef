# AI Hub personal agent — per-agent model selection design

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-13 | **Last updated:** 2026-05-13

## Why

The [AiHubPersonalAgent](../../../server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgent.java) entity today has `name`, `title`, `description`, `instructions`, and a tool list. There's **no per-agent LLM provider or model column**. Every personal agent in a workspace runs on whatever LLM the workspace has globally configured.

This is a product gap. "Personal agent" implies "their own personality AND their own brain," not just "their own instructions on top of the workspace's brain." A workspace running Claude Opus for general tasks might want a faster cheaper Sonnet 4.6 or even Haiku for a focused "code-style-checker" agent. Today that's impossible — all agents share the workspace default.

For voice specifically, it matters more: voice exposes latency differences between models more starkly than text does. A fast model = snappy conversation, a slow model = noticeable lag. Per-agent model choice lets a workspace optimise voice-heavy agents differently from chat-heavy ones.

After this ships, the per-task-kind model resolution is clean:

| Task kind | LLM model source |
|---|---|
| COPILOT | Workspace default |
| PERSONAL_AGENT | Per-agent override → falls back to workspace default if null |
| WORKFLOW_CHAT | Workflow's own LLM component (always was — no change) |

## What ships

Two new nullable columns on `ai_hub_personal_agent`, plus the routing-agent + Spring-AI integration so they're honored:

```sql
ALTER TABLE ai_hub_personal_agent
    ADD COLUMN llm_provider VARCHAR(64) NULL,
    ADD COLUMN llm_model VARCHAR(128) NULL;
```

- `llm_provider` — e.g. `"openai"`, `"anthropic"`, `"google"`, matching the keys the workspace LLM provider registry uses.
- `llm_model` — provider-specific model id (e.g. `"gpt-4o-mini"`, `"claude-3-5-sonnet-20241022"`).

Both nullable. **Both null → fall back to workspace default.** **Both set → use them.** Setting only one is invalid (validation at service layer; reject with a typed exception that surfaces as a GraphQL error).

## Implementation map

### 1. Entity + repository

- Add `llmProvider`, `llmModel` fields to `AiHubPersonalAgent` with `@Column` + getters/setters.
- No repository changes (existing CRUD covers the new fields).

### 2. Liquibase migration

`20260513000004_ai_hub_personal_agent_add_llm_model.xml`:

```xml
<changeSet id="20260513000004-1" author="Ivica Cardic">
    <addColumn tableName="ai_hub_personal_agent">
        <column name="llm_provider" type="VARCHAR(64)"/>
        <column name="llm_model" type="VARCHAR(128)"/>
    </addColumn>
    <rollback>
        <dropColumn columnName="llm_provider" tableName="ai_hub_personal_agent"/>
        <dropColumn columnName="llm_model" tableName="ai_hub_personal_agent"/>
    </rollback>
</changeSet>
```

### 3. Routing agent overlay

In [AiHubRoutingAgent.applyAiHubPersonalAgentOverlay](../../../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java), inject two more state keys when the agent's `llmProvider` + `llmModel` are set:

- `AiHubStateKeys.PERSONAL_AGENT_LLM_PROVIDER_KEY`
- `AiHubStateKeys.PERSONAL_AGENT_LLM_MODEL_KEY`

When the agent's fields are null, don't inject the keys — `AiHubSpringAIAgent` falls back to the workspace default.

### 4. AiHubSpringAIAgent integration

In `AiHubSpringAIAgent.getChatRequest` (or wherever the `ChatClient` is built), consult the state keys. If both are present, build the `ChatClient` against that provider/model instead of the workspace default.

The workspace LLM provider registry already knows how to construct a `ChatModel` for any (provider, model) pair (this is what `AiHubGateway` uses for routing). Reuse that lookup. If the (provider, model) pair is unknown — e.g. the admin deleted the connection between save time and run time — surface a typed error and fall back to workspace default with a warning log.

### 5. Cost attribution

`ai_llm_usage` rows already record `model` per turn so cost analytics work out of the box. No change to the usage table; the recorded `model` will just vary per agent.

### 6. GraphQL + UI

- Add `llmProvider`, `llmModel` to the `AiHubPersonalAgent` GraphQL type.
- Add them to the create/update mutation inputs.
- Personal-agent edit panel gains two dropdowns:
  - Provider dropdown — sourced from the workspace's configured providers (existing GraphQL query).
  - Model dropdown — sourced from the selected provider's available models (existing GraphQL query).
- Both nullable; "Use workspace default" is the first option in each dropdown (maps to null on save).

### 7. Validation

Service-layer guard:

```java
if ((llmProvider == null) != (llmModel == null)) {
    throw new IllegalArgumentException(
        "llmProvider and llmModel must both be set or both null");
}
```

Plus: validate at save time that the (provider, model) pair exists in the workspace's available providers. Otherwise surface a "model not available in this workspace" error to the UI.

## Voice implications

For voice in `PERSONAL_AGENT` tasks: today the model is workspace default + voice latency is dominated by network + provider hops. After this ships:

- A workspace can pick a fast-cheap model for a "voice receptionist" agent and a slow-smart model for a "research analyst" agent.
- Voice latency for the fast agent drops noticeably; the slow agent's responses justify the wait.
- Cost attribution to the per-agent model surfaces in usage analytics.

No additional voice-specific work needed — once `AiHubSpringAIAgent` honors the per-agent model, voice (which calls through the same agent) inherits the behaviour.

## Out of scope

- **Per-agent model for COPILOT.** Standard tasks still use workspace default. If users want per-task model selection later, it's a separate spec.
- **Per-agent fine-grained generation parameters** (temperature, top-p, max tokens, etc.). v1 is just provider + model. Generation parameters can be added later as additional nullable columns.
- **Cross-workspace personal-agent sharing.** Personal agents are per-user; this spec doesn't change that.

## Threat model

- **Model exfiltration / model-name probe.** The per-agent model is workspace-visible already (the admin chose it from the workspace's provider catalog). No new attack surface.
- **Bypassing workspace guardrails.** Per-agent model selection doesn't bypass workspace-level safety prompts — `AiHubSpringAIAgent`'s system-message construction always includes workspace defaults. The per-agent overlay (instructions) was already documented as additive, not replacing.

## Sequencing

Single ship, ~1.5 days:

1. Migration + entity (~0.25 day).
2. Routing agent overlay + state keys + Spring-AI agent integration (~0.5 day).
3. GraphQL schema + mutation + service-layer validation (~0.25 day).
4. UI dropdowns in personal-agent edit panel (~0.25 day).
5. Tests + docs (~0.25 day).

Ship as one PR; no incremental order needed because each piece is small.

## Acceptance

A workspace admin creates a personal agent, picks "Claude 3.5 Sonnet" from the new model dropdown, saves. Opening that agent in AI Hub (text or voice) drives requests through Claude 3.5 Sonnet, regardless of what the workspace default model is. Switching to a different personal agent in the same session uses that agent's model. Setting the dropdowns back to "Use workspace default" reverts to the workspace's globally configured model. Cost attribution in usage analytics shows the correct model per turn.
