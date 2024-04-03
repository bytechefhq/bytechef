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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectService {

    void addVersion(long id, List<String> duplicatedVersionWorkflowIds);

    Project addWorkflow(long id, String workflowId);

    long countProjects();

    Project create(Project project);

    void delete(long id);

    Optional<Project> fetchProject(String name);

    Project getProjectInstanceProject(long projectInstanceId);

    Project getProject(long id);

    List<Project> getProjects();

    List<ProjectVersion> getProjectVersions(Long id);

    List<Project> getProjects(List<Long> ids);

    List<Project> getProjects(Long categoryId, List<Long> ids, Long tagId, Status status);

    Project getWorkflowProject(String workflowId);

    void publishProject(long id, String description);

    void removeWorkflow(long id, String workflowId);

    Project update(long id, List<Long> tagIds);

    Project update(Project project);
}
