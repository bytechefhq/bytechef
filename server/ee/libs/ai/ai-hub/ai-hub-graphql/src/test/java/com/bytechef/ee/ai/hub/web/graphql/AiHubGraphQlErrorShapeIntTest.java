/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacade;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.TitleGenerationService;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Executes real GraphQL documents against the real schema and controllers, with the services mocked, to pin the
 * client-visible error shape: classification, no data, and no hint of whether a chat exists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = {
        AiHubChatArtifactGraphQlController.class, AiHubChatGraphQlController.class,
        AiHubGraphQlExceptionResolver.class, AiHubGraphQlTestConfiguration.class,
        AiHubToolApprovalGraphQlController.class, AiHubToolApprovalRuleGraphQlController.class
    })
@GraphQlTest(
    controllers = {
        AiHubChatArtifactGraphQlController.class, AiHubChatGraphQlController.class,
        AiHubToolApprovalGraphQlController.class, AiHubToolApprovalRuleGraphQlController.class
    },
    properties = {
        "bytechef.ai.copilot.enabled=true", "bytechef.ai.hub.enabled=true", "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee", "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class AiHubGraphQlErrorShapeIntTest {

    @MockitoBean
    private AiHubChatArtifactService chatArtifactService;

    @MockitoBean
    private AiHubChatService chatService;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private JsonMapper jsonMapper;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private TitleGenerationService titleGenerationService;

    @MockitoBean
    private AiHubToolApprovalFacade toolApprovalFacade;

    @MockitoBean
    private AiHubToolApprovalRuleFacade toolApprovalRuleFacade;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);

        when(user.getId()).thenReturn(10L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(permissionService.hasWorkspaceRole(7L, "VIEWER")).thenReturn(true);
    }

    @Test
    void testMessagesOfAnotherUsersChatAreNotFoundWithNoData() {
        when(chatService.loadMessages(42L, 7L, 10L))
            .thenThrow(new NotFoundException("ai_hub_chat", 42L, "Chat 42 belongs to someone else"));

        graphQlTester
            .document("query { aiHubChatMessages(workspaceId: 7, id: 42) { role content } }")
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors)
                    .hasSize(1);
                assertThat(errors.get(0)
                    .getErrorType())
                        .hasToString("NOT_FOUND");
                assertThat(errors.get(0)
                    .getMessage())
                        .isEqualTo("Not found");
            })
            .path("aiHubChatMessages")
            .pathDoesNotExist();
    }

    @Test
    void testCancelRunOnAnotherUsersChatIsNotFoundWithNoData() {
        when(chatService.cancelAiHubRun(42L, 7L, 10L, "run-1"))
            .thenThrow(new NotFoundException("ai_hub_chat", 42L, "Chat 42 belongs to someone else"));

        graphQlTester
            .document("mutation { cancelAiHubRun(workspaceId: 7, id: 42, runId: \"run-1\") }")
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors)
                    .hasSize(1);
                assertThat(errors.get(0)
                    .getErrorType())
                        .hasToString("NOT_FOUND");
            })
            .path("cancelAiHubRun")
            .pathDoesNotExist();
    }

    @Test
    void testMissingChatAndForeignChatProduceTheSameError() {
        when(chatService.loadMessages(1L, 7L, 10L)).thenThrow(new NotFoundException("ai_hub_chat", 1L, "missing"));
        when(chatService.loadMessages(2L, 7L, 10L)).thenThrow(new NotFoundException("ai_hub_chat", 2L, "foreign"));

        String missing = firstErrorSignature("query { aiHubChatMessages(workspaceId: 7, id: 1) { role } }");
        String foreign = firstErrorSignature("query { aiHubChatMessages(workspaceId: 7, id: 2) { role } }");

        assertThat(missing)
            .isEqualTo(foreign);
    }

    @Test
    void testWorkspaceWithoutViewerRoleIsForbiddenWithNoData() {
        when(permissionService.hasWorkspaceRole(99L, "VIEWER")).thenReturn(false);

        graphQlTester
            .document("query { aiHubChatMessages(workspaceId: 99, id: 42) { role } }")
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors.get(0)
                .getErrorType())
                    .hasToString("FORBIDDEN"))
            .path("aiHubChatMessages")
            .pathDoesNotExist();
    }

    @Test
    void testApprovalsListAndResolveAreForbiddenWithoutViewerRole() {
        when(permissionService.hasWorkspaceRole(99L, "VIEWER")).thenReturn(false);

        graphQlTester
            .document("query { aiHubToolApprovals(workspaceId: 99, chatId: 42) { id } }")
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors.get(0)
                .getErrorType())
                    .hasToString("FORBIDDEN"));

        graphQlTester
            .document(
                "mutation { resolveAiHubToolApproval(workspaceId: 99, approvalId: 5, approved: true) " +
                    "{ continuationStarted } }")
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors.get(0)
                .getErrorType())
                    .hasToString("FORBIDDEN"));

        verifyNoInteractions(toolApprovalFacade);
    }

    @Test
    void testExpiredApprovalIsAConflict() {
        when(toolApprovalFacade.resolve(anyLong(), anyLong(), anyBoolean(),
            any()))
                .thenThrow(new ConflictException("Approval has expired"));

        graphQlTester
            .document(
                "mutation { resolveAiHubToolApproval(workspaceId: 7, approvalId: 5, approved: true) " +
                    "{ continuationStarted } }")
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors.get(0)
                    .getErrorType())
                        .hasToString("CONFLICT");
                assertThat(errors.get(0)
                    .getMessage())
                        .isEqualTo("Approval has expired");
            });
    }

    @Test
    void testDeletingANonReferenceArtifactIsABadRequest() {
        doThrow(new IllegalArgumentException("Only reference artifacts can be deleted"))
            .when(chatArtifactService)
            .deleteReference(5L, 7L, 10L);

        graphQlTester
            .document(
                "mutation { deleteAiHubChatArtifact(input: {workspaceId: 7, artifactId: 5}) }")
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors.get(0)
                .getErrorType())
                    .hasToString("BAD_REQUEST"));
    }

    private String firstErrorSignature(String document) {
        String[] signature = new String[1];

        graphQlTester.document(document)
            .execute()
            .errors()
            .satisfy(errors -> signature[0] = errors.get(0)
                .getErrorType() + "|"
                + errors.get(0)
                    .getMessage()
                + "|" + errors.get(0)
                    .getExtensions());

        return signature[0];
    }
}
