# Embedded Copilot — "Generate from Chat" (design)

- **Date:** 2026-06-20
- **Branch:** 0_732
- **Status:** Design — pending review
- **Author:** Ivica Cardic

## Summary

Embedded mode today lets a connected end-user create a workflow in two non-conversational
ways: hand it a full definition, or one-shot "Generate from Prompt" (`generate=true`, runs
`workflow_editor_build` headlessly with `autonomous=true`). This design adds a **conversational,
multi-turn, streaming** path so products embedding ByteChef can offer their users a "Generate from
Chat" experience: the user builds a workflow by chatting with the Copilot agent, watching it stream
its work, instead of firing a single prompt.

The surface is **chat-only** — no workflow editor is shown. It reuses the real Copilot agent
(`workflow_editor_build`) over the **AG-UI protocol** (the same protocol the web client's
`CopilotPanel` uses), exposed through a single new public, frontend-only, SSE endpoint under
`/api/embedded/v1`, and a new self-contained React component in `@bytechef/embedded-react`.

### How this differs from the web `CopilotPanel`

The web `CopilotPanel` is a **side assistant for an already-open workflow** — it has an ASK/BUILD
ModeSwitch, a workspace-scoped model picker, and edits the workflow currently in the editor. This
embedded surface is a **standalone, chat-only, BUILD-mode workflow builder** for connected
end-users with no editor visible. Same Thread/assistant-ui DNA, different product.

## Goals

- A connected embedded end-user can build a single workflow by chatting, with streamed responses.
- Maximum reuse of the existing Copilot agent, tools, and tool-call rendering.
- Frontend-only (browser → embedded JWT → `externalUserId`); no backend `externalUserId`-in-path
  variant.
- A new SDK component (`@bytechef/embedded-react`) and a demo page + menu item in the sample app.
- Minimal new server surface: exactly one new endpoint.

## Non-goals

- No workflow editor / builder shown alongside the chat.
- No ASK mode — BUILD-only.
- No model picker (the embedded surface has no workspace context to scope it).
- **No interactive connection pickers in v1.** The web client's `select-connection` /
  `create-connection` / `select-property-option` data-parts bind to workspace connection APIs and a
  workspace-scoped picker UI. In embedded, connections are created through the existing
  `useConnectDialog` flow *after* the workflow is built. v1 renders text, tool-call/reasoning
  display, and the `ask-user-question` data-part only. This is a deliberate scope cut, not a
  reframing of the feature — connection pickers are a candidate follow-up.

## Key decisions (resolved during brainstorming)

| Decision | Choice | Rationale |
|---|---|---|
| Transport | **AG-UI protocol** end-to-end (`@ag-ui/client` ↔ `agUiService.runAgent` SSE) | Reuses the real Copilot agent, tool-calls, and interactive data-parts; mirrors the web client. |
| Workflow lifecycle | **Standalone chat; skeleton created up front; same `workflowUuid` refined each turn** | Multi-turn sibling of one-shot "Generate from Prompt"; chat-only, no editor. |
| Skeleton creation | **Reuse existing `createFrontendProjectWorkflow`** with a minimal SDK-side definition | Avoids a new creation endpoint and an extra protocol; only the SSE `/chat` is new. |
| Mode | **BUILD-only** | It's a "build my workflow by chatting" surface; the agent can still answer inline. |
| Server placement | **New hand-written controller in `embedded-configuration-public-rest`** | SSE can't be code-generated; endpoint must live under `/api/embedded/v1` for embedded JWT auth. |
| SDK Thread | **Vendor a focused Thread + AG-UI runtime into `@bytechef/embedded-react`** | Keeps the embedded SDK self-contained; no coupling to `@bytechef/automation-chat`'s bespoke runtime. |

## Architecture & data flow

