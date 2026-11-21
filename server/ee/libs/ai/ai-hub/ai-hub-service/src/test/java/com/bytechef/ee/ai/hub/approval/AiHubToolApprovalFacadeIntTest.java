/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvents;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.AiHubAuditIntTestConfiguration;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    AiHubToolApprovalFacadeIntTest.AiHubToolApprovalFacadeIntTestConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    AiHubChatService.class, AiHubToolApprovalRuleService.class, AiHubToolApprovalService.class, UserService.class
})
public class AiHubToolApprovalFacadeIntTest {

    private static final long APPROVAL_ID = 21L;
    private static final long CHAT_ID = 11L;
    private static final String COMMENT = "comment-not-recorded";
    private static final long DECIDER_USER_ID = 5L;
    private static final long REQUESTED_BY_USER_ID = 3L;
    private static final long WORKSPACE_ID = 7L;

    @Autowired
    private AiHubToolApprovalFacade approvalFacade;

    @Autowired
    private AiHubToolApprovalService approvalService;

    @Autowired
    private AiHubChatService chatService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        User user = new User();

        user.setId(DECIDER_USER_ID);

        when(userService.getCurrentUser()).thenReturn(user);

        AiHubChat chat = new AiHubChat(DECIDER_USER_ID);

        chat.setId(CHAT_ID);
        chat.setThreadId("thread-decided");
        chat.setWorkspaceId(WORKSPACE_ID);

        when(chatService.getManageable(CHAT_ID, WORKSPACE_ID, DECIDER_USER_ID)).thenReturn(chat);
        when(chatService.containsActiveToolCall(anyString(), any(), anyString(), anyString())).thenReturn(true);
        when(approvalService.save(any(AiHubToolApproval.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    public void afterEach() {
        SecurityContextHolder.clearContext();

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testRejectRecordsTheDecisionWithoutTheComment() {
        when(approvalService.get(APPROVAL_ID)).thenReturn(createApproval(AiHubToolApproval.Status.PENDING));

        approvalFacade.resolve(WORKSPACE_ID, APPROVAL_ID, false, COMMENT);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_DECIDED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("decision", "REJECTED")
            .containsEntry("approvalId", String.valueOf(APPROVAL_ID))
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("chatId", String.valueOf(CHAT_ID))
            .containsEntry("toolName", "sendMessage")
            .containsEntry("componentName", "slack")
            .containsEntry("requestedByUserId", String.valueOf(REQUESTED_BY_USER_ID))
            .containsEntry("decidedByUserId", String.valueOf(DECIDER_USER_ID))
            .doesNotContainKeys("arguments", "comment", "dataError", "mapperError")
            .doesNotContainValue(COMMENT);
    }

    @Test
    public void testApproveRecordsTheDecisionEvenWhenTheToolCannotRun() {
        when(approvalService.get(APPROVAL_ID)).thenReturn(createApproval(AiHubToolApproval.Status.PENDING));

        AiHubToolApprovalFacade.Resolution resolution = approvalFacade.resolve(WORKSPACE_ID, APPROVAL_ID, true, null);

        AiHubToolApproval approval = resolution.approval();

        assertThat(approval.getStatus()).isEqualTo(AiHubToolApproval.Status.FAILED);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_DECIDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("decision", "APPROVED")
            .containsEntry("approvalId", String.valueOf(APPROVAL_ID))
            .containsEntry("decidedByUserId", String.valueOf(DECIDER_USER_ID));
    }

    @Test
    public void testResolveOfADecidedApprovalRecordsAnError() {
        when(approvalService.get(APPROVAL_ID)).thenReturn(createApproval(AiHubToolApproval.Status.APPROVED));

        assertThatThrownBy(() -> approvalFacade.resolve(WORKSPACE_ID, APPROVAL_ID, true, null))
            .isInstanceOf(ConflictException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_DECIDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", ConflictException.class.getName())
            .containsEntry("decision", "APPROVED")
            .containsEntry("approvalId", String.valueOf(APPROVAL_ID))
            .doesNotContainKey("chatId");
    }

    private static AiHubToolApproval createApproval(AiHubToolApproval.Status status) {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setId(APPROVAL_ID);
        approval.setChatId(CHAT_ID);
        approval.setThreadId("thread-decided");
        approval.setRequestedByUserId(REQUESTED_BY_USER_ID);
        approval.setToolKind(AiHubToolApproval.ToolKind.COMPONENT);
        approval.setComponentName("slack");
        approval.setToolName("sendMessage");
        approval.setArguments("{\"text\":\"hello\"}");
        approval.setMode("BUILD");
        approval.setStatus(status);
        approval.setExpiresAt(Instant.now()
            .plus(1, ChronoUnit.HOURS));

        return approval;
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class AiHubToolApprovalFacadeIntTestConfiguration {

        @Bean
        AiHubToolApprovalFacade aiHubToolApprovalFacade(
            AiHubToolApprovalService approvalService, AiHubChatService chatService, UserService userService,
            ObjectProvider<AiHubPinnedToolCallbacks> pinnedToolCallbacksProvider,
            ObjectProvider<AiHubContinuationStarter> continuationStarterProvider,
            ObjectProvider<SecurityContextRehydrator> securityContextRehydratorProvider,
            PlatformTransactionManager transactionManager) {

            return new AiHubToolApprovalFacadeImpl(
                approvalService, chatService, userService, pinnedToolCallbacksProvider, continuationStarterProvider,
                securityContextRehydratorProvider.getIfAvailable(), transactionManager);
        }
    }
}
