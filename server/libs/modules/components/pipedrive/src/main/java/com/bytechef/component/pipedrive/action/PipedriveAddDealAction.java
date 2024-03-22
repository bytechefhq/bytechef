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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddDealAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addDeal")
        .title("Add a deal")
        .description(
            "Adds a new deal. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the dealFields and look for `key` values. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/creating-a-deal\" target=\"_blank\" rel=\"noopener noreferrer\">adding a deal</a>.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/deals", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(date("expected_close_date").label("Expected Close Date")
            .description("The expected close date of the deal. In ISO 8601 format: YYYY-MM-DD.")
            .required(false),
            integer("stage_id").label("Stage Id")
                .description(
                    "The ID of the stage this deal will be added to. Please note that a pipeline will be assigned automatically based on the `stage_id`. If omitted, the deal will be placed in the first stage of the default pipeline.")
                .required(false),
            number("probability").label("Probability")
                .description(
                    "The success probability percentage of the deal. Used/shown only when `deal_probability` for the pipeline of the deal is enabled.")
                .required(false),
            string("title").label("Title")
                .description("The title of the deal")
                .required(false),
            string("lost_reason").label("Lost Reason")
                .description("The optional message about why the deal was lost (to be used when status = lost)")
                .required(false),
            integer("user_id").label("User Id")
                .description(
                    "The ID of the user which will be the owner of the created deal. If not provided, the user making the request will be used.")
                .required(false),
            string("visible_to").label("Visible To")
                .description(
                    "The visibility of the deal. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                .required(false),
            integer("org_id").label("Org Id")
                .description(
                    "The ID of an organization which this deal will be linked to. If the organization does not exist yet, it needs to be created first. This property is required unless `person_id` is specified.")
                .required(false),
            integer("pipeline_id").label("Pipeline Id")
                .description(
                    "The ID of the pipeline this deal will be added to. By default, the deal will be added to the first stage of the specified pipeline. Please note that `pipeline_id` and `stage_id` should not be used together as `pipeline_id` will be ignored.")
                .required(false),
            string("currency").label("Currency")
                .description(
                    "The currency of the deal. Accepts a 3-character currency code. If omitted, currency will be set to the default currency of the authorized user.")
                .required(false),
            string("value").label("Value")
                .description("The value of the deal. If omitted, value will be set to 0.")
                .required(false),
            string("add_time").label("Add Time")
                .description(
                    "The optional creation date & time of the deal in UTC. Requires admin user API token. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            integer("person_id").label("Person Id")
                .description(
                    "The ID of a person which this deal will be linked to. If the person does not exist yet, it needs to be created first. This property is required unless `org_id` is specified.")
                .required(false),
            string("status").label("Status")
                .description(
                    "open = Open, won = Won, lost = Lost, deleted = Deleted. If omitted, status will be set to open.")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                    option("Deleted", "deleted"))
                .required(false))
            .label("Add Deal Request")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(bool("success").description("If the response is successful or not")
            .required(false),
            integer("email_messages_count").description("The number of emails associated with the deal")
                .required(false),
            string("cc_email").description("The BCC email of the deal")
                .required(false),
            integer("products_count").description("The number of products associated with the deal")
                .required(false),
            string("next_activity_date").description("The date of the next activity associated with the deal")
                .required(false),
            string("next_activity_type").description("The type of the next activity associated with the deal")
                .required(false),
            string("next_activity_duration").description("The duration of the next activity associated with the deal")
                .required(false),
            integer("id").description("The ID of the deal")
                .required(false),
            string("name").description("The name of the person associated with the deal")
                .required(false),
            bool("active_flag").description("Whether the associated person is active or not")
                .required(false),
            array("phone").items(object().properties(string("label").description("The type of the phone number")
                .required(false),
                string("value").description("The phone number of the person associated with the deal")
                    .required(false),
                bool("primary").description("If this is the primary phone number or not")
                    .required(false))
                .description("The phone numbers of the person associated with the deal"))
                .description("The phone numbers of the person associated with the deal")
                .required(false),
            integer("value").description("The ID of the person associated with the deal")
                .required(false),
            array("email").items(object().properties(string("label").description("The type of the email")
                .required(false),
                string("value").description("The email of the associated person")
                    .required(false),
                bool("primary").description("If this is the primary email or not")
                    .required(false))
                .description("The emails of the person associated with the deal"))
                .description("The emails of the person associated with the deal")
                .required(false),
            integer("owner_id").description("The ID of the owner of the person that is associated with the deal")
                .required(false),
            object("creator_user_id").properties(integer("id").description("The ID of the deal creator")
                .required(false),
                string("name").description("The name of the deal creator")
                    .required(false),
                string("email").description("The email of the deal creator")
                    .required(false),
                bool("has_pic").description("If the creator has a picture or not")
                    .required(false),
                string("pic_hash").description("The creator picture hash")
                    .required(false),
                bool("active_flag").description("Whether the creator is active or not")
                    .required(false),
                integer("value").description("The ID of the deal creator")
                    .required(false))
                .description("The creator of the deal")
                .required(false),
            date("expected_close_date").description("The expected close date of the deal")
                .required(false),
            integer("participants_count").description("The number of participants associated with the deal")
                .required(false),
            string("owner_name").description("The name of the deal owner")
                .required(false),
            integer("stage_id").description("The ID of the deal stage")
                .required(false),
            number("probability").description("The success probability percentage of the deal")
                .required(false),
            integer("undone_activities_count")
                .description("The number of incomplete activities associated with the deal")
                .required(false),
            bool("active").description("Whether the deal is active or not")
                .required(false),
            string("last_activity_date").description("The date of the last activity associated with the deal")
                .required(false),
            string("person_name").description("The name of the person associated with the deal")
                .required(false),
            string("close_time").description("The date and time of closing the deal")
                .required(false),
            integer("next_activity_id").description("The ID of the next activity associated with the deal")
                .required(false),
            string("weighted_value_currency").description("The currency associated with the deal")
                .required(false),
            bool("org_hidden").description("If the organization that is associated with the deal is hidden or not")
                .required(false),
            integer("stage_order_nr").description("The order number of the deal stage associated with the deal")
                .required(false),
            string("next_activity_subject").description("The subject of the next activity associated with the deal")
                .required(false),
            string("rotten_time").description("The date and time of changing the deal status as rotten")
                .required(false),
            string("name").description("The name of the user")
                .required(false),
            bool("has_pic").description("If the user has a picture or not")
                .required(false),
            bool("active_flag").description("Whether the user is active or not")
                .required(false),
            integer("id").description("The ID of the user")
                .required(false),
            integer("value").description("The ID of the user")
                .required(false),
            string("email").description("The email of the user")
                .required(false),
            string("pic_hash").description("The user picture hash")
                .required(false),
            string("visible_to").description("The visibility of the deal")
                .required(false),
            string("address").description("The address of the organization that is associated with the deal")
                .required(false),
            integer("owner_id").description("The ID of the owner of the organization that is associated with the deal")
                .required(false),
            string("cc_email").description("The BCC email of the organization associated with the deal")
                .required(false),
            string("name").description("The name of the organization associated with the deal")
                .required(false),
            bool("active_flag").description("Whether the associated organization is active or not")
                .required(false),
            integer("people_count")
                .description("The number of people connected with the organization that is associated with the deal")
                .required(false),
            integer("value").description("The ID of the organization associated with the deal")
                .required(false),
            integer("notes_count").description("The number of notes associated with the deal")
                .required(false),
            string("next_activity_time").description("The time of the next activity associated with the deal")
                .required(false),
            string("formatted_value").description("The deal value formatted with selected currency. E.g. US$500")
                .required(false),
            string("status").description("The status of the deal")
                .required(false),
            string("formatted_weighted_value")
                .description("The weighted_value formatted with selected currency. E.g. US$500")
                .required(false),
            string("first_won_time").description("The date and time of the first time changing the deal status as won")
                .required(false),
            string("last_outgoing_mail_time")
                .description("The date and time of the last outgoing email associated with the deal")
                .required(false),
            string("title").description("The title of the deal")
                .required(false),
            integer("last_activity_id").description("The ID of the last activity associated with the deal")
                .required(false),
            string("update_time").description("The last updated date and time of the deal")
                .required(false),
            integer("activities_count").description("The number of activities associated with the deal")
                .required(false),
            integer("pipeline_id").description("The ID of pipeline associated with the deal")
                .required(false),
            string("lost_time").description("The date and time of changing the deal status as lost")
                .required(false),
            string("currency").description("The currency associated with the deal")
                .required(false),
            number("weighted_value").description(
                "Probability times deal value. Probability can either be deal probability or if not set, then stage probability.")
                .required(false),
            string("org_name").description("The name of the organization associated with the deal")
                .required(false),
            number("value").description("The value of the deal")
                .required(false),
            string("next_activity_note").description("The note of the next activity associated with the deal")
                .required(false),
            bool("person_hidden").description("If the person that is associated with the deal is hidden or not")
                .required(false),
            integer("files_count").description("The number of files associated with the deal")
                .required(false),
            string("last_incoming_mail_time")
                .description("The date and time of the last incoming email associated with the deal")
                .required(false),
            integer("label").description("The label assigned to the deal")
                .required(false),
            string("lost_reason").description("The reason for losing the deal")
                .required(false),
            bool("deleted").description("Whether the deal is deleted or not")
                .required(false),
            string("won_time").description("The date and time of changing the deal status as won")
                .required(false),
            integer("followers_count").description("The number of followers associated with the deal")
                .required(false),
            string("stage_change_time").description("The last updated date and time of the deal stage")
                .required(false),
            string("add_time").description("The creation date and time of the deal")
                .required(false),
            integer("done_activities_count").description("The number of completed activities associated with the deal")
                .required(false),
            object("related_objects")
                .properties(
                    object(
                        "user").additionalProperties(
                            object().properties(integer("id").description("The ID of the user")
                                .required(false),
                                string("name").description("The name of the user")
                                    .required(false),
                                string("email").description("The email of the user")
                                    .required(false),
                                integer(
                                    "has_pic")
                                        .description(
                                            "Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                                        .required(false),
                                string("pic_hash").description("The user picture hash")
                                    .required(false),
                                bool("active_flag").description("Whether the user is active or not")
                                    .required(false)))
                            .required(false),
                    object("organization").additionalProperties(object().properties(bool("active_flag").description(
                        "Whether the associated organization is active or not")
                        .required(false),
                        integer("id").description("The ID of the organization associated with the item")
                            .required(false),
                        string("name").description("The name of the organization associated with the item")
                            .required(false),
                        integer("people_count").description(
                            "The number of people connected with the organization that is associated with the item")
                            .required(false),
                        integer("owner_id").description(
                            "The ID of the owner of the organization that is associated with the item")
                            .required(false),
                        string("address").description("The address of the organization")
                            .required(false),
                        string("cc_email").description("The BCC email of the organization associated with the item")
                            .required(false)))
                        .required(false),
                    object("person")
                        .additionalProperties(
                            object()
                                .properties(
                                    bool("active_flag").description("Whether the associated person is active or not")
                                        .required(false),
                                    integer("id").description("The ID of the person associated with the item")
                                        .required(false),
                                    string("name").description("The name of the person associated with the item")
                                        .required(false),
                                    array(
                                        "email")
                                            .items(object().properties(
                                                string("label").description("The type of the email")
                                                    .required(false),
                                                string("value").description("The email of the associated person")
                                                    .required(false),
                                                bool("primary").description("Whether this is the primary email or not")
                                                    .required(false))
                                                .description("The emails of the person associated with the item"))
                                            .description("The emails of the person associated with the item")
                                            .required(false),
                                    array("phone")
                                        .items(
                                            object()
                                                .properties(string("label").description("The type of the phone number")
                                                    .required(false),
                                                    string("value")
                                                        .description(
                                                            "The phone number of the person associated with the item")
                                                        .required(false),
                                                    bool("primary")
                                                        .description("Whether this is the primary phone number or not")
                                                        .required(false))
                                                .description(
                                                    "The phone numbers of the person associated with the item"))
                                        .description("The phone numbers of the person associated with the item")
                                        .required(false),
                                    integer("owner_id")
                                        .description(
                                            "The ID of the owner of the person that is associated with the item")
                                        .required(false)))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("success", true),
            Map.entry("data",
                Map.<String, Object>ofEntries(Map.entry("id", 1),
                    Map.entry("creator_user_id",
                        Map.<String, Object>ofEntries(Map.entry("id", 8877), Map.entry("name", "Creator"),
                            Map.entry("email", "john.doe@pipedrive.com"), Map.entry("has_pic", false),
                            Map.entry("pic_hash", ""), Map.entry("active_flag", true), Map.entry("value", 8877))),
                    Map.entry("user_id",
                        Map.<String, Object>ofEntries(Map.entry("id", 8877), Map.entry("name", "Creator"),
                            Map.entry("email", "john.doe@pipedrive.com"), Map.entry("has_pic", false),
                            Map.entry("pic_hash", ""), Map.entry("active_flag", true), Map.entry("value", 8877))),
                    Map.entry("person_id",
                        Map.<String, Object>ofEntries(Map.entry("active_flag", true), Map.entry("name", "Person"),
                            Map.entry("email",
                                List.of(Map.<String, Object>ofEntries(Map.entry("label", "work"),
                                    Map.entry("value", "person@pipedrive.com"), Map.entry("primary", true)))),
                            Map.entry("phone",
                                List.of(Map.<String, Object>ofEntries(Map.entry("label", "work"),
                                    Map.entry("value", 3.7244499911E10), Map.entry("primary", true)))),
                            Map.entry("value", 1101))),
                    Map.entry("org_id",
                        Map.<String, Object>ofEntries(Map.entry("name", "Organization"), Map.entry("people_count", 2),
                            Map.entry("owner_id", 8877), Map.entry("address", ""), Map.entry("active_flag", true),
                            Map.entry("cc_email", "org@pipedrivemail.com"), Map.entry("value", 5))),
                    Map.entry("stage_id", 2), Map.entry("title", "Deal One"), Map.entry("value", 5000),
                    Map.entry("currency", "EUR"), Map.entry("add_time", "2019-05-29 04:21:51"),
                    Map.entry("update_time", "2019-11-28 16:19:50"),
                    Map.entry("stage_change_time", "2019-11-28 15:41:22"), Map.entry("active", true),
                    Map.entry("deleted", false), Map.entry("status", "open"), Map.entry("probability", ""),
                    Map.entry("next_activity_date", LocalDate.of(2019, 11, 29)),
                    Map.entry("next_activity_time", "11:30:00"), Map.entry("next_activity_id", 128),
                    Map.entry("last_activity_id", ""), Map.entry("last_activity_date", ""),
                    Map.entry("lost_reason", ""), Map.entry("visible_to", 1.0), Map.entry("close_time", ""),
                    Map.entry("pipeline_id", 1), Map.entry("won_time", "2019-11-27 11:40:36"),
                    Map.entry("first_won_time", "2019-11-27 11:40:36"), Map.entry("lost_time", ""),
                    Map.entry("products_count", 0), Map.entry("files_count", 0), Map.entry("notes_count", 2),
                    Map.entry("followers_count", 0), Map.entry("email_messages_count", 4),
                    Map.entry("activities_count", 1), Map.entry("done_activities_count", 0),
                    Map.entry("undone_activities_count", 1), Map.entry("participants_count", 1),
                    Map.entry("expected_close_date", LocalDate.of(2019, 6, 29)),
                    Map.entry("last_incoming_mail_time", "2019-05-29 18:21:42"),
                    Map.entry("last_outgoing_mail_time", "2019-05-30 03:45:35"), Map.entry("label", 11),
                    Map.entry("stage_order_nr", 2), Map.entry("person_name", "Person"),
                    Map.entry("org_name", "Organization"), Map.entry("next_activity_subject", "Call"),
                    Map.entry("next_activity_type", "call"), Map.entry("next_activity_duration", "00:30:00"),
                    Map.entry("next_activity_note", "Note content"), Map.entry("formatted_value", "€5,000"),
                    Map.entry("weighted_value", 5000), Map.entry("formatted_weighted_value", "€5,000"),
                    Map.entry("weighted_value_currency", "EUR"), Map.entry("rotten_time", ""),
                    Map.entry("owner_name", "Creator"), Map.entry("cc_email", "company+deal1@pipedrivemail.com"),
                    Map.entry("org_hidden", false), Map.entry("person_hidden", false))),
            Map.entry("related_objects", Map.<String, Object>ofEntries(
                Map.entry("user",
                    Map.<String, Object>ofEntries(Map.entry("8877", Map.<String, Object>ofEntries(Map.entry("id", 8877),
                        Map.entry("name", "Creator"), Map.entry("email", "john.doe@pipedrive.com"),
                        Map.entry("has_pic", false), Map.entry("pic_hash", ""), Map.entry("active_flag", true))))),
                Map.entry("organization",
                    Map.<String, Object>ofEntries(Map.entry("2",
                        Map.<String, Object>ofEntries(Map.entry("id", 2), Map.entry("name", "Organization"),
                            Map.entry("people_count", 2), Map.entry("owner_id", 8877),
                            Map.entry("address", "Mustamäe tee 3a, 10615 Tallinn"), Map.entry("active_flag", true),
                            Map.entry("cc_email", "org@pipedrivemail.com"))))),
                Map.entry("person",
                    Map.<String, Object>ofEntries(Map.entry("1101",
                        Map.<String, Object>ofEntries(Map.entry("active_flag", true), Map.entry("id", 1101),
                            Map.entry("name", "Person"),
                            Map.entry("email",
                                List.of(Map.<String, Object>ofEntries(Map.entry("label", "work"),
                                    Map.entry("value", "person@pipedrive.com"), Map.entry("primary", true)))),
                            Map.entry("phone",
                                List.of(Map.<String, Object>ofEntries(Map.entry("label", "work"),
                                    Map.entry("value", 3.421787767E9), Map.entry("primary", true)))),
                            Map.entry("owner_id", 8877))))),
                Map.entry("stage",
                    Map.<String, Object>ofEntries(Map.entry("2", Map.<String, Object>ofEntries(Map.entry("id", 2),
                        Map.entry("company_id", 123), Map.entry("order_nr", 1), Map.entry("name", "Stage Name"),
                        Map.entry("active_flag", true), Map.entry("deal_probability", 100), Map.entry("pipeline_id", 1),
                        Map.entry("rotten_flag", false), Map.entry("rotten_days", ""),
                        Map.entry("add_time", "2015-12-08 13:54:06"), Map.entry("update_time", "2015-12-08 13:54:06"),
                        Map.entry("pipeline_name", "Pipeline"), Map.entry("pipeline_deal_probability", true))))),
                Map.entry("pipeline",
                    Map.<String, Object>ofEntries(Map.entry("1",
                        Map.<String, Object>ofEntries(Map.entry("id", 1), Map.entry("name", "Pipeline"),
                            Map.entry("url_title", "Pipeline"), Map.entry("order_nr", 0), Map.entry("active", true),
                            Map.entry("deal_probability", true), Map.entry("add_time", "2015-12-08 10:00:24"),
                            Map.entry("update_time", "2015-12-08 10:00:24")))))))));
}
