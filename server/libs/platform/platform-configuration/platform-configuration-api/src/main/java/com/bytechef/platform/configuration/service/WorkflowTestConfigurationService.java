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

    Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId);

    Optional<Long> fetchWorkflowTestConfigurationConnectionId(String workflowId, String workflowNodeName);

    List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName);

    WorkflowTestConfiguration saveWorkflowTestConfiguration(WorkflowTestConfiguration workflowTestConfiguration);

    List<Long> getWorkflowTestConfigurationConnectionIds(String workflowId, List<String> workflowTaskNames);

    Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId);

    void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId);

    void saveWorkflowTestConfigurationInputs(String workflowId, Map<String, String> inputs);
}
