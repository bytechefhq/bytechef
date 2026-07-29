/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentScheduleRepository;
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
 * Verifies the {@code uk_ai_hub_personal_agent_schedule_agent} UNIQUE constraint introduced for the v2
 * one-schedule-per-agent design. Pins behavior the service-level upsert cannot guarantee on its own (interleaved
 * transactions, direct repository misuse).
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

    @AfterEach
    public void afterEach() {
        scheduleRepository.deleteAll();
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
        agent.setWorkspaceId(workspaceId);

        AiHubPersonalAgent saved = agentRepository.save(agent);

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
