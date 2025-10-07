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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.MONITOR_FIELDS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.POST_FIELDS;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class BambooHrUpdatedEmployeeTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("updatedEmployee")
        .title("Updated Employee")
        .description("Triggers when specific employee fields are updated.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            array(MONITOR_FIELDS)
                .label("Fields to Monitor")
                .description("The fields to monitor for changes.")
                .items(string())
                .options((OptionsFunction<String>) BambooHrUtils::getFieldOptions)
                .required(true),
            array(POST_FIELDS)
                .label("Fields to include in the Output")
                .description("The fields to include in the output.")
                .items(string())
                .options((OptionsFunction<String>) BambooHrUtils::getFieldOptions)
                .required(true))
        .output()
        .webhookEnable(BambooHrUpdatedEmployeeTrigger::webhookEnable)
        .webhookDisable(BambooHrUpdatedEmployeeTrigger::webhookDisable)
        .webhookRequest(BambooHrUpdatedEmployeeTrigger::webhookRequest);

    private BambooHrUpdatedEmployeeTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        List<String> postFields = inputParameters.getRequiredList(POST_FIELDS, String.class);

        Map<String, String> options = new HashMap<>();
        for (String postField : postFields) {
            options.put(postField, postField);
        }

        Map<String, ?> body = context.http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "name", "bambooHRWebhook",
                    MONITOR_FIELDS, inputParameters.getRequiredList(MONITOR_FIELDS, String.class),
                    POST_FIELDS, options,
                    "url", webhookUrl,
                    "format", "json"))
            .header("accept", "application/json")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (String) body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(http -> http.delete("/webhooks/" + outputParameters.getString(ID)))
            .header("accept", "application/json")
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        Map<String, ?> content = body.getContent(new TypeReference<>() {});

        return content.get("employees");
    }
}
