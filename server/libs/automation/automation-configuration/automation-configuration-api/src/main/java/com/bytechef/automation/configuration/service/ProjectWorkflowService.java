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

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectWorkflowService {

    ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId);

    void deleteProjectWorkflows(List<Long> ids);

    ProjectWorkflow getProjectWorkflow(long id);

    List<Long> getProjectWorkflowIds(long projectId, int projectVersion);

    List<ProjectWorkflow> getProjectWorkflows();

    List<ProjectWorkflow> getProjectWorkflows(long projectId);

    List<ProjectWorkflow> getProjectWorkflows(long projectId, int projectVersion);

    List<String> getWorkflowIds(long projectId);

    List<String> getWorkflowIds(long projectId, int projectVersion);

    ProjectWorkflow getWorkflowProjectWorkflow(String workflowId);

    void removeWorkflow(long projectId, int projectVersion, String workflowId);

    ProjectWorkflow update(ProjectWorkflow projectWorkflow);
}
