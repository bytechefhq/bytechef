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

package com.bytechef.automation.configuration.tool;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class PersistedWorkflowChecker {

    private final WorkflowValidatorFacade workflowValidatorFacade;

    @SuppressFBWarnings("EI")
    public PersistedWorkflowChecker(WorkflowValidatorFacade workflowValidatorFacade) {
        this.workflowValidatorFacade = workflowValidatorFacade;
    }

    public List<Issue> check(String workflowId) {
        WorkflowValidationResult workflowValidationResult = validate(workflowId);

        return collectIssues(workflowValidationResult, WorkflowIssueSeverity.ERROR, workflowValidationResult.errors());
    }

    public List<Issue> checkWarnings(String workflowId) {
        WorkflowValidationResult workflowValidationResult = validate(workflowId);

        return collectIssues(
            workflowValidationResult, WorkflowIssueSeverity.WARNING, workflowValidationResult.warnings());
    }

    private static List<Issue> collectIssues(
        WorkflowValidationResult workflowValidationResult, WorkflowIssueSeverity severity, List<String> messages) {

        List<Issue> issues = new ArrayList<>();

        for (NodeValidationIssue nodeValidationIssue : workflowValidationResult.nodeIssues()) {
            if (nodeValidationIssue.severity() == severity) {
                issues.add(
                    new Issue(
                        nodeValidationIssue.nodeName(), nodeValidationIssue.message(),
                        nodeValidationIssue.userAction()));
            }
        }

        for (String message : messages) {
            boolean coveredByNodeIssue = issues.stream()
                .anyMatch(issue -> message.contains(issue.message()));

            if (!coveredByNodeIssue) {
                issues.add(new Issue(null, message, false));
            }
        }

        return issues;
    }

    private WorkflowValidationResult validate(String workflowId) {
        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        long environmentId = environment == null ? Environment.DEVELOPMENT.ordinal() : environment.ordinal();

        return workflowValidatorFacade.validateWorkflowById(workflowId, environmentId);
    }

    public record Issue(@Nullable String nodeName, String message, boolean userAction) {
    }
}
