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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.Serializable;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
@ContextConfiguration(classes = AutomationGettingStartedFacadeIntTest.MethodSecurityConfiguration.class)
class AutomationGettingStartedFacadeIntTest {

    private static final String GETTING_STARTED = "gettingStarted";
    private static final long OTHER_WORKSPACE_ID = 12L;
    private static final long WORKSPACE_ID = 11L;

    @Autowired
    private AutomationGettingStartedFacade automationGettingStartedFacade;

    @MockitoBean
    private PermissionService permissionService;

    @Autowired
    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));
    }

    @AfterEach
    void afterEach() {
        propertyService.delete(GETTING_STARTED, Scope.AUTOMATION, null);

        SecurityContextHolder.clearContext();
    }

    @Test
    void testDismissInOneWorkspaceCompletesAnotherWorkspace() {
        propertyService.delete(GETTING_STARTED, Scope.AUTOMATION, null);

        Serializable workspaceId = WORKSPACE_ID;
        Serializable otherWorkspaceId = OTHER_WORKSPACE_ID;

        when(permissionService.hasResourceScope(workspaceId, "Workspace", "PROJECT_CREATE")).thenReturn(true);
        when(permissionService.hasResourceScope(otherWorkspaceId, "Workspace", "PROJECT_CREATE")).thenReturn(true);

        automationGettingStartedFacade.dismissAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(OTHER_WORKSPACE_ID)).isTrue();
        assertThat(propertyService.getProperties(List.of(GETTING_STARTED), Scope.AUTOMATION, null)).hasSize(1);
    }

    @Test
    void testDismissAutomationGettingStartedRequiresProjectCreate() {
        assertRequiresProjectCreate(() -> automationGettingStartedFacade.dismissAutomationGettingStarted(WORKSPACE_ID));
    }

    @Test
    void testGetAutomationGettingStartedRequiresProjectCreate() {
        assertRequiresProjectCreate(() -> automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID));
    }

    @Test
    void testIsAutomationGettingStartedCompletedRequiresProjectCreate() {
        assertRequiresProjectCreate(
            () -> automationGettingStartedFacade.isAutomationGettingStartedCompleted(WORKSPACE_ID));
    }

    private void assertRequiresProjectCreate(ThrowingCallable call) {
        reset(permissionService);

        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);

        Serializable workspaceId = WORKSPACE_ID;

        when(permissionService.hasResourceScope(workspaceId, "Workspace", "PROJECT_CREATE")).thenReturn(true);

        Throwable throwable = catchThrowable(call);

        assertThat(throwable)
            .as("Expected the gate to let the call through, but it was denied: %s", throwable)
            .satisfiesAnyOf(
                actual -> assertThat(actual).isNull(),
                actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));

        reset(permissionService);
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
