# Connected-user ownership authorization — design

Replace `EmbeddedAutomationAuthorizationSkipFilter` with one ownership decision for connected users, so the RBAC
gates apply to the embedded surface instead of being switched off for it.

## Problem

Every request authenticated as an embedded principal (a connected user's JWT, or an embedded API key naming a
connected user) runs inside `AutomationAuthorizationContext.callSkippingChecks`. Under skip, the resource-scope
checks return `true`: `hasPermission(#id,'Type','SCOPE')`, `hasWorkspaceScope*`, `hasResourceScope*`,
`hasWorkflowScope*`, `hasWorkflowScopeIfProjectWorkflow*`, `canUseConnectionIn*`. The embedded chain authenticates
on the shared `/graphql` and `/api/(automation|embedded|platform)/internal/**`, so a connected user holding a valid
JWT can call automation mutations on any id in the tenant — delete a project, drop a data table, edit a connection —
not only on their own resources.

What is already closed (not part of this design): only tenant admins mint EMBEDDED keys and only EMBEDDED keys
authenticate there (`49cbbcab86a`, `977e0bb4089`); tenant-admin, current-user, owner, role and every-environment
checks ignore skip (`cbe11ea60b5`); embedded admin endpoints are being gated `isTenantAdmin()` separately.

The skip cannot simply be deleted: the embedded workflow builder legitimately crosses the same checks (open, edit,
test, attach/stop, logs, publish, enable), and a connected user has no user row and no workspace membership, so
every one of them would deny.

## Key observation

Embedded has only two roles, tenant-wide ADMIN and USER, and a connected user holds neither. A connected user never
needs a role or scope answer. Every check they cross reduces to one question:

> Does this resource belong to **this** connected user, in **their** environment?

That replaces role/scope resolution with a single ownership predicate, much smaller than the membership resolver
branch `0_732` built.

## Goals

- A connected user can do everything the builder and SDK need on their own resources, and nothing else.
- The decision is a property of the principal, so it survives async hand-offs, `SecurityUtils.runAs` and
  `@SkipAutomationAuthorization` — a thread-local never did.
- Skip mode stops applying to connected users entirely; the filter is deleted.

## Non-goals

- Trusted system paths that already skip after their own authentication (MCP runtime tool list/calls, MCP save/delete
  listeners) keep `callSkippingChecks` for now. Moving them to an explicit system principal is a follow-up.
- Ungated reads that expose no ownership (component definitions, options, dynamic properties, validation, AI catalog)
  are out of scope; the decider neither helps nor breaks them.
- Members of the default workspace reaching connected-user projects through ordinary RBAC is a separate question.

## Design

### 1. The principal carries its environment and connected user

`EmbeddedApiKeyAuthenticationProvider` (and `EmbeddedMcpServerApiKeyAuthenticationProvider`) build the authenticated
token with the environment and the connected-user id instead of dropping them (today the token reports environment
`0` = DEVELOPMENT and no connected user). Pattern: `0_732`'s `PrincipalEnvironment` plus new
`AbstractApiKeyAuthenticationToken(long environmentId, User user)` / `EmbeddedApiKeyAuthenticationToken(long, long
connectedUserId, User)` constructors.

**The JWT's environment is authoritative.** The builder JWT carries an `environmentId` claim, ignored today, so the
same JWT sent with another `X-ENVIRONMENT` acts as another (auto-created) connected user. For a JWT, the environment
comes from the claim. A request whose `X-ENVIRONMENT` names a different environment is refused as bad credentials;
an absent header takes the claim. An embedded API key (no claim) keeps selecting the environment by header.

A small `ConnectedUserPrincipal` helper (platform-security-web-api) answers `fetchConnectedUser()` →
`Optional<(connectedUserId, externalId, environment)>` from the SecurityContext. It is the only way code recognises a
connected user; `instanceof` checks move behind it.

### 2. One decider, consulted first

New SPI in `automation-configuration-api`:

```java
public interface ConnectedUserAccessDecider {
    enum Decision { NOT_GOVERNED, GRANT, DENY }

    Decision decide(Serializable id, String resourceType, String scope);   // resource checks
    Decision decideWorkflow(String workflowId, String scope);              // workflow-UUID checks
    Decision decideWorkspace(long workspaceId, String scope);              // workspace-level checks
}
```

- `NOT_GOVERNED` when the current principal is not a connected user (CE, platform users, system).
- For a connected user it is never `NOT_GOVERNED`: unknown types and workspace-level checks are `DENY`.
- The EE implementation lives in `embedded-configuration-service`.

**Call sites.** Every place that reads skip today consults the decider first and returns its answer for a governed
principal, never reading skip:

- `AutomationPermissionEvaluator` (both `hasPermission` forms)
- `AutomationMethodSecurityExpressionRoot` (`hasWorkspaceScopeInEnvironment[Id]`, `hasResourceScopeInEnvironment[Id]`,
  `hasWorkflowScope*`, `hasWorkflowScopeIfProjectWorkflow*`)
