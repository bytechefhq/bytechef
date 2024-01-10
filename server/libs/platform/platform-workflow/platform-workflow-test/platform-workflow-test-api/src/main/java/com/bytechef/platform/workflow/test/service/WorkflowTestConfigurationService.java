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

package com.bytechef.platform.workflow.test.service;

import com.bytechef.platform.workflow.test.domain.WorkflowTestConfiguration;
import com.bytechef.platform.workflow.test.domain.WorkflowTestConfigurationConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface WorkflowTestConfigurationService {

    WorkflowTestConfiguration create(WorkflowTestConfiguration workflowTestConfiguration);

    Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId);

    List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String operationName);

    WorkflowTestConfiguration getWorkflowTestConfiguration(long id);

    List<WorkflowTestConfiguration> getWorkflowTestConfigurations();

    WorkflowTestConfiguration updateWorkflowTestConfiguration(WorkflowTestConfiguration workflowTestConfiguration);

    void updateWorkflowTestConfigurationConnection(
        String workflowId, String operationName, String key, long connectionId);

    void updateWorkflowTestConfigurationInputs(String workflowId, Map<String, Object> inputs);
}
