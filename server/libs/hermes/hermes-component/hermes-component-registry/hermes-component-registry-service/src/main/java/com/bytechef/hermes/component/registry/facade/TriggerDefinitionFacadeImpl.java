
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

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade {

    private final ConnectionService connectionService;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeImpl(
        ConnectionService connectionService, TriggerDefinitionService triggerDefinitionService) {

        this.connectionService = connectionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, Object> triggerParameters, Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, triggerParameters, propertyName, connectionId,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName());
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
        String workflowExecutionId, Long connectionId, String webhookUrl) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookEnable(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(),
            createWebhookUrl(workflowExecutionId, webhookUrl), workflowExecutionId);
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeEditorDescription(
            componentName, componentVersion, triggerName, triggerParameters, connectionId,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName());
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

        triggerDefinitionService.executeOnEnableListener(
            componentName, componentVersion, triggerName, triggerParameters,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), workflowExecutionId);
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> triggerParameters, Long connectionId, String searchText) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            componentName, componentVersion, triggerName, triggerParameters, propertyName, connectionId,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName(), searchText);
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeOutputSchema(
            componentName, componentVersion, triggerName, triggerParameters, connectionId,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName());

    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId) {

        Connection connection = connectionId == null ? null : connectionService.getConnection(connectionId);

        return triggerDefinitionService.executeSampleOutput(
            componentName, componentVersion, triggerName, triggerParameters, connectionId,
            connection == null ? null : connection.getParameters(),
            connection == null ? null : connection.getAuthorizationName());

    }

    private String createWebhookUrl(String workflowExecutionId, String webhookUrl) {
        return webhookUrl + "/api/webhooks/" + workflowExecutionId;
    }
}
