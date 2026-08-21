# Open work after the 4750 follow-up batch — 2026-08-20

**Branch:** `0_732` (main checkout). 61 commits landed this day above `0d859c5ef75`.

What closed: a review over the preceding 20-commit batch (1 HIGH, 2 MEDIUM, 11 LOW — all fixed),
ten in-body controller gates moved onto facades, the `AGENT_*` scope family end-to-end, five
`Map.of` ordering defects, 61 Liquibase `MARK_RAN` rollback hazards, four Spring bean-name
collisions that were preventing the EE server from starting, and Ruby disabled across four surfaces.

Everything below was found during that work and deliberately **not** fixed at the time. Each item
states its evidence and a command to re-check it — figures are a snapshot, so verify before writing
code. Section G has since been partly closed: it now records what was fixed as well as what was not,
so read G1 before assuming an agent-visibility gap is still open.

---

## A. Controller `@PreAuthorize` annotations still bypass the facade layer

**46** controllers carry `@PreAuthorize`; **8** carry it at class level, gating every method at once.

    grep -rl '@PreAuthorize' server --include='*Controller.java' | grep -v test

(column-0 annotation = class level; indented = method level)

    ee/libs/ai/ai-hub/ai-hub-graphql/.../AiHubChatArtifactGraphQlController.java
    ee/libs/ai/ai-hub/ai-hub-graphql/.../AiHubChatGraphQlController.java
    ee/libs/ai/ai-hub/ai-hub-graphql/.../AiHubChatToolGraphQlController.java
    ee/libs/ai/ai-hub/ai-hub-graphql/.../AiHubMcpServerGraphQlController.java
    ee/libs/ai/ai-hub/ai-hub-rest/.../AiHubApiController.java
    ee/libs/automation/automation-ai/automation-ai-gateway/.../AiGatewayPlaygroundRestController.java
    libs/automation/automation-asset-file/automation-asset-file-rest/.../AssetFileRestController.java
    libs/platform/platform-ai/platform-ai-auto-memory/.../AiAutoMemoryGraphQlController.java

**Hazard:** `AiGatewayPlaygroundRestController`'s class-level `isAuthenticated()` was left in place
deliberately — removing it without first tracing the URL security configuration risks opening the
endpoint, since it may be the only thing authenticating that path.

**Lesson from the ten already moved:** three could not become annotations. One needed a denial
message naming the resolved workflow; one keyed on a rank check (`hasWorkspaceRole`) that no scope
token expresses; one guarded a row with no workspace relation rather than a request parameter.
*Gates belong on facades* is the rule; *gates are annotations* is not.

Pattern to copy: `ProjectDeploymentWorkflowGraphQlController#workspaceChatWorkflows` +
`ProjectDeploymentFacadeImpl#getWorkspaceChatWorkflows`, and the delegation test in
`ProjectDeploymentWorkflowGraphQlControllerAuthorizationTest`, which also asserts the raw services
are NOT touched.

---

## B. Component definition snapshots cannot be reproduced by the generator — highest value item here

The committed snapshots use `"key": value`; `JsonFileAssert` emits `"key" : value`. Regenerating any
snapshot therefore rewrites the whole file. Measured on `knowledgeBase_v1.json`: **1880 insertions,
1880 deletions** for a change that touched four lines, with a normalized-content comparison
confirming nothing but formatting and key order moved.

    # delete a snapshot, re-run its component test, diff
    git diff --stat -- '*_v1.json'

**Why it matters:** this is what let commit `0412711ece8` silently delete real data — nulled `help`,
flipped `customAction`, emptied `clusterElements` — in exactly 2 of 291 files. The real changes were
invisible inside formatting churn. Until the generator's pretty-printer matches the committed
format, no snapshot regeneration in this repo is reviewable.

`JsonFileAssert` only writes a file that does not exist, so "regenerate" means delete-and-rerun.

---

## C. GraalVM 25.2.4 vs TruffleRuby — worked around, not resolved

`gradle/libs.versions.toml` pins `graalvm = "25.2.4"`; `org.graalvm.polyglot:ruby` is published only
to **25.0.0** (metadata `lastUpdated` 20260818; 34.0.1 and 40.0.0 both 404 on Maven Central). One
classpath resolves one Truffle version, highest wins, so TruffleRuby 25.0.0 on Truffle 25.2.4 throws
`Invalid inline context node ... TryNodeGen expected but is RescueClassesNode` — **production Ruby
execution, every platform**, not just tests.

TruffleRuby itself has moved on: it now versions against CRuby (`AB.C.D` ↔ CRuby `A.B`), and 40.0.0
runs on Truffle 25.2.4. It ships as native/JVM **standalones**, not as the polyglot embedding jar.

