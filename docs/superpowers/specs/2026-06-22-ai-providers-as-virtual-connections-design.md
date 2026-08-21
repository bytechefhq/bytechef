# AI Providers as Read-Only Virtual Connections — Design

Date: 2026-06-22
Status: Approved for planning

## Problem

Enabling an AI Provider (OpenAI, Anthropic, …) and creating a component Connection for
the same provider are two separate acts today, storing the same API key twice:

- **AI Providers** live in the `property` table (`key = "ai.provider.openAi"`, encrypted
  `apiKey`, per-environment `enabled` flag). They are consumed by building chat clients
  directly from the key via `CatalogChatClientResolver` — they are **not** component
  connections. The admin UI for them is EE-only (`AiProviderFacade`); keys can also be set
  statically through `application.yml` (works in CE + EE).
- **Connections** live in the `connection` table (`componentName`, encrypted `parameters`,
  `environment`) and are what a workflow actually selects when it uses, e.g., the OpenAI
  component.

A user who has already configured an OpenAI key as an AI Provider still has to re-enter it
as a Connection to use the OpenAI component in a workflow.

## Goal

An **enabled** AI Provider (from `application.yml` **or** the EE admin UI) shows up in the
connections list as a regular, **workflow-selectable** connection. It behaves like any other
connection **except that it cannot be deleted or edited** from the connections surface — it
is system-managed. The AI Providers screen (or `application.yml`) remains the single source
of truth for the key.

## Decisions

These were settled during brainstorming and are fixed for this design:

1. **Synthesize, don't materialize.** No rows are written to the `connection` table and no
   schema migration is needed. AI-provider connections are projected on read.
2. **Compose at `ConnectionService`.** `ConnectionServiceImpl` is the single chokepoint every
   reader funnels through (`getConnection`, the `getConnections(...)` overloads,
   `getConnections(List<id>)`) and every mutator lives there too. Projecting here makes the
   workflow dropdown, job-time resolution, and the connections UI all see AI-provider
   connections with no change to any downstream caller.
3. **One-way, read-only mirror.** `property` / `application.yml` is the source of truth. The
   projected connection's credential is read-only on the connections surface; enabling,
   disabling, and editing happen on the AI Providers screen or in `application.yml`. No
   reverse sync.
4. **CE + EE, both sources.** Any enabled provider is mirrored — `application.yml`-configured
   ones in CE, plus property-table ones in EE. The projector lives in a CE-reachable module.
5. **Single `managed` flag** surfaced to the client (system-managed ⇒ hide delete and edit).
6. **New CE module** `platform-connection-ai-provider` hosts the projector and the shared
   apiKey-resolution logic.
7. **Supported providers: the 7 BEARER_TOKEN/`token` providers** — OpenAI, Anthropic, Groq,
   Mistral, Nvidia, Perplexity, DeepSeek. **Gemini is excluded** (see Edge Cases).
8. **`AUTOMATION` platform type only.** Embedded connections are per-customer-instance; a
   platform AI key does not belong there.

## Architecture

### Virtual-connection identity

Real connection ids are positive (autoincrement from 1050). Virtual ids are **negative** and
encode `(provider, environment)`:

```
id = -(providerId * 100 + environmentId)      // providerId ≥ 1, environmentId ∈ {0,1,2}
```

A small utility `AiProviderConnectionId` (in `platform-connection-api`):

- `long encode(Provider provider, int environmentId)`
- `decode(long id) → (int providerId, int environmentId)`
- `boolean isAiProviderConnectionId(long id)` ⇒ `id < 0`

Negative ids never collide with the real sequence, and `findById` / `findAllByIdIn` on a
negative id naturally miss the real `ConnectionRepository`, so routing is unambiguous. Because
a workflow definition persists the chosen `connectionId`, a negative id is stable across
restarts and re-resolves to the same `(provider, environment)`.

`environmentId` here is the same `int` the AI-provider code already passes to
`PropertyService` as `(long) environment`, and the same `int` stored in
`Connection.environment` — the dimensions line up natively.

### Shared apiKey resolution

Extract the existing duplicated logic (`enabled = property.isEnabled() ||
hasConfigApiKey(provider)` and apiKey lookup from property-or-config, currently inlined in
`AiProviderFacadeImpl` and again in `CatalogChatClientResolverImpl`) into a CE service in the
new module, e.g. `AiProviderConnectionSource`:

