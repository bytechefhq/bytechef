# AI Hub Audit Emitter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an `AiHubAuditPublisher` + `@AuditAiHub` aspect that mirrors `@AuditConnection`, then annotate the AI Hub services that perform material durable-state mutations so each one writes a `persistent_audit_event` row through the existing centralized audit pipeline.

**Architecture:** A small audit sub-package under `automation-ai-hub-service` houses an `AiHubAuditEvent` enum, an `AiHubAuditPublisher` Spring bean, an `@AuditAiHub` annotation, and an `AiHubAuditAspect` that intercepts annotated methods, evaluates SpEL data expressions, and forwards into the publisher (which publishes a Spring Boot `AuditApplicationEvent` — already wired in EE to land in `persistent_audit_event`). The aspect re-uses `AuditCorrelation` and `AuditCaptureFailedException` from `platform-connection-api`. Quartz-thread emission (no SecurityContext, no `@Transactional`) goes through the imperative `publish(...)` path directly. Zero schema or GraphQL changes.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring AOP + AspectJ (already on classpath via the existing `org.aspectj:aspectjweaver` dep through `platform-connection-service`) / SpEL / Spring Boot Actuator audit bus / Postgres `persistent_audit_event` table.

**Spec:** `docs/superpowers/specs/2026-05-18-ai-hub-audit-emitter-design.md`

---

## File Structure

### Backend — create
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java` — the enum.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisher.java` — Spring component that publishes `AuditApplicationEvent`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AuditAiHub.java` — the annotation.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspect.java` — the aspect bean.

### Backend — edit
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts` — add `spring-aop` + `aspectjweaver` + `platform-connection-api` dependencies.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java` — add `@AuditAiHub` on six methods.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/workspacesettings/AiHubWorkspaceSettingsServiceImpl.java` — add `@AuditAiHub` on two methods.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java` — constructor-inject `AiHubAuditPublisher`; call `publish(...)` at the three exit branches of `upsertOrDelete` and in the three-strike branch of `recordFailure`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java` — constructor-inject `AiHubAuditPublisher`; call `publish(...)` after a successful fire.

### Backend — tests
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisherTest.java` — unit, covers SYSTEM-principal fallback + counter on publisher throw.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectTest.java` — unit, covers SpEL evaluation + correlation + strictAudit rethrow + afterCommit deferral via a mocked `ApplicationContext`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectIntTest.java` — Testcontainers; calls real service methods (personal-agent create + delete), asserts rows land in `persistent_audit_event` with the right event_type and `data["workspaceId"]`.

---

## Task ordering

Annotation + publisher + aspect first (Tasks 1–4), then test the wiring works against the existing audit pipeline (Task 5 — `AiHubAuditAspectIntTest`), then add `@AuditAiHub` annotations at the personal-agent and workspace-settings call sites (Tasks 6–7), then wire up the imperative emissions at the schedule + listener sites (Tasks 8–9). Final `./gradlew check` gate (Task 10).

---

