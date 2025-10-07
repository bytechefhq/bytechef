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

package com.bytechef.component.mailchimp.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.mailchimp.util.MailchimpUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class MailchimpAddMemberToListAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addMemberToList")
        .title("Add Member to List")
        .description("Adds a new member to the list.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/lists/{listId}/members", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("listId").label("List ID")
            .description("The unique ID for the list.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) MailchimpUtils::getListIdOptions)
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
            string("email_address").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Email Address")
                .description("Email address for a subscriber.")
                .required(true),
            string("status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Status")
                .description("Subscriber's current status.")
                .options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                    option("Cleaned", "cleaned"), option("Pending", "pending"),
                    option("Transactional", "transactional"))
                .required(true),
            string("email_type").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Email Type")
                .description("Type of email this member asked to get ('html' or 'text').")
                .options(option("Html", "html"), option("Text", "text"))
                .required(false),
            object("merge_fields").additionalProperties(string())
                .placeholder("Add to Merge Fields")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Merge Fields")
                .description("A dictionary of merge fields where the keys are the merge tags.")
                .required(false),
            object("interests").additionalProperties(string())
                .placeholder("Add to Interests")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Interests")
                .description("The key of this object's properties is the ID of the interest in question.")
                .required(false),
            string("language").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Language")
                .description("If set/detected, the subscriber's language.")
                .required(false),
            bool("vip").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Vip")
                .description("VIP status for subscriber.")
                .required(false),
            object("location").properties(number("latitude").label("Latitude")
                .description("The location latitude.")
                .required(false),
                number("longitude").label("Longitude")
                    .description("The location longitude.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
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
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Marketing Permissions")
                .description("The marketing permissions for the subscriber.")
                .required(false),
            string("ip_signup").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Ip Signup")
                .description("IP address the subscriber signed up from.")
                .required(false),
            string("timestamp_signup").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Timestamp Signup")
                .description("The date and time the subscriber signed up for the list in ISO 8601 format.")
                .required(false),
            string("ip_opt").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Ip Opt")
                .description("The IP address the subscriber used to confirm their opt-in status.")
                .required(false),
            string("timestamp_opt").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Timestamp Opt")
                .description("The date and time the subscriber confirmed their opt-in status in ISO 8601 format.")
                .required(false),
            array("tags").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("The tags that are associated with a member."))
                .placeholder("Add to Tags")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Tags")
                .description("The tags that are associated with a member.")
                .required(false))
        .output(
            outputSchema(object().properties(string("id")
                .description("The MD5 hash of the lowercase version of the list member's email address.")
                .required(false),
                string("email_address").description("Email address for a subscriber.")
                    .required(false),
                string("unique_email_id").description("An identifier for the address across all of Mailchimp.")
                    .required(false),
                string("contact_id").description(
                    "As Mailchimp evolves beyond email, you may eventually have contacts without email addresses. While the id is the MD5 hash of their email address, this contact_id is agnostic of contact’s inclusion of an email address.")
                    .required(false),
                string("full_name").description("The contact's full name.")
                    .required(false),
                string("web_id").description(
                    "The ID used in the Mailchimp web application. View this member in your Mailchimp account at https://{dc}.admin.mailchimp.com/lists/members/view?id={web_id}.")
                    .required(false),
                string("email_type").description("Type of email this member asked to get ('html' or 'text').")
                    .required(false),
                string("status").description("Subscriber's current status.")
                    .options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                        option("Cleaned", "cleaned"), option("Pending", "pending"),
                        option("Transactional", "transactional"))
                    .required(false),
                string("unsubscribe_reason").description("A subscriber's reason for unsubscribing.")
                    .required(false),
                bool("consents_to_one_to_one_messaging")
                    .description("Indicates whether a contact consents to 1:1 messaging.")
                    .required(false),
                object("merge_fields").additionalProperties(string())
                    .description(
                        "A dictionary of merge fields where the keys are the merge tags. See the Merge Fields documentation for more about the structure.")
                    .required(false),
                object("interests").additionalProperties(string())
                    .description("The key of this object's properties is the ID of the interest in question.")
                    .required(false),
                object("stats").properties(number("avg_open_rate").description("A subscriber's average open rate.")
                    .required(false),
                    number("avg_click_rate").description("A subscriber's average clickthrough rate.")
                        .required(false),
                    object("ecommerce_data").properties(
                        number("total_revenue").description("The total revenue the list member has brought in.")
                            .required(false),
                        number("number_of_orders").description("The total number of orders placed by the list member.")
                            .required(false),
                        string("currency_code")
                            .description("The three-letter ISO 4217 code for the currency that the store accepts.")
                            .required(false))
                        .description("Ecommerce stats for the list member if the list is attached to a store.")
                        .required(false))
                    .description("Open and click rates for this subscriber.")
                    .required(false),
                string("ip_signup").description("IP address the subscriber signed up from.")
                    .required(false),
                string("timestamp_signup")
                    .description("The date and time the subscriber signed up for the list in ISO 8601 format.")
                    .required(false),
                string("ip_opt").description("The IP address the subscriber used to confirm their opt-in status.")
                    .required(false),
                string("timestamp_opt")
                    .description("The date and time the subscriber confirmed their opt-in status in ISO 8601 format.")
                    .required(false),
                integer("member_rating").description("Star rating for this member, between 1 and 5.")
                    .required(false),
                string("last_changed")
                    .description("The date and time the member's info was last changed in ISO 8601 format.")
                    .required(false),
                string("language").description("If set/detected, the subscriber's language.")
                    .required(false),
                bool("vip").description("VIP status for subscriber.")
                    .required(false),
                string("email_client").description("The list member's email client.")
                    .required(false),
                object("location").properties(number("latitude").description("The location latitude.")
                    .required(false),
                    number("longitude").description("The location longitude.")
                        .required(false),
                    integer("gmtoff").description("The time difference in hours from GMT.")
                        .required(false),
                    integer("dstoff").description("The offset for timezones where daylight saving time is observed.")
                        .required(false),
                    string("country_code").description("The unique code for the location country.")
                        .required(false),
                    string("timezone").description("The timezone for the location.")
                        .required(false),
                    string("region").description("The region for the location.")
                        .required(false))
                    .description("Subscriber location information.")
                    .required(false),
                array("marketing_permissions")
                    .items(object().properties(
                        string("marketing_permission_id").description("The id for the marketing permission on the list")
                            .required(false),
                        string("text").description("The text of the marketing permission.")
                            .required(false),
                        bool("enabled").description("If the subscriber has opted-in to the marketing permission.")
                            .required(false))
                        .description("The marketing permissions for the subscriber."))
                    .description("The marketing permissions for the subscriber.")
                    .required(false),
                object("last_note").properties(integer("note_id").description("The note id.")
                    .required(false),
                    string("created_at").description("The date and time the note was created in ISO 8601 format.")
                        .required(false),
                    string("created_by").description("The author of the note.")
                        .required(false),
                    string("note").description("The content of the note.")
                        .required(false))
                    .description("The most recent Note added about this member.")
                    .required(false),
                string("source").description("The source from which the subscriber was added to this list.")
                    .required(false),
                integer("tags_count").description("The number of tags applied to this member.")
                    .required(false),
                object("tags").properties(integer("id").description("The tag id.")
                    .required(false),
                    string("name").description("The name of the tag.")
                        .required(false))
                    .description("Returns up to 50 tags applied to this member.")
                    .required(false),
                string("list_id").description("The list id.")
                    .required(false),
                array("_links").items(object().properties(string("rel")
                    .description("As with an HTML 'rel' attribute, this describes the type of link.")
                    .required(false),
                    string("href").description(
                        "This property contains a fully-qualified URL that can be called to retrieve the linked resource or perform the linked action.")
                        .required(false),
                    string("method").description(
                        "The HTTP method that should be used when accessing the URL defined in 'href'. Possible values: \"GET\", \"POST\", \"PUT\", \"PATCH\", \"DELETE\", \"OPTIONS\", or \"HEAD\".")
                        .required(false),
                    string("targetSchema")
                        .description(
                            "For GETs, this is a URL representing the schema that the response should conform to.")
                        .required(false),
                    string("schema").description(
                        "For HTTP methods that can receive bodies (POST and PUT), this is a URL representing the schema that the body should conform to.")
                        .required(false))
                    .description("The list of link types and descriptions for the API schema documents."))
                    .description("The list of link types and descriptions for the API schema documents.")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))),
            sampleOutput(Map.<String, Object>ofEntries(Map.entry("id", "string"), Map.entry("email_address", "string"),
                Map.entry("unique_email_id", "string"), Map.entry("contact_id", "string"),
                Map.entry("full_name", "string"), Map.entry("web_id", 0), Map.entry("email_type", "string"),
                Map.entry("status", "subscribed"), Map.entry("unsubscribe_reason", "string"),
                Map.entry("consents_to_one_to_one_messaging", true),
                Map.entry("merge_fields",
                    Map.<String, Object>ofEntries(Map.entry("property1", ""), Map.entry("property2", ""))),
                Map.entry("interests",
                    Map.<String, Object>ofEntries(Map.entry("property1", true), Map.entry("property2", true))),
                Map.entry("stats",
                    Map.<String, Object>ofEntries(Map.entry("avg_open_rate", 0), Map.entry("avg_click_rate", 0),
                        Map.entry("ecommerce_data",
                            Map.<String, Object>ofEntries(Map.entry("total_revenue", 0),
                                Map.entry("number_of_orders", 0), Map.entry("currency_code", "USD"))))),
                Map.entry("ip_signup", "string"),
                Map.entry("timestamp_signup", LocalDateTime.of(2019, 8, 24, 14, 15, 22)), Map.entry("ip_opt", "string"),
                Map.entry("timestamp_opt", LocalDateTime.of(2019, 8, 24, 14, 15, 22)), Map.entry("member_rating", 0),
                Map.entry("last_changed", LocalDateTime.of(2019, 8, 24, 14, 15, 22)), Map.entry("language", "string"),
                Map.entry("vip", true), Map.entry("email_client", "string"),
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
                Map.entry("tags",
                    List.of(Map.<String, Object>ofEntries(Map.entry("id", 0), Map.entry("name", "string")))),
                Map.entry("list_id", "string"),
                Map.entry("_links",
                    List.of(Map.<String, Object>ofEntries(Map.entry("rel", "string"), Map.entry("href", "string"),
                        Map.entry("method", "GET"), Map.entry("targetSchema", "string"),
                        Map.entry("schema", "string")))))));

    private MailchimpAddMemberToListAction() {
    }
}
