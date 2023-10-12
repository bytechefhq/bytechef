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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.Context.Http.BodyContentType;
import static com.bytechef.hermes.component.definition.Context.Http.ResponseType;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddPersonAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addPerson")
        .title("Add a person")
        .description(
            "Adds a new person. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the personFields and look for `key` values.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also accept and return the `data.marketing_status` field.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("marketing_status").label("Marketing Status")
            .description(
                "If the person does not have a valid email address, then the marketing status is **not set** and `no_consent` is returned for the `marketing_status` value when the new person is created. If the change is forbidden, the status will remain unchanged for every call that tries to modify the marketing status. Please be aware that it is only allowed **once** to change the marketing status from an old status to a new one.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`no_consent`</td><td>The customer has not given consent to receive any marketing communications</td></tr><tr><td>`unsubscribed`</td><td>The customers have unsubscribed from ALL marketing communications</td></tr><tr><td>`subscribed`</td><td>The customers are subscribed and are counted towards marketing caps</td></tr><tr><td>`archived`</td><td>The customers with `subscribed` status can be moved to `archived` to save consent, but they are not paid for</td></tr></table>")
            .options(option("No_consent", "no_consent"), option("Unsubscribed", "unsubscribed"),
                option("Subscribed", "subscribed"), option("Archived", "archived"))
            .required(false),
            array("phone").items(object().properties(string("value").label("Value")
                .description("The phone number")
                .required(false),
                bool("primary").label("Primary")
                    .description("Boolean that indicates if phone number is primary for the person or not")
                    .required(false),
                string("label").label("Label")
                    .description(
                        "The label that indicates the type of the phone number. (Possible values - work, home, mobile or other)")
                    .required(false))
                .description(
                    "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required."))
                .placeholder("Add to Phone")
                .label("Phone")
                .description(
                    "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required.")
                .required(false),
            string("visible_to").label("Visible To")
                .description(
                    "The visibility of the person. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                .required(false),
            integer("owner_id").label("Owner Id")
                .description(
                    "The ID of the user who will be marked as the owner of this person. When omitted, the authorized user ID will be used.")
                .required(false),
            integer("org_id").label("Org Id")
                .description("The ID of the organization this person will belong to")
                .required(false),
            string("name").label("Name")
                .description("The name of the person")
                .required(false),
            string("add_time").label("Add Time")
                .description(
                    "The optional creation date & time of the person in UTC. Requires admin user API token. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            array("email").items(object().properties(string("value").label("Value")
                .description("The email")
                .required(false),
                bool("primary").label("Primary")
                    .description("Boolean that indicates if email is primary for the person or not")
                    .required(false),
                string("label").label("Label")
                    .description(
                        "The label that indicates the type of the email. (Possible values - work, home or other)")
                    .required(false))
                .description(
                    "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" }]`. Please note that only `value` is required."))
                .placeholder("Add to Email")
                .label("Email")
                .description(
                    "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" }]`. Please note that only `value` is required.")
                .required(false))
            .label("Item")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(
            object()
                .properties(
                    integer("related_closed_deals_count")
                        .description("The count of related closed deals related with the item")
                        .required(false),
                    integer("email_messages_count").description("The count of email messages related to the person")
                        .required(false),
                    string("cc_email").description("The BCC email associated with the person")
                        .required(false),
                    string("name").description("The name of the user")
                        .required(false),
                    integer("has_pic")
                        .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                        .required(false),
                    bool("active_flag").description("Whether the user is active or not")
                        .required(false),
                    integer("id").description("The ID of the user")
                        .required(false),
                    integer("value").description("The ID of the owner")
                        .required(false),
                    string("email").description("The email of the user")
                        .required(false),
                    string("pic_hash").description("The user picture hash")
                        .required(false),
                    integer("open_deals_count").description("The count of open deals related with the item")
                        .required(false),
                    string("last_outgoing_mail_time")
                        .description("The date and time of the last outgoing email associated with the person")
                        .required(false),
                    bool("active_flag").description("Whether the person is active or not")
                        .required(false),
                    string("update_time").description("The update time of the picture")
                        .required(false),
                    integer("added_by_user_id").description("The ID of the user who added the picture")
                        .required(false),
                    integer("item_id").description("The ID of related item")
                        .required(false),
                    string("item_type").description("The type of item the picture is related to")
                        .required(false),
                    bool("active_flag").description("Whether the associated picture is active or not")
                        .required(false),
                    integer("id").description("The ID of the picture associated with the item")
                        .required(false),
                    string("add_time").description("The add time of the picture")
                        .required(false),
                    object("pictures").properties(string("128").description("The URL of the 128*128 picture")
                        .required(false),
                        string("512").description("The URL of the 512*512 picture")
                            .required(false))
                        .required(false),
                    integer("last_activity_id").description("The ID of the last activity associated with the deal")
                        .required(false),
                    string("next_activity_date").description("The date of the next activity associated with the deal")
                        .required(false),
                    string("update_time")
                        .description("The last updated date and time of the person. Format: YYYY-MM-DD HH:MM:SS")
                        .required(false),
                    integer("activities_count").description("The count of activities related to the person")
                        .required(false),
                    integer("id").description("The ID of the person")
                        .required(false),
                    string("org_name").description("The name of the organization associated with the person")
                        .required(false),
                    string("first_name").description("The first name of the person")
                        .required(false),
                    array("email").items(object().properties(string("value").description("Email")
                        .required(false),
                        bool("primary").description("Boolean that indicates if email is primary for the person or not")
                            .required(false),
                        string(
                            "label").description(
                                "The label that indicates the type of the email. (Possible values - work, home or other)")
                                .required(false))
                        .description(
                            "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" } ]`. Please note that only `value` is required."))
                        .description(
                            "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" } ]`. Please note that only `value` is required.")
                        .required(false),
                    integer("won_deals_count").description("The count of won deals related with the item")
                        .required(false),
                    string("owner_name").description("The name of the owner associated with the person")
                        .required(false),
                    integer("files_count").description("The count of files related to the person")
                        .required(false),
                    integer("company_id").description("The ID of the company related to the person")
                        .required(false),
                    integer("related_won_deals_count")
                        .description("The count of related won deals related with the item")
                        .required(false),
                    string("last_incoming_mail_time")
                        .description("The date and time of the last incoming email associated with the person")
                        .required(false),
                    string("first_char").description("The first letter of the name of the person")
                        .required(false),
                    integer("undone_activities_count")
                        .description("The count of undone activities related to the person")
                        .required(false),
                    integer("closed_deals_count").description("The count of closed deals related with the item")
                        .required(false),
                    string("last_name").description("The last name of the person")
                        .required(false),
                    string("last_activity_date").description("The date of the last activity associated with the deal")
                        .required(false),
                    integer("label").description("The label assigned to the person")
                        .required(false),
                    integer("related_open_deals_count")
                        .description("The count of related open deals related with the item")
                        .required(false),
                    integer("related_lost_deals_count")
                        .description("The count of related lost deals related with the item")
                        .required(false),
                    integer("next_activity_id").description("The ID of the next activity associated with the deal")
                        .required(false),
                    array("phone").items(object().properties(string("value").description("The phone number")
                        .required(false),
                        bool("primary")
                            .description("Boolean that indicates if phone number is primary for the person or not")
                            .required(false),
                        string("label").description(
                            "The label that indicates the type of the phone number. (Possible values - work, home, mobile or other)")
                            .required(false))
                        .description(
                            "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required."))
                        .description(
                            "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required.")
                        .required(false),
                    string("visible_to").description("The visibility group ID of who can see the person")
                        .required(false),
                    string("address").description("The address of the organization")
                        .required(false),
                    integer("owner_id")
                        .description("The ID of the owner of the organization that is associated with the item")
                        .required(false),
                    string("cc_email").description("The BCC email of the organization associated with the item")
                        .required(false),
                    string("name").description("The name of the organization associated with the item")
                        .required(false),
                    bool("active_flag").description("Whether the associated organization is active or not")
                        .required(false),
                    integer("people_count")
                        .description(
                            "The number of people connected with the organization that is associated with the item")
                        .required(false),
                    integer("value").description("The ID of the organization")
                        .required(false),
                    integer("notes_count").description("The count of notes related to the person")
                        .required(false),
                    integer("followers_count").description("The count of followers related to the person")
                        .required(false),
                    string("name").description("The name of the person")
                        .required(false),
                    integer("lost_deals_count").description("The count of lost deals related with the item")
                        .required(false),
                    string("next_activity_time").description("The time of the next activity associated with the deal")
                        .required(false),
                    string("add_time")
                        .description("The date and time when the person was added/created. Format: YYYY-MM-DD HH:MM:SS")
                        .required(false),
                    integer("done_activities_count").description("The count of done activities related to the person")
                        .required(false),
                    object("related_objects")
                        .properties(object("user").properties(string("name").description("The name of the user")
                            .required(false), object("USER_ID").required(false),
                            integer("has_pic")
                                .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                                .required(false),
                            bool("active_flag").description("Whether the user is active or not")
                                .required(false),
                            integer("id").description("The ID of the user")
                                .required(false),
                            string("email").description("The email of the user")
                                .required(false),
                            string("pic_hash").description("The user picture hash")
                                .required(false))
                            .required(false))
                        .required(false),
                    bool("success").description("If the response is successful or not")
                        .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON)))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("success", true),
            Map.entry("data",
                Map.<String, Object>ofEntries(Map.entry("id", 1), Map.entry("company_id", 12),
                    Map.entry("owner_id",
                        Map.<String, Object>ofEntries(Map.entry("id", 123), Map.entry("name", "Jane Doe"),
                            Map.entry("email", "jane@pipedrive.com"), Map.entry("has_pic", 1),
                            Map.entry("pic_hash", "2611ace8ac6a3afe2f69ed56f9e08c6b"), Map.entry("active_flag", true),
                            Map.entry("value", 123))),
                    Map.entry("org_id",
                        Map.<String, Object>ofEntries(Map.entry("name", "Org Name"), Map.entry("people_count", 1),
                            Map.entry("owner_id", 123), Map.entry("address", "Mustamäe tee 3a, 10615 Tallinn"),
                            Map.entry("active_flag", true), Map.entry("cc_email", "org@pipedrivemail.com"),
                            Map.entry("value", 1234))),
                    Map.entry("name", "Will Smith"), Map.entry("first_name", "Will"), Map.entry("last_name", "Smith"),
                    Map.entry("open_deals_count", 2), Map.entry("related_open_deals_count", 2),
                    Map.entry("closed_deals_count", 3), Map.entry("related_closed_deals_count", 3),
                    Map.entry("participant_open_deals_count", 1), Map.entry("participant_closed_deals_count", 1),
                    Map.entry("email_messages_count", 1), Map.entry("activities_count", 1),
                    Map.entry("done_activities_count", 1), Map.entry("undone_activities_count", 2),
                    Map.entry("files_count", 2), Map.entry("notes_count", 2), Map.entry("followers_count", 3),
                    Map.entry("won_deals_count", 3), Map.entry("related_won_deals_count", 3),
                    Map.entry("lost_deals_count", 1), Map.entry("related_lost_deals_count", 1),
                    Map.entry("active_flag", true),
                    Map.entry("phone",
                        List.of(Map.<String, Object>ofEntries(Map.entry("value", 12345.0), Map.entry("primary", true),
                            Map.entry("label", "work")))),
                    Map.entry(
                        "email",
                        List.of(Map.<String, Object>ofEntries(
                            Map.entry("value", "12345@email.com"), Map.entry("primary", true),
                            Map.entry("label", "work")))),
                    Map.entry("primary_email", "12345@email.com"), Map.entry("first_char", "w"),
                    Map.entry("update_time", "2020-05-08 05:30:20"), Map.entry("add_time", "2017-10-18 13:23:07"),
                    Map.entry("visible_to", 3.0), Map.entry("marketing_status", "no_consent"),
                    Map.entry("picture_id", Map.<String, Object>ofEntries(Map.entry("item_type", "person"),
                        Map.entry("item_id", 25), Map.entry("active_flag", true),
                        Map.entry("add_time", "2020-09-08 08:17:52"), Map.entry("update_time", "0000-00-00 00:00:00"),
                        Map.entry("added_by_user_id", 967055),
                        Map.entry("pictures", Map.<String, Object>ofEntries(Map.entry("128",
                            "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg"),
                            Map.entry("512",
                                "https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg"))),
                        Map.entry("value", 4))),
                    Map.entry("next_activity_date", LocalDate.of(2019, 11, 29)),
                    Map.entry("next_activity_time", "11:30:00"), Map.entry("next_activity_id", 128),
                    Map.entry("last_activity_id", 34), Map.entry("last_activity_date", LocalDate.of(2019, 11, 28)),
                    Map.entry("last_incoming_mail_time", "2019-05-29 18:21:42"),
                    Map.entry("last_outgoing_mail_time", "2019-05-30 03:45:35"), Map.entry("label", 1),
                    Map.entry("org_name", "Organization name"), Map.entry("owner_name", "Jane Doe"),
                    Map.entry("cc_email", "org@pipedrivemail.com"))),
            Map.entry("related_objects", Map.<String, Object>ofEntries(Map.entry("user",
                Map.<String, Object>ofEntries(Map.entry("123", Map.<String, Object>ofEntries(Map.entry("id", 123),
                    Map.entry("name", "Jane Doe"), Map.entry("email", "jane@pipedrive.com"), Map.entry("has_pic", 1),
                    Map.entry("pic_hash", "2611ace8ac6a3afe2f69ed56f9e08c6b"), Map.entry("active_flag", true)))))))));
}
