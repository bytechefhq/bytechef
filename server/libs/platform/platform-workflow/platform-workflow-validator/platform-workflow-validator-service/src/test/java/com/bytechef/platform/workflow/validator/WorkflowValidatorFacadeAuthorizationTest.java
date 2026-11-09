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

package com.bytechef.platform.workflow.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorFacadeAuthorizationTest {

    private static final String IN_DEVELOPMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironment(#workflowId, 'WORKFLOW_VIEW', "
            + "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT)";
    private static final String IN_REQUESTED_ENVIRONMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_VIEW', #environmentId)";

    @Test
    void testValidateWorkflowByIdNeedsTheWorkflowViewScopeInDevelopment() {
        assertThat(getGate("validateWorkflowById", String.class)).isEqualTo(IN_DEVELOPMENT);
    }

    @Test
    void testValidateWorkflowByIdInEnvironmentNeedsTheWorkflowViewScopeInThatEnvironment() {
        assertThat(getGate("validateWorkflowById", String.class, long.class)).isEqualTo(IN_REQUESTED_ENVIRONMENT);
    }

    @Test
    void testValidateWorkflowOfAStoredWorkflowNeedsTheWorkflowViewScopeInThatEnvironment() {
        assertThat(getGate("validateWorkflow", String.class, String.class, long.class))
            .isEqualTo(IN_REQUESTED_ENVIRONMENT);
    }

    @Test
    void testValidateWorkflowOfABareDefinitionIsNotGated() {
        assertThat(findMethod("validateWorkflow", String.class).isAnnotationPresent(PreAuthorize.class)).isFalse();
        assertThat(findMethod("validateWorkflow", String.class, long.class).isAnnotationPresent(PreAuthorize.class))
            .isFalse();
    }

    private static String getGate(String methodName, Class<?>... parameterTypes) {
        PreAuthorize preAuthorize = findMethod(methodName, parameterTypes).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();

        return preAuthorize.value();
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return WorkflowValidatorFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new AssertionError("method " + methodName + " not found", noSuchMethodException);
        }
    }
}
