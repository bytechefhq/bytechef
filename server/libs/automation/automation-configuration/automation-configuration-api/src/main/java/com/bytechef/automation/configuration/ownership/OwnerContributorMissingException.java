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

package com.bytechef.automation.configuration.ownership;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class OwnerContributorMissingException extends IllegalStateException {

    private final List<ProjectDeploymentOwner> projectDeploymentOwners;
    private final List<ProjectOwner> projectOwners;

    public OwnerContributorMissingException(
        String message, List<ProjectOwner> projectOwners, List<ProjectDeploymentOwner> projectDeploymentOwners) {

        super(message);

        this.projectDeploymentOwners = List.copyOf(projectDeploymentOwners);
        this.projectOwners = List.copyOf(projectOwners);
    }

    public List<ProjectDeploymentOwner> getProjectDeploymentOwners() {
        return projectDeploymentOwners;
    }

    public List<ProjectOwner> getProjectOwners() {
        return projectOwners;
    }
}