### Task 1: Add Gradle dependencies for AOP + audit-correlation utility

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts`

- [ ] **Step 1: Add the three new dependencies**

Locate the `dependencies {` block and add (in alphabetical order with the existing entries):

```kotlin
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springframework:spring-aop")
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
```

`spring-aop` + `aspectjweaver` give us `@Aspect` runtime weaving. `platform-connection-api` exposes `AuditCorrelation` (ThreadLocal helper) and `AuditCaptureFailedException` (the strict-audit rollback signal) — both reused by the new aspect.

- [ ] **Step 2: Verify the module still compiles**

Run:
```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts
git commit -m "$(cat <<'EOF'
- Add AOP + connection-audit-api deps for AI Hub audit emitter

Pulls in spring-aop + aspectjweaver for the upcoming @AuditAiHub aspect,
plus platform-connection-api for the AuditCorrelation / AuditCaptureFailedException
utilities reused from the connection module.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Add `AiHubAuditEvent` enum

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java`

- [ ] **Step 1: Write the enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

/**
 * Audit event types emitted through {@link AiHubAuditPublisher} for material durable-state mutations on AI Hub
 * resources. Every event carries {@code workspaceId} in its payload; additional required keys are documented per
 * constant. The payload contract is convention-enforced — changes must be applied at every emitter.
 *
 * <p>
 * {@code strictAudit = true} means an SpEL-evaluation failure during {@link AiHubAuditAspect} capture rolls back the
 * surrounding business transaction rather than absorbing the failure into the counter. Reserved for compliance-grade
 * events where a missing trail is itself a regression.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubAuditEvent {

    /**
     * A personal agent was persisted. Payload: {@code workspaceId}, {@code agentId} ({@code #result.id}),
     * {@code name} ({@code #result.name}), {@code environment}.
     */
    AI_HUB_PERSONAL_AGENT_CREATED(false),

    /**
     * An existing personal agent's editable fields were updated. Payload: {@code workspaceId}, {@code agentId}.
     * Diff-level granularity (which fields changed) is intentionally omitted in v1 — the {@code update} method's
     * conditional patch logic isn't reachable from the aspect's arg/return SpEL context.
     */
    AI_HUB_PERSONAL_AGENT_UPDATED(false),

    /**
     * A personal agent was deleted. Payload: {@code workspaceId}, {@code agentId}.
     *
     * <p>
     * Marked {@code strictAudit} — deletion without a trail is the prototypical compliance blind spot. SpEL evaluation
     * failure here rethrows {@code AuditCaptureFailedException} so the surrounding {@code @Transactional} rolls back
     * rather than the agent disappearing without an audit row.
     */
    AI_HUB_PERSONAL_AGENT_DELETED(true),

    /**
     * A tool template was attached to an agent. Payload: {@code workspaceId}, {@code agentId}, {@code componentName},
     * {@code componentVersion}, {@code operationName}.
     */
    AI_HUB_PERSONAL_AGENT_TOOL_ADDED(false),

    /**
     * A tool template was detached from an agent. Payload: {@code workspaceId}, {@code toolId}. The parent {@code
     * agentId} is intentionally omitted in v1 — {@code removeTool} returns {@code void} and the row is gone by
     * {@code @AfterReturning} time, so the aspect cannot reach it. Audit consumers correlate via {@code toolId}.
     */
    AI_HUB_PERSONAL_AGENT_TOOL_REMOVED(false),

    /**
     * A tool template's pinned connection or pre-set parameters were updated. Payload: {@code workspaceId},
     * {@code agentId} ({@code #result.aiHubPersonalAgentId}), {@code toolId}, {@code connectionId} (nullable),
     * {@code parameterKeys} (string rendering of the parameter map's key set, e.g. {@code "[a, b, c]"}).
     */
    AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED(false),

    /**
     * A schedule was inserted or updated. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId},
     * {@code enabled}, {@code frequencyKind}, {@code effectiveCronExpression}. Emitted imperatively from
     * {@code AiHubPersonalAgentScheduleServiceImpl.upsertOrDelete} (not via the aspect) because one method emits two
     * different event types depending on the input-null branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED(false),

    /**
     * A schedule was removed. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId}. Emitted imperatively
     * from {@code upsertOrDelete}'s delete branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED(false),

    /**
     * A scheduled fire produced a new task. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId},
     * {@code taskId}. Emitted from {@code AgentScheduleFiredEventListener.onFired} on the Quartz thread; principal
     * falls back to {@code "SYSTEM"} via {@link AiHubAuditPublisher}.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED(false),

    /**
     * A schedule was auto-disabled after three consecutive failures. Payload: {@code workspaceId}, {@code agentId},
     * {@code scheduleId}, {@code reason} (currently always {@code "three_consecutive_failures"}). Emitted imperatively
     * from {@code AiHubPersonalAgentScheduleServiceImpl.recordFailure} in the threshold branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED(false),

    /**
     * Workspace-level AI Hub settings changed. Payload: {@code workspaceId}, {@code changedFields} (the literal name
     * of the changed field group, e.g. {@code "voiceWebhookUrl"} or {@code "voiceProvider"}).
     */
    AI_HUB_WORKSPACE_SETTINGS_UPDATED(false);

    private final boolean strictAudit;

    AiHubAuditEvent(boolean strictAudit) {
        this.strictAudit = strictAudit;
    }

    public boolean isStrictAudit() {
        return strictAudit;
    }
}
```

- [ ] **Step 2: Compile**

Run:
```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditEvent.java
git commit -m "$(cat <<'EOF'
- Add AiHubAuditEvent enum with payload contracts

Eleven event types covering personal-agent CRUD, tool-template CRUD,
schedule lifecycle (upsert/delete/fired/auto-disabled), and workspace
settings updates. Per-constant Javadoc pins the payload keys each
emitter must produce.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Add `AiHubAuditPublisher` + unit test

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisher.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisherTest.java`

- [ ] **Step 1: Write the publisher**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes AI Hub audit events through Spring Boot's actuator audit bus. Mirrors {@code ConnectionAuditPublisher} —
 * the Spring Boot listener already wired in EE picks the {@link AuditApplicationEvent} up and persists it via
 * {@code CustomAuditEventRepository} into {@code persistent_audit_event}.
 *
 * <p>
 * Failures absorb silently into {@code bytechef_ai_hub_audit_failed} + a warn log; emission must never break the
 * just-succeeded business transaction. Unauthenticated callers (Quartz scheduled-fire thread) get the {@code "SYSTEM"}
 * principal fallback so the row still records who would have been credited if a user had initiated the action.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AiHubAuditPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AiHubAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable Counter auditFailureCounter;

    @SuppressFBWarnings({"EI", "CT_CONSTRUCTOR_THROW"})
    public AiHubAuditPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.auditFailureCounter = meterRegistry == null ? null : Counter.builder("bytechef_ai_hub_audit_failed")
            .description(
                "AI Hub audit events that failed to publish. Non-zero values indicate a gap in the audit trail.")
            .register(meterRegistry);
    }

    public void publish(AiHubAuditEvent event, @Nullable Map<String, Object> data) {
        try {
            String principal;

            try {
                principal = SecurityUtils.fetchCurrentUserLogin()
                    .orElse("SYSTEM");
            } catch (RuntimeException securityException) {
                logger.warn(
                    "Could not resolve principal for audit event {}, using SYSTEM",
                    event, securityException);

                principal = "SYSTEM";
            }

            Map<String, Object> dataCopy = new HashMap<>();

            if (data != null) {
                dataCopy.putAll(data);
            }

            AuditEvent auditEvent = new AuditEvent(principal, event.name(), dataCopy);

            applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (Exception exception) {
            // Catch Exception (not Throwable): Error subtypes (OOM, StackOverflowError) must propagate per the JVM
            // contract. Audit emission must never throw out to the caller — the business transaction has already
            // committed by the time publishers run (afterCommit path) or is irrelevant to the audit attempt (Quartz
            // path). Drift is observable via bytechef_ai_hub_audit_failed.
            if (auditFailureCounter != null) {
                auditFailureCounter.increment();
            }

            logger.error(
                "Failed to publish AI Hub audit event {} (data={})",
                event, data, exception);
        }
    }
}
```

- [ ] **Step 2: Write the publisher unit test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAuditPublisherTest {

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AiHubAuditPublisher newPublisher() {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        return new AiHubAuditPublisher(applicationEventPublisher, provider);
    }

    @Test
    void testPublishesAuditApplicationEventWithSystemPrincipalWhenUnauthenticated() {
        AiHubAuditPublisher publisher = newPublisher();

        publisher.publish(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED, Map.of("scheduleId", "42"));

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        AuditEvent auditEvent = captor.getValue()
            .getAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(auditEvent.getType()).isEqualTo("AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED");
        assertThat(auditEvent.getData()).containsEntry("scheduleId", "42");
    }

    @Test
    void testNullDataIsTreatedAsEmpty() {
        AiHubAuditPublisher publisher = newPublisher();

        publisher.publish(AiHubAuditEvent.AI_HUB_WORKSPACE_SETTINGS_UPDATED, null);

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()
            .getAuditEvent()
            .getData()).isEmpty();
    }

    @Test
    void testPublisherFailureTicksCounter() {
        AiHubAuditPublisher publisher = newPublisher();

        doThrow(new RuntimeException("downstream blew up")).when(applicationEventPublisher)
            .publishEvent(any(AuditApplicationEvent.class));

        publisher.publish(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED, Map.of("workspaceId", "1"));

        assertThat(meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter()
            .count()).isEqualTo(1.0);
    }
}
```

