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

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.TriggerDefinitionErrorType;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
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
    private final ContextFactory contextFactory;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TokenRefreshHelper tokenRefreshHelper;

    @SuppressFBWarnings("EI")
    public TriggerDefinitionFacadeImpl(
        ConnectionService connectionService, ContextFactory contextFactory,
        TriggerDefinitionService triggerDefinitionService, TokenRefreshHelper tokenRefreshHelper) {

        this.connectionService = connectionService;
        this.contextFactory = contextFactory;
        this.triggerDefinitionService = triggerDefinitionService;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, inputParameters, propertyName, lookupDependsOnPaths,
            componentConnection, contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection, true));
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, triggerName, componentConnection, outputParameters,
            contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, null, false));
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId,
            componentConnection, contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection, false));
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerEnable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, componentConnection,
            contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection, false));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, triggerContext, null,
            (componentConnection1, triggerContext1) -> triggerDefinitionService.executeOptions(
                componentName, componentVersion, triggerName, inputParameters,
                propertyName, lookupDependsOnPaths, searchText, componentConnection1, triggerContext1),
            componentConnection1 -> contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection1, true));
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, triggerContext, null,
            (componentConnection1, triggerContext1) -> triggerDefinitionService.executeOutput(
                componentName, componentVersion, triggerName, inputParameters, componentConnection1, triggerContext1),
            componentConnection1 -> contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection1, true));
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, ModeType type, Long jobPrincipalId,
        String workflowUuid, Map<String, ?> inputParameters, Object triggerState,
        WebhookRequest webhookRequest, Long connectionId, boolean editorEnvironment) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, jobPrincipalId, workflowUuid,
            componentConnection, editorEnvironment);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, triggerContext,
            TriggerDefinitionErrorType.TRIGGER_TEST_FAILED,
            (componentConnection1, triggerContext1) -> triggerDefinitionService.executeTrigger(
                componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
                componentConnection1, triggerContext1),
            componentConnection1 -> contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, jobPrincipalId, workflowUuid,
                componentConnection1, editorEnvironment));
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection, false);

        tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, triggerContext, null,
            (componentConnection1, triggerContext1) -> {
                triggerDefinitionService.executeWebhookDisable(
                    componentName, componentVersion, triggerName, inputParameters, workflowExecutionId,
                    outputParameters, componentConnection1, triggerContext1);

                return null;
            },
            componentConnection1 -> contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection1, false));
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Long connectionId, String webhookUrl) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection, false);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, triggerContext,
            TriggerDefinitionErrorType.DYNAMIC_WEBHOOK_ENABLE_FAILED,
            (componentConnection1, triggerContext1) -> triggerDefinitionService.executeWebhookEnable(
                componentName, componentVersion, triggerName, inputParameters,
                webhookUrl, workflowExecutionId, componentConnection1, triggerContext1),
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null, null, null, componentConnection1, false));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext triggerContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection, false);

        return tokenRefreshHelper.executeSingleConnectionFunction(componentName, componentVersion, componentConnection,
            triggerContext, null,
            (componentConnection1, triggerContext1) -> triggerDefinitionService.executeWebhookValidate(
                componentName, componentVersion, triggerName, inputParameters,
                webhookRequest, componentConnection1, triggerContext1),
            componentConnection1 -> contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection1, false));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidateOnEnable(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, componentConnection, false));
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters) {

        return triggerDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, triggerName, inputParameters,
            contextFactory.createTriggerContext(
                componentName, componentVersion, triggerName, null, null, null, null, true));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String triggerName, int statusCode, Object body) {

        TriggerContext actionContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, null, false);

        return triggerDefinitionService.executeProcessErrorResponse(
            componentName, componentVersion, triggerName, statusCode, body, actionContext);
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
