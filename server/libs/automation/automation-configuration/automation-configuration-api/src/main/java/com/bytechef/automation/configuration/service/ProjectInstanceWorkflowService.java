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

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflowConnection;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectInstanceWorkflowService {

    ProjectInstanceWorkflow create(ProjectInstanceWorkflow projectInstanceWorkflow);

    List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows);

    void delete(long id);

    void deleteProjectInstanceWorkflows(long projectInstanceId);

    Optional<ProjectInstanceWorkflowConnection> fetchProjectInstanceWorkflowConnection(
        long projectInstanceOd, String workflowId, String workflowNodeName, String workflowConnectionKey);

    ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId);

    List<ProjectInstanceWorkflowConnection> getProjectInstanceWorkflowConnections(
        long projectInstanceOd, String workflowId, String workflowNodeName);

    List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId);

    List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds);

    boolean isConnectionUsed(long connectionId);

    boolean isProjectInstanceWorkflowEnabled(long projectInstanceId, String workflowId);

    ProjectInstanceWorkflow update(ProjectInstanceWorkflow projectInstanceWorkflow);

    List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows);

    void updateEnabled(Long id, boolean enable);
}
