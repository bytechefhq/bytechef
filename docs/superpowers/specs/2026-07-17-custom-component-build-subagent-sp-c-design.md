# Custom Component Copilot Subagent (SP-C) — Design

**Date:** 2026-07-17
**Status:** Approved (design)
**Sub-project:** SP-C of the Custom Components initiative (SP-A = settings source editing / create-empty, done; SP-B = AI Hub CRUD tools + open-in-panel, done).
**Area:** `ai-copilot` (`server/libs/ai/ai-copilot`), EE AI Hub (`server/ee/libs/ai/ai-hub`), EE `automation-ai-tool` (SP-B's `CustomComponentTools`/`ReadCustomComponentTools`).

## Problem

SP-B exposed custom-component CRUD to the **main** AI Hub agent as directly-registered tools
(`CustomComponentTools` in the BUILD catalog, `ReadCustomComponentTools` in the ASK catalog) plus the
`openCustomComponentTab` signaling tool. That works but diverges from the skills feature, where
authoring lives behind a **dedicated copilot subagent** (`skills_ask` / `skills_build`) rather than on
the main agent's tool surface. SP-C brings custom components to parity: a specialist subagent that
deeply knows the single-file JavaScript component contract and owns all CRUD, with the main agent
merely delegating to it.

## Scope decisions (from brainstorming)

- **Ask + build** — a full mirror of the skills pair: a read-only `custom_component_ask` subagent
  (Q&A / inspect) and a `custom_component_build` subagent (authors and iterates source). Matches
  `skillsAskSpringAIAgent` / `skillsBuildSpringAIAgent`.
- **Move CRUD behind the subagent** — remove the direct `CustomComponentTools` (BUILD catalog) and
  `ReadCustomComponentTools` (ASK + BUILD catalogs) registrations SP-B put on the main AI Hub agent.
  CRUD now lives **only** on the subagents' tool sets (build gets full CRUD + read; ask gets
  read-only). The main agent keeps `openCustomComponentTab` (signaling, both sites) and gains the
  delegating `custom_component_agent` tool — exactly how skills is wired: the skills build subagent
  carries `SkillsTools` (CRUD) but **not** `openSkillTab`; the main agent owns tab-opening and calls it
  after the subagent returns the built component's id.

## Non-goals

- Multi-file components / engine changes (never — single-file per SP-A).
- Python/Ruby create-empty (SP-A ships JS create only; the subagent authors JavaScript).
- Any change to SP-A settings behavior or SP-B's panel/artifact wiring (kept intact; only the
  main-agent tool registration is unwound).

## Server design

### 1. Subagent knowledge — the build prompt

`server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_custom_component_build.txt`
teaches the single-file JavaScript component contract that the GraalVM polyglot loader
(`ComponentHandlerPolyglotEngine`) expects the `eval` result to expose:

- Top-level members: `name` (String, required), `version` (Integer, required), `title`, `description`,
  `actions` (list of maps, each with `name` / `title` / `description` / `perform` function).
- The compile-gate authoring loop:
  1. `createCustomComponent(name, "JAVASCRIPT")` → returns the created id/name (create-empty from the
     JS starter template).
  2. `updateCustomComponentSource(id, source)` — compile-gated; a bad source throws and the tool
     returns the loader error. Iterate on the source until it loads. The component **name** cannot be
     changed by an update (name-lock in SP-A).
  3. `openCustomComponentTab(id, name)` to surface the finished component in the AI Hub panel.

`prompt_custom_component_ask.txt` is the read-only counterpart: answer questions about existing custom
components using `getCustomComponentSource` / `listCustomComponents`; do not mutate.

### 2. Subagent ChatClient beans (`CustomComponentAgentConfiguration`, EE `ai-hub-service`)

Mirror the `skillsAskSubAgentChatClient` / `skillsBuildSubAgentChatClient` chat-client beans (see the
module-placement note above for why these live in EE rather than CE `CopilotConfiguration`):
- `customComponentBuildSubAgentChatClient` — system prompt = `prompt_custom_component_build.txt`;
  tools = `CustomComponentTools` + `ReadCustomComponentTools` (no `openCustomComponentTab` — the main
  agent owns tab-opening).
- `customComponentAskSubAgentChatClient` — system prompt = `prompt_custom_component_ask.txt`; tools =
  `ReadCustomComponentTools`.

