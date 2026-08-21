# AI Hub centralized audit emitter

**Status:** Design
**Author:** Ivica Cardic
**Date:** 2026-05-18
**Scope:** EE (Enterprise Edition); `automation-ai-hub` module

## Problem

AI Hub has no audit trail. Personal-agent creation/deletion, schedule
upserts, scheduled fires, workspace-settings changes — none of them leave
a persistent forensic record. Operators investigating "who deleted my
agent" or compliance teams reviewing "who can spin up an agent that runs
unattended" have nothing to grep.

Meanwhile, the platform already has a working centralized audit
subsystem (`platform-audit`, EE) with:

- `persistent_audit_event` table (id, principal, event_date, event_type,
  data: Map<String,String>).
- `AuditEventService.save(...)` with `Propagation.REQUIRES_NEW`.
- `PersistenceAuditEventRepository` registered as Spring Boot Actuator's
  `AuditEventRepository`, so any `AuditApplicationEvent` published into
  the `ApplicationEventPublisher` flows through.
- `AuditEventGraphQlController` exposing `auditEvents(principal,
  eventType, fromDate, toDate, dataSearch, page, size)` plus
  `auditEventTypes`.
- Frontend at `client/src/ee/pages/settings/platform/audit-events/`.
- Retention job `AuditEventRetentionJob`.

`platform-connection` is the working template for plugging a new emitter
into this bus:

- `ConnectionAuditPublisher` publishes `AuditApplicationEvent` via the
  Spring `ApplicationEventPublisher`.
