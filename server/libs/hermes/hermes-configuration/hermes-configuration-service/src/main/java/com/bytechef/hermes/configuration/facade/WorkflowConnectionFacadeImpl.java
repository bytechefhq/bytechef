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

package com.bytechef.hermes.configuration.facade;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.registry.domain.Property;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowConnectionFacadeImpl implements WorkflowConnectionFacade {

    private final ConnectionDefinitionService connectionDefinitionService;

    public WorkflowConnectionFacadeImpl(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTask workflowTask) {
        return getWorkflowConnections(
            workflowTask.getType(), (connectionRequired) -> WorkflowConnection.of(workflowTask, connectionRequired));
    }

    @Override
    public List<WorkflowConnection> getWorkflowConnections(WorkflowTrigger workflowTrigger) {
        return getWorkflowConnections(
            workflowTrigger.getType(),
            (connectionRequired) -> WorkflowConnection.of(workflowTrigger, connectionRequired));
    }

    private List<WorkflowConnection> getWorkflowConnections(
        String type, Function<Boolean, List<WorkflowConnection>> workflowConnectionsFunction) {

        List<WorkflowConnection> workflowConnections = List.of();

        OperationType operationType = OperationType.ofType(type);

        if (connectionDefinitionService.connectionExists(operationType.componentName())) {
            ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionDefinition(
                operationType.componentName(), operationType.componentVersion());

            boolean propertiesRequired = CollectionUtils.anyMatch(
                connectionDefinition.getProperties(), Property::getRequired);

            workflowConnections = workflowConnectionsFunction.apply(
                connectionDefinition.isAuthorizationRequired() || propertiesRequired);
        }
        return workflowConnections;
    }
}
