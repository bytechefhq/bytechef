
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

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final ConnectionService connectionService;

    public ActionDefinitionFacadeImpl(
        ActionDefinitionService actionDefinitionService, ConnectionService connectionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.connectionService = connectionService;
    }

    @Override
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return actionDefinitionService.executeDynamicProperties(
            propertyName, actionName, componentName, componentVersion, actionParameters,
            connection.getAuthorizationName(), connection.getParameters());
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return actionDefinitionService.executeEditorDescription(
            actionName, componentName, componentVersion, actionParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return actionDefinitionService.executeOptions(
            propertyName, actionName, componentName, componentVersion, actionParameters,
            connection.getAuthorizationName(), connection.getParameters());
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return actionDefinitionService.executeOutputSchema(
            actionName, componentName, componentVersion, actionParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return actionDefinitionService.executeSampleOutput(
            actionName, componentName, componentVersion, actionParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }
}
