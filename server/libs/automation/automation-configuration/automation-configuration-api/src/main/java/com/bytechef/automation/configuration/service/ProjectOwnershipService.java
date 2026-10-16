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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import java.util.Collection;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public interface ProjectOwnershipService {

    String getExcludeProjectsCondition(Set<ProjectOwner> projectOwners, String projectIdColumn);

    String getSelectProjectDeploymentsCondition(
        ProjectDeploymentOwner projectDeploymentOwner, String projectDeploymentIdColumn);

    String getExcludeProjectDeploymentsCondition(
        Set<ProjectDeploymentOwner> projectDeploymentOwners, String projectDeploymentIdColumn);

    Set<Long> getOwnedProjectIds(Collection<Long> projectIds, ProjectOwner projectOwner);
}
