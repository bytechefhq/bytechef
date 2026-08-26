# Environment promotion (EE)

Promoting an API collection, MCP server, A2A server or plain project deployment from one
environment to its counterpart in another.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

Design rationale and the as-built record live in
`docs/superpowers/specs/2026-08-17-environment-promotion-design.md`; the task-by-task plan in
`docs/superpowers/plans/2026-08-17-environment-promotion.md`.

## Shape

`server/ee/libs/automation/automation-promotion` (`-api`/`-service`/`-graphql`) puts a
"Promote to environment…" action on each list row, EE-gated and hidden below two environments.
**Monolith only**: wired into `server-app`'s `build.gradle.kts` alone. `configuration-app` carries
none of the four handlers' collaborator services, so there is nothing to wire in — same posture as
orphaned-job recovery.

Lineage is a `uuid` column on the root row (`api_collection.uuid`, `mcp_server.uuid`,
`a2a_server.uuid`, `project_deployment.uuid`); `(uuid, environment)` is "the same thing in another
environment", robust to renames. Each `EnvironmentPromotionHandler` mints the target counterpart via
its own surface's normal create path (`createApiCollection` / `createMcpProject` /
`createA2aProject` / `createProjectDeployment` — **never a create branch inside the promoter**) and
then hands it to the shared `ProjectDeploymentPromoter.sync`, which reconciles
`project_deployment_workflow` rows **in place by `project_workflow.uuid`** — the same pivot
`checkProjectDeploymentWorkflows` uses for a plain version change. That in-place reconciliation is
what keeps `api_collection_endpoint` / `mcp_project_workflow` / `a2a_project_workflow` child-row FKs
valid across a promotion instead of orphaning them.

## Sync-vs-local rule

First promotion copies everything. **Every re-promotion after that syncs ONLY the exposed surface**:
pinned project version, the *set* of endpoints/tools/skills and their mapping metadata, and (API
collections) `contextPath` / `collectionVersion`. Name, description, tags, every `enabled` flag,
authentication settings, secret keys and connection bindings already present in the target are
environment-local and are never overwritten.

A created counterpart is always **disabled**, including when connections are left unresolved —
unresolved connections do not block promotion, they keep the new counterpart off until reviewed. A
created MCP or A2A counterpart mints its **own secret key** (new URL); re-promotion never touches it.
API collections have no per-collection secret — dispatch is
`(environment, contextPath, collectionVersion, path)` — so promoting one never changes a URL.

Promoting a *synthetic* deployment (the one backing an API collection / MCP / A2A server) is refused;
promote the owning resource instead. Metric: `bytechef_environment_promotion{resource, outcome}`.

## Name uniqueness differs per surface, on purpose

- `api_collection` has no denormalised `environment` column (it is read through the joined
  deployment), so `uk_api_collection_name` was dropped **without a DB replacement**. Name uniqueness
  per `(workspace, environment)` is an app-level facade check. This is a known, accepted
  check-then-insert race — no partial unique index can express a column reached through a join — so
  two concurrent creates for the same `(workspace, environment, name)` can both pass and both insert.
- `mcp_server` and `a2a_server` carry `environment` on the row and get real `(name, environment)` /
  `(uuid, environment)` constraints.
- `a2a_server` has no name constraint at all, so its handler adds no name-conflict guard, while
  `project_deployment` (also no DB name constraint) adds one anyway, because
  `fetchProjectDeploymentByName` is a real disambiguation path elsewhere (the embedded-automation
  bridge).

## Invariants that bite

- **Guards resolve ids through a separate `promotionAuthorizer` bean, never a handler's
  self-reference.** A guarded bean cannot safely call itself through its own security proxy
  mid-check.
- **`PromotionHandlerAuthorizationTest` pins the `@PreAuthorize` strings byte-exact** AND
  bytecode-inspects each handler's `promote` for a direct call to
  `PromotionConnectionScope.checkMappedConnectionsBelongToSource`. Moving that call into a superclass
  or helper fails the pin — by design. That is why `promoteProject` was extracted into
  `ServerProjectPromoter` but `promote` was not.
