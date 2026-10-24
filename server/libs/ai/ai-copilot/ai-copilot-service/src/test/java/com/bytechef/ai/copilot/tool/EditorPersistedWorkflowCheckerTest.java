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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EditorPersistedWorkflowCheckerTest {

    private final WorkflowValidatorFacade workflowValidatorFacade = mock(WorkflowValidatorFacade.class);

    @AfterEach
    void afterEach() {
        EnvironmentContext.clear();
    }

    @Test
    void testReportsErrorsOfTheRunEnvironmentAndMarksMissingConnectionsForTheUser() {
        EnvironmentContext.set(Environment.STAGING);

        when(workflowValidatorFacade.validateWorkflowById("wf-1", Environment.STAGING.ordinal())).thenReturn(
            new WorkflowValidationResult(
                List.of("Workflow has no trigger", "Property 'getEmail_1.subject' does not exist"), List.of(),
                List.of(
                    new NodeValidationIssue(
                        "createRecords_1", "records", WorkflowIssueKind.BROKEN_REFERENCE,
                        WorkflowIssueSeverity.ERROR, "Property 'getEmail_1.subject' does not exist"),
                    new NodeValidationIssue(
                        "searchEmail_1", null, WorkflowIssueKind.MISSING_CONNECTION, WorkflowIssueSeverity.ERROR,
                        "Missing required connection: Gmail"),
                    new NodeValidationIssue(
                        "loop_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.WARNING, "Just a warning"))));

        List<PersistedWorkflowChecker.Issue> issues = new EditorPersistedWorkflowChecker(workflowValidatorFacade)
            .check("wf-1");

        assertThat(issues).containsExactly(
            new PersistedWorkflowChecker.Issue(
                "createRecords_1", "Property 'getEmail_1.subject' does not exist", false),
            new PersistedWorkflowChecker.Issue("searchEmail_1", "Missing required connection: Gmail", true),
            new PersistedWorkflowChecker.Issue(null, "Workflow has no trigger", false));
    }

    @Test
    void testValidatesAgainstDevelopmentWithoutARunEnvironment() {
        when(workflowValidatorFacade.validateWorkflowById("wf-1", Environment.DEVELOPMENT.ordinal()))
            .thenReturn(new WorkflowValidationResult(List.of(), List.of()));

        assertThat(new EditorPersistedWorkflowChecker(workflowValidatorFacade).check("wf-1")).isEmpty();
    }
}
