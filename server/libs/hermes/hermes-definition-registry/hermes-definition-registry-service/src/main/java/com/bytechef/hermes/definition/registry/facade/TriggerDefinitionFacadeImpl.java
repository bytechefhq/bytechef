
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
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade {

    private final ConnectionService connectionService;
    private final TriggerDefinitionService triggerDefinitionService;

    public TriggerDefinitionFacadeImpl(
        ConnectionService connectionService, TriggerDefinitionService triggerDefinitionService) {

        this.connectionService = connectionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            propertyName, triggerName, componentName, componentVersion, triggerParameters,
            connection.getAuthorizationName(), connection.getParameters());
    }

    @Override
    public String executeEditorDescription(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeEditorDescription(
            triggerName, componentName, componentVersion, triggerParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            propertyName, triggerName, componentName, componentVersion, triggerParameters,
            connection.getAuthorizationName(), connection.getParameters());
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOutputSchema(
            triggerName, componentName, componentVersion, triggerParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }

    @Override
    public Object executeSampleOutput(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        long connectionId) {

        Connection connection = connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeSampleOutput(
            triggerName, componentName, componentVersion, triggerParameters, connection.getAuthorizationName(),
            connection.getParameters());
    }
}