- `@AuditConnection` annotation + `ConnectionAuditAspect` for declarative
  SpEL-based emission, with boot-time SpEL validation, `afterCommit`
  publish (so rolled-back transactions don't emit), `strictAudit` flag
  for compliance-grade events that must roll back on capture failure,
  `establishCorrelation` for umbrella events, and a
  `bytechef_connection_audit_failed` counter.

This spec adds an `AiHubAuditPublisher` + `@AuditAiHub` aspect that
mirrors that pattern, then annotates the AI Hub services that perform
material durable-state mutations.

## Non-goals (v1)

- Per-chat-turn audit events. Volume risk; route through metrics
  (`bytechef_ai_hub_*`).
- Tool-invocation audit during chat. Overlaps with the existing artifact
  lineage view; deferred to v2.
- Renaming `AiHubAuditPage.tsx` → `AiHubArtifactHistoryPage.tsx`.
  Separate follow-up; the term overlap with the new audit work creates
  confusion but the rename is out of scope here.
- Adding a `workspace_id` column to `persistent_audit_event`. v1 stashes
  the workspace in `data["workspaceId"]` so the shared table needs no
  schema change.
- Changing the EE GraphQL `auditEvents(...)` query. The existing
  `eventType` + `dataSearch` filters cover AI Hub events.
- Front-end changes. The existing audit-events settings page renders new
  event types automatically once they appear in the table.
- Audit for `AiHubPersonalAgentService.list` / `findOwned` reads. Audit
  only mutations.

## Architecture

### Module placement

Co-located under `automation-ai-hub-service`, in a new sub-package
`com.bytechef.ee.automation.aihub.audit`. No new Gradle module — the
file count is small (~6 files including tests) and there is no consumer
outside AI Hub that needs the audit API.

```
server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/
  src/main/java/com/bytechef/ee/automation/aihub/audit/
    AiHubAuditEvent.java         enum
    AiHubAuditPublisher.java     imperative publisher (Spring component)
    AuditAiHub.java              annotation
    AiHubAuditAspect.java        @Aspect bean
  src/test/java/com/bytechef/ee/automation/aihub/audit/
    AiHubAuditPublisherTest.java
    AiHubAuditAspectTest.java
    AiHubAuditAspectIntTest.java
```

The package lives in `-service` because the aspect mutates running
behavior and SpEL parsing requires Spring infrastructure. Pulling the
annotation + enum up to `-api` is a future refactor if a sibling EE
module needs to emit events without depending on the aspect — not
needed for v1.

### Event taxonomy (v1)

```java
public enum AiHubAuditEvent {
    AI_HUB_PERSONAL_AGENT_CREATED(false),
    AI_HUB_PERSONAL_AGENT_UPDATED(false),
    AI_HUB_PERSONAL_AGENT_DELETED(true),
    AI_HUB_PERSONAL_AGENT_TOOL_ADDED(false),
    AI_HUB_PERSONAL_AGENT_TOOL_REMOVED(false),
    AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED(false),
    AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED(false),
    AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED(false),
    AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED(false),
    AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED(false),
    AI_HUB_WORKSPACE_SETTINGS_UPDATED(false);

    private final boolean strictAudit;
    AiHubAuditEvent(boolean strictAudit) { this.strictAudit = strictAudit; }
    public boolean isStrictAudit() { return strictAudit; }
}
```

`strictAudit = true` means an SpEL evaluation failure during audit capture
rolls back the surrounding business transaction rather than silently
ticking the failure counter. For v1, only `AI_HUB_PERSONAL_AGENT_DELETED`
is strict — a deletion without a trail is the prototypical compliance
blind spot. Other events absorb capture failures into
`bytechef_ai_hub_audit_failed`.

### Payload contract

Every event carries `workspaceId` in `data`. Additional required keys
per event:

| Event | Additional `data` keys |
|---|---|
| `AI_HUB_PERSONAL_AGENT_CREATED` | `agentId`, `name`, `environment` |
| `AI_HUB_PERSONAL_AGENT_UPDATED` | `agentId` |
| `AI_HUB_PERSONAL_AGENT_DELETED` | `agentId` |
| `AI_HUB_PERSONAL_AGENT_TOOL_ADDED` | `agentId`, `componentName`, `componentVersion`, `operationName` |
| `AI_HUB_PERSONAL_AGENT_TOOL_REMOVED` | `toolId` |
| `AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED` | `agentId`, `toolId`, `connectionId` (nullable), `parameterKeys` (CSV) |
| `AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED` | `agentId`, `scheduleId`, `enabled`, `frequencyKind`, `effectiveCronExpression` |
| `AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED` | `agentId`, `scheduleId` |
| `AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED` | `agentId`, `scheduleId`, `taskId` |
| `AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED` | `agentId`, `scheduleId`, `reason` |
| `AI_HUB_WORKSPACE_SETTINGS_UPDATED` | `changedFields` (CSV, e.g. `voiceWebhookUrl` or `voiceProvider`) |

The contract is convention-enforced rather than type-checked — Javadoc
on each enum constant pins the expected keys, the same way
`ConnectionAuditEvent` does it.

### Annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditAiHub {
    AiHubAuditEvent event();
    AuditData[] data() default {};
    boolean establishCorrelation() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {
        String key();
        /** SpEL: {@code #paramName}, {@code #result}, {@code 'literal'}, etc. */
        String value();
    }
}
```

`@AuditAiHub` deliberately omits the first-class subject-id slot that
`@AuditConnection` has (`connectionId()`). AI Hub events don't share a
single subject — `AI_HUB_WORKSPACE_SETTINGS_UPDATED` has only the
workspace, `AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED` has both an agent and
a schedule id. Putting everything into `data()` keeps the annotation
flexible at the cost of one extra `AuditData` line per event.

### Publisher

```java
@Component
public class AiHubAuditPublisher {
    public void publish(AiHubAuditEvent event, Map<String, Object> data) { ... }
}
```

Mirrors `ConnectionAuditPublisher.publish(eventType, additionalData)`
exactly:
- Resolves principal via `SecurityUtils.fetchCurrentUserLogin()`, falls
  back to `"SYSTEM"` for unauthenticated callers (Quartz fire path).
- Builds an `org.springframework.boot.actuate.audit.AuditEvent`,
  publishes via `ApplicationEventPublisher` as
  `AuditApplicationEvent`.
- `data` map is mutable and may be `null` (treated as empty).
- Never throws; the caller's business transaction is never broken by
  audit emission failure.

The publisher is `@Component` (always-on); failures are absorbed
internally with a `logger.warn(...)` and a `bytechef_ai_hub_audit_failed`
counter tick.

### Aspect

`AiHubAuditAspect` mirrors `ConnectionAuditAspect` with these
differences:

- No `connectionId` SpEL slot to evaluate. SpEL evaluation only iterates
  `auditAiHub.data()`.
- Same boot-time SpEL validation via `@EventListener(ContextRefreshedEvent.class)`.
- Same `establishCorrelation` advice using the existing
  `AuditCorrelation` ThreadLocal from `platform-connection-api`. AI Hub
  re-uses the connection module's correlation utility — they live in
  the same JVM and the helper is generic. **Note:** this introduces a
  dependency from `automation-ai-hub-service` onto
  `platform-connection-api`, but that dependency already exists
  transitively (every EE app pulls in connections), so the explicit
  dep declaration is a small change.
- Same `@AfterReturning` advice with the
  `TransactionSynchronizationManager.isSynchronizationActive()` /
  `afterCommit` deferral.
- Same `strictAudit` handling: SpEL evaluation failure rethrows as
  `AuditCaptureFailedException` (the existing exception type from
  `platform-connection-api`) so the surrounding `@Transactional` rolls
  back. Non-strict events absorb the failure.
- `bytechef_ai_hub_audit_failed` counter, tagged
  `outcome={parse|evaluate|publish}` for triage. (One counter with a
  tag is cheaper than three counters at the cost of one extra
  meter-registry tag dimension.)
- `bytechef_ai_hub_audit_validation_skipped` counter, ticked at boot
  when a bean's `@AuditAiHub` methods can't be SpEL-validated due to
  bean resolution failure.

The aspect's `@Order` is `Ordered.HIGHEST_PRECEDENCE` (matching
`PermissionAuditAspect`) so it wraps `@PreAuthorize` security
interceptors — irrelevant here for AI Hub mutations (none are
`@PreAuthorize`-gated; ownership is service-internal), but consistent.

### Call-site map

**`@AuditAiHub`-annotated methods** (transactional, user-initiated):

```
AiHubPersonalAgentServiceImpl
  create(...)              → AI_HUB_PERSONAL_AGENT_CREATED
  update(...)              → AI_HUB_PERSONAL_AGENT_UPDATED
  delete(...)              → AI_HUB_PERSONAL_AGENT_DELETED   [STRICT]
  addTool(...)             → AI_HUB_PERSONAL_AGENT_TOOL_ADDED
  removeTool(...)          → AI_HUB_PERSONAL_AGENT_TOOL_REMOVED
  updateToolConfig(...)    → AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED

