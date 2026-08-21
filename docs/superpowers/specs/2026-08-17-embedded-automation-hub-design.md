# Embedded Automation Hub

- **Date**: 2026-08-17
- **Status**: Approved
- **Ticket**: 732 (embedded)
- **Modeled on**: Appmixer's Automation Hub (https://www.appmixer.com/features,
  https://docs.appmixer.com/getting-started/embed-your-automation-hub)

## Context

The embedded frontend SDK (`sdks/frontend/embedded/library`) exports two things: `useConnectDialog`
(a native React dialog that activates *Integrations*) and `EmbeddedWorkflowBuilder` (an iframe that
loads `/embedded/builder/:workflowUuid`, served by the dedicated Vite entry
`client/workflow-builder.html` → `src/ee/workflow-builder.tsx`). The builder authenticates through an
`EMBED_READY` → `EMBED_INIT` postMessage handshake: the parent posts `{jwtToken, environment,
includeComponents, sharedConnectionIds, connectionDialogAllowed}`, the iframe stores the JWT in
`sessionStorage`, and `useFetchInterceptor` adds `Authorization: Bearer` + `X-ENVIRONMENT` to every
`/internal/` and `/graphql` request.

The embedded **automation bridge** (see CLAUDE.md, "Embedded automation code workflow bridge") already
gives a SaaS vendor a catalog of published workflow templates (`__EMBEDDED_AUTOMATION__` catalog
projects, `kind = COPY | REFERENCE`), per-connected-user copies and references, provisioning with
connection auto-wiring, and enable/disable — all exposed on the **public frontend API**
(`/api/embedded/v1`, JWT-only routes with no `{externalUserId}` in the path: `GET /automation/projects`,
`GET|POST /automation/workflows`, `POST /automation/workflow-templates/{uuid}/copy`,
`PUT|DELETE /automation/workflows/{uuid}/enable`, `GET /components/{name}/connections`, …). The catalog
payload already lists the `components[]` each template uses.

What is missing is the *end-user portal* Appmixer calls the Automation Hub: one embeddable surface where
a connected user browses templates, activates them through a wizard, sees and manages their automations,
manages their connections, and opens the builder to customize a copy. Today a vendor must build all of
that on top of the SDK's raw API and wire `EmbeddedWorkflowBuilder` themselves.

## Decisions

Recorded from the brainstorm; each has a rationale in the relevant section.

| # | Decision |
|---|---|
| D1 | Sections in v1: **Templates**, **My Automations**, **Connections**, **New automation** (blank workflow). **No Logs tab.** |
| D2 | Templates are backed by the **automation bridge only** (catalog projects). Integrations (`ConnectDialog` model) stay out of the hub. |
| D3 | The builder opens **inside the hub iframe** as an internal route; the vendor embeds one component. `EmbeddedWorkflowBuilder` remains for builder-only vendors. |
| D4 | Configurability in v1: **tab visibility + basic theming** (mode, primary color, font family, radius). |
| D5 | Connections tab: **list + delete + reconnect**. No standalone "create connection"; connections are created in context (wizard, builder). |
| D6 | Template activation is a **guided 3-step wizard** (connect accounts → configure → activate). |
| D7 | Architecture: **dedicated Vite entry** for the hub (Approach A) — not an extension of the builder entry, not a native JS widget. |
| D8 | Hub views call the **public frontend API** (`/api/embedded/v1`, principal-scoped); the builder route keeps its internal API. |

## Architecture

```
host page (vendor SaaS)
└── <AutomationHub jwtToken=… tabs=… theme=…/>          sdks/frontend/embedded/library
    └── <iframe src="{baseUrl}/embedded/hub">
        └── automation-hub.html → src/ee/automation-hub.tsx  (new Vite entry)
            └── Router
                ├── /embedded/hub               AutomationHubLayout (header tabs)
                │   ├── (index) templates       TemplatesView   → ActivationWizard
                │   ├── automations             AutomationsView
                │   └── connections             ConnectionsView
                └── /embedded/hub/builder/:uuid WorkflowBuilder (existing page, unchanged)
```

