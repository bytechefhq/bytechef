
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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class NotesActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("addNote")
        .display(
            display("Add a note")
                .description("Adds a new note."))
        .metadata(
            Map.of(
                "requestMethod", "POST",
                "path", "/notes", "bodyContentType", "JSON"

            ))
        .properties(object(null).properties(number("pinned_to_organization_flag").label("Pinned_to_organization_flag")
            .description(
                "If set, the results are filtered by note to organization pinning state (`org_id` is also required)")
            .options(option("0", 0), option("1", 1))
            .required(false),
            integer("user_id").label("User_id")
                .description(
                    "The ID of the user who will be marked as the author of the note. Only an admin can change the author.")
                .required(false),
            integer("org_id").label("Org_id")
                .description(
                    "The ID of the organization this note will be attached to. This property is required unless one of (`deal_id/lead_id/person_id`) is specified.")
                .required(false),
            number("pinned_to_lead_flag").label("Pinned_to_lead_flag")
                .description(
                    "If set, the results are filtered by note to lead pinning state (`lead_id` is also required)")
                .options(option("0", 0), option("1", 1))
                .required(false),
            integer("deal_id").label("Deal_id")
                .description(
                    "The ID of the deal the note will be attached to. This property is required unless one of (`lead_id/person_id/org_id`) is specified.")
                .required(false),
            string("add_time").label("Add_time")
                .description(
                    "The optional creation date & time of the note in UTC. Can be set in the past or in the future. Requires admin user API token. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            number("pinned_to_person_flag").label("Pinned_to_person_flag")
                .description(
                    "If set, the results are filtered by note to person pinning state (`person_id` is also required)")
                .options(option("0", 0), option("1", 1))
                .required(false),
            string("content").label("Content")
                .description("The content of the note in HTML format. Subject to sanitization on the back-end.")
                .required(false),
            string("lead_id").label("Lead_id")
                .description(
                    "The ID of the lead the note will be attached to. This property is required unless one of (`deal_id/person_id/org_id`) is specified.")
                .required(false),
            number("pinned_to_deal_flag").label("Pinned_to_deal_flag")
                .description(
                    "If set, the results are filtered by note to deal pinning state (`deal_id` is also required)")
                .options(option("0", 0), option("1", 1))
                .required(false),
            integer("person_id").label("Person_id")
                .description(
                    "The ID of the person this note will be attached to. This property is required unless one of (`deal_id/lead_id/org_id`) is specified.")
                .required(false))
            .metadata(
                Map.of(
                    "type", "BODY")))
        .output(object(null).properties(bool("success").label("Success")
            .description("If the request was successful or not")
            .required(false),
            object("data").properties(integer("id").label("Id")
                .description("The ID of the note")
                .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the note is active or deleted")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the note")
                    .required(false),
                string("content").label("Content")
                    .description("The content of the note in HTML format. Subject to sanitization on the back-end.")
                    .required(false),
                object("deal").properties(string("title").label("Title")
                    .description("The title of the deal this note is attached to")
                    .required(false))
                    .label("Deal")
                    .description("The deal this note is attached to")
                    .required(false),
                string("lead_id").label("Lead_id")
                    .description("The ID of the lead the note is attached to")
                    .required(false),
                integer("deal_id").label("Deal_id")
                    .description("The ID of the deal the note is attached to")
                    .required(false),
                integer("last_update_user_id").label("Last_update_user_id")
                    .description("The ID of the user who last updated the note")
                    .required(false),
                integer("org_id").label("Org_id")
                    .description("The ID of the organization the note is attached to")
                    .required(false),
                object("organization").properties(string("name").label("Name")
                    .description("The name of the organization the note is attached to")
                    .required(false))
                    .label("Organization")
                    .description("The organization the note is attached to")
                    .required(false),
                object("person").properties(string("name").label("Name")
                    .description("The name of the person the note is attached to")
                    .required(false))
                    .label("Person")
                    .description("The person the note is attached to")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of the person the note is attached to")
                    .required(false),
                bool("pinned_to_deal_flag").label("Pinned_to_deal_flag")
                    .description("If true, the results are filtered by note to deal pinning state")
                    .required(false),
                bool("pinned_to_organization_flag").label("Pinned_to_organization_flag")
                    .description("If true, the results are filtered by note to organization pinning state")
                    .required(false),
                bool("pinned_to_person_flag").label("Pinned_to_person_flag")
                    .description("If true, the results are filtered by note to person pinning state")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last updated date and time of the note")
                    .required(false),
                object("user").properties(string("email").label("Email")
                    .description("The email of the note creator")
                    .required(false),
                    string("icon_url").label("Icon_url")
                        .description("The URL of the note creator avatar picture")
                        .required(false),
                    bool("is_you").label("Is_you")
                        .description("Whether the note is created by you or not")
                        .required(false),
                    string("name").label("Name")
                        .description("The name of the note creator")
                        .required(false))
                    .label("User")
                    .description("The user who created the note")
                    .required(false),
                integer("user_id").label("User_id")
                    .description("The ID of the note creator")
                    .required(false))
                .label("Data")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", "JSON")))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"id\":1,\"active_flag\":true,\"add_time\":\"2019-12-09 13:59:21\",\"content\":\"abc\",\"deal\":{\"title\":\"Deal title\"},\"lead_id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"deal_id\":1,\"last_update_user_id\":1,\"org_id\":1,\"organization\":{\"name\":\"Organization name\"},\"person\":{\"name\":\"Person name\"},\"person_id\":1,\"pinned_to_lead_flag\":false,\"pinned_to_deal_flag\":true,\"pinned_to_organization_flag\":false,\"pinned_to_person_flag\":false,\"update_time\":\"2019-12-09 14:26:11\",\"user\":{\"email\":\"user@email.com\",\"icon_url\":\"https://iconurl.net/profile_120x120_123.jpg\",\"is_you\":true,\"name\":\"User Name\"},\"user_id\":1}}"));
}
