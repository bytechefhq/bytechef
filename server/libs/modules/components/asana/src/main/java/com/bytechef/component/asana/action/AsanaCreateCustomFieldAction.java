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

package com.bytechef.component.asana.action;

import static com.bytechef.component.asana.constant.AsanaConstants.COLOR;
import static com.bytechef.component.asana.constant.AsanaConstants.CURRENCY_CODE;
import static com.bytechef.component.asana.constant.AsanaConstants.DATE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.ENABLED;
import static com.bytechef.component.asana.constant.AsanaConstants.ENUM_OPTIONS;
import static com.bytechef.component.asana.constant.AsanaConstants.FORMAT;
import static com.bytechef.component.asana.constant.AsanaConstants.INPUT_RESTRICTIONS;
import static com.bytechef.component.asana.constant.AsanaConstants.NAME;
import static com.bytechef.component.asana.constant.AsanaConstants.NUMBER_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.PEOPLE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.PRECISION;
import static com.bytechef.component.asana.constant.AsanaConstants.REFERENCE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.RESOURCE_SUBTYPE;
import static com.bytechef.component.asana.constant.AsanaConstants.TEXT_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class AsanaCreateCustomFieldAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCustomField")
        .title("Create Custom Field")
        .description("Creates a new custom field in a workspace.")
        .properties(
            string(WORKSPACE)
                .label("Workspace")
                .description("The workspace to create a custom field in.")
                .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getWorkspaceOptions)
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name of the custom field.")
                .required(true),
            string(RESOURCE_SUBTYPE)
                .label("Resource Subtype")
                .description("The type of the custom field.")
                .required(true)
                .options(List.of(
                    option("text", "text"),
                    option("enum", "enum"),
                    option("multi_enum", "multi_enum"),
                    option("number", "number"),
                    option("date", "date"),
                    option("people", "people"),
                    option("reference", "reference"))),
            string(TEXT_VALUE)
                .label("Text Value")
                .description("The text value of the custom field.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "text")),
            array(ENUM_OPTIONS)
                .label("Enum Options")
                .description("This array specifies the possible values for an enum custom field.")
                .displayCondition("%s == 'multi_enum' || %s == 'enum'".formatted(RESOURCE_SUBTYPE, RESOURCE_SUBTYPE))
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Name")
                                .description("The name of the enum option.")
                                .required(true),
                            bool(ENABLED)
                                .label("Enabled")
                                .description("Whether or not the enum option is selectable."),
                            string(COLOR)
                                .label("Color")
                                .description("The color of the enum option. Defaults to none."))),
            number(NUMBER_VALUE)
                .label("Number Value")
                .description("This number is the value of a number custom field.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "number")),
            integer(PRECISION)
                .label("Precision")
                .description("This field dictates the number of places after the decimal to round to.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "number")),
            object(DATE_VALUE)
                .label("Date Value")
                .description(
                    "This object reflects the chosen date (and optionally, time) value of a date custom field.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "date"))
                .properties(
                    date("date")
                        .label("Date")
                        .description("The date on which this task is due."),
                    dateTime("date_time")
                        .label("Date Time")
                        .description("The date and time on which this task is due.")),
            array(PEOPLE_VALUE)
                .label("People Value")
                .description("This array of compact user objects reflects the values of a people custom field.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "people"))
                .items(string()),
            array(REFERENCE_VALUE)
                .label("Reference Value")
                .description("This array of objects reflects the values of a reference custom field.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "reference"))
                .items(string()),
            array(INPUT_RESTRICTIONS)
                .label("Input Restrictions")
                .description(
                    "This array of strings reflects the allowed types of objects that can be written to a reference custom field value.")
                .displayCondition("%s == '%s'".formatted(RESOURCE_SUBTYPE, "reference"))
                .items(string()),
            string(FORMAT)
                .label("Format")
                .description("The format of this custom field.")
                .options(
                    option("currency", "currency"),
                    option("identifier", "identifier"),
                    option("percentage", "percentage"),
                    option("custom", "custom"),
                    option("duration", "duration"),
                    option("none", "none")),
            string(CURRENCY_CODE)
                .label("Currency Code")
                .description("ISO 4217 currency code to format this custom field.")
                .displayCondition("%s == '%s'".formatted(FORMAT, "currency")))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                string("gid")
                                    .description("Globally unique identifier for the custom field.")
                                    .required(false),
                                string(NAME)
                                    .description("Name of the custom field.")
                                    .required(false),
                                string("resource_type")
                                    .description("Resource type, typically 'custom_field'.")
                                    .required(false),
                                string(RESOURCE_SUBTYPE)
                                    .description("Subtype of the custom field (text, number, enum, etc.).")
                                    .required(false),
                                array(ENUM_OPTIONS)
                                    .description(
                                        "List of enum options if the custom field is of type enum or multi_enum.")
                                    .items(
                                        object()
                                            .properties(
                                                string("gid")
                                                    .description("Globally unique identifier for the enum option.")
                                                    .required(false),
                                                string(NAME)
                                                    .description("Name of the enum option.")
                                                    .required(false),
                                                bool(ENABLED)
                                                    .description("Whether this enum option is enabled.")
                                                    .required(false),
                                                string(COLOR)
                                                    .description("Color of the enum option.")
                                                    .required(false)))
                                    .required(false),
                                number(NUMBER_VALUE)
                                    .description("Value of the custom field if type is number.")
                                    .required(false),
                                string(TEXT_VALUE)
                                    .description("Value of the custom field if type is text.")
                                    .required(false),
                                object(DATE_VALUE)
                                    .description("Value of the custom field if type is date.")
                                    .properties(
                                        date("date")
                                            .description("The date value of the custom field.")
                                            .required(false),
                                        dateTime("date_time")
                                            .description("The date-time value of the custom field.")
                                            .required(false))
                                    .required(false),
                                array(PEOPLE_VALUE)
                                    .description("List of user objects if the custom field is of type people.")
                                    .items(
                                        object()
                                            .properties(
                                                string("gid")
                                                    .description("Globally unique identifier for the user.")
                                                    .required(false),
                                                string(NAME)
                                                    .description("Name of the user.")
                                                    .required(false)))
                                    .required(false),
                                array(REFERENCE_VALUE)
                                    .description("List of reference objects if the custom field is of type reference.")
                                    .items(
                                        object()
                                            .properties(
                                                string("gid")
                                                    .description("Globally unique identifier for the reference object.")
                                                    .required(false),
                                                string(NAME)
                                                    .description("Name of the reference object.")
                                                    .required(false)))
                                    .required(false),
                                string(FORMAT)
                                    .description("Format of the custom field, e.g., currency, custom.")
                                    .required(false),
                                string(CURRENCY_CODE)
                                    .description("Currency code if the custom field format is 'currency'.")
                                    .required(false))
                            .required(false))))
        .perform(AsanaCreateCustomFieldAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> customFieldData = new HashMap<>();

        customFieldData.put(WORKSPACE, inputParameters.getRequiredString(WORKSPACE));
        customFieldData.put(NAME, inputParameters.getRequiredString(NAME));

        putIfNotNull(customFieldData, RESOURCE_SUBTYPE, inputParameters.getString(RESOURCE_SUBTYPE));
        putIfNotNull(customFieldData, TEXT_VALUE, inputParameters.getString(TEXT_VALUE));
        putIfNotNull(customFieldData, ENUM_OPTIONS, inputParameters.get(ENUM_OPTIONS));
        putIfNotNull(customFieldData, NUMBER_VALUE, inputParameters.get(NUMBER_VALUE));
        putIfNotNull(customFieldData, PRECISION, inputParameters.getInteger(PRECISION));
        putIfNotNull(customFieldData, DATE_VALUE, inputParameters.get(DATE_VALUE));
        putIfNotNull(customFieldData, PEOPLE_VALUE, inputParameters.get(PEOPLE_VALUE));
        putIfNotNull(customFieldData, REFERENCE_VALUE, inputParameters.get(REFERENCE_VALUE));
        putIfNotNull(customFieldData, INPUT_RESTRICTIONS, inputParameters.get(INPUT_RESTRICTIONS));
        putIfNotNull(customFieldData, FORMAT, inputParameters.getString(FORMAT));
        putIfNotNull(customFieldData, CURRENCY_CODE, inputParameters.getString(CURRENCY_CODE));

        Map<String, Object> body = Map.of("data", customFieldData);

        return context.http(
            http -> http.post("/custom_fields")
                .body(
                    Http.Body.of(body)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static void putIfNotNull(Map<String, Object> customFieldData, String key, Object value) {
        if (value != null) {
            customFieldData.put(key, value);
        }
    }
}