- [ ] **Step 3: Run test**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AiHubAuditPublisherTest"
```

Expected: 3 tests, all pass.

- [ ] **Step 4: Commit**

```bash
git add \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisher.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditPublisherTest.java
git commit -m "$(cat <<'EOF'
- Add AiHubAuditPublisher with SYSTEM-principal fallback + failure counter

Mirrors ConnectionAuditPublisher: resolves principal via SecurityUtils,
falls back to SYSTEM for unauthenticated callers (Quartz path), publishes
via ApplicationEventPublisher as AuditApplicationEvent so Spring Boot
Actuator's listener persists into persistent_audit_event. Publisher
failure absorbs into bytechef_ai_hub_audit_failed counter.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Add `@AuditAiHub` annotation + `AiHubAuditAspect` + aspect unit test

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AuditAiHub.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspect.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectTest.java`

- [ ] **Step 1: Write the annotation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative AI Hub audit annotation. Methods annotated with this publish an {@link AiHubAuditEvent} after
 * successful completion. SpEL expressions in {@link AuditData#value()} are evaluated against method parameters
 * ({@code #paramName}) and the return value ({@code #result}). Unlike {@code @AuditConnection}, this annotation has
 * no first-class subject-id slot — AI Hub events don't share a single subject, so everything goes through {@link
 * #data()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditAiHub {

    AiHubAuditEvent event();

    AuditData[] data() default {};

    /**
     * When {@code true}, the aspect opens an {@code AuditCorrelation} scope around the method so nested audited calls
     * inherit the same correlation ID under {@code data["correlationId"]}. Reserved for umbrella facade methods; the
     * annotation default is {@code false}.
     */
    boolean establishCorrelation() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface AuditData {

        String key();

        /**
         * SpEL expression. Examples: {@code "#workspaceId"}, {@code "#result.name"}, {@code "'voiceWebhookUrl'"}.
         */
        String value();
    }
}
```

