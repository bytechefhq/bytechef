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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class ProjectDeploymentErrorType extends AbstractErrorType {

    public static final ProjectDeploymentErrorType PROJECT_NOT_PUBLISHED = new ProjectDeploymentErrorType(100);
    public static final ProjectDeploymentErrorType WORKFLOW_CONNECTIONS_NOT_FOUND = new ProjectDeploymentErrorType(101);
    public static final ProjectDeploymentErrorType INVALID_PROJECT_VERSION = new ProjectDeploymentErrorType(102);

    private ProjectDeploymentErrorType(int errorKey) {
        super(ProjectDeployment.class, errorKey);
    }
}
