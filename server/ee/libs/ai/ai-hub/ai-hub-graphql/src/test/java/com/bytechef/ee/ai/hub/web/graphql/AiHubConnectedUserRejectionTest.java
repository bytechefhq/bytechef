/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalFacade;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRuleFacade;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The embedded connected-user token also authenticates {@code /graphql}; the hub's GraphQL entry points refuse it
 * before any permission or chat lookup, as the REST turn endpoint does.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConnectedUserRejectionTest {

    private final PermissionService permissionService = mock(PermissionService.class);

    @BeforeEach
    void authenticateConnectedUser() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(TestConnectedUserAuthentication.of("someone@localhost.com"));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testVerifyViewerRefusesAConnectedUserBeforeTheRoleLookup() {
        assertThatThrownBy(() -> AiHubWorkspaceAccess.verifyViewer(permissionService, 7L))
            .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testToolApprovalResolutionRefusesAConnectedUser() {
        AiHubToolApprovalFacade toolApprovalFacade = mock(AiHubToolApprovalFacade.class);

        AiHubToolApprovalGraphQlController controller =
            new AiHubToolApprovalGraphQlController(toolApprovalFacade, permissionService);

        assertThatThrownBy(() -> controller.resolveAiHubToolApproval(7L, 42L, true, null))
            .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(toolApprovalFacade, permissionService);
    }

    @Test
    void testToolApprovalRuleEntryPointsRefuseAConnectedUser() {
        AiHubToolApprovalRuleFacade toolApprovalRuleFacade = mock(AiHubToolApprovalRuleFacade.class);

        AiHubToolApprovalRuleGraphQlController controller =
            new AiHubToolApprovalRuleGraphQlController(toolApprovalRuleFacade);

        assertThatThrownBy(() -> controller.aiHubToolApprovalRules(7L))
            .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(controller::aiHubToolApprovalDefaultToolNames)
            .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.createAiHubToolApprovalRule(
            7L, AiHubToolApproval.ToolKind.CATALOG, "deleteProject", AiHubToolApprovalRule.Mode.EXEMPT))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.deleteAiHubToolApprovalRule(7L, 42L))
            .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(toolApprovalRuleFacade);
    }
}