Auth: same handshake as the builder. The hub entry and the builder entry share one extracted hook,
`useEmbedHandshake`, so the parent-origin allow-list and the `sessionStorage` contract cannot drift.

### Why a dedicated entry (D7)

- Precedent: `workflow-builder.html` exists precisely so the embedded bundle graph stays out of the
  main SPA and the builder gets its own first-paint boot. The hub gets the same treatment; Vite shares
  chunks between the two entries, so common code is not duplicated on the wire.
- Extending the builder entry (rejected) would rename nothing but change everything: the builder's
  optimized boot would carry hub code paths, and `/embedded/builder/*` would become a misleading name
  for a two-app entry.
- A native JS widget (rejected) is Appmixer's shape, but the builder cannot leave its iframe and the
  user explicitly asked for iframe embedding; iframe is also the stronger CSS/auth isolation boundary.

### Why the public frontend API for hub views (D8)

- The `/api/embedded/v1` frontend routes resolve the connected user from the JWT principal — no
  `connectedUserId` in any URL, so no id to get wrong. The builder's internal calls carry
  `connectedUserId` in the path because it has a workflow row to read it from; the hub's catalog and
  connections views have no such row before activation.
- Every gap the hub fills (frontend provision, connections list/create/delete/reauthorize) is a gap
  SDK consumers had too, so filling it in the public API pays twice.
- The public API is IntTest-covered under a stable versioned contract.

## Section 1 — Entry, routing, server wiring

**Client**

- `client/automation-hub.html` (title "Automation Hub - ByteChef") loading `/src/ee/automation-hub.tsx`.
- `client/vite.config.mts`: add `automationHub: resolve(import.meta.dirname, 'automation-hub.html')`
  to `rollupOptions.input`.
- `src/ee/automation-hub.tsx`: boots like `workflow-builder.tsx` — `I18n`, `ThemeProvider`,
  `QueryClientProvider`, `TooltipProvider`, `createBrowserRouter`, non-awaited
  `applicationInfoStore.getState().getApplicationInfo()`. Root element `EmbeddedAutomationHubApp`
  (renders `useFetchInterceptor()`, `<Outlet/>`, `<Toaster/>`), routes as in the diagram above.
- `src/ee/pages/embedded/automation-hub/` holds the hub: `AutomationHubLayout.tsx`, `views/`
  (`TemplatesView`, `AutomationsView`, `ConnectionsView`, `HubBuilderView` — a thin wrapper that
  renders `WorkflowBuilder` under a slim top bar with back navigation and the automation label),
  `wizard/`, `stores/useAutomationHubStore.ts` (Zustand: `initialized`, `tabs`, `theme`,
  `includeComponents`, `sharedConnectionIds`, `connectionDialogAllowed`), `queries/` + `mutations/`.
- `src/ee/pages/embedded/shared/useEmbedHandshake.ts`: **pure extraction** of the listener /
  `EMBED_READY` broadcast / `sessionStorage` writes from `useWorkflowBuilder`. Signature
  `useEmbedHandshake(onInit: (params: EmbedInitParams) => void)`. `useWorkflowBuilder` switches to it
  with no behavior change; the hub's `onInit` writes the store.
- `src/ee/pages/embedded/workflow-builder/config/useFetchInterceptor.ts`: also inject the bearer and
  `X-ENVIRONMENT` for URLs containing `/api/embedded/v1/`.
