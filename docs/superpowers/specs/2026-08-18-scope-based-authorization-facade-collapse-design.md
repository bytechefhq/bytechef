# Scope-based authorization, and collapsing the two-facade split

**Status:** design approved, not implemented.
**Date:** 2026-08-18
**Ticket:** 732

## Why this exists

Two things landed earlier today that, together, make a long-standing convention worth revisiting.

First, five context-store tool callbacks were repointed from a deliberately-unguarded shared facade to
an `@PreAuthorize("hasAuthority(\"ROLE_ADMIN\")")`-guarded API facade, and the management MCP surface
was made to authenticate by default so a real principal exists to authorize.

Second, an investigation established that the authenticated `SecurityContext` **does** reach the tool
execution thread on the management MCP surface. There is exactly one thread boundary —
`McpToolUtils.toAsyncToolSpecification` wrapping the callback in
`Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` — and it is crossed because
`ReactorContextPropagationConfiguration` calls `Hooks.enableAutomaticContextPropagation()` and
spring-security auto-registers `SecurityContextHolderThreadLocalAccessor`.

That second fact matters because the API-facade/shared-facade convention rests on a premise:

> The shared facade is deliberately unguarded because runtime agent tools call it with no security
> context.

For agent tools reached over HTTP, that premise is **false today**. They have a principal. The
convention is preserving a workaround for a problem that has since been solved on that path.

## The real boundary

The premise is not wrong everywhere — it is wrong *as stated*. Looking at who actually consumes the
shared facades:

- The shared `AiSkillFacade`'s consumers include `AiAgentUtilsCreateAiSkillAction`,
  `AiAgentUtilsUpdateAiSkillAction`, `AiAgentUtilsDeleteAiSkillAction`, `AiAgentUtilsSkillsTool` —
  **workflow component actions**, running on the Atlas worker during job execution.
- The shared `WorkspaceContextStoreSourceFacade`'s only non-trivial consumer is
  `ContextStoreToolCallbacksFactory`, whose tools run on **HTTP-originated** threads.

So the boundary that matters is not API-vs-shared. It is:

| Caller kind | Origin | Has a principal? | Treatment |
|---|---|---|---|
| **Request-scoped** — GraphQL, REST, MCP tools, AI Hub, Copilot panels | an authenticated HTTP request | yes (proven for MCP; rehydrated for the hub catalog path) | guard normally |
| **Job-scoped** — workflow component actions on the worker | a dispatched job | no — there was never a request | explicit skip |

For a job-scoped caller, authorization already happened: a human authored the workflow and
**published** it. Re-authorizing at run time would be checking the wrong principal at the wrong
moment — the workflow's author is not the caller, and often nobody is.

That is a defensible security model, and stating it explicitly is half the value of this design.

## The model

**One facade per domain, guarded.** Two expression forms, both already present in
`AutomationMethodSecurityExpressionRoot` / `AutomationPermissionEvaluator`:

- by-id operations → `hasPermission(id, "<ResourceType>", "<SCOPE>")`
- create and list operations, which have a workspace but no resource id →
  `hasWorkspaceScope(workspaceId, "<SCOPE>")`

`hasPermission` resolves through `PermissionService.hasResourceScope`, in this order:

1. `isSkipChecks()` → allow
2. `isTenantAdmin()` → allow (`SecurityUtils.hasCurrentUserThisAuthority(ADMIN)` — the *same*
   authority `hasAuthority("ROLE_ADMIN")` reads, so admins behave identically before and after)
3. visibility precondition
4. **`ResourceOwnershipResolver` lookup — absent ⇒ `false`**
5. resolve the owning workspace, check the workspace scope

Step 4 is the trap this design must not fall into: swapping the expression *without* registering a
resolver produces a gate that reads as delegable and is silently admin-only. That is the same
claim-versus-enforcement gap ticket 732 spent the day removing, relocated into the expression
language.

## Delete the shared interface — do not alias it

This is the load-bearing implementation choice.

Keeping the shared facade as a deprecated alias means a caller that should have been re-pointed keeps
compiling, keeps calling the unguarded path, and is discovered — if ever — in production. Deleting the
interface makes **every** call site a compile error, and forces its author to choose consciously:
guarded call, or `skipChecks`-wrapped job-scoped call.

That converts the migration's risk from runtime to compile time. It is the same reasoning behind
`IntelligentToolSurfaceParityTest` iterating the catalog rather than a hardcoded name list: make the
compiler or a test enumerate the surface, never a human's memory.

## `skipChecks()` goes at the job seam, once

`AutomationAuthorizationContext.skipChecks()` should be applied where the worker establishes task
execution — one place — not scattered across component actions. Scattering it produces exactly the
audit problem the chokepoint exists to solve; a single seam is grep-auditable, which is the whole
argument for `hasPermission` over `hasAuthority` (the latter cannot consult `isSkipChecks` at all).

