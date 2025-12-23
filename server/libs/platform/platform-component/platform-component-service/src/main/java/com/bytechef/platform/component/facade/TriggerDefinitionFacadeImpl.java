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

package com.bytechef.platform.component.facade;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("triggerDefinitionFacade")
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
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, inputParameters, propertyName, lookupDependsOnPaths,
            componentConnection);
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, triggerName, componentConnection, outputParameters);
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, componentConnection);
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerEnable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, componentConnection);
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOptions(
            componentName, componentVersion, triggerName, propertyName, inputParameters,
            lookupDependsOnPaths, searchText, componentConnection);
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeOutput(
            componentName, componentVersion, triggerName, inputParameters, componentConnection);
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Long jobPrincipalId, String workflowUuid,
        Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest, Long connectionId,
        Long environmentId, PlatformType type, boolean editorEnvironment) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeTrigger(
            componentName, componentVersion, triggerName, jobPrincipalId, workflowUuid, inputParameters, triggerState,
            webhookRequest, componentConnection, environmentId, type, editorEnvironment);
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeWebhookDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, outputParameters,
            componentConnection);
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId, String webhookUrl) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookEnable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, webhookUrl,
            componentConnection);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidate(
            componentName, componentVersion, triggerName, inputParameters,
            webhookRequest, componentConnection);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidateOnEnable(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest, componentConnection);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationType());
        }

        return componentConnection;
    }
}