- [ ] **Step 2: Write the aspect**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.audit.AuditCaptureFailedException;
import com.bytechef.platform.connection.audit.AuditCorrelation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aspect that intercepts {@link AuditAiHub}-annotated methods, evaluates the data SpEL, and publishes an
 * {@link AiHubAuditEvent} via {@link AiHubAuditPublisher}. Mirrors {@code ConnectionAuditAspect}: boot-time SpEL
 * validation, {@code afterCommit} publish (so rolled-back transactions don't emit), {@code strictAudit} rethrow on
 * capture failure, {@code establishCorrelation} ThreadLocal scope.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Aspect
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
@SuppressFBWarnings({"CT_CONSTRUCTOR_THROW", "SPEL_INJECTION"})
public class AiHubAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AiHubAuditAspect.class);

    private final ApplicationContext applicationContext;
    private final AiHubAuditPublisher publisher;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final @Nullable MeterRegistry meterRegistry;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @SuppressFBWarnings("EI")
    public AiHubAuditAspect(
        ApplicationContext applicationContext, AiHubAuditPublisher publisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationContext = applicationContext;
        this.publisher = publisher;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * Boot-time validation of every {@code @AuditAiHub} SpEL expression in the context. Parse failures are logged at
     * ERROR with the offending method so a typo surfaces at startup rather than as a runtime miss the first time the
     * method is invoked. Implementation note: annotations are read off the bean <em>class</em> (not the instantiated
     * bean) to avoid forcing-instantiation of every lazy bean in the context.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void validateAuditAnnotations() {
        int checked = 0;
        int failed = 0;
        int skipped = 0;

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType;

            try {
                beanType = applicationContext.getType(beanName);
            } catch (RuntimeException typeLookup) {
                skipped++;

                recordValidationSkipped();

                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Skipping @AuditAiHub validation for bean '{}': {}",
                        beanName, typeLookup.getClass()
                            .getSimpleName(),
                        typeLookup);
                }

                continue;
            }

            if (beanType == null) {
                continue;
            }

            Class<?> targetClass = beanType.getName()
                .contains("$$SpringCGLIB$$") ? beanType.getSuperclass() : beanType;

            if (targetClass == null) {
                continue;
            }

            for (Method method : targetClass.getDeclaredMethods()) {
                AuditAiHub annotation = method.getAnnotation(AuditAiHub.class);

                if (annotation == null) {
                    continue;
                }

                checked++;

                for (AuditAiHub.AuditData auditData : annotation.data()) {
                    failed += validateExpression(targetClass, method, auditData.value());
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info(
                "AiHubAuditAspect validated {} @AuditAiHub-annotated method(s); {} SpEL parse failure(s); "
                    + "{} bean(s) skipped due to resolution failure",
                checked, failed, skipped);
        }
    }

    private int validateExpression(Class<?> targetClass, Method method, String expression) {
        try {
            expressionParser.parseExpression(expression);

            return 0;
        } catch (ParseException parseException) {
            logger.error(
                "@AuditAiHub SpEL parse failure on {}#{} expression='{}'",
                targetClass.getName(), method.getName(), expression, parseException);

            return 1;
        }
    }

    @Around("@annotation(auditAiHub)")
    public Object establishCorrelation(ProceedingJoinPoint joinPoint, AuditAiHub auditAiHub) throws Throwable {
        if (!auditAiHub.establishCorrelation()) {
            return joinPoint.proceed();
        }

        AuditCorrelation.CorrelationId previous = AuditCorrelation.push(AuditCorrelation.newId());

        try {
            return joinPoint.proceed();
        } finally {
            AuditCorrelation.pop(previous);
        }
    }

    @AfterReturning(pointcut = "@annotation(auditAiHub)", returning = "result")
    public void audit(JoinPoint joinPoint, AuditAiHub auditAiHub, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        EvaluationContext evaluationContext = buildEvaluationContext(method, joinPoint.getArgs(), result);

        Map<String, Object> data;

        try {
            data = evaluateAuditData(auditAiHub.data(), evaluationContext);
        } catch (Exception exception) {
            recordAuditFailure();

            logger.error(
                "Failed to evaluate audit event {} for method {}",
                auditAiHub.event(),
                joinPoint.getSignature()
                    .toShortString(),
                exception);

            if (auditAiHub.event()
                .isStrictAudit()) {
                throw new AuditCaptureFailedException(
                    "Strict audit capture failed for event " + auditAiHub.event()
                        + "; rolling back the mutation rather than committing without a trail",
                    exception);
            }

            return;
        }

        AiHubAuditEvent eventType = auditAiHub.event();
        Map<String, Object> resolvedData = data;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publish(eventType, resolvedData);
                }
            });
        } else {
            publisher.publish(eventType, resolvedData);
        }
    }

    private void recordAuditFailure() {
        if (meterRegistry != null) {
            Counter.builder("bytechef_ai_hub_audit_failed")
                .register(meterRegistry)
                .increment();
        }
    }

    private void recordValidationSkipped() {
        if (meterRegistry != null) {
            Counter.builder("bytechef_ai_hub_audit_validation_skipped")
                .description(
                    "Beans whose @AuditAiHub SpEL could not be validated at boot because the bean failed to "
                        + "resolve during context refresh")
                .register(meterRegistry)
                .increment();
        }
    }

    private EvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setBeanResolver(new BeanFactoryResolver(applicationContext));

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        context.setVariable("result", result);

        return context;
    }

    private Map<String, Object> evaluateAuditData(
        AuditAiHub.AuditData[] auditDataEntries, EvaluationContext evaluationContext) {

        Map<String, Object> data = new HashMap<>();

        for (AuditAiHub.AuditData entry : auditDataEntries) {
            Object value = expressionParser.parseExpression(entry.value())
                .getValue(evaluationContext);

            data.put(entry.key(), value != null ? value.toString() : "null");
        }

        String correlationId = AuditCorrelation.current();

        if (correlationId != null) {
            data.putIfAbsent("correlationId", correlationId);
        }

        return data;
    }
}
```

- [ ] **Step 3: Write the aspect unit test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.connection.audit.AuditCaptureFailedException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAuditAspectTest {

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final AiHubAuditPublisher publisher = mock(AiHubAuditPublisher.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AiHubAuditAspect newAspect() {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        return new AiHubAuditAspect(applicationContext, publisher, provider);
    }

    @Test
    void testAuditEvaluatesSpelDataAndPublishes() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED, false,
            entry("workspaceId", "#workspaceId"),
            entry("agentName", "#result.toString()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "create", new Class[]{long.class, String.class},
            new Object[]{42L, "ignored"});

        aspect.audit(joinPoint, annotation, "ResearchBot");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(publisher).publish(eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED), dataCaptor.capture());

        assertThat(dataCaptor.getValue())
            .containsEntry("workspaceId", "42")
            .containsEntry("agentName", "ResearchBot");
    }

    @Test
    void testStrictAuditRethrowsOnEvaluationFailure() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_DELETED, false,
            entry("workspaceId", "#nonexistent.boom()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "delete", new Class[]{long.class},
            new Object[]{42L});

        assertThatThrownBy(() -> aspect.audit(joinPoint, annotation, null))
            .isInstanceOf(AuditCaptureFailedException.class);

        assertThat(meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void testNonStrictAuditAbsorbsEvaluationFailure() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED, false,
            entry("workspaceId", "#nonexistent.boom()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "update", new Class[]{long.class},
            new Object[]{42L});

        aspect.audit(joinPoint, annotation, null);

        verify(publisher, org.mockito.Mockito.never()).publish(any(), any());

        assertThat(meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter()
            .count()).isEqualTo(1.0);
    }

    private JoinPoint joinPointFor(Class<?> type, String name, Class<?>[] paramTypes, Object[] args)
        throws NoSuchMethodException {

        Method method = type.getDeclaredMethod(name, paramTypes);
        MethodSignature methodSignature = mock(MethodSignature.class);

        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.toShortString()).thenReturn(type.getSimpleName() + "." + name);

        JoinPoint joinPoint = mock(JoinPoint.class);

        when(joinPoint.getSignature()).thenReturn((Signature) methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    private AuditAiHub annotation(
        AiHubAuditEvent event, boolean establishCorrelation, AuditAiHub.AuditData... data) {

        AuditAiHub annotation = mock(AuditAiHub.class);

        when(annotation.event()).thenReturn(event);
        when(annotation.establishCorrelation()).thenReturn(establishCorrelation);
        when(annotation.data()).thenReturn(data);

        return annotation;
    }

    private AuditAiHub.AuditData entry(String key, String expression) {
        AuditAiHub.AuditData entry = mock(AuditAiHub.AuditData.class);

        when(entry.key()).thenReturn(key);
        when(entry.value()).thenReturn(expression);

        return entry;
    }

    @SuppressWarnings("unused")
    private static final class ExampleService {

        String create(long workspaceId, String name) {
            return name;
        }

        void delete(long agentId) {
        }

        void update(long agentId) {
        }
    }
}
```

