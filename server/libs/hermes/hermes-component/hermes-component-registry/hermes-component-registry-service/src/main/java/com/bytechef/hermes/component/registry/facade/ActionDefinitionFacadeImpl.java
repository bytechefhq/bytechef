
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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.hermes.component.registry.service.ActionDefinitionService;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.RemoteConnectionService;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service("actionDefinitionFacade")
public class ActionDefinitionFacadeImpl implements ActionDefinitionFacade, RemoteActionDefinitionFacade {

    private final RemoteConnectionService connectionService;
    private final ActionDefinitionService actionDefinitionService;

    @SuppressFBWarnings("EI")
    public ActionDefinitionFacadeImpl(
        RemoteConnectionService connectionService, ActionDefinitionService actionDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.connectionService = connectionService;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        Map<String, Object> actionParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executeDynamicProperties(
            componentName, componentVersion, actionName, propertyName, actionParameters, connection);
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> actionParameters,
        Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executeEditorDescription(
            componentName, componentVersion, actionName, actionParameters, connection);
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, Object> actionParameters, Long connectionId, String searchText) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executeOptions(
            componentName, componentVersion, actionName, propertyName, actionParameters, searchText, connection);
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> actionParameters,
        Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executeOutputSchema(
            componentName, componentVersion, actionName, actionParameters, connection);
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, long taskExecutionId,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executePerform(
            componentName, componentVersion, actionName, taskExecutionId, inputParameters, connection);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, Object> inputParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return actionDefinitionService.executeSampleOutput(
            componentName, componentVersion, actionName, inputParameters, connection);
    }
}
