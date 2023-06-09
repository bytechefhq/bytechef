
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.facade;

import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.definition.registry.component.util.ComponentContextSupplier;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.component.context.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI")
    public ActionDefinitionFacadeImpl(
        ActionDefinitionService actionDefinitionService, ConnectionService connectionService,
        ContextFactory contextFactory) {

        this.actionDefinitionService = actionDefinitionService;
        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, long connectionId) {

        ActionContext context = contextFactory.createActionContext(Map.of(componentName, connectionId));

        return ComponentContextSupplier.get(
            context,
            () -> {
                Connection connection = connectionService.getConnection(connectionId);

                return actionDefinitionService.executeDynamicProperties(
                    componentVersion, componentName, actionName, propertyName, actionParameters,
                    connection.getAuthorizationName(), connection.getParameters());
            });
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        long connectionId) {

        ActionContext context = contextFactory.createActionContext(Map.of(componentName, connectionId));

        return ComponentContextSupplier.get(
            context,
            () -> {
                Connection connection = connectionService.getConnection(connectionId);

                return actionDefinitionService.executeEditorDescription(
                    componentName, componentVersion, actionName, actionParameters, connection.getAuthorizationName(),
                    connection.getParameters());
            });
    }

    @Override
    public List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, long connectionId, String searchText) {

        ActionContext context = contextFactory.createActionContext(Map.of(componentName, connectionId));

        return ComponentContextSupplier.get(
            context,
            () -> {
                Connection connection = connectionService.getConnection(connectionId);

                return actionDefinitionService.executeOptions(
                    componentName, componentVersion, actionName, propertyName, actionParameters,
                    connection.getAuthorizationName(), connection.getParameters(), searchText);
            });
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        long connectionId) {

        ActionContext context = contextFactory.createActionContext(Map.of(componentName, connectionId));

        return ComponentContextSupplier.get(
            context,
            () -> {
                Connection connection = connectionService.getConnection(connectionId);

                return actionDefinitionService.executeOutputSchema(
                    componentName, componentVersion, actionName, actionParameters, connection.getAuthorizationName(),
                    connection.getParameters());
            });
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        ActionContext context = contextFactory.createActionContext(Map.of(actionName, connectionId));

        return ComponentContextSupplier.get(
            context,
            () -> {
                Connection connection = connectionService.getConnection(connectionId);

                return actionDefinitionService.executeSampleOutput(
                    componentName, componentVersion, actionName, actionParameters, connection.getAuthorizationName(),
                    connection.getParameters());
            });
    }
}
