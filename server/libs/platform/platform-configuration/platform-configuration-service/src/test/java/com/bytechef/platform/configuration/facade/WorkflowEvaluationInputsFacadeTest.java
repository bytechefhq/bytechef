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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.variable.WorkflowVariablesResolver;
import com.bytechef.platform.workflow.JobInputConstants;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class WorkflowEvaluationInputsFacadeTest {

    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);

    @Test
    @SuppressWarnings("unchecked")
    void testMergesTestConfigurationInputsWithVars() {
        WorkflowVariablesResolver resolver = mock(WorkflowVariablesResolver.class);
        ObjectProvider<WorkflowVariablesResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resolver);
        doReturn(Map.of("name", "x"))
            .when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs("wf", 0L);
        when(resolver.resolveForWorkflow("wf", 0L)).thenReturn(Map.of("A", "1"));

        WorkflowEvaluationInputsFacadeImpl facade = new WorkflowEvaluationInputsFacadeImpl(
            objectProvider, workflowTestConfigurationService);

        Map<String, ?> inputs = facade.getEvaluationInputs("wf", 0L);

        assertEquals("x", inputs.get("name"));
        assertEquals(Map.of("A", "1"), inputs.get(JobInputConstants.VARIABLES_INPUT));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReturnsInputsOnlyWithoutResolver() {
        ObjectProvider<WorkflowVariablesResolver> objectProvider = mock(ObjectProvider.class);

        doReturn(Map.of("name", "x"))
            .when(workflowTestConfigurationService)
            .getWorkflowTestConfigurationInputs("wf", 0L);

        WorkflowEvaluationInputsFacadeImpl facade = new WorkflowEvaluationInputsFacadeImpl(
            objectProvider, workflowTestConfigurationService);

        assertFalse(facade.getEvaluationInputs("wf", 0L)
            .containsKey(JobInputConstants.VARIABLES_INPUT));
    }
}
