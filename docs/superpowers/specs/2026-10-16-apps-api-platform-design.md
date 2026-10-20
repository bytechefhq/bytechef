# Apps — API Platform as the Default Backend Path — Design

## Summary

Apps (`2026-10-06-apps-governed-ai-built-apps-design.md`, `2026-10-06-apps-workflows-and-sdk-design.md`) let an app's
code reach ByteChef four ways: a direct component action, a tool, the raw connection proxy and a bound Call-workflow
workflow. This spec makes **API Platform endpoints the default** way an app's code calls ByteChef, keeps the other
paths for concrete needs, and teaches the app build loop (skill, template `AGENTS.md`, `.agents/apps.md`, management
MCP tools) to build the workflow type the app needs, including scheduled ones.

The work lands on `0_732` folded into the existing Apps commit group (`f1c55a9f62f` … `7cb1546c47c`), so the group
reads as one feature. Each hunk goes into the Apps commit whose scope it belongs to (docs, schema, server, CLI, client,
SDK, regenerated sources).

*Design author's call:* the API Platform enhancements (in-place version update, public API with endpoint
connections, and CLI commands) are
general API Platform features, not Apps code, so they get their own commits on `0_732` placed just before the Apps
group (the Apps commits then use them). Any schema change they need goes in its own changelog-only commit.

## Why

A component action called from app code hard-wires business logic into the app: changing it means changing and
redeploying the app, an admin sees only "action X ran", and the logic cannot be reused by anything else. An API
Platform endpoint puts that logic in a ByteChef workflow behind a versioned, REST-shaped contract the admin can see,
change, disable and reuse. The app's code stays a thin client of endpoints it was given.

## Decisions (made with the user, 2026-10-16)

1. **Order of preference for the app's code.**
   1. **API Platform endpoint** — the default, for logic that answers within a request.
   2. **Call-workflow binding** — for long-running runs, approval-waiting runs, and *background* runs (one-off,
      started on demand, the app does not wait: `waitSeconds: 0`, immediate 202 and a `runId` to poll). Bindings stay
      as they are; only the guidance about when to use them changes.
   3. **Direct action or tool** — when there is a concrete need: a single operation where a workflow would be
      overkill, or tools for an app that runs its own LLM locally instead of through ByteChef.
   4. **Proxy** — last resort, for raw API calls no action covers.

   The build loop takes the first option that fits and states why when it goes past option 1.
2. **Whole collections are bound to an app.** An admin (or the CLI in DEVELOPMENT) binds an API collection to an app;
   the app's key may then call every endpoint of that collection. No per-endpoint allow-list.
3. **Scheduled vs background.** *Scheduled* = runs periodically via the schedule trigger; the app never starts it.
   *Background* = runs once, on demand, started by the app (decision 1.2).
4. **Results of scheduled work** are written by the workflow to a ByteChef **data table**; the app reads them through
   an API Platform endpoint whose workflow queries that table. Background runs return their result through run polling;
   a result that is large or must outlive run retention is also written to a data table and read the same way.
5. **Push to the app** (a scheduled workflow notifying the app's `url` that results are ready) is a later follow-up,
   not in this cut.

## Non-goals

- Removing or changing direct actions, tools, the proxy or Call-workflow bindings (their endpoints, tables, SDK calls
  and CLI commands stay).
- Run handles (202 + polling) on API Platform endpoints. Long or approval runs use Call-workflow bindings.
- Per-endpoint app permissions, rate limits or quotas on app API Platform calls.
- Changing how platform API keys authenticate on `/api/o/**`.
- Fixing the pre-existing API Platform issues listed under "Known API Platform issues", except where this design needs
  the fix.

## Anti-goals

- An app key that reaches a collection it was not bound to, or a collection in another workspace or environment.
- An app API Platform call that keeps working after the app is disabled, its key rotated, or the collection unbound.
- Guidance that makes the coding agent reach for direct actions first because they are fewer steps.

## Design — server

### Binding model

New table in `automation-app-service`'s changelog, in the existing Apps schema changeset file so the group stays one
unreleased feature (see the Liquibase rule: one changelog per unreleased feature, schema in its own commit, which here
is `8d3198e2c92`):

**`app_api_collection`**: `id`, `app_id` (FK to `app`, cascade delete), `api_collection_id`, `version`. Unique
`(app_id, api_collection_id)`. No audit columns, like `app_connection` and `app_workflow`; changes are audited through
`AppAuditPublisher` (`APP_API_COLLECTION_BOUND`, `APP_API_COLLECTION_UNBOUND`).

*Builder's call:* whether to add an FK to `api_collection` with cascade delete, or to follow `app_workflow` (no FK,
binding reported unavailable when the target is gone). Prefer the `app_workflow` rule for consistency: deleting a
collection is not blocked, the binding lists as `available: false`.

