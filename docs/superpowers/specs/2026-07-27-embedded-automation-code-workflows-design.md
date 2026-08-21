# Automation code workflows inside embedded (bridge + shared deploy)

**Status:** Designed, not implemented.

Lets a vendor author an automation-style **code workflow** (the `ProjectHandler` / `workflow-api`
contract) and serve it to embedded **connected users** through the existing
automation-inside-embedded bridge — deployed once, referenced per user, invoked through the
existing public embedded endpoints.

## Context (verified on branch 0_732)

- SDK layering is already right: shared `sdks/backend/java/workflow-api` (the whole workflow DSL),
  with thin `automation/project-api` (`ProjectHandler`) and `embedded/integration-api`
  (`IntegrationHandler`) on top. Neither depends on the other. **This feature changes no SDK.**
- The bridge exists for visual workflows: `AutomationWorkflowProjectFacade` (admin catalog, real
  automation `Project` rows behind the `__EMBEDDED_AUTOMATION__` marker) and
  `ConnectedUserProjectFacade` / `ConnectedUserProjectWorkflow` (per-user tier, `__EMBEDDED__`).
  The automation `Project` is deliberately hidden behind these embedded entities; this spec keeps
  that rule everywhere.
- Code workflows exist for embedded **integrations** (`/api/embedded/internal/integrations/deploy`)
  and for automation projects — but the bridge only ever calls
  `projectWorkflowFacade.addWorkflow(json)`; it has never been wired to the code-workflow pipeline.
- The public invocation endpoints `POST /app-events` (async) and `POST /workflows/{workflowUuid}`
  (sync), in `embedded-webhook-public-rest`, today resolve **integration workflows only**. Bridge
  workflows are not programmatically invocable at all.

## Decisions (each settled in brainstorming)

1. **Build as automation, relate via the embedded endpoint.** The artifact is a plain automation
   code workflow (`ProjectHandler`). Deploying it through the embedded endpoint is what creates the
   embedded relation; deploying the same bytes through the automation endpoint creates a plain
   project with no embedded involvement. Nothing in the artifact says "embedded".
2. **Deploy once, reference per user.** One admin deploy → one `CodeWorkflowContainer` → catalog
   project. Connected users hold references, never copies: code is not per-user editable, so copies
   would only duplicate containers. Redeploy upgrades every user at once (per-user version pinning
   is out of scope). Per-user bespoke deploys are out of scope.
3. **Attachment point is the embedded entity** (`ConnectedUserProjectWorkflow` /
   `AutomationWorkflowProject`), never the `Project` directly.
4. **Invocation reuses the two existing public endpoints** — no new invocation surface.

## Deploy path

- `POST /api/embedded/internal/automation/projects/deploy` — internal admin surface, beside
  `/integrations/deploy`; multipart `projectFile`; `ADMIN` authority; the same Java hardening
  posture (`BYTECHEF_WORKFLOW_CODE_WORKFLOW_JAVA_ENABLED`, Espresso loader flag).
- Flow: `ProjectHandlerLoader` loads the `ProjectDefinition` → resolve-or-create the catalog
  project through `AutomationWorkflowProjectFacade`'s marker convention (default workspace) →
  `CodeWorkflowContainerFacade.create(..., PlatformType.AUTOMATION)` → add workflows →
  `publishProject`.
- `PlatformType.AUTOMATION` because the workflows execute through automation project machinery
  behind the bridge; `EMBEDDED` containers belong to the integration pipeline, which this is not.
- Idempotent on project name: redeploy updates, never duplicates. Redeploy is the shared upgrade
  point for all referencing users.

## Per-user reference

`ConnectedUserProjectWorkflow` gains a **reference mode**: a nullable discriminator column pointing
at the catalog `ProjectWorkflow` (uuid-stable across versions), instead of holding a copied
definition. Per the workspace-collapse convention: a column, not a relation table.

- Carries what is genuinely per-user: enable state and connection configuration (reusing the
  existing `updateWorkflowConfigurationConnection` shape).
- **No per-user editing**: reference rows have no definition; the update/edit APIs reject them.
- **Dangling on vendor removal**: if a redeploy removes a workflow a user referenced, the reference
  flips to a disabled `dangling` state — invisible to invocation, listed with a reason, never
  silently deleted. The user's connection config survives a vendor mistake.
- Copy-mode rows (visual bridge) are untouched.

## Invocation — extend the two existing endpoints

Today both endpoints resolve integration workflows only. Each gains an **automation-bridge branch**;
integration behavior stays byte-for-byte unchanged, same URLs, auth, and response contracts.

