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

package com.bytechef.platform.component.registry.facade;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.registry.definition.factory.ContextFactory;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.helper.TokenRefreshHelper;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
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
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeDynamicProperties(
            componentName, componentVersion, triggerName, inputParameters, propertyName, lookupDependsOnPaths,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        return triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, triggerName, outputParameters,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                null));
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerDisable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        triggerDefinitionService.executeListenerEnable(
            componentName, componentVersion, triggerName, inputParameters, workflowExecutionId, componentConnection,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                componentConnection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext context = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, context, null,
            (componentConnection1, triggerContext) -> triggerDefinitionService.executeOptions(
                componentName, componentVersion, triggerName, inputParameters,
                propertyName, lookupDependsOnPaths, searchText, componentConnection, triggerContext),
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null,
                null, null, componentConnection1));
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext context = contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
            null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, context, null,
            (componentConnection1, triggerContext) -> triggerDefinitionService.executeOutput(
                componentName, componentVersion, triggerName, inputParameters, componentConnection, triggerContext),
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null,
                null, null, componentConnection1));
    }

    @Override
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull AppType type, Long instanceId, String workflowReferenceCode, Long jobId,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeTrigger(
            componentName, componentVersion, triggerName, inputParameters, triggerState, webhookRequest,
            componentConnection,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, type,
                workflowReferenceCode, jobId, componentConnection));
    }

    @Override
    public void executeWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext context = contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
            null, null, componentConnection);

        tokenRefreshHelper.executeSingleConnectionFunction(componentName, componentVersion, componentConnection,
            context, null,
            (componentConnection1, triggerContext) -> {
                triggerDefinitionService.executeWebhookDisable(
                    componentName, componentVersion, triggerName, inputParameters, workflowExecutionId,
                    outputParameters,
                    componentConnection1, triggerContext);

                return null;
            },
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null,
                null, null, componentConnection1));
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext context = contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
            null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(componentName, componentVersion, componentConnection,
            context, null,
            (componentConnection1, triggerContext) -> triggerDefinitionService.executeWebhookEnable(
                componentName, componentVersion, triggerName, inputParameters,
                webhookUrl, workflowExecutionId, componentConnection, triggerContext),
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null,
                null, null, componentConnection1));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        TriggerContext context = contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
            null, null, componentConnection);

        return tokenRefreshHelper.executeSingleConnectionFunction(componentName, componentVersion, componentConnection,
            context, null,
            (componentConnection1, triggerContext) -> triggerDefinitionService.executeWebhookValidate(
                componentName, componentVersion, triggerName, inputParameters,
                webhookRequest, componentConnection, triggerContext),
            componentConnection1 -> contextFactory.createTriggerContext(componentName, componentVersion, triggerName,
                null,
                null, null, componentConnection1));
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId) {

        ComponentConnection componentConnection = getComponentConnection(connectionId);

        return triggerDefinitionService.executeWebhookValidateOnEnable(
            componentName, componentVersion, triggerName, inputParameters, webhookRequest,
            componentConnection, contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null,
                null, null, componentConnection));
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters) {

        return triggerDefinitionService.executeWorkflowNodeDescription(
            componentName, componentVersion, triggerName, inputParameters,
            contextFactory.createTriggerContext(componentName, componentVersion, triggerName, null, null, null,
                null));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, int statusCode,
        Object body) {

        TriggerContext actionContext = contextFactory.createTriggerContext(
            componentName, componentVersion, triggerName, null, null, null, null);

        return triggerDefinitionService.executeProcessErrorResponse(
            componentName, componentVersion, triggerName, statusCode, body, actionContext);
    }

    private ComponentConnection getComponentConnection(Long connectionId) {
        ComponentConnection componentConnection = null;

        if (connectionId != null) {
            Connection connection = connectionService.getConnection(connectionId);

            componentConnection = new ComponentConnection(
                connection.getComponentName(), connection.getConnectionVersion(), connectionId,
                connection.getParameters(), connection.getAuthorizationName());
        }

        return componentConnection;
    }
}