A binding is **available** when the collection exists, is in the app's workspace and environment, and its project
deployment is enabled. One definition of this check is used by binding, listing and execution (like
`AppCallableWorkflowResolver`).

### Admin surface and public API

Mirrors workflow bindings:

- `AppApiCollectionFacade` (or methods on `AppWorkflowFacade`'s sibling — builder's call): `APP_VIEW` to list,
  `APP_MANAGE` to bind and unbind. The bindable list offers only collections of projects the caller can see in the
  app's workspace and environment; binding one of an invisible project is refused.
- GraphQL (`app.graphqls`): `appApiCollections(appId)`, `appBindableApiCollections(appId)`, `bindAppApiCollection`,
  `unbindAppApiCollection`.
- Public REST (the Apps public API spec, `openapi-management.yaml`): `GET /apps/{id}/api-collections`,
  `PUT /apps/{id}/api-collections/{apiCollectionId}`, `DELETE /apps/{id}/api-collections/{apiCollectionId}`.
- Admin UI: app detail gains an **API collections** tab (bind, list with Unavailable marker, unbind), next to the
  Workflows tab.

### App-facing calls

App keys and platform API keys have the same shape (`TenantKey`) and both arrive as `Authorization: Bearer`, so
`/api/o/**` cannot tell them apart without trying both stores. **Decision (builder may revisit with reason):** the app
calls API Platform under its own base path, authenticated by the existing app-key filter
(`AppApiKeySecurityConfigurer`, `^/api/app/v[0-9]+/.+`):

```
{METHOD} /api/app/v1/api/v{collectionVersion}/{contextPath}/{endpointPath}
```

- The controller resolves the endpoint exactly as `ApiPlatformHandlerController` does today (same matching, same
  enabled checks, same `apiPlatform/newApiRequest` trigger, same sync execution and `WebhookResponse` handling), but
  only among the collections **bound to the calling app** and in the app's environment (from `AppPrincipal`, never from
  `X-ENVIRONMENT`).
- Extract the shared resolve-and-execute logic out of `ApiPlatformHandlerController` into a component both controllers
  use, so the public `/api/o/**` path and the app path cannot drift. `/api/o/**` behaviour for platform API keys is
  unchanged.
- No match among bound collections → 404 `APP_API_ENDPOINT_NOT_FOUND` (do not reveal whether an unbound collection
  would have matched). Bound but unavailable → 409 `APP_API_COLLECTION_UNAVAILABLE`.
- Each call is logged against the app the same way action and workflow calls are (tool invocation log with `app_id`).
- `GET /api/app/v1/api-collections` lists the app's bound collections with their endpoints: `contextPath`,
  `collectionVersion`, per endpoint `httpMethod`, `path`, `name`, and the request/response JSON schemas taken from the
  `newApiRequest` trigger (`request`, `response`), plus `available`.

### API Platform: endpoint connections from the API and the build loop (decided 2026-10-16)

Connections are set **per endpoint** today: each endpoint row in the API Platform UI (`ApiCollectionEndpointListItem`)
opens `ProjectDeploymentEditWorkflowDialog` on the endpoint's `ProjectDeploymentWorkflow` of the hidden
`__API_COLLECTION__` deployment, and saves connections and inputs through the existing project-deployment-workflow
update path. At run time `AbstractDispatcherPreSendProcessor.getConnectionIdMap` reads those
`ProjectDeploymentWorkflowConnection` rows. The UI and the data model need no change.

What is missing is that path outside the UI:

- The public API's endpoint create and update accept `connections` (slots: `workflowNodeName`, `key`,
  `connectionId`) and `inputs`, applied to the endpoint's `ProjectDeploymentWorkflow` through the same facade/service
  call the UI's dialog uses — not a second implementation, and with the same authorization and connection checks.
