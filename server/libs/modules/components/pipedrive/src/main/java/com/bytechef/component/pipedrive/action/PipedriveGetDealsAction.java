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
                .options(option("0", 0D), option("1", 1D))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(bool("success").required(false), array("data")
            .items(integer("email_messages_count").required(false), string("cc_email").required(false),
                integer("products_count").required(false), string("next_activity_date").required(false),
                string("next_activity_type").required(false), string("next_activity_duration").required(false),
                integer("id").required(false), string("name").required(false), bool("active_flag").required(false),
                array("phone")
                    .items(string("label").required(false), string("value").required(false),
                        bool("primary").required(false))
                    .required(false),
                integer("value").required(false),
                array("email")
                    .items(string("label").required(false), string("value").required(false),
                        bool("primary").required(false))
                    .required(false),
                integer("owner_id").required(false),
                object("creator_user_id")
                    .properties(integer("id").required(false), string("name").required(false),
                        string("email").required(false), bool("has_pic").required(false),
                        string("pic_hash").required(false), bool("active_flag").required(false),
                        integer("value").required(false))
                    .required(false),
                date("expected_close_date").required(false), integer("participants_count").required(false),
                string("owner_name").required(false), integer("stage_id").required(false),
                number("probability").required(false), integer("undone_activities_count").required(false),
                bool("active").required(false), string("last_activity_date").required(false),
                string("person_name").required(false), string("close_time").required(false),
                integer("next_activity_id").required(false), string("weighted_value_currency").required(false),
                bool("org_hidden").required(false), integer("stage_order_nr").required(false),
                string("next_activity_subject").required(false), string("rotten_time").required(false),
                string("name").required(false), bool("has_pic").required(false), bool("active_flag").required(false),
                integer("id").required(false), integer("value").required(false), string("email").required(false),
                string("pic_hash").required(false), string("visible_to").required(false),
                string("address").required(false), integer("owner_id").required(false),
                string("cc_email").required(false), string("name").required(false), bool("active_flag").required(false),
                integer("people_count").required(false), integer("value").required(false),
                integer("notes_count").required(false), string("next_activity_time").required(false),
                string("formatted_value").required(false), string("status").required(false),
                string("formatted_weighted_value").required(false), string("first_won_time").required(false),
                string("last_outgoing_mail_time").required(false), string("title").required(false),
                integer("last_activity_id").required(false), string("update_time").required(false),
                integer("activities_count").required(false), integer("pipeline_id").required(false),
                string("lost_time").required(false), string("currency").required(false),
                number("weighted_value").required(false), string("org_name").required(false),
                number("value").required(false), string("next_activity_note").required(false),
                bool("person_hidden").required(false), integer("files_count").required(false),
                string("last_incoming_mail_time").required(false), integer("label").required(false),
                string("lost_reason").required(false), bool("deleted").required(false),
                string("won_time").required(false), integer("followers_count").required(false),
                string("stage_change_time").required(false), string("add_time").required(false),
                integer("done_activities_count").required(false))
            .required(false),
            object("additional_data")
                .properties(integer("start").required(false), integer("limit").required(false),
                    bool("more_items_in_collection").required(false))
                .required(false),
            object("related_objects")
                .properties(
                    object("user")
                        .properties(integer("id").required(false), string("name").required(false),
                            string("email").required(false), bool("has_pic").required(false),
                            string("pic_hash").required(false), bool("active_flag").required(false))
                        .required(false),
                    object("organization")
                        .properties(string("name").required(false), integer("people_count").required(false),
                            integer("owner_id").required(false), string("address").required(false),
                            bool("active_flag").required(false), string("cc_email").required(false))
                        .required(false),
                    object("person")
                        .properties(bool("active_flag").required(false), string("name").required(false),
                            array("email")
                                .items(string("label").required(false), string("value").required(false),
                                    bool("primary").required(false))
                                .required(false),
                            array("phone")
                                .items(string("label").required(false), string("value").required(false),
                                    bool("primary").required(false))
                                .required(false),
                            integer("owner_id").required(false))
                        .required(false))
                .required(false))
        .outputSchemaMetadata(Map.of(
            "responseType", ResponseType.JSON))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("success", true),
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
                        Map.<String, Object>ofEntries(Map.entry("name", "Organization"), Map.entry("people_count", 2),
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
                    Map.entry("first_won_time", "2019-11-27 11:40:36"), Map.entry("lost_time", "2019-11-27 11:40:36"),
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
                        Map.<String, Object>ofEntries(Map.entry("2",
                            Map.<String, Object>ofEntries(Map.entry("id", 2), Map.entry("company_id", 123),
                                Map.entry("order_nr", 1), Map.entry("name", "Stage Name"),
                                Map.entry("active_flag", true), Map.entry("deal_probability", 100),
                                Map.entry("pipeline_id", 1), Map.entry("rotten_flag", false),
                                Map.entry("rotten_days", ""), Map.entry("add_time", "2015-12-08 13:54:06"),
                                Map.entry("update_time", "2015-12-08 13:54:06"), Map.entry("pipeline_name", "Pipeline"),
                                Map.entry("pipeline_deal_probability", true))))),
                    Map.entry("pipeline",
                        Map.<String, Object>ofEntries(Map.entry("1",
                            Map.<String, Object>ofEntries(Map.entry("id", 1), Map.entry("name", "Pipeline"),
                                Map.entry("url_title", "Pipeline"), Map.entry("order_nr", 0), Map.entry("active", true),
                                Map.entry("deal_probability", true), Map.entry("add_time", "2015-12-08 10:00:24"),
                                Map.entry("update_time", "2015-12-08 10:00:24"))))))),
            Map.entry("additional_data",
                Map.<String, Object>ofEntries(Map.entry("pagination",
                    Map.<String, Object>ofEntries(Map.entry("start", 0), Map.entry("limit", 100),
                        Map.entry("more_items_in_collection", false), Map.entry("next_start", 1)))))));
}
