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

package com.bytechef.helios.configuration.service;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectInstanceWorkflowService {

    List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows);

    void delete(Long id);

    Optional<ProjectInstanceWorkflowConnection> fetchProjectInstanceWorkflowConnection(
        long projectInstanceId, String workflowId, String operationName, String key);

    ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId);

    ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(
        long projectInstanceOd, String workflowId, String operationName, String key);

    List<ProjectInstanceWorkflowConnection> getProjectInstanceWorkflowConnections(
        long projectInstanceOd, String workflowId, String operationName);

    List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId);

    List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds);

    boolean isProjectInstanceWorkflowEnabled(long projectInstanceId, String workflowId);

    ProjectInstanceWorkflow update(ProjectInstanceWorkflow projectInstanceWorkflow);

    List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows);

    void updateEnabled(Long id, boolean enable);
}