Ruby was therefore disabled temporarily (grep `RUBY-DISABLED`). To restore, either a polyglot ruby
jar on Truffle 25.2+ appears, or `graalvm` drops to 25.0.x — which unpicks part of `07a3ccb8057`,
the commit that also brought Gradle 9.7.0.

**Residue left untouched:** 9 files under `docs/content` mention Ruby (user-facing); 2 javadoc lines
in `ApplicationProperties`; the generated GraphQL wire enum still carries `RUBY`, so a `RUBY`
request fails at the facade rather than at the schema.

---

## D. Liquibase rollback — two residual gaps

The `MARK_RAN` + auto-inverse hazard is fixed (61 changesets across 40 files) and guarded by
`LiquibaseMarkRanRollbackTest` + `LiquibaseMarkRanRollbackIntTest`. Remaining:

1. The static guard accepts **any** `<rollback>`, not only an empty one, so a hand-written
   destructive rollback would pass it. The integration test exists because of this gap.
2. **26 `createTable` init changesets can now never be rolled back.** Intended, but a real
   behavioural change worth knowing about.

Note that full rollback is not a working operation in this repo anyway — 40 changesets contain a
non-invertible change with no rollback.

---

## E. `getProjectRows()` is O(workspaces) authorization lookups on a whole-tenant scan

`ProjectFacadeImpl.getProjectRows()` reads every project row in the tenant with no predicate, then
makes one `hasWorkspaceScope` call per distinct workspace, then one batched visibility call.

A batch scope API was **considered and rejected**: it moves the loop inside `PermissionService`
rather than removing it (CE is `isAuthenticated()`; EE still does one cache entry and one
custom-role lookup per workspace), so its `times(1)` test would pin an API shape while asserting
nothing about work saved. The real cost is the whole-tenant scan, not the scope half.

---

## F. Listing and by-id disagreed about system projects — CLOSED in `196f96fd3c5`

Kept here for the reasoning, not as open work.

`getProjectRows()` filtered `SystemProjects.isSystemProject` and `getProjectRow(long)` did not, so a
caller holding a `__AI_AGENT__` project's id and the workspace scope could read it by id though it
appeared in no listing. Nothing secret leaked — the name is `__AI_AGENT__<uuid>`, the description
empty, and the gate refused a caller without `WORKFLOW_VIEW` either way. What was wrong was the
claim: the javadoc pair insisted the two answered the same question.

Resolved by making the by-id read answer as though a system project does not exist, throwing the
same bare `NoSuchElementException` that `ProjectService.getProject` raises for an absent id, so a
caller cannot distinguish "system project" from "nothing".

**Two things the fix needed beyond the filter, both worth knowing if you touch this area:**

The agreement loop in `ProjectFacadeRowVisibilityTest` measured `isByIdReadPermitted`, which
evaluates only the `@PreAuthorize` expression — and the filter lives in the method body, which a
unit test calling the facade directly reaches without crossing a security proxy. Adding the filter
alone would have left the loop reporting system projects as readable and the skip still necessary.
The predicate now composes both halves.

And the fixture stubbed only one id. An unstubbed Mockito mock returns `null`,
`SystemProjects.isSystemProject(null)` is `false`, so the filter never fired and the loop would have
passed while proving nothing about the case it exists for. With lenient mocks, "unstubbed" is itself
a branch, and a null-tolerant helper is what makes it invisible.

---

## G. Visibility governs management surfaces only — no runtime path consults it

Verified repo-wide: every consumer of visibility is a facade, GraphQL controller, search asset
provider, `PermissionService`, or a provider. Nothing under `atlas-*`, and zero visibility or
permission references in any webhook entry point.

    grep -rln "isResourceVisible\|ResourceVisibilityResolver\|filterVisibleIds\|ProjectVisibilityFilter" \
      server --include="*.java" | grep -v test

For projects this is arguably correct — a webhook URL *is* the credential, deliberately handed to
third parties, and coupling it to a sharing dropdown would let a teammate break a production
integration. For **agents** the same mechanism reads differently: a PRIVATE agent keeps answering
everyone in Slack and WhatsApp, because a channel is a room people are already in rather than an
unguessable URL. Agent visibility ships labelled for what it does ("who can **see** this agent");
making visibility gate channel serving would be a new capability, not an extension.

