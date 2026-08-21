# Connection Visibility → EE — Design

**Date:** 2026-06-13
**Status:** Approved (pending spec review)
**Branch:** 0_732

## Goal

The connection-visibility feature (introduced in `696e7c12` "4750 Add ConnectionVisibility,
ConnectionStatus enums, organization connections, and reassignment") was built entirely inside
Apache-licensed CE modules (`server/libs/platform/platform-connection`,
`server/libs/automation/automation-configuration`). It is conceptually an EE feature: in CE only
`PRIVATE` connections are reachable. The goal is to relocate the **EE-only logic** to EE
(`server/ee/libs/...`) so that **no EE-licensed visibility code ships in the CE artifact**, while
keeping the unavoidable CE foundation (connections still exist in CE and carry a visibility of
`PRIVATE`).

This is a packaging/licensing-driven extraction. Strict-audit synchronous rollback is **not**
required to be preserved (after-commit auditing is acceptable) — relevant to the follow-on audit
slice.

## What stays in CE (the foundation floor)

These cannot move — the CE `Connection` domain and CE connection CRUD depend on them:

- `ConnectionVisibility` / `ConnectionStatus` enums and `Connection.visibility` / `Connection.status`
  fields + their Liquibase columns. Ordinal stability remains pinned by `ConnectionVisibilityTest`.
- Base connection CRUD on `WorkspaceConnectionFacade` (`create`, `delete`, `getConnection`,
  `getConnections`, `disconnectConnection`, `registerExisting`), `ConnectionApiController` (REST),
  and the basic GraphQL ops (`disconnectConnection`, `registerExistingConnection`).
- `ConnectionFacadeImpl.create()`'s `bytechef.edition`→`PRIVATE` gate (forces PRIVATE for non-EE /
  embedded). This is the CE-side enforcement that the feature is inert in CE.
- `ConnectionService` state-setters `updateVisibility` / `updateConnectionStatus` / `updateCreatedBy`
  — dumb field persistence, no policy. EE owns the policy that calls them.
- `ProjectMembershipAccessor` SPI + CE no-op default (returns input unchanged). Existing seam.

## The three extraction seams

Each fault line gets the mechanism that matches its entanglement.

### 1. Read path → CE SPI with EE strategy

The scope-resolution logic is inlined in `WorkspaceConnectionFacadeImpl.getConnections`
(`:299–348`): membership narrowing + the visibility switch (`ORGANIZATION/WORKSPACE → visible`,
`PROJECT → project-shared`, `PRIVATE → creator/admin`). `getConnections` must stay CE (CE REST +
GraphQL call it), so extract only the decision behind a CE-defined SPI:

- **`ConnectionVisibilityResolver`** (CE interface). `getConnections` calls
  `resolver.filterVisible(connections, principal, workspaceId)` instead of the inline switch.
- **CE default impl**: PRIVATE-only — keep connections where the principal is creator, or is admin.
  WORKSPACE/PROJECT/ORG never appear in CE (creation is forced PRIVATE), so they are simply not
  visible. Honest CE behavior.
- **EE impl**: the full scope resolution, pulling in `ProjectMembershipAccessor` (real membership
  narrowing — no EE impl exists today; one is added here) and `ProjectConnectionService` (now EE,
  see below).

### 2. Write path → segregate the facade interface by edition

Follow the established `WorkspaceService` pattern: a CE base interface plus an EE interface that
`extends` it (same simple name, CE vs EE package), with paired `@ConditionalOnCEVersion` /
`@ConditionalOnEEVersion` impls.

- **CE `WorkspaceConnectionFacade`** (`com.bytechef.automation.configuration.facade`) keeps only base
  CRUD: `create`, `delete`, `getConnection`, `getConnections`, `disconnectConnection`,
  `registerExisting`. CE impl `@ConditionalOnCEVersion`.
- **EE `WorkspaceConnectionFacade`** (`com.bytechef.ee.automation.configuration.facade`) `extends` the
  CE interface and adds the transitions: `promoteToWorkspace`, `promoteAllPrivateToWorkspace`,
  `demoteToPrivate`, `shareConnectionToProject`, `revokeConnectionFromProject`,
  `revokeSingleProjectShareAuditOnly`, `setConnectionProjects`. EE impl `@ConditionalOnEEVersion`.
  Because the EE interface extends the CE contract, the EE impl is the active bean for *both*
  interfaces in EE and transparently satisfies CE consumers (CE REST/GraphQL, EE AI callers
  `CopilotConfiguration` / `ListConnectionsForComponentToolCallback` / `AiHubConfiguration`); in CE,
  only the base interface + CE impl exist.
