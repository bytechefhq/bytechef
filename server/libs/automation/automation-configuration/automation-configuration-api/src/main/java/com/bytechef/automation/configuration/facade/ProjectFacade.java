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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    ProjectWorkflow addWorkflow(long id, String definition);

    long createProject(ProjectDTO projectDTO);

    void deleteProject(long id);

    void deleteWorkflow(String workflowId);

    ProjectDTO duplicateProject(long id);

    String duplicateWorkflow(long id, String workflowId);

    ProjectDTO getProject(long id);

    List<Category> getProjectCategories();

    List<Tag> getProjectTags();

    ProjectWorkflowDTO getProjectWorkflow(String workflowId);

    ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId);

    List<ProjectWorkflowDTO> getProjectWorkflows();

    List<ProjectWorkflowDTO> getProjectWorkflows(long id);

    List<ProjectWorkflowDTO> getProjectVersionWorkflows(long id, int projectVersion, boolean includeAllFields);

    List<ProjectDTO> getProjects(
        @Nullable Long categoryId, @Nullable Boolean projectDeployments, @Nullable Long tagId, @Nullable Status status);

    List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, @Nullable Long categoryId, boolean includeAllFields, Boolean projectDeployments,
        @Nullable Status status, @Nullable Long tagId, long workspaceId);

    int publishProject(long id, @Nullable String description, boolean syncWithGit);

    void updateProject(ProjectDTO projectDTO);

    void updateProjectTags(long id, List<Tag> tags);

    void updateWorkflow(String workflowId, String definition, int version);
}
