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

import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionFacade {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, @Nullable Long connectionId);

    /**
     * Renews webhook subscription definition at provider side. <br>
     * This lambda function is invoked when your webhook subscription is set to have an expiry time, defined in the
     * output of webhook_subscribe. It allows you to refresh as webhook subscriptions so your trigger can continue to
     * receive events.
     *
     * @param componentName    The name of the component for which the webhook refresh is being executed.
     * @param componentVersion The version of the component for which the webhook refresh is being executed.
     * @param triggerName      The name of the trigger associated with the webhook.
     * @param outputParameters A map containing key-value pairs of output parameters that influence the refresh logic.
     * @param connectionId     An optional ID representing the connection context for the execution.
     * @return A {@code TriggerDefinition.WebhookEnableOutput} object containing the result of the webhook refresh
     *         execution.
     */
    TriggerDefinition.WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, Map<String, ?> outputParameters,
        @Nullable Long connectionId);

    void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable Long connectionId);

    void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable Long connectionId);

    List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable Long connectionId);

    OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        @Nullable Long connectionId);

    TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, @Nullable Long jobPrincipalId,
        @Nullable String workflowUuid, Map<String, ?> inputParameters, Object triggerState,
        WebhookRequest webhookRequest, @Nullable Long connectionId, @Nullable Long environmentId,
        @Nullable PlatformType type, boolean editorEnvironment);

    void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, @Nullable Long connectionId);

    TriggerDefinition.WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable Long connectionId, String webhookUrl);

    WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable Long connectionId);

    WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable Long connectionId);
}
