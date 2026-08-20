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
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pins that {@link AiHubAgentConversationRecorder}'s fail-closed branches really are quiet skips against a REAL
 * transaction manager and REAL transaction proxies — which the mocked unit suite structurally cannot show.
 *
 * <p>
 * The bug this guards: {@code ProjectService#getWorkflowProject} is itself {@code @Transactional} and throws for an
 * unknown workflow. Under Spring's default {@code globalRollbackOnParticipationFailure}, the inner proxy marks the
 * PARTICIPATING transaction rollback-only the moment it sees the throw, so a caller that catches the exception still
 * dies at commit with {@code UnexpectedRollbackException} — and takes any surrounding caller transaction with it. A
 * mocked {@code ProjectService} has no proxy and therefore never reproduces this.
 * </p>
 *
 * <p>
 * {@code ProjectService} is stubbed here rather than imported from {@code automation-configuration-service}, because
 * what has to be faithful is the <b>proxy shape</b> — a {@code @Transactional(readOnly = true)} bean whose
 * {@code getWorkflowProject} throws and whose {@code fetchWorkflowProject} returns empty — not the real query.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubAgentConversationRecorderTransactionIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubAgentConversationRecorderTransactionIntTest {

    private static final long AI_AGENT_ID = 20L;
    private static final long CREATOR_USER_ID = 30L;
    private static final long WORKSPACE_ID = 10L;

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private AiHubAgentConversationRecorder recorder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    public void afterEach() {
        chatRepository.deleteAll();
    }

    /**
     * The whole point: a caller transaction that records a turn for an unknown workflow, then writes a row of its own,
     * must still commit. Against a recorder that catches {@code getWorkflowProject}'s exception this fails at commit
     * with {@code UnexpectedRollbackException} and the row is gone.
     */
    @Test
    public void testUnresolvableWorkflowLeavesTheCallerTransactionUsable() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        String threadId = UUID.randomUUID()
            .toString();

        assertThatCode(
            () -> transactionTemplate.executeWithoutResult(
                status -> {
                    recorder.recordTurn(
                        new AgentConversation(
                            WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, threadId, null, null, "unknown-workflow",
                            (long) Environment.PRODUCTION.ordinal()));

                    chatRepository.save(buildChat("committed-after-skip"));
                }))
                    .doesNotThrowAnyException();

        assertThat(chatRepository.findByThreadId("committed-after-skip")).isPresent();
        assertThat(chatRepository.findByThreadId(threadId)).isEmpty();
    }

    /**
     * Same guarantee for the recorder standing alone: it starts its own transaction, and the skip must commit it
     * cleanly rather than surfacing a rollback error to the agent's turn.
     */
    @Test
    public void testUnresolvableWorkflowSkipsQuietlyWithoutACallerTransaction() {
        String threadId = UUID.randomUUID()
            .toString();

        assertThatCode(
            () -> recorder.recordTurn(
                new AgentConversation(
                    WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, threadId, null, null, "unknown-workflow",
                    (long) Environment.PRODUCTION.ordinal())))
                        .doesNotThrowAnyException();

        assertThat(chatRepository.findByThreadId(threadId)).isEmpty();
    }

    private static AiHubChat buildChat(String threadId) {
        AiHubChat chat = new AiHubChat(CREATOR_USER_ID);

        chat.setThreadId(threadId);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setEnvironment(Environment.PRODUCTION);
        chat.setWorkspaceId(WORKSPACE_ID);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        return chat;
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubAgentConversationRecorderTransactionIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

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
         * Mirrors {@code ProjectServiceImpl}'s transactional shape for the two methods the recorder can reach. Only
         * these two are implemented; the rest of the interface is irrelevant to this test and fails loudly if some
         * future change starts calling it.
         */
        @Bean
        ProjectService projectService() {
            return new ThrowingOnGetProjectService();
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }

    /**
     * A {@code ProjectService} whose {@code getWorkflowProject} throws from inside a {@code @Transactional} proxy —
     * exactly like the real one — and whose {@code fetchWorkflowProject} answers with an empty {@code Optional}.
     */
    static class ThrowingOnGetProjectService implements ProjectService {

        @Override
        @Transactional(readOnly = true)
        public Optional<Project> fetchWorkflowProject(String workflowId) {
            return Optional.empty();
        }

        @Override
        @Transactional(readOnly = true)
        public Project getWorkflowProject(String workflowId) {
            throw new java.util.NoSuchElementException("No value present");
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
        public Optional<Project> fetchProject(long id) {
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
        public List<com.bytechef.automation.configuration.domain.ProjectVersion> getProjectVersions(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(List<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(
            Boolean apiCollections, Long categoryId, Boolean projectDeployments, Long tagId,
            com.bytechef.automation.configuration.domain.ProjectVersion.Status status, Long workspaceId) {

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

        @Override
        public Project updateVisibility(long id, ResourceVisibility visibility) {
            throw new UnsupportedOperationException();
        }
    }
}
