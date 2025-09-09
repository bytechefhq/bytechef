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

package com.bytechef.component.form.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.form.constant.FormConstants.CHECKBOX;
import static com.bytechef.component.form.constant.FormConstants.DATETIME_PICKER;
import static com.bytechef.component.form.constant.FormConstants.DATE_PICKER;
import static com.bytechef.component.form.constant.FormConstants.FIELD_DESCRIPTION;
import static com.bytechef.component.form.constant.FormConstants.FIELD_NAME;
import static com.bytechef.component.form.constant.FormConstants.FIELD_TYPE;
import static com.bytechef.component.form.constant.FormConstants.FILE_INPUT;
import static com.bytechef.component.form.constant.FormConstants.INPUT;
import static com.bytechef.component.form.constant.FormConstants.INPUTS;
import static com.bytechef.component.form.constant.FormConstants.REQUIRED;
import static com.bytechef.component.form.constant.FormConstants.SELECT;
import static com.bytechef.component.form.constant.FormConstants.TEXTAREA;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * @author ByteChef
 */
public class NewFormRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFormRequest")
        .title("New Form Request")
        .description("Triggers when a new form request is received.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .properties(
            array(INPUTS)
                .label("Form Inputs")
                .description("Define the form input fields")
                .items(
                    object()
                        .properties(
                            string(FIELD_NAME)
                                .label("Field Name")
                                .description("The name of the form field")
                                .required(true),
                            integer(FIELD_TYPE)
                                .label("Field Type")
                                .description("The type of the form field")
                                .options(
                                    option("Checkbox", CHECKBOX),
                                    option("Date Picker", DATE_PICKER),
                                    option("Datetime Picker", DATETIME_PICKER),
                                    option("File Input", FILE_INPUT),
                                    option("TextArea", TEXTAREA),
                                    option("Input", INPUT),
                                    option("Select", SELECT))
                                .required(true),
                            string(FIELD_DESCRIPTION)
                                .label("Field Description")
                                .description("Description of the form field")
                                .required(false),
                            bool(REQUIRED)
                                .label("Required")
                                .description("Whether this field is required")
                                .defaultValue(false)
                                .required(false)))
                .required(true))
        .output(
            outputSchema(
                object()))
        .webhookRequest(NewFormRequestTrigger::getWebhookResult);

    protected static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput webhookEnableOutput, TriggerContext context) {

        Assert.notNull(body.getContent(), "Body content is required.");

        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.getContent();

        return content;
    }
}
