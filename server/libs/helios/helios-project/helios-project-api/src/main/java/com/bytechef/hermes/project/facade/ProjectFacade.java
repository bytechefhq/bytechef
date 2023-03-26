
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

package com.bytechef.hermes.project.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.domain.ProjectInstance;
import com.bytechef.hermes.project.dto.ProjectExecutionDTO;
import com.bytechef.tag.domain.Tag;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    Project addWorkflow(long id, String name, String description, String definition);

    Project createProject(Project project);

    ProjectInstance createProjectInstance(ProjectInstance projectInstance);

    void deleteProject(Long projectId);

    void deleteProjectInstance(Long projectInstanceId);

    Project duplicateProject(long projectId);

    Project getProject(Long projectId);

    ProjectInstance getProjectInstance(Long projectInstanceId);

    List<Category> getProjectCategories();

    List<Tag> getProjectInstanceTags();

    List<Tag> getProjectTags();

    List<Workflow> getProjectWorkflows(Long id);

    Page<ProjectExecutionDTO> searchProjectExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber);

    List<ProjectInstance> searchProjectInstances(List<Long> projectIds, List<Long> tagIds);

    List<Project> searchProjects(List<Long> categoryIds, boolean projectInstances, List<Long> tagIds);

    Project update(Project project);

    ProjectInstance update(ProjectInstance projectInstance);

    ProjectInstance updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags);

    Project updateProjectTags(Long projectId, List<Tag> tags);

}