AiHubWorkspaceSettingsServiceImpl
  updateVoiceWebhookUrl(...) → AI_HUB_WORKSPACE_SETTINGS_UPDATED  data:[changedFields='voiceWebhookUrl']
  updateVoiceProvider(...)   → AI_HUB_WORKSPACE_SETTINGS_UPDATED  data:[changedFields='voiceProvider']
```

**Imperative `publisher.publish(...)` call sites** (the aspect can't
cover these because they need conditional-branch logic or run outside a
`@Transactional`):

```
AiHubPersonalAgentScheduleServiceImpl.upsertOrDelete
  - input == null + existing present  → AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED
  - input != null + existing present  → AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED (enabled = current)
  - input != null + absent            → AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED (enabled = new)
  (the "input null + absent" branch is silent — nothing happened)

AiHubPersonalAgentScheduleServiceImpl.recordFailure
  - in the three-strike branch only   → AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED

AgentScheduleFiredEventListener.onFired
  - on the success log line, after taskId is known
                                       → AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED
```

`upsertOrDelete` does not get an `@AuditAiHub` annotation because one
method emits three different event types depending on branch — the
aspect's single-event-per-method model doesn't fit. Imperative
publication inside the existing `@Transactional` is cleaner than
splitting the method.

`AgentScheduleFiredEventListener` is on the Quartz thread without a
SecurityContext (no user initiated the fire), so the publisher's
`SYSTEM` principal fallback applies. There's no surrounding
`@Transactional`, so no `afterCommit` deferral — the publish runs
immediately at the success log line.

### Failure modes considered

- **SpEL parse error.** Boot-time validator catches it; logs at ERROR
  with method + slot + expression; ticks
  `bytechef_ai_hub_audit_validation_skipped`. Application continues to
  boot.
- **SpEL evaluation error at runtime.** Non-strict event → counter +
  log + return; non-strict mutation commits. Strict event (only
  `AI_HUB_PERSONAL_AGENT_DELETED`) → rethrow as
  `AuditCaptureFailedException`; transaction rolls back; user sees a
  500.
- **Publisher throws.** Counter + log inside `publishSafely`; mutation
  has already committed (afterCommit), no business impact.
- **DB outage on `persistent_audit_event`.**
  `AuditEventService.save(...)` runs in a `REQUIRES_NEW` transaction; a
  Postgres error there is absorbed by the actuator listener and surfaces
  as a counter tick. The mutation in the outer transaction has already
  committed and is unaffected.
- **Missing SecurityContext on Quartz thread.** Publisher falls back to
  `"SYSTEM"` principal. The audit row is recorded; operators reading
  the event know it was system-initiated.
- **Correlation ID leak across threads.** `AuditCorrelation` is a
  ThreadLocal pushed/popped in try/finally; no async dispatch in the
  AI Hub annotated paths today, so leakage isn't possible. Documented
  as a constraint for future async work.

## Migration

No data migration. No schema change. The new table column work is
deferred to a future v2 spec.

## Rollout

EE-only behavior gated by edition (the audit module is already
`@ConditionalOnEEVersion`). The publisher + aspect ride along on the
same gate; CE deployments instantiate neither bean. No feature flag —
the surface is invisible to users unless they open the existing
audit-events settings page (already EE-only).

## Open questions

- **No `name` on `AI_HUB_PERSONAL_AGENT_DELETED`, no `changedFields` on `AI_HUB_PERSONAL_AGENT_UPDATED`, no `agentId` on `AI_HUB_PERSONAL_AGENT_TOOL_REMOVED`.** Three related limitations driven by the same constraint: the aspect's SpEL only sees method args + return value, and these three methods either don't return the entity (`removeTool` returns void) or don't expose the missing fact via their args (`update`'s diff is computed inside the method body; `delete`'s entity name is only loaded internally). v1 ships these payloads in their reduced form; the audit table preserves the agentId/toolId so investigators can still correlate. Revisit if compliance reviewers want diff-level or name-level granularity — three resolution paths exist: change the method signature to return the entity (small API churn), add a SpEL helper bean lookup (verbose annotation), or switch the aspect to `@Around` advice that captures pre-state (heavier).
- **Strictness of `AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED`.** Schedule
  deletion is a privilege-narrowing event (it removes a recurring
  surface that could have been auditable later). Argument for strict:
  consistency with personal-agent deletion. Argument against: schedules
  carry no credentials, the underlying agent's audit trail still
  exists. v1 keeps it non-strict; revisit if compliance feedback says
  otherwise.
- **Audit for the listener's failure branch.** The listener already
  calls `scheduleService.recordFailure(scheduleId)`, which on the third
  strike emits `AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED`. Should
  every failure (not just the auto-disable threshold) emit an event?
  v1 says no — failures are visible in
  `bytechef_ai_hub_agent_schedule_fire{outcome="failed"}`; audit-row
  granularity is reserved for state transitions, not error counters.
