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
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionFacade {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, @Nullable Long connectionId);

    /**
     * Renews webhook subscription definition at provider side.
     *
     * This lambda function is invoked when your webhook subscription is set to have an expiry time, defined in the
     * output of webhook_subscribe. It allows you to refresh as webhook subscriptions so your trigger can continue to
     * receive events.
     *
     * @param componentName
     * @param componentVersion
     * @param triggerName
     * @param outputParameters
     * @return
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
        WebhookRequest webhookRequest, @Nullable Long connectionId, boolean editorEnvironment, @Nullable ModeType type);

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
