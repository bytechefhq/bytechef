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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
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
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AsanaCreateCustomFieldAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createCustomField")
        .title("Create Custom Field")
        .description("Creates a custom field for a task.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/custom_fields", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("data").properties(string("workspace").label("Workspace")
            .description("The workspace to create a custom field in.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getWorkspaceOptions),
            string("name").label("Name")
                .description("The name of the custom field.")
                .required(true),
            string("description").label("Description")
                .description("The description of the custom field.")
                .required(false),
            string("resource_subtype").label("Resource Subtype")
                .description("The type of the custom field.")
                .options(option("Text", "text"), option("Enum", "enum"), option("Multi_enum", "multi_enum"),
                    option("Date", "date"), option("Number", "number"), option("People", "people"),
                    option("Reference", "reference"))
                .required(true),
            string("text_value").label("Text Value")
                .description("The value of the text custom field.")
                .required(false),
            array("enum_options").items(object().properties(string("name").label("Name")
                .description("The name of the enum option.")
                .required(false),
                bool("enabled").label("Enabled")
                    .description("Whether or not the enum option is a selectable value for the custom field.")
                    .required(false),
                string("color").label("Color")
                    .description("The color of the enum option.")
                    .required(false)))
                .placeholder("Add to Enum Options")
                .label("Enum Options")
                .required(false),
            number("number_value").label("Number Value")
                .description("The value of the number custom field.")
                .required(false),
            integer("precision").label("Precision")
                .description("This field dictates the number of places after the decimal to round to.")
                .required(false),
            object("date_value").properties(date("date").label("Date")
                .required(false),
                dateTime("date_time").label("Date Time")
                    .required(false))
                .label("Date Value")
                .description("The value of the date custom field.")
                .required(false),
            array("people_value").items(string().description("The value of the people custom field."))
                .placeholder("Add to People Value")
                .label("People Value")
                .description("The value of the people custom field.")
                .required(false),
            array("reference_value").items(string().description("The value of the reference custom field."))
                .placeholder("Add to Reference Value")
                .label("Reference Value")
                .description("The value of the reference custom field.")
                .required(false),
            string("format").label("Format")
                .description("The format of this custom field.")
                .options(option("Currency", "currency"), option("Identifier", "identifier"),
                    option("Percentage", "percentage"), option("Custom", "custom"), option("Duration", "duration"),
                    option("None", "none"))
                .required(false),
            string("currency_code").label("Currency Code")
                .description("ISO 4217 currency code to format this custom field.")
                .required(false))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Data")
            .required(false))
        .output(
            outputSchema(
                object()
                    .properties(object("data")
                        .properties(string("gid").description("Globally unique identifier for the task.")
                            .required(false),
                            string("resource_type").description("Type of the resource.")
                                .required(false),
                            string("name").description("Name of the custom field.")
                                .required(false),
                            string("type").description("The type of custom field.")
                                .required(false),
                            array("enum_options")
                                .items(
                                    object()
                                        .properties(
                                            string("gid").description("Globally unique identifier for the enum option.")
                                                .required(false),
                                            string("resource_type").description("Type of resource.")
                                                .required(false),
                                            string("name").description("Name of the enum option.")
                                                .required(false),
                                            bool("enabled").description("Whether the enum option is enabled.")
                                                .required(false),
                                            string("color").description("Color associated with the enum option.")
                                                .required(false))
                                        .description("Enumeration options available for the custom field."))
                                .description("Enumeration options available for the custom field.")
                                .required(false),
                            string("input_restrictions").description("Restrictions on where the field can be used.")
                                .required(false),
                            object("date_value").properties(date("date").description("Date portion of the value.")
                                .required(false),
                                dateTime("date_time").description("Date-time value of the field.")
                                    .required(false))
                                .description("Date value stored in the field.")
                                .required(false),
                            object(
                                "enum_value")
                                    .properties(
                                        string("gid").description("Globally unique identifier for the enum option.")
                                            .required(false),
                                        string("resource_type").description("Type of resource.")
                                            .required(false),
                                        string("name").description("Name of the enum option.")
                                            .required(false),
                                        bool("enabled").description("Whether the option is enabled.")
                                            .required(false),
                                        string("color").description("Color of the enum option.")
                                            .required(false))
                                    .description("Currently selected enum value.")
                                    .required(false),
                            array("multi_enum_values")
                                .items(
                                    object()
                                        .properties(
                                            string("gid").description("Globally unique identifier for the enum option.")
                                                .required(false),
                                            string("resource_type").description("Type of resource.")
                                                .required(false),
                                            string("name").description("Name of the enum option.")
                                                .required(false),
                                            bool("enabled").description("Whether the option is enabled.")
                                                .required(false),
                                            string("color").description("Color of the enum option.")
                                                .required(false))
                                        .description("Multiple enum values selected."))
                                .description("Multiple enum values selected.")
                                .required(false),
                            number("number_value").description("Numeric value stored in the field.")
                                .required(false),
                            string("text_value").description("Text value stored in the field.")
                                .required(false),
                            integer("precision").description("Number of decimal places allowed.")
                                .required(false),
                            string("format").description("Formatting style of the field.")
                                .required(false),
                            string("currency_code").description("Currency code used if the field represents money.")
                                .required(false),
                            array("people_value")
                                .items(
                                    object()
                                        .properties(
                                            string("gid").description("Globally unique identifier for the user.")
                                                .required(false),
                                            string("resource_type").description("Type of resource.")
                                                .required(false),
                                            string("name").description("Name of the user.")
                                                .required(false))
                                        .description("Users referenced by the custom field."))
                                .description("Users referenced by the custom field.")
                                .required(false),
                            array("reference_value")
                                .items(object().properties(string("gid").description("Globally unique identifier.")
                                    .required(false),
                                    string("resource_type").description("Type of referenced resource.")
                                        .required(false),
                                    string("name").description("Name of the referenced resource.")
                                        .required(false))
                                    .description("Referenced resources such as tasks."))
                                .description("Referenced resources such as tasks.")
                                .required(false),
                            string("resource_subtype").description("Subtype of the resource.")
                                .required(false))
                        .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/asana_v1#create-custom-field");

    private AsanaCreateCustomFieldAction() {
    }
}
