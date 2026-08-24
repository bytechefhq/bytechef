# Public Automation API — Slice 1: Conventions and Read Surface (Design)

**Date:** 2026-08-24
**Status:** Approved design, pending spec review
**Scope of this document:** Sub-project #1 of five. Establishes the conventions the whole public
automation API inherits, and proves them on a read-only surface. Later slices have their own specs.

## 1. Context

`/api/automation/v1` today is four operations, in
`server/ee/libs/automation/automation-configuration/automation-configuration-public-rest`:

| Operation | Guard |
|---|---|
| `GET /workflow-executions` | none beyond authentication |
| `GET /workflow-executions/{id}` | none beyond authentication |
| `POST /projects/deploy` | `ROLE_ADMIN` on `ProjectCodeWorkflowFacadeImpl#save` |
| `POST /projects/{id}/git/pull` | `hasPermission(#projectId, 'Project', 'DEPLOYMENT_PULL')` |

Three of the four exist because the CLI needed them, not because anyone designed a public contract.
The internal surface behind them is 36 CE paths plus 6 EE, across `project`, `project-deployment`,
`workflow`, `connection`, `workspace`, `category` and `tag`.

The surface also carries two inconsistencies this slice settles:

- `POST /projects/deploy` uses a blanket `ROLE_ADMIN` while its sibling `git/pull` uses a
  per-resource scope check, and the `workspaceId` it acts on arrives in the request body unchecked.
- `AutomationApiKeyAuthenticationProvider` performs no `getType()` check, so an admin key or an
  `EMBEDDED` key authenticates against the automation surface. Only the two admin providers
  (`PlatformApiKeyAuthenticationProvider`, `EmbeddedPlatformUserApiKeyAuthenticationProvider`)
  inspect the type at all.

## 2. Decomposition

The full request — CI/CD, ops, full control plane, and workflow authoring — is five sub-projects,
each with its own spec, plan and implementation cycle. CLI commands ship inside each slice rather
than as a separate project, extending the command set established in
`2026-07-22-bytechef-cli-public-api-design.md`.

| # | Slice | Contents |
|---|---|---|
| 1 | **Conventions + read surface** (this document) | credential model, scoping, pagination, errors, identifiers; reads for workspaces, projects, versions, workflows, deployments, executions |
| 2 | CI/CD lifecycle | project create/update/publish/duplicate/export/import, git configuration and pull, deploy |
| 3 | Ops control | deployment enable/disable, per-workflow enable/disable, run a workflow, job inspection, re-run |
| 4 | Connections and workspaces | connection CRUD, register-existing, tags, categories, workspace management |
| 5 | Workflow authoring | the workflow-definition JSON schema as a public contract, node and connection editing |

They are ordered by dependency. Slice 1 fixes the conventions everything else inherits; getting
them wrong later is a breaking change. Slice 5 is last because publishing the workflow-definition
schema is the largest and least reversible commitment, and it benefits from the rest being settled.

## 3. Scope

**In scope:** ten read endpoints (two already published), one declared error schema, the credential
rule, two Liquibase migrations, and six new CLI commands.

**Out of scope:** writes of any kind; connections, tags and categories (slice 4 — credential-bearing
resources need their own security section); component definitions (a platform concern, not
automation configuration); the workflow-definition schema (slice 5).

## 4. Credential and authorization model

### 4.1 Key type

`AutomationApiKeyAuthenticationProvider` gains the check it currently lacks:

```java
if (apiKey.getType() != PlatformType.AUTOMATION) {
    throw new BadCredentialsException("Automation API key required");
}
```

Admin keys and `EMBEDDED` keys are rejected. This mirrors the decision already taken for the
embedded bridge and makes one rule hold across the product: **an admin key is for tenant-wide
operations only — today just `POST /api/platform/v1/custom-components/deploy` — and every other
surface takes a typed key.**

A consequence worth stating: a single `AUTOMATION` key now covers the entire CLI, because the
embedded providers accept any typed key.

### 4.2 Authorization

No `ROLE_ADMIN` anywhere on this surface. Every operation is guarded by a `hasPermission(...)`
scope token on the facade, which keeps `AutomationAuthorizationContext.isSkipChecks()` the single
chokepoint that `ROLE_ADMIN` guards bypass.

`POST /projects/deploy` moves from `ROLE_ADMIN` to
`hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')`. This is in scope here, not in slice 2,
because it is the same decision as the rest of section 4 and leaving it inconsistent for two slices
invites copying.

Every read in this slice maps onto a facade method that is **already** correctly guarded — verified
against `ProjectFacadeImpl`, `ProjectDeploymentFacadeImpl` and `WorkspaceFacadeImpl`. The public
controllers delegate; no new `@PreAuthorize` annotation is introduced for reads.

