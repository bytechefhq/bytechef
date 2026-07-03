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
import com.bytechef.ee.ai.hub.personalagent.repository.WorkspaceAiHubPersonalAgentRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiHubPersonalAgentRepository}. Pins the parts of the schema that ServiceTest can't reach
 * with a mocked repository:
 *
 * <ul>
 * <li>The JOIN through {@code workspace_ai_hub_personal_agent} actually returns the right rows scoped to (workspace,
 * user, environment) and ordered by {@code updated_at DESC} — sidebar render depends on that order.</li>
 * <li>The unique constraint on {@code (workspace_id, ai_hub_personal_agent_id)} on the relation table fires at the DB
 * level — service-layer pre-flight checks are belt; this is suspenders.</li>
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

    @Autowired
    private WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository;

    @AfterEach
    public void afterEach() {
        // Order matters: the relation has a CASCADE on the agent FK, so dropping agents first nukes the relation
        // rows, but doing both explicitly leaves the test deterministic regardless of cascade timing.
        workspaceAiHubPersonalAgentRepository.deleteAll();
        aiHubPersonalAgentRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindByWorkspaceUserEnvironmentAndName() {
        long agentId = saveAgentWithMembership(1L, 10L, "research-bot");

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

        AiHubPersonalAgent savedOlder = aiHubPersonalAgentRepository.save(older);
        AiHubPersonalAgent savedNewer = aiHubPersonalAgentRepository.save(newer);

        workspaceAiHubPersonalAgentRepository.save(
            new WorkspaceAiHubPersonalAgent(1L, savedOlder.getId()));
        workspaceAiHubPersonalAgentRepository.save(
            new WorkspaceAiHubPersonalAgent(1L, savedNewer.getId()));

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
    public void testRelationUniqueOnWorkspaceAndAgent() {
        long agentId = saveAgentWithMembership(1L, 10L, "research-bot");

        // Same (workspace_id, ai_hub_personal_agent_id) pair → DB rejects the second insert. The unique
        // constraint on the relation prevents the same workspace from claiming the same agent twice; without it,
        // a concurrent create-or-attach could fan out duplicate memberships.
        assertThatThrownBy(() -> workspaceAiHubPersonalAgentRepository.save(
            new WorkspaceAiHubPersonalAgent(1L, agentId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testRelationCascadeDeletesMembershipWhenAgentRemoved() {
        long agentId = saveAgentWithMembership(1L, 10L, "research-bot");

        assertThat(workspaceAiHubPersonalAgentRepository.findAllByWorkspaceId(1L)).hasSize(1);

        aiHubPersonalAgentRepository.deleteById(agentId);

        // ON DELETE CASCADE on the agent FK clears the membership row automatically — without it, deleting an
        // agent would leave the relation row dangling with a broken FK reference.
        assertThat(workspaceAiHubPersonalAgentRepository.findAllByWorkspaceId(1L)).isEmpty();
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

    private long saveAgentWithMembership(long workspaceId, long userId, String name) {
        AiHubPersonalAgent saved = aiHubPersonalAgentRepository.save(buildAgent(userId, name));

        workspaceAiHubPersonalAgentRepository.save(
            new WorkspaceAiHubPersonalAgent(workspaceId, saved.getId()));

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