- `boolean isEnabled(Provider provider, int environmentId)`
- `Optional<String> getApiKey(Provider provider, int environmentId)`

It depends only on `PropertyService` (CE), `ApplicationProperties` (CE), and the `Provider`
enum (CE). `AiProviderFacadeImpl` (EE) is refactored to delegate to it to remove the
duplication; the catalog resolver can adopt it later (out of scope here, noted as follow-up).

### `AiProviderConnectionRepository` (new, custom)

A hand-written projector — **not** a Spring Data repository. Interface in
`platform-connection-api` (returns `Connection` / `List<Connection>`, primitives only — no new
dependencies in that module):

```java
public interface AiProviderConnectionRepository {
    Optional<Connection> findById(long id);
    List<Connection> findAll();                                  // all enabled, all environments
    List<Connection> findAllByComponentName(
        String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId);
    List<Connection> findAllByEnvironment(int environmentId);
    List<Connection> findAllByIdIn(List<Long> ids);
}
```

Implementation (`AiProviderConnectionRepositoryImpl`, `@Component` in
`platform-connection-ai-provider`):

- Iterates the 7 supported providers × environments (`Environment.values()` for unfiltered
  queries, the requested env otherwise).
- Includes a provider/env only when `AiProviderConnectionSource.isEnabled(...)` is true and an
  apiKey is present.
- Builds each `Connection` in memory:
  - `id` = `AiProviderConnectionId.encode(provider, env)` (negative)
  - `componentName` = provider's component name (`Provider.getName()`; matches the existing
    component names for the 7 — e.g. `openAi`, `anthropic`)
  - `connectionVersion` = 1
  - `authorizationType` = `BEARER_TOKEN`
  - `parameters` = `{ "token": <apiKey> }` (key from `Authorization.TOKEN`)
  - `environment` = env, `type` = `AUTOMATION`, `status` = `ACTIVE`, `name` = provider label
  - `managed` = true
- **Bypasses `CredentialStore`** — the key is already decrypted by `PropertyService` /
  `ApplicationProperties`; parameters are populated directly.

Module dependencies: `platform-connection-api`, `platform-configuration-api`
(`PropertyService`), `app-config` (`ApplicationProperties`), and the `ai/llm` component module
(`Provider`, `Authorization.TOKEN`). It must be on the classpath of every app that lists
connections. `platform-connection-service` does **not** depend on it.

### `ConnectionServiceImpl` composition

Inject `ObjectProvider<AiProviderConnectionRepository>` (optional — when no impl bean is
present, behavior is identical to today). A private helper resolves the repo or returns an
empty projection.

**Reads** — merge real + projected, projected entries only for `type == AUTOMATION`, final
list sorted by name (matching existing ordering):

