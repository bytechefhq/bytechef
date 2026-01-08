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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;
import static com.bytechef.component.form.constant.FormConstants.APPEND_ATTRIBUTION;
import static com.bytechef.component.form.constant.FormConstants.BUTTON_LABEL;
import static com.bytechef.component.form.constant.FormConstants.CUSTOM_FORM_STYLING;
import static com.bytechef.component.form.constant.FormConstants.DEFAULT_VALUE;
import static com.bytechef.component.form.constant.FormConstants.FIELD_DESCRIPTION;
import static com.bytechef.component.form.constant.FormConstants.FIELD_LABEL;
import static com.bytechef.component.form.constant.FormConstants.FIELD_NAME;
import static com.bytechef.component.form.constant.FormConstants.FIELD_OPTIONS;
import static com.bytechef.component.form.constant.FormConstants.FIELD_TYPE;
import static com.bytechef.component.form.constant.FormConstants.FORM_DESCRIPTION;
import static com.bytechef.component.form.constant.FormConstants.FORM_TITLE;
import static com.bytechef.component.form.constant.FormConstants.IGNORE_BOTS;
import static com.bytechef.component.form.constant.FormConstants.INPUTS;
import static com.bytechef.component.form.constant.FormConstants.MAX_SELECTION;
import static com.bytechef.component.form.constant.FormConstants.MIN_SELECTION;
import static com.bytechef.component.form.constant.FormConstants.MULTIPLE_CHOICE;
import static com.bytechef.component.form.constant.FormConstants.PLACEHOLDER;
import static com.bytechef.component.form.constant.FormConstants.REQUIRED;
import static com.bytechef.component.form.constant.FormConstants.USE_WORKFLOW_TIMEZONE;
import static com.bytechef.component.form.util.FieldType.CHECKBOX;
import static com.bytechef.component.form.util.FieldType.CUSTOM_HTML;
import static com.bytechef.component.form.util.FieldType.DATETIME_PICKER;
import static com.bytechef.component.form.util.FieldType.DATE_PICKER;
import static com.bytechef.component.form.util.FieldType.EMAIL_INPUT;
import static com.bytechef.component.form.util.FieldType.FILE_INPUT;
import static com.bytechef.component.form.util.FieldType.HIDDEN_FIELD;
import static com.bytechef.component.form.util.FieldType.INPUT;
import static com.bytechef.component.form.util.FieldType.NUMBER_INPUT;
import static com.bytechef.component.form.util.FieldType.PASSWORD_INPUT;
import static com.bytechef.component.form.util.FieldType.RADIO;
import static com.bytechef.component.form.util.FieldType.SELECT;
import static com.bytechef.component.form.util.FieldType.TEXTAREA;
import static com.bytechef.component.form.util.FieldType.fromValue;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.form.util.FieldType;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class NewFormRequestTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFormRequest")
        .title("New Form Request")
        .description("Triggers when a new form request is received.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .properties(
            string(FORM_TITLE)
                .label("Form Title")
                .description("The title for your form. Displayed as the main page title (h1).")
                .required(false),
            string(FORM_DESCRIPTION)
                .label("Form Description")
                .description("A subtitle shown under the form title. Use \n or <br> for line breaks.")
                .required(false),
            string(BUTTON_LABEL)
                .label("Button Label")
                .description("Label for the submit button.")
                .required(false),
            bool(IGNORE_BOTS)
                .label("Ignore Bots")
                .description("Ignore requests from bots and link previewers.")
                .defaultValue(false)
                .required(false),
            bool(USE_WORKFLOW_TIMEZONE)
                .label("Use Workflow Timezone")
                .description("Use the workflow timezone for the submittedAt timestamp instead of UTC.")
                .defaultValue(false)
                .required(false),
            bool(APPEND_ATTRIBUTION)
                .label("Append Attribution")
                .description("Show an attribution footer on the public form.")
                .defaultValue(true)
                .required(false),
            string(CUSTOM_FORM_STYLING)
                .label("Custom Form Styling (CSS)")
                .description("Override default form styles with custom CSS.")
                .controlType(TEXT_AREA)
                .required(false),
            array(INPUTS)
                .label("Form Inputs")
                .description("Define the form input fields")
                .items(
                    object()
                        .properties(
                            string(FIELD_LABEL)
                                .label("Field Label")
                                .description("The label of the form field"),
                            string(FIELD_NAME)
                                .label("Field Name")
                                .description("The name of the form field")
                                .displayCondition("fieldType != %s".formatted(CUSTOM_HTML.getValue())),
                            integer(FIELD_TYPE)
                                .label("Field Type")
                                .description("The type of the form field")
                                .options(
                                    option("Checkbox", CHECKBOX.getValue()),
                                    option("Custom HTML", CUSTOM_HTML.getValue()),
                                    option("Date Picker", DATE_PICKER.getValue()),
                                    option("Datetime Picker", DATETIME_PICKER.getValue()),
                                    option("Email", EMAIL_INPUT.getValue()),
                                    option("File", FILE_INPUT.getValue()),
                                    option("Hidden Field", HIDDEN_FIELD.getValue()),
                                    option("Input", INPUT.getValue()),
                                    option("Number", NUMBER_INPUT.getValue()),
                                    option("Password", PASSWORD_INPUT.getValue()),
                                    option("Radio Button", RADIO.getValue()),
                                    option("Select", SELECT.getValue()),
                                    option("Textarea", TEXTAREA.getValue()))
                                .required(true),
                            string(FIELD_DESCRIPTION)
                                .label("Field Description")
                                .description("Description of the form field")
                                .required(false),
                            string(PLACEHOLDER)
                                .label("Placeholder")
                                .description("The placeholder text.")
                                .required(false)
                                .displayCondition("contains({%s,%s,%s,%s,%s}, fieldType)".formatted(
                                    EMAIL_INPUT.getValue(), INPUT.getValue(), NUMBER_INPUT.getValue(),
                                    PASSWORD_INPUT.getValue(), TEXTAREA.getValue())),
                            string(DEFAULT_VALUE)
                                .label("Default value")
                                .description("Pre-filled or pre-selected value for compatible fields.")
                                .displayCondition("fieldType != %s".formatted(CUSTOM_HTML.getValue()))
                                .required(false),
                            string(DEFAULT_VALUE)
                                .label("Default value")
                                .description("Pre-filled or pre-selected value for compatible fields.")
                                .controlType(TEXT_AREA)
                                .displayCondition("fieldType == %s".formatted(CUSTOM_HTML.getValue()))
                                .required(false),
                            array(FIELD_OPTIONS)
                                .label("Field Options")
                                .description("The field label/value options.")
                                .items(
                                    object()
                                        .properties(
                                            string("label")
                                                .label("Label")
                                                .required(true),
                                            string("value")
                                                .label("Value")
                                                .required(true)))
                                .required(false)
                                .displayCondition(
                                    "contains({%s,%s}, fieldType)".formatted(RADIO.getValue(), SELECT.getValue())),
                            bool(MULTIPLE_CHOICE)
                                .label("Multiple Choice")
                                .description("Allow multiple selections.")
                                .defaultValue(false)
                                .required(false)
                                .displayCondition(
                                    "fieldType == %s".formatted(SELECT.getValue())),
                            integer(MIN_SELECTION)
                                .label("Min selection")
                                .description("Minimum selections required.")
                                .required(false)
                                .displayCondition(
                                    "fieldType == %s and multipleChoice == true".formatted(SELECT.getValue())),
                            integer(MAX_SELECTION)
                                .label("Max selection")
                                .description("Maximum selections allowed.")
                                .required(false)
                                .displayCondition(
                                    "fieldType == %s and multipleChoice == true".formatted(SELECT.getValue())),
                            bool(REQUIRED)
                                .label("Required")
                                .description("Whether this field is required")
                                .defaultValue(false)
                                .required(false)))
                .required(true))
        .output(NewFormRequestTrigger::getOutput)
        .webhookRequest(NewFormRequestTrigger::getWebhookResult);

    private static final List<String> BOT_KEYWORDS = List.of(
        "bot", "spider", "crawler", "preview", "facebookexternalhit", "slackbot", "twitterbot", "whatsapp");

    protected static OutputResponse getOutput(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Map<String, ?> input : inputs) {
            String fieldName = (String) input.get(FIELD_NAME);

            if (fieldName == null) {
                continue;
            }

            Integer fieldTypeInt = (Integer) input.get(FIELD_TYPE);

            FieldType fieldType = fieldTypeInt != null ? fromValue(fieldTypeInt) : INPUT;

            ModifiableValueProperty<?, ?> property = switch (fieldType) {
                case CHECKBOX -> bool(fieldName);
                case DATE_PICKER -> date(fieldName);
                case DATETIME_PICKER -> dateTime(fieldName);
                case FILE_INPUT -> fileEntry(fieldName);
                case NUMBER_INPUT -> number(fieldName);
                case SELECT -> {
                    Boolean multipleChoice = (Boolean) input.get(MULTIPLE_CHOICE);

                    yield Boolean.TRUE.equals(multipleChoice)
                        ? array(fieldName).items(string()) : string(fieldName);
                }
                default -> string(fieldName);
            };

            properties.add(property);
        }

        return OutputResponse.of(
            object()
                .properties(
                    string("submittedAt"),
                    object("body")
                        .properties(properties.toArray(new ModifiableValueProperty[0]))));
    }

    protected static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        Objects.requireNonNull(body.getContent(), "Body content is required.");

        boolean ignoreBots = inputParameters.getBoolean(IGNORE_BOTS, false);

        if (ignoreBots) {
            String userAgent = headers.firstValue("User-Agent")
                .orElse("")
                .toLowerCase();

            Stream<String> stream = BOT_KEYWORDS.stream();

            if (stream.anyMatch(userAgent::contains)) {
                return Map.of();
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> content = new HashMap<>((Map<String, Object>) body.getContent());

        boolean useWorkflowTimezone = inputParameters.getBoolean(USE_WORKFLOW_TIMEZONE, false);

        if (!useWorkflowTimezone) {
            content.put("submittedAt", String.valueOf(Instant.now(Clock.systemUTC())));
        }

        return content;
    }
}