- The 6 EE-only mutations in `ConnectionGraphQlController` move to a new EE GraphQL controller that
  injects the EE `WorkspaceConnectionFacade`; CE's controller keeps `disconnectConnection` /
  `registerExisting`. The corresponding parts of `connection.graphqls` move to an EE schema file.

### 3. Organization + reassignment → move wholesale to EE

- **Organization** (least entangled): `OrganizationConnectionFacade` (+impl),
  `OrganizationConnectionGraphQlController`, `organization-connection.graphqls` move to EE as a unit
  (only its own controller consumes it). The `@Value("${bytechef.edition}")` runtime gate becomes
  `@ConditionalOnEEVersion` bean presence.
- **Reassignment**: `ConnectionReassignmentFacade` (+impl),
  `ConnectionReassignmentGraphQlController`, `connection-reassignment.graphqls` move to EE. The one
  CE tie — `WorkspaceUserRemovalListener` calling `markConnectionsPendingReassignment` — is broken
  via the domain event `WorkspaceUserRemovedEvent` (already added in `696e7c12`): CE keeps firing
  the event; the listener moves to EE as an `@EventListener`. CE→EE with no compile dependency.

### 4. ProjectConnection → move fully to EE

`ProjectConnection` + `ProjectConnectionService` + `ProjectConnectionRepository` + the
`project_connection` table migration all move to EE. Justification: the only consumer is
`WorkspaceConnectionFacadeImpl`, and every use is in the read filter (→ EE resolver) or the
share/revoke/demote paths (→ EE facade); nothing else in CE references it. EE-only tables are an
established pattern (`persistent_audit_event` is defined in an EE changelog). The table's FKs to the
CE `connection`/`project` tables remain valid; in CE the table does not exist and there is nothing
to cascade. Result: zero connection-sharing storage in CE.

**Ordering constraint:** because CE `WorkspaceConnectionFacadeImpl` references
`ProjectConnectionService` in both its read and write paths, `ProjectConnection` can only move to EE
*after* the read-path SPI extraction and the write-path facade split have removed those CE
references. It is therefore the **last** slice. Until then the EE resolver and EE write facade call
the still-CE `ProjectConnectionService` (EE→CE is allowed).

## Module placement

EE targets under `server/ee/libs/automation/automation-configuration/` (creating an EE
`automation-configuration-graphql` module if absent):

- `automation-configuration-api` (EE): EE `WorkspaceConnectionFacade` (extends the CE interface),
  `ConnectionReassignmentFacade`, `OrganizationConnectionFacade` interfaces, `ProjectConnection`
  domain + `ProjectConnectionService` interface.
- `automation-configuration-service` (EE): the impls (EE `WorkspaceConnectionFacadeImpl`, EE
  `ConnectionVisibilityResolver`, EE `ProjectMembershipAccessor`, `ProjectConnectionServiceImpl`,
  `ProjectConnectionRepository`, `OrganizationConnectionFacadeImpl`,
  `ConnectionReassignmentFacadeImpl`, the moved reassignment listener) + the EE Liquibase changelog
  for `project_connection`.
- `automation-configuration-graphql` (EE): an EE connection GraphQL controller for the visibility
  mutations, `OrganizationConnectionGraphQlController`, `ConnectionReassignmentGraphQlController` +
  their `.graphqls` schemas.
- `automation-configuration-remote-client` (EE): `@ConditionalOnEEVersion` stub clients for the
  relocated facades. The existing `RemoteWorkspaceConnectionFacadeClient` is extended to implement the
  EE `WorkspaceConnectionFacade`'s added transition methods.

Staying in CE: the `ConnectionVisibilityResolver` SPI interface + CE PRIVATE-only default (since the
CE `WorkspaceConnectionFacadeImpl.getConnections` depends on it), and the CE base
`WorkspaceConnectionFacade` interface + CE CRUD impl.

## Edition gating

- EE beans are presence-gated with `@ConditionalOnEEVersion` (replacing the runtime
  `@Value("${bytechef.edition}")` checks in `OrganizationConnectionFacadeImpl`).
- `@ConditionalOnCoordinator` retained where it currently applies (org/reassignment controllers run
  on the coordinator node).