| Method | Projection behavior |
| --- | --- |
| `getConnection(long id)` | If `isAiProviderConnectionId(id)` → projector `findById` (throws `Connection does not exist` if disabled/absent), bypassing `populateParameters`. Else existing path. |
| `getConnections(PlatformType type)` | Append `findAll()` when `type == AUTOMATION`. |
| `getConnections(componentName, version, type)` | Append `findAllByComponentName(componentName, version, null)` when `AUTOMATION`. |
| `getConnections(componentName, version, tagId, environmentId, type)` | Append projected filtered by componentName/version/environmentId when `AUTOMATION` **and `tagId == null`** (virtual connections have no tags). |
| `getConnections(List<Long> ids)` | Split ids by sign; real ids → `connectionRepository.findAllByIdIn`, negative ids → projector `findAllByIdIn`; merge. |
| `getConnectionsByVisibility(...)` | Unchanged — virtual connections are excluded (they don't participate in promote/demote). |
| `getInactiveConnections` / `validateConnectionsActive` | Unchanged — `findAllByIdIn` skips negative ids, so a workflow referencing a virtual connection is treated as ACTIVE (correct). |

**Writes** — guard at the top on `isAiProviderConnectionId(id)` and throw a new
`ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY` (`ConfigurationException`):
`delete`, `update(id, tagIds)`, `update(id, name, …)`, `updateConnectionParameters`,
`updateConnectionStatus`, `updateVisibility`, `updateCreatedBy`,
`updateConnectionCredentialStatus`. `create` / `registerExisting` take no id and are
unaffected.

### Surfacing `managed` to the UI

- Add `@Transient private boolean managed = false;` to `Connection` (no DB column).
- Projected connections set it `true`; all persisted connections keep `false`.
- Propagate through `ConnectionDTO` → REST `ConnectionModel` and the GraphQL connection type
  as `managed`.
- Client: when `managed === true`, hide both the **delete** and **edit** affordances in the
  connections list/detail. The credential field, if shown, is read-only.
- Server-side guards (above) are the authority; the flag is cosmetic / UX.

## Edge Cases

- **Gemini excluded.** `VERTEX_GEMINI`'s AI-provider credential is an `apiKey`, but the
  `gemini` component connection requires `projectId` + `location` (Vertex AI). The shapes do
  not match, so a projected gemini connection would be invalid. Excluded from the supported
  set. Future: add it if/when the gemini component supports a `token`-based (Gemini Developer
  API) authorization.
- **Negative ids through the stack.** `connectionId` is `Long` everywhere (domain, DTOs, REST,
  GraphQL ID, workflow JSON), so negative values serialize fine. The plan must grep for any
  `id > 0` / positivity validation on connection ids and confirm none rejects a virtual id.
- **A disabled provider with a workflow still referencing its virtual id.** Resolution returns
  "does not exist" and the job fails at use — acceptable and consistent with deleting a real
  connection that a workflow still references.
- **`isConnectionUsed` on delete.** Reachable only via the facade before the service guard;
  the service-level read-only guard is authoritative regardless. The facade/UI should also
  suppress the delete action for `managed` connections to avoid pointless side effects
  (e.g. `deleteScheduledConnectionRefresh`).
- **Performance.** Projection issues up to `providers × environments` small `PropertyService`
  lookups per unfiltered list call. Acceptable at current scale; a short-lived cache is a
  possible later optimization, not part of this design.

## Out of Scope

- Migrating `CatalogChatClientResolverImpl` onto the shared `AiProviderConnectionSource`
  (noted as a follow-up; this design only de-duplicates the facade).
- Gemini support.
- Embedded (`EMBEDDED`) platform type.
- Any change to how AI Providers are enabled/edited (that surface is unchanged).

## Implementation correction (post-final-review)

The original premise of Decision 2 — "compose at `ConnectionService` so the connections UI sees
AI-provider connections with no change to any downstream caller" — was **incomplete**. The automation
connections list and the workflow-editor dropdown do not read `ConnectionService` directly; they go
through `WorkspaceConnectionFacadeImpl.getConnections`, which scopes results to the
`workspace_connection` join table (positive ids only) and re-filters by those ids. That layer
discarded the projected (negative-id) connections, so the `ConnectionService`-level merge never
reached the UI.

Correction (shipped): a dedicated projected-only path was added —
`ConnectionService.getAiProviderConnections(...)` → `ConnectionFacade.getAiProviderConnections(...)` →
appended in `WorkspaceConnectionFacadeImpl.getConnections` **after** `filterVisible`. This makes the
projected connections **platform-global**: they appear as read-only, non-deletable connections in
**every** workspace, in both CE and EE, bypassing the join-table scoping and the visibility resolver
(appending after `filterVisible` means CE's PRIVATE-only resolver cannot drop them, and EE's resolver
is likewise bypassed). The real-connection workspace-scoping is preserved (an empty join table yields
an empty real-connection list, never an unscoped "match everything" query). Job-time connection
resolution is unaffected — it already funnels through `ConnectionService` where the merge applies.

## Testing

**Unit**
- `AiProviderConnectionId`: encode/decode round-trip, `isAiProviderConnectionId`, no collision
  with positive ids.
- `AiProviderConnectionRepositoryImpl`: enabled via property; enabled via `application.yml`;
  disabled ⇒ absent; per-environment projection; `findById` decodes to the right provider/env;
  `findAllByComponentName` filters; `findAllByIdIn` returns only enabled negatives; Gemini
  never projected.
- `ConnectionServiceImpl` composition: virtual id routes to the projector; list overloads
  merge and sort; `tagId`-filtered and visibility-filtered queries exclude virtual; write
  methods throw `AI_PROVIDER_CONNECTION_READ_ONLY` for virtual ids; **absent `ObjectProvider`
  ⇒ behavior identical to today**.

**Integration**
- Workspace connections list endpoint includes the AI-provider connection for an enabled
  provider and offers it in the OpenAI-component connection dropdown.
- `DELETE` of an AI-provider connection returns the read-only error and the connection
  remains.
