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

package com.bytechef.platform.configuration.service;

import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface WorkflowTestConfigurationService {

    void delete(String workflowId);

    void delete(String workflowId, long environmentId);

    void delete(List<String> workflowIds);

    void deleteWorkflowTestConfigurationConnection(long connectionId);

    Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId, long environmentId);

    Optional<Long> fetchWorkflowTestConfigurationConnectionId(
        String workflowId, String workflowNodeName, long environmentId);

    List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, long environmentId);

    List<WorkflowTestConfiguration> getWorkflowTestConfigurations(String workflowId);

    Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId, long environmentId);

    boolean isConnectionUsed(long connectionId);

    WorkflowTestConfiguration saveWorkflowTestConfiguration(WorkflowTestConfiguration workflowTestConfiguration);

    void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId, boolean workflowNodeTrigger,
        long environmentId);

    void saveWorkflowTestConfigurationInputs(String workflowId, String key, String value, long environmentId);

    void updateWorkflowId(String oldWorkflowId, String newWorkflowId);

    void deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long environmentId);
}
