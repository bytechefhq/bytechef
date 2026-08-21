# Personal Agent Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users schedule one or more "scheduled tasks" per AI Hub personal agent. Each scheduled fire creates a fresh `PERSONAL_AGENT` task and posts a stored prompt as the first user turn — as if the user had clicked the agent in the sidebar and typed the message.

**Architecture:** New EE table `ai_hub_personal_agent_schedule` (workspace-id directly on the row, per the automation-package convention). A new platform-scheduler API (`AgentScheduler`) wraps the shared Quartz `Scheduler` bean and registers cron-triggered jobs keyed by schedule id. The Quartz job publishes a Spring event; an EE `AgentScheduleFiredEventListener` listens, creates the task via the existing `AiHubTaskService.createAiHubPersonalAgentChat(...)`, and dispatches the prompt via a new `AiHubScheduledChatDispatcher` thin wrapper over `AiHubChatStreamer`. GraphQL surface and React tab + dialog follow existing patterns.

**Tech Stack:** Java 25 / Spring Boot 4.0.6, Spring Data JDBC, Quartz (via Spring Boot auto-config + JDBC JobStore), Liquibase, GraphQL (Spring for GraphQL), React 19 + TypeScript 5.9, GraphQL Codegen, Vitest.

**Spec:** [docs/superpowers/specs/2026-05-16-personal-agent-scheduling-design.md](docs/superpowers/specs/2026-05-16-personal-agent-scheduling-design.md)

---

## File Structure

### Platform (new + modified files)

**New:**
- `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AgentScheduler.java`
- `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AgentScheduleFiredEvent.java`
- `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/QuartzAgentScheduler.java`
- `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AgentScheduleJob.java`

**Modified:**
- `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/config/QuartzSchedulerConfiguration.java` (add `quartzAgentScheduler` bean)

**Tests (new):**
- `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/test/java/com/bytechef/platform/scheduler/QuartzAgentSchedulerIntTest.java`

### EE: AI Hub schedule domain + service

**New:**
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentSchedule.java`
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/ScheduleFrequencyKind.java`
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/ScheduleLifecycleKind.java`
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizer.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java`
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubScheduledChatDispatcher.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml`

**Modified:**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts` (add `platform-scheduler-api` dependency)
- `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java` (pin two new enums)

**Tests (new):**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizerTest.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java`
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListenerTest.java`

### EE: GraphQL

**New:**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls`

**Modified:**
- `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java` (add schedule query/mutation methods)

### Client

**New:**
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleFrequencyFields.tsx`
- `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts`
- `client/src/graphql/ai-hub/aiHubPersonalAgentSchedules.graphql`
- `client/src/graphql/ai-hub/aiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai-hub/createAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai-hub/updateAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai-hub/deleteAiHubPersonalAgentSchedule.graphql`
- `client/src/graphql/ai-hub/toggleAiHubPersonalAgentSchedule.graphql`

**Modified:**
- `client/codegen.ts` (add the new graphqls path)
- `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx` (wrap content in a Tabs shell, add Schedules tab)
- `client/src/shared/middleware/graphql.ts` (auto-regenerated by `npx graphql-codegen`)

---

## Phases

1. **Platform scheduler API + Quartz wiring** (Tasks 1–4)
2. **EE schema migration** (Task 5)
3. **EE enums + domain class** (Tasks 6–8)
4. **EE repository + service** (Tasks 9–12)
5. **EE listener + dispatcher** (Tasks 13–15)
6. **GraphQL surface** (Tasks 16–17)
7. **Client tab + dialog** (Tasks 18–23)
8. **End-to-end smoke + cleanup** (Task 24)

---

## Phase 1 — Platform scheduler API + Quartz wiring

### Task 1: AgentScheduleFiredEvent (platform-scheduler-api)

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AgentScheduleFiredEvent.java`

- [ ] **Step 1: Write the event class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.scheduler.event;

/**
 * Application event published by {@code AgentScheduleJob} when a scheduled agent run fires.
 * Consumers in EE modules (e.g., AI Hub) subscribe via {@code @EventListener} and do the
 * actual task creation / LLM dispatch. Keeps platform-scheduler free of EE dependencies.
 *
 * @author Ivica Cardic
 */
public record AgentScheduleFiredEvent(long agentScheduleId) {
}
```

- [ ] **Step 2: Commit**

```bash
git add server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/event/AgentScheduleFiredEvent.java
git commit -m "Add AgentScheduleFiredEvent for scheduled agent runs"
```

### Task 2: AgentScheduler interface (platform-scheduler-api)

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AgentScheduler.java`

- [ ] **Step 1: Write the interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.scheduler;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Scheduler API for AI Hub personal-agent scheduled runs. Wraps the shared Quartz scheduler
 * with a domain-specific surface — jobs are keyed by {@code agentScheduleId} (an opaque
 * long owned by the consuming module's table). Sibling to {@code TriggerScheduler}, which
 * is workflow-bound; this one is keyed by a plain long so it doesn't leak workflow types.
 *
 * @author Ivica Cardic
 */
public interface AgentScheduler {

    /**
     * Register a new cron-triggered job. Idempotent — registering twice with the same id
     * replaces the existing trigger.
     *
     * @param agentScheduleId   row id from {@code ai_hub_personal_agent_schedule}
     * @param cronExpression    6-field Quartz cron (seconds-resolution)
     * @param zoneId            IANA zone id (e.g., "Europe/Zagreb")
     * @param startAt           earliest fire time, or {@code null} for "starts immediately"
     */
    void scheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt);

    /**
     * Replace the trigger for an existing job. Equivalent to cancel + schedule, but
     * preserves the job key so any in-flight job that's already executing finishes
     * against the old trigger and the new trigger picks up on the next fire boundary.
     */
    void rescheduleAgentRun(long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt);

    /**
     * Remove the trigger and job. No-op if not registered.
     */
    void cancelAgentRun(long agentScheduleId);

    /**
     * Returns {@code true} iff a job/trigger pair exists for this id. Used by boot
     * reconciliation to detect drift between persisted schedules and Quartz state.
     */
    boolean exists(long agentScheduleId);
}
```

- [ ] **Step 2: Commit**

```bash
git add server/libs/platform/platform-scheduler/platform-scheduler-api/src/main/java/com/bytechef/platform/scheduler/AgentScheduler.java
git commit -m "Add AgentScheduler interface for agent-run scheduling"
```

### Task 3: AgentScheduleJob (Quartz job, platform-scheduler-impl)

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AgentScheduleJob.java`

- [ ] **Step 1: Write the job**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.scheduler.job;

import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Quartz job that fires on the cron schedule registered by {@code QuartzAgentScheduler}.
 * Publishes an {@link AgentScheduleFiredEvent} and returns — all real work (task creation,
 * LLM dispatch) happens on the listener thread so the Quartz worker pool is not pinned
 * by long-running LLM calls.
 *
 * @author Ivica Cardic
 */
public class AgentScheduleJob implements Job {

    public static final String JOB_DATA_KEY_SCHEDULE_ID = "agentScheduleId";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void execute(JobExecutionContext context) {
        long agentScheduleId = context.getMergedJobDataMap()
            .getLong(JOB_DATA_KEY_SCHEDULE_ID);

        eventPublisher.publishEvent(new AgentScheduleFiredEvent(agentScheduleId));
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-impl:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/job/AgentScheduleJob.java
git commit -m "Add AgentScheduleJob Quartz job for agent schedule fires"
```

### Task 4: QuartzAgentScheduler impl + bean wiring

**Files:**
- Create: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/QuartzAgentScheduler.java`
- Modify: `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/main/java/com/bytechef/platform/scheduler/config/QuartzSchedulerConfiguration.java`

- [ ] **Step 1: Write the impl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.scheduler;

import com.bytechef.platform.scheduler.job.AgentScheduleJob;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * Quartz-backed {@link AgentScheduler}. Group {@value #GROUP}, name format
 * {@code agent-schedule-<id>}. Misfire policy: fire once on recovery (no catch-up loop).
 *
 * @author Ivica Cardic
 */
public class QuartzAgentScheduler implements AgentScheduler {

    private static final String GROUP = "agent-run";

    private final Scheduler scheduler;

    public QuartzAgentScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleAgentRun(
        long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt) {

        JobKey jobKey = jobKey(agentScheduleId);
        TriggerKey triggerKey = triggerKey(agentScheduleId);

        JobDetail jobDetail = JobBuilder.newJob(AgentScheduleJob.class)
            .withIdentity(jobKey)
            .usingJobData(AgentScheduleJob.JOB_DATA_KEY_SCHEDULE_ID, agentScheduleId)
            .storeDurably(false)
            .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .forJob(jobKey)
            .startAt(startAt != null ? Date.from(startAt) : new Date())
            .withSchedule(
                CronScheduleBuilder.cronSchedule(cronExpression)
                    .inTimeZone(TimeZone.getTimeZone(zoneId))
                    .withMisfireHandlingInstructionFireAndProceed())
            .build();

        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule agent run " + agentScheduleId, e);
        }
    }

    @Override
    public void rescheduleAgentRun(
        long agentScheduleId, String cronExpression, String zoneId, @Nullable Instant startAt) {

        scheduleAgentRun(agentScheduleId, cronExpression, zoneId, startAt);
    }

    @Override
    public void cancelAgentRun(long agentScheduleId) {
        try {
            scheduler.deleteJob(jobKey(agentScheduleId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to cancel agent run " + agentScheduleId, e);
        }
    }

    @Override
    public boolean exists(long agentScheduleId) {
        try {
            return scheduler.checkExists(jobKey(agentScheduleId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to check agent run " + agentScheduleId, e);
        }
    }

    private static JobKey jobKey(long id) {
        return JobKey.jobKey("agent-schedule-" + id, GROUP);
    }

    private static TriggerKey triggerKey(long id) {
        return TriggerKey.triggerKey("agent-schedule-" + id, GROUP);
    }
}
```

- [ ] **Step 2: Wire the bean**

Open `QuartzSchedulerConfiguration.java`. Find the existing `quartzTriggerScheduler` bean (around line 70). Add this new bean below it:

```java
    @Bean
    AgentScheduler quartzAgentScheduler(@Lazy Scheduler scheduler) {
        return new QuartzAgentScheduler(scheduler);
    }
```

Add the import at the top:
```java
import com.bytechef.platform.scheduler.AgentScheduler;
import com.bytechef.platform.scheduler.QuartzAgentScheduler;
```

- [ ] **Step 3: Compile**

```bash
./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-impl:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Write integration test**

Create `server/libs/platform/platform-scheduler/platform-scheduler-impl/src/test/java/com/bytechef/platform/scheduler/QuartzAgentSchedulerIntTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.bytechef.platform.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SpringBootTest(classes = QuartzAgentSchedulerIntTest.TestConfig.class)
class QuartzAgentSchedulerIntTest {

    @Autowired
    private AgentScheduler agentScheduler;

    @Autowired
    private FireCounter fireCounter;

    @AfterEach
    void cleanup() {
        agentScheduler.cancelAgentRun(42L);
        fireCounter.reset();
    }

