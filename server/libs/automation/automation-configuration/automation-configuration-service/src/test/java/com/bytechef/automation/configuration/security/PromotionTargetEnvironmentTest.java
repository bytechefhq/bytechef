/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Creating a project deployment is a promotion, so the role that decides it is the one held in the environment being
 * deployed <em>into</em>. Checking the caller's current environment instead would make per-environment roles
 * decorative.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class PromotionTargetEnvironmentTest {

    private static final long PROJECT_ID = 42L;
    private static final String SCOPE = "WORKFLOW_EDIT";

    @Mock
    private PermissionService permissionService;

    @Mock
    private ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;

    @InjectMocks
    private AutomationPermissionEvaluator automationPermissionEvaluator;

    @Test
    void testChecksTheEnvironmentCarriedByTheDeployment() {
        when(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, SCOPE, Environment.PRODUCTION))
            .thenReturn(false);

        boolean allowed = automationPermissionEvaluator.hasPermission(
            authentication(), projectDeployment(PROJECT_ID, Environment.PRODUCTION), SCOPE);

        assertThat(allowed).isFalse();

        verify(permissionService).hasWorkspaceScopeForProject(PROJECT_ID, SCOPE, Environment.PRODUCTION);
    }

    @Test
    void testAllowsWhenTheTargetEnvironmentGrantsTheScope() {
        when(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, SCOPE, Environment.DEVELOPMENT))
            .thenReturn(true);

        assertThat(
            automationPermissionEvaluator.hasPermission(
                authentication(), projectDeployment(PROJECT_ID, Environment.DEVELOPMENT), SCOPE)).isTrue();
    }

    @Test
    void testNeverConsultsTheAmbientEnvironment() {
        EnvironmentContext.set(Environment.DEVELOPMENT);

        try {
            when(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, SCOPE, Environment.PRODUCTION))
                .thenReturn(false);

            assertThat(
                automationPermissionEvaluator.hasPermission(
                    authentication(), projectDeployment(PROJECT_ID, Environment.PRODUCTION), SCOPE)).isFalse();

            verify(permissionService, never())
                .hasWorkspaceScopeForProject(anyLong(), anyString(), eq(Environment.DEVELOPMENT));
        } finally {
            EnvironmentContext.clear();
        }
    }

    @Test
    void testFailsClosedForAnUnrecognisedTargetObject() {
        assertThat(automationPermissionEvaluator.hasPermission(authentication(), "some-object", SCOPE)).isFalse();
    }

    private static Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());
    }

    private static ProjectDeploymentDTO projectDeployment(long projectId, Environment environment) {
        return new ProjectDeploymentDTO(
            null, null, null, true, environment, 1L, "deployment", null, null, null, null, projectId, 1, List.of(),
            List.of(), 0);
    }
}