- Generated public client: a `typescript-fetch` generator task on
  `embedded-configuration-public-rest` writing to `client/src/ee/shared/middleware/embedded/public/`
  (same task shape as `embedded-configuration-rest-impl`'s). Only the frontend (JWT-only)
  operations are used by the hub.

**Server**

- `SpaWebFilter`: `if (path.startsWith("/embedded/hub") && !path.contains("."))` → forward to
  `/automation-hub.html`, immediately after the existing `/embedded/builder/` branch.

## Section 2 — SDK component

`sdks/frontend/embedded/library/src/components/automation-hub/AutomationHub.tsx`, exported from
`main.ts` beside `EmbeddedWorkflowBuilder`.

```ts
export interface AutomationHubTabsConfig {
    templates?: boolean;      // default true
    automations?: boolean;    // default true
    connections?: boolean;    // default true
    newWorkflow?: boolean;    // default true — "New automation" button on My Automations
}

export interface AutomationHubTheme {
    mode?: 'light' | 'dark';  // default 'light'
    primaryColor?: string;    // any CSS color
    fontFamily?: string;      // CSS font-family value; must be loadable inside the iframe
    borderRadius?: string;    // CSS length, e.g. '0.5rem'
}

interface AutomationHubProps {
    baseUrl?: string;                       // default 'https://app.bytechef.io'
    jwtToken: string;
    environment?: 'DEVELOPMENT' | 'STAGING' | 'PRODUCTION'; // default 'PRODUCTION'
    tabs?: AutomationHubTabsConfig;
    theme?: AutomationHubTheme;
    connectionDialogAllowed?: boolean;      // default true (builder passthrough)
    includeComponents?: string[];           // builder passthrough
    sharedConnectionIds?: number[];         // builder passthrough
    className?: string;                     // host controls sizing; no hardcoded layout classes
}
```

- Renders `<div className={className}><iframe src={`${baseUrl}/embedded/hub`} …/></div>` with
  `width/height 100%`, `border: none`, `title="Automation Hub"`.
- Handshake identical to `EmbeddedWorkflowBuilder`: on `EMBED_READY` from `new URL(baseUrl).origin`,
  post `{type: 'EMBED_INIT', params}` where `params` = all props except `baseUrl` and `className`,
  read from a `propsRef` so late prop changes reach the next handshake.
- Unlike the builder component, no `absolute inset-0 lg:pl-72` — that class encodes the test-app's
  sidebar layout and must not be repeated. `EmbeddedWorkflowBuilder` is left as-is (out of scope).
- Test-app: `sdks/frontend/embedded/test-apps` gains a `/hub` page rendering `<AutomationHub>`.
- Sample app: `bytechef-samples/bytechef-embedded-sample-app/front-end` (separate repo, SDK via yalc) gains an
  **Automation Hub** sidebar tab at `/automation-hub` rendering `<AutomationHub>` with the app's `getToken()`
  JWT — the showcase for vendors, alongside the existing Automations/Integrations pages.

## Section 3 — Hub views

All views read/write through query and mutation hooks over the generated public client. Query keys live
in one `AutomationHubKeys` object; mutations invalidate the affected keys.

**Layout.** Header with the product-neutral title "Automations", tabs Templates / My Automations /
Connections filtered by `tabs`. If only one tab is enabled the tab strip is hidden. Deep links
(`/embedded/hub/automations`) work on reload because the SPA filter forwards every `/embedded/hub*`
path.

**Templates** — `GET /automation/projects`. Cards grouped by catalog project (project = category;
project `description` shown as the group subtitle), search over label/description, each card shows
component icons (`template.components[].icon`) and a "Use template" action → activation wizard.
Empty state: "No templates published yet."

**My Automations** — `GET /automation/workflows` (schema extended, see Section 5). Rows: label,
component icons, enabled `Switch` (`PUT /automation/workflows/{uuid}/enable` on, `DELETE …/enable`
off), status badge (Enabled / Disabled / **Needs attention** when `dangling`), row menu:
- **Open in builder** — COPY only → `/embedded/hub/builder/{uuid}`.
- **Delete** — confirm dialog; COPY → `DELETE /automation/workflows/{uuid}`, REFERENCE →
  `DELETE /automation/workflow-templates/{catalogWorkflowUuid}/provision`.
Header button **New automation** (when `tabs.newWorkflow`) → `POST /automation/workflows` with a
minimal blank definition (`{"label":"New automation","triggers":[],"tasks":[]}`) → navigate to the
builder route. Empty state links to Templates when that tab is enabled.

**Connections** — `GET /connections` (new). Rows: name, component icon + title, created date. Row
menu: **Reconnect** (opens the connection dialog for the same component/authorization type; on submit
→ `POST /connections/{id}/reauthorize`), **Delete** (confirm; a `409` "in use by an enabled
automation" is shown inline with the automation labels the server returns).

**Builder** — `HubBuilderView` renders the existing `WorkflowBuilder` for `:workflowUuid`. Back
navigation invalidates `AutomationHubKeys.automations`. `WorkflowBuilder` reads
`includeComponents`/`sharedConnectionIds`/`connectionDialogAllowed` from the hub store instead of its
own handshake state when mounted under the hub — implemented by having `useWorkflowBuilder` accept
those from an optional context provided by the hub, defaulting to its own handshake otherwise.

## Section 4 — Activation wizard

`wizard/ActivationWizard.tsx` (a `Dialog` with a stepper) driven by a reducer in
`wizard/activationReducer.ts` so transitions are unit-testable without rendering. Branch on the
template's project `kind`.

**State**: `{step: 'connect' | 'configure' | 'activate' | 'done', kind, template,
selections: Record<componentName, connectionId | undefined>, highlightedComponent?: string,
workflowUuid?: string, inputs: Record<string, unknown>, error?: string}`.

1. **Connect accounts.** One row per distinct component in `template.components` that has a
   connection definition (looked up via the existing component definition query the builder already
   uses; components without one are skipped, and if none remain the step is skipped). Each row: a
   select of the user's connections for that component (`GET /components/{name}/connections`) and a
   **Connect** button that opens the platform `ConnectionDialog` — the same component the builder
   parameterizes with `useCreateConnectionMutation` / `useGetConnectionsQuery` props — backed here by
   `POST /components/{name}/connections` and `GET /components/{name}/connections`. OAuth2 flows run
   exactly as they do in the builder. **Next** enables when every row has a selection.
2. **Configure.**
   - COPY: `POST /automation/workflow-templates/{uuid}/copy` → copy `workflowUuid`; then, for each
     workflow node whose component matches a selection, `PUT
     /automation/workflows/{uuid}/workflow-nodes/{node}/connection/{key}` with the selected id
     (nodes and keys come from `GET /automation/workflows/{uuid}`, whose definition lists task
     connections). The step then shows the wired connections and the template description; there is
     **no inputs form in v1** — the public frontend API has no endpoint that persists per-user
     workflow input values, so a form would have nowhere to write. Inputs are configured in the
     builder after activation.
   - REFERENCE: `POST /automation/workflow-templates/{uuid}/provision`. A `409 MissingConnectionError`
     sets `highlightedComponent` and returns the reducer to step 1 with that row marked — the reference
     path auto-wires by component match, so having a connection for that component is what makes the
     retry succeed. Because a 409 still leaves a *disabled* reference row behind and
     `getOrCreateReference` returns that row unchanged on later calls (CLAUDE.md), the wizard issues
     `DELETE …/provision` before re-running step 2. There is no inputs form for references.
3. **Activate.** Summary (template label, connections chosen) + **Activate**. COPY: `PUT
   /automation/workflows/{uuid}/publish` first — `enableProjectWorkflow` refuses a copy that is not in
   the active deployment — then `PUT /automation/workflows/{uuid}/enable`. REFERENCE: `enable` only. On
   success: "Your automation is running", with **Open in builder** (COPY only) and **Done** (→ My
   Automations).

Errors are shown inline in the step (not toast-only). Closing the dialog after step 2 leaves a
disabled automation in My Automations — honest state, and the user can enable it there.

## Section 5 — Server additions

All EE, public frontend API `/api/embedded/v1`, JWT-only. Every new root segment (`connections`) is
added to `ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS`. Facades resolve the connected
user from `SecurityUtils.getCurrentUserLogin()` and enforce ownership; a foreign id yields **404**,
never 403, so ids cannot be enumerated.

| Route | Purpose | Implementation notes |
|---|---|---|
| `POST /automation/workflow-templates/{uuid}/provision` | frontend twin of `provisionWorkflowReference` | same `ConnectedUserCodeWorkflowReferenceFacade.getOrCreateReference`; 409 `MissingConnectionError` unchanged |
| `DELETE /automation/workflow-templates/{uuid}/provision` | frontend twin of `deprovisionWorkflowReference` | |
| `GET /connections` | every connection the user owns | `ConnectedUserConnectionFacade.getConnections(connectedUserId, null, List.of())` made null-component-safe; public `Connection` schema gains `componentName`, `connectionVersion`, `authorizationType`, `createdDate` (`authorizationParameters`/`parameters` are never exposed, same as the internal controller). Component title/icon are joined client-side from the component definitions the iframe already loads |
| `POST /components/{componentName}/connections` | create a connected-user connection | body `{name, authorizationType, connectionVersion, parameters}`; delegates to `ConnectedUserConnectionFacade.createConnectedUserConnection`; returns the id |
| `DELETE /connections/{id}` | delete own connection | ownership via `ConnectedUserConnectionFacade` (foreign → 404); `ConnectionFacade.delete` already throws `CONNECTION_IS_USED` when a deployment/test configuration references it — the controller maps that to **409 `{"reason": "CONNECTION_IS_USED"}`** instead of the generic 400 |
| `POST /connections/{id}/reauthorize` | replace `parameters` in place (OAuth2 code exchange re-run when a `code` is present) | **new facade methods**: platform `ConnectionFacade.updateAuthorization(id, parameters)` (extracts the OAuth2 authorization-code exchange out of `create` so both share it, then `ConnectionService.updateConnectionParameters`) and EE `ConnectedUserConnectionFacade.reauthorizeConnectedUserConnection(connectedUserId, connectionId, parameters)` (ownership check, then delegate). Keeps id/name/version so wired automations stay wired |
| `ConnectedUserProjectWorkflow` (public schema) | add `kind` (`COPY`/`REFERENCE`), `catalogWorkflowUuid`, `dangling`, `components[]` | **and the frontend list now includes reference rows**: today `getFrontendProjectWorkflows` returns only the user's own project (copies + scratch); references from `ConnectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows` are appended, resolved against the published catalog for label/description/components |

No changes under `server/libs/atlas/`, no new tables, no liquibase changesets.

## Section 6 — Theming

Applied once in `automation-hub.tsx` after `EMBED_INIT`:

- `mode` → `ThemeProvider` theme (`light` default).
- `primaryColor` → `--primary` and `--ring`; `--primary-foreground` is computed for contrast
  (relative-luminance threshold → white or near-black).
- `fontFamily` → `--font-sans`; `borderRadius` → `--radius`.

These are the shadcn/Tailwind v4 tokens the whole app reads, so the builder view under the hub route
inherits them for free. Documented limitation: host-page `@font-face` does not cross the iframe
boundary — `fontFamily` must be system/web-safe or already loadable by the iframe (a `fontUrl` option
is a later addition). Invalid values are ignored (never break the hub).

## Section 7 — Testing

- **Server** (`*IntTest`, Testcontainers, modeled on
  `ConnectedUserProjectWorkflowApiControllerCopyIntTest`): frontend provision/de-provision (incl. the
  409); `GET /connections`; create; delete (owned, foreign → 404, in-use → 409); reauthorize (owned,
  foreign → 404, platform-owned → 404). Unit tests: `FRONTEND_RESERVED_PATH_SEGMENTS` contains
  `connections`; `SpaWebFilter` forwards `/embedded/hub`, `/embedded/hub/automations`,
  `/embedded/hub/builder/x` to `automation-hub.html` and leaves `/embedded/hub/x.js` alone.
- **Client** (vitest): `useEmbedHandshake` (origin allow-list, READY broadcast, INIT storage) with
  the builder's existing tests still green after the extraction; `useAutomationHubStore`;
  `activationReducer` (step gating, COPY vs REFERENCE branches, 409 → step 1 with highlight, done
  state); each view with mocked hooks (loading / empty / error / happy); tab filtering; theme
  application (`primaryColor` → `--primary` + computed foreground, invalid ignored).
- **SDK**: `AutomationHub.test.tsx` — iframe `src`, `EMBED_READY` → `EMBED_INIT` round-trip, exact
  `params` shape (no `baseUrl`/`className`), `className` passthrough, `targetOrigin` derived from
  `baseUrl`.
- **Manual**: test-app `/hub` page against a local server with a published catalog project.

## Section 8 — Out of scope (v1)

- Logs / executions tab (explicitly deferred).
- Standalone "create connection" from the Connections tab.
- Full theme palette / typography / shadow surface; `fontUrl`.
- Host-navigation events (`onOpenWorkflow` callback) — the builder opens in-iframe only.
- Integrations (`ConnectDialog` model) inside the catalog.
- A wizard inputs form (needs a public endpoint for per-user workflow input values first).
- Template categories beyond catalog-project grouping; template thumbnails.
- Reworking `EmbeddedWorkflowBuilder`'s hardcoded layout classes.

## Risks and notes

- **JWT authorization on internal routes.** The builder already calls internal
  `/connected-users/{connectedUserId}/connections` with the connected-user JWT. The hub does not
  widen that surface — its own views use principal-scoped public routes — but the builder route
  under the hub keeps the same calls it makes today.
- **Reference references cannot self-heal** (CLAUDE.md): a `dangling` reference stays dangling; the
  hub surfaces it as "Needs attention" with Delete as the only action, matching the documented
  recovery (de-provision, then re-activate from Templates).
- **`getOrCreateReference` is not self-healing on repeat calls** either: a disabled row from a
  missing-connection provision is returned unchanged on later calls. The wizard therefore
  de-provisions before retrying step 2 after the user connects the highlighted component.

---

## Revision 2026-08-18 — two views, usage state on the template card

Directed by the product owner mid-implementation, after Tasks 1–10 had landed. This section
**supersedes** D1 and the parts of §1/§2/§3 it names; everything else in the spec stands.

Modelled on Appmixer's Automation Hub, where the catalog itself carries each template's activation
state rather than sending the user to a separate list of what they have activated.

### R1 — Sections in v1 are **Automations** and **Connections**. There is no Templates tab and no My Automations tab.

D1 is superseded. The hub has two tabs. The former Templates and My Automations views merge into one
**Automations** view at the index route `/embedded/hub`; `/embedded/hub/automations` is removed.

### R2 — The Automations view is a template grid plus a "Your automations" section

**Template grid.** Every published catalog template renders as a card carrying its own usage state:

- *Unused* — a **Use template** action opening the activation wizard (§4, unchanged).
- *Activated* — an **enable/disable toggle** wired to the same enable/disable routes the row list used,
  and a **Customize** action opening the in-hub builder on the user's copy. Customize is offered for
  `COPY` templates only: a `REFERENCE` points at a shared catalog workflow and is never editable.

**"Your automations" section.** Below the grid, a row list of everything the user has that does *not*
match a currently published template: blank workflows created from scratch, copies whose source
template has since been withdrawn, and dangling references. This is the row UI specified in §3's My
Automations section — label, component icons, enabled switch, status badge (Enabled / Disabled /
**Needs attention** when `dangling`), and a row menu with Open in builder (COPY only) and Delete.
Nothing about that row behaviour changes; only where it lives.

### R3 — Matching an automation to its template

- `REFERENCE` → the template whose `id` equals the row's `catalogWorkflowUuid`.
- `COPY` → the template whose `id` equals the row's **`copiedFromWorkflowUuid`**.
- Anything unmatched falls to the "Your automations" section.

`copied_from_workflow_uuid` already exists on `connected_user_project_workflow` and is already written
by `ConnectedUserProjectFacadeImpl#copyWorkflowTemplate` on every copy the hub's wizard creates (it
backs the sync bridge's implicit-copy dedup). It is **not** currently exposed on the public
`ConnectedUserProjectWorkflow` schema, so §5 gains one field:

| Route | Purpose | Implementation notes |
|---|---|---|
| `ConnectedUserProjectWorkflow` (public schema) | add `copiedFromWorkflowUuid` | the source template uuid for `COPY` rows, null otherwise; DTO + MapStruct mapper + regenerated TS client, exactly as `kind`/`catalogWorkflowUuid`/`dangling` were added |

Still no new tables and no liquibase changesets — the column exists.

### R4 — Consequences elsewhere in the spec

- §2 `AutomationHubTabsConfig` drops `templates`; the remaining keys are `automations`, `connections`
  and `newWorkflow`.
- §1's route list drops `/embedded/hub/automations`; the index route is the merged Automations view.
- §3's Builder section: back navigation returns to `/embedded/hub`.
- §4's wizard is unchanged and is launched from an unused template card.
- §6 theming, §5's other routes, §7 testing and §8 out-of-scope are unaffected.