    @Test
    void testScheduleAgentRunFiresOnCron() {
        agentScheduler.scheduleAgentRun(42L, "0/2 * * * * ?", "UTC", null);

        await().atMost(Duration.ofSeconds(6))
            .until(() -> fireCounter.count() >= 1);

        assertThat(fireCounter.count()).isGreaterThanOrEqualTo(1);
        assertThat(fireCounter.lastScheduleId()).isEqualTo(42L);
    }

    @Test
    void testCancelAgentRunStopsFires() {
        agentScheduler.scheduleAgentRun(42L, "0/1 * * * * ?", "UTC", null);
        agentScheduler.cancelAgentRun(42L);

        assertThat(agentScheduler.exists(42L)).isFalse();
    }

    @Import({/* whichever config registers Scheduler bean — Spring Boot autoconfig handles it */})
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        AgentScheduler quartzAgentScheduler(org.quartz.Scheduler scheduler) {
            return new QuartzAgentScheduler(scheduler);
        }

        @org.springframework.context.annotation.Bean
        FireCounter fireCounter() {
            return new FireCounter();
        }
    }

    @Component
    static class FireCounter {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long lastScheduleId = -1;

        @EventListener
        void onFired(AgentScheduleFiredEvent event) {
            count.incrementAndGet();
            lastScheduleId = event.agentScheduleId();
        }

        int count() { return count.get(); }
        long lastScheduleId() { return lastScheduleId; }
        void reset() { count.set(0); lastScheduleId = -1; }
    }
}
```

- [ ] **Step 5: Run integration test**

```bash
./gradlew :server:libs:platform:platform-scheduler:platform-scheduler-impl:testIntegration --tests QuartzAgentSchedulerIntTest
```

Expected: PASS. If the Quartz `Scheduler` bean isn't autoconfigured in the test slice, add the appropriate `@AutoConfigureXxx` annotation — copy from any existing `*IntTest` in the same module.

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-scheduler
git commit -m "Add QuartzAgentScheduler implementation with integration test"
```

---

## Phase 2 — EE schema migration

### Task 5: Liquibase migration for ai_hub_personal_agent_schedule

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml`

- [ ] **Step 1: Write the changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260516000001-1" author="ivica">
        <createTable tableName="ai_hub_personal_agent_schedule">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="ai_hub_personal_agent_id" type="BIGINT">
                <constraints nullable="false"
                             foreignKeyName="fk_ai_hub_pa_schedule_agent"
                             references="ai_hub_personal_agent(id)"
                             deleteCascade="true"/>
            </column>
            <column name="workspace_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="environment" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="title" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="prompt" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="frequency_kind" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="interval_minutes" type="INT"/>
            <column name="minute_of_hour" type="INT"/>
            <column name="time_of_day" type="TIME"/>
            <column name="day_of_week" type="INT"/>
            <column name="day_of_month" type="INT"/>
            <column name="cron_expression" type="VARCHAR(255)"/>
            <column name="effective_cron_expression" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="zone_id" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="start_date" type="TIMESTAMP"/>
            <column name="lifecycle_kind" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="max_runs" type="INT"/>
            <column name="remaining_runs" type="INT"/>
            <column name="consecutive_failures" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="last_run_at" type="TIMESTAMP"/>
            <column name="next_run_at" type="TIMESTAMP"/>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="INT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="ai_hub_personal_agent_schedule"
                     indexName="idx_ai_hub_personal_agent_schedule_agent">
            <column name="ai_hub_personal_agent_id"/>
        </createIndex>

        <createIndex tableName="ai_hub_personal_agent_schedule"
                     indexName="idx_ai_hub_personal_agent_schedule_workspace_user">
            <column name="workspace_id"/>
            <column name="user_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Verify migration loads at startup**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml down -v && docker compose -f docker-compose.dev.infra.yml up -d
cd .. && ./gradlew -p server/apps/server-app bootRun &
# Wait ~30s, then:
docker exec -i $(docker compose -f server/docker-compose.dev.infra.yml ps -q postgres) \
  psql -U postgres -d postgres -c "\d ai_hub_personal_agent_schedule"
```

Expected output: table listing with all columns. Stop the server.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260516000001_ai_hub_personal_agent_schedule_init.xml
git commit -m "Add Liquibase migration for ai_hub_personal_agent_schedule"
```

---

## Phase 3 — EE enums + domain class

### Task 6: ScheduleFrequencyKind enum

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/ScheduleFrequencyKind.java`

- [ ] **Step 1: Write the enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

