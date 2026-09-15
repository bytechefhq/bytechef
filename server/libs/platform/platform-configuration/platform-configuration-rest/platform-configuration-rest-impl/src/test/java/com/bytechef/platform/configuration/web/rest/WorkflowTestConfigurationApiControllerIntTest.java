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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowTestConfigurationConnectionRequestModel;
import com.bytechef.platform.configuration.web.rest.model.SaveWorkflowTestConfigurationInputsRequestModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationModel;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
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
    WorkflowTestConfigurationApiController.class,
    WorkflowTestConfigurationApiControllerIntTest.MethodSecurityConfiguration.class
})
@WebMvcTest(
    controllers = WorkflowTestConfigurationApiController.class, properties = "bytechef.coordinator.enabled=true")
class WorkflowTestConfigurationApiControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowTestConfigurationApi workflowTestConfigurationApi;

    @MockitoBean
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @MockitoBean
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        doThrow(bodyReachedException).when(conversionService)
            .convert(any(), eq(WorkflowTestConfiguration.class));
        doThrow(bodyReachedException).when(workflowTestConfigurationFacade)
            .deleteWorkflowTestConfigurationConnection(anyString(), anyString(), anyString(), anyLong(), anyLong());
        doThrow(bodyReachedException).when(workflowTestConfigurationFacade)
            .saveWorkflowTestConfigurationInputs(anyString(), any(), any(), anyLong());
        doThrow(bodyReachedException).when(workflowTestConfigurationService)
            .fetchWorkflowTestConfiguration(anyString(), anyLong());
        doThrow(bodyReachedException).when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationConnections(anyString(), anyString(), anyLong());

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
        assertThatThrownBy(() -> guardedOperation.invoke(workflowTestConfigurationApi))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @ParameterizedTest
    @MethodSource("guardedOperations")
    void testGuardAllowsWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsGranted(GuardedOperation guardedOperation) {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(true);

        assertBodyReached(() -> guardedOperation.invoke(workflowTestConfigurationApi));
    }

    @Test
    void testSaveWorkflowTestConfigurationWithoutABodyDeniesWhenTheWorkflowEditScopeInDevelopmentIsRefused() {
        assertThatThrownBy(() -> workflowTestConfigurationApi.saveWorkflowTestConfiguration(WORKFLOW_ID, null))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT);
    }

    @Test
    void testSaveWorkflowTestConfigurationWithoutABodyAllowsWhenTheWorkflowEditScopeInDevelopmentIsGranted() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .thenReturn(true);

        assertThatThrownBy(() -> workflowTestConfigurationApi.saveWorkflowTestConfiguration(WORKFLOW_ID, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("workflowTestConfigurationModel");
    }

    @Test
    void testTestConfigurationReadsAreNotGuarded() {
        assertBodyReached(
            () -> workflowTestConfigurationApi.getWorkflowTestConfiguration(WORKFLOW_ID, PRODUCTION_ENVIRONMENT_ID));
        assertBodyReached(
            () -> workflowTestConfigurationApi.getWorkflowTestConfigurationConnections(
                WORKFLOW_ID, "node_1", PRODUCTION_ENVIRONMENT_ID));
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames =
            Arrays.stream(WorkflowTestConfigurationApiController.class.getDeclaredMethods())
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
                "saveWorkflowTestConfiguration",
                (GuardedOperation) guardedApi -> guardedApi.saveWorkflowTestConfiguration(
                    WORKFLOW_ID, new WorkflowTestConfigurationModel().environmentId(PRODUCTION_ENVIRONMENT_ID))),
            Named.of(
                "deleteWorkflowTestConfigurationConnection",
                (GuardedOperation) guardedApi -> guardedApi.deleteWorkflowTestConfigurationConnection(
                    WORKFLOW_ID, "node_1", "key", PRODUCTION_ENVIRONMENT_ID,
                    new DeleteWorkflowTestConfigurationConnectionRequestModel().connectionId(1L))),
            Named.of(
                "saveWorkflowTestConfigurationInputs",
                (GuardedOperation) guardedApi -> guardedApi.saveWorkflowTestConfigurationInputs(
                    WORKFLOW_ID, PRODUCTION_ENVIRONMENT_ID,
                    new SaveWorkflowTestConfigurationInputsRequestModel().key("key"))));
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @FunctionalInterface
    interface GuardedOperation {

        void invoke(WorkflowTestConfigurationApi guardedApi);
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean
        @Primary
        ConversionService conversionService() {
            return mock(ConversionService.class, MockReset.withSettings(MockReset.AFTER));
        }

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class, MockReset.withSettings(MockReset.AFTER));
        }
    }
}