**The in-app chat launcher is not an exception to that, and was made to stop looking like one.**
`AiAgentFacadeImpl.getWorkspaceChatAgents` shipped unfiltered on the reading that in-app chat is
one more channel. It is now filtered through `visibleAgents`, matching its sibling cascade
`ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows`: the two are the two halves of one AI Hub
launcher popup, and what the popup does is name agents for a colleague to pick. Filtering a listing
is not switching a channel off — a withheld agent still answers Slack, WhatsApp and its webhooks
unchanged, and its hosted-chat trigger still answers anyone holding the URL.

### G1. Closed on 2026-08-20, in the five follow-up commits above

| gap | what changed |
|---|---|
| workspace admin could not re-share a withheld agent | `AiAgentSharingFacadeImpl`'s four gates gained `\|\| @permissionService.hasResourceRole(#agentId, 'AiAgent', 'ADMIN')`. Projects never had the hole: `ProjectSharingFacadeImpl` gates on ownership and workspace role, neither of which consults visibility. Agents were the anomaly because visibility is a precondition of every `'AiAgent'`-keyed gate and the resolver's admin bypass is TENANT admin. |
| `getWorkspaceChatAgents` unfiltered | see above |
| `getAgentTags` aggregated withheld agents' tag names | filtered through `visibleAgents`. Two siblings had the same leak and were fixed with it: `getProjectDeploymentTags` (now shares `filterOutSystemProjectDeployments`) and `ProjectTagFacadeImpl.getProjectTags` (now goes through `ProjectVisibilityFilter`). `getAgentDeploymentTags` was already filtered, via `getAgentDeployments`. |
| sharing audit records named the hidden project only | `ProjectAuditSubjectResolver` (new SPI in `automation-configuration-api`, discovered as a list like `ResourceOwnershipResolver`) lets the owning feature contribute `subjectType`/`subjectId`/`subjectName`. `AiAgentProjectAuditSubjectResolver` implements it with one `findByProjectId`. Still one record per question; the `projectId` stays on it. |
| no visibility badge on the agent list | `AgentListItem` renders `ResourceVisibilityBadge` over a `ResourceVisibilityPicker` dropdown, driven by the same `useAiAgentVisibility` the detail dialog uses. `aiAgents.graphql` now selects `visibility`. |

### G2. Still open

- **`WorkspaceConnectionFacadeImpl.getConnectionTags` has the tag leak too.** It aggregates over
  every connection in the workspace while its sibling `getConnections` filters through the
  connection-keyed `filterVisible`. Left alone deliberately: connections resolve visibility off
  `ConnectionDTO` rather than through `ProjectVisibilityFilter`, so it is the connection half of
  4750 rather than the project/agent half the batch above closed.

      grep -n "getConnectionTags" -A 12 \
        server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/WorkspaceConnectionFacadeImpl.java

- **A `PROJECT_CREATED` audit record for an agent's project still carries no subject.**
  `AiAgentFacadeImpl.createAgent` saves the project *before* the `ai_agent` row exists, so
  `findByProjectId` finds nothing at the moment the event is published. Harmless — the sharing
  events, which are the ones an auditor goes looking for, all fire long after — but it means the
  subject is not universal across the `ProjectAuditEvent` family.

- **`platform-api` is still an `implementation` dependency of `automation-ai-agent-api`** although
  `AiAgentDTO` exposes `ResourceVisibility` in its public signature. Every consumer declares
  `platform-api` itself, so it compiles; promoting it to `api` risks the dependency-analysis check.

---

## H. Two pre-existing test failures on darwin-aarch64

`ProjectHandlerLoaderTest.testLoadProjectHandlerFromJavaJar`,
`IntegrationHandlerLoaderTest.testLoadIntegrationHandlerFromJavaJar`, and
`ComponentHandlerEspressoEngineTest.testLoadAndExecuteAction` — GraalVM Espresso
`Object ... does not have the expected shape` on a second polyglot context. Fine on Linux; verified
pre-existing at `0d859c5ef75`. See [[espresso-sandbox-loaders]].

---

## I. Whole-repo `check` has not been run since roughly 30 commits ago

Deferred at the repo owner's request. Worth doing before this branch is considered green.

**Two traps, both hit repeatedly on 2026-08-20:**
- Background-task notifications reported *exit code 0* for runs that actually exited **1** — the
  notification reflects the last statement in the pipeline, not Gradle. Write `$?` to a file and
  read it back, and separately grep `^> Task .* FAILED`.
- Without `--continue`, Gradle stops at the first failing task. The first whole-repo run failed at
  `server-app:testIntegration` and never scheduled the two loader modules that were also red.
  Fixing one red task revealed two more.

A green per-module `check` says nothing about modules you did not name: 2803 of 3068 tasks were
up-to-date, which is how a two-day-old breakage in `server-app` stayed invisible.
