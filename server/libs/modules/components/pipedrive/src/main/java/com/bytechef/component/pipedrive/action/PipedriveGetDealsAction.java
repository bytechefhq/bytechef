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
public class PipedriveGetDealsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getDeals")
        .title("Get all deals")
        .description(
            "Returns all deals. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/getting-all-deals\" target=\"_blank\" rel=\"noopener noreferrer\">getting all deals</a>.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/deals"

            ))
        .properties(integer("user_id").label("User Id")
            .description(
                "If supplied, only deals matching the given user will be returned. However, `filter_id` and `owned_by_you` takes precedence over `user_id` when supplied.")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            integer("filter_id").label("Filter Id")
                .description("The ID of the filter to use")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("stage_id").label("Stage Id")
                .description("If supplied, only deals within the given stage will be returned")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("status").label("Status")
                .description(
                    "Only fetch deals with a specific status. If omitted, all not deleted deals are returned. If set to deleted, deals that have been deleted up to 30 days ago will be included.")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                    option("Deleted", "deleted"), option("All_not_deleted", "all_not_deleted"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("start").label("Start")
                .description("Pagination start")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("limit").label("Limit")
                .description("Items shown per page")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("sort").label("Sort")
                .description(
                    "The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            number("owned_by_you").label("Owned By You")
                .description(
                    "When supplied, only deals owned by you are returned. However, `filter_id` takes precedence over `owned_by_you` when both are supplied.")
                .options(option("0", 0), option("1", 1))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(object().properties(bool("success").description("If the response is successful or not")
            .required(false),
            array("data")
                .items(
                    object()
                        .properties(
                            integer("email_messages_count").description("The number of emails associated with the deal")
                                .required(false),
                            string("cc_email").description("The BCC email of the deal")
                                .required(false),
                            integer("products_count").description("The number of products associated with the deal")
                                .required(false),
                            string("next_activity_date").description(
                                "The date of the next activity associated with the deal")
                                .required(false),
                            string("next_activity_type").description(
                                "The type of the next activity associated with the deal")
                                .required(false),
                            string("next_activity_duration").description(
                                "The duration of the next activity associated with the deal")
                                .required(false),
                            integer("id").description("The ID of the deal")
                                .required(false),
                            string("name").description("The name of the person associated with the deal")
                                .required(false),
                            bool("active_flag").description("Whether the associated person is active or not")
                                .required(false),
                            array("phone").items(object().properties(
                                string("label").description("The type of the phone number")
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
                            array("email")
                                .items(object().properties(string("label").description("The type of the email")
                                    .required(false),
                                    string("value").description("The email of the associated person")
                                        .required(false),
                                    bool("primary").description("If this is the primary email or not")
                                        .required(false))
                                    .description("The emails of the person associated with the deal"))
                                .description("The emails of the person associated with the deal")
                                .required(false),
                            integer("owner_id")
                                .description("The ID of the owner of the person that is associated with the deal")
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
                            integer("participants_count")
                                .description("The number of participants associated with the deal")
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
                            string("last_activity_date")
                                .description("The date of the last activity associated with the deal")
                                .required(false),
                            string("person_name").description("The name of the person associated with the deal")
                                .required(false),
                            string("close_time").description("The date and time of closing the deal")
                                .required(false),
                            integer("next_activity_id")
                                .description("The ID of the next activity associated with the deal")
                                .required(false),
                            string("weighted_value_currency").description("The currency associated with the deal")
                                .required(false),
                            bool("org_hidden")
                                .description("If the organization that is associated with the deal is hidden or not")
                                .required(false),
                            integer("stage_order_nr")
                                .description("The order number of the deal stage associated with the deal")
                                .required(false),
                            string("next_activity_subject")
                                .description("The subject of the next activity associated with the deal")
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
                            string("address")
                                .description("The address of the organization that is associated with the deal")
                                .required(false),
                            integer("owner_id")
                                .description("The ID of the owner of the organization that is associated with the deal")
                                .required(false),
                            string("cc_email").description("The BCC email of the organization associated with the deal")
                                .required(false),
                            string("name").description("The name of the organization associated with the deal")
                                .required(false),
                            bool("active_flag").description("Whether the associated organization is active or not")
                                .required(false),
                            integer("people_count").description(
                                "The number of people connected with the organization that is associated with the deal")
                                .required(false),
                            integer("value").description("The ID of the organization associated with the deal")
                                .required(false),
                            integer("notes_count").description("The number of notes associated with the deal")
                                .required(false),
                            string("next_activity_time")
                                .description("The time of the next activity associated with the deal")
                                .required(false),
                            string("formatted_value")
                                .description("The deal value formatted with selected currency. E.g. US$500")
                                .required(false),
                            string("status").description("The status of the deal")
                                .required(false),
                            string("formatted_weighted_value")
                                .description("The weighted_value formatted with selected currency. E.g. US$500")
                                .required(false),
                            string("first_won_time")
                                .description("The date and time of the first time changing the deal status as won")
                                .required(false),
                            string("last_outgoing_mail_time")
                                .description("The date and time of the last outgoing email associated with the deal")
                                .required(false),
                            string("title").description("The title of the deal")
                                .required(false),
                            integer("last_activity_id")
                                .description("The ID of the last activity associated with the deal")
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
                            string("next_activity_note")
                                .description("The note of the next activity associated with the deal")
                                .required(false),
                            bool("person_hidden")
                                .description("If the person that is associated with the deal is hidden or not")
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
                            integer("done_activities_count")
                                .description("The number of completed activities associated with the deal")
                                .required(false))
                        .description("The array of deals"))
                .description("The array of deals")
                .required(false),
            object("additional_data").properties(integer("start").description("Pagination start")
                .required(false),
                integer("limit").description("Items shown per page")
                    .required(false),
                bool("more_items_in_collection")
                    .description("If there are more list items in the collection than displayed or not")
                    .required(false))
                .description("The additional data of the list")
                .required(false),
            object("related_objects")
                .properties(object("user").properties(integer("id").description("The ID of the user")
                    .required(false),
                    string("name").description("The name of the user")
                        .required(false),
                    string("email").description("The email of the user")
                        .required(false),
                    bool("has_pic").description("If the user has a picture or not")
                        .required(false),
                    string("pic_hash").description("The user picture hash")
                        .required(false),
                    bool("active_flag").description("Whether the user is active or not")
                        .required(false))
                    .description("The user who is associated with the deal")
                    .required(false),
                    object("organization").properties(string("name").description(
                        "The name of the organization associated with the deal")
                        .required(false),
                        integer("people_count")
                            .description(
                                "The number of people connected with the organization that is associated with the deal")
                            .required(false),
                        integer("owner_id").description(
                            "The ID of the owner of the organization that is associated with the deal")
                            .required(false),
                        string("address").description(
                            "The address of the organization that is associated with the deal")
                            .required(false),
                        bool("active_flag").description("Whether the associated organization is active or not")
                            .required(false),
                        string("cc_email").description("The BCC email of the organization associated with the deal")
                            .required(false))
                        .description("The organization which is associated with the deal")
                        .required(false),
                    object("person")
                        .properties(bool("active_flag").description("Whether the associated person is active or not")
                            .required(false),
                            string("name").description("The name of the person associated with the deal")
                                .required(false),
                            array("email")
                                .items(object().properties(string("label").description("The type of the email")
                                    .required(false),
                                    string("value").description("The email of the associated person")
                                        .required(false),
                                    bool("primary").description("If this is the primary email or not")
                                        .required(false))
                                    .description("The emails of the person associated with the deal"))
                                .description("The emails of the person associated with the deal")
                                .required(false),
                            array("phone").items(object().properties(
                                string("label").description("The type of the phone number")
                                    .required(false),
                                string("value").description("The phone number of the person associated with the deal")
                                    .required(false),
                                bool("primary").description("If this is the primary phone number or not")
                                    .required(false))
                                .description("The phone numbers of the person associated with the deal"))
                                .description("The phone numbers of the person associated with the deal")
                                .required(false),
                            integer("owner_id")
                                .description("The ID of the owner of the person that is associated with the deal")
                                .required(false))
                        .description("The person who is associated with the deal")
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)),
            Map.<String, Object>ofEntries(Map.entry("success", true),
                Map.entry("data",
                    List.of(Map.<String, Object>ofEntries(Map.entry("id", 1),
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
                            Map.<String, Object>ofEntries(Map.entry("name", "Organization"),
                                Map.entry("people_count", 2),
                                Map.entry("owner_id", 8877), Map.entry("address", "Mustamäe tee 3a, 10615 Tallinn"),
                                Map.entry("active_flag", true), Map.entry("cc_email", "org@pipedrivemail.com"),
                                Map.entry("value", 5))),
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
                        Map.entry("first_won_time", "2019-11-27 11:40:36"),
                        Map.entry("lost_time", "2019-11-27 11:40:36"),
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
                        Map.entry("org_hidden", false), Map.entry("person_hidden", false)))),
                Map.entry("related_objects",
                    Map.<String, Object>ofEntries(
                        Map.entry("user",
                            Map.<String, Object>ofEntries(Map.entry("8877",
                                Map.<String, Object>ofEntries(Map.entry("id", 8877), Map.entry("name", "Creator"),
                                    Map.entry("email", "john.doe@pipedrive.com"), Map.entry("has_pic", false),
                                    Map.entry("pic_hash", ""), Map.entry("active_flag", true))))),
                        Map.entry("organization",
                            Map.<String, Object>ofEntries(Map.entry("5",
                                Map.<String, Object>ofEntries(Map.entry("id", 5), Map.entry("name", "Organization"),
                                    Map.entry("people_count", 2), Map.entry("owner_id", 8877),
                                    Map.entry("address", "Mustamäe tee 3a, 10615 Tallinn"),
                                    Map.entry("active_flag", true),
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
                            Map.<String, Object>ofEntries(Map.entry("2",
                                Map.<String, Object>ofEntries(Map.entry("id", 2), Map.entry("company_id", 123),
                                    Map.entry("order_nr", 1), Map.entry("name", "Stage Name"),
                                    Map.entry("active_flag", true), Map.entry("deal_probability", 100),
                                    Map.entry("pipeline_id", 1), Map.entry("rotten_flag", false),
                                    Map.entry("rotten_days", ""), Map.entry("add_time", "2015-12-08 13:54:06"),
                                    Map.entry("update_time", "2015-12-08 13:54:06"),
                                    Map.entry("pipeline_name", "Pipeline"),
                                    Map.entry("pipeline_deal_probability", true))))),
                        Map.entry("pipeline",
                            Map.<String, Object>ofEntries(Map.entry("1",
                                Map.<String, Object>ofEntries(Map.entry("id", 1), Map.entry("name", "Pipeline"),
                                    Map.entry("url_title", "Pipeline"), Map.entry("order_nr", 0),
                                    Map.entry("active", true),
                                    Map.entry("deal_probability", true), Map.entry("add_time", "2015-12-08 10:00:24"),
                                    Map.entry("update_time", "2015-12-08 10:00:24"))))))),
                Map.entry("additional_data",
                    Map.<String, Object>ofEntries(Map.entry("pagination",
                        Map.<String, Object>ofEntries(Map.entry("start", 0), Map.entry("limit", 100),
                            Map.entry("more_items_in_collection", false), Map.entry("next_start", 1)))))));
}
