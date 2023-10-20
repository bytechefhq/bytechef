
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

package com.bytechef.component.mailchimp.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class MailchimpAddMemberToListAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addMemberToList")
        .title("Add a new member to the list.")
        .description("Add a new member to the list.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/lists/{listId}/members", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("listId").label("ListId")
            .description("The unique ID for the list.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            bool("skip_merge_validation").label("Skip_merge_validation")
                .description(
                    "If skip_merge_validation is true, member data will be accepted without merge field values, even if the merge field is usually required. This defaults to false.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            object().properties(string("email_address").label("Email_address")
                .description("Email address for a subscriber.")
                .required(true),
                string("status").label("Status")
                    .description("Subscriber's current status.")
                    .options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                        option("Cleaned", "cleaned"), option("Pending", "pending"),
                        option("Transactional", "transactional"))
                    .required(true),
                string("email_type").label("Email_type")
                    .description("Type of email this member asked to get ('html' or 'text').")
                    .required(false),
                object("merge_fields").additionalProperties(string())
                    .placeholder("Add")
                    .label("Merge_fields")
                    .description("A dictionary of merge fields where the keys are the merge tags.")
                    .required(false),
                object("interests").additionalProperties(string())
                    .placeholder("Add")
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
                    .items(object().properties(string("marketing_permission_id").label("Marketing_permission_id")
                        .description("The id for the marketing permission on the list.")
                        .required(false),
                        bool("enabled").label("Enabled")
                            .description("If the subscriber has opted-in to the marketing permission.")
                            .required(false))
                        .description("The marketing permissions for the subscriber."))
                    .placeholder("Add")
                    .label("Marketing_permissions")
                    .description("The marketing permissions for the subscriber.")
                    .required(false),
                string("ip_signup").label("Ip_signup")
                    .description("IP address the subscriber signed up from.")
                    .required(false),
                string("timestamp_signup").label("Timestamp_signup")
                    .description("The date and time the subscriber signed up for the list in ISO 8601 format.")
                    .required(false),
                string("ip_opt").label("Ip_opt")
                    .description("The IP address the subscriber used to confirm their opt-in status.")
                    .required(false),
                string("timestamp_opt").label("Timestamp_opt")
                    .description("The date and time the subscriber confirmed their opt-in status in ISO 8601 format.")
                    .required(false),
                array("tags").items(string().description("The tags that are associated with a member."))
                    .placeholder("Add")
                    .label("Tags")
                    .description("The tags that are associated with a member.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object().properties(string("id").label("Id")
            .description("The MD5 hash of the lowercase version of the list member's email address.")
            .required(false),
            string("email_address").label("Email_address")
                .description("Email address for a subscriber.")
                .required(false),
            string("unique_email_id").label("Unique_email_id")
                .description("An identifier for the address across all of Mailchimp.")
                .required(false),
            string("contact_id").label("Contact_id")
                .description(
                    "As Mailchimp evolves beyond email, you may eventually have contacts without email addresses. While the id is the MD5 hash of their email address, this contact_id is agnostic of contact’s inclusion of an email address.")
                .required(false),
            string("full_name").label("Full_name")
                .description("The contact's full name.")
                .required(false),
            string("web_id").label("Web_id")
                .description(
                    "The ID used in the Mailchimp web application. View this member in your Mailchimp account at https://{dc}.admin.mailchimp.com/lists/members/view?id={web_id}.")
                .required(false),
            string("email_type").label("Email_type")
                .description("Type of email this member asked to get ('html' or 'text').")
                .required(false),
            string("status").label("Status")
                .description("Subscriber's current status.")
                .options(option("Subscribed", "subscribed"), option("Unsubscribed", "unsubscribed"),
                    option("Cleaned", "cleaned"), option("Pending", "pending"),
                    option("Transactional", "transactional"))
                .required(false),
            string("unsubscribe_reason").label("Unsubscribe_reason")
                .description("A subscriber's reason for unsubscribing.")
                .required(false),
            bool("consents_to_one_to_one_messaging").label("Consents_to_one_to_one_messaging")
                .description("Indicates whether a contact consents to 1:1 messaging.")
                .required(false),
            object("merge_fields").additionalProperties(string())
                .placeholder("Add")
                .label("Merge_fields")
                .description(
                    "A dictionary of merge fields where the keys are the merge tags. See the Merge Fields documentation for more about the structure.")
                .required(false),
            object("interests").additionalProperties(string())
                .placeholder("Add")
                .label("Interests")
                .description("The key of this object's properties is the ID of the interest in question.")
                .required(false),
            object("stats").properties(number("avg_open_rate").label("Avg_open_rate")
                .description("A subscriber's average open rate.")
                .required(false),
                number("avg_click_rate").label("Avg_click_rate")
                    .description("A subscriber's average clickthrough rate.")
                    .required(false),
                object("ecommerce_data").properties(number("total_revenue").label("Total_revenue")
                    .description("The total revenue the list member has brought in.")
                    .required(false),
                    number("number_of_orders").label("Number_of_orders")
                        .description("The total number of orders placed by the list member.")
                        .required(false),
                    string("currency_code").label("Currency_code")
                        .description("The three-letter ISO 4217 code for the currency that the store accepts.")
                        .required(false))
                    .label("Ecommerce_data")
                    .description("Ecommerce stats for the list member if the list is attached to a store.")
                    .required(false))
                .label("Stats")
                .description("Open and click rates for this subscriber.")
                .required(false),
            string("ip_signup").label("Ip_signup")
                .description("IP address the subscriber signed up from.")
                .required(false),
            string("timestamp_signup").label("Timestamp_signup")
                .description("The date and time the subscriber signed up for the list in ISO 8601 format.")
                .required(false),
            string("ip_opt").label("Ip_opt")
                .description("The IP address the subscriber used to confirm their opt-in status.")
                .required(false),
            string("timestamp_opt").label("Timestamp_opt")
                .description("The date and time the subscriber confirmed their opt-in status in ISO 8601 format.")
                .required(false),
            integer("member_rating").label("Member_rating")
                .description("Star rating for this member, between 1 and 5.")
                .required(false),
            string("last_changed").label("Last_changed")
                .description("The date and time the member's info was last changed in ISO 8601 format.")
                .required(false),
            string("language").label("Language")
                .description("If set/detected, the subscriber's language.")
                .required(false),
            bool("vip").label("Vip")
                .description("VIP status for subscriber.")
                .required(false),
            string("email_client").label("Email_client")
                .description("The list member's email client.")
                .required(false),
            object("location").properties(number("latitude").label("Latitude")
                .description("The location latitude.")
                .required(false),
                number("longitude").label("Longitude")
                    .description("The location longitude.")
                    .required(false),
                integer("gmtoff").label("Gmtoff")
                    .description("The time difference in hours from GMT.")
                    .required(false),
                integer("dstoff").label("Dstoff")
                    .description("The offset for timezones where daylight saving time is observed.")
                    .required(false),
                string("country_code").label("Country_code")
                    .description("The unique code for the location country.")
                    .required(false),
                string("timezone").label("Timezone")
                    .description("The timezone for the location.")
                    .required(false),
                string("region").label("Region")
                    .description("The region for the location.")
                    .required(false))
                .label("Location")
                .description("Subscriber location information.")
                .required(false),
            array("marketing_permissions")
                .items(object().properties(string("marketing_permission_id").label("Marketing_permission_id")
                    .description("The id for the marketing permission on the list")
                    .required(false),
                    string("text").label("Text")
                        .description("The text of the marketing permission.")
                        .required(false),
                    bool("enabled").label("Enabled")
                        .description("If the subscriber has opted-in to the marketing permission.")
                        .required(false))
                    .description("The marketing permissions for the subscriber."))
                .placeholder("Add")
                .label("Marketing_permissions")
                .description("The marketing permissions for the subscriber.")
                .required(false),
            object("last_note").properties(integer("note_id").label("Note_id")
                .description("The note id.")
                .required(false),
                string("created_at").label("Created_at")
                    .description("The date and time the note was created in ISO 8601 format.")
                    .required(false),
                string("created_by").label("Created_by")
                    .description("The author of the note.")
                    .required(false),
                string("note").label("Note")
                    .description("The content of the note.")
                    .required(false))
                .label("Last_note")
                .description("The most recent Note added about this member.")
                .required(false),
            string("source").label("Source")
                .description("The source from which the subscriber was added to this list.")
                .required(false),
            integer("tags_count").label("Tags_count")
                .description("The number of tags applied to this member.")
                .required(false),
            object("tags").properties(integer("id").label("Id")
                .description("The tag id.")
                .required(false),
                string("name").label("Name")
                    .description("The name of the tag.")
                    .required(false))
                .label("Tags")
                .description("Returns up to 50 tags applied to this member.")
                .required(false),
            string("list_id").label("List_id")
                .description("The list id.")
                .required(false),
            array("_links").items(object().properties(string("rel").label("Rel")
                .description("As with an HTML 'rel' attribute, this describes the type of link.")
                .required(false),
                string("href").label("Href")
                    .description(
                        "This property contains a fully-qualified URL that can be called to retrieve the linked resource or perform the linked action.")
                    .required(false),
                string("method").label("Method")
                    .description(
                        "The HTTP method that should be used when accessing the URL defined in 'href'. Possible values: \"GET\", \"POST\", \"PUT\", \"PATCH\", \"DELETE\", \"OPTIONS\", or \"HEAD\".")
                    .required(false),
                string("targetSchema").label("TargetSchema")
                    .description("For GETs, this is a URL representing the schema that the response should conform to.")
                    .required(false),
                string("schema").label("Schema")
                    .description(
                        "For HTTP methods that can receive bodies (POST and PUT), this is a URL representing the schema that the body should conform to.")
                    .required(false))
                .description("A list of link types and descriptions for the API schema documents."))
                .placeholder("Add")
                .label("_links")
                .description("A list of link types and descriptions for the API schema documents.")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .sampleOutput(
            "{ \"id\": \"string\", \"email_address\": \"string\", \"unique_email_id\": \"string\", \"contact_id\": \"string\", \"full_name\": \"string\", \"web_id\": 0, \"email_type\": \"string\", \"status\": \"subscribed\", \"unsubscribe_reason\": \"string\", \"consents_to_one_to_one_messaging\": true, \"merge_fields\": { \"property1\": null, \"property2\": null }, \"interests\": { \"property1\": true, \"property2\": true }, \"stats\": { \"avg_open_rate\": 0, \"avg_click_rate\": 0, \"ecommerce_data\": { \"total_revenue\": 0, \"number_of_orders\": 0, \"currency_code\": \"USD\" } }, \"ip_signup\": \"string\", \"timestamp_signup\": \"2019-08-24T14:15:22Z\", \"ip_opt\": \"string\", \"timestamp_opt\": \"2019-08-24T14:15:22Z\", \"member_rating\": 0, \"last_changed\": \"2019-08-24T14:15:22Z\", \"language\": \"string\", \"vip\": true, \"email_client\": \"string\", \"location\": { \"latitude\": 0, \"longitude\": 0, \"gmtoff\": 0, \"dstoff\": 0, \"country_code\": \"string\", \"timezone\": \"string\", \"region\": \"string\" }, \"marketing_permissions\": [ { \"marketing_permission_id\": \"string\", \"text\": \"string\", \"enabled\": true } ], \"last_note\": { \"note_id\": 0, \"created_at\": \"2019-08-24T14:15:22Z\", \"created_by\": \"string\", \"note\": \"string\" }, \"source\": \"string\", \"tags_count\": 0, \"tags\": [ { \"id\": 0, \"name\": \"string\" } ], \"list_id\": \"string\", \"_links\": [ { \"rel\": \"string\", \"href\": \"string\", \"method\": \"GET\", \"targetSchema\": \"string\", \"schema\": \"string\" } ] }");
}