```
Sample app "Generate from Chat" page
  └─ <EmbeddedWorkflowChat jwtToken baseUrl environment onWorkflowReady/>   (@bytechef/embedded-react)
        1. on mount → POST /api/embedded/v1/automation/workflows   { definition: <minimal skeleton> }
                       → workflowUuid   (EXISTING endpoint; createFrontendProjectWorkflow)
        2. assistant-ui Thread + @ag-ui/client HttpAgent
             POST /api/embedded/v1/automation/workflows/{workflowUuid}/copilot/chat   (SSE, AG-UI events)
             headers: Authorization: Bearer <jwt>, X-Environment: <env>
             state per turn: { workflowUuid, mode: "BUILD" }
        3. each turn streams text + tool-calls; on run-finish → onWorkflowReady(workflowUuid)
```

Both endpoints are under `/api/embedded/v1`, so `EmbeddedApiKeySecurityConfigurer`
(`^/api/embedded/v[0-9]+/.+`) authenticates them automatically: the embedded JWT resolves to a
connected user, available via `SecurityUtils.fetchCurrentUserLogin()`. Both are `@CrossOrigin`
(browser-facing).

## Server design

### New endpoint

`ConnectedUserCopilotApiController` — new hand-written controller in
`server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/.../public_/web/rest`.

- ByteChef Enterprise license header + `@version ee`.
- `@RestController`, `@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")`.
- `@ConditionalOnCoordinator`, `@ConditionalOnEEVersion`,
  `@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")`
  (mirrors `CopilotApiController` so the bean is absent when copilot is disabled).

```java
@PostMapping(
    value = "/automation/workflows/{workflowUuid}/copilot/chat",
    produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@CrossOrigin
public SseEmitter copilotChat(
    @PathVariable String workflowUuid,
    @RequestBody AgUiParameters agUiParameters,
    @RequestHeader(value = "X-Environment", required = false) @Nullable EnvironmentModel xEnvironment) {

    String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");
    Environment environment = getEnvironment(xEnvironment);

    // Authz (IDOR) + resolve latest workflowId + allowed components for this embedded environment.
    CopilotChatContext context =
        connectedUserProjectFacade.prepareCopilotChat(externalUserId, workflowUuid, environment);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String tenantId = TenantContext.getCurrentTenantId();

    return embeddedCopilotChatService.chat(
        context.workflowId(), context.allowedComponentNames(), authentication, tenantId, agUiParameters);
}
```

Controller dependencies: `ConnectedUserProjectFacade`, `EnvironmentService`,
`EmbeddedCopilotChatService`. Body type `AgUiParameters` comes from `agui-core`.

### New facade method

On `ConnectedUserProjectFacade` (api) + `ConnectedUserProjectFacadeImpl` (service):

```java
record CopilotChatContext(long workflowId, Set<String> allowedComponentNames) {}

CopilotChatContext prepareCopilotChat(String externalUserId, String workflowUuid, Environment environment);
```

Implementation reuses existing building blocks:
- **Ownership / IDOR check + latest workflowId:** call the existing
  `getConnectedUserProjectWorkflow(externalUserId, workflowUuid, environmentId)` (already throws if
  the uuid is not owned by this connected user) and resolve the latest workflowId via
  `projectWorkflowService.getLastWorkflowId(workflowUuid)`.
- **Allowed components:** reuse the existing private `resolveAllowedComponentNames(environment)`.

No new creation method — the skeleton is created via the existing
`createFrontendProjectWorkflow(...)` path (see SDK below).

### New copilot service

`EmbeddedCopilotChatService` — interface in `ai-copilot-api`, impl in `ai-copilot-service`. Keeps
Copilot state-key/agent-map/`TaskTools` knowledge inside `ai-copilot` so the embedded module never
reaches into Copilot internals.

```java
SseEmitter chat(
    long workflowId, Set<String> allowedComponentNames, @Nullable Authentication authentication,
    String tenantId, AgUiParameters agUiParameters);
```

Impl responsibilities (mirrors `CopilotApiController.chat` + `CopilotWorkflowGeneratorImpl`):
- Take `agUiParameters.getState().getState()` and inject:
  - `workflowId` → resolved server-side value (never trusts the client).
  - `mode` = `Mode.BUILD.name()`, `autonomous` = `false` (interactive multi-turn).
  - `CopilotStateKeys.STATE_TENANT_ID` = `tenantId`.
  - `CopilotStateKeys.STATE_AUTHENTICATION` = `authentication` — the embedded API-key principal has
    no backing platform `User`, so the full `Authentication` is captured for tool rehydration on
    Reactor worker threads (same mechanism the one-shot generator uses).
  - allowed-component-names key (the same key `CopilotWorkflowGeneratorImpl` uses;
    `TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY`) when the set is non-empty.
