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

package com.bytechef.component.keap.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class KeapCreateCompanyAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create a Company")
        .description("Creates a new company")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/v1/companies", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item")
            .properties(object("address")
                .properties(object("address").properties(string("country_code").label("Country Code")
                    .required(false),
                    string("line1").label("Line 1")
                        .required(false),
                    string("line2").label("Line 2")
                        .required(false),
                    string("locality").label("Locality")
                        .required(false),
                    string("region").label("Region")
                        .required(false),
                    string("zip_code").label("Zip Code")
                        .required(false),
                    string("zip_four").label("Zip Four")
                        .required(false))
                    .label("Company Address")
                    .required(false),
                    string("company_name").label("Company Name")
                        .required(false),
                    array("custom_fields").items(object().properties(object("content").label("Content")
                        .required(false),
                        integer("id").label("Id")
                            .required(false)))
                        .placeholder("Add to Custom Fields")
                        .label("Custom Fields")
                        .required(false),
                    string("email_address").label("Email Address")
                        .required(false),
                    object("fax_number").properties(string("number").label("Number")
                        .required(false),
                        string("type").label("Type")
                            .required(false))
                        .label("Company Fax Number")
                        .required(false),
                    string("notes").label("Notes")
                        .required(false),
                    string("opt_in_reason").label("Opt In Reason")
                        .required(false),
                    object("phone_number").properties(string("extension").label("Extension")
                        .required(false),
                        string("number").label("Number")
                            .required(false),
                        string("type").label("Type")
                            .required(false))
                        .label("Company Phone Number")
                        .required(false),
                    string("website").label("Website")
                        .required(false))
                .label("Company Address")
                .required(false))
            .label("Item")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(
                object("address")
                    .properties(string("country_code").required(false), string("line1").required(false),
                        string("line2").required(false), string("locality").required(false),
                        string("region").required(false), string("zip_code").required(false),
                        string("zip_four").required(false))
                    .required(false),
                string("company_name").required(false),
                array("custom_fields")
                    .items(object().properties(object("content").required(false), integer("id").required(false)))
                    .required(false),
                string("email_address").required(false), bool("email_opted_in").required(false),
                string("email_status")
                    .options(option("UnengagedMarketable", "UnengagedMarketable"), option("SingleOptIn", "SingleOptIn"),
                        option("DoubleOptin", "DoubleOptin"), option("Confirmed", "Confirmed"),
                        option("UnengagedNonMarketable", "UnengagedNonMarketable"),
                        option("NonMarketable", "NonMarketable"), option("Lockdown", "Lockdown"),
                        option("Bounce", "Bounce"), option("HardBounce", "HardBounce"), option("Manual", "Manual"),
                        option("Admin", "Admin"), option("System", "System"),
                        option("ListUnsubscribe", "ListUnsubscribe"), option("Feedback", "Feedback"),
                        option("Spam", "Spam"), option("Invalid", "Invalid"), option("Deactivated", "Deactivated"))
                    .required(false),
                object("fax_number").properties(string("number").required(false), string("type").required(false))
                    .required(false),
                integer("id").required(false), string("notes").required(false),
                object("phone_number")
                    .properties(string("extension").required(false), string("number").required(false),
                        string("type").required(false))
                    .required(false),
                string("website").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
