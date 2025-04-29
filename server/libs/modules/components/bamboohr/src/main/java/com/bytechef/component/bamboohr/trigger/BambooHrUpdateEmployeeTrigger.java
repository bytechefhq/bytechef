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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHrUpdateEmployeeTrigger {

    public static final ComponentDsl.ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("updateEmployee")
        .title("Update Employee")
        .description("Triggers when specific employee fields are updated.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties()
        .output(outputSchema(
            array()
                .items(
                    object()
                        .properties(
                            string(FIRST_NAME),
                            string(LAST_NAME),
                            string(EMPLOYEE_NUMBER)))))
        .webhookEnable(BambooHrUpdateEmployeeTrigger::webhookEnable)
        .webhookDisable(BambooHrUpdateEmployeeTrigger::webhookDisable)
        .webhookRequest(BambooHrUpdateEmployeeTrigger::webhookRequest);

    private BambooHrUpdateEmployeeTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID,
                BambooHrUtils.addWebhook(webhookUrl, context)),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        BambooHrUtils.deleteWebhook(outputParameters, context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, TriggerDefinition.HttpHeaders headers,
        TriggerDefinition.HttpParameters parameters,
        TriggerDefinition.WebhookBody body, TriggerDefinition.WebhookMethod method,
        TriggerDefinition.WebhookEnableOutput output, TriggerContext context) {

        List<Map<String, ?>> outputObject = new ArrayList<>();

        if (body.getContent(new TypeReference<Map<String, ?>>() {})
            .get("employees") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    Map<String, Map<String, ?>> fields = (Map<String, Map<String, ?>>) map.get("fields");

                    Map<String, Object> fieldMap = new HashMap<>();

                    for (Map.Entry<String, Map<String, ?>> fieldEntry : fields.entrySet()) {
                        String fieldName = fieldEntry.getKey();
                        Map<String, ?> fieldValue = fieldEntry.getValue();

                        if (fieldValue.containsKey("value")) {
                            fieldMap.put(fieldName, fieldValue.get("value"));
                        }
                    }
                    outputObject.add(fieldMap);
                }
            }
        }
        return outputObject;
    }
}
