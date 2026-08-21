# Runtime-shared IDOR authorization — design (T29, T30)

**Date:** 2026-06-21
**Status:** Design
**Companion tasks:** `gecko-remediation-tasks.md` T29 (AiSkill), T30 (AI-tool / Copilot cluster)
**Companion plan:** `docs/superpowers/plans/2026-06-21-runtime-shared-idor-authorization.md`

## 1. Problem

The gap re-sweep after T28 surfaced IDOR findings whose user-facing handlers share their
facade/service with **runtime AI-agent execution**. The headline instance is **AiSkill** (7 findings,
4× CVSS 8.8):

| Finding (report line) | Sev | Method |
| --- | --- | --- |
| 305, 323 | 8.8 | `AiSkillGraphQlController` — `aiSkill`/`aiSkills`/`aiSkillFilePaths`/`aiSkillFileContent`/`updateAiSkill`/`updateAiSkillContent`/`deleteAiSkill` accept `id` with no check |
| 272 | 8.8 | `AiSkillServiceImpl` — cross-user skill access |
| 314 | 8.8 | `AiSkillFacadeImpl` — `deleteAiSkill`/`updateAiSkillContent`/`getAiSkillFileContent` |
| 1209 | 6.2 | `AiSkillFacadeImpl` — `getAiSkill`/`getAiSkillFileContent`/`getAiSkillFilePaths` |
| 1104 | 6.5 | `AiSkillDownloadController` — archive theft via `getAiSkillWithDownload` |

`AiSkill` is **per-user**: `@CreatedBy String createdBy`, no `workspaceId`. Every API method takes a
bare `id` with zero authorization, so any authenticated user can read/update/delete/download any
other user's skills.

### 1.1 Why this is not a mechanical facade gate

The obvious fix — `@PreAuthorize("@permissionService.isResourceOwner('AiSkill', #id)")` on
`AiSkillFacadeImpl` — **breaks agent execution**. Verified callers of the *same facade/service*
methods (including the mutations) include the runtime AI tools:

- `server/ee/.../ai/tool/SkillsTools.java`, `ReadSkillsTools.java`
- `server/.../components/ai/agent/utils/cluster/AiAgentUtilsSkillsTool.java`,
  `.../action/AiAgentUtils{Update,Delete,AppendFilesTo}AiSkillAction.java`,
  `.../workflow/connection/SkillComponentConnectionFactory.java`

These run on worker / Reactor threads with **no platform-user `SecurityContext`** (the fail-closed
authorization ThreadLocal deliberately does not cross to workers — see the embedded generate-workflow
notes). `PermissionService.isResourceOwner` fails closed when there is no current user
(`ownerUserId.isPresent() && isCurrentUser(...)` → false), so a facade gate would deny the agent's own
skill tools.

This is the project's documented **"shared method → new per-controller facade wrapping the shared
service"** rule (controller-auth convention). The user-facing entry points need owner-isolation; the
runtime path must keep calling the shared service untouched.

## 2. Design

### 2.1 Decision: platform-native, login-based owner check (not the automation ownership framework)

The `ResourceOwnershipResolver` / `PermissionService` framework lives in the **automation** layer
(`com.bytechef.automation.configuration.security`). `AiSkill` lives in **platform**
(`com.bytechef.platform.ai.skill`, CE). Forcing AiSkill into that framework would require:

1. a backwards `platform → automation` dependency for the resolver, and
2. mapping `createdBy` (a **login string**) → a numeric `userId`, because `ResourceOwner.ownerUserId`
   is an `OptionalLong` while `isResourceOwner` compares user ids.

Both are avoidable. `platform-api` already provides
`com.bytechef.platform.security.util.SecurityUtils` (already a dependency of every AiSkill module):

- `fetchCurrentUserLogin(): Optional<String>`
- `checkCurrentUserLogin(String expectedLogin)` — throws `AccessDeniedException` on mismatch / no user
- `hasCurrentUserThisAuthority(String authority)` — for the tenant-admin bypass

Because `AiSkill.createdBy` **is** the owner's login, a direct login comparison is exact, self-contained
in platform, needs no new dependency, no resolver registry, and no id mapping.

### 2.2 New per-controller facade: `AiSkillApiFacade`

Add `AiSkillApiFacade` (interface in `platform-ai-skill-api`, impl in `platform-ai-skill-service`) that
wraps the existing `AiSkillFacade` and adds owner-isolation. It exposes exactly the operations the two
HTTP entry points need:

```
AiSkill getAiSkill(long id)                                  // owner-checked
List<AiSkill> getAiSkills()                                  // filtered to current user
List<String> getAiSkillFilePaths(long id)                   // owner-checked
String getAiSkillFileContent(long id, String path)          // owner-checked
AiSkill createAiSkill(...)                                   // owner = current user (no IDOR)
AiSkill createAiSkillFromInstructions(...)                  // owner = current user (no IDOR)
AiSkill updateAiSkill(long id, name, description)           // owner-checked
AiSkill updateAiSkillContent(long id, path, content)        // owner-checked
void deleteAiSkill(long id)                                  // owner-checked
AiSkillDownload getAiSkillWithDownload(long id)             // owner-checked
```

