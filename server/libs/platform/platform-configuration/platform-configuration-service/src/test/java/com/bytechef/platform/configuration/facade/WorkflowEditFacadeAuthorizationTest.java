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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Changing a workflow's node parameters, test configuration or test output, or running its scripts, needs
 * {@code WORKFLOW_EDIT} on the workflow's project.
 *
 * @author Ivica Cardic
 */
class WorkflowEditFacadeAuthorizationTest {

    private static final String IN_DEVELOPMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'WORKFLOW_EDIT', "
            + "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)";
    private static final String IN_REQUESTED_ENVIRONMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_EDIT', #environmentId)";
    private static final String IN_REQUESTED_ENVIRONMENT_WITH_A_VISIBLE_CONNECTION =
        IN_REQUESTED_ENVIRONMENT + " && " +
            "hasResourceScopeIfProjectWorkflow(#workflowId, #connectionId, 'Connection', 'CONNECTION_VIEW')";

    @ParameterizedTest
    @MethodSource("guardedFacades")
    void testEveryMutatingMethodNeedsTheWorkflowEditScope(Class<?> facadeClass, Map<String, String> expectedGates) {
        Map<String, String> gates = Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .collect(Collectors.toMap(
                Method::getName, method -> method.getAnnotation(PreAuthorize.class)
                    .value(),
                (first, second) -> {
                    assertThat(second).isEqualTo(first);

                    return first;
                }));

        assertThat(gates).isEqualTo(expectedGates);
    }

    @ParameterizedTest
    @MethodSource("guardedFacades")
    void testEveryOverloadOfAGuardedMethodIsGuarded(Class<?> facadeClass, Map<String, String> expectedGates)
        throws NoSuchMethodException {

        Method webhookTestOutputMethod = WorkflowNodeTestOutputFacadeImpl.class.getMethod(
            "saveWorkflowNodeTestOutput", WorkflowExecutionId.class, long.class, WebhookRequest.class);

        // A webhook test delivery is not a user request; the user who started the test was checked then.
        assertThat(Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
            .filter(method -> expectedGates.containsKey(method.getName()))
            .filter(method -> !method.equals(webhookTestOutputMethod))
            .filter(method -> !method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::toGenericString))
                .isEmpty();
    }

    static Stream<Arguments> guardedFacades() {
        return Stream.of(
            Arguments.of(
                WorkflowNodeParameterFacadeImpl.class,
                Map.of(
                    "deleteClusterElementParameter", IN_DEVELOPMENT,
                    "deleteWorkflowNodeParameter", IN_DEVELOPMENT,
                    "updateClusterElementParameter", IN_DEVELOPMENT,
                    "updateWorkflowNodeParameter", IN_DEVELOPMENT)),
            Arguments.of(
                WorkflowNodeScriptFacadeImpl.class,
                Map.of(
                    "testClusterElementScript", IN_REQUESTED_ENVIRONMENT,
                    "testWorkflowNodeScript", IN_REQUESTED_ENVIRONMENT)),
            Arguments.of(
                WorkflowNodeTestOutputFacadeImpl.class,
                Map.of(
                    "deleteWorkflowNodeTestOutput", IN_REQUESTED_ENVIRONMENT,
                    "saveClusterElementTestOutput", IN_REQUESTED_ENVIRONMENT,
                    "saveWorkflowNodeSampleFile", IN_REQUESTED_ENVIRONMENT,
                    "saveWorkflowNodeSampleOutput", IN_REQUESTED_ENVIRONMENT,
                    "saveWorkflowNodeTestOutput", IN_REQUESTED_ENVIRONMENT)),
            Arguments.of(
                WorkflowTestConfigurationFacadeImpl.class,
                Map.of(
                    "deleteWorkflowTestConfigurationConnection", IN_REQUESTED_ENVIRONMENT,
                    "fetchWorkflowTestConfiguration",
                    "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_VIEW', #environmentId)",
                    "getWorkflowTestConfigurationConnections",
                    "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_VIEW', #environmentId)",
                    "saveClusterElementTestConfigurationConnection", IN_REQUESTED_ENVIRONMENT_WITH_A_VISIBLE_CONNECTION,
                    "saveWorkflowTestConfiguration",
                    "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowTestConfiguration.workflowId, "
                        + "'WORKFLOW_EDIT', #workflowTestConfiguration.environmentId)",
                    "saveWorkflowTestConfigurationConnection", IN_REQUESTED_ENVIRONMENT_WITH_A_VISIBLE_CONNECTION,
                    "saveWorkflowTestConfigurationInputs", IN_REQUESTED_ENVIRONMENT)));
    }
}
