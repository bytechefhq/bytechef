
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

import com.bytechef.category.domain.Category;
import com.bytechef.helios.project.dto.ProjectDTO;
import com.bytechef.hermes.workflow.dto.WorkflowDTO;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    WorkflowDTO addProjectWorkflow(long id, String label, String description, String definition);

    ProjectDTO createProject(ProjectDTO projectDTO);

    void deleteProject(long id);

    ProjectDTO duplicateProject(long id);

    ProjectDTO getProject(long id);

    List<Category> getProjectCategories();

    List<Tag> getProjectTags();

    List<WorkflowDTO> getProjectWorkflows(long id);

    List<ProjectDTO> searchProjects(List<Long> categoryIds, boolean projectInstances, List<Long> tagIds);

    ProjectDTO updateProject(ProjectDTO projectDTO);

    void updateProjectTags(long id, List<Tag> tags);

}