The owner check is one private helper, applied before delegating any by-id call:

```java
private void checkOwnerOrAdmin(long id) {
    if (SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {
        return;                          // tenant/system admin bypass, matches isResourceOwner semantics
    }
    AiSkill aiSkill = aiSkillFacade.getAiSkill(id);     // throws if not found
    SecurityUtils.checkCurrentUserLogin(aiSkill.getCreatedBy());   // AccessDenied on mismatch / no user
}
```

`getAiSkills()` filters: admins see all; otherwise return only rows whose `createdBy == currentLogin`.
`create*` set the owner via `@CreatedBy` auditing already, so they carry no IDOR and need no check (but
still require an authenticated user, which the security filter chain guarantees for these endpoints).

**Legacy/`null createdBy` rows:** `checkCurrentUserLogin(null)` throws (fails closed). Acceptable — a
skill with no recorded owner is not attributable to the caller. Documented as a deliberate
fail-closed.

### 2.3 Wire the two controllers to `AiSkillApiFacade`

- `AiSkillGraphQlController` (`platform-ai-skill-graphql`) — inject `AiSkillApiFacade` instead of
  `AiSkillFacade`; all 9 query/mutation handlers route through it.
- `AiSkillDownloadController` (`platform-ai-skill-rest`) — inject `AiSkillApiFacade`;
  `downloadAiSkill` → `getAiSkillWithDownload`.

The runtime AI tools (`SkillsTools`, `ReadSkillsTools`, `AiAgentUtils*`) are **untouched** — they keep
using `AiSkillFacade` / `AiSkillService`, so agent execution is unaffected.

### 2.4 What stays ungated (and why)

- `AiSkillFacade` / `AiSkillService` — unchanged; the runtime contract. The IDOR is closed at the only
  two surfaces that carry an authenticated user (GraphQL + download REST).
- `getAiSkillDownload(long)` (the raw-bytes facade method used by `SkillComponentConnectionFactory` /
  `SkillsTool` at runtime) — stays on `AiSkillFacade`, ungated. The user-facing download goes through
  `AiSkillApiFacade.getAiSkillWithDownload`, which is owner-checked.

## 3. T30 — the rest of the runtime-shared cluster (same pattern, scoped separately)

These share the same shape (user surface ⇄ runtime AI/execution path) and reuse either the
per-controller-facade pattern above or workflow-scoping:

- **`WebhookTriggerTestApiController` + `WebhookTriggerTestFacadeImpl`** (7.6 + 6.3/5.4) — workflow-scoped.
  Gate the editor-facing methods with `@permissionService.hasWorkflowScope(#workflowId, …)` like T22b,
  **but** `WebhookTriggerTestFacade` is also invoked from the runtime webhook-test path
  (`WorkflowNodeTestOutputFacadeImpl.disableTrigger`), so the runtime entry must be separated (a
  runtime-only method or self-invocation, as in T22b) before gating.
- **`copilot/agent/WorkflowEditorSpringAIAgent`** (6.5) — the Copilot agent reads arbitrary workflow
  data; scope the workflow lookup to the caller's accessible workspace (it carries the editor's
  `SecurityContext`; confirm before gating).
- **`tool/automation/ReadProjectWorkflowTools`** (6.2) — MCP tool-search lists all workflows; scope the
  underlying query to the caller's accessible workspaces (like T25 search providers), not a `@PreAuthorize`.

**Needs-review (separate, lower confidence, not part of this design):**
`AbstractApiKeyAuthenticationConverter` (7.8 env-boundary; auth-infra, T3/T23/T4-adjacent),
`ConnectedUserProjectWorkflowApiController` (T23 claims coverage but has 0 `checkCurrentUserLogin` —
verify), `PlatformConfigurationAuthorizeHttpRequestContributor` (5.3 trigger-form; likely covered by
T24's HMAC token signing).

## 4. Testing

- `AiSkillApiFacadeImplTest` (unit, mock `AiSkillFacade` + a `SecurityContext`):
  - owner can read/update/delete their own skill;
  - non-owner is denied (`AccessDeniedException`) on every by-id method, and the delegate is **not**
    invoked (assert no mutation) — a positive-only test would pass against the un-gated code;
  - tenant-admin bypasses;
  - `getAiSkills()` returns only the caller's rows for a non-admin, all rows for an admin;
  - `null createdBy` → denied (fail-closed).
- Verify the runtime path is untouched: `AiAgentUtilsSkillsTool` / `SkillsTools` still depend on
  `AiSkillFacade` (compile check + existing agent-utils tests stay green).

## 5. Risks / rollout

- **Behavioural change:** skills become per-user-private at the API. If any existing UI assumed a
  shared/global skill list, that surface now shows only the caller's skills (admins still see all).
  This matches the report's intent ("cross-user skill access" = a vulnerability) and the `createdBy`
  data model. Flag for product confirmation.
- **No DB / migration changes.** No new module dependencies (platform-api already present).
- **EE/CE:** AiSkill is CE; the fix is CE (Apache header). `SecurityUtils` + `AuthorityConstants` are
  platform-api, available in both editions.
