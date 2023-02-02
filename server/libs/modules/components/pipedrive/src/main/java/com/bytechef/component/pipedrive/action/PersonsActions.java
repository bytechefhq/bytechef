
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

import static com.bytechef.hermes.component.RestComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.utils.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PersonsActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("deletePersons")
        .display(
            display("Delete multiple persons in bulk")
                .description(
                    "Marks multiple persons as deleted. After 30 days, the persons will be permanently deleted."))
        .metadata(
            Map.of(
                "requestMethod", "DELETE",
                "path", "/persons"

            ))
        .properties(string("ids").label("Ids")
            .description("The comma-separated IDs that will be deleted")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(object(null)
            .properties(
                object("data")
                    .properties(array("id").items(integer(null).description("The list of deleted persons IDs"))
                        .placeholder("Add")
                        .label("Id")
                        .description("The list of deleted persons IDs")
                        .required(false))
                    .label("Data")
                    .required(false),
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput("{\"success\":true,\"data\":{\"id\":[123,456]}}"),
        action("getPersons")
            .display(
                display("Get all persons")
                    .description("Returns all persons."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/persons"

                ))
            .properties(integer("user_id").label("User_id")
                .description(
                    "If supplied, only persons owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
                integer("filter_id").label("Filter_id")
                    .description("The ID of the filter to use")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                string("first_char").label("First_char")
                    .description(
                        "If supplied, only persons whose name starts with the specified letter will be returned (case insensitive)")
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
                            "type", PropertyType.QUERY)))
            .output(object(null)
                .properties(object("additional_data")
                    .properties(object("pagination").properties(integer("start").label("Start")
                        .description("Pagination start")
                        .required(false),
                        integer("limit").label("Limit")
                            .description("Items shown per page")
                            .required(false),
                        bool("more_items_in_collection").label("More_items_in_collection")
                            .description("Whether there are more list items in the collection than displayed")
                            .required(false),
                        integer("next_start").label("Next_start")
                            .description("Next pagination start")
                            .required(false))
                        .label("Pagination")
                        .description("Pagination details of the list")
                        .required(false))
                    .label("Additional_data")
                    .required(false),
                    array("data").items(object(null).properties(integer("related_closed_deals_count")
                        .label("Related_closed_deals_count")
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
                        array("email").items(object(null).properties(string("value").label("Value")
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
                        array("phone").items(object(null).properties(string("value").label("Value")
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
                            .description(
                                "The number of people connected with the organization that is associated with the item")
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
                            .description(
                                "The date and time when the person was added/created. Format: YYYY-MM-DD HH:MM:SS")
                            .required(false),
                        integer("done_activities_count").label("Done_activities_count")
                            .description("The count of done activities related to the person")
                            .required(false))
                        .description("The array of persons"))
                        .placeholder("Add")
                        .label("Data")
                        .description("The array of persons")
                        .required(false),
                    object("related_objects").properties(object("organization").properties(string("name").label("Name")
                        .description("The name of the organization associated with the item")
                        .required(false),
                        integer("id").label("Id")
                            .description("The ID of the organization associated with the item")
                            .required(false),
                        string("address").label("Address")
                            .description("The address of the organization")
                            .required(false),
                        integer("people_count").label("People_count")
                            .description(
                                "The number of people connected with the organization that is associated with the item")
                            .required(false),
                        integer("owner_id").label("Owner_id")
                            .description("The ID of the owner of the organization that is associated with the item")
                            .required(false),
                        string("cc_email").label("Cc_email")
                            .description("The BCC email of the organization associated with the item")
                            .required(false))
                        .label("Organization")
                        .required(false),
                        object("user").properties(string("name").label("Name")
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
                            .required(false),
                        object("picture").properties(string("update_time").label("Update_time")
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
                                .required(false))
                            .label("Picture")
                            .description("The picture that is associated with the item")
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
                "{\"success\":true,\"data\":[{\"id\":1,\"company_id\":12,\"owner_id\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":123},\"org_id\":{\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":1234},\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345@email.com\",\"primary\":true,\"label\":\"work\"}],\"primary_email\":\"12345@email.com\",\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"marketing_status\":\"no_consent\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\"}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false,\"next_start\":100}},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}"),
        action("addPerson")
            .display(
                display("Add a person")
                    .description(
                        "Adds a new person. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the personFields and look for `key` values.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also accept and return the `data.marketing_status` field."))
            .metadata(
                Map.of(
                    "requestMethod", "POST",
                    "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                ))
            .properties(object(null).properties(string("marketing_status").label("Marketing_status")
                .description(
                    "If the person does not have a valid email address, then the marketing status is **not set** and `no_consent` is returned for the `marketing_status` value when the new person is created. If the change is forbidden, the status will remain unchanged for every call that tries to modify the marketing status. Please be aware that it is only allowed **once** to change the marketing status from an old status to a new one.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`no_consent`</td><td>The customer has not given consent to receive any marketing communications</td></tr><tr><td>`unsubscribed`</td><td>The customers have unsubscribed from ALL marketing communications</td></tr><tr><td>`subscribed`</td><td>The customers are subscribed and are counted towards marketing caps</td></tr><tr><td>`archived`</td><td>The customers with `subscribed` status can be moved to `archived` to save consent, but they are not paid for</td></tr></table>")
                .options(option("No_consent", "no_consent"), option("Unsubscribed", "unsubscribed"),
                    option("Subscribed", "subscribed"), option("Archived", "archived"))
                .required(false),
                array("phone").items(object(null).properties(string("value").label("Value")
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
                array("email").items(object(null).properties(string("value").label("Value")
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
            .output(object(null).properties(integer("related_closed_deals_count").label("Related_closed_deals_count")
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
                array("email").items(object(null).properties(string("value").label("Value")
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
                array("phone").items(object(null).properties(string("value").label("Value")
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
                    .description(
                        "The number of people connected with the organization that is associated with the item")
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
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":12,\"owner_id\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":123},\"org_id\":{\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":1234},\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345@email.com\",\"primary\":true,\"label\":\"work\"}],\"primary_email\":\"12345@email.com\",\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"marketing_status\":\"no_consent\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}}}}"),
        action("searchPersons")
            .display(
                display("Search persons")
                    .description(
                        "Searches all persons by name, email, phone, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope. Found persons can be filtered by organization ID."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/persons/search"

                ))
            .properties(string("term").label("Term")
                .description(
                    "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
                string("fields").label("Fields")
                    .description(
                        "A comma-separated string array. The fields to perform the search from. Defaults to all of them.")
                    .options(option("Custom_fields", "custom_fields"), option("Email", "email"),
                        option("Notes", "notes"), option("Phone", "phone"), option("Name", "name"))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                bool("exact_match").label("Exact_match")
                    .description(
                        "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                    .options(option("True", true), option("False", false))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                integer("organization_id").label("Organization_id")
                    .description(
                        "Will filter persons by the provided organization ID. The upper limit of found persons associated with the organization is 2000.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                string("include_fields").label("Include_fields")
                    .description("Supports including optional fields in the results which are not provided by default")
                    .options(option("Person.picture", "person.picture"))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                integer("start").label("Start")
                    .description(
                        "Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)))
            .output(
                object(null)
                    .properties(
                        object("data")
                            .properties(
                                array("items")
                                    .items(object(null).properties(number("result_score").label("Result_score")
                                        .description("Search result relevancy")
                                        .required(false),
                                        object("item").properties(integer("id").label("Id")
                                            .description("The ID of the person")
                                            .required(false),
                                            string("type").label("Type")
                                                .description("The type of the item")
                                                .required(false),
                                            string("name").label("Name")
                                                .description("The name of the person")
                                                .required(false),
                                            array("phones").items(string(null).description("An array of phone numbers"))
                                                .placeholder("Add")
                                                .label("Phones")
                                                .description("An array of phone numbers")
                                                .required(false),
                                            array("emails")
                                                .items(string(null).description("An array of email addresses"))
                                                .placeholder("Add")
                                                .label("Emails")
                                                .description("An array of email addresses")
                                                .required(false),
                                            integer("visible_to").label("Visible_to")
                                                .description("The visibility of the person")
                                                .required(false),
                                            object("owner").properties(integer("id").label("Id")
                                                .description("The ID of the owner of the person")
                                                .required(false))
                                                .label("Owner")
                                                .required(false),
                                            object("organization").properties(integer("id").label("Id")
                                                .description("The ID of the organization the person is associated with")
                                                .required(false),
                                                string("name").label("Name")
                                                    .description(
                                                        "The name of the organization the person is associated with")
                                                    .required(false))
                                                .label("Organization")
                                                .required(false),
                                            array("custom_fields").items(string(null).description("Custom fields"))
                                                .placeholder("Add")
                                                .label("Custom_fields")
                                                .description("Custom fields")
                                                .required(false),
                                            array("notes").items(string(null).description("An array of notes"))
                                                .placeholder("Add")
                                                .label("Notes")
                                                .description("An array of notes")
                                                .required(false))
                                            .label("Item")
                                            .required(false))
                                        .description("The array of found items"))
                                    .placeholder("Add")
                                    .label("Items")
                                    .description("The array of found items")
                                    .required(false))
                            .label("Data")
                            .required(false),
                        bool("success").label("Success")
                            .description("If the response is successful or not")
                            .required(false),
                        object("additional_data")
                            .properties(object("pagination").properties(integer("start").label("Start")
                                .description("Pagination start")
                                .required(false),
                                integer("limit").label("Limit")
                                    .description("Items shown per page")
                                    .required(false),
                                bool("more_items_in_collection").label("More_items_in_collection")
                                    .description("Whether there are more list items in the collection than displayed")
                                    .required(false),
                                integer("next_start").label("Next_start")
                                    .description("Next pagination start")
                                    .required(false))
                                .label("Pagination")
                                .description("Pagination details of the list")
                                .required(false))
                            .label("Additional_data")
                            .required(false))
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON)))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.5092,\"item\":{\"id\":1,\"type\":\"person\",\"name\":\"Jane Doe\",\"phones\":[\"+372 555555555\"],\"emails\":[\"jane@pipedrive.com\"],\"visible_to\":3,\"owner\":{\"id\":1},\"organization\":{\"id\":1,\"name\":\"Organization name\",\"address\":null},\"custom_fields\":[],\"notes\":[]}}]},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}"),
        action("deletePerson")
            .display(
                display("Delete a person")
                    .description("Marks a person as deleted. After 30 days, the person will be permanently deleted."))
            .metadata(
                Map.of(
                    "requestMethod", "DELETE",
                    "path", "/persons/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the person")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
            .output(object(null).properties(object("data").properties(integer("id").label("Id")
                .description("The ID of the deleted person")
                .required(false))
                .label("Data")
                .required(false),
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.JSON)))
            .exampleOutput("{\"success\":true,\"data\":{\"id\":12}}"),
        action("getPerson")
            .display(
                display("Get details of a person")
                    .description(
                        "Returns the details of a person. Note that this also returns some additional fields which are not present when asking for all persons. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of personFields.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also return the `data.marketing_status` field."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/persons/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the person")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)))
            .output(object(null).properties(
                object("additional_data").properties(string("dropbox_email").label("Dropbox_email")
                    .description("Dropbox email for the person")
                    .required(false))
                    .label("Additional_data")
                    .required(false),
                integer("related_closed_deals_count").label("Related_closed_deals_count")
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
                array("email").items(object(null).properties(string("value").label("Value")
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
                array("phone").items(object(null).properties(string("value").label("Value")
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
                    .description(
                        "The number of people connected with the organization that is associated with the item")
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
                object("related_objects").properties(object("organization").properties(string("name").label("Name")
                    .description("The name of the organization associated with the item")
                    .required(false),
                    integer("id").label("Id")
                        .description("The ID of the organization associated with the item")
                        .required(false),
                    string("address").label("Address")
                        .description("The address of the organization")
                        .required(false),
                    integer("people_count").label("People_count")
                        .description(
                            "The number of people connected with the organization that is associated with the item")
                        .required(false),
                    integer("owner_id").label("Owner_id")
                        .description("The ID of the owner of the organization that is associated with the item")
                        .required(false),
                    string("cc_email").label("Cc_email")
                        .description("The BCC email of the organization associated with the item")
                        .required(false))
                    .label("Organization")
                    .required(false),
                    object("user").properties(string("name").label("Name")
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
                        .required(false),
                    object("picture").properties(string("update_time").label("Update_time")
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
                            .required(false))
                        .label("Picture")
                        .description("The picture that is associated with the item")
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
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":12,\"owner_id\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":123},\"org_id\":{\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":1234},\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345@email.com\",\"primary\":true,\"label\":\"work\"}],\"primary_email\":\"12345@email.com\",\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"marketing_status\":\"no_consent\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"additional_data\":{\"dropbox_email\":\"test@email.com\"},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}"),
        action("updatePerson")
            .display(
                display("Update a person")
                    .description(
                        "Updates the properties of a person. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/updating-a-person\" target=\"_blank\" rel=\"noopener noreferrer\">updating a person</a>.<br>If a company uses the [Campaigns product](https://pipedrive.readme.io/docs/campaigns-in-pipedrive-api), then this endpoint will also accept and return the `data.marketing_status` field."))
            .metadata(
                Map.of(
                    "requestMethod", "PUT",
                    "path", "/persons/{id}", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the person")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
                object(null).properties(string("marketing_status").label("Marketing_status")
                    .description(
                        "If the person does not have a valid email address, then the marketing status is **not set** and `no_consent` is returned for the `marketing_status` value when the new person is created. If the change is forbidden, the status will remain unchanged for every call that tries to modify the marketing status. Please be aware that it is only allowed **once** to change the marketing status from an old status to a new one.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`no_consent`</td><td>The customer has not given consent to receive any marketing communications</td></tr><tr><td>`unsubscribed`</td><td>The customers have unsubscribed from ALL marketing communications</td></tr><tr><td>`subscribed`</td><td>The customers are subscribed and are counted towards marketing caps</td></tr><tr><td>`archived`</td><td>The customers with `subscribed` status can be moved to `archived` to save consent, but they are not paid for</td></tr></table>")
                    .options(option("No_consent", "no_consent"), option("Unsubscribed", "unsubscribed"),
                        option("Subscribed", "subscribed"), option("Archived", "archived"))
                    .required(false),
                    array("phone").items(object(null).properties(string("value").label("Value")
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
                    array("email").items(object(null).properties(string("value").label("Value")
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
            .output(object(null).properties(integer("related_closed_deals_count").label("Related_closed_deals_count")
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
                array("email").items(object(null).properties(string("value").label("Value")
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
                array("phone").items(object(null).properties(string("value").label("Value")
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
                    .description(
                        "The number of people connected with the organization that is associated with the item")
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
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":12,\"owner_id\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":123},\"org_id\":{\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":1234},\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345@email.com\",\"primary\":true,\"label\":\"work\"}],\"primary_email\":\"12345@email.com\",\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"marketing_status\":\"no_consent\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}}}}"),
        action("getPersonDeals")
            .display(
                display("List deals associated with a person")
                    .description("Lists deals associated with a person."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/persons/{id}/deals"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the person")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
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
                string("status").label("Status")
                    .description(
                        "Only fetch deals with a specific status. If omitted, all not deleted deals are returned. If set to deleted, deals that have been deleted up to 30 days ago will be included.")
                    .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                        option("Deleted", "deleted"), option("All_not_deleted", "all_not_deleted"))
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
                            "type", PropertyType.QUERY)))
            .output(object(null).properties(object("additional_data").properties(integer("start").label("Start")
                .description("Pagination start")
                .required(false),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false),
                bool("more_items_in_collection").label("More_items_in_collection")
                    .description("If there are more list items in the collection than displayed or not")
                    .required(false))
                .label("Additional_data")
                .description("The additional data of the list")
                .required(false),
                array("data").items(object(null).properties(
                    integer("email_messages_count").label("Email_messages_count")
                        .description("The number of emails associated with the deal")
                        .required(false),
                    string("cc_email").label("Cc_email")
                        .description("The BCC email of the deal")
                        .required(false),
                    integer("products_count").label("Products_count")
                        .description("The number of products associated with the deal")
                        .required(false),
                    string("next_activity_date").label("Next_activity_date")
                        .description("The date of the next activity associated with the deal")
                        .required(false),
                    string("next_activity_type").label("Next_activity_type")
                        .description("The type of the next activity associated with the deal")
                        .required(false),
                    string("next_activity_duration").label("Next_activity_duration")
                        .description("The duration of the next activity associated with the deal")
                        .required(false),
                    integer("id").label("Id")
                        .description("The ID of the deal")
                        .required(false),
                    string("name").label("Name")
                        .description("The name of the person associated with the deal")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the associated person is active or not")
                        .required(false),
                    array("phone").items(object(null).properties(string("label").label("Label")
                        .description("The type of the phone number")
                        .required(false),
                        string("value").label("Value")
                            .description("The phone number of the person associated with the deal")
                            .required(false),
                        bool("primary").label("Primary")
                            .description("If this is the primary phone number or not")
                            .required(false))
                        .description("The phone numbers of the person associated with the deal"))
                        .placeholder("Add")
                        .label("Phone")
                        .description("The phone numbers of the person associated with the deal")
                        .required(false),
                    integer("value").label("Value")
                        .description("The ID of the person associated with the deal")
                        .required(false),
                    array("email").items(object(null).properties(string("label").label("Label")
                        .description("The type of the email")
                        .required(false),
                        string("value").label("Value")
                            .description("The email of the associated person")
                            .required(false),
                        bool("primary").label("Primary")
                            .description("If this is the primary email or not")
                            .required(false))
                        .description("The emails of the person associated with the deal"))
                        .placeholder("Add")
                        .label("Email")
                        .description("The emails of the person associated with the deal")
                        .required(false),
                    integer("owner_id").label("Owner_id")
                        .description("The ID of the owner of the person that is associated with the deal")
                        .required(false),
                    object("creator_user_id").properties(integer("id").label("Id")
                        .description("The ID of the deal creator")
                        .required(false),
                        string("name").label("Name")
                            .description("The name of the deal creator")
                            .required(false),
                        string("email").label("Email")
                            .description("The email of the deal creator")
                            .required(false),
                        bool("has_pic").label("Has_pic")
                            .description("If the creator has a picture or not")
                            .required(false),
                        string("pic_hash").label("Pic_hash")
                            .description("The creator picture hash")
                            .required(false),
                        bool("active_flag").label("Active_flag")
                            .description("Whether the creator is active or not")
                            .required(false),
                        integer("value").label("Value")
                            .description("The ID of the deal creator")
                            .required(false))
                        .label("Creator_user_id")
                        .description("The creator of the deal")
                        .required(false),
                    date("expected_close_date").label("Expected_close_date")
                        .description("The expected close date of the deal")
                        .required(false),
                    integer("participants_count").label("Participants_count")
                        .description("The number of participants associated with the deal")
                        .required(false),
                    string("owner_name").label("Owner_name")
                        .description("The name of the deal owner")
                        .required(false),
                    integer("stage_id").label("Stage_id")
                        .description("The ID of the deal stage")
                        .required(false),
                    number("probability").label("Probability")
                        .description("The success probability percentage of the deal")
                        .required(false),
                    integer("undone_activities_count").label("Undone_activities_count")
                        .description("The number of incomplete activities associated with the deal")
                        .required(false),
                    bool("active").label("Active")
                        .description("Whether the deal is active or not")
                        .required(false),
                    string("last_activity_date").label("Last_activity_date")
                        .description("The date of the last activity associated with the deal")
                        .required(false),
                    string("person_name").label("Person_name")
                        .description("The name of the person associated with the deal")
                        .required(false),
                    string("close_time").label("Close_time")
                        .description("The date and time of closing the deal")
                        .required(false),
                    integer("next_activity_id").label("Next_activity_id")
                        .description("The ID of the next activity associated with the deal")
                        .required(false),
                    string("weighted_value_currency").label("Weighted_value_currency")
                        .description("The currency associated with the deal")
                        .required(false),
                    bool("org_hidden").label("Org_hidden")
                        .description("If the organization that is associated with the deal is hidden or not")
                        .required(false),
                    integer("stage_order_nr").label("Stage_order_nr")
                        .description("The order number of the deal stage associated with the deal")
                        .required(false),
                    string("next_activity_subject").label("Next_activity_subject")
                        .description("The subject of the next activity associated with the deal")
                        .required(false),
                    string("rotten_time").label("Rotten_time")
                        .description("The date and time of changing the deal status as rotten")
                        .required(false),
                    string("name").label("Name")
                        .description("The name of the user")
                        .required(false),
                    bool("has_pic").label("Has_pic")
                        .description("If the user has a picture or not")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the user is active or not")
                        .required(false),
                    integer("id").label("Id")
                        .description("The ID of the user")
                        .required(false),
                    integer("value").label("Value")
                        .description("The ID of the user")
                        .required(false),
                    string("email").label("Email")
                        .description("The email of the user")
                        .required(false),
                    string("pic_hash").label("Pic_hash")
                        .description("The user picture hash")
                        .required(false),
                    string("visible_to").label("Visible_to")
                        .description("The visibility of the deal")
                        .required(false),
                    string("address").label("Address")
                        .description("The address of the organization that is associated with the deal")
                        .required(false),
                    integer("owner_id").label("Owner_id")
                        .description("The ID of the owner of the organization that is associated with the deal")
                        .required(false),
                    string("cc_email").label("Cc_email")
                        .description("The BCC email of the organization associated with the deal")
                        .required(false),
                    string("name").label("Name")
                        .description("The name of the organization associated with the deal")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the associated organization is active or not")
                        .required(false),
                    integer("people_count").label("People_count")
                        .description(
                            "The number of people connected with the organization that is associated with the deal")
                        .required(false),
                    integer("value").label("Value")
                        .description("The ID of the organization associated with the deal")
                        .required(false),
                    integer("notes_count").label("Notes_count")
                        .description("The number of notes associated with the deal")
                        .required(false),
                    string("next_activity_time").label("Next_activity_time")
                        .description("The time of the next activity associated with the deal")
                        .required(false),
                    string("formatted_value").label("Formatted_value")
                        .description("The deal value formatted with selected currency. E.g. US$500")
                        .required(false),
                    string("status").label("Status")
                        .description("The status of the deal")
                        .required(false),
                    string("formatted_weighted_value").label("Formatted_weighted_value")
                        .description("The weighted_value formatted with selected currency. E.g. US$500")
                        .required(false),
                    string("first_won_time").label("First_won_time")
                        .description("The date and time of the first time changing the deal status as won")
                        .required(false),
                    string("last_outgoing_mail_time").label("Last_outgoing_mail_time")
                        .description("The date and time of the last outgoing email associated with the deal")
                        .required(false),
                    string("title").label("Title")
                        .description("The title of the deal")
                        .required(false),
                    integer("last_activity_id").label("Last_activity_id")
                        .description("The ID of the last activity associated with the deal")
                        .required(false),
                    string("update_time").label("Update_time")
                        .description("The last updated date and time of the deal")
                        .required(false),
                    integer("activities_count").label("Activities_count")
                        .description("The number of activities associated with the deal")
                        .required(false),
                    integer("pipeline_id").label("Pipeline_id")
                        .description("The ID of pipeline associated with the deal")
                        .required(false),
                    string("lost_time").label("Lost_time")
                        .description("The date and time of changing the deal status as lost")
                        .required(false),
                    string("currency").label("Currency")
                        .description("The currency associated with the deal")
                        .required(false),
                    number("weighted_value").label("Weighted_value")
                        .description(
                            "Probability times deal value. Probability can either be deal probability or if not set, then stage probability.")
                        .required(false),
                    string("org_name").label("Org_name")
                        .description("The name of the organization associated with the deal")
                        .required(false),
                    number("value").label("Value")
                        .description("The value of the deal")
                        .required(false),
                    string("next_activity_note").label("Next_activity_note")
                        .description("The note of the next activity associated with the deal")
                        .required(false),
                    bool("person_hidden").label("Person_hidden")
                        .description("If the person that is associated with the deal is hidden or not")
                        .required(false),
                    integer("files_count").label("Files_count")
                        .description("The number of files associated with the deal")
                        .required(false),
                    string("last_incoming_mail_time").label("Last_incoming_mail_time")
                        .description("The date and time of the last incoming email associated with the deal")
                        .required(false),
                    integer("label").label("Label")
                        .description("The label assigned to the deal")
                        .required(false),
                    string("lost_reason").label("Lost_reason")
                        .description("The reason for losing the deal")
                        .required(false),
                    bool("deleted").label("Deleted")
                        .description("Whether the deal is deleted or not")
                        .required(false),
                    string("won_time").label("Won_time")
                        .description("The date and time of changing the deal status as won")
                        .required(false),
                    integer("followers_count").label("Followers_count")
                        .description("The number of followers associated with the deal")
                        .required(false),
                    string("stage_change_time").label("Stage_change_time")
                        .description("The last updated date and time of the deal stage")
                        .required(false),
                    string("add_time").label("Add_time")
                        .description("The creation date and time of the deal")
                        .required(false),
                    integer("done_activities_count").label("Done_activities_count")
                        .description("The number of completed activities associated with the deal")
                        .required(false))
                    .description("The array of deals"))
                    .placeholder("Add")
                    .label("Data")
                    .description("The array of deals")
                    .required(false),
                object("related_objects").properties(
                    object("organization").properties(string("address").label("Address")
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
                        integer("id").label("Id")
                            .description("The ID of the organization associated with the item")
                            .required(false),
                        integer("people_count").label("People_count")
                            .description(
                                "The number of people connected with the organization that is associated with the item")
                            .required(false))
                        .label("Organization")
                        .required(false),
                    object("person").properties(string("name").label("Name")
                        .description("The name of the person associated with the item")
                        .required(false),
                        bool("active_flag").label("Active_flag")
                            .description("Whether the associated person is active or not")
                            .required(false),
                        integer("id").label("Id")
                            .description("The ID of the person associated with the item")
                            .required(false),
                        array("phone").items(object(null).properties(string("label").label("Label")
                            .description("The type of the phone number")
                            .required(false),
                            string("value").label("Value")
                                .description("The phone number of the person associated with the item")
                                .required(false),
                            bool("primary").label("Primary")
                                .description("Whether this is the primary phone number or not")
                                .required(false))
                            .description("The phone numbers of the person associated with the item"))
                            .placeholder("Add")
                            .label("Phone")
                            .description("The phone numbers of the person associated with the item")
                            .required(false),
                        array("email").items(object(null).properties(string("label").label("Label")
                            .description("The type of the email")
                            .required(false),
                            string("value").label("Value")
                                .description("The email of the associated person")
                                .required(false),
                            bool("primary").label("Primary")
                                .description("Whether this is the primary email or not")
                                .required(false))
                            .description("The emails of the person associated with the item"))
                            .placeholder("Add")
                            .label("Email")
                            .description("The emails of the person associated with the item")
                            .required(false),
                        integer("owner_id").label("Owner_id")
                            .description("The ID of the owner of the person that is associated with the item")
                            .required(false))
                        .label("Person")
                        .required(false),
                    object("user").properties(string("name").label("Name")
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
                        .required(false),
                    object("stage").properties(integer("id").label("Id")
                        .description("The ID of the stage")
                        .required(false),
                        integer("order_nr").label("Order_nr")
                            .description("Defines the order of the stage")
                            .required(false),
                        string("name").label("Name")
                            .description("The name of the stage")
                            .required(false),
                        bool("active_flag").label("Active_flag")
                            .description("Whether the stage is active or deleted")
                            .required(false),
                        integer("deal_probability").label("Deal_probability")
                            .description(
                                "The success probability percentage of the deal. Used/shown when the deal weighted values are used.")
                            .required(false),
                        integer("pipeline_id").label("Pipeline_id")
                            .description("The ID of the pipeline to add the stage to")
                            .required(false),
                        bool("rotten_flag").label("Rotten_flag")
                            .description("Whether deals in this stage can become rotten")
                            .options(option("True", true), option("False", false))
                            .required(false),
                        integer("rotten_days").label("Rotten_days")
                            .description(
                                "The number of days the deals not updated in this stage would become rotten. Applies only if the `rotten_flag` is set.")
                            .required(false),
                        string("add_time").label("Add_time")
                            .description("The stage creation time. Format: YYYY-MM-DD HH:MM:SS.")
                            .required(false),
                        string("update_time").label("Update_time")
                            .description("The stage update time. Format: YYYY-MM-DD HH:MM:SS.")
                            .required(false))
                        .label("Stage")
                        .required(false),
                    object("pipeline").properties(integer("id").label("Id")
                        .description("The ID of the pipeline")
                        .required(false),
                        string("name").label("Name")
                            .description("The name of the pipeline")
                            .required(false),
                        string("url_title").label("Url_title")
                            .description("The pipeline title displayed in the URL")
                            .required(false),
                        integer("order_nr").label("Order_nr")
                            .description(
                                "Defines the order of pipelines. First order (`order_nr=0`) is the default pipeline.")
                            .required(false),
                        bool("active").label("Active")
                            .description("Whether this pipeline will be made inactive (hidden) or active")
                            .required(false),
                        bool("deal_probability").label("Deal_probability")
                            .description("Whether deal probability is disabled or enabled for this pipeline")
                            .required(false),
                        string("add_time").label("Add_time")
                            .description("The pipeline creation time. Format: YYYY-MM-DD HH:MM:SS.")
                            .required(false),
                        string("update_time").label("Update_time")
                            .description("The pipeline update time. Format: YYYY-MM-DD HH:MM:SS.")
                            .required(false))
                        .label("Pipeline")
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
                "{\"success\":true,\"data\":[{\"id\":1,\"creator_user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"person_id\":{\"active_flag\":true,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"37244499911\",\"primary\":true}],\"value\":1101},\"org_id\":{\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":5},\"stage_id\":2,\"title\":\"Deal One\",\"value\":5000,\"currency\":\"EUR\",\"add_time\":\"2019-05-29 04:21:51\",\"update_time\":\"2019-11-28 16:19:50\",\"stage_change_time\":\"2019-11-28 15:41:22\",\"active\":true,\"deleted\":false,\"status\":\"open\",\"probability\":null,\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":null,\"last_activity_date\":null,\"lost_reason\":null,\"visible_to\":\"1\",\"close_time\":null,\"pipeline_id\":1,\"won_time\":\"2019-11-27 11:40:36\",\"first_won_time\":\"2019-11-27 11:40:36\",\"lost_time\":\"2019-11-27 11:40:36\",\"products_count\":0,\"files_count\":0,\"notes_count\":2,\"followers_count\":0,\"email_messages_count\":4,\"activities_count\":1,\"done_activities_count\":0,\"undone_activities_count\":1,\"participants_count\":1,\"expected_close_date\":\"2019-06-29\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":11,\"stage_order_nr\":2,\"person_name\":\"Person\",\"org_name\":\"Organization\",\"next_activity_subject\":\"Call\",\"next_activity_type\":\"call\",\"next_activity_duration\":\"00:30:00\",\"next_activity_note\":\"Note content\",\"formatted_value\":\"€5,000\",\"weighted_value\":5000,\"formatted_weighted_value\":\"€5,000\",\"weighted_value_currency\":\"EUR\",\"rotten_time\":null,\"owner_name\":\"Creator\",\"cc_email\":\"company+deal1@pipedrivemail.com\",\"org_hidden\":false,\"person_hidden\":false}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":true}},\"related_objects\":{\"user\":{\"8877\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"person\":{\"1101\":{\"active_flag\":true,\"id\":1101,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"stage\":{\"2\":{\"id\":2,\"company_id\":123,\"order_nr\":1,\"name\":\"Stage Name\",\"active_flag\":true,\"deal_probability\":100,\"pipeline_id\":1,\"rotten_flag\":false,\"rotten_days\":null,\"add_time\":\"2015-12-08 13:54:06\",\"update_time\":\"2015-12-08 13:54:06\",\"pipeline_name\":\"Pipeline\",\"pipeline_deal_probability\":true}},\"pipeline\":{\"1\":{\"id\":1,\"name\":\"Pipeline\",\"url_title\":\"Pipeline\",\"order_nr\":0,\"active\":true,\"deal_probability\":true,\"add_time\":\"2015-12-08 10:00:24\",\"update_time\":\"2015-12-08 10:00:24\"}}}}"),
        action("mergePersons")
            .display(
                display("Merge two persons")
                    .description(
                        "Merges a person with another person. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/merging-two-persons\" target=\"_blank\" rel=\"noopener noreferrer\">merging two persons</a>."))
            .metadata(
                Map.of(
                    "requestMethod", "PUT",
                    "path", "/persons/{id}/merge", "bodyContentType", BodyContentType.JSON, "mimeType",
                    "application/json"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the person")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
                object(null).properties(integer("merge_with_id").label("Merge_with_id")
                    .description(
                        "The ID of the person that will not be overwritten. This person’s data will be prioritized in case of conflict with the other person.")
                    .required(true))
                    .metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
            .output(object(null).properties(integer("related_closed_deals_count").label("Related_closed_deals_count")
                .description("The count of related closed deals related with the item")
                .required(false),
                integer("email_messages_count").label("Email_messages_count")
                    .description("The count of email messages related to the person")
                    .required(false),
                string("cc_email").label("Cc_email")
                    .description("The BCC email associated with the person")
                    .required(false),
                integer("owner_id").label("Owner_id")
                    .description("The ID of the owner related to the person")
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
                integer("participant_open_deals_count").label("Participant_open_deals_count")
                    .description("The count of open participant deals related with the item")
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
                array("email").items(object(null).properties(string("value").label("Value")
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
                integer("merge_what_id").label("Merge_what_id")
                    .description("The ID of the person with what the main person was merged")
                    .required(false),
                array("phone").items(object(null).properties(string("value").label("Value")
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
                integer("org_id").label("Org_id")
                    .description("The ID of the organization related to the person")
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
                integer("participant_closed_deals_count").label("Participant_closed_deals_count")
                    .description("The count of closed participant deals related with the item")
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
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.JSON)))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":12,\"owner_id\":123,\"org_id\":123,\"name\":\"Will Smith\",\"first_name\":\"Will\",\"last_name\":\"Smith\",\"open_deals_count\":2,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":3,\"participant_open_deals_count\":1,\"participant_closed_deals_count\":1,\"email_messages_count\":1,\"activities_count\":1,\"done_activities_count\":1,\"undone_activities_count\":2,\"files_count\":2,\"notes_count\":2,\"followers_count\":3,\"won_deals_count\":3,\"related_won_deals_count\":3,\"lost_deals_count\":1,\"related_lost_deals_count\":1,\"active_flag\":true,\"phone\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"email\":[{\"value\":\"12345\",\"primary\":true,\"label\":\"work\"}],\"first_char\":\"w\",\"update_time\":\"2020-05-08 05:30:20\",\"add_time\":\"2017-10-18 13:23:07\",\"visible_to\":\"3\",\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"},\"value\":4},\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":1,\"org_name\":\"Organization name\",\"owner_name\":\"Jane Doe\",\"cc_email\":\"org@pipedrivemail.com\",\"merge_what_id\":456}}"));
}