- The build-loop MCP endpoint tool takes `connections` with the same shape and auto-pick rule as `deployProject`
  (exactly one usable candidate is picked, otherwise the candidates are listed).

### API Platform: moving a collection to a new project version (decided 2026-10-16)

A general `ApiCollectionFacade` operation (facade, REST, GraphQL, UI action, MCP tool) updates a collection **in place**
to a newer published project version: the hidden deployment moves to that version, every endpoint is re-pointed to the
new version's `project_deployment_workflow` for its workflow uuid (as promotion's `reconcileEndpoints` does), connection
slots are kept where the slot still exists and reported where a new slot needs one, and endpoint enabled states carry
over. `collectionVersion` and `app_api_collection` bindings do not change, so the app's paths stay valid. An endpoint
whose workflow uuid no longer exists in the new version is reported, and the update refuses unless the caller confirms
removing it. Bumping `/v{n}` stays a separate, deliberate act for a breaking contract change.

### API Platform: public API for collections and endpoints (decided 2026-10-16)

Collections and endpoints today have only the internal UI API (`/api/automation/api-platform/internal`, session auth,
`automation-api-platform-configuration-rest/openapi.yaml`). Add a public API callable with an automation
API key, following the Apps public API (`openapi-management.yaml`, `/api/automation/v1`, `X-ENVIRONMENT`-scoped,
see `AppApiController` / `AppEnvironmentScope`):

- Collections: list (per workspace), get, create (project, project version, name, context path, collection version,
  description), update (name, description), delete, and the in-place version update above.
- Endpoints: list per collection, get, create (collection, name, HTTP method, path, workflow uuid), update, delete,
  enable/disable.
- Every operation goes through `ApiCollectionFacade`, which owns authorization (the facade rule: controllers own none);
  a collection outside the request's environment answers 404, as Apps do.
- The CLI gains `bytechef automation api-collection …` and `bytechef automation api-collection endpoint …` commands on
  this API (create/list/get/update/delete, `update-version`, endpoint enable/disable), usable in any environment like
  other non-app automation commands; the app build loop uses them in DEVELOPMENT. Regenerate the CLI client.
- Docs: the public OpenAPI page for the new API (the docs site publishes public OpenAPI only).

### Scheduled workflows in the build loop

`deployProject` (`AppLoopTools`) today enables app-callable workflows when `enabledWorkflows` is omitted. Its default
stays; the skill passes scheduled workflows in `enabledWorkflows` explicitly, so the loop never starts a schedule it did
not mean to. `deployProject` does not touch API endpoint workflows: those run on the collection's own hidden deployment
(below).

