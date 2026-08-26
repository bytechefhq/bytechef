/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionAuthorizerTest {

    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private final PromotionAuthorizer promotionAuthorizer =
        new PromotionAuthorizer(apiCollectionService, projectDeploymentService, workspaceMcpServerService);

    @Test
    void testProjectIdOfApiCollectionIsResolvedThroughItsDeployment() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(100L);
        apiCollection.setProjectDeploymentId(300L);

        when(apiCollectionService.fetchApiCollection(100L)).thenReturn(Optional.of(apiCollection));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(300L);
        projectDeployment.setProjectId(42L);

        when(projectDeploymentService.getProjectDeployment(300L)).thenReturn(projectDeployment);

        assertThat(promotionAuthorizer.projectIdOfApiCollection(100L)).isEqualTo(42L);
    }

    /**
     * The guard runs before any authorization verdict exists, so "no such collection" and "a collection you may not
     * see" must be indistinguishable to the caller — otherwise the guard is an id-enumeration oracle. It must also not
     * throw {@code NoSuchElementException}, which would escape expression evaluation as a 500 rather than a 403.
     */
    @Test
    void testUnknownApiCollectionIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(apiCollectionService.fetchApiCollection(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionAuthorizer.projectIdOfApiCollection(404L))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    void testWorkspaceIdOfMcpServerIsResolvedThroughItsWorkspaceAssignment() {
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(100L)).thenReturn(Optional.of(5L));

        assertThat(promotionAuthorizer.workspaceIdOfMcpServer(100L)).isEqualTo(5L);
    }

    /**
     * Same disclosure argument as the API collection lookup: this runs before any authorization verdict exists, so a
     * server that does not exist and one assigned to a workspace the caller cannot see must be indistinguishable. An
     * MCP server with no workspace row lands here too — nothing else scopes it to a caller.
     */
    @Test
    void testMcpServerWithoutAWorkspaceIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionAuthorizer.workspaceIdOfMcpServer(404L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testProjectIdOfProjectDeploymentIsResolvedThroughTheDeploymentItself() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(300L);
        projectDeployment.setProjectId(42L);

        when(projectDeploymentService.fetchProjectDeployment(300L)).thenReturn(Optional.of(projectDeployment));

        assertThat(promotionAuthorizer.projectIdOfProjectDeployment(300L)).isEqualTo(42L);
    }

    /**
     * Same disclosure argument as the other two lookups: this runs before any authorization verdict exists, so "no such
     * deployment" and "a deployment you may not see" must be indistinguishable to the caller, and it must not throw
     * {@code NoSuchElementException}, which would escape expression evaluation as a 500 rather than a 403.
     */
    @Test
    void testUnknownProjectDeploymentIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(projectDeploymentService.fetchProjectDeployment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionAuthorizer.projectIdOfProjectDeployment(404L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
