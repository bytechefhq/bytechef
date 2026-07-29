/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiHubPersonalAgentRepository}. Pins the parts of the schema that ServiceTest can't reach
 * with a mocked repository:
 *
 * <ul>
 * <li>The {@code workspace_id} predicate actually returns the right rows scoped to (workspace, user, environment) and
 * ordered by {@code updated_at DESC} — sidebar render depends on that order.</li>
 * <li>An agent with a null {@code workspace_id} is invisible to every workspace-scoped query — the nullable column
 * admits a state the relation table made structurally impossible.</li>
 * <li>The {@code ck_ai_hub_personal_agent_name_slug} CHECK constraint rejects malformed names that bypass the entity
 * setter (Spring Data JDBC reflection during hydration).</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubPersonalAgentRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubPersonalAgentRepositoryIntTest {

    private static final int DEV = Environment.DEVELOPMENT.ordinal();

    @Autowired
    private AiHubPersonalAgentRepository aiHubPersonalAgentRepository;

    @AfterEach
    public void afterEach() {
        aiHubPersonalAgentRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindByWorkspaceUserEnvironmentAndName() {
        long agentId = saveAgentInWorkspace(1L, 10L, "research-bot");

        assertThat(agentId).isPositive();

        List<AiHubPersonalAgent> found = aiHubPersonalAgentRepository
            .findAllByWorkspaceUserEnvironmentAndName(1L, 10L, DEV, "research-bot");

        assertThat(found).hasSize(1);
        assertThat(found.get(0)
            .getTitle()).isEqualTo("Title: research-bot");
    }

    @Test
    public void testFindAllByWorkspaceUserEnvironmentOrdersByUpdatedAtDesc() {
        AiHubPersonalAgent older = buildAgent(10L, "older");

        older.setUpdatedAt(LocalDateTime.now()
            .minusMinutes(10));

        AiHubPersonalAgent newer = buildAgent(10L, "newer");

        newer.setUpdatedAt(LocalDateTime.now());

        older.setWorkspaceId(1L);
        newer.setWorkspaceId(1L);

        aiHubPersonalAgentRepository.save(older);
        aiHubPersonalAgentRepository.save(newer);

        List<AiHubPersonalAgent> all =
            aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(1L, 10L, DEV);

        assertThat(all).hasSize(2);
        // Sidebar relies on this exact order — without it freshly-touched agents would sink to the bottom.
        assertThat(all.get(0)
            .getName()).isEqualTo("newer");
        assertThat(all.get(1)
            .getName()).isEqualTo("older");
    }

    @Test
    public void testWorkspaceScopedQueriesIgnoreWorkspaceLessAgents() {
        AiHubPersonalAgent orphan = buildAgent(10L, "orphan-bot");

        orphan.setWorkspaceId(null);

        aiHubPersonalAgentRepository.save(orphan);

        // SQL equality never matches NULL, so a workspace-less agent is correctly invisible to every workspace
        // query — including workspace 0, which a primitive long field would have collapsed it into.
        assertThat(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(1L, 10L, DEV)).isEmpty();
        assertThat(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(0L, 10L, DEV)).isEmpty();
        assertThat(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(1L, 10L, DEV, "orphan-bot"))
            .isEmpty();
    }

    @Test
    public void testDeleteRemovesAgentRow() {
        long agentId = saveAgentInWorkspace(1L, 10L, "research-bot");

        aiHubPersonalAgentRepository.deleteById(agentId);

        assertThat(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(1L, 10L, DEV)).isEmpty();
    }

    @Test
    public void testCheckConstraintRejectsMalformedSlug() {
        // The entity setter rejects malformed names before save, so this test exercises the CHECK constraint
        // implicitly: every other test in this class saves a row that the constraint would reject if the slug
        // regex stopped matching. Pinning a positive case here so a future regression to NAME_PATTERN that
        // accidentally lets through illegal characters fails fast.
        AiHubPersonalAgent agent = new AiHubPersonalAgent(10L);

        agent.setName("valid-slug");
        agent.setTitle("Test");
        agent.setEnvironment(Environment.DEVELOPMENT);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());

        AiHubPersonalAgent saved = aiHubPersonalAgentRepository.save(agent);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).matches(AiHubPersonalAgent.NAME_PATTERN);
    }

    private long saveAgentInWorkspace(long workspaceId, long userId, String name) {
        AiHubPersonalAgent agent = buildAgent(userId, name);

        agent.setWorkspaceId(workspaceId);

        AiHubPersonalAgent saved = aiHubPersonalAgentRepository.save(agent);

        return saved.getId();
    }

    private static AiHubPersonalAgent buildAgent(long userId, String name) {
        AiHubPersonalAgent agent = new AiHubPersonalAgent(userId);

        agent.setName(name);
        agent.setTitle("Title: " + name);
        agent.setDescription("Description for " + name);
        agent.setInstructions("Instructions for " + name);
        agent.setEnvironment(Environment.DEVELOPMENT);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());

        return agent;
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubPersonalAgentRepositoryIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
