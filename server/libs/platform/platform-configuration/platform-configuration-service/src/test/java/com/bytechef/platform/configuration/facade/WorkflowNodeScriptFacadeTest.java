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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeScriptFacadeTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;

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
            List.of(), connectionService, evaluator, workflowEvaluationInputsFacade, workflowNodeOutputFacade,
            workflowNodeTestOutputFacade, workflowService, workflowTestConfigurationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    /**
     * hasPermission(#workflowId, 'Workflow', ...) on every method in this facade is environment-agnostic, so the
     * caller-supplied environmentId is never checked by the gate. These tests pin the execution side: for a confined
     * (api-key) principal the environment reaching the downstream call must be the principal's own, not the request
     * argument -- otherwise a connected user could read another environment's `vars` and test-configuration inputs by
     * passing a different environmentId, exactly the hole PrincipalEnvironment closes.
     */
    @Test
    void testGetWorkflowNodeScriptInputUsesConfinedPrincipalEnvironmentAtExecutionNotTheRequestedOne() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        Workflow workflow = workflowWithScriptTask();

        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong())).thenReturn(Map.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(Map.of());
        when(evaluator.evaluate(anyMap(), anyMap())).thenReturn(Map.of());

        // The connected-user token belongs to PRODUCTION; the request asks for DEVELOPMENT. The effective
        // environment reaching getEvaluationInputs -- the single place `vars` is merged for editor previews -- must
        // be PRODUCTION, not the requested DEVELOPMENT.
        workflowNodeScriptFacade.getWorkflowNodeScriptInput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetWorkflowNodeScriptInputHonoursSessionPrincipalRequestedEnvironmentAtExecution() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        Workflow workflow = workflowWithScriptTask();

        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong())).thenReturn(Map.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(Map.of());
        when(evaluator.evaluate(anyMap(), anyMap())).thenReturn(Map.of());

        // A session principal has no environment of its own -- the requested DEVELOPMENT must reach execution
        // unchanged. This is the containment half: the fix above must not confine an ordinary platform user too.
        workflowNodeScriptFacade.getWorkflowNodeScriptInput("workflow-1", "node-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testTestWorkflowNodeScriptUsesConfinedPrincipalEnvironmentAtExecutionNotTheRequestedOne() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong())).thenReturn(mock(WorkflowNodeTestOutput.class));

        workflowNodeScriptFacade.testWorkflowNodeScript("workflow-1", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("script_1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testTestWorkflowNodeScriptHonoursSessionPrincipalRequestedEnvironmentAtExecution() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
            anyString(), anyString(), anyLong())).thenReturn(mock(WorkflowNodeTestOutput.class));

        workflowNodeScriptFacade.testWorkflowNodeScript("workflow-1", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            eq("workflow-1"), eq("script_1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testTestClusterElementScriptUsesConfinedPrincipalEnvironmentAtExecutionNotTheRequestedOne() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        workflowNodeScriptFacade.testClusterElementScript(
            "workflow-1", "node-1", "processor", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testTestClusterElementScriptHonoursSessionPrincipalRequestedEnvironmentAtExecution() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(mock(WorkflowNodeTestOutput.class));

        workflowNodeScriptFacade.testClusterElementScript(
            "workflow-1", "node-1", "processor", "script_1", DEVELOPMENT_ORDINAL, null);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowNodeTestOutputFacade).saveClusterElementTestOutput(
            eq("workflow-1"), eq("node-1"), eq("PROCESSOR"), eq("script_1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    private static Workflow workflowWithScriptTask() {
        return new Workflow(
            "workflow-1",
            """
                {
                    "tasks": [
                        {
                            "name": "node-1",
                            "type": "javascript/v1",
                            "parameters": {
                                "input": {
                                    "key": "value"
                                }
                            }
                        }
                    ]
                }
                """,
            Format.JSON);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