Slice 1 reuses existing tokens only. Introducing a distinct `PROJECT_VIEW` is deliberately deferred:
enum ordinals are persisted as INT, so a new value must be appended, and it is worth deciding
alongside the write operations in slice 2 rather than on read evidence alone.

### 4.3 Resolving a uuid before the permission check

`hasPermission` targets a numeric id, but public paths carry uuids. The resolution therefore happens
at the API-facade boundary: the facade resolves uuid → id and delegates to the guarded method, which
still takes the numeric id and keeps its existing annotation. Two consequences:

- An unknown uuid returns 404 before any permission check runs. This is acceptable — uuids are not
  enumerable, so the 404-versus-403 distinction leaks nothing an attacker could use.
- No existing `@PreAuthorize` annotation changes shape, so the guards stay greppable and the
  internal callers are unaffected.

Every guarded method in this slice already takes the id of the resource being checked, so the
resolution is always a single uuid → id hop. Verified against the existing annotations:
`getWorkspaceProjects` and `getWorkspaceProjectDeployments` take a workspace id, `getProject` and
`getProjectVersions` take a project id, and `getProjectDeployment` is guarded on
`hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_VIEW')` — its own id, not its workspace's.

## 5. Identifiers

**Configuration is addressed by uuid. Execution records are addressed by their numeric id.**

That one line is the whole rule, and it is stated this way deliberately: a per-resource lookup table
of id shapes is not something consumers can hold in their heads, but "config versus runtime" is.

| Entity | Today | This slice |
|---|---|---|
| `Project` | has `uuid` | expose it as the public identifier |
| `ProjectWorkflow` | has `uuid` | expose it as the public identifier |
| `ProjectDeployment` | numeric only | **new** nullable `uuid` column, backfilled, unique index |
| `Workspace` | numeric only | **new** nullable `uuid` column, backfilled, unique index |
| `ProjectVersion` | numeric only | unchanged — addressed by version number, already stable |
| `Job` (workflow execution) | numeric only | **unchanged** |

`Job` is excluded on principle, not on cost. It is an `atlas-execution` entity, and this repo keeps
the workflow engine agnostic of platform concerns; adding a column to `Job` so that a REST contract
can have a uniform id shape is that boundary violation. The supporting arguments point the same way:
`job` is the highest-volume table in the schema, nothing ever promotes an execution across
environments, and the two execution operations are the only ones already published with `int64`.

If uniformity is wanted later, the additive path stays open — publish a uuid for executions
alongside the numeric id.

In every case the uuid is external identity only. Primary keys and internal joins stay numeric; only
what the mappers project changes.

## 6. Conventions

### 6.1 Workspace scoping

`workspaceId` is a required query parameter on every collection endpoint; item URLs stay flat
(`/projects/{uuid}`, not `/workspaces/{id}/projects/{uuid}`). This matches the automation public
surface as already published — `/workflow-executions` is flat with query filters, and
`POST /projects/deploy` takes `workspaceId` as a parameter — and it composes uniformly with the
other filters.

`GET /workspaces` is therefore load-bearing: without it a caller has no way to discover a
`workspaceId`. It returns the workspaces the caller is a member of.

### 6.2 Pagination

The existing `Page` envelope, unchanged on the way out: `number`, `size`, `numberOfElements`,
`totalPages`, `totalElements` and `content`.

On the way in, the published endpoint takes `pageNumber` only — page size is fixed server-side.
Slice 1 adds an optional `pageSize` across all collections including the published one, which is
additive and therefore allowed under §6.4. Its default and maximum are set in the plan, not here.

Cursor pagination was considered and rejected. The one already-published paged endpoint is offset,
so cursors would mean either breaking it or running two idioms on one public API. The facades
already return `Page`, and offset's weakness — drift under concurrent inserts — barely applies to
projects, deployments and workflows. Deep-offset cost is real but applies only to executions, where
cursors can be added later as an additional parameter without disturbing the rest.

### 6.3 Errors

One `Error` schema, declared on every 4xx and 5xx response in the spec, Spring `ProblemDetail`-
compatible:

| Field | Meaning |
|---|---|
| `type` | URI identifying the problem type |
| `title` | short human-readable summary |
| `status` | HTTP status code |
| `detail` | human-readable explanation of this occurrence |
| `errorKey` | stable machine-readable key |

`errorKey` is the point of the exercise: consumers branch on a key, never on prose. Today the public
specs declare only `401 → UnauthorizedError` with no body schema, so Spring's default error shape
leaks as the de-facto contract.

### 6.4 Versioning

`/v1` is additive-only: new fields, new optional parameters, new endpoints. No field ever changes
meaning or type within `v1`; a change that would requires `/v2`.

## 7. Endpoints

