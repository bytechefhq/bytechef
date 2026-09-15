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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockReset;

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
            anyString(), anyString(), anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
                assert output.get("result")
                    .equals("test output");
                assert output.get("count")
                    .equals(42);
            });

        verify(workflowNodeScriptFacade).testClusterElementScript(
            eq("workflow-123"), eq("data-stream_1"), eq("PROCESSOR"), eq("script_1"), eq(1L), isNull());
    }

    @Test
    void testTestClusterElementScriptWithError() {
        ExecutionError executionError = new ExecutionError(
            "Script execution failed", List.of("at line 1", "at line 2"));
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(executionError, null);

        when(workflowNodeScriptFacade.testClusterElementScript(
            anyString(), anyString(), anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
            anyString(), anyString(), anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
            anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
                assert output.get("result")
                    .equals("script output");
                assert output.get("value")
                    .equals(100);
            });

        verify(workflowNodeScriptFacade).testWorkflowNodeScript(
            eq("workflow-456"), eq("script_1"), eq(1L), isNull());
    }

    @Test
    void testTestWorkflowNodeScriptWithError() {
        ExecutionError executionError = new ExecutionError(
            "Workflow script failed", List.of("at script line 5", "at script line 10"));
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(executionError, null);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
            anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
            anyString(), anyString(), anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
                assert output.get("processed")
                    .equals(true);
            });

        verify(workflowNodeScriptFacade).testClusterElementScript(
            eq("workflow-123"), eq("data-stream_1"), eq("PROCESSOR"), eq("script_1"), eq(1L),
            eq(Map.of("key", "value", "count", 5)));
    }

    @Test
    void testTestWorkflowNodeScriptWithInputParameters() {
        Map<String, Object> outputMap = Map.of("transformed", "data");
        ScriptTestExecutionDTO dto = new ScriptTestExecutionDTO(null, outputMap);

        when(workflowNodeScriptFacade.testWorkflowNodeScript(
            anyString(), anyString(), anyLong(), any())).thenReturn(dto);

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
                assert output.get("transformed")
                    .equals("data");
            });

        verify(workflowNodeScriptFacade).testWorkflowNodeScript(
            eq("workflow-456"), eq("script_1"), eq(1L), eq(Map.of("input", "test data")));
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
                assert result.get("field1")
                    .equals("value1");
                assert result.get("field2")
                    .equals(42);
            });

        verify(workflowNodeScriptFacade).getClusterElementScriptInput(
            eq("workflow-123"), eq("data-stream_1"), eq("PROCESSOR"), eq("script_1"), eq(1L));
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
                assert result.get("data")
                    .equals("sample input");
            });

        verify(workflowNodeScriptFacade).getWorkflowNodeScriptInput(
            eq("workflow-456"), eq("script_1"), eq(1L));
    }

    @Nested
    @Import(MethodSecurityEnforcement.MethodSecurityConfiguration.class)
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
        private static final String WORKFLOW_ID = "workflow-1";

        @Autowired
        private PermissionService permissionService;

        @Autowired
        private WorkflowNodeScriptGraphQlController workflowNodeScriptGraphQlController;

        @BeforeEach
        void beforeEach() {
            IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

            doThrow(bodyReachedException).when(workflowNodeScriptFacade)
                .getClusterElementScriptInput(anyString(), anyString(), anyString(), anyString(), anyLong());
            doThrow(bodyReachedException).when(workflowNodeScriptFacade)
                .getWorkflowNodeScriptInput(anyString(), anyString(), anyLong());
            doThrow(bodyReachedException).when(workflowNodeScriptFacade)
                .testClusterElementScript(anyString(), anyString(), anyString(), anyString(), anyLong(), any());
            doThrow(bodyReachedException).when(workflowNodeScriptFacade)
                .testWorkflowNodeScript(anyString(), anyString(), anyLong(), any());

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @ParameterizedTest
        @MethodSource("guardedOperations")
        void testGuardDeniesWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsRefused(
            GuardedOperation guardedOperation) {

            assertThatThrownBy(() -> guardedOperation.invoke(workflowNodeScriptGraphQlController))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasWorkflowScopeIfProjectWorkflow(
                WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
        }

        @ParameterizedTest
        @MethodSource("guardedOperations")
        void testGuardAllowsWhenTheWorkflowEditScopeInTheRequestedEnvironmentIsGranted(
            GuardedOperation guardedOperation) {

            when(permissionService.hasWorkflowScopeIfProjectWorkflow(
                WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION)).thenReturn(true);

            assertBodyReached(() -> guardedOperation.invoke(workflowNodeScriptGraphQlController));
        }

        @Test
        void testScriptInputReadsAreNotGuarded() {
            assertBodyReached(
                () -> workflowNodeScriptGraphQlController.clusterElementScriptInput(
                    WORKFLOW_ID, "node_1", "tools", "tool_1", PRODUCTION_ENVIRONMENT_ID));
            assertBodyReached(
                () -> workflowNodeScriptGraphQlController.workflowNodeScriptInput(
                    WORKFLOW_ID, "node_1", PRODUCTION_ENVIRONMENT_ID));
        }

        @Test
        void testEveryGuardedMethodHasAnEvaluatedCase() {
            Set<String> guardedMethodNames =
                Arrays.stream(WorkflowNodeScriptGraphQlController.class.getDeclaredMethods())
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
                    "testClusterElementScript",
                    (GuardedOperation) guardedController -> guardedController.testClusterElementScript(
                        WORKFLOW_ID, "node_1", "tools", "tool_1", PRODUCTION_ENVIRONMENT_ID, Map.of())),
                Named.of(
                    "testWorkflowNodeScript",
                    (GuardedOperation) guardedController -> guardedController.testWorkflowNodeScript(
                        WORKFLOW_ID, "node_1", PRODUCTION_ENVIRONMENT_ID, Map.of())));
        }

        private static void assertBodyReached(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        @FunctionalInterface
        interface GuardedOperation {

            void invoke(WorkflowNodeScriptGraphQlController guardedController);
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
}
