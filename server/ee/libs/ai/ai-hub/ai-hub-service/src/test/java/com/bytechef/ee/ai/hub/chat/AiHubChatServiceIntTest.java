/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleService;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvents;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.AiHubAuditIntTestConfiguration;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubAuditIntTestConfiguration.class,
    properties = {
        "bytechef.audit.enabled=true", "bytechef.edition=ee",
        "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-audit-testint.xml"
    })
@Import({
    AiHubChatServiceIntTest.AiHubChatServiceIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    AiHubToolApprovalRuleService.class, AiHubToolApprovalService.class, InFlightAiHubRunRegistry.class
})
public class AiHubChatServiceIntTest {

    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private AiHubChatRepository chatRepository;

    @Autowired
    private AiHubChatService chatService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    public void afterEach() {
        chatRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testCreateRecordsTheNewChat() {
        AiHubChat chat = chatService.create(WORKSPACE_ID, USER_ID, Environment.STAGING.ordinal(), "thread-created");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_CHAT_CREATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("chatId", String.valueOf(chat.getId()))
            .containsEntry("threadId", "thread-created")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("kind", AiHubChatKind.STANDARD.name())
            .containsEntry("environment", Environment.STAGING.name())
            .doesNotContainKeys("captureError", "dataError", "mapperError");
    }

    @Test
    public void testCreateOfAnExistingThreadRecordsNothingNew() {
        chatService.create(WORKSPACE_ID, USER_ID, Environment.STAGING.ordinal(), "thread-existing");
        chatService.create(WORKSPACE_ID, USER_ID, Environment.STAGING.ordinal(), "thread-existing");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_CHAT_CREATED);
    }

    @Test
    public void testCreateWorkflowChatRecordsTheNewChatWithoutItsTitle() {
        AiHubChat chat = chatService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, Environment.PRODUCTION.ordinal(), "execution-1", 42L, "title-not-recorded");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_CHAT_CREATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("chatId", String.valueOf(chat.getId()))
            .containsEntry("threadId", chat.getThreadId())
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("kind", AiHubChatKind.WORKFLOW_CHAT.name())
            .containsEntry("environment", Environment.PRODUCTION.name())
            .doesNotContainValue("title-not-recorded");
    }

    @Test
    public void testDeleteRecordsTheDeletedChat() {
        AiHubChat chat = chatService.create(WORKSPACE_ID, USER_ID, Environment.STAGING.ordinal(), "thread-deleted");

        persistenceAuditEventRepository.deleteAll();

        chatService.delete(chat.getId(), WORKSPACE_ID, USER_ID);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_CHAT_DELETED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("chatId", String.valueOf(chat.getId()))
            .containsEntry("threadId", "thread-deleted")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .doesNotContainKeys("captureError", "dataError", "mapperError");
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class AiHubChatServiceIntTestConfiguration {

        @Bean
        AiHubChatService aiHubChatService(
            AiHubChatRepository chatRepository, AiHubToolApprovalService approvalService,
            ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider,
            InFlightAiHubRunRegistry inFlightRunRegistry, PlatformTransactionManager transactionManager,
            ObjectProvider<WebhookResumeRegistry> webhookResumeRegistryProvider,
            ApplicationEventPublisher eventPublisher) {

            return new AiHubChatServiceImpl(
                chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider,
                inFlightRunRegistry, transactionManager, webhookResumeRegistryProvider, eventPublisher);
        }
    }
}
