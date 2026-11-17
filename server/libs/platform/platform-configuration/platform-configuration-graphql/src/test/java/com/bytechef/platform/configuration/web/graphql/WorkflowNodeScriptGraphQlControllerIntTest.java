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

package com.bytechef.platform.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class,
    WorkflowNodeScriptGraphQlController.class
})
@GraphQlTest(
    controllers = WorkflowNodeScriptGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
public class WorkflowNodeScriptGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorkflowNodeScriptFacade workflowNodeScriptFacade;

    @Test
    void testTestClusterElementScriptWithSuccessfulExecution() {
        Map<String, Object> outputMap = Map.of("result", "test output", "count", 42);
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, outputMap);

        when(workflowNodeScriptFacade.testClusterElementScript(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testClusterElementScript(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementType: "PROCESSOR"
                        clusterElementWorkflowNodeName: "script_1"
                        environmentId: 1
                    ) {
                        error {
                            message
                            stackTrace
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testClusterElementScript.error")
            .valueIsNull()
            .path("testClusterElementScript.output")
            .entity(Map.class)
            .satisfies(output -> {
                assertThat(output.get("result")).isEqualTo("test output");
                assertThat(output.get("count")).isEqualTo(42);
            });

        verify(workflowNodeScriptFacade).testClusterElementScript(
            eq("workflow-123"), eq("data-stream_1"), eq("PROCESSOR"), eq("script_1"), eq(1L), isNull(), isNull());
    }

    @Test
    void testTestClusterElementScriptWithError() {
        ExecutionError executionError = new ExecutionError(
            "Script execution failed", List.of("at line 1", "at line 2"));
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(executionError, null);

        when(workflowNodeScriptFacade.testClusterElementScript(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testClusterElementScript(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementType: "PROCESSOR"
                        clusterElementWorkflowNodeName: "script_1"
                        environmentId: 1
                    ) {
                        error {
                            message
                            stackTrace
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testClusterElementScript.error.message")
            .entity(String.class)
            .isEqualTo("Script execution failed")
            .path("testClusterElementScript.error.stackTrace")
            .entityList(String.class)
            .hasSize(2)
            .path("testClusterElementScript.output")
            .valueIsNull();
    }

    @Test
    void testTestClusterElementScriptWithNullOutput() {
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, null);

        when(workflowNodeScriptFacade.testClusterElementScript(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testClusterElementScript(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementType: "SOURCE"
                        clusterElementWorkflowNodeName: "csv-file_1"
                        environmentId: 2
                    ) {
                        error {
                            message
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testClusterElementScript.error")
            .valueIsNull()
            .path("testClusterElementScript.output")
            .valueIsNull();
    }

    @Test
    void testTestWorkflowNodeScriptWithSuccessfulExecution() {
        Map<String, Object> outputMap = Map.of("result", "script output", "value", 100);
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, outputMap);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testWorkflowNodeScript(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 1
                    ) {
                        error {
                            message
                            stackTrace
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testWorkflowNodeScript.error")
            .valueIsNull()
            .path("testWorkflowNodeScript.output")
            .entity(Map.class)
            .satisfies(output -> {
                assertThat(output.get("result")).isEqualTo("script output");
                assertThat(output.get("value")).isEqualTo(100);
            });

        verify(workflowNodeScriptFacade).testWorkflowNodeScript(
            eq("workflow-456"), eq("script_1"), eq(1L), isNull(), isNull());
    }

    @Test
    void testTestWorkflowNodeScriptWithError() {
        ExecutionError executionError = new ExecutionError(
            "Workflow script failed", List.of("at script line 5", "at script line 10"));
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(executionError, null);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testWorkflowNodeScript(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 1
                    ) {
                        error {
                            message
                            stackTrace
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testWorkflowNodeScript.error.message")
            .entity(String.class)
            .isEqualTo("Workflow script failed")
            .path("testWorkflowNodeScript.error.stackTrace")
            .entityList(String.class)
            .hasSize(2)
            .path("testWorkflowNodeScript.output")
            .valueIsNull();
    }

    @Test
    void testTestWorkflowNodeScriptWithNullOutput() {
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, null);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testWorkflowNodeScript(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 2
                    ) {
                        error {
                            message
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testWorkflowNodeScript.error")
            .valueIsNull()
            .path("testWorkflowNodeScript.output")
            .valueIsNull();
    }

    @Test
    void testTestClusterElementScriptWithInputParameters() {
        Map<String, Object> outputMap = Map.of("processed", true);
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, outputMap);

        when(workflowNodeScriptFacade.testClusterElementScript(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testClusterElementScript(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementType: "PROCESSOR"
                        clusterElementWorkflowNodeName: "script_1"
                        environmentId: 1
                        inputParameters: {key: "value", count: 5}
                    ) {
                        error {
                            message
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testClusterElementScript.error")
            .valueIsNull()
            .path("testClusterElementScript.output")
            .entity(Map.class)
            .satisfies(output -> {
                assertThat(output.get("processed")).isEqualTo(true);
            });

        verify(workflowNodeScriptFacade).testClusterElementScript(
            eq("workflow-123"), eq("data-stream_1"), eq("PROCESSOR"), eq("script_1"), eq(1L), isNull(),
            eq(Map.of("key", "value", "count", 5)));
    }

    @Test
    void testTestWorkflowNodeScriptWithInputParameters() {
        Map<String, Object> outputMap = Map.of("transformed", "data");
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, outputMap);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testWorkflowNodeScript(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 1
                        inputParameters: {input: "test data"}
                    ) {
                        error {
                            message
                        }
                        output
                    }
                }
                """)
            .execute()
            .path("testWorkflowNodeScript.error")
            .valueIsNull()
            .path("testWorkflowNodeScript.output")
            .entity(Map.class)
            .satisfies(output -> {
                assertThat(output.get("transformed")).isEqualTo("data");
            });

        verify(workflowNodeScriptFacade).testWorkflowNodeScript(
            eq("workflow-456"), eq("script_1"), eq(1L), isNull(), eq(Map.of("input", "test data")));
    }

    @Test
    void testStopScriptTest() {
        when(workflowNodeScriptFacade.stopScriptTest("execution-1")).thenReturn(true);

        this.graphQlTester
            .document("""
                mutation {
                    stopScriptTest(executionId: "execution-1")
                }
                """)
            .execute()
            .path("stopScriptTest")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workflowNodeScriptFacade).stopScriptTest("execution-1");
    }

    @Test
    void testTestWorkflowNodeScriptPassesTheExecutionId() {
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, Map.of());

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any(), any())).thenReturn(dto);

        this.graphQlTester
            .document("""
                mutation {
                    testWorkflowNodeScript(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 1
                        executionId: "execution-2"
                    ) {
                        output
                    }
                }
                """)
            .execute()
            .path("testWorkflowNodeScript.output")
            .entity(Map.class)
            .isEqualTo(Map.of());

        verify(workflowNodeScriptFacade).testWorkflowNodeScript(
            eq("workflow-456"), eq("script_1"), eq(1L), eq("execution-2"), isNull());
    }

    @Test
    void testClusterElementScriptInput() {
        Map<String, Object> scriptInput = Map.of("field1", "value1", "field2", 42);

        when(workflowNodeScriptFacade.getClusterElementScriptInput(
            anyString(), anyString(), anyString(), anyString(), anyLong())).thenReturn(scriptInput);

        this.graphQlTester
            .document("""
                query {
                    clusterElementScriptInput(
                        workflowId: "workflow-123"
                        workflowNodeName: "data-stream_1"
                        clusterElementType: "PROCESSOR"
                        clusterElementWorkflowNodeName: "script_1"
                        environmentId: 1
                    )
                }
                """)
            .execute()
            .path("clusterElementScriptInput")
            .entity(Map.class)
            .satisfies(result -> {
                assertThat(result.get("field1")).isEqualTo("value1");
                assertThat(result.get("field2")).isEqualTo(42);
            });

        verify(workflowNodeScriptFacade).getClusterElementScriptInput(
            "workflow-123", "data-stream_1", "PROCESSOR", "script_1", 1L);
    }

    @Test
    void testWorkflowNodeScriptInput() {
        Map<String, Object> scriptInput = Map.of("data", "sample input");

        when(workflowNodeScriptFacade.getWorkflowNodeScriptInput(
            anyString(), anyString(), anyLong())).thenReturn(scriptInput);

        this.graphQlTester
            .document("""
                query {
                    workflowNodeScriptInput(
                        workflowId: "workflow-456"
                        workflowNodeName: "script_1"
                        environmentId: 1
                    )
                }
                """)
            .execute()
            .path("workflowNodeScriptInput")
            .entity(Map.class)
            .satisfies(result -> {
                assertThat(result.get("data")).isEqualTo("sample input");
            });

        verify(workflowNodeScriptFacade).getWorkflowNodeScriptInput(
            "workflow-456", "script_1", 1L);
    }
}
