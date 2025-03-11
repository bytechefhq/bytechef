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

package com.bytechef.platform.component.service;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionService {

    List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, @Nullable ComponentConnection connection,
        TriggerContext context);

    WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, ComponentConnection connection,
        Map<String, ?> outputParameters, TriggerContext context);

    String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        TriggerContext context);

    void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable ComponentConnection connection, TriggerContext context);

    void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, @Nullable ComponentConnection connection, TriggerContext context);

    List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, @Nullable String searchText,
        @Nullable ComponentConnection connection, TriggerContext context);

    OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection connection, TriggerContext context);

    ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String triggerName, int statusCode, Object body,
        Context triggerContext);

    TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        @Nullable ComponentConnection connection, TriggerContext context);

    void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String workflowExecutionId,
        Map<String, ?> outputParameters, @Nullable ComponentConnection connection,
        TriggerContext context);

    WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String webhookUrl, String workflowExecutionId, @Nullable ComponentConnection connection,
        TriggerContext context);

    WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable ComponentConnection connection, TriggerContext context);

    WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, @Nullable ComponentConnection connection, TriggerContext context);

    TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName);

    List<TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion);

    WebhookTriggerFlags getWebhookTriggerFlags(String componentName, int componentVersion, String triggerName);
}
