/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRepository;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvents;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.AiHubAuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    AiHubToolApprovalServiceIntTest.AiHubToolApprovalServiceIntTestConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    AiHubChatService.class, AiHubToolApprovalRuleService.class
})
public class AiHubToolApprovalServiceIntTest {

    private static final long CHAT_ID = 11L;
    private static final long REQUESTED_BY_USER_ID = 3L;
    private static final String ARGUMENTS = "{\"secret\":\"do-not-record\"}";

    @Autowired
    private AiHubToolApprovalRepository approvalRepository;

    @Autowired
    private AiHubToolApprovalService approvalService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    public void afterEach() {
        approvalRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testCreatePendingRecordsTheRequestWithoutItsArguments() {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setChatId(CHAT_ID);
        approval.setThreadId("thread-requested");
        approval.setRequestedByUserId(REQUESTED_BY_USER_ID);
        approval.setToolKind(AiHubToolApproval.ToolKind.COMPONENT);
        approval.setComponentName("slack");
        approval.setToolName("sendMessage");
        approval.setArguments(ARGUMENTS);
        approval.setMode("BUILD");
        approval.setEnvironment(0);

        AiHubToolApproval savedApproval = approvalService.createPending(approval);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_REQUESTED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("approvalId", String.valueOf(savedApproval.getId()))
            .containsEntry("chatId", String.valueOf(CHAT_ID))
            .containsEntry("toolName", "sendMessage")
            .containsEntry("componentName", "slack")
            .containsEntry("requestedByUserId", String.valueOf(REQUESTED_BY_USER_ID))
            .doesNotContainKeys("arguments", "dataError")
            .doesNotContainValue(ARGUMENTS);
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class AiHubToolApprovalServiceIntTestConfiguration {

        @Bean
        AiHubToolApprovalService aiHubToolApprovalService(AiHubToolApprovalRepository approvalRepository) {
            return new AiHubToolApprovalServiceImpl(approvalRepository);
        }
    }
}