- Look up `workflow_editor_build` from the injected `List<LocalAgent>` (keyed by `getAgentId()`),
  i.e. `(Source.WORKFLOW_EDITOR + "_" + Mode.BUILD).toLowerCase()`.
- Return `agUiService.runAgent(localAgent, agUiParameters)`.

Service dependencies: `AgUiService`, `List<LocalAgent>` (same wiring as `CopilotApiController`).

### Module dependency additions

- `embedded-configuration-public-rest`: `agui-core` (for `AgUiParameters`), `ai-copilot-api` (the
  new service interface). All heavy Copilot wiring stays in `ai-copilot-service`.

## Client SDK design — `@bytechef/embedded-react`

New directory: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/`.

### `EmbeddedWorkflowChat.tsx`

Public component. Props:

```ts
interface EmbeddedWorkflowChatPropsI {
    jwtToken: string;                                  // required
    baseUrl?: string;                                  // default 'https://app.bytechef.io'
    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION';  // default 'PRODUCTION'
    title?: string;
    description?: string;
    suggestions?: string[];
    onWorkflowReady?: (workflowUuid: string) => void;
    className?: string;
}
```

Behavior:
1. **On mount**, create the skeleton: `POST {baseUrl}/api/embedded/v1/automation/workflows` with
   headers `Authorization: Bearer {jwtToken}`, `X-Environment: {environment}`,
   `Content-Type: application/json`, body `{ definition: <minimal skeleton JSON> }`. Response is the
   `workflowUuid` string. Store it in component state. (The minimal skeleton is a small constant in
   the SDK; the agent overwrites it on the first build turn.)
2. Render the vendored `Thread` wrapped in `EmbeddedCopilotRuntimeProvider` once `workflowUuid` is
   known (show a lightweight loading state until then).
3. Call `onWorkflowReady(workflowUuid)` after the first successful run finishes (host can link to /
   open the workflow). Idempotent: fire once per uuid.

### `EmbeddedCopilotRuntimeProvider`

Vendored, mirrors `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx`
but parameterized for embedded:

- Builds an `@ag-ui/client` `HttpAgent`:
  - `url`: `{baseUrl}/api/embedded/v1/automation/workflows/{workflowUuid}/copilot/chat`
  - `headers`: `{ Authorization: 'Bearer ' + jwtToken, 'X-Environment': environment }`
  - `threadId`: a generated `conversationId`.
- Per turn, `agent.setState({ workflowUuid, mode: 'BUILD' })` then
  `agent.runAgent({ runId }, subscriber)`. Server resolves `workflowId`, authz, tenant, principal,
  allowed components — the client never sends those.
- `AgentSubscriber` maps AG-UI events into an assistant-ui external-store runtime (text streaming
  via `onTextMessageContentEvent`, tool calls, `ask-user-question` data-part, `onRunErrorEvent` →
  humanized error). Uses `useExternalStoreRuntime` + a small Zustand store, same pattern as the web
  client and `@bytechef/automation-chat`.

### Vendored UI

- `thread.tsx` — a focused assistant-ui Thread (composer, message list, suggestions). Adapted from
  the web client / automation-chat Thread; no ModeSwitch, no ModelPicker.
- `dataComponents` — `{ 'ask-user-question': … }` for v1. (Connection pickers deferred — see
  non-goals.)

### Exports & dependency footprint

- Export `EmbeddedWorkflowChat` (and `EmbeddedWorkflowChatPropsI`) from `src/main.ts`.
- New deps: `@ag-ui/client`, `@ag-ui/core`, `@assistant-ui/react`, `@assistant-ui/react-markdown`,
  `zustand`, `lucide-react`, the Radix primitives the Thread needs, `tailwind-merge`. This is a
  notable increase for a previously dependency-light SDK; the component is a separate named export so
  bundlers tree-shake it for consumers who only use `useConnectDialog` / `EmbeddedWorkflowBuilder`.

## Sample app (`bytechef-embedded-sample-app/front-end`)

- New page `src/app/automations/chat/page.tsx` rendering
  `<EmbeddedWorkflowChat onWorkflowReady={(uuid) => /* navigate to the new workflow */} />` (reuse
  the existing post-generate navigation used by `generate-workflow-dialog.tsx`).
- New dropdown item **"Generate from Chat"** beside "Generate from Prompt" (`page.tsx:141`), routing
  to the new page (e.g. `MessageSquareIcon`).

## Error handling

- **AG-UI `RunErrorEvent`** → humanized message rendered in the Thread (strip Java FQCNs), same as
  the web client.
- **Skeleton create failure** → page-level error state in `EmbeddedWorkflowChat` (no chat shown).
- **Invalid / expired JWT** → 401 from the embedded security filter; surfaced as an error.
- **Foreign `workflowUuid`** → `prepareCopilotChat` throws (ownership check) → 403; chat does not
  start.
- **No hard timeout.** The 10-minute latch is one-shot-only (`CopilotWorkflowGeneratorImpl`);
  interactive turns simply end when the agent finishes streaming.

## Testing

### Server
- `ConnectedUserCopilotApiControllerIntTest` — `/chat` returns `text/event-stream`; a `workflowUuid`
  not owned by the connected user → 403.
- `EmbeddedCopilotChatServiceTest` — state injection assertions (`mode=BUILD`, `autonomous=false`,
  `STATE_TENANT_ID`, `STATE_AUTHENTICATION`, allowed-components key, server-resolved `workflowId`);
  selects the `workflow_editor_build` agent; delegates to `agUiService.runAgent`.
- Facade test — `prepareCopilotChat` resolves the latest workflowId, enforces ownership, returns
  allowed components.
- All new server files carry the EE license header + `@version ee` (incl. tests).

### Client (Vitest)
- `EmbeddedWorkflowChat` creates the session on mount with correct headers and skeleton definition.
- Posts to `/copilot/chat` with state `{ workflowUuid, mode: 'BUILD' }` and the Bearer/X-Environment
  headers.
- Renders the Thread once `workflowUuid` resolves; fires `onWorkflowReady` once on run-finish.

### Sample app
- Smoke-level only (demo): menu item routes to the page; page mounts the component.

## Files (anticipated)

**Server (new):**
- `…/embedded-configuration-public-rest/.../public_/web/rest/ConnectedUserCopilotApiController.java`
- `…/ai-copilot-api/.../EmbeddedCopilotChatService.java`
- `…/ai-copilot-service/.../EmbeddedCopilotChatServiceImpl.java`
- tests for the above + facade.

**Server (modified):**
- `ConnectedUserProjectFacade` (api) + `ConnectedUserProjectFacadeImpl` — add `prepareCopilotChat`
  + `CopilotChatContext`.
- `embedded-configuration-public-rest/build.gradle.kts` — add `agui-core`, `ai-copilot-api` deps.

**Client (new):**
- `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/EmbeddedWorkflowChat.tsx`
- `…/embedded-workflow-chat/EmbeddedCopilotRuntimeProvider.tsx`
- `…/embedded-workflow-chat/thread.tsx`
- `…/embedded-workflow-chat/dataComponents.tsx`
- `…/embedded-workflow-chat/store.ts`
- tests.

**Client (modified):**
- `sdks/frontend/embedded/library/react/src/main.ts` — export `EmbeddedWorkflowChat`.
- `sdks/frontend/embedded/library/react/package.json` — new deps.

**Sample app (new/modified):**
- `front-end/src/app/automations/chat/page.tsx` (new).
- `front-end/src/app/automations/page.tsx` — add "Generate from Chat" menu item.

## Open questions

- Exact route/path for the sample-app page (`/automations/chat` assumed).
- Whether `onWorkflowReady` should fire once on first run-finish or on every turn (assumed: once per
  uuid).
- Whether the minimal skeleton definition should eventually move server-side (a default applied when
  `definition` is empty) to avoid the SDK owning that shape — deferred; client-owned skeleton for v1.