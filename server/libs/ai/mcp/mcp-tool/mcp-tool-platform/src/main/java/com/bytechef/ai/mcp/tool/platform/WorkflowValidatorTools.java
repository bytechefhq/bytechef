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

package com.bytechef.ai.mcp.tool.platform;

import com.bytechef.ai.mcp.tool.platform.exception.WorkflowValidatorToolErrorType;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Platform-level workflow validation tool.
 *
 * @author Marko Kriskovic
 */
@Component
public class WorkflowValidatorTools {

    private static final Logger log = LoggerFactory.getLogger(WorkflowValidatorTools.class);

    private final WorkflowValidatorFacade workflowValidatorFacade;

    @SuppressFBWarnings("EI")
    public WorkflowValidatorTools(WorkflowValidatorFacade workflowValidatorFacade) {
        this.workflowValidatorFacade = workflowValidatorFacade;
    }

    @Tool(
        description = "Validate a workflow configuration by checking its structure, properties and outputs against the task definitions. Returns validation results with any errors found")
    public WorkflowValidationResult validateWorkflow(
        @ToolParam(description = "The JSON string of the workflow to validate") String workflow) {

        try {
            WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
                workflowValidatorFacade.validateWorkflow(workflow);

            List<String> errors = workflowValidationResult.errors();

            String errorMessages = errors.toString();

            List<String> warnings = workflowValidationResult.warnings();

            String warningMessages = warnings.toString();

            boolean isValid = errorMessages.equals("[]");

            if (log.isDebugEnabled()) {
                log.debug(
                    "validateWorkflow(): Validated workflow. Valid: {}, Errors: {}, Warnings: {}", isValid,
                    errorMessages, warningMessages);
            }

            return new WorkflowValidationResult(isValid, errorMessages, warningMessages);
        } catch (Exception e) {
            log.error("validateWorkflow(): Failed to validate workflow", e);

            throw new ExecutionException(
                "Failed to validate workflow", e, WorkflowValidatorToolErrorType.VALIDATE_WORKFLOW);
        }
    }

    /**
     * Workflow validation result record for API responses.
     *
     * @author Marko Kriskovic
     */
    @SuppressFBWarnings("EI")
    public record WorkflowValidationResult(
        @JsonProperty("valid") @JsonPropertyDescription("Whether the workflow is valid") boolean valid,
        @JsonProperty("errors") @JsonPropertyDescription("Error details, which need to be fixed before the workflow can be valid") String errors,
        @JsonProperty("warnings") @JsonPropertyDescription("Warning details that give additional information") String warnings) {
    }
}
