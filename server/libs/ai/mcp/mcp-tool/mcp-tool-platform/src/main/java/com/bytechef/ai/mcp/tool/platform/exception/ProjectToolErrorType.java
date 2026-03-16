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
public class ProjectToolErrorType extends AbstractErrorType {

    public static final ProjectToolErrorType CREATE_PROJECT = new ProjectToolErrorType(100);
    public static final ProjectToolErrorType DELETE_PROJECT = new ProjectToolErrorType(101);
    public static final ProjectToolErrorType GET_PROJECT = new ProjectToolErrorType(102);
    public static final ProjectToolErrorType GET_PROJECT_STATUS = new ProjectToolErrorType(103);
    public static final ProjectToolErrorType LIST_PROJECTS = new ProjectToolErrorType(104);
    public static final ProjectToolErrorType PUBLISH_PROJECT = new ProjectToolErrorType(105);
    public static final ProjectToolErrorType SEARCH_PROJECTS = new ProjectToolErrorType(106);
    public static final ProjectToolErrorType UPDATE_PROJECT = new ProjectToolErrorType(107);

    private ProjectToolErrorType(int errorKey) {
        super(ProjectToolErrorType.class, errorKey);
    }
}
