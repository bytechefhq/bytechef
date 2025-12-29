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

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectWorkflowFacade {

    ProjectWorkflow addWorkflow(long projectId, String definition);

    void deleteSharedWorkflow(String workflowId);

    void deleteWorkflow(String workflowId);

    String duplicateWorkflow(long projectId, String workflowId);

    void exportSharedWorkflow(String workflowId, @Nullable String description);

    List<WorkflowTemplateDTO> getPreBuiltWorkflowTemplates(String query, String category);

    ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId);

    ProjectWorkflowDTO getProjectWorkflow(String workflowId);

    List<ProjectWorkflowDTO> getProjectWorkflows();

    List<ProjectWorkflowDTO> getProjectWorkflows(long projectId);

    List<ProjectWorkflowDTO> getProjectVersionWorkflows(long projectId, int projectVersion, boolean includeAllFields);

    SharedWorkflowDTO getSharedWorkflow(String workflowUuid);

    WorkflowTemplateDTO getWorkflowTemplate(String id, boolean sharedWorkflow);

    long importWorkflowTemplate(long projectId, String id, boolean sharedWorkflow);

    void updateWorkflow(String workflowId, String definition, int version);
}
