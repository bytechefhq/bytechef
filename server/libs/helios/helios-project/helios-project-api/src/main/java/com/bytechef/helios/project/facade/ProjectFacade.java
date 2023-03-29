
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

package com.bytechef.helios.project.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.helios.project.dto.ProjectExecutionDTO;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.dto.ProjectDTO;
import com.bytechef.tag.domain.Tag;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    Workflow addWorkflow(long id, String label, String description, String definition);

    ProjectDTO createProject(ProjectDTO projectDTO);

    ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstance);

    long createProjectInstanceJob(long id, String workflowId);

    void deleteProject(Long id);

    void deleteProjectInstance(Long projectInstanceId);

    ProjectDTO duplicateProject(long id);

    ProjectDTO getProject(Long id);

    ProjectInstanceDTO getProjectInstance(Long projectInstanceId);

    List<Category> getProjectCategories();

    List<Tag> getProjectInstanceTags();

    List<Tag> getProjectTags();

    List<Workflow> getProjectWorkflows(Long id);

    ProjectExecutionDTO getProjectExecution(long id);

    Page<ProjectExecutionDTO> searchProjectExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber);

    List<ProjectInstanceDTO> searchProjectInstances(List<Long> projectIds, List<Long> tagIds);

    List<ProjectDTO> searchProjects(List<Long> categoryIds, boolean projectInstances, List<Long> tagIds);

    ProjectDTO update(ProjectDTO project);

    ProjectInstanceDTO update(ProjectInstanceDTO projectInstance);

    ProjectInstanceDTO updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags);

    ProjectDTO updateProjectTags(Long id, List<Tag> tags);

}
