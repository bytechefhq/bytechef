/*
 * Copyright 2023-present ByteChef Inc.
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

    public static final ProjectDeploymentErrorType CREATE_PROJECT_DEPLOYMENT = new ProjectDeploymentErrorType(100);
    public static final ProjectDeploymentErrorType REQUIRED_WORKFLOW_CONNECTIONS = new ProjectDeploymentErrorType(101);

    private ProjectDeploymentErrorType(int errorKey) {
        super(ProjectDeployment.class, errorKey);
    }
}
