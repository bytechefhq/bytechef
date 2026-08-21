# Personal Agent — single schedule per agent: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the v1 multi-schedule subsystem to one schedule per personal agent, exposed inline on a dedicated **Schedule** tab in the agent create/edit form. Replaces the v1 list view + dialog + four CRUD mutations with a single `setAiHubPersonalAgentSchedule` upsert/delete mutation and an inline tab editor.

**Architecture:** Keep the `ai_hub_personal_agent_schedule` table (renaming its agent-id index to a UNIQUE constraint), keep all v1 Quartz wiring + the `ScheduleCronNormalizer` + the listener + metrics, and rewrite only the GraphQL surface (single `setAiHubPersonalAgentSchedule` mutation, `schedule` field on `AiHubPersonalAgent` payload) and the React surface (delete dialog + list + 6 graphql operations; add a `AiHubPersonalAgentScheduleTab` rendered inline). The v1 init migration is edited in place since the v1 spec is unreleased on `0_732`.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring Data JDBC / Postgres / Quartz on the backend; React 19 / TypeScript / GraphQL codegen / Vitest on the frontend; Liquibase XML for schema; Testcontainers for the new unique-constraint integration test.

**Spec:** `docs/superpowers/specs/2026-05-18-personal-agent-single-schedule-design.md`
**Supersedes (UI only — backend infrastructure mostly reused):** `docs/superpowers/plans/2026-05-16-personal-agent-scheduling.md`

---

## File Structure

### Backend — edit
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml` — drop the simple agent-id index, add UNIQUE constraint on `ai_hub_personal_agent_id`.
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java` — drop `findByAgent(long)` and `toggle(...)`; add `findByAgentId(long)` and `upsertOrDelete(...)`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java` — implement the two new methods; delete the two removed methods.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java` — add `findByAiHubPersonalAgentId(long)`; drop `findByAiHubPersonalAgentIdOrderByCreatedDateDesc(long)`.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls` — strip mutations + queries + the two input types; keep only the output type + enums.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls` — add `schedule: AiHubPersonalAgentSchedule` field on agent; add `SetAiHubPersonalAgentScheduleInput`, `AiHubPersonalAgentScheduleInput`, and `setAiHubPersonalAgentSchedule` mutation.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java` — delete v1 mutations/queries; add `setAiHubPersonalAgentSchedule` mutation and a `@SchemaMapping(typeName = "AiHubPersonalAgent", field = "schedule")` resolver.

### Backend — tests
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java` — replace multi-schedule assertions with upsert/delete coverage.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleUniqueConstraintIntTest.java` — **new**: verifies the UNIQUE constraint rejects a second insert for the same agent.
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/test/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlControllerTest.java` — drop tests for the four removed mutations + two removed queries; add `setAiHubPersonalAgentSchedule` upsert/delete tests and a `schedule` field resolver test.

### Frontend — delete
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts`
- `client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedules.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/createAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/updateAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/deleteAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai/aihub/personal-agent/schedule/toggleAiHubPersonalAgentSchedule.graphql`

### Frontend — edit
- `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx` — rename Schedules tab to Schedule, show it in both create + edit modes, replace the list with `<AiHubPersonalAgentScheduleTab>`, extend create + update handlers with the optional `setAiHubPersonalAgentSchedule` step.
- `client/src/graphql/ai/aihub/personal-agent/aiHubPersonalAgent.graphql` — extend selection set with the `schedule { ... }` field block.

### Frontend — new
- `client/src/graphql/ai/aihub/personal-agent/schedule/setAiHubPersonalAgentSchedule.graphql` — the single mutation.
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.tsx` — the inline editor.
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.test.tsx` — Vitest coverage.
- `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedule.ts` — single mutation hook.

### Frontend — kept (no change)
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleFrequencyFields.tsx`

---

## Task ordering

Backend before frontend (GraphQL surface must exist before codegen). Within the backend, schema → service → repository → controller. Tests sit next to the code they cover so each task is independently committable.

---

### Task 1: Add UNIQUE constraint to schedule table (Liquibase, edit in place)

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml`

- [ ] **Step 1: Drop the simple agent-id index, replace with a UNIQUE constraint**

Find the `<createIndex>` block named `idx_ai_hub_personal_agent_schedule_agent` and replace it with an `<addUniqueConstraint>`:

```xml
        <addUniqueConstraint tableName="ai_hub_personal_agent_schedule"
                             columnNames="ai_hub_personal_agent_id"
                             constraintName="uniq_ai_hub_personal_agent_schedule_agent"/>
```

Leave the `idx_ai_hub_personal_agent_schedule_workspace_user` composite index alone.

- [ ] **Step 2: Wipe stale build artifacts so Liquibase picks up the edit**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:clean
rm -rf server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build/resources/main/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml
```

Expected: no error. (CLAUDE.md note: "After renaming migration files, delete stale copies from `build/resources/`.")

- [ ] **Step 3: Verify the XML still parses**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml
git commit -m "$(cat <<'EOF'
- Enforce 1:1 agent→schedule via UNIQUE constraint (edit init in place)

The simple agent-id index is replaced with a UNIQUE constraint that also
serves as the lookup index. v1 init edited in place since the migration
is unreleased on 0_732.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Add `findByAiHubPersonalAgentId` repository method

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java`

- [ ] **Step 1: Add the single-row finder, drop the list-by-agent finder**

Replace the body of `AiHubPersonalAgentScheduleRepository` with:

```java
@Repository
public interface AiHubPersonalAgentScheduleRepository extends CrudRepository<AiHubPersonalAgentSchedule, Long> {

    Optional<AiHubPersonalAgentSchedule> findByAiHubPersonalAgentId(long aiHubPersonalAgentId);

    List<AiHubPersonalAgentSchedule> findByWorkspaceIdAndUserId(long workspaceId, long userId);

    @Query("SELECT * FROM ai_hub_personal_agent_schedule WHERE enabled = true")
    List<AiHubPersonalAgentSchedule> findAllEnabled();
}
```

Imports: `java.util.Optional` (already present transitively via `CrudRepository`, but add explicit `import java.util.Optional;` if missing). Drop unused `import java.util.List;`? — keep, `findByWorkspaceIdAndUserId` + `findAllEnabled` still return `List`.

- [ ] **Step 2: Verify it compiles**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit (deferred — bundled with service refactor in Task 4)**

No commit yet; this change is incomplete on its own (the old method's callers in `AiHubPersonalAgentScheduleServiceImpl` still reference it).

---

### Task 3: Add UNIQUE-constraint integration test (must fail before code change ships)

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleUniqueConstraintIntTest.java`

