# Component Rules (EE): agent tool governance

`server/ee/libs/platform/platform-component-rule/` (`-api`, `-service`, `-graphql`). The settings page lives at
**Settings → Current Workspace → Agent Rules** (`ai/rules` among the workspace settings routes,
`client/src/ee/pages/settings/automation/ai/rules/`). The route is admin-only and EE-only; there is no read-only
workspace-member view.

## What a rule governs

A `component_rule` row governs one **AI agent tool call**, not a workflow action execution. It is keyed on
`(componentName, toolName, workspaceId)`, where `toolName` is the name of a TOOLS cluster element. A `null`
`toolName` means every tool of that component, and a `null` `workspaceId` means every workspace in the tenant.
Enforcement never touches `ActionDefinitionServiceImpl`: a workflow node that is not inside an agent's TOOLS list
is never evaluated against any rule.

Each row also carries `phase` (`BEFORE`/`AFTER`, relative to the tool running), `ruleAction`
(`BLOCK`/`TAG`/`REQUIRE_APPROVAL`), a `condition`, `enabled` and `strict`. `BLOCK` and `REQUIRE_APPROVAL` are
rejected at save time in the `AFTER` phase, since the tool has already run by then.

## Where enforcement happens

`ComponentRuleEnforcer` (`platform-component-api`, no CE implementation) is injected as a
`List<ComponentRuleEnforcer>` into the `aiAgent` and `aiAgentUtils` component handlers. Each passes the list to
the `ClusterElementToolCallbacks` it builds, and `build()` wraps every callback it produces in a
`RuleEnforcingToolCallback` (`server/libs/modules/components/ai/llm/.../tool/`). That covers:

- the agent's own TOOLS (`AbstractAiAgentChatAction`, chat and streaming chat),
- tools nested in the approval gate (`AiAgentUtilsApprovalGate`),
- a subagent's tools (`AiAgentUtilsTaskTool`).

**Not governed: the Embabel `agentic-ai` component.** Its facade builds its own callbacks and never goes through
`ClusterElementToolCallbacks`, so a rule against a component that agent calls never fires, with no audit event.

**An empty enforcer list is a silent pass-through.** `build()` returns the unwrapped callbacks when the list is
empty, which is what CE does by design. Whenever the wiring of these handlers changes (a new app hosting the
agent action, DI changes), re-check that the list is non-empty there. `WorkerApplicationIntTest
.testComponentRuleEnforcerIsReachable` is the cheap guard for worker-app.

**An enforcer exception allows the call.** `RuleEnforcingToolCallback` catches a per-enforcer `RuntimeException`,
logs it, and proceeds. A `strict` rule does not survive an enforcer that throws.

## The wrapper is deliberately NOT a `DelegatingToolCallback`

The approval gate's resume branch in `AbstractAiAgentChatAction` rebuilds the callbacks and unwraps them with
`DelegatingToolCallback.unwrap` to strip the gate before re-running the approved call. `unwrap` is recursive: if
the rule layer were delegating, an approved re-execution would run with no rule check at all. Do not make it one.

The flip side: `build()` skips a callback that is already governed
(`DelegatingToolCallback.unwrap(toolCallback) instanceof RuleEnforcingToolCallback`). The gate is itself a TOOLS
element, so its callbacks pass through `build()` twice. Wrapping the second time put a rule layer outside the
gate, `unwrap` became a no-op, and every approved gated call raised the approval again, forever.
`ClusterElementToolCallbacksTest` pins the skip.

## Precedence and the approval protocol

