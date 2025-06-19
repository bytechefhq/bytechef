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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentService {

    ProjectDeployment create(ProjectDeployment projectDeployment);

    void delete(long id);

    Optional<ProjectDeployment> fetchProjectDeployment(long projectId, Environment environment);

    ProjectDeployment getProjectDeployment(long id);

    ProjectDeployment getProjectDeployment(long projectId, Environment environment);

    long getProjectDeploymentId(long projectId, Environment environment);

    List<ProjectDeployment> getProjectDeployments();

    List<ProjectDeployment> getProjectDeployments(long projectId);

    List<ProjectDeployment> getProjectDeployments(
        Boolean embedded, Environment environment, Long projectId, Long tagId, Long workspaceId);

    boolean isProjectDeploymentEnabled(long projectDeploymentId);

    ProjectDeployment update(long id, List<Long> tagIds);

    ProjectDeployment update(ProjectDeployment projectDeployment);

    void updateEnabled(long id, boolean enabled);
}
