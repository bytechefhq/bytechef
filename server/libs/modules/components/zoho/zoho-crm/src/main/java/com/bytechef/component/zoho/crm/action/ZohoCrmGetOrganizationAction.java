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

package com.bytechef.component.zoho.crm.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 */
public class ZohoCrmGetOrganizationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getOrganization")
        .title("Get Organization")
        .description("Gets information about the current organization.")
        .output(
            outputSchema(
                object()
                    .properties(
                        array("org")
                            .items(
                                object()
                                    .properties(
                                        string("country")
                                            .description("Country of the organization."),
                                        string("city")
                                            .description("City of the organization."),
                                        string("street")
                                            .description("Street address of the organization."),
                                        string("zip")
                                            .description("ZIP code of the organization."),
                                        string("photo_id")
                                            .description("ID of the organization photo file."),
                                        string("description")
                                            .description("Description of the organization."),
                                        string("alias")
                                            .description("Alias name of the organization."),
                                        string("created_time")
                                            .description("Date and time when the organization was created."),
                                        string("type")
                                            .description(
                                                "Represents the environment type of the org. " +
                                                    "The possible values are production, sandbox, bigin and developer."),
                                        string("currency")
                                            .description("Base/home currency details of the organization."),
                                        string("id")
                                            .description("ID of the organization."),
                                        string("phone")
                                            .description("Phone number of the organization."),
                                        string("company_name")
                                            .description("Name of the company in the organization"),
                                        string("primary_email")
                                            .description("Primary email address of the organization."),
                                        string("website")
                                            .description("Website of the organization."))))))
        .perform(ZohoCrmGetOrganizationAction::perform);

    private ZohoCrmGetOrganizationAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/org"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