- EE `PermissionServiceImpl` (`hasWorkspaceScope*`, `hasWorkspaceScopeForProject`, `hasResourceScope*`,
  `hasWorkflowScope*`, `canUseConnectionInWorkflow`, `canUseConnectionInWorkspace`)

**Precedence rule.** Decider before skip, everywhere. `@SkipAutomationAuthorization` and `callSkippingChecks` grant a
governed principal nothing. This makes the skip moot for connected users before the filter is deleted.

### 3. Ownership rules

The connected user owns exactly one project per environment (`connected_user_project`, project
`__EMBEDDED__<externalId>`). Resolve the resource to a project and compare.

| Resource | Rule |
|---|---|
| `Project` | GRANT iff it is the principal's connected-user project |
| Workflow UUID (`hasWorkflowScope*`, `…IfProjectWorkflow…`) | `projectRepository.findByWorkflowId` → project rule. No project (integration workflow) → DENY |
| `ProjectWorkflow`, `ProjectDeployment`, `ProjectDeploymentWorkflow` | → project → project rule |
| `Job`, `TestJob`, `TriggerExecution` | → workflow / deployment → project rule |
| `Connection` | GRANT iff owned (`connected_user_connection` or `integration_instance.connection_id`) |
| Template project/workflow (Automation Workflows, `__EMBEDDED_AUTOMATION__`) | read-only: `WORKFLOW_VIEW`, iff published to the connected user (`getPublishedProjects(ext, env)`). Listing and `copyWorkflowTemplate` are the only template operations a connected user has; the copy lands in their own project, which the project rule covers |
| Embedded `McpServer` / `McpTool` (no workspace row) | `MCP_VIEW` iff reachable through the connected user's integration instances |
| Workspace-level (`Workspace`, `DataTable`, `KnowledgeBase*`, `ApiKey`, workspace `Mcp*`, `hasWorkspaceScope*`) | DENY |
| Anything else | DENY |

Scope is ignored on a GRANT for the connected user's own project: they own it outright (open, edit, test, publish,
deploy, enable, delete). The resource → project path reuses the existing `ResourceOwnershipResolver`s, which already
walk through the project for every type above. `ResourceOwner` gains an `OptionalLong projectId`. The connected-user
project id is memoised per request.

### 4. Environment

For a governed principal, the principal's environment is authoritative and the environment argument of a gate is
inert. The builder sends `environmentId` = DEVELOPMENT from its store while its `X-ENVIRONMENT` defaults to PRODUCTION,
and several gates hard-code DEVELOPMENT; ownership of the connected-user project for the principal's environment is
the whole answer. `canUseConnectionInWorkflow` compares against the principal's environment.

### 5. Copilot and async hand-offs

The copilot hand-offs set skip when `STATE_AUTHENTICATION` is present (`WorkflowEditorSpringAIAgent`,
`RehydrateContextToolCallback` via `CopilotToolContextUtils`). They instead bind the carried authentication, and the
tenant, on the worker thread, so the decider sees the connected user. Pattern: `0_732`'s `f2c765b633e`.

## Rollout

1. **Principal.** Carry environment and connected user on the tokens; add `ConnectedUserPrincipal`. No behaviour
   change.
2. **Decider, log-only.** Add the SPI, the EE implementation and all call sites. Governed decisions are computed and
   logged (WARN once per resource type + scope + decision) wherever the decider would DENY what skip grants; the
   returned answer is still skip's. Property `bytechef.embedded.connected-user-authorization.mode = log | enforce`,
   default `log`.
3. **Observe.** One full embedded builder session and the SDK flows (connect dialog, integration instances, MCP
   tools, copilot): build, test, attach/stop, logs, publish, enable, delete. Every logged DENY is either a rule gap
   (fix the rule) or a real hole (keep denying).
4. **Enforce.** Default `enforce`; decider answers are authoritative for governed principals.
5. **Delete.** Remove `EmbeddedAutomationAuthorizationSkipFilter`, `@SkipAutomationAuthorization` on the connected-user
   facades and managers, the copilot skip, and the `mode` property. Skip remains only for the trusted system paths.

## Testing

- Decider unit tests per rule: own resource GRANT, another connected user's DENY, other environment DENY,
  workspace-level DENY, unknown type DENY, published template read-only.
- Call-site tests: a governed principal under `callSkippingChecks` is still denied another user's resource
  (proves decider-before-skip).
- End-to-end IntTest with real rows: two connected users, same tenant, one tries the other's workflow, deployment,
  test job and connection through real proxies; a workspace member's project; a data table.
- Builder regression: the embedded builder session steps in step 3 as an IntTest sequence against the real controllers.
- `EndpointGateCoverageTest` extended to `server/ee/libs/embedded`, with connected-user endpoints allow-listed with
  a reason.

## Outstanding after this design

- Trusted system paths still use skip; moving them to an explicit system principal is a follow-up.
- Ungated reads with no ownership (definitions, options, dynamic properties) stay as they are.