- [ ] **Step 1: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentRepository;
import com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
import com.bytechef.ee.automation.aihub.personalagent.repository.WorkspaceAiHubPersonalAgentRepository;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleLifecycleKind;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the {@code uniq_ai_hub_personal_agent_schedule_agent} UNIQUE constraint introduced for
 * the v2 one-schedule-per-agent design. Pins behavior the service-level upsert cannot guarantee
 * on its own (interleaved transactions, direct repository misuse).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubPersonalAgentScheduleUniqueConstraintIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubPersonalAgentScheduleUniqueConstraintIntTest {

    @Autowired
    private AiHubPersonalAgentRepository agentRepository;

    @Autowired
    private AiHubPersonalAgentScheduleRepository scheduleRepository;

    @Autowired
    private WorkspaceAiHubPersonalAgentRepository workspaceAgentRepository;

    @AfterEach
    public void afterEach() {
        scheduleRepository.deleteAll();
        workspaceAgentRepository.deleteAll();
        agentRepository.deleteAll();
    }

    @Test
    public void testSecondInsertForSameAgentFails() {
        long agentId = saveAgentWithMembership(1L, 10L, "research-bot");

        AiHubPersonalAgentSchedule first = buildSchedule(agentId, 1L, 10L, "Daily report");

        scheduleRepository.save(first);

        AiHubPersonalAgentSchedule second = buildSchedule(agentId, 1L, 10L, "Conflicting schedule");

        assertThatThrownBy(() -> scheduleRepository.save(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testDifferentAgentsCanCoexist() {
        long agentA = saveAgentWithMembership(1L, 10L, "agent-a");
        long agentB = saveAgentWithMembership(1L, 10L, "agent-b");

        scheduleRepository.save(buildSchedule(agentA, 1L, 10L, "Schedule for A"));
        scheduleRepository.save(buildSchedule(agentB, 1L, 10L, "Schedule for B"));

        assertThat(scheduleRepository.findByAiHubPersonalAgentId(agentA)).isPresent();
        assertThat(scheduleRepository.findByAiHubPersonalAgentId(agentB)).isPresent();
    }

    private long saveAgentWithMembership(long workspaceId, long userId, String name) {
        AiHubPersonalAgent agent = new AiHubPersonalAgent(userId);

        agent.setName(name);
        agent.setTitle("Title: " + name);
        agent.setEnvironment(Environment.DEVELOPMENT);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());

        AiHubPersonalAgent saved = agentRepository.save(agent);

        workspaceAgentRepository.save(new WorkspaceAiHubPersonalAgent(workspaceId, saved.getId()));

        return saved.getId();
    }

    private AiHubPersonalAgentSchedule buildSchedule(
        long agentId, long workspaceId, long userId, String title) {

        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();

        schedule.setAiHubPersonalAgentId(agentId);
        schedule.setWorkspaceId(workspaceId);
        schedule.setUserId(userId);
        schedule.setEnvironment(Environment.DEVELOPMENT);
        schedule.setTitle(title);
        schedule.setPrompt("Sample prompt");
        schedule.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setEffectiveCronExpression("0 0 9 * * ?");
        schedule.setZoneId("UTC");
        schedule.setLifecycleKind(ScheduleLifecycleKind.RECURRING);

        return schedule;
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubPersonalAgentScheduleUniqueConstraintIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
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

- [ ] **Step 2: Run the test, verify both pass**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:testIntegration --tests "com.bytechef.ee.automation.aihub.personalagent.AiHubPersonalAgentScheduleUniqueConstraintIntTest"
```

Expected: BUILD SUCCESSFUL, 2 tests run, 0 failures. (If `testSecondInsertForSameAgentFails` is green here, the UNIQUE constraint from Task 1 is wired correctly; if it doesn't throw, re-check the Liquibase XML.)

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleUniqueConstraintIntTest.java
git commit -m "$(cat <<'EOF'
- Pin one-schedule-per-agent UNIQUE constraint with int test

Two cases: second insert for same agent throws DataIntegrityViolationException;
two distinct agents can each have one schedule.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Refactor service interface — drop `findByAgent` + `toggle`, add `findByAgentId` + `upsertOrDelete`

**Files:**
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java`

- [ ] **Step 1: Add the new input record alongside the interface**

Append a public record to the same file so callers can pass schedule fields without leaking the JDBC entity:

```java
package com.bytechef.ee.platform.aihub.personalagent;

import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentScheduleService {

    AiHubPersonalAgentSchedule create(AiHubPersonalAgentSchedule schedule);

    AiHubPersonalAgentSchedule update(AiHubPersonalAgentSchedule schedule);

    void delete(long scheduleId, long workspaceId, long userId);

    Optional<AiHubPersonalAgentSchedule> findById(long scheduleId);

    Optional<AiHubPersonalAgentSchedule> findByAgentId(long agentId);

    /**
     * Upserts the agent's single schedule when {@code input} is non-null; deletes any existing schedule when {@code input}
     * is null. The DB-level UNIQUE constraint on {@code (ai_hub_personal_agent_id)} is the authoritative 1:1 guard;
     * implementations catch the resulting integrity exception and re-throw as {@link IllegalStateException}.
     */
    @Nullable
    AiHubPersonalAgentSchedule upsertOrDelete(
        long agentId, long workspaceId, long userId, Environment environment, @Nullable ScheduleInput input);

    /**
     * Called by listener on each fire; updates last_run_at, decrements remaining_runs, recomputes next_run_at. Returns
     * {@code true} if the schedule is still enabled after this update; {@code false} when it auto-disabled
     * (remaining_runs hit zero).
     */
    boolean recordFire(long scheduleId);

    /** Called by listener on dispatch failure. */
    void recordFailure(long scheduleId);

    @Nullable
    AiHubPersonalAgentSchedule findEnabled(long scheduleId);

    /**
     * Transport-shaped schedule payload for {@link #upsertOrDelete}. All fields mirror the schedule entity's user-set
     * columns; lifecycle bookkeeping (remaining_runs, consecutive_failures, last/next_run_at) is owned by the service.
     */
    record ScheduleInput(
        boolean enabled,
        String title,
        String prompt,
        ScheduleFrequencyKind frequencyKind,
        @Nullable Integer intervalMinutes,
        @Nullable Integer minuteOfHour,
        @Nullable LocalTime timeOfDay,
        @Nullable Integer dayOfWeek,
        @Nullable Integer dayOfMonth,
        @Nullable String cronExpression,
        String zoneId,
        @Nullable LocalDateTime startDate,
        ScheduleLifecycleKind lifecycleKind,
        @Nullable Integer maxRuns) {
    }
}
```

Note the deletions: `toggle(...)` and `findByAgent(...)` are gone. `List` import stays because `recordFire` still returns a `boolean` and nothing else uses `List` here — actually drop `import java.util.List;` since no method returns `List`.

- [ ] **Step 2: Verify it compiles**

Run:
```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: No commit yet** — the impl class still references the removed methods, so the project won't compile end-to-end until Task 5 lands. Both Tasks 2, 4, 5 commit together at the end of Task 5.

---

### Task 5: Implement service changes — `findByAgentId`, `upsertOrDelete`, remove `toggle` + `findByAgent`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`

- [ ] **Step 1: Replace `findByAgent` body with `findByAgentId`, delete `toggle`**

Locate:
```java
@Override
public List<AiHubPersonalAgentSchedule> findByAgent(long agentId) {
    return repository.findByAiHubPersonalAgentIdOrderByCreatedDateDesc(agentId);
}
```

Replace with:
```java
@Override
public Optional<AiHubPersonalAgentSchedule> findByAgentId(long agentId) {
    return repository.findByAiHubPersonalAgentId(agentId);
}
```

Delete the entire `toggle(...)` method (lines that begin `public AiHubPersonalAgentSchedule toggle(...)` and end with the matching closing brace). Drop the `import java.util.List;` if no other call site references `List`.

- [ ] **Step 2: Add `upsertOrDelete`**

Insert this method between `delete(...)` and `findById(...)`:

```java
@Override
@Transactional
@Nullable
public AiHubPersonalAgentSchedule upsertOrDelete(
    long agentId, long workspaceId, long userId, Environment environment,
    @Nullable ScheduleInput input) {

    Optional<AiHubPersonalAgentSchedule> existing = repository.findByAiHubPersonalAgentId(agentId);

    if (input == null) {
        existing.ifPresent(row -> delete(row.getId(), workspaceId, userId));

        return null;
    }

    if (existing.isPresent()) {
        AiHubPersonalAgentSchedule row = existing.get();

        if (row.getWorkspaceId() != workspaceId || row.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule for agent " + agentId + " not owned by caller");
        }

        applyInput(row, input);

        return update(row);
    }

    AiHubPersonalAgentSchedule row = new AiHubPersonalAgentSchedule();

    row.setAiHubPersonalAgentId(agentId);
    row.setWorkspaceId(workspaceId);
    row.setUserId(userId);
    row.setEnvironment(environment);

    applyInput(row, input);

    try {
        return create(row);
    } catch (DataIntegrityViolationException e) {
        throw new IllegalStateException(
            "Schedule already exists for agent " + agentId + " (concurrent create)", e);
    }
}

private static void applyInput(AiHubPersonalAgentSchedule row, ScheduleInput input) {
    row.setEnabled(input.enabled());
    row.setTitle(input.title());
    row.setPrompt(input.prompt());
    row.setFrequencyKind(input.frequencyKind());
    row.setIntervalMinutes(input.intervalMinutes());
    row.setMinuteOfHour(input.minuteOfHour());
    row.setTimeOfDay(input.timeOfDay());
    row.setDayOfWeek(input.dayOfWeek());
    row.setDayOfMonth(input.dayOfMonth());
    row.setCronExpression(input.cronExpression());
    row.setZoneId(input.zoneId());
    row.setStartDate(input.startDate());
    row.setLifecycleKind(input.lifecycleKind());
    row.setMaxRuns(input.maxRuns());
}
```

Add imports:
```java
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
import org.springframework.dao.DataIntegrityViolationException;
```

- [ ] **Step 3: Verify the full backend compiles**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL. (Compilation of `AiHubPersonalAgentGraphQlController.java` will still break — it references `findByAgent` and `toggle`. We fix it in Task 7. To validate the service module standalone, that's fine; full `./gradlew compileJava` will fail until Task 7. If the harness requires it green, do Tasks 4–7 as one unit.)

- [ ] **Step 4: Commit Tasks 2 + 4 + 5 as one cohesive backend-service change**

```bash
git add \
  server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java
git commit -m "$(cat <<'EOF'
- Replace findByAgent+toggle with findByAgentId+upsertOrDelete (service)

ScheduleService now exposes a single upsert/delete entry point that
honours the 1:1 invariant. List-by-agent and toggle disappear; the
GraphQL controller is updated in the next commit.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Update `AiHubPersonalAgentScheduleServiceImplTest` — upsert + delete coverage

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java`

- [ ] **Step 1: Audit the existing tests**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.personalagent.AiHubPersonalAgentScheduleServiceImplTest"
```

Expected: BUILD FAILED (the tests call `service.toggle(...)` and `service.findByAgent(...)`, both removed). The compiler errors tell you which tests must be rewritten.

- [ ] **Step 2: Replace the `toggle` test with an upsert-update test**

Find tests that call `service.toggle(...)` and rewrite them as `upsertOrDelete` tests. Example replacement:

```java
@Test
void testUpsertOrDeleteUpdatesExistingRow() {
    AiHubPersonalAgentSchedule existing = sampleSchedule();
    existing.setId(99L);
    existing.setEnabled(true);

    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.of(existing));
    given(repository.save(any(AiHubPersonalAgentSchedule.class))).willAnswer(invocation -> invocation.getArgument(0));

    ScheduleInput input = new ScheduleInput(
        false, "Updated title", "Updated prompt",
        ScheduleFrequencyKind.DAILY, null, null,
        LocalTime.of(10, 30), null, null, null,
        "UTC", null, ScheduleLifecycleKind.RECURRING, null);

    AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

    assertThat(result).isNotNull();
    assertThat(result.isEnabled()).isFalse();
    assertThat(result.getTitle()).isEqualTo("Updated title");

    verify(agentScheduler).cancelAgentRun(99L);
}
```

- [ ] **Step 3: Replace the `findByAgent` test with `findByAgentId`**

```java
@Test
void testFindByAgentIdReturnsTheSingleRowIfPresent() {
    AiHubPersonalAgentSchedule schedule = sampleSchedule();
    schedule.setId(7L);

    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.of(schedule));

    Optional<AiHubPersonalAgentSchedule> result = service.findByAgentId(42L);

    assertThat(result).hasValueSatisfying(row -> assertThat(row.getId()).isEqualTo(7L));
}
```

- [ ] **Step 4: Add upsert-insert + upsert-delete-when-input-null tests**

```java
@Test
void testUpsertOrDeleteInsertsWhenAbsent() {
    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.empty());
    given(repository.save(any(AiHubPersonalAgentSchedule.class)))
        .willAnswer(invocation -> {
            AiHubPersonalAgentSchedule row = invocation.getArgument(0);
            row.setId(123L);
            return row;
        });

    ScheduleInput input = new ScheduleInput(
        true, "New", "Prompt",
        ScheduleFrequencyKind.DAILY, null, null,
        LocalTime.of(9, 0), null, null, null,
        "UTC", null, ScheduleLifecycleKind.RECURRING, null);

    AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input);

    assertThat(result).isNotNull();
    assertThat(result.getAiHubPersonalAgentId()).isEqualTo(42L);

    verify(agentScheduler).scheduleAgentRun(eq(123L), anyString(), eq("UTC"), any());
}

@Test
void testUpsertOrDeleteDeletesWhenInputNull() {
    AiHubPersonalAgentSchedule existing = sampleSchedule();
    existing.setId(99L);

    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.of(existing));

    AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

    assertThat(result).isNull();

    verify(repository).deleteById(99L);
    verify(agentScheduler).cancelAgentRun(99L);
}