**Module placement — EE, not CE.** The skills sub-agent chat-client beans live in the CE
`CopilotConfiguration` because CE `SkillsTools` are available there. `CustomComponentTools` /
`ReadCustomComponentTools` are **EE** (`automation-ai/automation-ai-tool`), so a CE config cannot wire
them. The two custom-component sub-agent `ChatClient` beans therefore live in a new **EE** config
`CustomComponentAgentConfiguration` in `ai-hub-service` (mirroring `ResearchConfiguration`'s pattern:
`@ConditionalOnProperty(bytechef.ai.hub.enabled=true)`, prompt loaded via a `readPrompt` helper,
tools via `.defaultTools(...)`). There is **no** `SpringAIAgent` bean and no in-editor Copilot `Source`
enum change — SP-C is the AI Hub delegation path only.

### 3. Delegating tool — `CustomComponentAgentToolCallback`

New `server/libs/ai/ai-copilot/ai-copilot-tool/.../tool/CustomComponentAgentToolCallback.java`,
a clone of `SkillsAgentToolCallback`: implements `ToolCallback`, holds a `ChatClient`, uses
`CurrentAgentContext.callWith(CopilotAgentType.CUSTOM_COMPONENT_AGENT, parentAgent, ...)`, delegates
the incoming `request` to its sub-agent chat client and returns the result text. Tool name
`custom_component_agent`. Requires appending `CUSTOM_COMPONENT_AGENT("custom_component_agent", false)`
to the CE `CopilotAgentType` enum (append-only, mirroring `CLUSTER_ELEMENT_AGENT`).

### 4. `AiHubConfiguration` wiring

- Add `ObjectProvider<ChatClient> customComponentAskSubAgentChatClientProvider` and
  `...BuildSubAgentChatClientProvider` parameters to `registerCopilotSubAgentToolCallbacks`, and
  register (mirroring the `skills_agent` registration):
  - build site → `new ProgressReportingToolCallback(new CustomComponentAgentToolCallback(buildChatClient), "custom_component_agent")`
  - ask site → the ask chat client (matching however skills routes ask vs build).
- **Unwind SP-B's direct registration:** remove `CustomComponentTools` from `aiHubBuildGlobalToolCatalog`
  and `ReadCustomComponentTools` from **both** `aiHubAskGlobalToolCatalog` and
  `aiHubBuildGlobalToolCatalog` (all custom-component CRUD/read leaves the main agent). Keep
  `OpenCustomComponentTabToolCallback` registered on the main agent at both sites (recorder@BUILD,
  null@ASK) — the panel-open signaling and `CUSTOM_COMPONENT_REFERENCED` artifact recording are
  unchanged.
- Update `prompt_ai_hub_ask.txt` / `prompt_ai_hub_build.txt`: replace the direct-CRUD guidance with
  "delegate custom-component authoring/inspection to `custom_component_agent`"; keep the
  `openCustomComponentTab({customComponentId, name})` documentation.

## Data flow (build a custom component)

1. User asks the main AI Hub (BUILD) agent to "build a component that does X".
2. Main agent calls `custom_component_agent(request)` (the delegating tool).
3. `CustomComponentAgentToolCallback` runs the `custom_component_build` sub-agent loop:
   `createCustomComponent` → `updateCustomComponentSource` (iterate to green). The sub-agent returns a
   summary that includes the built component's id/name.
4. The main agent then calls `openCustomComponentTab({customComponentId, name})`, which records
   `CUSTOM_COMPONENT_REFERENCED` (SP-B, dedup-aware); the client opens the editable
   `CustomComponentDetail` panel.
5. The main agent relays the summary to the user.

## Error handling

- Source-compile failures surface from SP-A's facade as loader errors, wrapped by `CustomComponentTools`
  into tool errors the **sub-agent** reads and iterates on — the main agent only sees the final summary.
- Sub-agent/delegation failures propagate as the `custom_component_agent` tool's error text (mirrors
  `skills_agent`); `ProgressReportingToolCallback` reports progress/failures.

## Testing

- `CustomComponentAgentToolCallbackTest` — mirror `SkillsAgentToolCallback`'s test (delegates to the
  chat client, returns its text, name/description surface).
- `AiHubConfiguration` wiring: assert the direct `CustomComponentTools` is no longer in the BUILD
  catalog and `custom_component_agent` is registered (mirror the skills subagent registration test if
  one exists; otherwise a focused catalog/registration assertion).
- Reuse SP-B's `CustomComponentToolsTest` / `OpenCustomComponentTabToolCallbackTest` unchanged.

## Rollout / compatibility

- No enum / schema / client changes — SP-C is server-side copilot wiring only. The client panel,
  artifact kind, and sidebar from SP-B are untouched.
- The only behavioral change for the main agent is *how* custom-component work is reached (delegation
  vs direct tools); end-user capability is unchanged or improved (a specialist prompt).
- Additive prompt/bean changes; unwinding SP-B's two direct registrations is the only removal.
