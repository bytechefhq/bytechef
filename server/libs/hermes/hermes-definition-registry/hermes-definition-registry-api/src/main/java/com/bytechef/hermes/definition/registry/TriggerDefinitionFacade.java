
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

package com.bytechef.hermes.definition.registry;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface TriggerDefinitionFacade {

    void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output);

    DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String webhookUrl,
        String workflowExecutionId);

    DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String triggerName, String componentName, int componentVersion, DynamicWebhookEnableOutput output);

    void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId);

    void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId);
}
