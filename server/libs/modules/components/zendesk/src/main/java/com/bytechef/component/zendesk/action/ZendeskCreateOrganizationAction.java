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

package com.bytechef.component.zendesk.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.DETAILS;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.DOMAIN_NAMES;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NAME;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NOTES;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.ORGANIZATION;
import static com.bytechef.component.zendesk.util.ZendeskUtils.checkIfNull;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskCreateOrganizationAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrganization")
        .title("Create Organization")
        .description("Creates an organization.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the organization.")
                .required(true),
            string(DETAILS)
                .label("Details")
                .description("Any details about the organization, such as the address.")
                .required(false),
            array(DOMAIN_NAMES)
                .label("Domain Names")
                .description("An array of domain names associated with this organization.")
                .required(true)
                .items(
                    string("domain_name")
                        .label("Domain Name")
                        .description("Domain name associated with this organization.")
                        .required(false)),
            string(NOTES)
                .label("Notes")
                .description("Any notes you have about the organization.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url")
                            .description("URL of the created organization."),
                        string("id")
                            .description("Organization ID."),
                        string("name")
                            .description("Organization name."),
                        bool("shared_tickets")
                            .description("Whether the organization can share tickets."),
                        bool("shared_comments")
                            .description("Whether the organization can share comments."),
                        integer("external_id")
                            .description("External ID of the organization."),
                        string("created_at")
                            .description("Date when the organization was created."),
                        string("updated_at")
                            .description("Date when the organization was last updated."),
                        array("domain_names")
                            .description("Array of domain names of the organization.")
                            .items(
                                string("domain_name")),
                        string("detail")
                            .description("Details about the organization."),
                        string("notes")
                            .description("Notes about the organization."),
                        integer("group_id")
                            .description("Group ID of the organization."),
                        array("tags")
                            .description("Tags of the organization.")
                            .items(
                                string("tag")),
                        object("organization_fields")
                            .description("Custom organization fields of the organization."))))
        .perform(ZendeskCreateOrganizationAction::perform);

    private ZendeskCreateOrganizationAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/organizations"))
            .body(
                Body.of(
                    ORGANIZATION, Map.of(
                        NAME, inputParameters.getRequiredString(NAME),
                        DOMAIN_NAMES, inputParameters.getRequiredList(DOMAIN_NAMES),
                        DETAILS, checkIfNull(inputParameters.getString(DETAILS)),
                        NOTES, checkIfNull(inputParameters.getString(NOTES)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
