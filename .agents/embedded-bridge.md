# Embedded automation code workflow bridge

Catalog projects, reference vs copy mode, and the sync/async dispatch fallbacks.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### Embedded automation code workflow bridge

`POST /api/embedded/internal/automation/projects/deploy` (`ADMIN`-only via `@PreAuthorize` on
`AutomationWorkflowProjectCodeWorkflowFacadeImpl#save`, not the controller — same posture as
`/integrations/deploy`) deploys a plain automation code workflow (`ProjectHandler`/`project-api`)
behind `AutomationWorkflowProjectFacade`'s `__EMBEDDED_AUTOMATION__` marker via
`AutomationWorkflowProjectCodeWorkflowFacadeImpl` -- the SAME artifact deployed through the plain
`/api/automation/v1/projects/deploy` endpoint creates an unmarked, unrelated project; the marker is
what makes it embedded-servable. `ConnectedUserProjectWorkflow` gained a nullable
`catalog_workflow_uuid` discriminator (XOR with `project_workflow_id`, never both): non-null means
the row is a reference to a shared catalog workflow (never a per-user copy, never editable) instead
of a copy-mode row. A catalog project's client-facing `kind` (`COPY`/`REFERENCE`,
`AutomationWorkflowProjectMapper#mapKind`) mirrors that split at the project level. Per-user
connection wiring for a reference lives in a new `connected_user_project_workflow_connection` table
(`ConnectedUserProjectWorkflowConnection`), NOT `WorkflowTestConfiguration` (that table is keyed by
`workflowId` alone, and a shared catalog workflow has exactly one `workflowId` across every
referencing user -- reusing it would leak one user's connection into another's run;
`ConnectedUserWorkflowConnectionResolver` is a deliberately separate node-scanning class rather than
a refactor of that path). Each reference gets its own `ProjectDeployment` scoped to (catalog project,
external user id, **environment** -- the name is `__EMBEDDED__<externalUserId>__<ENVIRONMENT>`, since
one external user can be connected in more than one environment), looked up by name via
`ProjectDeploymentService.fetchProjectDeploymentByName` (new; the existing
`fetchProjectDeployment(projectId, environment)` assumes one deployment per project+environment,
which only holds because copy-mode gives each user their own private project).
`RequestTriggerApiController#executeWorkflow` (sync `POST /workflows/{workflowUuid}`) and
`AppEventTriggerApiController#executeWorkflows` (async `POST /app-events`) both gained an
automation-bridge fallback branch that only runs once the existing integration-workflow lookup comes
back empty (regression-pinned unchanged); dispatch reuses `AbstractWebhookTriggerController
#doProcessTrigger` unmodified with `PlatformType.AUTOMATION` and the reference's (or copy's)
`ProjectDeploymentId` in place of an `IntegrationInstance` id. Both branches now resolve every shape
the bridge can produce, sharing copy-mode resolution through a package-private
`ConnectedUserCopyModeWorkflowResolver` (`embedded-webhook-public-rest`) instead of forking it: (1) a
connected user's own copy uuid dispatches directly; (2) a catalog uuid whose project is a code
catalog (`kind = REFERENCE`) goes through the pre-existing `getOrCreateReference` path; (3) a catalog
uuid whose project is a visual catalog (`kind = COPY`, `AutomationWorkflowProjectDTO
#codeWorkflowProject() == false`) is resolved via implicit copy-then-run on the SYNC endpoint only --
no existing copy provisions one through `ConnectedUserProjectFacade#copyWorkflowTemplate` (the same
copy the explicit `POST /automation/workflow-templates/{uuid}/copy` endpoint performs) and dispatches
it, an existing copy is reused. Dedup for (3) is a new nullable `copied_from_workflow_uuid` column on
`connected_user_project_workflow` (partial unique index alongside it, mirroring
`uk_cupw_connected_user_project_id_catalog_workflow_uuid`), set only when `copyWorkflowTemplate`
provisions the row -- the explicit copy endpoint's own contract is unchanged, it still always creates
a new copy. The async fan-out iterates every `ConnectedUserProjectWorkflow` row for the connected
user and dispatches both reference-mode and copy-mode rows; it has no implicit-provisioning case
(nothing to iterate before a row exists), so shape (3) is sync-only by construction. A redeploy that
drops a workflow flips existing
references to a disabled `dangling` state (`ConnectedUserCodeWorkflowReferenceFacade
#markDanglingReferences`, comparing one catalog project's previous-vs-current published uuid sets)
instead of deleting them; nothing ever clears `dangling` back to false, and since uuid carry-forward
(`AutomationWorkflowProjectCodeWorkflowFacadeImpl#fetchPreviousWorkflowUuidsByName`) only looks one
deploy back, restoring a same-named workflow after an intervening deploy that dropped it mints a
**new** uuid -- a dangling reference never self-heals; recovery is de-provision the dangling row,
then provision fresh against the new uuid. `getOrCreateReference` is NOT self-healing on repeat calls
either: once a disabled row exists (missing-connection case, `MissingConnectionException` -> HTTP
409 `{"missingConnectionComponentName": ...}`), later calls -- invocation or the explicit
`POST .../automation/workflow-templates/{workflowUuid}/provision` -- find the existing row and
return it unchanged rather than re-resolving; only de-provision (`DELETE` on the same path) + a
fresh provision reruns connection auto-wiring. That method's `@Transactional(noRollbackFor =
MissingConnectionException.class)` is required for the "still create the row, just disabled"
contract to hold at all -- without it Spring's default rollback rule would erase the row the method's
own Javadoc promises to keep. There is a SECOND, unrelated `noRollbackFor` site in this feature area:
`ProjectCodeWorkflowServiceImpl#getProjectCodeWorkflow` is
`@Transactional(noRollbackFor = IllegalArgumentException.class)`, needed because
`AutomationWorkflowProjectCodeWorkflowFacadeImpl#fetchPreviousWorkflowUuidsByName` catches that
exception as normal control flow for "no previous deploy yet" and would otherwise poison the
caller's participating transaction on every project's first deploy. Distributed webhook-app invocation
now works: `RemoteAutomationWorkflowProjectFacadeClient#getPublishedProjects`,
`RemoteConnectedUserProjectFacadeClient#copyWorkflowTemplate`, and
`RemoteConnectedUserCodeWorkflowReferenceFacadeClient#getOrCreateReference`/`getConnectedUserWorkflows`
(all in `embedded-configuration-remote-client`) make real REST calls to configuration-app's
`/remote/*-facade` controllers, covering every method the sync/async bridge dispatch paths need. The
remaining methods on those three remote clients (project/workflow CRUD, `enableReference`,
`deleteReference`, `markDanglingReferences`, etc. -- admin-console and per-user-mutation operations,
not invocation) still throw `UnsupportedOperationException`; both `RequestTriggerApiController` and
`AppEventTriggerApiController` catch that and degrade to the same `404`/empty-list an absent bridge
would produce (WARN-logged once via a per-instance `AtomicBoolean`), so an unimplemented remote method
never surfaces as a 500. Spec:
`docs/superpowers/specs/2026-07-27-embedded-automation-code-workflows-design.md`.