- [ ] **Step 4: Run aspect test**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AiHubAuditAspectTest"
```

Expected: 3 tests, all pass.

- [ ] **Step 5: Commit**

```bash
git add \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AuditAiHub.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspect.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectTest.java
git commit -m "$(cat <<'EOF'
- Add @AuditAiHub annotation + AiHubAuditAspect

The aspect mirrors ConnectionAuditAspect: boot-time SpEL validation,
@AfterReturning advice with afterCommit transaction deferral, strictAudit
rethrow on capture failure, establishCorrelation for umbrella scopes.
Reuses AuditCorrelation + AuditCaptureFailedException from
platform-connection-api.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Integration test — aspect-driven event lands in `persistent_audit_event`

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectIntTest.java`

- [ ] **Step 1: Write the integration test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that an {@code @AuditAiHub}-annotated method, when called inside a Spring transaction, lands a row in
 * {@code persistent_audit_event} after the transaction commits.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubAuditAspectIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubAuditAspectIntTest {

    @Autowired
    private AuditedBean auditedBean;

    @Autowired
    private PersistenceAuditEventRepository auditEventRepository;

    @AfterEach
    public void afterEach() {
        auditEventRepository.deleteAll();
    }

    @Test
    public void testAnnotatedMethodLandsAuditRow() {
        auditedBean.simulateUpdate(7L, 99L);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                List<PersistentAuditEvent> events = auditEventRepository.findAll();

                assertThat(events).hasSize(1);

                PersistentAuditEvent event = events.get(0);

                assertThat(event.getEventType()).isEqualTo("AI_HUB_PERSONAL_AGENT_UPDATED");
                assertThat(event.getData())
                    .containsEntry("workspaceId", "7")
                    .containsEntry("agentId", "99");
            });
    }

    @org.springframework.stereotype.Component
    @Transactional
    static class AuditedBean {

        @AuditAiHub(
            event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED,
            data = {
                @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
                @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
            })
        public String simulateUpdate(long workspaceId, long agentId) {
            return "ok";
        }
    }

    @EnableAutoConfiguration
    @ComponentScan(basePackages = {
        "com.bytechef.ee.automation.aihub.audit",
        "com.bytechef.ee.platform.audit"
    })
    @Import({
        LiquibaseConfiguration.class,
        AiHubAuditAspectIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
```

- [ ] **Step 2: Confirm `awaitility` is on the classpath**

The aspect's `afterCommit` callback fires asynchronously; the test uses Awaitility to wait. Check the dependency:

```
grep -nE "awaitility" /Volumes/Data/bytechef/bytechef/server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts
```

If absent, add to the test deps in `build.gradle.kts`:
```kotlin
    testImplementation("org.awaitility:awaitility")
```

- [ ] **Step 3: Run**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:testIntegration --tests "AiHubAuditAspectIntTest"
```

Expected: BUILD SUCCESSFUL, 1 test passes (Testcontainers Postgres startup adds ~30s).

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/audit/AiHubAuditAspectIntTest.java
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts
git commit -m "$(cat <<'EOF'
- Pin AI Hub audit aspect end-to-end with Testcontainers int test

Verifies an @AuditAiHub-annotated method's emission lands a row in
persistent_audit_event with the expected event_type and SpEL-evaluated
data payload, exercising the full path through ApplicationEventPublisher
+ Spring Boot Actuator listener + CustomAuditEventRepository.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Annotate `AiHubPersonalAgentServiceImpl` (6 methods)

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java`

- [ ] **Step 1: Add import for the annotation**

Insert near the other `com.bytechef.ee.*` imports:
```java
import com.bytechef.ee.automation.aihub.audit.AiHubAuditEvent;
import com.bytechef.ee.automation.aihub.audit.AuditAiHub;
```

- [ ] **Step 2: Annotate `create`**

Above the existing `@Override @Transactional public AiHubPersonalAgent create(...)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#result.id"),
            @AuditAiHub.AuditData(key = "name", value = "#result.name"),
            @AuditAiHub.AuditData(key = "environment", value = "#environment")
        })
```

- [ ] **Step 3: Annotate `update`**

Above `public AiHubPersonalAgent update(...)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
        })
```

- [ ] **Step 4: Annotate `delete`**

Above `public void delete(long agentId, long workspaceId, long userId)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_DELETED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
        })
```

