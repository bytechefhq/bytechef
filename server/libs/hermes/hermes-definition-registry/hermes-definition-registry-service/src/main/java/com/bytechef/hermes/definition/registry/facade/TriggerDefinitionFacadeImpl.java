
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

import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.component.context.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.util.ComponentContextSupplier;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade {

    private final ConnectionService connectionService;
    private final ContextFactory contextFactory;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeImpl(
        ConnectionService connectionService, ContextFactory contextFactory,
        TriggerDefinitionService triggerDefinitionService, String webhookUrl) {

        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, Object> triggerParameters, Long connectionId) {

        return ComponentContextSupplier.get(
            getTriggerContext(componentName, connectionId),
            () -> {
                Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

                return triggerDefinitionService.executeDynamicProperties(
                    componentName, componentVersion, triggerName, triggerParameters, propertyName,
                    connection == null ? null : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName());
            });
    }

    @Override
    public void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, DynamicWebhookEnableOutput output, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeDynamicWebhookDisable(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), workflowExecutionId, output);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookEnable(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), createWebhookUrl(workflowExecutionId),
            workflowExecutionId);
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        return ComponentContextSupplier.get(
            getTriggerContext(componentName, connectionId),
            () -> {
                Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

                return triggerDefinitionService.executeEditorDescription(
                    componentName, componentVersion, triggerName, triggerParameters,
                    connection == null ? null : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName());
            });
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), workflowExecutionId);

    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        triggerDefinitionService.executeListenerEnable(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), workflowExecutionId);
    }

    @Override
    public List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> triggerParameters, Long connectionId, String searchText) {

        return ComponentContextSupplier.get(
            getTriggerContext(componentName, connectionId),
            () -> {
                Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

                return triggerDefinitionService.executeOptions(
                    componentName, componentVersion, triggerName, triggerParameters, propertyName,
                    connection == null ? null : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName(), searchText);
            });
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        return ComponentContextSupplier.get(
            getTriggerContext(componentName, connectionId),
            () -> {
                Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

                return triggerDefinitionService.executeOutputSchema(
                    componentName, componentVersion, triggerName, triggerParameters,
                    connection == null ? null : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName());
            });
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        return ComponentContextSupplier.get(
            getTriggerContext(componentName, connectionId),
            () -> {
                Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

                return triggerDefinitionService.executeSampleOutput(
                    componentName, componentVersion, triggerName, triggerParameters,
                    connection == null ? null : connection.getParameters(),
                    connection == null ? null : connection.getAuthorizationName());
            });
    }

    private String createWebhookUrl(String workflowExecutionId) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }

    private TriggerContext getTriggerContext(String componentName, Long connectionId) {
        return contextFactory.createTriggerContext(
            connectionId == null ? Map.of() : Map.of(componentName, connectionId));
    }
}
