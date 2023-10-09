
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.service;

import com.bytechef.helios.configuration.domain.Project;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectService {

    Project addWorkflow(long id, String workflowId);

    long countProjects();

    Project create(Project project);

    void delete(long id);

    Optional<Project> fetchProject(String name);

    Project getProject(long id);

    Project getProject(String name);

    List<Project> getProjects();

    List<Project> getProjects(List<Long> ids);

    List<Project> getProjects(Long categoryId, List<Long> ids, Long tagId, Boolean published);

    Project getWorkflowProject(String workflowId);

    void removeWorkflow(long id, String workflowId);

    Project update(long id, List<Long> tagIds);

    Project update(Project project);
}
