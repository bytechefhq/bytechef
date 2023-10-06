
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

package com.bytechef.helios.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.helios.configuration.dto.ProjectDTO;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    Workflow addProjectWorkflow(long id, String label, String description, String definition);

    ProjectDTO createProject(ProjectDTO projectDTO);

    void deleteProject(long id);

    void deleteWorkflow(long id, String workflowId);

    ProjectDTO duplicateProject(long id);

    Workflow duplicateWorkflow(long id, String workflowId);

    ProjectDTO getProject(long id);

    List<Category> getProjectCategories();

    List<Tag> getProjectTags();

    List<Workflow> getProjectWorkflows(long id);

    List<ProjectDTO> getPublishedProjects(Long categoryId, boolean projectInstances, Long tagId);

    ProjectDTO updateProject(ProjectDTO projectDTO);

    void updateProjectTags(long id, List<Tag> tags);
}
