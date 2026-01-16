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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectService {

    long countProjects();

    Project create(Project project);

    void delete(long id);

    Optional<Project> fetchProject(String name);

    Project getProjectDeploymentProject(long projectDeploymentId);

    Project getProject(long id);

    Project getProject(UUID uuid);

    List<Project> getProjects();

    List<ProjectVersion> getProjectVersions(Long id);

    List<Project> getProjects(List<Long> ids);

    List<Project> getProjects(
        @Nullable Boolean apiCollections, @Nullable Long categoryId, Boolean projectDeployments,
        @Nullable Long tagId, @Nullable Status status, @Nullable Long workspaceId);

    Project getWorkflowProject(String workflowId);

    int publishProject(long id, @Nullable String description, boolean syncWithGit);

    Project update(long id, List<Long> tagIds);

    Project update(Project project);
}
