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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    WorkflowDTO addWorkflow(long id, @NonNull String definition);

    ProjectDTO createProject(@NonNull ProjectDTO projectDTO);

    void deleteProject(long id);

    void deleteWorkflow(@NonNull String workflowId);

    Map<String, ?> deleteWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex);

    ProjectDTO duplicateProject(long id);

    String duplicateWorkflow(long id, @NonNull String workflowId);

    ProjectDTO getProject(long id);

    List<Category> getProjectCategories();

    List<Tag> getProjectTags();

    WorkflowDTO getProjectWorkflow(String workflowId);

    WorkflowDTO getProjectWorkflow(long projectWorkflowId);

    List<WorkflowDTO> getProjectWorkflows();

    List<WorkflowDTO> getProjectWorkflows(long id);

    List<WorkflowDTO> getProjectVersionWorkflows(long id, int projectVersion);

    List<ProjectDTO> getProjects(Long categoryId, boolean projectInstances, Long tagId, Status status);

    ProjectDTO updateProject(@NonNull ProjectDTO projectDTO);

    void updateProjectTags(long id, @NonNull List<Tag> tags);

    WorkflowDTO updateWorkflow(String workflowId, String definition, int version);

    UpdateParameterResultDTO updateWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex, Object value);
}
