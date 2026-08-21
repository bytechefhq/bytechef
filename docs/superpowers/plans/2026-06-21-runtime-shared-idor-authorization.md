# Runtime-shared IDOR authorization — plan (T29, T30)

**Date:** 2026-06-21
**Spec:** `docs/superpowers/specs/2026-06-21-runtime-shared-idor-authorization-design.md`

Tiny, independently-verifiable commits. T29 (AiSkill) is fully actionable now; T30 instances need a
short investigation step each and are sequenced after.

## Phase A — T29: AiSkill per-user owner-isolation

### A1. Add `AiSkillApiFacade` interface
- New `com.bytechef.platform.ai.skill.facade.AiSkillApiFacade` in `platform-ai-skill-api`, declaring the
  10 entry-point methods from spec §2.2 (the subset the GraphQL + download controllers use). Reuse the
  existing `AiSkillFacade.AiSkillDownload` record type.
- **Verify:** `./gradlew :…:platform-ai-skill-api:compileJava`.

### A2. Implement `AiSkillApiFacadeImpl` (owner-isolation)
- New impl in `platform-ai-skill-service`, `@Service`, constructor-injects `AiSkillFacade`.
- Private `checkOwnerOrAdmin(long id)`: admin bypass via
  `SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)`, else
  `SecurityUtils.checkCurrentUserLogin(aiSkillFacade.getAiSkill(id).getCreatedBy())`.
- By-id methods call `checkOwnerOrAdmin(id)` then delegate. `getAiSkills()` filters to
  `createdBy == fetchCurrentUserLogin()` (all if admin). `create*` delegate directly (owner set by
  `@CreatedBy`).
- Both symbols are already on the classpath via `platform-api`; no new dependency.
- **Verify:** `./gradlew :…:platform-ai-skill-service:compileJava`.

### A3. Unit test `AiSkillApiFacadeImplTest`
- Mock `AiSkillFacade`; drive `SecurityContextHolder` (or `SecurityUtils.runAs`) for owner / non-owner /
  admin / unauthenticated. Assert: owner & admin succeed; non-owner + `null createdBy` throw
  `AccessDeniedException` **and** the delegate mutation is never called; `getAiSkills()` filtering.
- **Verify:** `./gradlew :…:platform-ai-skill-service:test --tests '*AiSkillApiFacadeImplTest'`.

### A4. Route the two controllers through `AiSkillApiFacade`
- `AiSkillGraphQlController` (`platform-ai-skill-graphql`) — swap `AiSkillFacade` → `AiSkillApiFacade`
  for all 9 handlers.
- `AiSkillDownloadController` (`platform-ai-skill-rest`) — swap to `AiSkillApiFacade`;
  `downloadAiSkill` → `getAiSkillWithDownload`.
- Leave the AI runtime tools (`SkillsTools`, `ReadSkillsTools`, `AiAgentUtils*`) on `AiSkillFacade`.
- **Verify:** compile both controller modules + `:…:components:ai:agent:utils:compileJava` (runtime path
  unchanged); `spotlessApply`; checkstyle/pmd/spotbugs on the three AiSkill modules.

### A5. Mark T29 done in the tracker
- Flip `[ ] T29` → `[x]`, add the **Done** note (per-controller `AiSkillApiFacade`, login-based check,
  runtime path untouched, behavioural note about per-user skill lists).
- Commit message: `gecko Close AiSkill per-user IDOR via AiSkillApiFacade (T29)`.

## Phase B — T30: cluster (investigated 2026-06-21 — each needs its own per-surface design)

**Investigation outcome:** none of the three is a mechanical gate. Each shares its
facade/service/tool with runtime AI-agent (and, for B3, embedded) execution, so a blanket
`@PreAuthorize`/`hasWorkflowScope` on the shared bean would break those paths. Each needs the
AiSkill-style per-controller-facade or a context-aware query scope tied to its subsystem. Details below
so each can be picked up and done correctly.

### B3. `WebhookTriggerTest` (7.6/6.3/5.4) — per-caller, not a shared-facade gate
- `WebhookTriggerTestFacade.enableTrigger`/`disableTrigger(String workflowId, …)` is shared by **three**
  callers: the **automation** `WebhookTriggerTestApiController` (platform-user editor surface), the
  **embedded** EE `WebhookTriggerTestApiController` (`/internal`, an API-key principal with **no**
  `hasWorkflowScope`), and the **runtime** `WorkflowNodeTestOutputFacadeImpl` (calls `disableTrigger`
  after capturing a test webhook — no user context).
- So `hasWorkflowScope` on the facade would break **both** the embedded controller and the runtime
  auto-disable. `enableTrigger` has only the two controllers (no runtime), but the embedded one still
  blocks a blanket gate.
- **Design:** a per-controller facade for the *automation* controller gated with
  `hasWorkflowScope(#workflowId, 'WORKFLOW_EDIT')`; the embedded controller follows T23 (verify the
  `/internal` surface's own auth — likely admin/instance-scoped); the runtime keeps the shared facade
  (split `disableTrigger` into an ungated internal method if the automation gate is placed on the shared
  method).

### B1. `WorkflowEditorSpringAIAgent.createSystemMessage` (6.5) — copilot subsystem
- Runs inside the SpringAI agent (EE `ai-copilot`); reads `workflowId` from `State` and calls
  `workflowService.getWorkflow` with no scope. The fix is to validate `workflowId` resolves to a
  workflow in the **caller's** accessible workspace, but that requires the caller identity inside the
  agent execution — which the copilot propagates via its own auth-context mechanism
  (`STATE_AUTHENTICATION` / tool-context rehydration / `SecurityUtils.runAs`). Scope it there; do **not**
  gate the shared `WorkflowService`.

### B2. `ReadProjectWorkflowTools.searchWorkflows` (6.2) — agent-tool subsystem (3 wirings)
- `searchWorkflows(query, projectId=null)` → `ProjectWorkflowTools` → `getProjectWorkflows()` (no-arg =
  every project's workflows). `ProjectWorkflowTools` is wired into **AI-Hub**, **Copilot**, and the
  **Management MCP server** — three agent contexts. TenantContext bounds it to one tenant, but not to the
  caller's workspace/user within the tenant.
- **Design:** scope the no-`projectId` path to the caller's accessible workspaces (mirror T25 search
  providers), resolving the caller from each agent context's auth propagation. Not a `@PreAuthorize`.

### B4. Needs-review items (lower confidence; verify before any change)
- `AbstractApiKeyAuthenticationConverter` (7.8) — auth-infra, T3/T23/T4-adjacent.
- `ConnectedUserProjectWorkflowApiController` — T23 claims coverage but 0 `checkCurrentUserLogin`; verify.
- `PlatformConfigurationAuthorizeHttpRequestContributor` (5.3) — likely covered by T24's HMAC signing.

## Global verification gate
- `./gradlew spotlessApply check` for the touched modules (the app-context load is the regression net
  for `@PreAuthorize`/bean wiring), plus the existing agent-utils tests stay green to prove the runtime
  path is intact.
