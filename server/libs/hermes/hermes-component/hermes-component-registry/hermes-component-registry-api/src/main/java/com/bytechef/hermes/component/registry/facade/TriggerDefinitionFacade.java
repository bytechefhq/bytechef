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

package com.bytechef.hermes.component.registry.facade;

import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.registry.domain.EditorDescriptionResponse;
import com.bytechef.hermes.registry.domain.OptionsResponse;
import com.bytechef.hermes.registry.domain.OutputSchemaResponse;
import com.bytechef.hermes.registry.domain.PropertiesResponse;
import com.bytechef.hermes.registry.domain.SampleOutputResponse;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionFacade {

    PropertiesResponse executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, Object> inputParameters, Long connectionId);

    void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Long connectionId);

    TriggerDefinition.DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId,
        @NonNull String webhookUrl);

    TriggerDefinition.DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters);

    EditorDescriptionResponse executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId);

    void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId);

    void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Long connectionId);

    OptionsResponse executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, Long connectionId, String searchText);

    OutputSchemaResponse executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId);

    SampleOutputResponse executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Long connectionId);

    TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, @NonNull WebhookRequest webhookRequest,
        Long connectionId);

    boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Long connectionId);
}
