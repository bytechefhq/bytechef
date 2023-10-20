
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

package com.bytechef.hermes.definition.registry.facade;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.definition.registry.domain.Option;
import com.bytechef.hermes.definition.registry.domain.ValueProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionFacade {

    List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, Object> triggerParameters, Long connectionId);

    void executeDynamicWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, DynamicWebhookEnableOutput output, Long connectionId);

    DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId);

    String executeEditorDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId);

    void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId);

    void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        String workflowExecutionId, Long connectionId);

    List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Map<String, ?> triggerParameters, Long connectionId, String searchText);

    List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId);

    Object executeSampleOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> triggerParameters,
        Long connectionId);
}