/**
 * Cadence kind for {@code AiHubPersonalAgentSchedule}. Persisted as INT ordinal.
 * <strong>Append new values at the end only.</strong> Ordinal stability is pinned
 * by {@code EnumOrdinalStabilityTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ScheduleFrequencyKind {
    EVERY_X_MINUTES,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM_CRON
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/ScheduleFrequencyKind.java
git commit -m "Add ScheduleFrequencyKind enum"
```

### Task 7: ScheduleLifecycleKind enum + ordinal stability test update

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/ScheduleLifecycleKind.java`
- Modify: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/test/java/com/bytechef/ee/platform/aihub/util/EnumOrdinalStabilityTest.java`

- [ ] **Step 1: Write the enum**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

/**
 * Lifecycle kind for {@code AiHubPersonalAgentSchedule} — UI affordance: did the user
 * pick "Recurring" or "Number of runs"? The effective behavior is governed by
 * {@code max_runs} (null = unbounded). Persisted as INT ordinal; append-only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ScheduleLifecycleKind {
    RECURRING,
    NUMBER_OF_RUNS
}
```

- [ ] **Step 2: Pin in EnumOrdinalStabilityTest**

Open `EnumOrdinalStabilityTest.java`. Find the existing `@Test` method that enumerates pinned mappings. Add two new pinned assertions following the existing pattern. The exact form depends on what the test currently looks like — read the file first, then mirror the existing style. Sketch:

```java
@Test
void testScheduleFrequencyKindOrdinals() {
    assertThat(ScheduleFrequencyKind.EVERY_X_MINUTES.ordinal()).isEqualTo(0);
    assertThat(ScheduleFrequencyKind.HOURLY.ordinal()).isEqualTo(1);
    assertThat(ScheduleFrequencyKind.DAILY.ordinal()).isEqualTo(2);
    assertThat(ScheduleFrequencyKind.WEEKLY.ordinal()).isEqualTo(3);
    assertThat(ScheduleFrequencyKind.MONTHLY.ordinal()).isEqualTo(4);
    assertThat(ScheduleFrequencyKind.CUSTOM_CRON.ordinal()).isEqualTo(5);
    assertThat(ScheduleFrequencyKind.values()).hasSize(6);
}

@Test
void testScheduleLifecycleKindOrdinals() {
    assertThat(ScheduleLifecycleKind.RECURRING.ordinal()).isEqualTo(0);
    assertThat(ScheduleLifecycleKind.NUMBER_OF_RUNS.ordinal()).isEqualTo(1);
    assertThat(ScheduleLifecycleKind.values()).hasSize(2);
}
```

- [ ] **Step 3: Run the test**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:test --tests EnumOrdinalStabilityTest
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api
git commit -m "Add ScheduleLifecycleKind enum and pin ordinals"
```

### Task 8: AiHubPersonalAgentSchedule domain class

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentSchedule.java`

- [ ] **Step 1: Write the domain class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

import com.bytechef.ee.platform.aihub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Scheduled run definition for an AI Hub personal agent. One agent can own many schedules.
 * Spring Data JDBC row-mapper; setters intentionally permissive for the
 * construct-then-save flow. Ownership and validation invariants live in
 * {@code AiHubPersonalAgentScheduleServiceImpl}, not on this type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent_schedule")
public class AiHubPersonalAgentSchedule {

    @Id
    private Long id;

    @Column("ai_hub_personal_agent_id")
    private long aiHubPersonalAgentId;

    @Column("workspace_id")
    private long workspaceId;

    @Column("user_id")
    private long userId;

    @Column("environment")
    private int environment;

    @Column("title")
    private String title;

    @Column("prompt")
    private String prompt;

    @Column("frequency_kind")
    private int frequencyKind;

    @Column("interval_minutes")
    private @Nullable Integer intervalMinutes;

    @Column("minute_of_hour")
    private @Nullable Integer minuteOfHour;

    @Column("time_of_day")
    private @Nullable LocalTime timeOfDay;

    @Column("day_of_week")
    private @Nullable Integer dayOfWeek;

    @Column("day_of_month")
    private @Nullable Integer dayOfMonth;

    @Column("cron_expression")
    private @Nullable String cronExpression;

    @Column("effective_cron_expression")
    private String effectiveCronExpression;

    @Column("zone_id")
    private String zoneId;

    @Column("start_date")
    private @Nullable LocalDateTime startDate;

    @Column("lifecycle_kind")
    private int lifecycleKind;

    @Column("max_runs")
    private @Nullable Integer maxRuns;

    @Column("remaining_runs")
    private @Nullable Integer remainingRuns;

    @Column("consecutive_failures")
    private int consecutiveFailures;

    @Column("enabled")
    private boolean enabled = true;

    @Column("last_run_at")
    private @Nullable LocalDateTime lastRunAt;

    @Column("next_run_at")
    private @Nullable LocalDateTime nextRunAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getAiHubPersonalAgentId() { return aiHubPersonalAgentId; }
    public void setAiHubPersonalAgentId(long v) { this.aiHubPersonalAgentId = v; }

    public long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(long v) { this.workspaceId = v; }

    public long getUserId() { return userId; }
    public void setUserId(long v) { this.userId = v; }

    public Environment getEnvironment() {
        Environment[] values = Environment.values();
        if (environment < 0 || environment >= values.length) {
            throw new IllegalStateException("Invalid environment ordinal: " + environment);
        }
        return values[environment];
    }
    public void setEnvironment(Environment env) {
        if (env != null) this.environment = env.ordinal();
    }

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String v) { this.prompt = v; }

    public ScheduleFrequencyKind getFrequencyKind() {
        return EnumOrdinals.fromOrdinal(frequencyKind, ScheduleFrequencyKind.class);
    }
    public void setFrequencyKind(ScheduleFrequencyKind k) {
        if (k != null) this.frequencyKind = k.ordinal();
    }

    public @Nullable Integer getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(@Nullable Integer v) { this.intervalMinutes = v; }

    public @Nullable Integer getMinuteOfHour() { return minuteOfHour; }
    public void setMinuteOfHour(@Nullable Integer v) { this.minuteOfHour = v; }

    public @Nullable LocalTime getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(@Nullable LocalTime v) { this.timeOfDay = v; }

    public @Nullable Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(@Nullable Integer v) { this.dayOfWeek = v; }

    public @Nullable Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(@Nullable Integer v) { this.dayOfMonth = v; }

    public @Nullable String getCronExpression() { return cronExpression; }
    public void setCronExpression(@Nullable String v) { this.cronExpression = v; }

    public String getEffectiveCronExpression() { return effectiveCronExpression; }
    public void setEffectiveCronExpression(String v) { this.effectiveCronExpression = v; }

    public String getZoneId() { return zoneId; }
    public void setZoneId(String v) { this.zoneId = v; }

    public @Nullable LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(@Nullable LocalDateTime v) { this.startDate = v; }

    public ScheduleLifecycleKind getLifecycleKind() {
        return EnumOrdinals.fromOrdinal(lifecycleKind, ScheduleLifecycleKind.class);
    }
    public void setLifecycleKind(ScheduleLifecycleKind k) {
        if (k != null) this.lifecycleKind = k.ordinal();
    }

    public @Nullable Integer getMaxRuns() { return maxRuns; }
    public void setMaxRuns(@Nullable Integer v) { this.maxRuns = v; }

    public @Nullable Integer getRemainingRuns() { return remainingRuns; }
    public void setRemainingRuns(@Nullable Integer v) { this.remainingRuns = v; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int v) { this.consecutiveFailures = v; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public @Nullable LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(@Nullable LocalDateTime v) { this.lastRunAt = v; }

    public @Nullable LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(@Nullable LocalDateTime v) { this.nextRunAt = v; }

    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public int getVersion() { return version; }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        AiHubPersonalAgentSchedule that = (AiHubPersonalAgentSchedule) other;
        if (id == null) return this == that;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-api:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentSchedule.java
git commit -m "Add AiHubPersonalAgentSchedule domain class"
```

---

## Phase 4 — EE repository + service

### Task 9: Add platform-scheduler-api dependency

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts`

- [ ] **Step 1: Open the build file**

Find the `dependencies { ... }` block. Add the platform-scheduler-api dependency:

```kotlin
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
```

- [ ] **Step 2: Compile**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts
git commit -m "Add platform-scheduler-api dependency to automation-ai-hub-service"
```

### Task 10: Repository

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java`

- [ ] **Step 1: Write the repository**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent.repository;

import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubPersonalAgentScheduleRepository extends CrudRepository<AiHubPersonalAgentSchedule, Long> {

    List<AiHubPersonalAgentSchedule> findByAiHubPersonalAgentIdOrderByCreatedDateDesc(long aiHubPersonalAgentId);

    List<AiHubPersonalAgentSchedule> findByWorkspaceIdAndUserId(long workspaceId, long userId);

    @Query("SELECT * FROM ai_hub_personal_agent_schedule WHERE enabled = true")
    List<AiHubPersonalAgentSchedule> findAllEnabled();
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/repository/AiHubPersonalAgentScheduleRepository.java
git commit -m "Add AiHubPersonalAgentScheduleRepository"
```

### Task 11: ScheduleCronNormalizer (TDD)

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizer.java`
- Test: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizerTest.java`

- [ ] **Step 1: Write the failing test**

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

import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ScheduleCronNormalizerTest {

    @Test
    void testEveryXMinutesProducesIntervalCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.EVERY_X_MINUTES);
        schedule.setIntervalMinutes(15);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0/15 * * * ?");
    }

    @Test
    void testHourlyProducesMinuteOfHourCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.HOURLY);
        schedule.setMinuteOfHour(30);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 30 * * * ?");
    }

    @Test
    void testDailyProducesTimeOfDayCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 9 * * ?");
    }

    @Test
    void testWeeklyProducesDayOfWeekCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.WEEKLY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setDayOfWeek(1); // Monday (ISO)

        String cron = ScheduleCronNormalizer.normalize(schedule);

        // Quartz day-of-week: 1=Sunday … 7=Saturday — normalizer converts ISO Mon(1) → Quartz 2
        assertThat(cron).isEqualTo("0 0 9 ? * 2");
    }

    @Test
    void testMonthlyProducesDayOfMonthCron() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.MONTHLY);
        schedule.setTimeOfDay(LocalTime.of(9, 0));
        schedule.setDayOfMonth(15);

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 9 15 * ?");
    }

    @Test
    void testCustomCronPassesThroughAfterValidation() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.CUSTOM_CRON);
        schedule.setCronExpression("0 0 12 * * ?");

        String cron = ScheduleCronNormalizer.normalize(schedule);

        assertThat(cron).isEqualTo("0 0 12 * * ?");
    }

    @Test
    void testInvalidCustomCronThrows() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.CUSTOM_CRON);
        schedule.setCronExpression("not a cron");

        assertThatThrownBy(() -> ScheduleCronNormalizer.normalize(schedule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cron");
    }

    @Test
    void testEveryXMinutesWithMissingIntervalThrows() {
        AiHubPersonalAgentSchedule schedule = new AiHubPersonalAgentSchedule();
        schedule.setFrequencyKind(ScheduleFrequencyKind.EVERY_X_MINUTES);

        assertThatThrownBy(() -> ScheduleCronNormalizer.normalize(schedule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("intervalMinutes");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests ScheduleCronNormalizerTest
```

Expected: FAIL (class not found).

- [ ] **Step 3: Write the implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.Objects;
import org.quartz.CronExpression;

/**
 * Stateless helper that translates the structured frequency fields of
 * {@link AiHubPersonalAgentSchedule} into a 6-field Quartz cron expression
 * (seconds-resolution). {@code CUSTOM_CRON} is validated and passed through.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScheduleCronNormalizer {

    private ScheduleCronNormalizer() {}

    static String normalize(AiHubPersonalAgentSchedule schedule) {
        ScheduleFrequencyKind kind = schedule.getFrequencyKind();

        String cron = switch (kind) {
            case EVERY_X_MINUTES -> everyXMinutes(schedule.getIntervalMinutes());
            case HOURLY -> hourly(schedule.getMinuteOfHour());
            case DAILY -> daily(schedule.getTimeOfDay());
            case WEEKLY -> weekly(schedule.getTimeOfDay(), schedule.getDayOfWeek());
            case MONTHLY -> monthly(schedule.getTimeOfDay(), schedule.getDayOfMonth());
            case CUSTOM_CRON -> customCron(schedule.getCronExpression());
        };

        validate(cron);

        return cron;
    }

    private static String everyXMinutes(Integer interval) {
        require(interval != null && interval >= 1 && interval <= 59,
            "intervalMinutes must be 1..59");
        return "0 0/" + interval + " * * * ?";
    }

    private static String hourly(Integer minute) {
        require(minute != null && minute >= 0 && minute <= 59,
            "minuteOfHour must be 0..59");
        return "0 " + minute + " * * * ?";
    }

    private static String daily(LocalTime time) {
        require(time != null, "timeOfDay required for DAILY");
        return "0 " + time.getMinute() + " " + time.getHour() + " * * ?";
    }

    private static String weekly(LocalTime time, Integer isoDayOfWeek) {
        require(time != null, "timeOfDay required for WEEKLY");
        require(isoDayOfWeek != null && isoDayOfWeek >= 1 && isoDayOfWeek <= 7,
            "dayOfWeek must be 1..7 (ISO)");
        // ISO: 1=Mon … 7=Sun. Quartz: 1=Sun … 7=Sat. Convert: quartz = (iso % 7) + 1.
        int quartzDay = (isoDayOfWeek % 7) + 1;
        return "0 " + time.getMinute() + " " + time.getHour() + " ? * " + quartzDay;
    }

    private static String monthly(LocalTime time, Integer dayOfMonth) {
        require(time != null, "timeOfDay required for MONTHLY");
        require(dayOfMonth != null && dayOfMonth >= 1 && dayOfMonth <= 31,
            "dayOfMonth must be 1..31");
        return "0 " + time.getMinute() + " " + time.getHour() + " " + dayOfMonth + " * ?";
    }

    private static String customCron(String expression) {
        require(expression != null && !expression.isBlank(), "cronExpression required for CUSTOM_CRON");
        return Objects.requireNonNull(expression).trim();
    }

    private static void validate(String cron) {
        try {
            new CronExpression(cron);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests ScheduleCronNormalizerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizer.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/ScheduleCronNormalizerTest.java
git commit -m "Add ScheduleCronNormalizer with frequency-kind translation"
```

### Task 12: Service interface + impl

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`
- Test: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java`

- [ ] **Step 1: Write the service interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.personalagent;

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

    AiHubPersonalAgentSchedule toggle(long scheduleId, long workspaceId, long userId, boolean enabled);

    void delete(long scheduleId, long workspaceId, long userId);

    Optional<AiHubPersonalAgentSchedule> findById(long scheduleId);

    List<AiHubPersonalAgentSchedule> findByAgent(long agentId);

    /**
     * Called by listener on each fire; updates last_run_at, decrements remaining_runs,
     * recomputes next_run_at. Returns {@code true} if the schedule is still enabled
     * after this update; {@code false} when it auto-disabled (remaining_runs hit zero).
     */
    boolean recordFire(long scheduleId);

    /** Called by listener on dispatch failure. */
    void recordFailure(long scheduleId);

    @Nullable
    AiHubPersonalAgentSchedule findEnabled(long scheduleId);
}
```

- [ ] **Step 2: Write the failing service test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleLifecycleKind;
import com.bytechef.platform.scheduler.AgentScheduler;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AiHubPersonalAgentScheduleServiceImplTest {

    private AiHubPersonalAgentScheduleRepository repository;
    private AgentScheduler agentScheduler;
    private AiHubPersonalAgentScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AiHubPersonalAgentScheduleRepository.class);
        agentScheduler = Mockito.mock(AgentScheduler.class);
        service = new AiHubPersonalAgentScheduleServiceImpl(repository, agentScheduler);
    }

    @Test
    void testCreateNormalizesCronAndRegistersQuartzJob() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        when(repository.save(any())).thenAnswer(invocation -> {
            AiHubPersonalAgentSchedule saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getEffectiveCronExpression()).isEqualTo("0 0 9 * * ?");
        verify(agentScheduler).scheduleAgentRun(42L, "0 0 9 * * ?", "Europe/Zagreb", null);
    }

    @Test
    void testCreateRecurringClearsMaxRuns() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        input.setMaxRuns(99); // user error: set max_runs while choosing Recurring
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getMaxRuns()).isNull();
        assertThat(result.getRemainingRuns()).isNull();
    }

    @Test
    void testCreateNumberOfRunsMirrorsMaxIntoRemaining() {
        AiHubPersonalAgentSchedule input = buildDaily9AmRecurring();
        input.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        input.setMaxRuns(5);
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        AiHubPersonalAgentSchedule result = service.create(input);

        assertThat(result.getRemainingRuns()).isEqualTo(5);
    }

    @Test
    void testRecordFireDecrementsAndReturnsTrueWhenStillBudget() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        existing.setMaxRuns(3);
        existing.setRemainingRuns(3);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        boolean stillEnabled = service.recordFire(7L);

        assertThat(stillEnabled).isTrue();
        assertThat(existing.getRemainingRuns()).isEqualTo(2);
        verify(agentScheduler, never()).cancelAgentRun(anyLong());
    }

    @Test
    void testRecordFireDisablesAndCancelsAtZero() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setLifecycleKind(ScheduleLifecycleKind.NUMBER_OF_RUNS);
        existing.setMaxRuns(1);
        existing.setRemainingRuns(1);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        boolean stillEnabled = service.recordFire(7L);

        assertThat(stillEnabled).isFalse();
        assertThat(existing.isEnabled()).isFalse();
        verify(agentScheduler, times(1)).cancelAgentRun(7L);
    }

    @Test
    void testRecordFailureAtThreeStrikesDisables() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        existing.setConsecutiveFailures(2);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.<AiHubPersonalAgentSchedule>getArgument(0));

        service.recordFailure(7L);

        assertThat(existing.getConsecutiveFailures()).isEqualTo(3);
        assertThat(existing.isEnabled()).isFalse();
        verify(agentScheduler).cancelAgentRun(7L);
    }

    @Test
    void testDeleteCancelsQuartzJob() {
        AiHubPersonalAgentSchedule existing = buildDaily9AmRecurring();
        existing.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));

        service.delete(7L, existing.getWorkspaceId(), existing.getUserId());

        verify(agentScheduler).cancelAgentRun(7L);
        verify(repository).deleteById(7L);
    }

    private AiHubPersonalAgentSchedule buildDaily9AmRecurring() {
        AiHubPersonalAgentSchedule s = new AiHubPersonalAgentSchedule();
        s.setAiHubPersonalAgentId(100L);
        s.setWorkspaceId(1L);
        s.setUserId(2L);
        s.setEnvironment(com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT);
        s.setTitle("Daily report");
        s.setPrompt("Summarize yesterday");
        s.setFrequencyKind(ScheduleFrequencyKind.DAILY);
        s.setTimeOfDay(LocalTime.of(9, 0));
        s.setZoneId("Europe/Zagreb");
        s.setLifecycleKind(ScheduleLifecycleKind.RECURRING);
        return s;
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests AiHubPersonalAgentScheduleServiceImplTest
```

