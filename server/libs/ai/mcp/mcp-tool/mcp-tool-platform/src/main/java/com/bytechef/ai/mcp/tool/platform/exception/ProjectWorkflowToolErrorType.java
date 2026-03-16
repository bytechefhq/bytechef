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

package com.bytechef.ai.mcp.tool.platform.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowToolErrorType extends AbstractErrorType {

    public static final ProjectWorkflowToolErrorType CREATE_WORKFLOW = new ProjectWorkflowToolErrorType(100);
    public static final ProjectWorkflowToolErrorType DELETE_WORKFLOW = new ProjectWorkflowToolErrorType(101);
    public static final ProjectWorkflowToolErrorType GET_WORKFLOW = new ProjectWorkflowToolErrorType(102);
    public static final ProjectWorkflowToolErrorType LIST_WORKFLOWS = new ProjectWorkflowToolErrorType(103);
    public static final ProjectWorkflowToolErrorType SEARCH_WORKFLOWS = new ProjectWorkflowToolErrorType(104);
    public static final ProjectWorkflowToolErrorType UPDATE_WORKFLOW = new ProjectWorkflowToolErrorType(105);
    public static final ProjectWorkflowToolErrorType VALIDATE_WORKFLOW = new ProjectWorkflowToolErrorType(106);

    private ProjectWorkflowToolErrorType(int errorKey) {
        super(ProjectWorkflowToolErrorType.class, errorKey);
    }
}