(Strict-audit event — SpEL evaluation failure on this method's expressions will roll back the delete transaction.)

- [ ] **Step 5: Annotate `addTool`**

Above `public AiHubPersonalAgentTool addTool(...)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_ADDED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId"),
            @AuditAiHub.AuditData(key = "componentName", value = "#componentName"),
            @AuditAiHub.AuditData(key = "componentVersion", value = "#componentVersion"),
            @AuditAiHub.AuditData(key = "operationName", value = "#operationName")
        })
```

- [ ] **Step 6: Annotate `removeTool`**

Above `public void removeTool(long toolId, long workspaceId, long userId)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_REMOVED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "toolId", value = "#toolId")
        })
```

- [ ] **Step 7: Annotate `updateToolConfig`**

Above `public AiHubPersonalAgentTool updateToolConfig(...)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#result.aiHubPersonalAgentId"),
            @AuditAiHub.AuditData(key = "toolId", value = "#toolId"),
            @AuditAiHub.AuditData(key = "connectionId", value = "#connectionId"),
            @AuditAiHub.AuditData(key = "parameterKeys", value = "#parameters == null ? '[]' : #parameters.keySet().toString()")
        })
```

The `parameterKeys` SpEL handles the null case (the method permits a null `parameters` argument meaning "preserve existing").

- [ ] **Step 8: Verify compilation + existing tests still pass**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AiHubPersonalAgentServiceTest"
```

Expected: existing tests still BUILD SUCCESSFUL (the annotation has no behavioral effect when the aspect isn't wired — and they're unit tests so the aspect isn't running anyway).

- [ ] **Step 9: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentServiceImpl.java
git commit -m "$(cat <<'EOF'
- Annotate AiHubPersonalAgentServiceImpl with @AuditAiHub on 6 methods

create/update/delete/addTool/removeTool/updateToolConfig now publish
their respective AI_HUB_PERSONAL_AGENT_* audit events when called inside
an active Spring transaction. The delete event is strictAudit — SpEL
capture failure rolls back the delete itself.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Annotate `AiHubWorkspaceSettingsServiceImpl` (2 methods)

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/workspacesettings/AiHubWorkspaceSettingsServiceImpl.java`

- [ ] **Step 1: Add import**

```java
import com.bytechef.ee.automation.aihub.audit.AiHubAuditEvent;
import com.bytechef.ee.automation.aihub.audit.AuditAiHub;
```

- [ ] **Step 2: Annotate `updateVoiceWebhookUrl`**

Above `public AiHubWorkspaceSettings updateVoiceWebhookUrl(long workspaceId, @Nullable String voiceWebhookUrl)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_WORKSPACE_SETTINGS_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "changedFields", value = "'voiceWebhookUrl'")
        })
```

- [ ] **Step 3: Annotate `updateVoiceProvider`**

Above `public AiHubWorkspaceSettings updateVoiceProvider(...)`:

```java
    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_WORKSPACE_SETTINGS_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "changedFields", value = "'voiceProvider'")
        })
```

- [ ] **Step 4: Verify the module still compiles**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/workspacesettings/AiHubWorkspaceSettingsServiceImpl.java
git commit -m "$(cat <<'EOF'
- Annotate AiHubWorkspaceSettingsServiceImpl with @AuditAiHub

Both update methods publish AI_HUB_WORKSPACE_SETTINGS_UPDATED with
changedFields literal indicating which voice field group was touched.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Wire imperative emissions into `AiHubPersonalAgentScheduleServiceImpl`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`

- [ ] **Step 1: Add imports**

```java
import com.bytechef.ee.automation.aihub.audit.AiHubAuditEvent;
import com.bytechef.ee.automation.aihub.audit.AiHubAuditPublisher;
import java.util.Map;
```

- [ ] **Step 2: Inject the publisher**

Add to the field block:
```java
    private final AiHubAuditPublisher auditPublisher;
```

Update the constructor signature + body:
```java
    @SuppressFBWarnings({
        "EI2", "CT"
    })
    public AiHubPersonalAgentScheduleServiceImpl(
        AiHubPersonalAgentScheduleRepository repository, AgentScheduler agentScheduler,
        AiHubAuditPublisher auditPublisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.repository = repository;
        this.agentScheduler = agentScheduler;
        this.auditPublisher = auditPublisher;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }
```

- [ ] **Step 3: Emit `AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED` in the delete branch of `upsertOrDelete`**

Find the existing branch:
```java
if (input == null) {
    existing.ifPresent(row -> delete(row.getId(), workspaceId, userId));

    return null;
}
```

Replace with:
```java
if (input == null) {
    existing.ifPresent(row -> {
        delete(row.getId(), workspaceId, userId);

        auditPublisher.publish(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED,
            Map.of(
                "workspaceId", String.valueOf(workspaceId),
                "agentId", String.valueOf(agentId),
                "scheduleId", String.valueOf(row.getId())));
    });

    return null;
}
```

- [ ] **Step 4: Emit `AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED` in the update + insert branches**

After `return update(row);` add (refactor so we capture the saved row):

```java
    if (existing.isPresent()) {
        AiHubPersonalAgentSchedule row = existing.get();

        if (row.getWorkspaceId() != workspaceId || row.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule for agent " + agentId + " not owned by caller");
        }

        applyInput(row, input);

        AiHubPersonalAgentSchedule saved = update(row);

        publishUpsertedEvent(saved, workspaceId, agentId);

        return saved;
    }

    AiHubPersonalAgentSchedule row = new AiHubPersonalAgentSchedule();

    row.setAiHubPersonalAgentId(agentId);
    row.setWorkspaceId(workspaceId);
    row.setUserId(userId);
    row.setEnvironment(environment);

    applyInput(row, input);

    try {
        AiHubPersonalAgentSchedule saved = create(row);

        publishUpsertedEvent(saved, workspaceId, agentId);

        return saved;
    } catch (DataIntegrityViolationException e) {
        throw new IllegalStateException(
            "Schedule already exists for agent " + agentId + " (concurrent create)", e);
    }
}