- CE-default/EE-override uses **paired edition conditionals** on plain `@Service` beans, matching the
  established convention in `WorkspaceServiceImpl` (CE `@Service @ConditionalOnCEVersion`, EE
  `@Service @ConditionalOnEEVersion`). The CE defaults (`ConnectionVisibilityResolver` PRIVATE-only,
  `ProjectMembershipAccessor` no-op) are `@ConditionalOnCEVersion`; the EE impls are
  `@ConditionalOnEEVersion`. Mutually exclusive — exactly one active per edition. No `@Primary`, no
  `@ConditionalOnMissingBean`, no autoconfiguration import. (Note: today's `DefaultProjectMembership-
  Accessor` is an unconditional `@Service`; this work adds `@ConditionalOnCEVersion` to it so the EE
  impl can take over.)

## Audit interplay (enables the follow-on audit slice)

Moving the transition facades to EE carries their `@AuditConnection` annotations to EE for free.
After this work the only remaining CE audit call sites are `ProjectDeploymentFacadeImpl` and
`ProjectDeploymentJobPrincipalAccessor` (project-deployment connection audit). The audit-severance
(`connection.audit` package → EE via a domain-event seam for those two callers) becomes a smaller
**follow-on effort**, out of scope for this spec but unblocked by it.

## Implementation slices (each its own plan → impl, in dependency order)

- **A. Read-path SPI** — introduce `ConnectionVisibilityResolver` (CE interface + PRIVATE-only CE
  default + EE impl). `WorkspaceConnectionFacadeImpl.getConnections` delegates to it. The EE impl
  calls the still-CE `ProjectConnectionService` + a new EE `ProjectMembershipAccessor`. This removes
  the read-path visibility logic from the CE facade. Self-contained, low risk.
- **B. Organization connections → EE** — move facade+controller+schema; flip the `bytechef.edition`
  gate to `@ConditionalOnEEVersion`. Least entangled.
- **C. Reassignment → EE** — move facade+controller+schema; listener → EE `@EventListener` on the
  CE-fired `WorkspaceUserRemovedEvent`.
- **D. Write-path interface segregation → EE** — extract an EE `WorkspaceConnectionFacade` that
  `extends` the CE one with the transition methods (paired-conditional impls); CE impl keeps CRUD
  only; the EE-only GraphQL mutations move to an EE controller injecting the EE facade. Removes the
  write-path `ProjectConnectionService` uses from the CE facade. Largest.
- **E. ProjectConnection → EE** — **last**, once A and D have removed all CE references: relocate
  `ProjectConnection` domain/service/repo + the `project_connection` table migration to EE.

(Audit severance is a separate follow-on effort, unblocked by D.)

## Testing

- **CE default resolver**: a CE test asserting non-`PRIVATE` connections are invisible and only
  creator/admin see `PRIVATE` ones.
- **EE resolver**: workspace/project/org visibility tests move to / are added in EE, including real
  `ProjectMembershipAccessor` narrowing.
- **Relocated tests**: `WorkspaceConnectionFacadeTest` (visibility portions),
  `ConnectionReassignmentFacadeTest`, `OrganizationConnectionFacadeTest`,
  `ConnectionGraphQlControllerSecurityIntTest`/`AuthorizationTest` move to EE with their subjects;
  the enum ordinal-stability test stays CE.
- **Edition wiring**: per-app context-load checks that a CE build starts with no EE visibility beans
  (resolver = PRIVATE-only) and an EE build wires the full set. EE microservice apps resolve the
  relocated facades via the new remote-client stubs.
- **Migration**: verify `project_connection` is created under EE and absent under CE; FK cascade
  from `connection`/`project` deletes still behaves in EE.

## Risks & decisions

- **`getConnections` stays CE, logic moves**: the method signature and CE callers are unchanged; only
  the filtering decision is delegated. This avoids dragging the CE REST/GraphQL surface into EE.
- **`ConnectionService` state-setters stay CE**: persisting an enum field is not policy; keeping them
  CE avoids splitting the core connection service. Accepted.
- **EE-only `project_connection` table**: introduces edition-divergent schema. Mitigated by the
  established EE-changelog pattern; documented so the FK-to-CE-table relationship is understood.
- **EE bean override via paired edition conditionals (decided):** CE defaults are `@Service
  @ConditionalOnCEVersion`, EE impls `@Service @ConditionalOnEEVersion` — mutually exclusive, one per
  edition. No `@Primary`, no `@ConditionalOnMissingBean`, no `@AutoConfiguration`. Chosen to match the
  established convention in `WorkspaceServiceImpl` (CE/EE pair). The existing unconditional
  `DefaultProjectMembershipAccessor` gains `@ConditionalOnCEVersion` so the EE impl can take over.
