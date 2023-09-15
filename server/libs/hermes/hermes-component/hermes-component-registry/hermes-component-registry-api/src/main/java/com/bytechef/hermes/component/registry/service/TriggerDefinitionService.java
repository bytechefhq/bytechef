
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionService {

    List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, @Nullable Connection connection);

    void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, @Nullable Connection connection);

    DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String webhookUrl, @NonNull String workflowExecutionId,
        @Nullable Connection connection);

    DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters);

    String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> triggerParameters, @Nullable Connection connection);

    void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Connection connection);

    void executeOnEnableListener(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Connection connection);

    List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, @Nullable String searchText,
        @Nullable Connection connection);

    List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @Nullable Connection connection);

    Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @Nullable Connection connection);

    TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, @NonNull WebhookRequest webhookRequest,
        @Nullable Connection connection);

    boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest,
        @Nullable Connection connection);

    TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName);

    List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion);

    List<TriggerDefinition> getTriggerDefinitions(@NonNull List<ComponentOperation> componentOperations);

    WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName);
}