| Method | Path | Authorization | Notes |
|---|---|---|---|
| GET | `/workspaces` | membership-filtered | no token; returns what the caller belongs to |
| GET | `/projects?workspaceId=` | `Workspace:WORKFLOW_VIEW` | paged; filters: tag, category |
| GET | `/projects/{uuid}` | `Project:WORKFLOW_VIEW` | |
| GET | `/projects/{uuid}/versions` | `Project:WORKFLOW_VIEW` | versions by number |
| GET | `/projects/{uuid}/workflows` | `Project:WORKFLOW_VIEW` | optional `?version=` |
| GET | `/workflows/{uuid}` | `Project:WORKFLOW_VIEW` on the owning project | see 7.1 |
| GET | `/project-deployments?workspaceId=` | `Workspace:DEPLOYMENT_VIEW` | paged; filters: project, enabled, environment |
| GET | `/project-deployments/{uuid}` | `ProjectDeployment:DEPLOYMENT_VIEW` | existing guard, unchanged |
| GET | `/workflow-executions` | unchanged | **published**; gains uuid filters, see 7.2 |
| GET | `/workflow-executions/{id}` | unchanged | **published**; unchanged |

### 7.1 What `GET /workflows/{uuid}` returns

Metadata — uuid, label, description, version, enabled, trigger and task counts — plus `definition`
as an **opaque JSON string** with no schema declared.

This is deliberate. Returning the definition as structured, schema-declared JSON would publish the
workflow-definition contract by accident and leave slice 5 with nothing to decide. An opaque string
is readable and diffable, which is what a config-as-code pipeline needs, while the shape stays
uncommitted until slice 5 commits it on purpose.

### 7.2 Filters on the published executions endpoint

`/workflow-executions` currently filters by `projectId` and `projectDeploymentId` as `int64`. It
gains `projectUuid` and `projectDeploymentUuid` alongside. The numeric parameters keep working and
are marked deprecated in the spec.

This is the only place the uuid decision collides with something already published, and the additive
form costs one lookup per filter. Leaving it would make the seam the first thing a consumer trips
over.

## 8. CLI

| Command | Endpoint |
|---|---|
| `automation workspace list` | `GET /workspaces` |
| `automation project list` | `GET /projects` |
| `automation project get <uuid>` | `GET /projects/{uuid}` |
| `automation project versions <uuid>` | `GET /projects/{uuid}/versions` |
| `automation project workflows <uuid>` | `GET /projects/{uuid}/workflows` |
| `automation workflow get <uuid>` | `GET /workflows/{uuid}` |
| `automation deployment list` | `GET /project-deployments` |
| `automation deployment get <uuid>` | `GET /project-deployments/{uuid}` |
| `automation execution list` | existing; gains the uuid filters |
| `automation execution get <id>` | existing; unchanged |

All go through `cli/clients/automation-configuration`. That client generates from
`$projectDir/openapi.yaml` — a **vendored copy** of the server spec, unlike the three embedded
clients, which reference `${rootDir}/server/...`. The copy is byte-identical today, so the drift is
latent rather than broken. This slice repoints it at the server module.

The `--workspace-id` already stored in the CLI profile supplies the required `workspaceId` parameter
so collection commands need no extra flag.

## 9. Testing

- **Credential rule** — a path-routing test pinning that `/api/automation/v1/**` is claimed by
  `AutomationApiKeySecurityConfigurer`, plus a provider unit test for each of the three key types
  (admin rejected, `EMBEDDED` rejected, `AUTOMATION` accepted).
- **Authorization** — a `@PreAuthorize` proxy IntTest per token, in the shape of
  `AutomationProjectCodeWorkflowApiControllerListAuthorizationIntTest`: bean method call under
  `@EnableMethodSecurity`, asserting `AccessDeniedException` for the wrong authority and success for
  the right one. Place these outside any component-scanned controller package.
- **uuid resolution** — a test per resource that an unknown uuid returns 404 and never reaches the
  guarded method.
- **Migrations** — verified through an existing `*IntTest` on Testcontainers, which builds the schema
  from scratch. The `liquibase` Spring profile does not apply migrations via `bootRun`.
- **CLI** — `StubApi` tests asserting the request path and the exit-code mapping for each command.

## 10. Risks and breaking changes

1. **Admin keys stop working against `/api/automation/v1`.** This is the intended effect of §4.1 but
   it is a real break for anyone scripting with one. Needs a release note, and the CLI should
   translate the 401 into a message that names the fix rather than reporting a bare auth failure.
2. **uuid backfill on `project_deployment`** is proportional to deployment count. `workspace` is
   trivial. Both tables are long released, so these are new changesets — never edits to init.
3. **`GET /workspaces` is load-bearing.** Its authorization is membership, not a scope token; get it
   wrong and it becomes a tenant-wide enumeration endpoint. It deserves the most careful test in the
   slice.
4. **`projects/deploy` changes its guard.** A caller who is an admin but holds no `PROJECT_CREATE`
   scope in the target workspace loses access they had. That is the point of the change, but it is a
   behaviour change in an already-published operation.
