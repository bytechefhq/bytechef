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

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Checks a saved workflow with the validation the workflow editor's issue panel shows, in the environment of the
 * current agent run.
 *
 * @author Ivica Cardic
 */
@Component
class EditorPersistedWorkflowChecker implements PersistedWorkflowChecker {

    private final WorkflowValidatorFacade workflowValidatorFacade;

    @SuppressFBWarnings("EI")
    EditorPersistedWorkflowChecker(WorkflowValidatorFacade workflowValidatorFacade) {
        this.workflowValidatorFacade = workflowValidatorFacade;
    }

    @Override
    public List<Issue> check(String workflowId) {
        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        long environmentId = environment == null ? Environment.DEVELOPMENT.ordinal() : environment.ordinal();

        WorkflowValidationResult workflowValidationResult = workflowValidatorFacade.validateWorkflowById(
            workflowId, environmentId);

        List<Issue> issues = new ArrayList<>();

        for (NodeValidationIssue nodeValidationIssue : workflowValidationResult.nodeIssues()) {
            if (nodeValidationIssue.severity() == WorkflowIssueSeverity.ERROR) {
                issues.add(
                    new Issue(
                        nodeValidationIssue.nodeName(), nodeValidationIssue.message(),
                        nodeValidationIssue.kind() == WorkflowIssueKind.MISSING_CONNECTION));
            }
        }

        for (String error : workflowValidationResult.errors()) {
            boolean coveredByNodeIssue = issues.stream()
                .anyMatch(issue -> error.contains(issue.message()));

            if (!coveredByNodeIssue) {
                issues.add(new Issue(null, error, false));
            }
        }

        return issues;
    }
}
