
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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
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
public class AddPersonAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addPerson")
        .display(
            display("Add a person")
                .description(
                    "Adds a new person. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the personFields and look for `key` values.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also accept and return the `data.marketing_status` field."))
        .metadata(
            Map.of(
                "requestMethod", "POST",
                "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object().properties(string("marketing_status").label("Marketing_status")
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
                .placeholder("Add")
                .label("Phone")
                .description(
                    "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required.")
                .required(false),
            string("visible_to").label("Visible_to")
                .description(
                    "The visibility of the person. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                .required(false),
            integer("owner_id").label("Owner_id")
                .description(
                    "The ID of the user who will be marked as the owner of this person. When omitted, the authorized user ID will be used.")
                .required(false),
            integer("org_id").label("Org_id")
                .description("The ID of the organization this person will belong to")
                .required(false),
            string("name").label("Name")
                .description("The name of the person")
                .required(false),
            string("add_time").label("Add_time")
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
                .placeholder("Add")
                .label("Email")
                .description(
                    "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" }]`. Please note that only `value` is required.")
                .required(false))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(integer("related_closed_deals_count").label("Related_closed_deals_count")
            .description("The count of related closed deals related with the item")
            .required(false),
            integer("email_messages_count").label("Email_messages_count")
                .description("The count of email messages related to the person")
                .required(false),
            string("cc_email").label("Cc_email")
                .description("The BCC email associated with the person")
                .required(false),
            string("name").label("Name")
                .description("The name of the user")
                .required(false),
            integer("has_pic").label("Has_pic")
                .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("Whether the user is active or not")
                .required(false),
            integer("id").label("Id")
                .description("The ID of the user")
                .required(false),
            integer("value").label("Value")
                .description("The ID of the owner")
                .required(false),
            string("email").label("Email")
                .description("The email of the user")
                .required(false),
            string("pic_hash").label("Pic_hash")
                .description("The user picture hash")
                .required(false),
            integer("open_deals_count").label("Open_deals_count")
                .description("The count of open deals related with the item")
                .required(false),
            string("last_outgoing_mail_time").label("Last_outgoing_mail_time")
                .description("The date and time of the last outgoing email associated with the person")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("Whether the person is active or not")
                .required(false),
            string("update_time").label("Update_time")
                .description("The update time of the picture")
                .required(false),
            integer("added_by_user_id").label("Added_by_user_id")
                .description("The ID of the user who added the picture")
                .required(false),
            integer("item_id").label("Item_id")
                .description("The ID of related item")
                .required(false),
            string("item_type").label("Item_type")
                .description("The type of item the picture is related to")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("Whether the associated picture is active or not")
                .required(false),
            integer("id").label("Id")
                .description("The ID of the picture associated with the item")
                .required(false),
            string("add_time").label("Add_time")
                .description("The add time of the picture")
                .required(false),
            object("pictures").properties(string("128").label("128")
                .description("The URL of the 128*128 picture")
                .required(false),
                string("512").label("512")
                    .description("The URL of the 512*512 picture")
                    .required(false))
                .label("Pictures")
                .required(false),
            integer("last_activity_id").label("Last_activity_id")
                .description("The ID of the last activity associated with the deal")
                .required(false),
            string("next_activity_date").label("Next_activity_date")
                .description("The date of the next activity associated with the deal")
                .required(false),
            string("update_time").label("Update_time")
                .description("The last updated date and time of the person. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            integer("activities_count").label("Activities_count")
                .description("The count of activities related to the person")
                .required(false),
            integer("id").label("Id")
                .description("The ID of the person")
                .required(false),
            string("org_name").label("Org_name")
                .description("The name of the organization associated with the person")
                .required(false),
            string("first_name").label("First_name")
                .description("The first name of the person")
                .required(false),
            array("email").items(object().properties(string("value").label("Value")
                .description("Email")
                .required(false),
                bool("primary").label("Primary")
                    .description("Boolean that indicates if email is primary for the person or not")
                    .required(false),
                string("label").label("Label")
                    .description(
                        "The label that indicates the type of the email. (Possible values - work, home or other)")
                    .required(false))
                .description(
                    "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" } ]`. Please note that only `value` is required."))
                .placeholder("Add")
                .label("Email")
                .description(
                    "An email address as a string or an array of email objects related to the person. The structure of the array is as follows: `[{ \"value\": \"mail@example.com\", \"primary\": \"true\", \"label\": \"main\" } ]`. Please note that only `value` is required.")
                .required(false),
            integer("won_deals_count").label("Won_deals_count")
                .description("The count of won deals related with the item")
                .required(false),
            string("owner_name").label("Owner_name")
                .description("The name of the owner associated with the person")
                .required(false),
            integer("files_count").label("Files_count")
                .description("The count of files related to the person")
                .required(false),
            integer("company_id").label("Company_id")
                .description("The ID of the company related to the person")
                .required(false),
            integer("related_won_deals_count").label("Related_won_deals_count")
                .description("The count of related won deals related with the item")
                .required(false),
            string("last_incoming_mail_time").label("Last_incoming_mail_time")
                .description("The date and time of the last incoming email associated with the person")
                .required(false),
            string("first_char").label("First_char")
                .description("The first letter of the name of the person")
                .required(false),
            integer("undone_activities_count").label("Undone_activities_count")
                .description("The count of undone activities related to the person")
                .required(false),
            integer("closed_deals_count").label("Closed_deals_count")
                .description("The count of closed deals related with the item")
                .required(false),
            string("last_name").label("Last_name")
                .description("The last name of the person")
                .required(false),
            string("last_activity_date").label("Last_activity_date")
                .description("The date of the last activity associated with the deal")
                .required(false),
            integer("label").label("Label")
                .description("The label assigned to the person")
                .required(false),
            integer("related_open_deals_count").label("Related_open_deals_count")
                .description("The count of related open deals related with the item")
                .required(false),
            integer("related_lost_deals_count").label("Related_lost_deals_count")
                .description("The count of related lost deals related with the item")
                .required(false),
            integer("next_activity_id").label("Next_activity_id")
                .description("The ID of the next activity associated with the deal")
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
                .placeholder("Add")
                .label("Phone")
                .description(
                    "A phone number supplied as a string or an array of phone objects related to the person. The structure of the array is as follows: `[{ \"value\": \"12345\", \"primary\": \"true\", \"label\": \"mobile\" }]`. Please note that only `value` is required.")
                .required(false),
            string("visible_to").label("Visible_to")
                .description("The visibility group ID of who can see the person")
                .required(false),
            string("address").label("Address")
                .description("The address of the organization")
                .required(false),
            integer("owner_id").label("Owner_id")
                .description("The ID of the owner of the organization that is associated with the item")
                .required(false),
            string("cc_email").label("Cc_email")
                .description("The BCC email of the organization associated with the item")
                .required(false),
            string("name").label("Name")
                .description("The name of the organization associated with the item")
                .required(false),
            bool("active_flag").label("Active_flag")
                .description("Whether the associated organization is active or not")
                .required(false),
            integer("people_count").label("People_count")
                .description("The number of people connected with the organization that is associated with the item")
                .required(false),
            integer("value").label("Value")
                .description("The ID of the organization")
                .required(false),
            integer("notes_count").label("Notes_count")
                .description("The count of notes related to the person")
                .required(false),
            integer("followers_count").label("Followers_count")
                .description("The count of followers related to the person")
                .required(false),
            string("name").label("Name")
                .description("The name of the person")
                .required(false),
            integer("lost_deals_count").label("Lost_deals_count")
                .description("The count of lost deals related with the item")
                .required(false),
            string("next_activity_time").label("Next_activity_time")
                .description("The time of the next activity associated with the deal")
                .required(false),
            string("add_time").label("Add_time")
                .description("The date and time when the person was added/created. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            integer("done_activities_count").label("Done_activities_count")
                .description("The count of done activities related to the person")
                .required(false),
            object("related_objects").properties(object("user").properties(string("name").label("Name")
                .description("The name of the user")
                .required(false),
                object("USER_ID").label("USER_ID")
                    .required(false),
                integer("has_pic").label("Has_pic")
                    .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the user is active or not")
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the user")
                    .required(false),
                string("email").label("Email")
                    .description("The email of the user")
                    .required(false),
                string("pic_hash").label("Pic_hash")
                    .description("The user picture hash")
                    .required(false))
                .label("User")
                .required(false))
                .label("Related_objects")
                .required(false),
            bool("success").label("Success")
                .description("If the response is successful or not")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"id\":1,\"company_id\":12,\"owner_id\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":123},\"org_id\":{\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":1234},\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345@email.com\",\"primary\":true,\"label\":\"work\"}],\"primary_email\":\"12345@email.com\",\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"marketing_status\":\"no_consent\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}}}}");
}
