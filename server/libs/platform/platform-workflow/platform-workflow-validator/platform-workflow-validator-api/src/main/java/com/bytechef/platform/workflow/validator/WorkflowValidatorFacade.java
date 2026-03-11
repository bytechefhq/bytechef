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

package com.bytechef.platform.workflow.validator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Facade for workflow validation operations.
 *
 * @author Marko Kriskovic
 */
public interface WorkflowValidatorFacade {

    /**
     * Validates a complete workflow JSON string.
     *
     * @param workflow the workflow JSON string to validate
     * @return a {@link WorkflowValidationResult} containing lists of errors and warnings
     */
    WorkflowValidationResult validateWorkflow(String workflow);

    /**
     * Holds the result of a workflow validation, containing lists of error messages and warning messages.
     */
    @SuppressFBWarnings("EI")
    record WorkflowValidationResult(List<String> errors, List<String> warnings) {

        public WorkflowValidationResult(List<String> errors, List<String> warnings) {
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }
    }
}