- **The mapping reconciliation order is not interchangeable**: delete stale mapping rows → `sync` →
  create from `workflowIdMapping` → `updateParameters`. `ProjectWorkflowMappingReconciler` owns it
  and both handlers' tests pin it with `InOrder`.
- **`ServerProjectPromoter.TargetProject` holds a `long` id, not the entity.** Both handlers used to
  hold the whole `McpProject` / `A2aProject` but every use reduced it to `getId()`; capturing the id
  is what removed the type parameter a generic superclass would have needed.

## Rulings that generalise beyond this feature

- **A UI-visibility query must never drive a delete cascade.** `getProjectDeployments` is the
  display-filtered *list* query — it hides synthetic deployments and system-named projects.
  `getAllProjectDeployments` is the cascade read. Driving `deleteWorkflow`'s sweep off the former is
  what left `project_deployment_workflow` orphans behind.
- **Enumerate a service method's callers before adding `@PreAuthorize` to it.** Three families run
  with no `SecurityContext`: the MCP serve path (authenticated by a server secret key, not a user),
  delete-cascade listeners, and agent tool callbacks on worker threads. Guard the entry point
  instead — that is the API-facade-owns-authorization convention.
- **Removing an unconsumed GraphQL query beats guarding it.** A guarded query is still reachable.
  Check for consumers first: `toolEligibleProjectVersionWorkflows` had them (the MCP/A2A add-workflow
  dialogs) and was guarded; three others had none and were removed.
- **Proxy-based method security is only guaranteed to intercept public methods.** A `@PreAuthorize`
  on a package-private controller method may silently never run. Pin the modifier in a test.
- **AI Hub catalog-tier tools are already rehydration-wrapped** by `ToolSearchAdvisorConfiguration`,
  so registering there supplies the principal for `@PreAuthorize` facades for free — no
  `RehydrateContextToolCallback` plumbing needed.
- **`git tag --sort=-v:refname` is wrong in this repo.** It picks `v1.1.5` over the newer `v0.31.4`.
  Use `--sort=-creatordate`, and verify the specific CHANGELOG FILE is in the tag, not just the
  module — a module can ship while the changelog creating your table does not.

## Agent surface

Four catalog-tier tools — `promoteApiCollection`, `promoteMcpServer`, `promoteA2aServer`,
`promoteProjectDeployment` — all from one `PromoteToEnvironmentToolCallback` constructed once per
`PromotionResourceType`. Concrete verbs rather than one generic tool taking a type argument: ids are
per-table, so a generic tool pairing a correct `sourceId` with the wrong `resourceType` would usually
find a real row of the other type and promote something the user never named.

Registered through `ObjectProvider<EnvironmentPromotionFacade>.ifAvailable`, which is load-bearing:
`ai-hub-service` also ships in `ai-copilot-app`, which does not carry `automation-promotion-service`,
so the bean is genuinely absent there and the tools go unregistered rather than failing startup.

The tool previews internally purely to enrich its result, then promotes in one shot, and states
explicitly that a created counterpart is DISABLED and lists `unresolvedConnectionIds` — the prompt
requires the model to relay both.

`cloneApiCollection` was retired with these: it copied five fields, deliberately set endpoints to
`List.of()`, and called the same facade method `createApiCollection` calls. `cloneMcpProject` stays —
its axis is the MCP server, not the environment.

## Orphan cleanup for databases upgraded from v0.31.4

Before the workflow-delete cascade fix, deleting a workflow of a project that also backed an MCP
server or API collection left the synthetic deployment's `project_deployment_workflow` row alive
along with the child row pointing at it. `project_deployment_workflow.workflow_id` has no foreign
key, so it was silent. Only the workflow-delete path produced orphans — the sibling project-delete
bug failed loudly on `fk_project_deployment_project`.

`scripts/dev/diagnose-synthetic-deployment-orphans.sql` (read-only) and
`scripts/dev/cleanup-synthetic-deployment-orphans.sql` (transaction-wrapped, ends in `ROLLBACK`)
handle it. Deliberately **not** a Liquibase changeset: a changeset runs unattended on every upgrade,
and these rows are user-visible REST endpoints and MCP tools selected by a join rather than a marker
column.