`ComponentRuleEnforcerImpl.checkBeforeCall` applies matching rules in a fixed order: any `BLOCK` wins, else any
`REQUIRE_APPROVAL` wins (carrying every matching rule's id), else matching `TAG` rules are recorded and the call
proceeds. A block returns a JSON denial (`{"blocked": true, "reason": ...}`) to the model instead of throwing, so
the agent can explain and replan.

`REQUIRE_APPROVAL` reuses the gate's suspend flow through `ToolApprovalRequests.raise(...)`, the helper
`ApprovalGateToolCallback` now delegates to as well, so both share one set of semantics:

- In the editor environment it returns `EDITOR_ENVIRONMENT_RESULT` and does not suspend.
- Delivery goes to the agent node's `APPROVAL_CHANNELS`, or to the chat channel when none are configured. Each
  channel is best-effort; the request fails only when every channel fails. There is no approval-task backstop on
  this branch, so a webhook- or schedule-triggered run with no channels has no listener for the chat channel.
- When another tool already suspended this round, the call returns `DEFERRED_RESULT`.

The suspend payload carries the rule ids under `ToolSuspendConstants.RULE_IDS`, `RULE_COMPONENT_NAME` and
`RULE_TOOL_NAME`. On resume, `AbstractAiAgentChatAction` reads them back and calls
`ClusterElementToolCallbacks.recordRuleApprovalResolution(...)`, which delegates to the static
`RuleEnforcingToolCallback.recordApprovalResolution` so the agent module never depends on the EE rule service.
The approved call is re-run with `AiAgentToolContext.APPROVED_BY_KEY` in its `ToolContext`.

`approvedBy` is what stops a loop: the enforcer raises `REQUIRE_APPROVAL` only when `approvedBy` is null. The
hosted approval form has no verified reviewer, so the resume branch substitutes `"anonymous"` rather than
leaving it null.

## Workspace scoping

Enforcement for a call is the **union** of the workspace's own rules and the tenant-wide ones
(`ComponentRuleRepository.findAllEnabledForWorkspace`: `workspace_id IS NULL OR workspace_id = :workspaceId`).
Workspace rules do not override tenant rules.

The enforcer resolves the workspace through `JobPrincipalWorkspaceResolver`
(`server/ee/libs/platform/platform-ai/platform-ai-workspace`): `(PlatformType, jobPrincipalId)` → project
deployment → project workspace. **Any resolution failure narrows to tenant-wide rules**, never to none and never
to another workspace's: a non-automation run, a null principal, a deleted deployment, or a lookup error all
resolve to `null`, which is exactly the tenant-wide query. `recordRuleApprovalResolution` passes no principal
because it only replays rule ids it already holds; do not add a resolution there.

The GraphQL `componentRules` listing filters in memory through `ComponentRule.appliesToWorkspace`. That
predicate and the repository's SQL must agree; if you change one, change the other.

## Observe mode and strict

`ComponentRuleSettings` is one `Property` row per scope, key `component_rule_settings`: the tenant default at
`Scope.PLATFORM` and an optional override at `Scope.WORKSPACE`. `getSettings(workspaceId)` falls back workspace →
tenant → `ComponentRuleSettings.DEFAULT` (`observeMode = false`, `approvalExpiresInHours = 1440`).

With `observeMode` on, every rule is still evaluated, but a would-be `BLOCK` or `REQUIRE_APPROVAL` is published as
`RULE_OBSERVED` and the call proceeds. That lets a tenant, or one workspace, roll rules out without a mis-authored
rule taking agents offline.

`strict` is per-rule and only governs an **unevaluable** condition (an unresolved reference or an evaluator
exception). A non-strict rule treats it as no match (fails open); a strict rule treats it as a match (fails
closed). Audit payloads carry `strictFallback` so a reviewer can tell a strict fallback from a real match.

## The condition is a formula body, not free SpEL

The stored `condition` is the text after the `=` prefix that `SpelEvaluator` requires before it parses full SpEL.
Both save-time validation and runtime evaluation prepend the `=`; it is never stored.
`validateFormulaExpression` rejects `T(`, any `.method(` call and `new`, so conditions use the whitelisted
evaluator functions: `contains(inputParameters['channel'], 'C05')`, never
`inputParameters['channel'].startsWith('C05')`. The context exposes `inputParameters`, `componentName`,
`toolName`, `toolCallName`, `connectionId`, and `output` in the `AFTER` phase.

## Caches

`ComponentRuleEnforcerImpl` caches enabled rules per `(tenantId, componentName, workspaceId)` and settings per
`(tenantId, workspaceId)`, each for 10 seconds. Component names are global across tenants, so a key without the
tenant would serve one tenant's rules to another; the workspace is in the key for the same reason. A component
with no matching rules returns `ALLOW` before any evaluation context is built. A rule or settings edit takes up
to 10 seconds to take effect.

## Authorization

Every GraphQL mutation and the service's write methods carry `@PreAuthorize` for `ROLE_ADMIN`. Two finer checks
are deliberately absent and become necessary only if a non-admin can ever reach this surface: a tenant-admin
check for tenant-wide rules (it belongs on the resolved target `workspaceId`, since clearing a rule's workspace
widens it as much as creating a tenant-wide one), and per-workspace management scope.

## Risk levels are advisory only

`ToolRiskLevelResolver` (`platform-component-api`) returns the risk level a tool's author declared, or infers one
from its name (`deleteX` → `CRITICAL`, `sendX` → `HIGH`, otherwise `MEDIUM`, never a false "safe"). It only helps
an admin choose which tools need a rule. The enforcer never reads it.

## Worker-app and distributed deployments

`platform-component-rule-service` is on worker-app's classpath so the enforcer bean exists where agents run. Two
of its dependencies cannot be satisfied there, and both degrade instead of failing startup:

- `ComponentRuleSettingsServiceImpl.getSettings()` catches the remote property client's
  `UnsupportedOperationException` and returns `ComponentRuleSettings.DEFAULT`.
- `ComponentRuleServiceImpl` takes `ObjectProvider<ComponentRuleRepository>`. With no local database,
  `getEnabledComponentRules()` fails open to an empty list with a once-per-JVM warning; the authoring methods
  throw `IllegalStateException`.

**The residual gap:** on a distributed EE deployment the worker-side enforcer finds zero rules for every
component, so every tool call there is allowed, `strict` `BLOCK` rules included. Closing it needs a
`component-rule-remote-client` and an endpoint on the app that owns the data. Audit events published from
worker-app have the same gap.

## Not on this branch

The 0_732 version of this feature also has AI authoring tools and a Copilot rule-builder agent, an approval-task
backstop channel, a `RULE_BLOCKED` tool-invocation log event, and a tabbed Settings → AI → Agents page. None of
those are here.
