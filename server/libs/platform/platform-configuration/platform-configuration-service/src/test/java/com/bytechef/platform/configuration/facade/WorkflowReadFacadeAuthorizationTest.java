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
 * Reading what a workflow's nodes resolve to - their options, dynamic properties, outputs, descriptions, connections
 * and script input - needs {@code WORKFLOW_VIEW} on the workflow's project.
 *
 * @author Ivica Cardic
 */
class WorkflowReadFacadeAuthorizationTest {

    private static final String IN_DEVELOPMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'WORKFLOW_VIEW', "
            + "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)";
    private static final String IN_REQUESTED_ENVIRONMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_VIEW', #environmentId)";

    @ParameterizedTest
    @MethodSource("guardedFacades")
    void testEveryWorkflowReadNeedsTheWorkflowViewScope(Class<?> facadeClass, Map<String, String> expectedGates) {
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
    void testEveryWorkflowIdOverloadOfAGuardedMethodIsGuarded(
        Class<?> facadeClass, Map<String, String> expectedGates) {

        assertThat(Arrays.stream(facadeClass.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
            .filter(method -> expectedGates.containsKey(method.getName()))
            .filter(method -> method.getParameterCount() > 0 && method.getParameterTypes()[0] == String.class)
            .filter(method -> !method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::toGenericString))
                .isEmpty();
    }

    static Stream<Arguments> guardedFacades() {
        return Stream.of(
            Arguments.of(
                ComponentConnectionFacadeImpl.class,
                Map.of(
                    "getClusterElementComponentConnection", IN_DEVELOPMENT,
                    "getClusterElementComponentConnections", IN_DEVELOPMENT,
                    "getComponentConnection", IN_DEVELOPMENT,
                    "getWorkflowNodeComponentConnections", IN_DEVELOPMENT)),
            Arguments.of(WorkflowFacadeImpl.class, Map.of("hasSseStreamResponse", IN_DEVELOPMENT)),
            Arguments.of(
                WorkflowNodeDescriptionFacadeImpl.class,
                Map.of(
                    "getClusterElementWorkflowNodeDescription", IN_REQUESTED_ENVIRONMENT,
                    "getWorkflowNodeDescription", IN_REQUESTED_ENVIRONMENT)),
            Arguments.of(
                WorkflowNodeDynamicPropertiesFacadeImpl.class,
                Map.of(
                    "getClusterElementDynamicProperties", IN_REQUESTED_ENVIRONMENT,
                    "getWorkflowNodeDynamicProperties", IN_REQUESTED_ENVIRONMENT)),
            Arguments.of(
                WorkflowNodeOptionFacadeImpl.class,
                Map.of(
                    "getClusterElementNodeOptions", IN_REQUESTED_ENVIRONMENT,
                    "getWorkflowNodeOptions", IN_REQUESTED_ENVIRONMENT)),
            Arguments.of(
                WorkflowNodeOutputFacadeImpl.class,
                Map.of(
                    "checkWorkflowCache", IN_REQUESTED_ENVIRONMENT,
                    "getClusterElementOutput", IN_REQUESTED_ENVIRONMENT,
                    "getPreviousWorkflowNodeSampleOutputs", IN_REQUESTED_ENVIRONMENT)));
    }
}
