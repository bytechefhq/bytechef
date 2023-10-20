
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.hermes.component.registry.remote.client.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.remote.client.AbstractWorkerClient;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnMissingClass(value = "com.bytechef.worker.WorkerApplication")
public class RemoteTriggerDefinitionServiceClient extends AbstractWorkerClient implements TriggerDefinitionService {

    private static final String TRIGGER_DEFINITION_SERVICE = "/trigger-definition-service";

    public RemoteTriggerDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        super(defaultWebClient, discoveryClient, objectMapper);
    }

    @Override
    public void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String webhookUrl, String workflowExecutionId, ComponentConnection connection,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void executeOnEnableListener(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, String searchText, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Object triggerState, WebhookRequest webhookRequest, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, ComponentConnection connection, TriggerContext context) {

        throw new UnsupportedOperationException();
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definition/{componentName}/{componentVersion}/{triggerName}",
                componentName, componentVersion, triggerName),
            TriggerDefinition.class);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-trigger-definitions/{componentName}/{componentVersion}",
                componentName,
                componentVersion),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(List<ComponentOperation> componentOperations) {
        return CollectionUtils.map(
            componentOperations,
            componentOperation -> getTriggerDefinition(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName()));
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return defaultWebClient.get(
            uriBuilder -> toUri(
                uriBuilder, componentName,
                TRIGGER_DEFINITION_SERVICE + "/get-webhook-trigger-flags/{componentName}/{componentVersion}" +
                    "/{triggerName}",
                componentName, componentVersion, triggerName),
            WebhookTriggerFlags.class);
    }
}
