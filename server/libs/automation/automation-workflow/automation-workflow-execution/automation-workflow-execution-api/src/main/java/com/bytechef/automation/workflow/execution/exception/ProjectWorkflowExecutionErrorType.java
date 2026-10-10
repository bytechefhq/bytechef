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

package com.bytechef.automation.workflow.execution.exception;

import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowExecutionErrorType extends AbstractErrorType {

    public static final ProjectWorkflowExecutionErrorType GET_WORKFLOW_EXECUTION =
        new ProjectWorkflowExecutionErrorType(100);
    public static final ProjectWorkflowExecutionErrorType LIST_WORKFLOW_EXECUTIONS =
        new ProjectWorkflowExecutionErrorType(101);

    private ProjectWorkflowExecutionErrorType(int errorKey) {
        super(WorkflowExecutionDTO.class, errorKey);
    }
}
