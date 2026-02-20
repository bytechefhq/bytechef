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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowNodeScriptFacadeTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeScriptFacadeImpl workflowNodeScriptFacade;

    @BeforeEach
    void setUp() {
        workflowNodeScriptFacade = new WorkflowNodeScriptFacadeImpl(
            List.of(), connectionService, evaluator, workflowNodeOutputFacade, workflowNodeTestOutputFacade,
            workflowService, workflowTestConfigurationService);
    }

    @Test
    void testTestClusterElementScriptWithNullInputParametersCallsOverloadWithoutInputParameters() {
        WorkflowNodeTestOutput testOutput = mock(WorkflowNodeTestOutput.class);

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong())).thenReturn(testOutput);

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testClusterElementScript(
            "workflow-1", "node-1", "processor", "script_1", 1L, null);

        assertNotNull(result);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"), eq(1L));

        verify(workflowNodeTestOutputFacade, never()).saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyMap(), anyLong());
    }

    @Test
    void testTestClusterElementScriptWithInputParametersCallsOverloadWithInputParameters() {
        WorkflowNodeTestOutput testOutput = mock(WorkflowNodeTestOutput.class);

        Map<String, Object> inputParameters = Map.of("key", "value");

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyMap(), anyLong())).thenReturn(testOutput);

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testClusterElementScript(
            "workflow-1", "node-1", "processor", "script_1", 1L, inputParameters);

        assertNotNull(result);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"),
            eq(Map.of("input", inputParameters)), eq(1L));

        verify(workflowNodeTestOutputFacade, never()).saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void testTestWorkflowNodeScriptWithNullInputParametersCallsOverloadWithoutInputParameters() {
        WorkflowNodeTestOutput testOutput = mock(WorkflowNodeTestOutput.class);

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong())).thenReturn(testOutput);

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testWorkflowNodeScript(
            "workflow-1", "script_1", 1L, null);

        assertNotNull(result);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("script_1"), eq(1L));

        verify(workflowNodeTestOutputFacade, never()).saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyMap(), anyLong());
    }

    @Test
    void testTestWorkflowNodeScriptWithInputParametersCallsOverloadWithInputParameters() {
        WorkflowNodeTestOutput testOutput = mock(WorkflowNodeTestOutput.class);

        Map<String, Object> inputParameters = Map.of("data", "test");

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyMap(), anyLong())).thenReturn(testOutput);

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testWorkflowNodeScript(
            "workflow-1", "script_1", 1L, inputParameters);

        assertNotNull(result);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("script_1"), eq(Map.of("input", inputParameters)), eq(1L));

        verify(workflowNodeTestOutputFacade, never()).saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong());
    }

    @Test
    void testTestWorkflowNodeScriptReturnsErrorOnException() {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong())).thenThrow(new RuntimeException("Script execution failed"));

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testWorkflowNodeScript(
            "workflow-1", "script_1", 1L, null);

        assertNotNull(result);
        assertNotNull(result.error());
        assertEquals("Script execution failed", result.error()
            .getMessage());
        assertNull(result.output());
    }

    @Test
    void testTestClusterElementScriptReturnsErrorOnException() {
        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("Element execution failed"));

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testClusterElementScript(
            "workflow-1", "node-1", "processor", "script_1", 1L, null);

        assertNotNull(result);
        assertNotNull(result.error());
        assertEquals("Element execution failed", result.error()
            .getMessage());
        assertNull(result.output());
    }

    @Test
    void testTestWorkflowNodeScriptReturnsNullOutputWhenTestOutputIsNull() {
        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong())).thenReturn(null);

        ScriptTestExecutionDTO result = workflowNodeScriptFacade.testWorkflowNodeScript(
            "workflow-1", "script_1", 1L, null);

        assertNotNull(result);
        assertNull(result.error());
        assertNull(result.output());
    }
}