Requirement: the seam must be **narrow**. It covers job-dispatched execution only. It must not be
widened to "all agent tools", which would disable authorization on the request-scoped paths that this
design is specifically enabling.

## Scope tokens

Eleven families already exist as `*PermissionScope` + `*PermissionScopeProvider` pairs (AiGateway,
ApiKey, Connection, DataTable, Deployment, Execution, KnowledgeBase, Mcp, Project, Workflow,
Workspace). Context store has none and needs one — `KnowledgeBasePermissionScope` is the closest
analogue, being a similarly-shaped EE data domain.

Tokens follow the established `<DOMAIN>_<VERB>` shape (`CONNECTION_EDIT`, `PROJECT_DELETE`,
`WORKFLOW_VIEW`). The view/edit split is the existing norm; adopt it rather than inventing a
finer-grained vocabulary before there is demand for one.

## Rollout

**Phase 1 — management MCP (the pilot).** Zero new infrastructure: five `ResourceOwnershipResolver`s
already exist (`McpServer`, `McpProject`, `McpProjectWorkflow`, `McpComponent`, `McpTool`),
`McpPermissionScope` already defines `MCP_VIEW` / `MCP_CREATE` / `MCP_EDIT`, and no workflow component
action touches the MCP facades — so there are no job-scoped callers and **no `skipChecks` seam is
needed**. The phase is therefore a pure expression swap plus a facade collapse, which is exactly the
pair of things under test, with nothing else varying.

**Phase 2 — context store.** Adds one new variable: a domain that is *not* pre-wired. Register
`ContextStoreSourceOwnershipResolver` (~20 lines; `WorkspaceContextStoreSourceService
.fetchWorkspaceIdByContextStoreSourceId` already returns exactly the `Optional<Long>` shape
`McpServerOwnershipResolver` consumes) and add a `ContextStorePermissionScope` family. Its consumers
are request-scoped too, so still no seam — this phase proves the pattern extends to a domain that has
to build its own resolver and tokens.

A pleasing detail: the workspace lookup removed from the context-store callbacks on 2026-08-18,
because it ran *before* `@PreAuthorize` and leaked resource existence to non-admins, is the very
lookup the resolver needs. Same query, moved inside the guard, where it is safe.

**Phase 3 — AI Skill.** The phase that proves the `skipChecks` seam, because it has genuine
job-scoped callers in the `AiAgentUtils*` component actions. It is the gate on whether the pattern
generalises beyond request-scoped surfaces.

**Phase 4 — the remaining domains.** 58 files still carry `hasAuthority(ADMIN)` against 73 already on
`hasPermission`.

Each phase introduces exactly one new variable: Phase 1 the expressions and the collapse, Phase 2 a
domain that must build its own resolver, Phase 3 the seam. A phase that fails tells you which of the
three is at fault.

Do not begin Phase 2 before Phase 1 has run against a live backend. See below.

## Failure modes

- **Resolver missing, expression swapped.** Gate reads delegable, is admin-only. Mitigation: a test
  asserting every resource type named in a `hasPermission` gate has a registered
  `ResourceOwnershipResolver`. This is mechanically checkable and should be written in Phase 1.
- **`skipChecks` seam too wide.** Silently disables authorization on request-scoped paths. Mitigation:
  the seam is one method; assert in test that it is not entered on a request-scoped call.
- **A job-scoped caller missed during collapse.** Fails closed at runtime. Mitigation: delete the
  interface (above) so it cannot compile.
- **`ai-mcp-server`'s implicit dependency.** Propagation works because
  `ReactorContextPropagationConfiguration` lives in `platform-configuration-service`, which
  `ai-mcp-server` does **not** declare — it depends on `platform-configuration-api`. It holds only
  because `server-app` is the sole host. This repo's own convention is that distributed EE apps carry
  `*-api` + `*-remote-client` *without* `*-service`; host the management MCP server in such an app and
  every guarded tool denies everyone, including admins, fail-closed and invisible. A unit-test pin is
  already queued; this design **depends** on that guarantee and should not ship without it.

## Verification debt to clear first

No integration test has run in the originating session — Testcontainers cannot reach the Docker socket
from the sandboxed JVM — and nothing has run against a live backend. Every claim here is compile-,
unit-test-, or read-verified.

That is adequate for a design. It is **not** adequate for shipping an authorization change: the
enforcement path is proven by reading, not by observing a request actually challenged and a non-admin
actually denied. Phase 1 must include an end-to-end check against a running backend — an admin
principal succeeding and a non-admin being denied, on both a request-scoped and a job-scoped path —
before Phase 2 begins.
