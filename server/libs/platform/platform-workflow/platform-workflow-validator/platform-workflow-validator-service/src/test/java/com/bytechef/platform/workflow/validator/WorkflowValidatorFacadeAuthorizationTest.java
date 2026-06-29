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
 * Pins the workflow-validation gate (T22). {@code validateWorkflowById} resolves the owning project's workspace via
 * {@code hasPermission(#workflowId, 'Workflow', ...)}; {@code validateWorkflow} validates a caller-supplied definition
 * string (no id, no IDOR) and stays ungated -- a negative assertion locks that in.
 *
 * @author Ivica Cardic
 */
class WorkflowValidatorFacadeAuthorizationTest {

    @Test
    void testValidateWorkflowByIdRequiresWorkflowView() {
        Method method = findMethod("validateWorkflowById", String.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on validateWorkflowById")
            .isNotNull();
        assertThat(preAuthorize.value())
            .isEqualTo("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')");
    }

    @Test
    void testValidateWorkflowIsNotGated() {
        Method method = findMethod("validateWorkflow", String.class);

        assertThat(method.isAnnotationPresent(PreAuthorize.class))
            .as("validateWorkflow (caller-supplied definition, no IDOR) must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return WorkflowValidatorFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }
    }
}
