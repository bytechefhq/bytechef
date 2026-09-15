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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.web.rest.model.DeleteClusterElementParameterRequestModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateClusterElementParameterRequestModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateWorkflowNodeParameterRequestModel;
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
    WorkflowNodeParameterApiController.class,
    WorkflowNodeParameterApiControllerIntTest.MethodSecurityConfiguration.class
})
@WebMvcTest(controllers = WorkflowNodeParameterApiController.class, properties = "bytechef.coordinator.enabled=true")
class WorkflowNodeParameterApiControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private WorkflowNodeParameterApi workflowNodeParameterApi;

    @MockitoBean
    private WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .deleteClusterElementParameter(anyString(), anyString(), anyString(), anyString(), any(), anyLong());
        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .deleteWorkflowNodeParameter(anyString(), anyString(), any(), anyLong());
        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .getClusterElementDisplayConditions(anyString(), anyString(), anyString(), anyString(), anyLong());
        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .getWorkflowNodeDisplayConditions(anyString(), anyString(), anyLong());
        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .updateClusterElementParameter(
                anyString(), anyString(), anyString(), anyString(), any(), any(), any(), anyBoolean(), anyBoolean(),
                anyLong());
        doThrow(bodyReachedException).when(workflowNodeParameterFacade)
            .updateWorkflowNodeParameter(anyString(), anyString(), any(), any(), any(), anyBoolean(), anyLong());

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
    void testGuardDeniesWhenTheWorkflowEditScopeInDevelopmentIsRefused(GuardedOperation guardedOperation) {

        assertThatThrownBy(() -> guardedOperation.invoke(workflowNodeParameterApi))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT);
    }

    @ParameterizedTest
    @MethodSource("guardedOperations")
    void testGuardAllowsWhenTheWorkflowEditScopeInDevelopmentIsGranted(GuardedOperation guardedOperation) {

        when(permissionService.hasWorkflowScopeIfProjectWorkflow(
            WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT)).thenReturn(true);

        assertBodyReached(() -> guardedOperation.invoke(workflowNodeParameterApi));
    }

    @Test
    void testDisplayConditionReadsAreNotGuarded() {
        assertBodyReached(
            () -> workflowNodeParameterApi.getWorkflowNodeParameterDisplayConditions(
                WORKFLOW_ID, "node_1", ENVIRONMENT_ID));
        assertBodyReached(
            () -> workflowNodeParameterApi.getClusterElementParameterDisplayConditions(
                WORKFLOW_ID, "node_1", "tools", "tool_1", ENVIRONMENT_ID));
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(WorkflowNodeParameterApiController.class.getDeclaredMethods())
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
                "deleteClusterElementParameter",
                (GuardedOperation) guardedApi -> guardedApi.deleteClusterElementParameter(
                    WORKFLOW_ID, "node_1", "tools", "tool_1", ENVIRONMENT_ID,
                    new DeleteClusterElementParameterRequestModel())),
            Named.of(
                "deleteWorkflowNodeParameter",
                (GuardedOperation) guardedApi -> guardedApi.deleteWorkflowNodeParameter(
                    WORKFLOW_ID, "node_1", ENVIRONMENT_ID, new DeleteClusterElementParameterRequestModel())),
            Named.of(
                "updateClusterElementParameter",
                (GuardedOperation) guardedApi -> guardedApi.updateClusterElementParameter(
                    WORKFLOW_ID, "node_1", "tools", "tool_1", ENVIRONMENT_ID,
                    new UpdateClusterElementParameterRequestModel()
                        .fromAiInMetadata(false)
                        .includeInMetadata(false))),
            Named.of(
                "updateWorkflowNodeParameter",
                (GuardedOperation) guardedApi -> guardedApi.updateWorkflowNodeParameter(
                    WORKFLOW_ID, "node_1", ENVIRONMENT_ID, new UpdateWorkflowNodeParameterRequestModel()
                        .includeInMetadata(false))));
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @FunctionalInterface
    interface GuardedOperation {

        void invoke(WorkflowNodeParameterApi guardedApi);
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
