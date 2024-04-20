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

package com.bytechef.platform.component.registry.service;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionService {

    List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName,
        List<String> loadDependsOnPaths, @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, @Nullable ComponentConnection connection,
        @NonNull TriggerContext context);

    DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String webhookUrl, @NonNull String workflowExecutionId,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters, @NonNull TriggerContext context);

    String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters,
        @NonNull TriggerContext context);

    void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, @NonNull List<String> loadDependsOnPaths,
        @Nullable String searchText, @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull TriggerContext context);

    TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context);

    TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName);

    List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion);

    WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName);
}
