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

package com.bytechef.automation.configuration.exception;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowErrorType extends AbstractErrorType {

    public static final ProjectWorkflowErrorType CREATE_WORKFLOW = new ProjectWorkflowErrorType(100);
    public static final ProjectWorkflowErrorType DELETE_WORKFLOW = new ProjectWorkflowErrorType(101);
    public static final ProjectWorkflowErrorType GET_WORKFLOW = new ProjectWorkflowErrorType(102);
    public static final ProjectWorkflowErrorType LIST_WORKFLOWS = new ProjectWorkflowErrorType(103);
    public static final ProjectWorkflowErrorType SEARCH_WORKFLOWS = new ProjectWorkflowErrorType(104);
    public static final ProjectWorkflowErrorType UPDATE_WORKFLOW = new ProjectWorkflowErrorType(105);
    public static final ProjectWorkflowErrorType VALIDATE_WORKFLOW = new ProjectWorkflowErrorType(106);
    public static final ProjectWorkflowErrorType SAVE_TEST_CONNECTION = new ProjectWorkflowErrorType(107);
    public static final ProjectWorkflowErrorType UPDATE_SCRIPT = new ProjectWorkflowErrorType(108);
    public static final ProjectWorkflowErrorType UPDATE_WORKFLOW_ROOT = new ProjectWorkflowErrorType(109);

    private ProjectWorkflowErrorType(int errorKey) {
        super(ProjectWorkflow.class, errorKey);
    }
}
