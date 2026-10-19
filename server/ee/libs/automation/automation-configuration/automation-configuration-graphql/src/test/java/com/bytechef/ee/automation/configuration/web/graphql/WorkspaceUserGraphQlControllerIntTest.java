/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = WorkspaceUserGraphQlController.class)
@GraphQlTest(
    controllers = WorkspaceUserGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class WorkspaceUserGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private PermissionService permissionService;

    @Test
    void testMyWorkspaceScopesReturnsTheCallersScopes() {
        when(permissionService.getMyWorkspaceScopes(7L)).thenReturn(Set.of("API_KEY_VIEW", "API_KEY_CREATE"));

        graphQlTester.document("{ myWorkspaceScopes(workspaceId: \"7\", environment: PRODUCTION) }")
            .execute()
            .path("myWorkspaceScopes")
            .entityList(String.class)
            .hasSize(2)
            .contains("API_KEY_VIEW", "API_KEY_CREATE");

        verify(permissionService).getMyWorkspaceScopes(7L);
    }

    @Test
    void testMyWorkspaceScopesWithoutAnEnvironment() {
        when(permissionService.getMyWorkspaceScopes(7L)).thenReturn(Set.of());

        graphQlTester.document("{ myWorkspaceScopes(workspaceId: \"7\") }")
            .execute()
            .path("myWorkspaceScopes")
            .entityList(String.class)
            .hasSize(0);
    }

    @Test
    void testMyWorkspaceRole() {
        when(permissionService.getMyWorkspaceRole(7L)).thenReturn("VIEWER");

        graphQlTester.document("{ myWorkspaceRole(workspaceId: \"7\") }")
            .execute()
            .path("myWorkspaceRole")
            .entity(String.class)
            .isEqualTo("VIEWER");
    }

    @Test
    void testMyWorkspaceScopesIsDeniedWhenTheServiceDenies() {
        when(permissionService.getMyWorkspaceScopes(7L)).thenThrow(new AccessDeniedException("Access Denied"));

        graphQlTester.document("{ myWorkspaceScopes(workspaceId: \"7\") }")
            .execute()
            .errors()
            .expect(error -> true)
            .verify();
    }
}
