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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AiHubIntTestConfiguration.class, AiHubAgentConversationRecorderTransactionIntTest.RecorderConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubAgentConversationRecorderTransactionIntTest {

    private static final long AI_AGENT_ID = 20L;
    private static final long CREATOR_USER_ID = 30L;
    private static final String KNOWN_WORKFLOW_ID = "workflow-known";
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

    @Test
    public void testUnresolvableWorkflowLeavesTheCallerTransactionUsable() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        String threadId = randomThreadId();

        assertThatCode(
            () -> transactionTemplate.executeWithoutResult(
                status -> {
                    recorder.recordTurn(agentConversation(threadId, "unknown-workflow"));

                    chatRepository.save(buildChat("committed-after-skip"));
                }))
                    .doesNotThrowAnyException();

        assertThat(chatRepository.findByThreadId("committed-after-skip")).isPresent();
        assertThat(chatRepository.findByThreadId(threadId)).isEmpty();
    }

    @Test
    public void testUnresolvableWorkflowSkipsQuietlyWithoutACallerTransaction() {
        String threadId = randomThreadId();

        assertThatCode(() -> recorder.recordTurn(agentConversation(threadId, "unknown-workflow")))
            .doesNotThrowAnyException();

        assertThat(chatRepository.findByThreadId(threadId)).isEmpty();
    }

    @Test
    public void testNoChatMemoryConversationIdCreatesNoRow() {
        assertThatCode(() -> recorder.recordTurn(agentConversation(null, KNOWN_WORKFLOW_ID)))
            .doesNotThrowAnyException();

        assertThat(chatRepository.findAll()).isEmpty();
    }

    @Test
    public void testAuthenticTurnCreatesOneOwnerOnlyAgentChatAcrossTurns() {
        String threadId = randomThreadId();

        recorder.recordTurn(agentConversation(threadId, KNOWN_WORKFLOW_ID));
        recorder.recordTurn(agentConversation(threadId, KNOWN_WORKFLOW_ID));

        assertThat(chatRepository.findAll()).hasSize(1);

        AiHubChat chat = chatRepository.findByThreadId(threadId)
            .orElseThrow();

        assertThat(chat.getKind()).isEqualTo(AiHubChatKind.AGENT_CHAT);
        assertThat(chat.getAiAgentId()).isEqualTo(AI_AGENT_ID);
        assertThat(chat.getUserId()).isEqualTo(CREATOR_USER_ID);
        assertThat(chat.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(chat.getWorkflowExecutionId()).isNull();
        assertThat(chat.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);
    }

    @Test
    public void testAComposerChatOnTheSameThreadIsNotAdopted() {
        String threadId = randomThreadId();

        chatRepository.save(buildChat(threadId));

        recorder.recordTurn(agentConversation(threadId, KNOWN_WORKFLOW_ID));

        AiHubChat chat = chatRepository.findByThreadId(threadId)
            .orElseThrow();

        assertThat(chat.getKind()).isEqualTo(AiHubChatKind.STANDARD);
        assertThat(chat.getAiAgentId()).isNull();
    }

    private static AgentConversation agentConversation(@Nullable String threadId, String workflowId) {
        return new AgentConversation(
            WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, threadId, null, null, workflowId,
            (long) Environment.PRODUCTION.ordinal());
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

    private static String randomThreadId() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }

    @Configuration
    static class RecorderConfiguration {

        @Bean
        @SuppressWarnings("unchecked")
        AiHubChatService aiHubChatService(
            AiHubChatRepository chatRepository, PlatformTransactionManager transactionManager) {

            return new AiHubChatServiceImpl(
                chatRepository, new OwnerOnlyAccessPolicy(), mock(AiHubToolApprovalService.class),
                (ObjectProvider<AiHubSessionMemory>) mock(ObjectProvider.class), mock(InFlightAiHubRunRegistry.class),
                transactionManager, mock(ObjectProvider.class), mock(ApplicationEventPublisher.class));
        }

        @Bean
        AiHubAgentConversationRecorder aiHubAgentConversationRecorder(
            AiHubChatRepository chatRepository, AiHubChatService aiHubChatService, ProjectService projectService) {

            return new AiHubAgentConversationRecorder(chatRepository, aiHubChatService, projectService);
        }

        @Bean
        ProjectService projectService() {
            return new TransactionalProjectService();
        }
    }

    static class TransactionalProjectService implements ProjectService {

        @Override
        @Transactional(readOnly = true)
        public Optional<Project> fetchWorkflowProject(String workflowId) {
            if (!KNOWN_WORKFLOW_ID.equals(workflowId)) {
                return Optional.empty();
            }

            return Optional.of(
                Project.builder()
                    .id(1L)
                    .name("Support Bot")
                    .workspaceId(WORKSPACE_ID)
                    .build());
        }

        @Override
        @Transactional(readOnly = true)
        public Project getWorkflowProject(String workflowId) {
            throw new NoSuchElementException("No value present");
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
        public ProjectVersion getPublishedProjectVersion(long id, int projectVersion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(List<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Project> getProjects(
            @Nullable Boolean apiCollections, @Nullable Long categoryId, Boolean projectDeployments,
            @Nullable Long tagId, @Nullable Status status, @Nullable Long workspaceId) {

            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> getWorkspaceProjectIds(long workspaceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> getWorkspaceProjectIds(long workspaceId, Set<ProjectOwner> excludedProjectOwners) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int publishProject(long id, @Nullable String description, boolean syncWithGit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeProjectVersion(long id, int projectVersion) {
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
    }
}