- **Sync** `POST /workflows/{workflowUuid}`: uuid not an integration workflow → try catalog
  `ProjectWorkflow` uuid → caller's enabled `ConnectedUserProjectWorkflow` reference → run via
  `PrincipalJobFacade` with that user's connections. No enabled reference → same error as an
  unknown workflow (no existence leak).
- **Async** `POST /app-events`: fan-out gains a second source — enabled connected-user references
  whose workflow declares the app-event trigger, fired per user with that user's connections.
- **Side benefit, in scope**: the branch resolves visual bridge copies too, making the whole
  bridge programmatically invocable — previously impossible.
- DSL implication (verified against `RequestTriggerApiController` / `AbstractWebhookTriggerController`):
  sync callability requires a **`request` trigger** (`findRequestTriggerName` matches
  `WorkflowNodeType.name() == "request"`; `workflow/newWorkflowCall` is a different gate and does
  not count) **plus an action that writes `MetadataConstants.WEBHOOK_RESPONSE`** — without it the
  caller gets an acknowledgment, not a payload. Async requires the app-event trigger. These are the
  exact same rules integration workflows follow today; the bridge branch inherits them by reusing
  the same controller machinery, adding no new trigger types and no new rules.

## Provisioning — implicit on first call, explicit where wanted

A backend may invoke a catalog template for a connected user who never provisioned it; the call
itself provisions. Applies to **both kinds**:

- **Code workflow**: no reference exists → the invocation creates the reference implicitly
  (enabled), auto-wiring connections from the user's existing connections by component — the same
  mechanism the visual copy flow uses today — then runs.
- **Visual template**: no copy exists → the invocation performs the same copy the explicit
  `POST /automation/workflow-templates/{uuid}/copy` endpoint performs (connection auto-wiring
  included), then runs the copy. Subsequent calls hit the copy's own uuid as usual; calling the
  template uuid again resolves to the existing copy rather than duplicating it.
- **Unresolvable required connection** → HTTP 409 naming the missing connection; the
  reference/copy is left in place but disabled, so the backend can create the connection and
  retry without redoing provisioning.
- **Explicit provisioning stays**: the existing visual copy endpoint is untouched, and a new
  public endpoint provisions a code reference ahead of time for backends that want to pre-wire
  connections before first call.

## Final decisions (closing brainstorm round)

- **Environments**: one deploy serves all environments. The catalog project and container are
  environment-agnostic; a user's reference binds it to their environment and connections resolve
  per environment. One upgrade point.
- **Upgrade flow**: deploy = publish, instantly for all users — matching the existing automation
  code-workflow flow. Vendors stage risk in their own staging environment.
- **De-provisioning**: full public parity — disable and delete for code references, matching the
  visual bridge's existing enable/delete operations.
- **Discovery**: code catalog projects appear in the existing `GET /automation/projects` listing
  beside visual templates, with a `kind` field so clients distinguish reference-provisioning from
  copy-provisioning.

## Copilot and the Claude Code plugin

- Admin authoring reuses the existing project-bound `code_workflow_ask` / `code_workflow_build`
  pair — the same reuse precedent as the visual bridge driving `workflow_editor_build`. No new
  agent family. Connected users never edit code, so the per-user surface needs nothing.
- The `bytechef-dev` plugin's `bytechef-code-workflow` skill gains a bridge section: same
  `ProjectHandler` artifact, embedded deploy endpoint, and the deploy-once/reference-per-user
  model. (The skill currently documents only the two standalone surfaces.)

## Out of scope

- Admin catalog authoring of code templates users copy (copy semantics for containers undecided).
- Per-user version pinning; per-user bespoke code deploys.
- New SDK modules or changes to `workflow-api` / `project-api` / `integration-api`.
- Embedded AI-Hub anything (none exists).

## Testing

- Deploy: artifact through the embedded endpoint creates a marker-hidden catalog project + one
  container; redeploy updates in place; the same artifact via the automation endpoint creates no
  embedded relation (the two doors stay distinct).
- Reference: enable/config round-trip; edit APIs reject reference rows; vendor-removal flips to
  dangling without deleting; copy-mode rows unaffected.
- Invocation: sync uuid resolution picks the caller's reference and connections (two users, same
  uuid, different connections — each run sees its own); disabled/dangling reference is invisible;
  integration-workflow resolution is regression-pinned unchanged. App-event fan-out fires per
  enabled reference.
- Security: connected-user auth cannot reach another user's reference; the internal deploy
  endpoint rejects connected-user tokens (as `/integrations/deploy` already does).
