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

package com.bytechef.platform.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class, WorkflowTestConfigurationGraphQlController.class,
    WorkflowTestConfigurationGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = WorkflowTestConfigurationGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class WorkflowTestConfigurationGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CONNECTION_ID = 5L;
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private PermissionService permissionService;

    @MockitoBean
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Autowired
    private WorkflowTestConfigurationGraphQlController workflowTestConfigurationGraphQlController;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        doThrow(bodyReachedException).when(workflowTestConfigurationFacade)
            .saveClusterElementTestConfigurationConnection(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyLong());
        doThrow(bodyReachedException).when(workflowTestConfigurationFacade)
            .saveWorkflowTestConfigurationConnection(anyString(), anyString(), anyString(), anyLong(), anyLong());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("guardedOperations")
    void testGuardDeniesWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsRefused(GuardedOperation guardedOperation) {
        assertThatThrownBy(() -> guardedOperation.invoke(workflowTestConfigurationGraphQlController))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @ParameterizedTest
    @MethodSource("guardedOperations")
    void testGuardAllowsWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsGranted(GuardedOperation guardedOperation) {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(true);

        assertThatThrownBy(() -> guardedOperation.invoke(workflowTestConfigurationGraphQlController))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames =
            Arrays.stream(WorkflowTestConfigurationGraphQlController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

        Set<String> coveredMethodNames = guardedOperations()
            .map(Named::getName)
            .map(name -> name.split(" ")[0])
            .collect(Collectors.toSet());

        assertThat(coveredMethodNames).isEqualTo(guardedMethodNames);
    }

    static Stream<Named<GuardedOperation>> guardedOperations() {
        return Stream.of(
            Named.of(
                "saveClusterElementTestConfigurationConnection",
                (GuardedOperation) guardedController -> guardedController.saveClusterElementTestConfigurationConnection(
                    WORKFLOW_ID, "node_1", "tools", "tool_1", "key", CONNECTION_ID, PRODUCTION_ENVIRONMENT_ID)),
            Named.of(
                "saveWorkflowTestConfigurationConnection",
                (GuardedOperation) guardedController -> guardedController.saveWorkflowTestConfigurationConnection(
                    WORKFLOW_ID, "node_1", "key", CONNECTION_ID, PRODUCTION_ENVIRONMENT_ID)));
    }

    @FunctionalInterface
    interface GuardedOperation {

        void invoke(WorkflowTestConfigurationGraphQlController guardedController);
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
        }
    }
}
