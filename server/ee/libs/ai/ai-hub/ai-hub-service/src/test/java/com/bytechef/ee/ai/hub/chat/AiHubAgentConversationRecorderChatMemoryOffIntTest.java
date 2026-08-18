/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pins the resolved product decision that an agent with chat memory switched off never gets an {@code ai_hub_chat} row
 * (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}, design spec §8) — checked against a REAL Postgres schema,
 * not a mocked repository.
 *
 * <p>
 * Switching {@code AgentSettingsCard}'s "Chat memory" toggle off deletes the agent's {@code CHAT_MEMORY} element
 * (client/src/pages/automation/agents/components/detail/AgentSettingsCard.tsx:106-113), which makes
 * {@code AiAgentWorkflowGenerator} omit the {@code chatMemory} cluster element entirely
 * ({@code AiAgentWorkflowGeneratorTest#testAiAgentNodeIsSecondTaskWithOnlyDefaultBuiltInToolsClusterElements} pins that
 * write side), which in turn makes {@code AbstractAiAgentChatAction#resolveConversationId} return {@code null} (pinned
 * at the component level, with a mocked recorder, by
 * {@code AbstractAiAgentChatActionAgentConversationRecorderTest#testNullConversationIdMeansNoCall}). What none of those
 * tests show is what the EE recorder does with that {@code null} once it actually reaches it: this class proves —
 * against a real database, with an otherwise fully authentic workspace/workflow stamp — that no row is ever persisted.
 * </p>
 *
 * <p>
 * The {@code ProjectService} stub here deliberately resolves the stamped workflow to the claimed workspace (unlike
 * {@link AiHubAgentConversationRecorderTransactionIntTest}'s {@code ThrowingOnGetProjectService}, whose
 * {@code fetchWorkflowProject} always answers empty) so that {@link #testValidConversationIdCreatesARealRow} proves the
 * harness genuinely persists a row when nothing is wrong — otherwise an empty table would prove nothing about the
 * {@code null}-conversationId case.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubAgentConversationRecorderChatMemoryOffIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class AiHubAgentConversationRecorderChatMemoryOffIntTest {

    private static final long AI_AGENT_ID = 20L;
    private static final long CREATOR_USER_ID = 30L;
    private static final long WORKSPACE_ID = 10L;
    private static final String WORKFLOW_ID = "workflow-chat-memory-off";

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private AiHubAgentConversationRecorder recorder;

    @AfterEach
    void afterEach() {
        chatRepository.deleteAll();
    }

    /**
     * The pin. {@code conversationId = null} is exactly what {@code resolveConversationId} returns for a node with no
     * {@code CHAT_MEMORY} cluster element — chat memory switched off — and must leave the real table untouched.
     */
    @Test
    void testNoChatMemoryConversationIdCreatesNoRealRow() {
        assertThatCode(
            () -> recorder.recordTurn(
                new AgentConversation(
                    WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, null, null, null, WORKFLOW_ID,
                    (long) Environment.PRODUCTION.ordinal())))
                        .doesNotThrowAnyException();

        assertThat(chatRepository.findAll()).isEmpty();
    }

    /**
     * Control: the same recorder, the same harness, an otherwise identical stamp — only the conversation id differs.
     * Proves the harness would have caught a broken guard by actually persisting a row when nothing is wrong.
     */
    @Test
    void testValidConversationIdCreatesARealRow() {
        String conversationId = UUID.randomUUID()
            .toString();

        recorder.recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, conversationId, null, null, WORKFLOW_ID,
                (long) Environment.PRODUCTION.ordinal()));

        assertThat(chatRepository.findByThreadId(conversationId)).isPresent();
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubAgentConversationRecorderChatMemoryOffIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    static class IntTestConfiguration {

        @Bean
        AiHubChatService aiHubChatService(
            AiHubChatRepository chatRepository, ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider) {

            return new AiHubChatServiceImpl(
                chatRepository, mock(JobFacade.class), mock(WorkflowChatJobRegistry.class),
                mock(InFlightAiHubRunRegistry.class), null, aiHubSessionMemoryProvider, null);
        }

        @Bean
        AiHubAgentConversationRecorder aiHubAgentConversationRecorder(
            AiHubChatRepository chatRepository, AiHubChatService chatService, ProjectService projectService) {

            return new AiHubAgentConversationRecorder(chatRepository, chatService, projectService);
        }

        /**
         * Resolves the stamped {@link #WORKFLOW_ID} to a project in {@link #WORKSPACE_ID} — an authentic stamp — so the
         * only reason a row would not be written is the conversation id itself.
         */
        @Bean
        ProjectService projectService() {
            return new ResolvingProjectService();
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }

    /**
     * A {@code ProjectService} whose {@code fetchWorkflowProject} resolves {@link #WORKFLOW_ID} to a project owned by
     * {@link #WORKSPACE_ID} — everything else throws, since the recorder reaches nothing else on this interface.
     */
    static class ResolvingProjectService implements ProjectService {

        @Override
        public Optional<Project> fetchWorkflowProject(String workflowId) {
            if (!WORKFLOW_ID.equals(workflowId)) {
                return Optional.empty();
            }

            return Optional.of(
                Project.builder()
                    .id(1L)
                    .name("__AI_AGENT__test")
                    .workspaceId(WORKSPACE_ID)
                    .build());
        }

        @Override
        public Project getWorkflowProject(String workflowId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countProjects() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project create(Project project) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Project> fetchProject(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Project> fetchProject(String name, long workspaceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project getProjectDeploymentProject(long projectDeploymentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project getProject(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project getProject(UUID uuid) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProjectVersion> getProjectVersions(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(List<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(
            Boolean apiCollections, Long categoryId, Boolean projectDeployments, Long tagId,
            ProjectVersion.Status status, Long workspaceId) {

            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> getWorkspaceProjectIds(long workspaceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int publishProject(long id, String description, boolean syncWithGit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project update(long id, List<Long> tagIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project update(Project project) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project updateErrorWorkflow(long id, Long errorProjectWorkflowId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Project updatePermissionExpression(long id, String permissionExpression) {
            throw new UnsupportedOperationException();
        }
    }
}