private void publishUpsertedEvent(AiHubPersonalAgentSchedule saved, long workspaceId, long agentId) {
    auditPublisher.publish(
        AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED,
        Map.of(
            "workspaceId", String.valueOf(workspaceId),
            "agentId", String.valueOf(agentId),
            "scheduleId", String.valueOf(saved.getId()),
            "enabled", String.valueOf(saved.isEnabled()),
            "frequencyKind", saved.getFrequencyKind().name(),
            "effectiveCronExpression", saved.getEffectiveCronExpression()));
}
```

- [ ] **Step 5: Emit `AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED` in the three-strike branch of `recordFailure`**

Find:
```java
if (schedule.getConsecutiveFailures() >= FAILURE_DISABLE_THRESHOLD) {
    schedule.setEnabled(false);
    schedule.setNextRunAt(null);

    agentScheduler.cancelAgentRun(scheduleId);
}
```

Replace with:
```java
if (schedule.getConsecutiveFailures() >= FAILURE_DISABLE_THRESHOLD) {
    schedule.setEnabled(false);
    schedule.setNextRunAt(null);

    agentScheduler.cancelAgentRun(scheduleId);

    auditPublisher.publish(
        AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED,
        Map.of(
            "workspaceId", String.valueOf(schedule.getWorkspaceId()),
            "agentId", String.valueOf(schedule.getAiHubPersonalAgentId()),
            "scheduleId", String.valueOf(scheduleId),
            "reason", "three_consecutive_failures"));
}
```

- [ ] **Step 6: Update the service-impl test to pass the new constructor arg**

Find `AiHubPersonalAgentScheduleServiceImplTest`. Add a `@Mock AiHubAuditPublisher auditPublisher;` field (or however the test wires mocks — mirror the existing `@Mock` for `agentScheduler`). Update the `service = new AiHubPersonalAgentScheduleServiceImpl(...)` constructor call (or @InjectMocks setup) to include the publisher.

If the test uses `@InjectMocks`, just adding the `@Mock` field is enough.

- [ ] **Step 7: Run the service-impl test**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AiHubPersonalAgentScheduleServiceImplTest"
```

Expected: 13 tests still pass. Then add new tests asserting the publisher was called on:
- successful upsert (insert path)
- successful upsert (update path)
- successful delete
- three-strike auto-disable

Append to the test class:

```java
    @Test
    void testUpsertOrDeleteInsertEmitsUpsertedAuditEvent() {
        given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.empty());
        given(repository.save(any(AiHubPersonalAgentSchedule.class)))
            .willAnswer(invocation -> {
                AiHubPersonalAgentSchedule row = invocation.getArgument(0);
                row.setId(123L);
                return row;
            });

        ScheduleInput input = new ScheduleInput(
            true, "Title", "Prompt",
            ScheduleFrequencyKind.DAILY, null, null,
            LocalTime.of(9, 0), null, null, null,
            "UTC", null, ScheduleLifecycleKind.RECURRING, null);

        service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED),
            argThat(data ->
                "1".equals(data.get("workspaceId"))
                    && "42".equals(data.get("agentId"))
                    && "123".equals(data.get("scheduleId"))));
    }

    @Test
    void testUpsertOrDeleteDeleteEmitsDeletedAuditEvent() {
        AiHubPersonalAgentSchedule existing = sampleSchedule();
        existing.setId(99L);

        given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.of(existing));

        service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED),
            argThat(data ->
                "1".equals(data.get("workspaceId"))
                    && "42".equals(data.get("agentId"))
                    && "99".equals(data.get("scheduleId"))));
    }

    @Test
    void testRecordFailureThreeStrikesEmitsAutoDisabledAuditEvent() {
        AiHubPersonalAgentSchedule schedule = sampleSchedule();
        schedule.setId(99L);
        schedule.setConsecutiveFailures(2);  // next call brings it to 3

        given(repository.findById(99L)).willReturn(Optional.of(schedule));
        given(repository.save(any(AiHubPersonalAgentSchedule.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.recordFailure(99L);

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED),
            argThat(data -> "three_consecutive_failures".equals(data.get("reason"))));
    }
```

Adjust the existing `sampleSchedule()` helper if needed so it sets `workspaceId=1L`, `userId=10L`, `aiHubPersonalAgentId=42L` (matching the upsert test args).

- [ ] **Step 8: Run again**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AiHubPersonalAgentScheduleServiceImplTest"
```

Expected: 16 tests pass.

- [ ] **Step 9: Commit**

```bash
git add \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java
git commit -m "$(cat <<'EOF'
- Emit AI Hub schedule audit events from service impl

