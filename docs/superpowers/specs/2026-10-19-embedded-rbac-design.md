# Embedded RBAC

Status: draft for review. Ticket: #1051 (no separate issue). Branch: stacked on `0_p_1051`.

## Intent

Today only tenant admins can use the embedded (iPaaS) side of ByteChef. About 107 `isTenantAdmin()` gates in
`server/ee/libs/embedded` and `PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}` on every embedded route make it
all-or-nothing. A team that builds integrations for its customers cannot give a support person read-only access, or let
a developer build integrations without also handing them signing keys and every customer's data.

The goal is full RBAC on the embedded side that works like automation RBAC (#1051): built-in Admin, Editor and Viewer
roles, optionally per environment, enforced on the server, with the UI showing only what the server allows. A person
who knows automation RBAC should find nothing new to learn, except that the container is "Embedded" instead of a
workspace.

## Goals

- Tenant users can be made embedded members with a built-in role (Admin, Editor, Viewer), either one embedded-wide role
  or one role per environment (Development, Staging, Production), exactly like automation workspace roles.
- Every internal embedded operation (GraphQL and REST) is authorised on the server by scope, not by tenant admin.
- The client shows embedded navigation, pages and controls according to the member's scopes, in the selected
  environment.
- Embedded membership changes are audited through the #746 `@Audited` machinery, like workspace membership.
- Per-customer access and embedded custom roles can be added later without reworking the model.

## Non-goals

- Custom roles for embedded. Custom roles stay automation-only for now.
- Per-customer (or third-party, per-resource) access. ByteChef has no "customer" entity today: a connected user is one
  end user with an `externalId`.
- Visibility and sharing (private / specific people) for integrations. Embedded is one shared space.
- Any change to what connected users (the customer's end users) can do through the public embedded API, the Automation
  Hub or the marketplace. Their ownership rules (`isConnectedUser`, `ConnectedUserAccessDecider`, the skip-checks
  context, `EmbeddedPermissionEvaluator` catalogue expressions) are untouched.

## Anti-goals (failure even if it works)

- A fake "embedded workspace" row in `workspace` / `workspace_user`. Embedded and automation roles must be independent:
  an automation workspace Editor gains nothing on embedded, and the reverse.
- Any existing tenant admin losing access, or any existing non-admin user gaining access they did not have.
- Member roles leaking into connected-user decisions, or connected-user decisions short-circuited by member roles.
- Client-only gating. Hiding a control without a server gate behind it is a failure.

## Decisions (made with the user)

1. **Container:** one tenant-wide embedded area. No embedded workspaces.
2. **Roles:** built-in `ADMIN` > `EDITOR` > `VIEWER`, ranked like `WorkspaceRole`, one per member or one per environment.
   Removing a member's last environment role turns it into an embedded-wide role, the rule restored on 1051.
3. **Membership is opt-in:** a tenant user sees nothing under Embedded until an embedded Admin or a tenant admin adds
   them with a role. Tenant admins keep full access everywhere and are not subject to per-environment roles.
4. **Integrations behave like automation projects:**
   - building an integration's workflows (create, edit, delete a workflow) needs the scope in **Development**, like
     `hasWorkflowScopeInEnvironment(..., DEVELOPMENT)` on project workflows;
   - integration-level operations (create, settings, publish, delete) use the environment-unaware check, like
     `PROJECT_CREATE`, `PROJECT_PUBLISH` and `PROJECT_DELETE`;
   - resources that live in an environment are checked against the member's role in that resource's environment, like
     deployments through `ResourceEnvironmentResolver`.
5. **Role capabilities mirror automation.** Each embedded area maps to its automation counterpart and copies its
   minimum role (catalogue below).
6. **Run details:** `EXECUTION_DETAILS_VIEW` (Editor) gates inputs, outputs, errors, trigger state and logs, as it now
   does in automation (`d6ecb483cca` on 1051).
7. **Custom roles:** not for embedded now. The membership row must be able to reference a custom role later.
8. **Automation Hub projects move to the embedded side.** The Hub templates and the connected users' own projects
   are stored in the automation default workspace (`AutomationWorkflowProjectFacadeImpl` and
   `ConnectedUserProjectWorkflowManager` set `DEFAULT_WORKSPACE_ID`), so default-workspace automation roles can reach
   them today. After this work automation checks and automation lists skip them, and only embedded roles, tenant admin
   and connected-user ownership apply. Where they are stored is the builder's call (a marker on the project, or no
   workspace), as long as no automation page or gate sees them.
9. **Embedded API keys are shared per environment.** Anyone with `API_KEY_VIEW` sees every embedded key in that
   environment, `API_KEY_DELETE` may delete any of them, and removing a member or deleting their tenant user does not
   revoke embedded keys they created. `ApiKeyRevoker` keeps revoking automation and personal keys as today. The
   creator is still recorded and shown.
10. **Per-customer access later:** it will be visibility-only grants in `resource_grant` (from #4750) on a future
   customer grouping, with the member's embedded role deciding the actions. Nothing in this design may assume that
   every embedded member sees every connected user forever.

## Scope catalogue

Scopes that name a concept both sides share keep the automation name, so shared facades (logs, jobs, connections,
workflows) check one string: `WORKFLOW_*`, `EXECUTION_*`, `CONNECTION_*`, `MCP_*`, `API_KEY_*`. Embedded-only areas
get new names. Which scopes a role grants is decided per side, so the registry must know which side a scope belongs to
(see "Builder's call").

| Embedded area | Automation counterpart | Scopes and minimum role |
|---|---|---|
| Integrations (and categories, tags) | Projects | `INTEGRATION_CREATE` Editor, `INTEGRATION_PUBLISH` Editor, `INTEGRATION_SETTINGS` Admin, `INTEGRATION_DELETE` Admin |
| Integration workflows | Workflows | `WORKFLOW_VIEW` Viewer, `WORKFLOW_CREATE` / `WORKFLOW_EDIT` / `WORKFLOW_DELETE` Editor, all checked in Development |
| Instance configurations (incl. MCP instance configurations) | Deployments | `INTEGRATION_INSTANCE_CONFIGURATION_VIEW` Viewer, `_CREATE` / `_EDIT` Editor, `_DELETE` Admin |
| Connected users (list, enable, disable, their instances and connections) | none | `CONNECTED_USER_VIEW` Viewer, `CONNECTED_USER_MANAGE` Editor, `CONNECTED_USER_DELETE` Admin |
| Automation Hub templates (Embedded > Automation Workflows, `AutomationWorkflowProjectAdminFacadeImpl`) | Projects | the `INTEGRATION_*` scopes and `WORKFLOW_*` in Development, as for integrations; editing `permissionExpression` (what connected users see) is `INTEGRATION_SETTINGS`, Admin |
| Connected users' own projects and workflows (`ConnectedUserProjectAdminFacadeImpl`, `ConnectedUserWorkflowReferenceAdminFacadeImpl`) | none | `CONNECTED_USER_VIEW` to read, `CONNECTED_USER_MANAGE` to change, `CONNECTED_USER_DELETE` to delete |
| Connected users' MCP servers (`ConnectedUserMcpServerFacadeImpl`) | MCP | `MCP_*` as for embedded MCP servers |
| App events | Data tables | `APP_EVENT_VIEW` Viewer, `APP_EVENT_CREATE` / `APP_EVENT_EDIT` Editor, `APP_EVENT_DELETE` Admin |
| Executions | Executions | `EXECUTION_VIEW` Viewer, `EXECUTION_DETAILS_VIEW` Editor, `EXECUTION_DELETE` Admin |
| Embedded connections | Connections | `CONNECTION_VIEW` Viewer, `CONNECTION_CREATE` / `CONNECTION_EDIT` / `CONNECTION_DELETE` Editor |
| Embedded MCP servers | MCP | `MCP_VIEW` Viewer, `MCP_CREATE` / `MCP_EDIT` Editor, `MCP_DELETE` Admin |
| Embedded API keys | API keys | `API_KEY_VIEW` / `API_KEY_CREATE` / `API_KEY_DELETE` Editor, on every key in the environment (decision 9) |
| Signing keys, embedded settings | none | `EMBEDDED_SETTINGS_MANAGE` Admin |
| Embedded members and their roles | Workspace member manage | `EMBEDDED_VIEW` Viewer, `EMBEDDED_MEMBER_MANAGE` Admin |

Connection edit and delete keep the 1051/4750 rule: the scope **and** ownership or tenant admin. Embedded connections
owned by connected users are not "owned" by members; a member with `CONNECTION_EDIT` manages them only through the
existing admin connection facade, and its current behaviour for tenant admins becomes the behaviour for that scope.

The existing coverage tests pin the catalogue and the gates and already scan `server/ee/libs/embedded`:
`PermissionScopeTypeTest` (every gate scope is in the catalogue; every catalogue scope is enforced or pinned as
unenforced), `ResourceOwnershipResolverTest` (every `hasPermission` resource type has a resolver) and
`AutomationMethodSecurityConfigurationTest` (every write endpoint is gated or listed). Embedded scopes, resolvers and
gates must satisfy them; their allowlists are not to be widened for embedded.

Webhook trigger testing from the integration workflow editor (`WebhookTriggerTestAdminFacadeImpl`) is a workflow write:
`WORKFLOW_EDIT` in Development.

## Resources and environments

| Resource | Lives in an environment? | Checked against |
|---|---|---|
| Integration, category, tag | No | environment-unaware (any environment role) for integration-level ops; Development for workflow writes |
| Integration workflow | No (built in Development) | Development |
| Integration instance configuration | Yes | its environment |
| Connected user, its instances | Yes | its environment |
| Embedded connection, MCP server, API key, signing key | Yes | its environment |
| Embedded job, trigger execution | Yes (via the instance configuration) | its environment |
| App event | No | environment-unaware |

Required: `PermissionServiceImpl.hasWorkflowScope`, `hasWorkflowScopeIfProjectWorkflow` and their environment
overloads deny non-tenant-admins for any workflow that is not a project workflow, and 21 `@PreAuthorize` gates in
`server/libs/platform` (node parameters, test runs, node tests, scripts, test configuration) use them. Integration
workflows go through those shared facades, so these methods get an embedded branch (workflow → integration → embedded
check in the given environment). Without it the integration workflow editor stays admin-only no matter what the
embedded facades gate.

## Enforcement

- `@PreAuthorize` gates move from `isTenantAdmin()` to scope checks, method by method, across the 23 embedded facades
  (`IntegrationFacadeImpl`, `IntegrationWorkflowFacadeImpl`, `IntegrationInstanceConfigurationFacadeImpl`,
  `ConnectedUserFacadeImpl`, `AppEventFacadeImpl`, `IntegrationWorkflowExecutionFacadeImpl`, `SigningKeyFacadeImpl`,
  the MCP facades, the connected-user admin facades, `ConnectionAdminFacadeImpl`, `WebhookTriggerTestAdminFacadeImpl`,
  and others). Operations that are genuinely tenant-level stay `isTenantAdmin()` and are listed in the docs, as 1051
  did for automation.
- Shared facades that already take `hasPermission(#id, '<Type>', '<SCOPE>')` (jobs, logs, trigger executions,
  connections, workflows) must answer for embedded resources through embedded ownership and environment resolvers,
  instead of falling through to "deny unless tenant admin".
- `IntegrationWorkflowExecutionFacadeImpl` redacts run details without `EXECUTION_DETAILS_VIEW`, using the
  `withoutDetails()` helpers added on 1051.
- The shared `ApiKeyFacadeImpl` enforces ownership in plain Java, not SpEL: `create(apiKey, type)` is tenant-admin
  only for non-automation keys, `getApiKeys` filters to the caller's own keys, and `revokeAll(userId)` revokes every
  key of a departing user. Decision 9 needs an embedded branch there: `PlatformType.EMBEDDED` keys are created with
  `API_KEY_CREATE`, listed and deleted by scope in their environment without an owner filter, and skipped by
  `revokeAll`. Automation and personal keys keep today's rules.
- Decision 8 is a behaviour change for automation: members of the default workspace lose list and by-id access to
  Automation Hub projects and connected users' projects. `automation_workflow_project` (one row per Hub project) and
  the connected-user project link are the existing markers; no new column is needed to tell them apart.
- The connected-user decision keeps precedence: it is evaluated first, exactly as `decideForConnectedUser` does now.
- Lists filter to what the caller can see in the selected environment; by-id reads check the same scope, so a list
  never hides what a by-id read would return.
- The scope cache, eviction on membership change, and `myWorkspaceScopes(workspaceId, environment)` have an embedded
  counterpart (`myEmbeddedScopes(environment)` or equivalent).

## Membership

- A new EE table for embedded membership (user, role, nullable environment, nullable custom role reference, audit
  columns), unique per user and environment, mirroring `workspace_user`. No backfill: existing tenant admins need no
  rows, and nobody else had access.
- Add, change role, set per-environment roles, remove, with the same guards as automation: the caller needs
  `EMBEDDED_MEMBER_MANAGE` (in every environment for embedded-wide changes) and may not grant beyond their own scopes.
  There is no last-Admin protection: embedded may have no Admin member, because tenant admins can always manage it.
  An Admin may step down or remove themselves.
- Removing a tenant user removes their embedded membership.
- Events `EMBEDDED_MEMBER_ADDED`, `EMBEDDED_MEMBER_REMOVED`, `EMBEDDED_MEMBER_ROLE_CHANGED` recorded through
  `@Audited` mappers, and denied checks recorded as `PERMISSION_CHECK` like everywhere else.

## Client

- Embedded routes guard on embedded membership and scopes instead of `AUTHORITIES.ADMIN`. The app shell shows the
  Embedded navigation to members, not only to tenant admins (`App.tsx` nav, `routes.tsx` embedded block,
  `Settings.tsx` `TENANT_ADMIN_NAV_ITEM_HREFS` for embedded API keys and signing keys).
- `useHasEmbeddedScope(scope, environmentId?)` and an embedded equivalent of `useAccessibleEnvironmentIds`, with the
  same CE, tenant-admin and fail-closed rules as `useHasWorkspaceScope`.
- Every create, edit, delete, publish, enable/disable and settings control is shown only with its scope. The
  integration workflow editor opens read-only without `WORKFLOW_EDIT` in Development, reusing the 1051 read-only
  editor. The run sheet hides details without `EXECUTION_DETAILS_VIEW` (`detailsHidden` from 1051).
- An **Embedded > Settings > Members** page mirrors the workspace members page, including the per-environment roles
  dialog.

## Docs, scenarios and tests

- Docs: an embedded users and roles page (catalogue, per-environment roles, notable rules, what stays tenant admin),
  links from the automation users page.
- Test scenarios and results go into #1051, in the same format as the automation scenarios.
- Unit and integration tests for the scope providers, resolvers, membership service and every changed gate
  (`PreAuthorizeAnnotationTest`-style pins where automation has them, int tests with a real `PermissionService`).
- Playwright: an embedded counterpart of `tests/rbac` (built-in roles, per-environment roles, server gates, membership,
  read-only editor, run details), with fixtures for embedded members. Existing connected-user specs must still pass.

## Constraints

- EE only. CE has no embedded side; nothing changes there.
- Conventions: EE license header and `@version ee`; scoped `:module:check` for verification; commit messages
  `1051 <desc>` / `1051 client - <desc>` / `1051 docs - <desc>`; no explanatory inline comments.
- The automation RBAC engine (`PermissionService`, `PermissionScopeRegistry`, resolvers) is reused and extended, not
  forked.

## Builder's call

- How the registry tells automation scopes from embedded scopes (a side on `ScopeDefinition`, separate registries, or
  separate providers per side), and how a shared name such as `EXECUTION_VIEW` resolves to the right side. My
  suggestion is a side discriminator resolved from the resource being checked.
- How `ResourceOwner` represents "embedded" (a new variant rather than a sentinel workspace id).
- Whether the embedded role type is `WorkspaceRole` reused or its own enum with the same ranking.
- The SpEL built-ins (`hasEmbeddedScope`, `hasEmbeddedScopeInEnvironment`, `hasEmbeddedScopeInEveryEnvironment` or
  equivalents) and how `hasPermission(#id, '<Type>', '<SCOPE>')` picks the embedded side for a shared type such as
  `Job`, `Connection` or `McpServer`.
- Exact method-by-method gate mapping, list filtering, the GraphQL shape of the membership and scope queries, and the
  order of work, provided each step leaves the stack green.
