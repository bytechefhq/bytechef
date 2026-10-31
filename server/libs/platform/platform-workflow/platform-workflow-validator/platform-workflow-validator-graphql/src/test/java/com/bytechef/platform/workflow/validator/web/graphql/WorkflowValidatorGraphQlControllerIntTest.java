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

package com.bytechef.platform.workflow.validator.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.platform.workflow.validator.web.graphql.config.WorkflowValidatorGraphQlTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    WorkflowValidatorGraphQlTestConfiguration.class, WorkflowValidatorGraphQlController.class
})
@GraphQlTest(
    controllers = WorkflowValidatorGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class WorkflowValidatorGraphQlControllerIntTest {

    private static final String VALIDATE_WORKFLOW_QUERY = """
        query validateWorkflow($workflow: String!, $workflowId: String, $environmentId: Long) {
            validateWorkflow(workflow: $workflow, workflowId: $workflowId, environmentId: $environmentId) {
                errors
                warnings
                nodeIssues {
                    nodeName
                    propertyPath
                    kind
                    severity
                    message
                }
            }
        }
        """;

    private static final String VALIDATE_WORKFLOW_BY_ID_QUERY = """
        query validateWorkflowById($workflowId: String!, $environmentId: Long) {
            validateWorkflowById(workflowId: $workflowId, environmentId: $environmentId) {
                errors
                warnings
            }
        }
        """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private WorkflowValidatorFacade workflowValidatorFacade;

    @Test
    void testPassesEnvironmentThroughWhenGiven() {
        when(workflowValidatorFacade.validateWorkflow("{}", 2L))
            .thenReturn(new WorkflowValidationResult(List.of("environment error"), List.of()));

        graphQlTester.document(VALIDATE_WORKFLOW_QUERY)
            .variable("workflow", "{}")
            .variable("environmentId", 2)
            .execute()
            .path("validateWorkflow.errors")
            .entityList(String.class)
            .containsExactly("environment error");

        verify(workflowValidatorFacade).validateWorkflow("{}", 2L);
        verifyNoMoreInteractions(workflowValidatorFacade);
    }

    @Test
    void testUsesFacadeDefaultWhenEnvironmentIsAbsent() {
        when(workflowValidatorFacade.validateWorkflow("{}"))
            .thenReturn(new WorkflowValidationResult(List.of(), List.of("default warning")));

        graphQlTester.document(VALIDATE_WORKFLOW_QUERY)
            .variable("workflow", "{}")
            .execute()
            .path("validateWorkflow.warnings")
            .entityList(String.class)
            .containsExactly("default warning");

        verify(workflowValidatorFacade).validateWorkflow("{}");
        verifyNoMoreInteractions(workflowValidatorFacade);
    }

    @Test
    void testPassesTheWorkflowIdThroughWhenGiven() {
        NodeValidationIssue nodeValidationIssue = new NodeValidationIssue(
            "httpClient_1", "uri", WorkflowIssueKind.MISSING_REQUIRED, WorkflowIssueSeverity.ERROR,
            "Property uri is required");

        when(workflowValidatorFacade.validateWorkflow("{}", "wf-1", 2L))
            .thenReturn(new WorkflowValidationResult(List.of(), List.of(), List.of(nodeValidationIssue)));

        graphQlTester.document(VALIDATE_WORKFLOW_QUERY)
            .variable("workflow", "{}")
            .variable("workflowId", "wf-1")
            .variable("environmentId", 2)
            .execute()
            .path("validateWorkflow.nodeIssues[0].nodeName")
            .entity(String.class)
            .isEqualTo("httpClient_1")
            .path("validateWorkflow.nodeIssues[0].propertyPath")
            .entity(String.class)
            .isEqualTo("uri")
            .path("validateWorkflow.nodeIssues[0].kind")
            .entity(String.class)
            .isEqualTo("MISSING_REQUIRED")
            .path("validateWorkflow.nodeIssues[0].severity")
            .entity(String.class)
            .isEqualTo("ERROR");

        verify(workflowValidatorFacade).validateWorkflow("{}", "wf-1", 2L);
        verifyNoMoreInteractions(workflowValidatorFacade);
    }

    @Test
    void testValidatesInDevelopmentWhenOnlyTheWorkflowIdIsGiven() {
        when(workflowValidatorFacade.validateWorkflow("{}", "wf-1", 0L))
            .thenReturn(new WorkflowValidationResult(List.of("development error"), List.of()));

        graphQlTester.document(VALIDATE_WORKFLOW_QUERY)
            .variable("workflow", "{}")
            .variable("workflowId", "wf-1")
            .execute()
            .path("validateWorkflow.errors")
            .entityList(String.class)
            .containsExactly("development error");

        verify(workflowValidatorFacade).validateWorkflow("{}", "wf-1", 0L);
        verifyNoMoreInteractions(workflowValidatorFacade);
    }

    @Test
    void testValidateByIdPassesEnvironmentThrough() {
        when(workflowValidatorFacade.validateWorkflowById("wf-1", 1L))
            .thenReturn(new WorkflowValidationResult(List.of("by id error"), List.of()));

        graphQlTester.document(VALIDATE_WORKFLOW_BY_ID_QUERY)
            .variable("workflowId", "wf-1")
            .variable("environmentId", 1)
            .execute()
            .path("validateWorkflowById.errors")
            .entityList(String.class)
            .containsExactly("by id error");

        verify(workflowValidatorFacade).validateWorkflowById("wf-1", 1L);
        verifyNoMoreInteractions(workflowValidatorFacade);
    }

    @Test
    void testValidateByIdUsesFacadeDefaultWhenEnvironmentIsAbsent() {
        when(workflowValidatorFacade.validateWorkflowById("wf-1"))
            .thenReturn(new WorkflowValidationResult(List.of(), List.of("by id warning")));

        graphQlTester.document(VALIDATE_WORKFLOW_BY_ID_QUERY)
            .variable("workflowId", "wf-1")
            .execute()
            .path("validateWorkflowById.warnings")
            .entityList(String.class)
            .containsExactly("by id warning");

        verify(workflowValidatorFacade).validateWorkflowById("wf-1");
        verifyNoMoreInteractions(workflowValidatorFacade);
    }
}