@Test
void testUpsertOrDeleteWithInputNullAndNoExistingIsNoop() {
    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.empty());

    AiHubPersonalAgentSchedule result = service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, null);

    assertThat(result).isNull();

    verify(repository, never()).deleteById(anyLong());
    verify(agentScheduler, never()).cancelAgentRun(anyLong());
}

@Test
void testUpsertOrDeleteRejectsForeignWorkspace() {
    AiHubPersonalAgentSchedule existing = sampleSchedule();
    existing.setId(99L);
    existing.setWorkspaceId(999L);
    existing.setUserId(999L);

    given(repository.findByAiHubPersonalAgentId(42L)).willReturn(Optional.of(existing));

    ScheduleInput input = new ScheduleInput(
        true, "Hijack", "Prompt",
        ScheduleFrequencyKind.DAILY, null, null,
        LocalTime.of(9, 0), null, null, null,
        "UTC", null, ScheduleLifecycleKind.RECURRING, null);

    assertThatThrownBy(() -> service.upsertOrDelete(42L, 1L, 10L, Environment.DEVELOPMENT, input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not owned by caller");
}
```

Imports to add:
```java
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
```

- [ ] **Step 5: Run the tests, verify green**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.personalagent.AiHubPersonalAgentScheduleServiceImplTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java
git commit -m "$(cat <<'EOF'
- Rewrite schedule service tests for upsertOrDelete

Drops toggle + findByAgent coverage. Adds insert, update, delete-on-null,
no-op, and foreign-workspace-rejection cases for the new upsertOrDelete.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Update GraphQL schema files

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls`

- [ ] **Step 1: Strip queries + mutations + input types from the schedule schema**

Replace the entire contents of `ai-hub-personal-agent-schedule.graphqls` with:

```graphql
"Scheduled run configuration for a personal agent. At most one row per agent."
type AiHubPersonalAgentSchedule {
    id: ID!
    aiHubPersonalAgentId: ID!
    title: String!
    prompt: String!
    frequencyKind: ScheduleFrequencyKind!
    intervalMinutes: Int
    minuteOfHour: Int
    timeOfDay: String
    dayOfWeek: Int
    dayOfMonth: Int
    cronExpression: String
    effectiveCronExpression: String!
    zoneId: String!
    startDate: String
    lifecycleKind: ScheduleLifecycleKind!
    maxRuns: Int
    remainingRuns: Int
    enabled: Boolean!
    lastRunAt: String
    nextRunAt: String
}

enum ScheduleFrequencyKind {
    EVERY_X_MINUTES
    HOURLY
    DAILY
    WEEKLY
    MONTHLY
    CUSTOM_CRON
}

enum ScheduleLifecycleKind {
    RECURRING
    NUMBER_OF_RUNS
}
```

- [ ] **Step 2: Add `schedule` field + `set` mutation to the agent schema**

Edit `ai-hub-personal-agent.graphqls`:

1. Add a new field on `type AiHubPersonalAgent` (after `tools`, before `createdAt`):

```graphql
    """
    Optional one-to-one schedule. Null when the user has not enabled scheduling for this agent.
    Read-only here — mutate via setAiHubPersonalAgentSchedule.
    """
    schedule: AiHubPersonalAgentSchedule
```

2. Add the new mutation to `extend type Mutation` (after `updateAiHubPersonalAgentToolConfig`):

```graphql
    """
    Upserts or deletes the agent's single schedule.
    - input.schedule != null  → upsert (insert if absent, update if present).
    - input.schedule == null  → delete any existing schedule + cancel Quartz.
    Returns the agent with its (possibly null) schedule field populated.
    """
    setAiHubPersonalAgentSchedule(input: SetAiHubPersonalAgentScheduleInput!): AiHubPersonalAgent!
```

3. Add the two new input types at the bottom of the file:

```graphql
input SetAiHubPersonalAgentScheduleInput {
    workspaceId: ID!
    aiHubPersonalAgentId: ID!
    """
    Null clears the schedule. Non-null upserts.
    """
    schedule: AiHubPersonalAgentScheduleInput
}

input AiHubPersonalAgentScheduleInput {
    enabled: Boolean!
    title: String!
    prompt: String!
    frequencyKind: ScheduleFrequencyKind!
    intervalMinutes: Int
    minuteOfHour: Int
    timeOfDay: String
    dayOfWeek: Int
    dayOfMonth: Int
    cronExpression: String
    zoneId: String!
    startDate: String
    lifecycleKind: ScheduleLifecycleKind!
    maxRuns: Int
}
```

- [ ] **Step 3: No commit yet** — the controller (Task 8) still binds the removed mutations; bundle commit.

---

### Task 8: Rewrite `AiHubPersonalAgentGraphQlController` — single `set` mutation, `schedule` resolver

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java`

- [ ] **Step 1: Delete the four schedule mutations + two queries + helper methods + records**

Remove:
- `aiHubPersonalAgentSchedules(...)` (around line 277)
- `aiHubPersonalAgentSchedule(...)` (around line 297)
- `createAiHubPersonalAgentSchedule(...)` (around line 321)
- `updateAiHubPersonalAgentSchedule(...)` (around line 340)
- `deleteAiHubPersonalAgentSchedule(...)` (around line 361)
- `toggleAiHubPersonalAgentSchedule(...)` (around line 375)
- `inputToEntity(...)` (around line 386)
- `applyUpdate(...)` (around line 412)
- `record CreateAiHubPersonalAgentScheduleInput` (around line 458)
- `record UpdateAiHubPersonalAgentScheduleInput` (around line 468)

Drop now-unused imports: `LocalTime`, `LocalDateTime`, `AiHubPersonalAgentSchedule` (used in the new resolver and `set` mutation — keep), `ScheduleFrequencyKind`, `ScheduleLifecycleKind` (used in the new input record — keep).

- [ ] **Step 2: Add the `schedule` field resolver**

After the `updatedAt` `@SchemaMapping` resolver (around line 269), insert:

```java
@SchemaMapping(typeName = "AiHubPersonalAgent", field = "schedule")
@Nullable
public AiHubPersonalAgentSchedule schedule(AiHubPersonalAgent agent) {
    return scheduleService.findByAgentId(agent.getId())
        .orElse(null);
}
```

- [ ] **Step 3: Add the `setAiHubPersonalAgentSchedule` mutation**

In the same file, add:

```java
@MutationMapping
public AiHubPersonalAgent setAiHubPersonalAgentSchedule(
    @Argument SetAiHubPersonalAgentScheduleInput input) {

    long userId = userService.getCurrentUser()
        .getId();

    WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

    AiHubPersonalAgent agent = aiHubPersonalAgentService
        .findOwned(input.aiHubPersonalAgentId(), input.workspaceId(), userId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Personal agent " + input.aiHubPersonalAgentId() + " not found in workspace " + input.workspaceId()));

    ScheduleInput serviceInput = input.schedule() == null
        ? null
        : new ScheduleInput(
            input.schedule().enabled(),
            input.schedule().title(),
            input.schedule().prompt(),
            input.schedule().frequencyKind(),
            input.schedule().intervalMinutes(),
            input.schedule().minuteOfHour(),
            input.schedule().timeOfDay() == null ? null : LocalTime.parse(input.schedule().timeOfDay()),
            input.schedule().dayOfWeek(),
            input.schedule().dayOfMonth(),
            input.schedule().cronExpression(),
            input.schedule().zoneId(),
            input.schedule().startDate() == null ? null : LocalDateTime.parse(input.schedule().startDate()),
            input.schedule().lifecycleKind(),
            input.schedule().maxRuns());

    scheduleService.upsertOrDelete(
        input.aiHubPersonalAgentId(), input.workspaceId(), userId,
        agent.getEnvironment(), serviceInput);

    return agent;
}
```

Restore `import java.time.LocalDateTime;` and `import java.time.LocalTime;` (they were dropped in Step 1; the new mutation needs them).

Add:
```java
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService.ScheduleInput;
```

- [ ] **Step 4: Add the two input records at the bottom of the class**

```java
public record SetAiHubPersonalAgentScheduleInput(
    long workspaceId, long aiHubPersonalAgentId,
    @Nullable AiHubPersonalAgentScheduleInputBody schedule) {
}

public record AiHubPersonalAgentScheduleInputBody(
    boolean enabled, String title, String prompt,
    ScheduleFrequencyKind frequencyKind,
    @Nullable Integer intervalMinutes, @Nullable Integer minuteOfHour, @Nullable String timeOfDay,
    @Nullable Integer dayOfWeek, @Nullable Integer dayOfMonth, @Nullable String cronExpression,
    String zoneId, @Nullable String startDate,
    ScheduleLifecycleKind lifecycleKind, @Nullable Integer maxRuns) {
}
```

(The inner record is named `…ScheduleInputBody` to avoid colliding with the service-layer `ScheduleInput` record imported above. Spring GraphQL will bind by field shape regardless of the Java class name — the GraphQL type is still `AiHubPersonalAgentScheduleInput`.)

- [ ] **Step 5: Verify full backend compiles**

Run:
```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit Tasks 7 + 8 together**

```bash
git add \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java
git commit -m "$(cat <<'EOF'
- Collapse schedule GraphQL surface to setAiHubPersonalAgentSchedule

Removes the four v1 mutations and two queries. Adds a single upsert/delete
mutation and a schedule field on the AiHubPersonalAgent payload.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: Rewrite `AiHubPersonalAgentGraphQlControllerTest`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/test/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlControllerTest.java`

- [ ] **Step 1: Confirm what's broken**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:test --tests "AiHubPersonalAgentGraphQlControllerTest"
```

Expected: BUILD FAILED on missing symbols. Note which test methods reference the removed mutations/queries — those are the ones to rewrite.

- [ ] **Step 2: Delete v1 schedule test methods**

Remove every test method whose name contains `Schedule` except the ones you're about to add. Specifically remove methods exercising:
- `createAiHubPersonalAgentSchedule`
- `updateAiHubPersonalAgentSchedule`
- `deleteAiHubPersonalAgentSchedule`
- `toggleAiHubPersonalAgentSchedule`
- `aiHubPersonalAgentSchedules` (the list query)
- `aiHubPersonalAgentSchedule` (the single-id query)

- [ ] **Step 3: Add new tests for `setAiHubPersonalAgentSchedule` + the resolver**

```java
@Test
void testSetAiHubPersonalAgentScheduleUpsert() {
    long workspaceId = 1L;
    long userId = 10L;
    long agentId = 42L;

    AiHubPersonalAgent agent = sampleAgent(agentId, workspaceId, userId);

    given(userService.getCurrentUser()).willReturn(sampleUser(userId));
    given(aiHubPersonalAgentService.findOwned(agentId, workspaceId, userId))
        .willReturn(Optional.of(agent));

    AiHubPersonalAgentScheduleInputBody body = new AiHubPersonalAgentScheduleInputBody(
        true, "Daily report", "Run morning report",
        ScheduleFrequencyKind.DAILY,
        null, null, "09:00", null, null, null,
        "UTC", null,
        ScheduleLifecycleKind.RECURRING, null);

    SetAiHubPersonalAgentScheduleInput input =
        new SetAiHubPersonalAgentScheduleInput(workspaceId, agentId, body);

    AiHubPersonalAgent result = controller.setAiHubPersonalAgentSchedule(input);

    assertThat(result).isSameAs(agent);

    verify(scheduleService).upsertOrDelete(
        eq(agentId), eq(workspaceId), eq(userId), eq(agent.getEnvironment()),
        argThat(serviceInput ->
            serviceInput != null
                && serviceInput.enabled()
                && serviceInput.title().equals("Daily report")
                && serviceInput.frequencyKind() == ScheduleFrequencyKind.DAILY));
}

@Test
void testSetAiHubPersonalAgentScheduleDelete() {
    long workspaceId = 1L;
    long userId = 10L;
    long agentId = 42L;

    AiHubPersonalAgent agent = sampleAgent(agentId, workspaceId, userId);

    given(userService.getCurrentUser()).willReturn(sampleUser(userId));
    given(aiHubPersonalAgentService.findOwned(agentId, workspaceId, userId))
        .willReturn(Optional.of(agent));

    SetAiHubPersonalAgentScheduleInput input =
        new SetAiHubPersonalAgentScheduleInput(workspaceId, agentId, null);

    controller.setAiHubPersonalAgentSchedule(input);

    verify(scheduleService).upsertOrDelete(
        eq(agentId), eq(workspaceId), eq(userId), eq(agent.getEnvironment()),
        isNull());
}

@Test
void testSetAiHubPersonalAgentScheduleRejectsUnknownAgent() {
    long workspaceId = 1L;
    long userId = 10L;

    given(userService.getCurrentUser()).willReturn(sampleUser(userId));
    given(aiHubPersonalAgentService.findOwned(999L, workspaceId, userId))
        .willReturn(Optional.empty());

    SetAiHubPersonalAgentScheduleInput input =
        new SetAiHubPersonalAgentScheduleInput(workspaceId, 999L, null);

    assertThatThrownBy(() -> controller.setAiHubPersonalAgentSchedule(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found in workspace");
}

@Test
void testScheduleResolverReturnsRowWhenPresent() {
    AiHubPersonalAgent agent = sampleAgent(42L, 1L, 10L);
    AiHubPersonalAgentSchedule schedule = sampleSchedule(42L);

    given(scheduleService.findByAgentId(42L)).willReturn(Optional.of(schedule));

    AiHubPersonalAgentSchedule resolved = controller.schedule(agent);

    assertThat(resolved).isSameAs(schedule);
}

@Test
void testScheduleResolverReturnsNullWhenAbsent() {
    AiHubPersonalAgent agent = sampleAgent(42L, 1L, 10L);

    given(scheduleService.findByAgentId(42L)).willReturn(Optional.empty());

    assertThat(controller.schedule(agent)).isNull();
}
```

Add helper methods if `sampleAgent`/`sampleUser`/`sampleSchedule` don't already exist in the test class. Imports:
```java
import com.bytechef.ee.automation.aihub.web.graphql.AiHubPersonalAgentGraphQlController.AiHubPersonalAgentScheduleInputBody;
import com.bytechef.ee.automation.aihub.web.graphql.AiHubPersonalAgentGraphQlController.SetAiHubPersonalAgentScheduleInput;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleLifecycleKind;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
```

- [ ] **Step 4: Run, verify green**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:test --tests "AiHubPersonalAgentGraphQlControllerTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/test/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlControllerTest.java
git commit -m "$(cat <<'EOF'
- Rewrite personal-agent graphql controller tests for set/resolver

Adds coverage for setAiHubPersonalAgentSchedule (upsert path, delete
path, unknown-agent rejection) and the schedule field resolver. Drops
the four removed mutations and two removed queries.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: Full backend `check` — Spotless + tests

**Files:** none new

- [ ] **Step 1: Format**

Run:
```bash
./gradlew spotlessApply
```

Expected: BUILD SUCCESSFUL (may rewrite formatting on files you edited).

- [ ] **Step 2: Re-stage formatting changes (if any) and amend the most recent commit only if formatting touched its files**

```bash
git status
```

If `spotlessApply` only modified files from earlier commits, amend each commit individually using `git add -p` + `git commit --amend --no-edit` per affected commit. If it touched the latest commit only, just amend it.

- [ ] **Step 3: Run the full check**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:check
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:check
```

Expected: all three BUILD SUCCESSFUL.

- [ ] **Step 4: Run the new integration test (sanity)**

Run:
```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:testIntegration --tests "AiHubPersonalAgentScheduleUniqueConstraintIntTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: No commit unless `spotlessApply` produced staged changes that haven't been amended.**

---

### Task 11: Extend `aiHubPersonalAgent` query with `schedule` field block

**Files:**
- Modify: `client/src/graphql/ai/aihub/personal-agent/aiHubPersonalAgent.graphql`

- [ ] **Step 1: Add a fragment for the schedule fields and reference it**

Replace the file with:

```graphql
query aiHubPersonalAgent($workspaceId: ID!, $id: ID!) {
    aiHubPersonalAgent(workspaceId: $workspaceId, id: $id) {
        id
        workspaceId
        userId
        name
        title
        description
        instructions
        environmentId
        llmProvider
        llmModel
        tools {
            id
            aiHubPersonalAgentId
            componentName
            componentVersion
            operationName
            connectionId
            parameters
            createdAt
        }
        schedule {
            ...AiHubPersonalAgentScheduleFields
        }
        createdAt
        updatedAt
    }
}

fragment AiHubPersonalAgentScheduleFields on AiHubPersonalAgentSchedule {
    id
    aiHubPersonalAgentId
    title
    prompt
    frequencyKind
    intervalMinutes
    minuteOfHour
    timeOfDay
    dayOfWeek
    dayOfMonth
    cronExpression
    effectiveCronExpression
    zoneId
    startDate
    lifecycleKind
    maxRuns
    remainingRuns
    enabled
    lastRunAt
    nextRunAt
}
```

(The fragment moves out of the deleted `aiHubPersonalAgentSchedules.graphql` into this file so the schedule selection is self-contained on the agent query.)

- [ ] **Step 2: No commit yet** — combined with Task 12.

---

### Task 12: Add `setAiHubPersonalAgentSchedule.graphql`, delete v1 schedule `.graphql` files

**Files:**
- Create: `client/src/graphql/ai/aihub/personal-agent/schedule/setAiHubPersonalAgentSchedule.graphql`
- Delete: six `.graphql` files under `client/src/graphql/ai/aihub/personal-agent/schedule/` (everything except the new one being created).

- [ ] **Step 1: Create the new mutation file**

```graphql
mutation setAiHubPersonalAgentSchedule($input: SetAiHubPersonalAgentScheduleInput!) {
    setAiHubPersonalAgentSchedule(input: $input) {
        id
        schedule {
            ...AiHubPersonalAgentScheduleFields
        }
    }
}
```

(Reuses the fragment declared in `aiHubPersonalAgent.graphql` — codegen scans all `.graphql` files in the project, so fragments declared anywhere are available everywhere.)

- [ ] **Step 2: Delete the v1 operations**

```bash
git rm \
  client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedules.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/aiHubPersonalAgentSchedule.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/createAiHubPersonalAgentSchedule.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/updateAiHubPersonalAgentSchedule.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/deleteAiHubPersonalAgentSchedule.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/toggleAiHubPersonalAgentSchedule.graphql
```

- [ ] **Step 3: Regenerate the codegen output**

Run:
```bash
cd client && npx graphql-codegen && cd ..
```

Expected: regenerates `client/src/shared/middleware/graphql.ts` with the new `useSetAiHubPersonalAgentScheduleMutation`, an updated `AiHubPersonalAgentQuery` type that includes `schedule`, and removes the v1 hooks.

- [ ] **Step 4: Commit Tasks 11 + 12 together**

```bash
git add \
  client/src/graphql/ai/aihub/personal-agent/aiHubPersonalAgent.graphql \
  client/src/graphql/ai/aihub/personal-agent/schedule/setAiHubPersonalAgentSchedule.graphql \
  client/src/shared/middleware/graphql.ts
git commit -m "$(cat <<'EOF'
- client - Replace schedule graphql ops with single set mutation

The agent query now selects the schedule field; the six v1 operation
files (create/update/delete/toggle/list/get) are removed.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 13: Add the single-mutation hook, delete v1 hooks file

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedule.ts`
- Delete: `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts`

- [ ] **Step 1: Write the new hooks file**

```typescript
import {useSetAiHubPersonalAgentScheduleMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

export const useSetAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useSetAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubPersonalAgent']}),
    });
};
```

(The query key matches the existing `useAiHubPersonalAgentQuery` cache key produced by the codegen — confirm by grepping the regenerated `graphql.ts` for `'aiHubPersonalAgent'`. If codegen uses a different casing like `AiHubPersonalAgent`, adjust accordingly.)

- [ ] **Step 2: Delete the v1 hooks file**

```bash
git rm client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts
```

- [ ] **Step 3: No commit yet** — bundled with Task 14 + 15.

---

### Task 14: Build `AiHubPersonalAgentScheduleTab`

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.tsx`

- [ ] **Step 1: Write the component**

```tsx
import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Switch} from '@/components/ui/switch';
import {Textarea} from '@/components/ui/textarea';
import {
    AiHubPersonalAgentScheduleFieldsFragment,
    ScheduleFrequencyKind,
    ScheduleLifecycleKind,
} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AiHubPersonalAgentScheduleFrequencyFields, {
    ScheduleFrequencyFieldsValueI,
} from './AiHubPersonalAgentScheduleFrequencyFields';

const COMMON_TIMEZONES = [
    'UTC',
    'America/New_York',
    'America/Chicago',
    'America/Denver',
    'America/Los_Angeles',
    'Europe/Zagreb',
    'Europe/London',
    'Europe/Berlin',
    'Asia/Tokyo',
    'Asia/Shanghai',
];

export interface AiHubPersonalAgentScheduleTabValueI {
    cronExpression?: string | null;
    dayOfMonth?: number | null;
    dayOfWeek?: number | null;
    enabled: boolean;
    frequencyKind: ScheduleFrequencyKind;
    intervalMinutes?: number | null;
    lifecycleKind: ScheduleLifecycleKind;
    maxRuns?: number | null;
    minuteOfHour?: number | null;
    nextRunAt?: string | null;
    prompt: string;
    startDate?: string | null;
    timeOfDay?: string | null;
    title: string;
    zoneId: string;
}

interface AiHubPersonalAgentScheduleTabPropsType {
    existingSchedule?: AiHubPersonalAgentScheduleFieldsFragment | null;
    onChange: (value: AiHubPersonalAgentScheduleTabValueI) => void;
    onRemove?: () => void;
    value: AiHubPersonalAgentScheduleTabValueI;
}

export const buildDefaultScheduleValue = (): AiHubPersonalAgentScheduleTabValueI => ({
    enabled: false,
    frequencyKind: ScheduleFrequencyKind.Daily,
    lifecycleKind: ScheduleLifecycleKind.Recurring,
    prompt: '',
    title: '',
    zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
});

export const fromExistingSchedule = (
    schedule: AiHubPersonalAgentScheduleFieldsFragment
): AiHubPersonalAgentScheduleTabValueI => ({
    cronExpression: schedule.cronExpression,
    dayOfMonth: schedule.dayOfMonth,
    dayOfWeek: schedule.dayOfWeek,
    enabled: schedule.enabled,
    frequencyKind: schedule.frequencyKind,
    intervalMinutes: schedule.intervalMinutes,
    lifecycleKind: schedule.lifecycleKind,
    maxRuns: schedule.maxRuns,
    minuteOfHour: schedule.minuteOfHour,
    nextRunAt: schedule.nextRunAt,
    prompt: schedule.prompt,
    startDate: schedule.startDate,
    timeOfDay: schedule.timeOfDay,
    title: schedule.title,
    zoneId: schedule.zoneId,
});

const AiHubPersonalAgentScheduleTab = ({
    existingSchedule,
    onChange,
    onRemove,
    value,
}: AiHubPersonalAgentScheduleTabPropsType) => {
    const update = (patch: Partial<AiHubPersonalAgentScheduleTabValueI>) => onChange({...value, ...patch});

    const frequencyFields: ScheduleFrequencyFieldsValueI = {
        cronExpression: value.cronExpression,
        dayOfMonth: value.dayOfMonth,
        dayOfWeek: value.dayOfWeek,
        intervalMinutes: value.intervalMinutes,
        minuteOfHour: value.minuteOfHour,
        timeOfDay: value.timeOfDay,
    };

    const [confirmingRemove, setConfirmingRemove] = useState(false);

    return (
        <fieldset className="mx-auto flex w-full max-w-2xl flex-col gap-5 border-0">
            <fieldset className="flex items-center gap-3 border-0">
                <Switch
                    checked={value.enabled}
                    id="schedule-enabled"
                    onCheckedChange={(enabled) => update({enabled})}
                />

                <Label htmlFor="schedule-enabled">Run this agent on a schedule</Label>
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-title">Title</Label>

                <Input
                    id="schedule-title"
                    onChange={(event) => update({title: event.target.value})}
                    placeholder="e.g., Daily report generation"
                    value={value.title}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-prompt">Task description</Label>

                <Textarea
                    id="schedule-prompt"
                    onChange={(event) => update({prompt: event.target.value})}
                    placeholder="Describe what this scheduled task should do..."
                    rows={3}
                    value={value.prompt}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-frequency">Run frequency</Label>

                <Select
                    onValueChange={(selected) => update({frequencyKind: selected as ScheduleFrequencyKind})}
                    value={value.frequencyKind}
                >
                    <SelectTrigger id="schedule-frequency">
                        <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value={ScheduleFrequencyKind.EveryXMinutes}>Every X Minutes</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Hourly}>Hourly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Daily}>Daily</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Weekly}>Weekly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.Monthly}>Monthly</SelectItem>

                        <SelectItem value={ScheduleFrequencyKind.CustomCron}>Custom (Cron)</SelectItem>
                    </SelectContent>
                </Select>
            </fieldset>

            <AiHubPersonalAgentScheduleFrequencyFields
                frequencyKind={value.frequencyKind}
                onChange={(next) => update(next)}
                value={frequencyFields}
            />

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-timezone">Timezone</Label>

                <Select onValueChange={(zoneId) => update({zoneId})} value={value.zoneId}>
                    <SelectTrigger id="schedule-timezone">
                        <SelectValue placeholder="Select..." />
                    </SelectTrigger>

                    <SelectContent>
                        {COMMON_TIMEZONES.map((zone) => (
                            <SelectItem key={zone} value={zone}>
                                {zone}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label htmlFor="schedule-start-date">Start date (optional)</Label>

                <Input
                    id="schedule-start-date"
                    onChange={(event) => update({startDate: event.target.value || null})}
                    placeholder="Starts immediately"
                    type="datetime-local"
                    value={value.startDate ?? ''}
                />
            </fieldset>

            <fieldset className="flex flex-col gap-1.5 border-0">
                <Label>Lifecycle</Label>

                <div className="flex gap-2">
                    <Button
                        label="Recurring"
                        onClick={() => update({lifecycleKind: ScheduleLifecycleKind.Recurring, maxRuns: null})}
                        variant={value.lifecycleKind === ScheduleLifecycleKind.Recurring ? 'default' : 'outline'}
                    />

                    <Button
                        label="Number of runs"
                        onClick={() => update({lifecycleKind: ScheduleLifecycleKind.NumberOfRuns})}
                        variant={
                            value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? 'default' : 'outline'
                        }
                    />
                </div>
            </fieldset>

            {value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns && (
                <fieldset className="flex flex-col gap-1.5 border-0">
                    <Label htmlFor="schedule-max-runs">Max runs (optional)</Label>

                    <Input
                        id="schedule-max-runs"
                        min={1}
                        onChange={(event) =>
                            update({maxRuns: event.target.value ? parseInt(event.target.value, 10) : null})
                        }
                        placeholder="No limit"
                        type="number"
                        value={value.maxRuns ?? ''}
                    />
                </fieldset>
            )}

            {value.nextRunAt && (
                <p className="text-xs text-muted-foreground">Next run: {value.nextRunAt}</p>
            )}

            {existingSchedule != null && onRemove != null && (
                <fieldset className="flex justify-start border-0 pt-4">
                    {confirmingRemove ? (
                        <div className="flex items-center gap-2">
                            <span className="text-sm text-muted-foreground">
                                Remove the schedule? Run history will be lost.
                            </span>

                            <Button
                                label="Confirm remove"
                                onClick={() => {
                                    setConfirmingRemove(false);
                                    onRemove();
                                }}
                                variant="destructive"
                            />

                            <Button
                                label="Cancel"
                                onClick={() => setConfirmingRemove(false)}
                                variant="outline"
                            />
                        </div>
                    ) : (
                        <Button
                            label="Remove schedule"
                            onClick={() => setConfirmingRemove(true)}
                            variant="outline"
                        />
                    )}
                </fieldset>
            )}
        </fieldset>
    );
};

export default AiHubPersonalAgentScheduleTab;
```

- [ ] **Step 2: No commit yet** — bundled with Task 15.

---

### Task 15: Wire `AiHubPersonalAgentScheduleTab` into `AiHubPersonalAgentForm`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`

- [ ] **Step 1: Swap the imports**

Replace:
```tsx
import AiHubPersonalAgentSchedulesList from './schedules/AiHubPersonalAgentSchedulesList';
```
with:
```tsx
import AiHubPersonalAgentScheduleTab, {
    AiHubPersonalAgentScheduleTabValueI,
    buildDefaultScheduleValue,
    fromExistingSchedule,
} from './schedules/AiHubPersonalAgentScheduleTab';
import {useSetAiHubPersonalAgentSchedule} from './schedules/hooks/useAiHubPersonalAgentSchedule';
```

- [ ] **Step 2: Add schedule state + initial-snapshot ref for change detection**

After `const [pendingTools, setPendingTools] = useState<AiHubPersonalAgentPendingToolI[]>([]);` add:

```tsx
const [schedule, setSchedule] = useState<AiHubPersonalAgentScheduleTabValueI>(buildDefaultScheduleValue());
const [initialSchedule, setInitialSchedule] = useState<AiHubPersonalAgentScheduleTabValueI | null>(null);

const setScheduleMutation = useSetAiHubPersonalAgentSchedule();
```

In the `useEffect` block that hydrates from `agent`, add:
```tsx
if (agent.schedule) {
    const hydrated = fromExistingSchedule(agent.schedule);
    setSchedule(hydrated);
    setInitialSchedule(hydrated);
} else {
    setSchedule(buildDefaultScheduleValue());
    setInitialSchedule(null);
}
```

- [ ] **Step 3: Add a `buildSchedulePayload` helper above the component**

Outside the component (right after `slugify`):

```tsx
const buildSchedulePayload = (value: AiHubPersonalAgentScheduleTabValueI) => ({
    cronExpression: value.cronExpression ?? null,
    dayOfMonth: value.dayOfMonth ?? null,
    dayOfWeek: value.dayOfWeek ?? null,
    enabled: value.enabled,
    frequencyKind: value.frequencyKind,
    intervalMinutes: value.intervalMinutes ?? null,
    lifecycleKind: value.lifecycleKind,
    maxRuns: value.lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? value.maxRuns ?? null : null,
    minuteOfHour: value.minuteOfHour ?? null,
    prompt: value.prompt,
    startDate: value.startDate || null,
    timeOfDay: value.timeOfDay ?? null,
    title: value.title,
    zoneId: value.zoneId,
});

const scheduleChanged = (
    initial: AiHubPersonalAgentScheduleTabValueI | null,
    current: AiHubPersonalAgentScheduleTabValueI
): boolean => {
    if (initial == null) {
        return current.enabled || current.title.length > 0 || current.prompt.length > 0;
    }

    return JSON.stringify(buildSchedulePayload(initial)) !== JSON.stringify(buildSchedulePayload(current));
};
```

Add the `ScheduleLifecycleKind` import:
```tsx
import {ScheduleLifecycleKind} from '@/shared/middleware/graphql';
```

- [ ] **Step 4: Extend the create-mode submit handler**

After the for-loop that attaches pending tools, but before the `if (failedAttaches.length > 0)` branch:

```tsx
let scheduleError: string | null = null;

if (schedule.enabled || schedule.title.length > 0 || schedule.prompt.length > 0) {
    try {
        await setScheduleMutation.mutateAsync({
            input: {
                aiHubPersonalAgentId: createdAgentId,
                schedule: buildSchedulePayload(schedule),
                workspaceId: String(currentWorkspaceId),
            },
        });
    } catch (error) {
        scheduleError = error instanceof Error ? error.message : String(error);
    }
}
```

Adjust the existing toast block to include the schedule failure:

```tsx
if (failedAttaches.length > 0 || scheduleError != null) {
    const parts: string[] = [];

    if (failedAttaches.length > 0) {
        parts.push(
            `${failedAttaches.length} tool${failedAttaches.length === 1 ? '' : 's'} failed to attach: ${failedAttaches.join(', ')}`
        );
    }

    if (scheduleError != null) {
        parts.push(`schedule failed to save: ${scheduleError}`);
    }

    toast.error(`Agent created, but ${parts.join('; ')}. Edit the agent to retry.`);
} else {
    toast.success(`Agent "${title.trim() || computedSlug}" created`);
}
```

- [ ] **Step 5: Extend the edit-mode submit handler**

After the `updateMutation.mutateAsync` call but before `toast.success`:

```tsx
if (scheduleChanged(initialSchedule, schedule)) {
    await setScheduleMutation.mutateAsync({
        input: {
            aiHubPersonalAgentId: String(agent.id),
            schedule: buildSchedulePayload(schedule),
            workspaceId: String(currentWorkspaceId),
        },
    });
}
```

- [ ] **Step 6: Swap the tab content + show the Schedule tab in both modes**

Find:
```tsx
{isEditMode && <TabsTrigger value="schedules">Schedules</TabsTrigger>}
```
Replace with:
```tsx
<TabsTrigger value="schedule">Schedule</TabsTrigger>
```

Find:
```tsx
{isEditMode && agentIdFromRoute !== undefined && (
    <TabsContent value="schedules">
        <AiHubPersonalAgentSchedulesList
            agentId={String(agentIdFromRoute)}
            environment={currentEnvironmentId}
            workspaceId={String(currentWorkspaceId ?? '')}
        />
    </TabsContent>
)}
```

Replace with:
```tsx
<TabsContent value="schedule">
    <AiHubPersonalAgentScheduleTab
        existingSchedule={agent?.schedule ?? null}
        onChange={setSchedule}
        onRemove={
            isEditMode && agent
                ? async () => {
                      await setScheduleMutation.mutateAsync({
                          input: {
                              aiHubPersonalAgentId: String(agent.id),
                              schedule: null,
                              workspaceId: String(currentWorkspaceId ?? ''),
                          },
                      });

                      setSchedule(buildDefaultScheduleValue());
                      setInitialSchedule(null);

                      toast.success('Schedule removed');
                  }
                : undefined
        }
        value={schedule}
    />
</TabsContent>
```

- [ ] **Step 7: Delete the v1 list + dialog files**

```bash
git rm \
  client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx \
  client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx
```

- [ ] **Step 8: Run lint + typecheck**

```bash
cd client && npm run check && cd ..
```

Expected: `lint` + `typecheck` pass; existing tests pass. If `AiHubPersonalAgentScheduleDialog.test.tsx` exists and fails because the file is gone — delete the test too:

```bash
git rm client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.test.tsx 2>/dev/null || true
```

Re-run `npm run check` until green.

- [ ] **Step 9: Commit Tasks 13 + 14 + 15 together**

```bash
git add \
  client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.tsx \
  client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedule.ts \
  client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx
git add -u client/src/pages/automation/ai-hub/personal-agents/schedules/
git commit -m "$(cat <<'EOF'
- client - Render schedule inline on agent form; drop list + dialog

Replaces the Schedules tab (list view + modal dialog + per-row CRUD)
with a Schedule tab that renders schedule fields inline. Create and edit
flows both write through a single setAiHubPersonalAgentSchedule mutation;
edit mode adds a Remove schedule button.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 16: Add `AiHubPersonalAgentScheduleTab.test.tsx`

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.test.tsx`

- [ ] **Step 1: Write the test**

```tsx
import {render, screen, fireEvent} from '@testing-library/react';
import {ScheduleFrequencyKind, ScheduleLifecycleKind} from '@/shared/middleware/graphql';
import {describe, expect, it, vi} from 'vitest';

import AiHubPersonalAgentScheduleTab, {
    AiHubPersonalAgentScheduleTabValueI,
    buildDefaultScheduleValue,
} from './AiHubPersonalAgentScheduleTab';

describe('AiHubPersonalAgentScheduleTab', () => {
    const baseValue: AiHubPersonalAgentScheduleTabValueI = buildDefaultScheduleValue();

    it('renders the Enabled switch defaulting to off', () => {
        render(
            <AiHubPersonalAgentScheduleTab onChange={vi.fn()} value={baseValue} />
        );

        const toggle = screen.getByLabelText('Run this agent on a schedule');

        expect(toggle).toBeInTheDocument();
        expect(toggle).not.toBeChecked();
    });

    it('emits onChange when Enabled is toggled on', () => {
        const onChange = vi.fn();

        render(<AiHubPersonalAgentScheduleTab onChange={onChange} value={baseValue} />);

        fireEvent.click(screen.getByLabelText('Run this agent on a schedule'));

        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({enabled: true}));
    });

    it('shows the Max runs input only when lifecycle is NumberOfRuns', () => {
        const {rerender} = render(
            <AiHubPersonalAgentScheduleTab onChange={vi.fn()} value={baseValue} />
        );

        expect(screen.queryByLabelText('Max runs (optional)')).not.toBeInTheDocument();

        rerender(
            <AiHubPersonalAgentScheduleTab
                onChange={vi.fn()}
                value={{...baseValue, lifecycleKind: ScheduleLifecycleKind.NumberOfRuns}}
            />
        );

        expect(screen.getByLabelText('Max runs (optional)')).toBeInTheDocument();
    });

    it('hides the Remove schedule button in create mode', () => {
        render(
            <AiHubPersonalAgentScheduleTab onChange={vi.fn()} onRemove={vi.fn()} value={baseValue} />
        );

        expect(screen.queryByRole('button', {name: /remove schedule/i})).not.toBeInTheDocument();
    });

    it('shows Remove schedule when an existingSchedule is supplied', () => {
        const onRemove = vi.fn();

        render(
            <AiHubPersonalAgentScheduleTab
                existingSchedule={{
                    aiHubPersonalAgentId: '42',
                    cronExpression: null,
                    dayOfMonth: null,
                    dayOfWeek: null,
                    effectiveCronExpression: '0 0 9 * * ?',
                    enabled: true,
                    frequencyKind: ScheduleFrequencyKind.Daily,
                    id: '7',
                    intervalMinutes: null,
                    lastRunAt: null,
                    lifecycleKind: ScheduleLifecycleKind.Recurring,
                    maxRuns: null,
                    minuteOfHour: null,
                    nextRunAt: null,
                    prompt: 'Run morning report',
                    remainingRuns: null,
                    startDate: null,
                    timeOfDay: '09:00',
                    title: 'Daily report',
                    zoneId: 'UTC',
                }}
                onChange={vi.fn()}
                onRemove={onRemove}
                value={{...baseValue, enabled: true, prompt: 'Run morning report', title: 'Daily report'}}
            />
        );

        fireEvent.click(screen.getByRole('button', {name: /remove schedule/i}));
        fireEvent.click(screen.getByRole('button', {name: /confirm remove/i}));

        expect(onRemove).toHaveBeenCalledOnce();
    });
});
```

- [ ] **Step 2: Run**

```bash
cd client && npm run test -- AiHubPersonalAgentScheduleTab && cd ..
```

Expected: all 5 tests pass.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleTab.test.tsx
git commit -m "$(cat <<'EOF'
- client - Add AiHubPersonalAgentScheduleTab vitest coverage

Covers default-off toggle, onChange emission, conditional max-runs
field, and the Remove-schedule confirm flow.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 17: Full-stack smoke test

**Files:** none

- [ ] **Step 1: Reset local infra (so the in-place migration applies cleanly)**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml down -v && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
```

Expected: containers recreated, fresh DB.

- [ ] **Step 2: Boot the server**

```bash
./gradlew -p server/apps/server-app bootRun
```

Wait until the log shows `Started Server in N.NNs (process running for N.NN)`. If Liquibase logs an error about the UNIQUE constraint, the init edit didn't make it into `build/resources/` — re-run `./gradlew clean` and retry.

- [ ] **Step 3: Boot the client**

In another terminal:
```bash
cd client && npm run dev
```

- [ ] **Step 4: Manually verify the golden path**

1. Log in as `admin@localhost.com` / `admin`.
2. Open AI Hub → Personal agents → **New personal agent**.
3. Fill the Overview tab (title `Research Bot`, instructions `Always cite sources.`).
4. Click the **Schedule** tab — confirm the Enabled toggle is off, fields are visible.
5. Toggle Enabled on; set Title `Daily summary`, Task description `Summarize my issues`, frequency `Daily`, time `09:00`, leave timezone at the browser default.
6. Click **Create agent**. Toast should say `Agent "Research Bot" created`.
7. Reopen the agent. Verify the Schedule tab shows Enabled = true with the values from step 5 and a **Next run** line.
8. Click **Remove schedule**, confirm. Toast: `Schedule removed`. Reload: Schedule tab now shows Enabled = off, empty fields.

- [ ] **Step 5: No commit. Smoke test is verification, not artifacts.**

---

### Task 18: Update the spec to note "implemented"

**Files:**
- Modify: `docs/superpowers/specs/2026-05-18-personal-agent-single-schedule-design.md`

- [ ] **Step 1: Flip status**

Replace:
```
**Status:** Design
```
with:
```
**Status:** Implemented
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-05-18-personal-agent-single-schedule-design.md
git commit -m "$(cat <<'EOF'
- Mark personal-agent single-schedule spec as implemented

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Self-review checklist

- **Spec coverage**: Every spec section maps to a task — UNIQUE constraint (Task 1), service interface (Task 4–5), GraphQL surface (Task 7–8), agent payload schedule field + resolver (Task 8), frontend tab + form integration (Tasks 14–15), removed v1 UI (Task 12 + Task 15 git-rm), tests (Tasks 3, 6, 9, 16), smoke test (Task 17). The "Failure modes considered" section in the spec is exercised by the unique-constraint int test (Task 3) and the foreign-workspace test (Task 6).
- **Placeholders**: none. Every step shows the code or command.
- **Type consistency**: `ScheduleInput` is the service-layer record (Task 4) and is passed into `upsertOrDelete` (Tasks 5, 9). `AiHubPersonalAgentScheduleInputBody` is the controller-layer wire record (Task 8) and is referenced in tests (Task 9). `AiHubPersonalAgentScheduleTabValueI` is the frontend shape (Task 14), built by `buildDefaultScheduleValue()` (Task 14), hydrated by `fromExistingSchedule(...)` (Task 14), serialised to wire by `buildSchedulePayload(...)` (Task 15). The `useSetAiHubPersonalAgentSchedule` hook (Task 13) is the only call site for the new mutation.
- **One known divergence between spec and plan**: the spec describes the Remove affordance as a "link button" that pops the "standard destructive-action confirm dialog." The plan implements the confirmation as an inline two-button toggle inside the Schedule tab itself rather than a modal, because the codebase does not have a generic `ConfirmDialog` primitive to reuse and adding one is out of scope. Both deliver "confirm before remove."
