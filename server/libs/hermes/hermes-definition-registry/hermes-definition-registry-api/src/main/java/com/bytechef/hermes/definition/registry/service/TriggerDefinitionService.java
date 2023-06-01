
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionService {

    void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output);

    DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String webhookUrl,
        String workflowExecutionId);

    DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output);

    String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String workflowExecutionId);

    void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> connectionParameters,
        String authorizationName, Map<String, ?> triggerParameters, String workflowExecutionId);

    List<OptionDTO> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> triggerParameters, String authorizationName, Map<String, ?> connectionParameters,
        String searchText);

    List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> triggerParameters, String authorizationName, Map<String, ?> connectionParameters);

    Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    TriggerDefinitionDTO getTriggerDefinition(String componentName, int componentVersion, String triggerName);

    List<TriggerDefinitionDTO> getTriggerDefinitions(String componentName, int componentVersion);
}