**Enabling a collection (design author's call).** `createApiCollection` leaves its hidden deployment disabled and new
endpoint rows disabled. In the build loop (DEVELOPMENT only) the MCP tools that create a collection and an endpoint
enable the hidden deployment and the endpoint's workflow, and report it; the admin UI and REST keep today's behaviour.
On the app path a bound collection whose deployment or endpoint workflow is disabled answers 409
`APP_API_COLLECTION_UNAVAILABLE` (bound but not runnable); 404 is reserved for "no endpoint of a bound collection
matches".

API collections create their own hidden project deployment (`"__API_COLLECTION__" + projectId`) in
`ApiCollectionFacadeImpl.createApiCollection`, separate from the DEVELOPMENT deployment `deployProject` manages. The
build loop must therefore create collections and endpoints through the API collection path, not through
`deployProject`. Management MCP tools needed in DEVELOPMENT (the agent-loop transport):

- `createApiCollection` / `listApiCollections` exist as AI tools (`CreateApiCollectionToolCallback`,
  `ListApiCollectionsToolCallback`) but reach the management MCP server only when Copilot is enabled, and
  `createDataTable` only Copilot agents. The build loop gets its own Copilot-independent, DEVELOPMENT-only tools beside
  `deployProject` (registered by `AppLoopMcpContributorConfiguration`), named apart from the Copilot ones to avoid a
  registry collision: `createAppApiCollection`, `createAppApiEndpoint` (method, path, workflow uuid, connections),
  `updateAppApiCollectionVersion`, `listAppApiCollections`, `createAppDataTable`.
- `buildWorkflow` must be able to build an API Platform workflow (`apiPlatform/newApiRequest` trigger with
  `request`/`response` schemas, ending in `apiPlatform/v1/responseToApiRequest`) and a scheduled workflow (schedule
  component trigger) that writes to a data table (`dataTable` component: `createRecords`, `updateRecord`, …). Whether
  that is a new flag like `appCallable` or prompt guidance is the builder's call; it must report, per saved workflow,
  what it is and what is missing, as `appCallable` does today.

### CLI

`bytechef automation app bind api-collection --api-collection-id <id>` / `unbind api-collection`, refusing any
environment but DEVELOPMENT like the other `automation app` commands. `bytechef automation app status` lists bound
collections with `available` and their endpoints.

## Design — SDK and template

- `@bytechef/app` gains `api`: `client.api.call({method, path, body?, query?, headers?})` where `path` is
  `v{n}/{contextPath}/{endpointPath}`, returning `{status, headers, body}` without throwing on workflow-chosen 4xx/5xx
  statuses (the endpoint's `response` cases are part of its contract); `client.api.list()` returns the bound
  collections. Transport errors and app-level errors (401, 404 `APP_API_ENDPOINT_NOT_FOUND`, 409) throw the SDK's
  existing error types.
- TanStack Start: a ready-made server function for `api.call`, like the existing ones for actions and workflows.
- AI SDK adapter: unchanged in this cut (tools stay the AI path).
- Regenerate `src/generated/openapi.ts`, the CLI app client and the GraphQL client (folded into `7cb1546c47c`).
- The template's example code uses `api.call` as its primary example; actions, workflows, tools and proxy examples move
  under "when you need it".

## Design — build-loop guidance

Update `claude-code-plugin/bytechef-dev/skills/bytechef-app-builder/SKILL.md`, the template's `AGENTS.md` (and
`CLAUDE.md` if it carries the loop), `.agents/apps.md`, and the Apps docs page with:

1. **The order of preference** (decision 1), as a short decision list the agent applies per need, with "say why" when
   it goes past API Platform.
2. **Picking the workflow type from the need:**

   | The app needs… | Build | App uses |
   |---|---|---|
   | Logic answering within a request | `apiPlatform/newApiRequest` workflow → collection endpoint, bind collection | `api.call` |
   | A long or approval-waiting run | Call-workflow workflow, bind it | `workflows.execute` + poll |
   | A one-off job it starts and doesn't wait for | Call-workflow workflow, bind it | `workflows.execute` with `waitSeconds: 0` + poll |
   | Periodic work | Schedule-triggered workflow writing to a data table + an API endpoint reading it | `api.call` on the read endpoint |
   | One operation, nothing else | — | bound action |
   | Its own local LLM calling tools | — | bound tools |
   | A raw API call no action covers | — | proxy |

3. **The loop steps** for API Platform (build workflow → publish → create/extend collection and endpoint → bind
   collection) and for scheduled work (create data table → build scheduled workflow → build read endpoint → publish →
   deploy with the scheduled workflow explicitly enabled → bind the collection). Scheduled workflows are enabled only
   in DEVELOPMENT by the loop; staging and production stay a person's decision, as for everything else.

## Known API Platform issues (context, not scope)

Found while mapping API Platform on `0_732`:

- `responseToApiRequest` swapped 400/500 — fixed on master in bytechefhq/bytechef#6331; `0_732` picks it up on its next
  master rebase.
- An unmatched path throws `IllegalArgumentException` instead of a 404; endpoint lookup scans every collection of the
  environment on each request; the trigger's `timeout` property is never read; trigger flags are hard-coded. The app
  path must return a proper 404 (see App-facing calls); the rest stay as they are.

## Testing

- Facade and controller tests follow the existing Apps patterns: authorization tests for the new facade methods,
  WebMvc/GraphQL controller IntTests, and an IntTest for the app API path covering: bound collection call succeeds;
  unbound collection → 404; other workspace or environment → 404; disabled app / rotated key → 401; unavailable
  binding → 409; workflow-chosen status and headers pass through; call is logged with the app id.
- `/api/o/**` regression: existing API Platform handler tests still pass after the extraction.
- SDK unit tests with the existing `mockFetch`; template smoke check in CI as today.
- `AppLoopTools` tests for the deploy/enable change and any new MCP tools.

## Left to the builder

Facade naming, the FK question on `app_api_collection`, whether `buildWorkflow` gets a flag or guidance, the exact
SDK method names, and how the fold is split across the Apps commits — each within the constraints above.
