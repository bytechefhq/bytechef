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

package com.bytechef.platform.workflow.test.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Running a workflow test executes the workflow with its test connections, so starting, running and stopping one needs
 * {@code WORKFLOW_EDIT}.
 *
 * @author Ivica Cardic
 */
class TestWorkflowExecutorAuthorizationTest {

    private static final String IN_REQUESTED_ENVIRONMENT =
        "hasWorkflowScopeIfProjectWorkflowInEnvironmentId(#workflowId, 'WORKFLOW_EDIT', #environmentId)";

    @Test
    void testStartingATestNeedsTheWorkflowEditScopeInTheRequestedEnvironment() throws NoSuchMethodException {
        assertThat(getGate("executeAsync", String.class, Map.class, long.class, Consumer.class, Function.class,
            BiConsumer.class, Consumer.class)).isEqualTo(IN_REQUESTED_ENVIRONMENT);
        assertThat(getGate("executeSync", String.class, Map.class, long.class)).isEqualTo(IN_REQUESTED_ENVIRONMENT);
    }

    @Test
    void testStoppingATestNeedsTheWorkflowEditScopeOnTheTestJob() throws NoSuchMethodException {
        assertThat(getGate("stop", long.class)).isEqualTo("hasPermission(#jobId, 'TestJob', 'WORKFLOW_EDIT')");
    }

    private static String getGate(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        PreAuthorize preAuthorize = TestWorkflowExecutorImpl.class.getMethod(methodName, parameterTypes)
            .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", methodName)
            .isNotNull();

        return preAuthorize.value();
    }
}
