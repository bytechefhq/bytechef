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

package com.bytechef.component.mailchimp.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class MailchimpAddMemberToListAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addMemberToList")
        .title("Add a new member to the list")
        .description("Add a new member to the list.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/lists/{listId}/members", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("listId").label("List Id")
            .description("The unique ID for the list.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            bool("skip_merge_validation").label("Skip Merge Validation")
                .description(
                    "If skip_merge_validation is true, member data will be accepted without merge field values, even if the merge field is usually required. This defaults to false.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object("__item").properties(string("email_address").label("Email Address")
                .description("Email address for a subscriber.")
                .required(true),
                string("status").label("Status")
                    .description("Subscriber's current status.")
                    .options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                        option("Cleaned", "cleaned"), option("Pending", "pending"),
                        option("Transactional", "transactional"))
                    .required(true),
                string("email_type").label("Email Type")
                    .description("Type of email this member asked to get ('html' or 'text').")
                    .options(option("Html", "html"), option("Text", "text"))
                    .required(false),
                object("merge_fields").additionalProperties(string())
                    .placeholder("Add to Merge Fields")
                    .label("Merge Fields")
                    .description("A dictionary of merge fields where the keys are the merge tags.")
                    .required(false),
                object("interests").additionalProperties(string())
                    .placeholder("Add to Interests")
                    .label("Interests")
                    .description("The key of this object's properties is the ID of the interest in question.")
                    .required(false),
                string("language").label("Language")
                    .description("If set/detected, the subscriber's language.")
                    .required(false),
                bool("vip").label("Vip")
                    .description("VIP status for subscriber.")
                    .required(false),
                object("location").properties(number("latitude").label("Latitude")
                    .description("The location latitude.")
                    .required(false),
                    number("longitude").label("Longitude")
                        .description("The location longitude.")
                        .required(false))
                    .label("Location")
                    .description("Subscriber location information.")
                    .required(false),
                array("marketing_permissions")
                    .items(object().properties(string("marketing_permission_id").label("Marketing Permission Id")
                        .description("The id for the marketing permission on the list.")
                        .required(false),
                        bool("enabled").label("Enabled")
                            .description("If the subscriber has opted-in to the marketing permission.")
                            .required(false))
                        .description("The marketing permissions for the subscriber."))
                    .placeholder("Add to Marketing Permissions")
                    .label("Marketing Permissions")
                    .description("The marketing permissions for the subscriber.")
                    .required(false),
                string("ip_signup").label("Ip Signup")
                    .description("IP address the subscriber signed up from.")
                    .required(false),
                string("timestamp_signup").label("Timestamp Signup")
                    .description("The date and time the subscriber signed up for the list in ISO 8601 format.")
                    .required(false),
                string("ip_opt").label("Ip Opt")
                    .description("The IP address the subscriber used to confirm their opt-in status.")
                    .required(false),
                string("timestamp_opt").label("Timestamp Opt")
                    .description("The date and time the subscriber confirmed their opt-in status in ISO 8601 format.")
                    .required(false),
                array("tags").items(string().description("The tags that are associated with a member."))
                    .placeholder("Add to Tags")
                    .label("Tags")
                    .description("The tags that are associated with a member.")
                    .required(false))
                .label("Item")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(string("id").required(false), string("email_address").required(false),
            string("unique_email_id").required(false), string("contact_id").required(false),
            string("full_name").required(false), string("web_id").required(false), string("email_type").required(false),
            string("status").options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                option("Cleaned", "cleaned"), option("Pending", "pending"), option("Transactional", "transactional"))
                .required(false),
            string("unsubscribe_reason").required(false), bool("consents_to_one_to_one_messaging").required(false),
            object("merge_fields").required(false), object("interests").required(false),
            object("stats")
                .properties(number("avg_open_rate").required(false), number("avg_click_rate").required(false),
                    object("ecommerce_data")
                        .properties(number("total_revenue").required(false), number("number_of_orders").required(false),
                            string("currency_code").required(false))
                        .required(false))
                .required(false),
            string("ip_signup").required(false), string("timestamp_signup").required(false),
            string("ip_opt").required(false), string("timestamp_opt").required(false),
            integer("member_rating").required(false), string("last_changed").required(false),
            string("language").required(false), bool("vip").required(false), string("email_client").required(false),
            object("location")
                .properties(number("latitude").required(false), number("longitude").required(false),
                    integer("gmtoff").required(false), integer("dstoff").required(false),
                    string("country_code").required(false), string("timezone").required(false),
                    string("region").required(false))
                .required(false),
            array("marketing_permissions")
                .items(string("marketing_permission_id").required(false), string("text").required(false),
                    bool("enabled").required(false))
                .required(false),
            object("last_note")
                .properties(integer("note_id").required(false), string("created_at").required(false),
                    string("created_by").required(false), string("note").required(false))
                .required(false),
            string("source").required(false), integer("tags_count").required(false),
            object("tags").properties(integer("id").required(false), string("name").required(false))
                .required(false),
            string("list_id").required(false),
            array("_links")
                .items(string("rel").required(false), string("href").required(false), string("method").required(false),
                    string("targetSchema").required(false), string("schema").required(false))
                .required(false))
        .outputSchemaMetadata(Map.of(
            "responseType", ResponseType.JSON))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("id", "string"), Map.entry("email_address", "string"),
            Map.entry("unique_email_id", "string"), Map.entry("contact_id", "string"), Map.entry("full_name", "string"),
            Map.entry("web_id", 0), Map.entry("email_type", "string"), Map.entry("status", "subscribed"),
            Map.entry("unsubscribe_reason", "string"), Map.entry("consents_to_one_to_one_messaging", true),
            Map.entry("merge_fields",
                Map.<String, Object>ofEntries(Map.entry("property1", ""), Map.entry("property2", ""))),
            Map.entry("interests",
                Map.<String, Object>ofEntries(Map.entry("property1", true), Map.entry("property2", true))),
            Map.entry("stats",
                Map.<String, Object>ofEntries(Map.entry("avg_open_rate", 0), Map.entry("avg_click_rate", 0),
                    Map.entry("ecommerce_data",
                        Map.<String, Object>ofEntries(Map.entry("total_revenue", 0), Map.entry("number_of_orders", 0),
                            Map.entry("currency_code", "USD"))))),
            Map.entry("ip_signup", "string"), Map.entry("timestamp_signup", LocalDateTime.of(2019, 8, 24, 14, 15, 22)),
            Map.entry("ip_opt", "string"), Map.entry("timestamp_opt", LocalDateTime.of(2019, 8, 24, 14, 15, 22)),
            Map.entry("member_rating", 0), Map.entry("last_changed", LocalDateTime.of(2019, 8, 24, 14, 15, 22)),
            Map.entry("language", "string"), Map.entry("vip", true), Map.entry("email_client", "string"),
            Map.entry("location",
                Map.<String, Object>ofEntries(Map.entry("latitude", 0), Map.entry("longitude", 0),
                    Map.entry("gmtoff", 0), Map.entry("dstoff", 0), Map.entry("country_code", "string"),
                    Map.entry("timezone", "string"), Map.entry("region", "string"))),
            Map.entry("marketing_permissions",
                List.of(Map.<String, Object>ofEntries(Map.entry("marketing_permission_id", "string"),
                    Map.entry("text", "string"), Map.entry("enabled", true)))),
            Map.entry("last_note",
                Map.<String, Object>ofEntries(Map.entry("note_id", 0),
                    Map.entry("created_at", LocalDateTime.of(2019, 8, 24, 14, 15, 22)),
                    Map.entry("created_by", "string"), Map.entry("note", "string"))),
            Map.entry("source", "string"), Map.entry("tags_count", 0),
            Map.entry("tags", List.of(Map.<String, Object>ofEntries(Map.entry("id", 0), Map.entry("name", "string")))),
            Map.entry("list_id", "string"),
            Map.entry("_links",
                List.of(Map.<String, Object>ofEntries(Map.entry("rel", "string"), Map.entry("href", "string"),
                    Map.entry("method", "GET"), Map.entry("targetSchema", "string"), Map.entry("schema", "string"))))));
}