Expected: FAIL (class not found).

- [ ] **Step 4: Write the impl**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import com.bytechef.ee.automation.aihub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.platform.aihub.personalagent.ScheduleLifecycleKind;
import com.bytechef.platform.scheduler.AgentScheduler;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class AiHubPersonalAgentScheduleServiceImpl implements AiHubPersonalAgentScheduleService {

    private static final int FAILURE_DISABLE_THRESHOLD = 3;

    private final AiHubPersonalAgentScheduleRepository repository;
    private final AgentScheduler agentScheduler;

    public AiHubPersonalAgentScheduleServiceImpl(
        AiHubPersonalAgentScheduleRepository repository, AgentScheduler agentScheduler) {

        this.repository = repository;
        this.agentScheduler = agentScheduler;
    }

    @Override
    @Transactional
    public AiHubPersonalAgentSchedule create(AiHubPersonalAgentSchedule schedule) {
        normalizeOnWrite(schedule);

        AiHubPersonalAgentSchedule saved = repository.save(schedule);

        Instant startAt = saved.getStartDate() != null
            ? saved.getStartDate().atZone(ZoneId.of(saved.getZoneId())).toInstant()
            : null;

        agentScheduler.scheduleAgentRun(
            saved.getId(), saved.getEffectiveCronExpression(), saved.getZoneId(), startAt);

        saved.setNextRunAt(computeNextRunAt(saved.getEffectiveCronExpression(), saved.getZoneId(), startAt));

        return repository.save(saved);
    }

    @Override
    @Transactional
    public AiHubPersonalAgentSchedule update(AiHubPersonalAgentSchedule schedule) {
        normalizeOnWrite(schedule);

        AiHubPersonalAgentSchedule saved = repository.save(schedule);

        Instant startAt = saved.getStartDate() != null
            ? saved.getStartDate().atZone(ZoneId.of(saved.getZoneId())).toInstant()
            : null;

        if (saved.isEnabled()) {
            agentScheduler.rescheduleAgentRun(
                saved.getId(), saved.getEffectiveCronExpression(), saved.getZoneId(), startAt);
            saved.setNextRunAt(computeNextRunAt(saved.getEffectiveCronExpression(), saved.getZoneId(), startAt));
        } else {
            agentScheduler.cancelAgentRun(saved.getId());
            saved.setNextRunAt(null);
        }

        return repository.save(saved);
    }

    @Override
    @Transactional
    public AiHubPersonalAgentSchedule toggle(long scheduleId, long workspaceId, long userId, boolean enabled) {
        AiHubPersonalAgentSchedule schedule = requireOwned(scheduleId, workspaceId, userId);
        schedule.setEnabled(enabled);

        if (enabled) {
            Instant startAt = schedule.getStartDate() != null
                ? schedule.getStartDate().atZone(ZoneId.of(schedule.getZoneId())).toInstant()
                : null;
            agentScheduler.scheduleAgentRun(
                schedule.getId(), schedule.getEffectiveCronExpression(), schedule.getZoneId(), startAt);
            schedule.setNextRunAt(computeNextRunAt(
                schedule.getEffectiveCronExpression(), schedule.getZoneId(), startAt));
        } else {
            agentScheduler.cancelAgentRun(scheduleId);
            schedule.setNextRunAt(null);
        }

        return repository.save(schedule);
    }

    @Override
    @Transactional
    public void delete(long scheduleId, long workspaceId, long userId) {
        AiHubPersonalAgentSchedule schedule = requireOwned(scheduleId, workspaceId, userId);
        agentScheduler.cancelAgentRun(scheduleId);
        repository.deleteById(schedule.getId());
    }

    @Override
    public Optional<AiHubPersonalAgentSchedule> findById(long scheduleId) {
        return repository.findById(scheduleId);
    }

    @Override
    public List<AiHubPersonalAgentSchedule> findByAgent(long agentId) {
        return repository.findByAiHubPersonalAgentIdOrderByCreatedDateDesc(agentId);
    }

    @Override
    @Transactional
    public boolean recordFire(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        schedule.setLastRunAt(LocalDateTime.now());
        schedule.setConsecutiveFailures(0);

        if (schedule.getRemainingRuns() != null) {
            int remaining = schedule.getRemainingRuns() - 1;
            schedule.setRemainingRuns(remaining);

            if (remaining <= 0) {
                schedule.setEnabled(false);
                schedule.setNextRunAt(null);

                agentScheduler.cancelAgentRun(scheduleId);
                repository.save(schedule);

                return false;
            }
        }

        schedule.setNextRunAt(
            computeNextRunAt(schedule.getEffectiveCronExpression(), schedule.getZoneId(), null));

        repository.save(schedule);

        return true;
    }

    @Override
    @Transactional
    public void recordFailure(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalStateException("Schedule not found: " + scheduleId));

        schedule.setConsecutiveFailures(schedule.getConsecutiveFailures() + 1);

        if (schedule.getConsecutiveFailures() >= FAILURE_DISABLE_THRESHOLD) {
            schedule.setEnabled(false);
            schedule.setNextRunAt(null);

            agentScheduler.cancelAgentRun(scheduleId);
        }

        repository.save(schedule);
    }

    @Override
    @Nullable
    public AiHubPersonalAgentSchedule findEnabled(long scheduleId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId).orElse(null);

        return (schedule != null && schedule.isEnabled()) ? schedule : null;
    }

    private void normalizeOnWrite(AiHubPersonalAgentSchedule schedule) {
        schedule.setEffectiveCronExpression(ScheduleCronNormalizer.normalize(schedule));

        clearOffFrequencyFields(schedule);

        if (schedule.getLifecycleKind() == ScheduleLifecycleKind.RECURRING) {
            schedule.setMaxRuns(null);
            schedule.setRemainingRuns(null);
        } else if (schedule.getMaxRuns() != null) {
            schedule.setRemainingRuns(schedule.getMaxRuns());
        } else {
            schedule.setRemainingRuns(null);
        }
    }

    private void clearOffFrequencyFields(AiHubPersonalAgentSchedule schedule) {
        ScheduleFrequencyKind kind = schedule.getFrequencyKind();

        if (kind != ScheduleFrequencyKind.EVERY_X_MINUTES) schedule.setIntervalMinutes(null);
        if (kind != ScheduleFrequencyKind.HOURLY) schedule.setMinuteOfHour(null);
        if (kind == ScheduleFrequencyKind.EVERY_X_MINUTES || kind == ScheduleFrequencyKind.HOURLY
            || kind == ScheduleFrequencyKind.CUSTOM_CRON) schedule.setTimeOfDay(null);
        if (kind != ScheduleFrequencyKind.WEEKLY) schedule.setDayOfWeek(null);
        if (kind != ScheduleFrequencyKind.MONTHLY) schedule.setDayOfMonth(null);
        if (kind != ScheduleFrequencyKind.CUSTOM_CRON) schedule.setCronExpression(null);
    }

    private AiHubPersonalAgentSchedule requireOwned(long scheduleId, long workspaceId, long userId) {
        AiHubPersonalAgentSchedule schedule = repository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        if (schedule.getWorkspaceId() != workspaceId || schedule.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule not owned by caller: " + scheduleId);
        }

        return schedule;
    }

    private @Nullable LocalDateTime computeNextRunAt(String cron, String zoneId, @Nullable Instant startAt) {
        try {
            CronExpression expression = new CronExpression(cron);
            expression.setTimeZone(java.util.TimeZone.getTimeZone(zoneId));

            Date after = startAt != null
                ? Date.from(startAt.isAfter(Instant.now()) ? startAt : Instant.now())
                : new Date();

            Date next = expression.getNextValidTimeAfter(after);

            return next != null ? LocalDateTime.ofInstant(next.toInstant(), ZoneId.of(zoneId)) : null;
        } catch (ParseException e) {
            return null;
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests AiHubPersonalAgentScheduleServiceImplTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-api/src/main/java/com/bytechef/ee/platform/aihub/personalagent/AiHubPersonalAgentScheduleService.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java
git commit -m "Add AiHubPersonalAgentScheduleService with normalization and lifecycle logic"
```

---

## Phase 5 — EE listener + dispatcher

### Task 13: AiHubScheduledChatDispatcher

This is the non-HTTP entry point for posting a user turn to the LLM agent from a background thread. It wraps `AiHubChatStreamer.runAgent(...)` by synthesizing an `AgUiParameters` and discarding the returned `SseEmitter`.

**Files:**
- Create: `server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubScheduledChatDispatcher.java`

- [ ] **Step 1: Read AiHubChatStreamer.runAgent to confirm the call shape**

```bash
sed -n '80,115p' server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubChatStreamer.java
```

Note the exact signature and any state/context the streamer expects in `AgUiParameters`. The dispatcher must populate equivalents to what the REST controller's `injectAuthenticatedContext(...)` helper adds (userId, workspaceId, threadId on the params' state/context).

- [ ] **Step 2: Read `injectAuthenticatedContext` for the context-injection contract**

```bash
grep -n "injectAuthenticatedContext" server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/rest/AiHubApiController.java
```

Open the method body. The dispatcher must mirror what it does (set userId / workspaceId / threadId into the params' state object).

- [ ] **Step 3: Write the dispatcher**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.aihub.agent;

import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.ee.platform.aihub.task.AiHubTaskKind;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Non-HTTP entry point for posting a user turn to an AI Hub agent. Used by the
 * scheduled-run listener — Quartz fires, listener creates a task, listener calls
 * this dispatcher with (agentId, threadId, prompt, userId, workspaceId).
 *
 * <p>Synthesizes an {@link AgUiParameters} that mirrors what
 * {@code AiHubApiController.chat(...)} would build after auth + ownership checks,
 * then calls {@link AiHubChatStreamer#runAgent(LocalAgent, AgUiParameters, String)}.
 * The returned {@code SseEmitter} is discarded — the agent runs to completion and
 * writes to {@code SPRING_AI_CHAT_MEMORY} regardless of whether anyone is listening
 * to the stream.</p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubScheduledChatDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(AiHubScheduledChatDispatcher.class);

    private final AiHubChatStreamer chatStreamer;
    private final Map<String, LocalAgent> localAgentMap;

    public AiHubScheduledChatDispatcher(AiHubChatStreamer chatStreamer, List<LocalAgent> localAgents) {
        this.chatStreamer = chatStreamer;
        this.localAgentMap = localAgents.stream()
            .collect(java.util.stream.Collectors.toMap(LocalAgent::getAgentId, a -> a));
    }

    public void dispatch(
        long userId, long workspaceId, int environment, long aiHubPersonalAgentId,
        String threadId, String prompt) {

        LocalAgent agent = localAgentMap.get("ai_hub_ask");

        if (agent == null) {
            throw new IllegalStateException("ai_hub_ask agent not registered");
        }

        AgUiParameters params = new AgUiParameters();
        params.setThreadId(threadId);
        params.setRunId(UUID.randomUUID().toString());
        params.setMessages(List.of(new UserMessage(UUID.randomUUID().toString(), prompt, null)));
        params.setTools(List.of());
        params.setContext(List.of());

        State state = new State();
        // Mirror AiHubApiController.injectAuthenticatedContext keys — confirm exact key
        // names by reading the helper before this commits.
        state.put("userId", userId);
        state.put("workspaceId", workspaceId);
        state.put("environment", environment);
        state.put("threadId", threadId);
        state.put("aiHubTaskKind", AiHubTaskKind.PERSONAL_AGENT.name());
        state.put("aiHubPersonalAgentId", aiHubPersonalAgentId);
        params.setState(state);

        logger.debug(
            "Dispatching scheduled chat: agent={}, threadId={}, userId={}, workspaceId={}",
            agent.getAgentId(), threadId, userId, workspaceId);

        chatStreamer.runAgent(agent, params, threadId);
    }
}
```

> **Note on State key names:** the comment in step 3 calls out that key names mirror `injectAuthenticatedContext`. Read that helper (step 2) and adjust the keys here to match exactly — the routing agent reads state by key and will silently produce wrong behavior if a key is misspelled. If the helper uses a different key like `auth.userId`, change accordingly.

- [ ] **Step 4: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-ai-hub:platform-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-ai-hub/platform-ai-hub-service/src/main/java/com/bytechef/ee/platform/aihub/agent/AiHubScheduledChatDispatcher.java
git commit -m "Add AiHubScheduledChatDispatcher for non-HTTP agent invocation"
```

### Task 14: AgentScheduleFiredEventListener (TDD)

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java`
- Test: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListenerTest.java`

- [ ] **Step 1: Write the failing listener test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.aihub.agent.AiHubScheduledChatDispatcher;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.platform.aihub.task.AiHubTask;
import com.bytechef.ee.platform.aihub.task.AiHubTaskService;
import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AgentScheduleFiredEventListenerTest {

    private AiHubPersonalAgentScheduleService scheduleService;
    private AiHubTaskService taskService;
    private AiHubScheduledChatDispatcher dispatcher;
    private AgentScheduleFiredEventListener listener;

    @BeforeEach
    void setUp() {
        scheduleService = Mockito.mock(AiHubPersonalAgentScheduleService.class);
        taskService = Mockito.mock(AiHubTaskService.class);
        dispatcher = Mockito.mock(AiHubScheduledChatDispatcher.class);
        listener = new AgentScheduleFiredEventListener(scheduleService, taskService, dispatcher);
    }

    @Test
    void testHappyPathCreatesTaskAndDispatches() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        AiHubTask task = mockTaskWithThread("thread-uuid");
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            eq(1L), eq(2L), anyInt(), eq(100L), eq("Daily report")))
                .thenReturn(task);

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(taskService).createAiHubPersonalAgentChat(1L, 2L, 0, 100L, "Daily report");
        verify(dispatcher).dispatch(2L, 1L, 0, 100L, "thread-uuid", "Summarize yesterday");
        verify(scheduleService).recordFire(7L);
        verify(scheduleService, never()).recordFailure(anyLong());
    }

    @Test
    void testDisabledOrMissingScheduleSkips() {
        when(scheduleService.findEnabled(7L)).thenReturn(null);

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(taskService, never()).createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any());
        verify(dispatcher, never()).dispatch(
            anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());
        verify(scheduleService, never()).recordFire(anyLong());
    }

    @Test
    void testDispatchFailureRecordsFailure() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        AiHubTask task = mockTaskWithThread("thread-uuid");
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any())).thenReturn(task);
        Mockito.doThrow(new RuntimeException("boom"))
            .when(dispatcher).dispatch(anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(scheduleService).recordFailure(7L);
        verify(scheduleService, never()).recordFire(anyLong());
    }

    @Test
    void testTaskCreationFailureRecordsFailure() {
        AiHubPersonalAgentSchedule schedule = buildEnabledSchedule(7L);
        when(scheduleService.findEnabled(7L)).thenReturn(schedule);
        when(taskService.createAiHubPersonalAgentChat(
            anyLong(), anyLong(), anyInt(), anyLong(), any()))
                .thenThrow(new RuntimeException("DB down"));

        listener.onFired(new AgentScheduleFiredEvent(7L));

        verify(scheduleService).recordFailure(7L);
        verify(dispatcher, never()).dispatch(
            anyLong(), anyLong(), anyInt(), anyLong(), anyString(), anyString());
    }

    private AiHubPersonalAgentSchedule buildEnabledSchedule(long id) {
        AiHubPersonalAgentSchedule s = new AiHubPersonalAgentSchedule();
        s.setId(id);
        s.setAiHubPersonalAgentId(100L);
        s.setWorkspaceId(1L);
        s.setUserId(2L);
        s.setEnvironment(com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT);
        s.setTitle("Daily report");
        s.setPrompt("Summarize yesterday");
        return s;
    }

    private AiHubTask mockTaskWithThread(String threadId) {
        AiHubTask task = Mockito.mock(AiHubTask.class);
        when(task.getThreadId()).thenReturn(threadId);
        return task;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests AgentScheduleFiredEventListenerTest
```

Expected: FAIL (class not found).

- [ ] **Step 3: Write the listener**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.personalagent;

import com.bytechef.ee.platform.aihub.agent.AiHubScheduledChatDispatcher;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentSchedule;
import com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.platform.aihub.task.AiHubTask;
import com.bytechef.ee.platform.aihub.task.AiHubTaskService;
import com.bytechef.platform.scheduler.event.AgentScheduleFiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class AgentScheduleFiredEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AgentScheduleFiredEventListener.class);

    private final AiHubPersonalAgentScheduleService scheduleService;
    private final AiHubTaskService taskService;
    private final AiHubScheduledChatDispatcher dispatcher;

    AgentScheduleFiredEventListener(
        AiHubPersonalAgentScheduleService scheduleService, AiHubTaskService taskService,
        AiHubScheduledChatDispatcher dispatcher) {

        this.scheduleService = scheduleService;
        this.taskService = taskService;
        this.dispatcher = dispatcher;
    }

    @EventListener
    void onFired(AgentScheduleFiredEvent event) {
        long scheduleId = event.agentScheduleId();

        AiHubPersonalAgentSchedule schedule = scheduleService.findEnabled(scheduleId);

        if (schedule == null) {
            logger.debug("Schedule {} missing or disabled — skipping fire", scheduleId);
            return;
        }

        try {
            AiHubTask task = taskService.createAiHubPersonalAgentChat(
                schedule.getWorkspaceId(),
                schedule.getUserId(),
                schedule.getEnvironment().ordinal(),
                schedule.getAiHubPersonalAgentId(),
                schedule.getTitle());

            dispatcher.dispatch(
                schedule.getUserId(),
                schedule.getWorkspaceId(),
                schedule.getEnvironment().ordinal(),
                schedule.getAiHubPersonalAgentId(),
                task.getThreadId(),
                schedule.getPrompt());

            scheduleService.recordFire(scheduleId);

            logger.info(
                "Scheduled fire ok: scheduleId={}, agentId={}, taskId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), task.getId());
        } catch (Exception e) {
            logger.warn(
                "Scheduled fire failed: scheduleId={}, agentId={}, workspaceId={}",
                scheduleId, schedule.getAiHubPersonalAgentId(), schedule.getWorkspaceId(), e);

            scheduleService.recordFailure(scheduleId);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests AgentScheduleFiredEventListenerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListener.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AgentScheduleFiredEventListenerTest.java
git commit -m "Add AgentScheduleFiredEventListener with TDD coverage"
```

### Task 15: Boot reconciler + metrics

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java`

- [ ] **Step 1: Add reconciliation method**

Open the impl file. Add these imports at the top:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
```

Add `MeterRegistry` to the constructor (via `ObjectProvider` so it's optional):

```java
    private final @Nullable MeterRegistry meterRegistry;

    public AiHubPersonalAgentScheduleServiceImpl(
        AiHubPersonalAgentScheduleRepository repository, AgentScheduler agentScheduler,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.repository = repository;
        this.agentScheduler = agentScheduler;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }
```

Add the reconciler method:

```java
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        List<AiHubPersonalAgentSchedule> enabled = repository.findAllEnabled();

        int reregistered = 0;
        for (AiHubPersonalAgentSchedule schedule : enabled) {
            if (!agentScheduler.exists(schedule.getId())) {
                Instant startAt = schedule.getStartDate() != null
                    ? schedule.getStartDate().atZone(ZoneId.of(schedule.getZoneId())).toInstant()
                    : null;
                agentScheduler.scheduleAgentRun(
                    schedule.getId(), schedule.getEffectiveCronExpression(), schedule.getZoneId(), startAt);
                reregistered++;
            }
        }

        if (reregistered > 0) {
            org.slf4j.LoggerFactory.getLogger(AiHubPersonalAgentScheduleServiceImpl.class)
                .info("Reconciled {} agent schedule(s) at boot", reregistered);
        }
    }
```

Add metric increments to `recordFire` (success) and `recordFailure` (failed):

In `recordFire`, just before each `return`:
```java
        counter("success");
```

In `recordFailure`, after the failure handling:
```java
        counter("failed");
```

And the helper:
```java
    private void counter(String outcome) {
        if (meterRegistry != null) {
            Counter.builder("bytechef_ai_hub_agent_schedule_fire")
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        }
    }
```

- [ ] **Step 2: Update the constructor in the test** to pass `Mockito.mock(ObjectProvider.class)` (with `getIfAvailable()` returning `null`). Re-run the service test to confirm it still passes:

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests AiHubPersonalAgentScheduleServiceImplTest
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImpl.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/personalagent/AiHubPersonalAgentScheduleServiceImplTest.java
git commit -m "Add boot reconciler and fire-outcome metrics to schedule service"
```

---

## Phase 6 — GraphQL surface

### Task 16: GraphQL schema additions

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls`

- [ ] **Step 1: Write the schema**

```graphql
"Scheduled run configuration for a personal agent."
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

input CreateAiHubPersonalAgentScheduleInput {
    workspaceId: ID!
    aiHubPersonalAgentId: ID!
    environment: Int!
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

input UpdateAiHubPersonalAgentScheduleInput {
    workspaceId: ID!
    id: ID!
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
    enabled: Boolean!
}

extend type Query {
    aiHubPersonalAgentSchedules(workspaceId: ID!, agentId: ID!): [AiHubPersonalAgentSchedule!]!
    aiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!): AiHubPersonalAgentSchedule
}

extend type Mutation {
    createAiHubPersonalAgentSchedule(input: CreateAiHubPersonalAgentScheduleInput!): AiHubPersonalAgentSchedule!
    updateAiHubPersonalAgentSchedule(input: UpdateAiHubPersonalAgentScheduleInput!): AiHubPersonalAgentSchedule!
    deleteAiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!): Boolean!
    toggleAiHubPersonalAgentSchedule(workspaceId: ID!, scheduleId: ID!, enabled: Boolean!): AiHubPersonalAgentSchedule!
}
```

- [ ] **Step 2: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls
git commit -m "Add GraphQL schema for personal-agent schedules"
```

### Task 17: GraphQL controller methods

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java`

- [ ] **Step 1: Open the controller and read the existing pattern**

```bash
grep -n "@QueryMapping\|@MutationMapping\|userService.getCurrentUser" \
  server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java
```

Confirm: every method extracts `long userId = userService.getCurrentUser().getId();` and then delegates to a service after ownership check.

- [ ] **Step 2: Add scheduleService field + constructor injection**

In the controller:

```java
    private final AiHubPersonalAgentScheduleService scheduleService;
    private final AiHubPersonalAgentService personalAgentService;  // already injected — reuse

    // Add to constructor:
    //   ..., AiHubPersonalAgentScheduleService scheduleService) {
    //     this.scheduleService = scheduleService;
    //     ...
```

- [ ] **Step 3: Add query methods**

```java
    @QueryMapping
    public List<AiHubPersonalAgentSchedule> aiHubPersonalAgentSchedules(
        @Argument long workspaceId, @Argument long agentId) {

        long userId = userService.getCurrentUser().getId();

        // Ownership: caller must own the agent.
        personalAgentService.findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        return scheduleService.findByAgent(agentId).stream()
            .filter(s -> s.getUserId() == userId && s.getWorkspaceId() == workspaceId)
            .toList();
    }

    @QueryMapping
    public AiHubPersonalAgentSchedule aiHubPersonalAgentSchedule(
        @Argument long workspaceId, @Argument long scheduleId) {

        long userId = userService.getCurrentUser().getId();

        AiHubPersonalAgentSchedule schedule = scheduleService.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        if (schedule.getWorkspaceId() != workspaceId || schedule.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule not found");
        }

        return schedule;
    }
```

- [ ] **Step 4: Add mutation methods**

```java
    @MutationMapping
    public AiHubPersonalAgentSchedule createAiHubPersonalAgentSchedule(
        @Argument CreateAiHubPersonalAgentScheduleInput input) {

        long userId = userService.getCurrentUser().getId();

        personalAgentService.findOwned(input.aiHubPersonalAgentId(), input.workspaceId(), userId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        AiHubPersonalAgentSchedule schedule = inputToEntity(input, userId);

        return scheduleService.create(schedule);
    }

    @MutationMapping
    public AiHubPersonalAgentSchedule updateAiHubPersonalAgentSchedule(
        @Argument UpdateAiHubPersonalAgentScheduleInput input) {

        long userId = userService.getCurrentUser().getId();

        AiHubPersonalAgentSchedule existing = scheduleService.findById(input.id())
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        if (existing.getWorkspaceId() != input.workspaceId() || existing.getUserId() != userId) {
            throw new IllegalArgumentException("Schedule not found");
        }

        applyUpdate(existing, input);

        return scheduleService.update(existing);
    }

    @MutationMapping
    public boolean deleteAiHubPersonalAgentSchedule(
        @Argument long workspaceId, @Argument long scheduleId) {

        long userId = userService.getCurrentUser().getId();

        scheduleService.delete(scheduleId, workspaceId, userId);

        return true;
    }

    @MutationMapping
    public AiHubPersonalAgentSchedule toggleAiHubPersonalAgentSchedule(
        @Argument long workspaceId, @Argument long scheduleId, @Argument boolean enabled) {

        long userId = userService.getCurrentUser().getId();

        return scheduleService.toggle(scheduleId, workspaceId, userId, enabled);
    }
```

- [ ] **Step 5: Add the two input record types (in the same file or a new `inputs/` package)**

```java
public record CreateAiHubPersonalAgentScheduleInput(
    long workspaceId, long aiHubPersonalAgentId, int environment,
    String title, String prompt,
    ScheduleFrequencyKind frequencyKind,
    @Nullable Integer intervalMinutes, @Nullable Integer minuteOfHour, @Nullable String timeOfDay,
    @Nullable Integer dayOfWeek, @Nullable Integer dayOfMonth, @Nullable String cronExpression,
    String zoneId, @Nullable String startDate,
    ScheduleLifecycleKind lifecycleKind, @Nullable Integer maxRuns) {}

public record UpdateAiHubPersonalAgentScheduleInput(
    long workspaceId, long id, String title, String prompt,
    ScheduleFrequencyKind frequencyKind,
    @Nullable Integer intervalMinutes, @Nullable Integer minuteOfHour, @Nullable String timeOfDay,
    @Nullable Integer dayOfWeek, @Nullable Integer dayOfMonth, @Nullable String cronExpression,
    String zoneId, @Nullable String startDate,
    ScheduleLifecycleKind lifecycleKind, @Nullable Integer maxRuns, boolean enabled) {}
```

- [ ] **Step 6: Add the `inputToEntity` and `applyUpdate` helpers**

```java
private AiHubPersonalAgentSchedule inputToEntity(
    CreateAiHubPersonalAgentScheduleInput input, long userId) {

    AiHubPersonalAgentSchedule s = new AiHubPersonalAgentSchedule();
    s.setAiHubPersonalAgentId(input.aiHubPersonalAgentId());
    s.setWorkspaceId(input.workspaceId());
    s.setUserId(userId);
    s.setEnvironment(Environment.values()[input.environment()]);
    s.setTitle(input.title());
    s.setPrompt(input.prompt());
    s.setFrequencyKind(input.frequencyKind());
    s.setIntervalMinutes(input.intervalMinutes());
    s.setMinuteOfHour(input.minuteOfHour());
    s.setTimeOfDay(input.timeOfDay() != null ? LocalTime.parse(input.timeOfDay()) : null);
    s.setDayOfWeek(input.dayOfWeek());
    s.setDayOfMonth(input.dayOfMonth());
    s.setCronExpression(input.cronExpression());
    s.setZoneId(input.zoneId());
    s.setStartDate(input.startDate() != null ? LocalDateTime.parse(input.startDate()) : null);
    s.setLifecycleKind(input.lifecycleKind());
    s.setMaxRuns(input.maxRuns());
    return s;
}

private void applyUpdate(
    AiHubPersonalAgentSchedule existing, UpdateAiHubPersonalAgentScheduleInput input) {

    existing.setTitle(input.title());
    existing.setPrompt(input.prompt());
    existing.setFrequencyKind(input.frequencyKind());
    existing.setIntervalMinutes(input.intervalMinutes());
    existing.setMinuteOfHour(input.minuteOfHour());
    existing.setTimeOfDay(input.timeOfDay() != null ? LocalTime.parse(input.timeOfDay()) : null);
    existing.setDayOfWeek(input.dayOfWeek());
    existing.setDayOfMonth(input.dayOfMonth());
    existing.setCronExpression(input.cronExpression());
    existing.setZoneId(input.zoneId());
    existing.setStartDate(input.startDate() != null ? LocalDateTime.parse(input.startDate()) : null);
    existing.setLifecycleKind(input.lifecycleKind());
    existing.setMaxRuns(input.maxRuns());
    existing.setEnabled(input.enabled());
}
```

- [ ] **Step 7: Compile and run all module tests**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-graphql:check
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Smoke-test the GraphQL endpoint**

Start the server, log in, and run a curl against the GraphQL endpoint with a `createAiHubPersonalAgentSchedule` mutation. Expected: 200 with the new schedule object. (Detailed curl is omitted — the React UI work below exercises the same surface.)

- [ ] **Step 9: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/java/com/bytechef/ee/automation/aihub/web/graphql/AiHubPersonalAgentGraphQlController.java
git commit -m "Add GraphQL controller methods for personal-agent schedules"
```

---

## Phase 7 — Client tab + dialog

### Task 18: GraphQL operation files + codegen

**Files:**
- Create six `.graphql` files under `client/src/graphql/ai-hub/` (one per query/mutation)
- Modify: `client/codegen.ts`

- [ ] **Step 1: Add the new graphqls path to codegen**

Open `client/codegen.ts`. In the `schema` array, add the new schema path:

```ts
"../server/ee/libs/automation/automation-ai-hub/automation-ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent-schedule.graphqls",
```

- [ ] **Step 2: Write the six operation files**

`client/src/graphql/ai-hub/aiHubPersonalAgentSchedules.graphql`:
```graphql
query AiHubPersonalAgentSchedules($workspaceId: ID!, $agentId: ID!) {
  aiHubPersonalAgentSchedules(workspaceId: $workspaceId, agentId: $agentId) {
    ...AiHubPersonalAgentScheduleFields
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

`aiHubPersonalAgentSchedule.graphql`:
```graphql
query AiHubPersonalAgentSchedule($workspaceId: ID!, $scheduleId: ID!) {
  aiHubPersonalAgentSchedule(workspaceId: $workspaceId, scheduleId: $scheduleId) {
    ...AiHubPersonalAgentScheduleFields
  }
}
```

`createAiHubPersonalAgentSchedule.graphql`:
```graphql
mutation CreateAiHubPersonalAgentSchedule($input: CreateAiHubPersonalAgentScheduleInput!) {
  createAiHubPersonalAgentSchedule(input: $input) {
    ...AiHubPersonalAgentScheduleFields
  }
}
```

`updateAiHubPersonalAgentSchedule.graphql`:
```graphql
mutation UpdateAiHubPersonalAgentSchedule($input: UpdateAiHubPersonalAgentScheduleInput!) {
  updateAiHubPersonalAgentSchedule(input: $input) {
    ...AiHubPersonalAgentScheduleFields
  }
}
```

`deleteAiHubPersonalAgentSchedule.graphql`:
```graphql
mutation DeleteAiHubPersonalAgentSchedule($workspaceId: ID!, $scheduleId: ID!) {
  deleteAiHubPersonalAgentSchedule(workspaceId: $workspaceId, scheduleId: $scheduleId)
}
```

`toggleAiHubPersonalAgentSchedule.graphql`:
```graphql
mutation ToggleAiHubPersonalAgentSchedule($workspaceId: ID!, $scheduleId: ID!, $enabled: Boolean!) {
  toggleAiHubPersonalAgentSchedule(workspaceId: $workspaceId, scheduleId: $scheduleId, enabled: $enabled) {
    ...AiHubPersonalAgentScheduleFields
  }
}
```

- [ ] **Step 3: Run codegen**

```bash
cd client && npx graphql-codegen && cd ..
```

Expected: `src/shared/middleware/graphql.ts` regenerated with new types and hooks (e.g., `useAiHubPersonalAgentSchedulesQuery`, `useCreateAiHubPersonalAgentScheduleMutation`, etc.).

- [ ] **Step 4: Commit**

```bash
git add client/codegen.ts client/src/graphql/ai-hub/ client/src/shared/middleware/graphql.ts
git commit -m "client - Add personal-agent schedule GraphQL operations and codegen"
```

### Task 19: Hooks wrapper

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts`

- [ ] **Step 1: Write the hooks file**

```ts
import {
    useAiHubPersonalAgentSchedulesQuery,
    useCreateAiHubPersonalAgentScheduleMutation,
    useDeleteAiHubPersonalAgentScheduleMutation,
    useToggleAiHubPersonalAgentScheduleMutation,
    useUpdateAiHubPersonalAgentScheduleMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

export const useAiHubPersonalAgentSchedules = (workspaceId: string, agentId: string) =>
    useAiHubPersonalAgentSchedulesQuery({agentId, workspaceId});

export const useCreateAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useCreateAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['AiHubPersonalAgentSchedules']}),
    });
};

export const useUpdateAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useUpdateAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['AiHubPersonalAgentSchedules']}),
    });
};

export const useDeleteAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useDeleteAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['AiHubPersonalAgentSchedules']}),
    });
};

export const useToggleAiHubPersonalAgentSchedule = () => {
    const queryClient = useQueryClient();

    return useToggleAiHubPersonalAgentScheduleMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['AiHubPersonalAgentSchedules']}),
    });
};
```

- [ ] **Step 2: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/schedules/hooks/useAiHubPersonalAgentSchedules.ts
git commit -m "client - Add schedule mutation hooks with query invalidation"
```

### Task 20: Frequency fields subcomponent

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleFrequencyFields.tsx`

- [ ] **Step 1: Write the component**

```tsx
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ScheduleFrequencyKind} from '@/shared/middleware/graphql';

export interface ScheduleFrequencyFieldsValueI {
    cronExpression?: string | null;
    dayOfMonth?: number | null;
    dayOfWeek?: number | null;
    intervalMinutes?: number | null;
    minuteOfHour?: number | null;
    timeOfDay?: string | null;
}

interface AiHubPersonalAgentScheduleFrequencyFieldsPropsType {
    frequencyKind: ScheduleFrequencyKind;
    onChange: (next: ScheduleFrequencyFieldsValueI) => void;
    value: ScheduleFrequencyFieldsValueI;
}

const DAYS_OF_WEEK = [
    {label: 'Monday', value: 1},
    {label: 'Tuesday', value: 2},
    {label: 'Wednesday', value: 3},
    {label: 'Thursday', value: 4},
    {label: 'Friday', value: 5},
    {label: 'Saturday', value: 6},
    {label: 'Sunday', value: 7},
];

const AiHubPersonalAgentScheduleFrequencyFields = ({
    frequencyKind,
    onChange,
    value,
}: AiHubPersonalAgentScheduleFrequencyFieldsPropsType) => {
    const update = (patch: Partial<ScheduleFrequencyFieldsValueI>) => onChange({...value, ...patch});

    if (frequencyKind === ScheduleFrequencyKind.EveryXMinutes) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="intervalMinutes">Every</Label>

                <Input
                    id="intervalMinutes"
                    max={59}
                    min={1}
                    onChange={(e) => update({intervalMinutes: parseInt(e.target.value, 10) || null})}
                    placeholder="minutes (1-59)"
                    type="number"
                    value={value.intervalMinutes ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Hourly) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="minuteOfHour">Minute of hour</Label>

                <Input
                    id="minuteOfHour"
                    max={59}
                    min={0}
                    onChange={(e) => update({minuteOfHour: parseInt(e.target.value, 10) || 0})}
                    type="number"
                    value={value.minuteOfHour ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Daily) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="timeOfDay">Time</Label>

                <Input
                    id="timeOfDay"
                    onChange={(e) => update({timeOfDay: e.target.value})}
                    type="time"
                    value={value.timeOfDay ?? ''}
                />
            </fieldset>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Weekly) {
        return (
            <>
                <fieldset className="border-0">
                    <Label htmlFor="dayOfWeek">Day of week</Label>

                    <Select
                        onValueChange={(v) => update({dayOfWeek: parseInt(v, 10)})}
                        value={value.dayOfWeek?.toString() ?? ''}
                    >
                        <SelectTrigger id="dayOfWeek">
                            <SelectValue placeholder="Select..." />
                        </SelectTrigger>

                        <SelectContent>
                            {DAYS_OF_WEEK.map((day) => (
                                <SelectItem key={day.value} value={day.value.toString()}>
                                    {day.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="timeOfDay">Time</Label>

                    <Input
                        id="timeOfDay"
                        onChange={(e) => update({timeOfDay: e.target.value})}
                        type="time"
                        value={value.timeOfDay ?? ''}
                    />
                </fieldset>
            </>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.Monthly) {
        return (
            <>
                <fieldset className="border-0">
                    <Label htmlFor="dayOfMonth">Day of month</Label>

                    <Input
                        id="dayOfMonth"
                        max={31}
                        min={1}
                        onChange={(e) => update({dayOfMonth: parseInt(e.target.value, 10) || null})}
                        type="number"
                        value={value.dayOfMonth ?? ''}
                    />
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="timeOfDay">Time</Label>

                    <Input
                        id="timeOfDay"
                        onChange={(e) => update({timeOfDay: e.target.value})}
                        type="time"
                        value={value.timeOfDay ?? ''}
                    />
                </fieldset>
            </>
        );
    }

    if (frequencyKind === ScheduleFrequencyKind.CustomCron) {
        return (
            <fieldset className="border-0">
                <Label htmlFor="cronExpression">Cron expression</Label>

                <Input
                    id="cronExpression"
                    onChange={(e) => update({cronExpression: e.target.value})}
                    placeholder="0 9 * * *"
                    type="text"
                    value={value.cronExpression ?? ''}
                />
            </fieldset>
        );
    }

    return null;
};

export default AiHubPersonalAgentScheduleFrequencyFields;
```

- [ ] **Step 2: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleFrequencyFields.tsx
git commit -m "client - Add schedule frequency fields subcomponent"
```

### Task 21: Schedule create/edit dialog

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx`

- [ ] **Step 1: Write the dialog**

```tsx
import Button from '@/components/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {
    AiHubPersonalAgentScheduleFieldsFragment,
    ScheduleFrequencyKind,
    ScheduleLifecycleKind,
} from '@/shared/middleware/graphql';
import {useState} from 'react';

import {
    useCreateAiHubPersonalAgentSchedule,
    useUpdateAiHubPersonalAgentSchedule,
} from './hooks/useAiHubPersonalAgentSchedules';
import AiHubPersonalAgentScheduleFrequencyFields, {
    ScheduleFrequencyFieldsValueI,
} from './AiHubPersonalAgentScheduleFrequencyFields';

interface AiHubPersonalAgentScheduleDialogPropsType {
    agentId: string;
    environment: number;
    existing?: AiHubPersonalAgentScheduleFieldsFragment;
    onClose: () => void;
    open: boolean;
    workspaceId: string;
}

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

const AiHubPersonalAgentScheduleDialog = ({
    agentId,
    environment,
    existing,
    onClose,
    open,
    workspaceId,
}: AiHubPersonalAgentScheduleDialogPropsType) => {
    const [title, setTitle] = useState(existing?.title ?? '');
    const [prompt, setPrompt] = useState(existing?.prompt ?? '');
    const [frequencyKind, setFrequencyKind] = useState<ScheduleFrequencyKind>(
        existing?.frequencyKind ?? ScheduleFrequencyKind.Daily
    );
    const [frequencyFields, setFrequencyFields] = useState<ScheduleFrequencyFieldsValueI>({
        cronExpression: existing?.cronExpression,
        dayOfMonth: existing?.dayOfMonth,
        dayOfWeek: existing?.dayOfWeek,
        intervalMinutes: existing?.intervalMinutes,
        minuteOfHour: existing?.minuteOfHour,
        timeOfDay: existing?.timeOfDay,
    });
    const [zoneId, setZoneId] = useState(existing?.zoneId ?? Intl.DateTimeFormat().resolvedOptions().timeZone);
    const [startDate, setStartDate] = useState(existing?.startDate ?? '');
    const [lifecycleKind, setLifecycleKind] = useState<ScheduleLifecycleKind>(
        existing?.lifecycleKind ?? ScheduleLifecycleKind.Recurring
    );
    const [maxRuns, setMaxRuns] = useState<number | null>(existing?.maxRuns ?? null);

    const createMutation = useCreateAiHubPersonalAgentSchedule();
    const updateMutation = useUpdateAiHubPersonalAgentSchedule();

    const submit = async () => {
        if (existing) {
            await updateMutation.mutateAsync({
                input: {
                    cronExpression: frequencyFields.cronExpression,
                    dayOfMonth: frequencyFields.dayOfMonth,
                    dayOfWeek: frequencyFields.dayOfWeek,
                    enabled: existing.enabled,
                    frequencyKind,
                    id: existing.id,
                    intervalMinutes: frequencyFields.intervalMinutes,
                    lifecycleKind,
                    maxRuns: lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? maxRuns : null,
                    minuteOfHour: frequencyFields.minuteOfHour,
                    prompt,
                    startDate: startDate || null,
                    timeOfDay: frequencyFields.timeOfDay,
                    title,
                    workspaceId,
                    zoneId,
                },
            });
        } else {
            await createMutation.mutateAsync({
                input: {
                    aiHubPersonalAgentId: agentId,
                    cronExpression: frequencyFields.cronExpression,
                    dayOfMonth: frequencyFields.dayOfMonth,
                    dayOfWeek: frequencyFields.dayOfWeek,
                    environment,
                    frequencyKind,
                    intervalMinutes: frequencyFields.intervalMinutes,
                    lifecycleKind,
                    maxRuns: lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? maxRuns : null,
                    minuteOfHour: frequencyFields.minuteOfHour,
                    prompt,
                    startDate: startDate || null,
                    timeOfDay: frequencyFields.timeOfDay,
                    title,
                    workspaceId,
                    zoneId,
                },
            });
        }

        onClose();
    };

    return (
        <Dialog onOpenChange={onClose} open={open}>
            <DialogContent className="max-w-xl">
                <DialogHeader>
                    <DialogTitle>{existing ? 'Edit scheduled task' : 'Create new scheduled task'}</DialogTitle>
                </DialogHeader>

                <fieldset className="border-0">
                    <Label htmlFor="title">Title</Label>

                    <Input
                        id="title"
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="e.g., Daily report generation"
                        value={title}
                    />
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="prompt">Task description</Label>

                    <Textarea
                        id="prompt"
                        onChange={(e) => setPrompt(e.target.value)}
                        placeholder="Describe what this scheduled task should do..."
                        rows={3}
                        value={prompt}
                    />
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="frequencyKind">Run frequency</Label>

                    <Select
                        onValueChange={(v) => setFrequencyKind(v as ScheduleFrequencyKind)}
                        value={frequencyKind}
                    >
                        <SelectTrigger id="frequencyKind">
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
                    frequencyKind={frequencyKind}
                    onChange={setFrequencyFields}
                    value={frequencyFields}
                />

                <fieldset className="border-0">
                    <Label htmlFor="zoneId">Timezone</Label>

                    <Select onValueChange={setZoneId} value={zoneId}>
                        <SelectTrigger id="zoneId">
                            <SelectValue placeholder="Select..." />
                        </SelectTrigger>

                        <SelectContent>
                            {COMMON_TIMEZONES.map((tz) => (
                                <SelectItem key={tz} value={tz}>
                                    {tz}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </fieldset>

                <fieldset className="border-0">
                    <Label htmlFor="startDate">Start date (optional)</Label>

                    <Input
                        id="startDate"
                        onChange={(e) => setStartDate(e.target.value)}
                        placeholder="Starts immediately"
                        type="datetime-local"
                        value={startDate}
                    />
                </fieldset>

                <fieldset className="border-0">
                    <Label>Lifecycle</Label>

                    <div className="flex gap-2">
                        <Button
                            onClick={() => setLifecycleKind(ScheduleLifecycleKind.Recurring)}
                            variant={lifecycleKind === ScheduleLifecycleKind.Recurring ? 'default' : 'outline'}
                        >
                            Recurring
                        </Button>

                        <Button
                            onClick={() => setLifecycleKind(ScheduleLifecycleKind.NumberOfRuns)}
                            variant={lifecycleKind === ScheduleLifecycleKind.NumberOfRuns ? 'default' : 'outline'}
                        >
                            Number of runs
                        </Button>
                    </div>
                </fieldset>

                {lifecycleKind === ScheduleLifecycleKind.NumberOfRuns && (
                    <fieldset className="border-0">
                        <Label htmlFor="maxRuns">Max runs (optional)</Label>

                        <Input
                            id="maxRuns"
                            min={1}
                            onChange={(e) => setMaxRuns(e.target.value ? parseInt(e.target.value, 10) : null)}
                            placeholder="No limit"
                            type="number"
                            value={maxRuns ?? ''}
                        />
                    </fieldset>
                )}

                <DialogFooter>
                    <Button onClick={onClose} variant="outline">
                        Cancel
                    </Button>

                    <Button onClick={submit}>{existing ? 'Save' : 'Create'}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubPersonalAgentScheduleDialog;
```

- [ ] **Step 2: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentScheduleDialog.tsx
git commit -m "client - Add personal-agent schedule create/edit dialog"
```

### Task 22: Schedules list tab

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx`

- [ ] **Step 1: Write the list component**

```tsx
import Button from '@/components/Button';
import {Switch} from '@/components/ui/switch';
import {AiHubPersonalAgentScheduleFieldsFragment} from '@/shared/middleware/graphql';
import {PlusIcon, TrashIcon} from 'lucide-react';
import {useState} from 'react';

import {
    useAiHubPersonalAgentSchedules,
    useDeleteAiHubPersonalAgentSchedule,
    useToggleAiHubPersonalAgentSchedule,
} from './hooks/useAiHubPersonalAgentSchedules';
import AiHubPersonalAgentScheduleDialog from './AiHubPersonalAgentScheduleDialog';

interface AiHubPersonalAgentSchedulesListPropsType {
    agentId: string;
    environment: number;
    workspaceId: string;
}

const formatFrequency = (schedule: AiHubPersonalAgentScheduleFieldsFragment) =>
    `${schedule.frequencyKind} (${schedule.effectiveCronExpression} ${schedule.zoneId})`;

const AiHubPersonalAgentSchedulesList = ({
    agentId,
    environment,
    workspaceId,
}: AiHubPersonalAgentSchedulesListPropsType) => {
    const {data, isLoading} = useAiHubPersonalAgentSchedules(workspaceId, agentId);

    const [dialogOpen, setDialogOpen] = useState(false);
    const [editing, setEditing] = useState<AiHubPersonalAgentScheduleFieldsFragment | undefined>();

    const toggleMutation = useToggleAiHubPersonalAgentSchedule();
    const deleteMutation = useDeleteAiHubPersonalAgentSchedule();

    if (isLoading) {
        return <div>Loading…</div>;
    }

    const schedules = data?.aiHubPersonalAgentSchedules ?? [];

    return (
        <section className="space-y-4">
            <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold">Scheduled tasks</h2>

                <Button
                    onClick={() => {
                        setEditing(undefined);
                        setDialogOpen(true);
                    }}
                >
                    <PlusIcon className="size-4" /> New scheduled task
                </Button>
            </div>

            {schedules.length === 0 ? (
                <p className="text-muted-foreground">No scheduled tasks yet.</p>
            ) : (
                <ul className="divide-y rounded border">
                    {schedules.map((schedule) => (
                        <li
                            className="flex items-center justify-between p-4 hover:bg-muted"
                            key={schedule.id}
                        >
                            <button
                                className="flex-1 text-left"
                                onClick={() => {
                                    setEditing(schedule);
                                    setDialogOpen(true);
                                }}
                            >
                                <div className="font-medium">{schedule.title}</div>

                                <div className="text-sm text-muted-foreground">
                                    {formatFrequency(schedule)}
                                    {schedule.nextRunAt && ` — Next run: ${schedule.nextRunAt}`}
                                </div>
                            </button>

                            <Switch
                                checked={schedule.enabled}
                                onCheckedChange={(enabled) =>
                                    toggleMutation.mutate({enabled, scheduleId: schedule.id, workspaceId})
                                }
                            />

                            <Button
                                onClick={() => deleteMutation.mutate({scheduleId: schedule.id, workspaceId})}
                                size="icon"
                                variant="ghost"
                            >
                                <TrashIcon className="size-4" />
                            </Button>
                        </li>
                    ))}
                </ul>
            )}

            {dialogOpen && (
                <AiHubPersonalAgentScheduleDialog
                    agentId={agentId}
                    environment={environment}
                    existing={editing}
                    onClose={() => setDialogOpen(false)}
                    open={dialogOpen}
                    workspaceId={workspaceId}
                />
            )}
        </section>
    );
};

export default AiHubPersonalAgentSchedulesList;
```

- [ ] **Step 2: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/schedules/AiHubPersonalAgentSchedulesList.tsx
git commit -m "client - Add personal-agent schedules list view"
```

### Task 23: Wire schedules tab into agent detail page

**Files:**
- Modify: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`

- [ ] **Step 1: Open the form file and find the existing layout**

The agent form is currently a flat layout. Wrap its content in a `Tabs` shell with two tabs: "Overview" (existing form) and "Schedules" (new list). Only show "Schedules" when editing an existing agent (i.e., when `agentId` is present in the route).

Add imports at the top:
```tsx
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import AiHubPersonalAgentSchedulesList from './schedules/AiHubPersonalAgentSchedulesList';
```

- [ ] **Step 2: Wrap the main content**

Find the JSX `return (...)`. Wrap the existing form-card content in:

```tsx
<Tabs defaultValue="overview">
    <TabsList>
        <TabsTrigger value="overview">Overview</TabsTrigger>

        {agentId && <TabsTrigger value="schedules">Schedules</TabsTrigger>}
    </TabsList>

    <TabsContent value="overview">
        {/* existing form JSX moves in here */}
    </TabsContent>

    {agentId && (
        <TabsContent value="schedules">
            <AiHubPersonalAgentSchedulesList
                agentId={agentId}
                environment={environment}
                workspaceId={workspaceId}
            />
        </TabsContent>
    )}
</Tabs>
```

> Confirm the exact names of the `agentId`, `environment`, and `workspaceId` variables in the existing component (they may come from `useParams`, `useEnvironmentStore`, `useWorkspaceStore`). Use whatever the file already has — don't duplicate.

- [ ] **Step 3: Run client checks**

```bash
cd client && npm run check && cd ..
```

Expected: lint + typecheck + tests all pass. Fix any sort-keys / naming-convention violations the linter flags (memory note: ESLint `--fix` does NOT auto-fix sort-keys).

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx
git commit -m "client - Add Schedules tab to personal-agent detail page"
```

---

## Phase 8 — End-to-end smoke + cleanup

### Task 24: Manual smoke test + spotless + final commit

- [ ] **Step 1: Format and run all server checks**

```bash
./gradlew spotlessApply
./gradlew check
```

Expected: BUILD SUCCESSFUL. Fix any Checkstyle / SpotBugs / PMD issues.

- [ ] **Step 2: Run client checks one more time**

```bash
cd client && npm run check && cd ..
```

Expected: all pass.

- [ ] **Step 3: Start the stack and exercise the flow**

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..
./gradlew -p server/apps/server-app bootRun &
cd client && npm run dev &
```

In the browser at `http://localhost:3000`:
1. Log in as `admin@localhost.com / admin`.
2. Navigate to AI Hub → Personal Agents → create or open an existing agent.
3. Click the **Schedules** tab.
4. Click **+ New scheduled task**.
5. Fill in: Title "Smoke test", Task description "Say hello", Run frequency "Every X Minutes", interval `1`, timezone `UTC`, lifecycle "Number of runs" with Max runs `2`.
6. Click **Create**.

Expected: row appears in the list. Within ~60s, a new conversation/task with title "Smoke test" appears under the agent in the sidebar with the agent's response to "Say hello". After two fires, the schedule should auto-disable.

- [ ] **Step 4: Tear down processes**

```bash
# Stop bootRun, npm run dev, then:
cd server && docker compose -f docker-compose.dev.infra.yml down && cd ..
```

- [ ] **Step 5: Final commit (if anything changed)**

```bash
git status
# Only commit if there are leftover formatting / fix changes
```

---

## Self-review checklist (post-write)

- [x] **Spec coverage:**
  - Data model — Task 5 (migration), Task 8 (domain class).
  - Enums (ordinal stability) — Tasks 6, 7.
  - Scheduler API + Quartz wiring — Tasks 1–4.
  - Normalization (cron derivation, lifecycle cleanup) — Tasks 11, 12.
  - Listener (7-step path) — Task 14.
  - Boot reconciliation — Task 15.
  - GraphQL surface — Tasks 16, 17.
  - Frontend tab + dialog + list — Tasks 18–23.
  - Metrics — Task 15.
  - Smoke test — Task 24.

- [x] **Placeholder scan:** the `injectAuthenticatedContext` key-name mirror in Task 13 explicitly tells the implementer to read the helper and adjust — this is a real dependency on existing-code inspection, not a placeholder.

- [x] **Type consistency:** `AiHubPersonalAgentSchedule`, `ScheduleFrequencyKind`, `ScheduleLifecycleKind`, `AiHubPersonalAgentScheduleService`, `AgentScheduler`, `AgentScheduleFiredEvent`, `AiHubScheduledChatDispatcher`, `AgentScheduleFiredEventListener`, `ScheduleCronNormalizer` — names match across tasks.

## Out-of-scope follow-ups (deliberately deferred)

- `AiHubPersonalAgentScheduleServiceIntTest` against Testcontainers Postgres (the unit-level mocked test in Task 12 plus the Quartz integration test in Task 4 cover the major risk paths).
- `bytechef_ai_hub_agent_schedule_active` gauge (only the `_fire` counter is in v1).
- Dialog Vitest spec — defer to a follow-up PR once the GraphQL types stabilize.
- "Run now" button on a schedule row.
- Per-fire failure record table.