upsertOrDelete fires SCHEDULE_UPSERTED (insert + update branches) or
SCHEDULE_DELETED. recordFailure fires SCHEDULE_AUTO_DISABLED in the
three-strike branch. Imperative publication (not via the aspect)
because one method emits two distinct event types depending on input
branch.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: Wire imperative emission into `AgentScheduleFiredEventListener`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java`

- [ ] **Step 1: Add imports + inject publisher**

```java
import com.bytechef.ee.automation.aihub.audit.AiHubAuditEvent;
import com.bytechef.ee.automation.aihub.audit.AiHubAuditPublisher;
import java.util.Map;
```

Add `private final AiHubAuditPublisher auditPublisher;` to the field block and extend the constructor:

```java
    AgentScheduleFiredEventListener(
        AiHubPersonalAgentScheduleService scheduleService, AiHubTaskService taskService,
        AiHubScheduledChatDispatcher dispatcher, AiHubAuditPublisher auditPublisher) {

        this.scheduleService = scheduleService;
        this.taskService = taskService;
        this.dispatcher = dispatcher;
        this.auditPublisher = auditPublisher;
    }
```

- [ ] **Step 2: Emit on success**

In the existing `onFired` method, replace the success block:

```java
            scheduleService.recordFire(scheduleId);

            logger.info(
                "Scheduled fire ok: scheduleId={}, agentId={}, taskId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), task.getId());
```

with:

```java
            scheduleService.recordFire(scheduleId);

            logger.info(
                "Scheduled fire ok: scheduleId={}, agentId={}, taskId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), task.getId());

            auditPublisher.publish(
                AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED,
                Map.of(
                    "workspaceId", String.valueOf(schedule.getWorkspaceId()),
                    "agentId", String.valueOf(schedule.getAiHubPersonalAgentId()),
                    "scheduleId", String.valueOf(scheduleId),
                    "taskId", String.valueOf(task.getId())));
```

- [ ] **Step 3: Update the listener test**

If `AgentScheduleFiredEventListenerTest` exists (per the v1 plan it does), add a `@Mock AiHubAuditPublisher auditPublisher;` field and pass it into the listener constructor.

Add a test asserting the publisher is called on success:

```java
    @Test
    void testOnFiredSuccessPublishesAuditEvent() {
        // ... existing happy-path arrangement ...

        listener.onFired(new AgentScheduleFiredEvent(scheduleId));

        verify(auditPublisher).publish(
            eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED),
            argThat(data -> data.get("taskId") != null));
    }
```

- [ ] **Step 4: Run**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "AgentScheduleFiredEventListenerTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListenerTest.java
git commit -m "$(cat <<'EOF'
- Emit AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED on successful Quartz fire

Listener publishes the audit event after the task is created and the
fire is recorded. Principal falls back to SYSTEM via AiHubAuditPublisher
because Quartz threads have no SecurityContext.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: Spotless + `./gradlew check` + fix every issue

**Files:** none (whatever Spotless rewrites)

- [ ] **Step 1: Apply Spotless**

```
./gradlew spotlessApply
```

Expected: BUILD SUCCESSFUL. May rewrite formatting on the new audit files.

- [ ] **Step 2: Stage + commit Spotless rewrites if any**

```
git status --short
```

If Spotless changed files, commit:

```bash
git add -u
git commit -m "$(cat <<'EOF'
- Apply Spotless formatting to AI Hub audit emitter

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 3: Run the full check**

```
./gradlew check
```

This runs every project's `check` task (Spotless verify, PMD, SpotBugs, Checkstyle, unit tests). Expected: BUILD SUCCESSFUL.

If it fails, fix the issues per category:
- **Spotless violation**: re-run `./gradlew spotlessApply` and commit.
- **PMD `UnusedFormalParameter`**: typically an interface-required parameter; suppress with `@SuppressWarnings("PMD.UnusedFormalParameter")` or remove if genuinely unused.
- **PMD `UnusedImports`**: remove the import.
- **SpotBugs `EI`/`EI2`**: add `@SuppressFBWarnings("EI")` or `@SuppressFBWarnings("EI2")` on the constructor.
- **SpotBugs `CT_CONSTRUCTOR_THROW`**: add `@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")` if the constructor genuinely needs to be able to throw (e.g., dep with `ObjectProvider.getIfAvailable()`).
- **Checkstyle test method naming**: rename `test_*` → `test*`.
- **Compilation errors in unrelated modules** (e.g. `RemoteProjectWorkflowServiceClient` if regressed): out of scope; flag and ask for help.

Iterate `./gradlew check` until BUILD SUCCESSFUL.

- [ ] **Step 4: Commit fix-up changes (one commit per category)**

For each category of fix:
```bash
git add <fixed files>
git commit -m "- Fix <category> violations in AI Hub audit emitter

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 5: Final sanity — run the int test once more**

```
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:testIntegration --tests "AiHubAuditAspectIntTest"
```

Expected: BUILD SUCCESSFUL, 1 test passes.

---

## Self-review checklist

- **Spec coverage**: Every spec section maps to a task — annotation surface (Task 4), publisher (Task 3), aspect with SpEL+correlation+strict+afterCommit (Task 4), boot-time validation (Task 4), event taxonomy (Task 2), payload contract (Tasks 6, 7, 8, 9), int test for end-to-end pipeline (Task 5), Quartz path (Task 9), metrics (Tasks 3, 4). Failure modes from the spec are exercised by the publisher test (publisher throw), aspect test (strict rethrow + non-strict absorb), and int test (end-to-end persistence).
- **Placeholders**: none. Every step shows the code or command.
- **Type consistency**: `AiHubAuditEvent.AI_HUB_*` enum values are referenced verbatim across Tasks 6, 7, 8, 9. The annotation type `AuditAiHub` is used uniformly. The publisher signature `publish(AiHubAuditEvent, @Nullable Map<String, Object>)` is consistent across Tasks 3, 4, 8, 9. The aspect's reused utilities `AuditCorrelation` and `AuditCaptureFailedException` are explicitly imported from `com.bytechef.platform.connection.audit` and the dependency added in Task 1.
