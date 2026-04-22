/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 */
class WorkspaceAuthorizationTest {

    private PermissionService permissionService;
    private WorkspaceAuthorization workspaceAuthorization;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        workspaceAuthorization = new WorkspaceAuthorization(permissionService);
    }

    @Test
    void testAllowsWhenPermissionServiceReturnsTrue() {
        when(permissionService.hasWorkspaceRole(1L, "VIEWER")).thenReturn(true);

        assertDoesNotThrow(() -> workspaceAuthorization.requireWorkspaceRole(1L, "VIEWER"));
    }

    @Test
    void testDeniesWithDescriptiveMessageWhenRoleInsufficient() {
        when(permissionService.hasWorkspaceRole(1L, "ADMIN")).thenReturn(false);

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class, () -> workspaceAuthorization.requireWorkspaceRole(1L, "ADMIN"));

        assertEquals("Not authorized for workspace 1 (requires ADMIN)", exception.getMessage());
    }
}
