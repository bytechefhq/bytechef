
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
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
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
public class OrganizationsActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("deleteOrganizations")
        .display(
            display("Delete multiple organizations in bulk")
                .description(
                    "Marks multiple organizations as deleted. After 30 days, the organizations will be permanently deleted."))
        .metadata(
            Map.of(
                "requestMethod", "DELETE",
                "path", "/organizations"

            ))
        .properties(string("ids").label("Ids")
            .description("The comma-separated IDs that will be deleted")
            .required(true)
            .metadata(
                Map.of(
                    "type", "QUERY")))
        .output(object(null).properties(bool("success").label("Success")
            .description("If the request was successful or not")
            .required(false),
            object("data")
                .properties(
                    array("id").items(number(null).description("The IDs of the organizations that were deleted"))
                        .placeholder("Add")
                        .label("Id")
                        .description("The IDs of the organizations that were deleted")
                        .required(false))
                .label("Data")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", "JSON")))
        .exampleOutput("{\"success\":true,\"data\":{\"id\":[123,100]}}"),
        action("getOrganizations")
            .display(
                display("Get all organizations")
                    .description("Returns all organizations."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/organizations"

                ))
            .properties(integer("user_id").label("User_id")
                .description(
                    "If supplied, only organizations owned by the given user will be returned. However, `filter_id` takes precedence over `user_id` when both are supplied.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
                integer("filter_id").label("Filter_id")
                    .description("The ID of the filter to use")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("first_char").label("First_char")
                    .description(
                        "If supplied, only organizations whose name starts with the specified letter will be returned (case insensitive)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("start").label("Start")
                    .description("Pagination start")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("sort").label("Sort")
                    .description(
                        "The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
            .output(
                object(null)
                    .properties(
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
                            .required(false),
                        array("data").items(object(null).properties(string("address_route").label("Address_route")
                            .description("The route of the organization location")
                            .required(false),
                            integer("related_closed_deals_count").label("Related_closed_deals_count")
                                .description("The count of related closed deals related with the item")
                                .required(false),
                            integer("email_messages_count").label("Email_messages_count")
                                .description("The count of email messages related to the organization")
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
                            string("cc_email").label("Cc_email")
                                .description("The BCC email of the organization")
                                .required(false),
                            integer("open_deals_count").label("Open_deals_count")
                                .description("The count of open deals related with the item")
                                .required(false),
                            bool("active_flag").label("Active_flag")
                                .description("Whether the organization is active or not")
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
                            integer("value").label("Value")
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
                            integer("people_count").label("People_count")
                                .description("The count of persons related to the organization")
                                .required(false),
                            integer("last_activity_id").label("Last_activity_id")
                                .description("The ID of the last activity associated with the deal")
                                .required(false),
                            string("next_activity_date").label("Next_activity_date")
                                .description("The date of the next activity associated with the deal")
                                .required(false),
                            string("update_time").label("Update_time")
                                .description("The last updated date and time of the organization")
                                .required(false),
                            integer("activities_count").label("Activities_count")
                                .description("The count of activities related to the organization")
                                .required(false),
                            integer("id").label("Id")
                                .description("The ID of the organization")
                                .required(false),
                            string("address_admin_area_level_2").label("Address_admin_area_level_2")
                                .description("The level 2 admin area of the organization location")
                                .required(false),
                            integer("won_deals_count").label("Won_deals_count")
                                .description("The count of won deals related with the item")
                                .required(false),
                            string("address_admin_area_level_1").label("Address_admin_area_level_1")
                                .description("The level 1 admin area of the organization location")
                                .required(false),
                            string("address_street_number").label("Address_street_number")
                                .description("The street number of the organization location")
                                .required(false),
                            string("owner_name").label("Owner_name")
                                .description("The name of the organization owner")
                                .required(false),
                            integer("files_count").label("Files_count")
                                .description("The count of files related to the organization")
                                .required(false),
                            string("address").label("Address")
                                .description("The full address of the organization")
                                .required(false),
                            integer("company_id").label("Company_id")
                                .description("The ID of the company related to the organization")
                                .required(false),
                            string("address_formatted_address").label("Address_formatted_address")
                                .description("The formatted organization location")
                                .required(false),
                            string("address_postal_code").label("Address_postal_code")
                                .description("The postal code of the organization location")
                                .required(false),
                            integer("related_won_deals_count").label("Related_won_deals_count")
                                .description("The count of related won deals related with the item")
                                .required(false),
                            string("address_country").label("Address_country")
                                .description("The country of the organization location")
                                .required(false),
                            string("first_char").label("First_char")
                                .description("The first character of the organization name")
                                .required(false),
                            integer("undone_activities_count").label("Undone_activities_count")
                                .description("The count of undone activities related to the organization")
                                .required(false),
                            integer("closed_deals_count").label("Closed_deals_count")
                                .description("The count of closed deals related with the item")
                                .required(false),
                            string("address_subpremise").label("Address_subpremise")
                                .description("The sub-premise of the organization location")
                                .required(false),
                            string("last_activity_date").label("Last_activity_date")
                                .description("The date of the last activity associated with the deal")
                                .required(false),
                            integer("label").label("Label")
                                .description("The ID of the label")
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
                            string("country_code").label("Country_code")
                                .description("The country code of the organization")
                                .required(false),
                            string("visible_to").label("Visible_to")
                                .description("The visibility group ID of who can see the organization")
                                .required(false),
                            integer("notes_count").label("Notes_count")
                                .description("The count of notes related to the organization")
                                .required(false),
                            integer("followers_count").label("Followers_count")
                                .description("The count of followers related to the organization")
                                .required(false),
                            string("name").label("Name")
                                .description("The name of the organization")
                                .required(false),
                            string("address_sublocality").label("Address_sublocality")
                                .description("The sub-locality of the organization location")
                                .required(false),
                            string("address_locality").label("Address_locality")
                                .description("The locality of the organization location")
                                .required(false),
                            integer("lost_deals_count").label("Lost_deals_count")
                                .description("The count of lost deals related with the item")
                                .required(false),
                            string("next_activity_time").label("Next_activity_time")
                                .description("The time of the next activity associated with the deal")
                                .required(false),
                            string("add_time").label("Add_time")
                                .description("The creation date and time of the organization")
                                .required(false),
                            integer("done_activities_count").label("Done_activities_count")
                                .description("The count of done activities related to the organization")
                                .required(false))
                            .description("The array of organizations"))
                            .placeholder("Add")
                            .label("Data")
                            .description("The array of organizations")
                            .required(false),
                        object("related_objects").properties(object("organization").properties(string("name")
                            .label("Name")
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
                                    .description(
                                        "Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
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
                            "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false,\"next_start\":100}},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}"),
        action("addOrganization")
            .display(
                display("Add an organization")
                    .description(
                        "Adds a new organization. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the organizationFields and look for `key` values. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/adding-an-organization\" target=\"_blank\" rel=\"noopener noreferrer\">adding an organization</a>."))
            .metadata(
                Map.of(
                    "requestMethod", "POST",
                    "path", "/organizations", "bodyContentType", "JSON"

                ))
            .properties(object(null).properties(string("name").label("Name")
                .description("The name of the organization")
                .required(false),
                string("add_time").label("Add_time")
                    .description(
                        "The optional creation date & time of the organization in UTC. Requires admin user API token. Format: YYYY-MM-DD HH:MM:SS")
                    .required(false),
                string("visible_to").label("Visible_to")
                    .description(
                        "The visibility of the organization. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                    .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                    .required(false),
                integer("owner_id").label("Owner_id")
                    .description(
                        "The ID of the user who will be marked as the owner of this organization. When omitted, the authorized user ID will be used.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", "BODY")))
            .output(object(null).properties(string("address_route").label("Address_route")
                .description("The route of the organization location")
                .required(false),
                integer("related_closed_deals_count").label("Related_closed_deals_count")
                    .description("The count of related closed deals related with the item")
                    .required(false),
                integer("email_messages_count").label("Email_messages_count")
                    .description("The count of email messages related to the organization")
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
                string("cc_email").label("Cc_email")
                    .description("The BCC email of the organization")
                    .required(false),
                integer("open_deals_count").label("Open_deals_count")
                    .description("The count of open deals related with the item")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the organization is active or not")
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
                integer("value").label("Value")
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
                integer("people_count").label("People_count")
                    .description("The count of persons related to the organization")
                    .required(false),
                integer("last_activity_id").label("Last_activity_id")
                    .description("The ID of the last activity associated with the deal")
                    .required(false),
                string("next_activity_date").label("Next_activity_date")
                    .description("The date of the next activity associated with the deal")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last updated date and time of the organization")
                    .required(false),
                integer("activities_count").label("Activities_count")
                    .description("The count of activities related to the organization")
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the organization")
                    .required(false),
                string("address_admin_area_level_2").label("Address_admin_area_level_2")
                    .description("The level 2 admin area of the organization location")
                    .required(false),
                integer("won_deals_count").label("Won_deals_count")
                    .description("The count of won deals related with the item")
                    .required(false),
                string("address_admin_area_level_1").label("Address_admin_area_level_1")
                    .description("The level 1 admin area of the organization location")
                    .required(false),
                string("address_street_number").label("Address_street_number")
                    .description("The street number of the organization location")
                    .required(false),
                string("owner_name").label("Owner_name")
                    .description("The name of the organization owner")
                    .required(false),
                integer("files_count").label("Files_count")
                    .description("The count of files related to the organization")
                    .required(false),
                string("address").label("Address")
                    .description("The full address of the organization")
                    .required(false),
                bool("edit_name").label("Edit_name")
                    .description("If the company ID of the organization and company ID of the request is same or not")
                    .required(false),
                integer("company_id").label("Company_id")
                    .description("The ID of the company related to the organization")
                    .required(false),
                string("address_formatted_address").label("Address_formatted_address")
                    .description("The formatted organization location")
                    .required(false),
                string("address_postal_code").label("Address_postal_code")
                    .description("The postal code of the organization location")
                    .required(false),
                integer("related_won_deals_count").label("Related_won_deals_count")
                    .description("The count of related won deals related with the item")
                    .required(false),
                string("address_country").label("Address_country")
                    .description("The country of the organization location")
                    .required(false),
                string("first_char").label("First_char")
                    .description("The first character of the organization name")
                    .required(false),
                integer("undone_activities_count").label("Undone_activities_count")
                    .description("The count of undone activities related to the organization")
                    .required(false),
                integer("closed_deals_count").label("Closed_deals_count")
                    .description("The count of closed deals related with the item")
                    .required(false),
                string("address_subpremise").label("Address_subpremise")
                    .description("The sub-premise of the organization location")
                    .required(false),
                string("last_activity_date").label("Last_activity_date")
                    .description("The date of the last activity associated with the deal")
                    .required(false),
                integer("label").label("Label")
                    .description("The ID of the label")
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
                string("country_code").label("Country_code")
                    .description("The country code of the organization")
                    .required(false),
                string("visible_to").label("Visible_to")
                    .description("The visibility group ID of who can see the organization")
                    .required(false),
                integer("notes_count").label("Notes_count")
                    .description("The count of notes related to the organization")
                    .required(false),
                integer("followers_count").label("Followers_count")
                    .description("The count of followers related to the organization")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the organization")
                    .required(false),
                string("address_sublocality").label("Address_sublocality")
                    .description("The sub-locality of the organization location")
                    .required(false),
                string("address_locality").label("Address_locality")
                    .description("The locality of the organization location")
                    .required(false),
                integer("lost_deals_count").label("Lost_deals_count")
                    .description("The count of lost deals related with the item")
                    .required(false),
                string("next_activity_time").label("Next_activity_time")
                    .description("The time of the next activity associated with the deal")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the organization")
                    .required(false),
                integer("done_activities_count").label("Done_activities_count")
                    .description("The count of done activities related to the organization")
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
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}"),
        action("searchOrganization")
            .display(
                display("Search organizations")
                    .description(
                        "Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/organizations/search"

                ))
            .properties(string("term").label("Term")
                .description(
                    "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
                string("fields").label("Fields")
                    .description(
                        "A comma-separated string array. The fields to perform the search from. Defaults to all of them.")
                    .options(option("Address", "address"), option("Custom_fields", "custom_fields"),
                        option("Notes", "notes"), option("Name", "name"))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                bool("exact_match").label("Exact_match")
                    .description(
                        "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                    .options(option("True", true), option("False", false))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("start").label("Start")
                    .description(
                        "Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
            .output(object(null)
                .properties(
                    object("data")
                        .properties(array("items")
                            .items(object(null).properties(number("result_score").label("Result_score")
                                .description("Search result relevancy")
                                .required(false),
                                object("item").properties(integer("id").label("Id")
                                    .description("The ID of the organization")
                                    .required(false),
                                    string("type").label("Type")
                                        .description("The type of the item")
                                        .required(false),
                                    string("name").label("Name")
                                        .description("The name of the organization")
                                        .required(false),
                                    string("address").label("Address")
                                        .description("The address of the organization")
                                        .required(false),
                                    integer("visible_to").label("Visible_to")
                                        .description("The visibility of the organization")
                                        .required(false),
                                    object("owner").properties(integer("id").label("Id")
                                        .description("The ID of the owner of the deal")
                                        .required(false))
                                        .label("Owner")
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
                    object("additional_data").properties(object("pagination").properties(integer("start").label("Start")
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
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.316,\"item\":{\"id\":1,\"type\":\"organization\",\"name\":\"Organization name\",\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"visible_to\":3,\"owner\":{\"id\":1},\"custom_fields\":[],\"notes\":[]}}]},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}"),
        action("deleteOrganization")
            .display(
                display("Delete an organization")
                    .description(
                        "Marks an organization as deleted. After 30 days, the organization will be permanently deleted."))
            .metadata(
                Map.of(
                    "requestMethod", "DELETE",
                    "path", "/organizations/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the organization")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")))
            .output(object(null).properties(bool("success").label("Success")
                .description("If the request was successful or not")
                .required(false),
                object("data").properties(integer("id").label("Id")
                    .description("The ID of the organization that was deleted")
                    .required(false))
                    .label("Data")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput("{\"success\":true,\"data\":{\"id\":123}}"),
        action("getOrganization")
            .display(
                display("Get details of an organization")
                    .description(
                        "Returns the details of an organization. Note that this also returns some additional fields which are not present when asking for all organizations. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of organizationFields."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/organizations/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the organization")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")))
            .output(object(null)
                .properties(object("additional_data")
                    .properties(object("followers").properties(string("name").label("Name")
                        .description("The name of the follower")
                        .required(false),
                        integer("id").label("Id")
                            .description("The ID of the follower associated with the item")
                            .required(false),
                        integer("user_id").label("User_id")
                            .description("The user ID of the follower")
                            .required(false),
                        string("email").label("Email")
                            .description("The email of the follower")
                            .required(false),
                        string("pic_hash").label("Pic_hash")
                            .description("The follower picture hash")
                            .required(false))
                        .label("Followers")
                        .description("The follower that is associated with the item")
                        .required(false),
                        string("dropbox_email").label("Dropbox_email")
                            .description("Dropbox email for the organization")
                            .required(false))
                    .label("Additional_data")
                    .required(false),
                    string("address_route").label("Address_route")
                        .description("The route of the organization location")
                        .required(false),
                    integer("related_closed_deals_count").label("Related_closed_deals_count")
                        .description("The count of related closed deals related with the item")
                        .required(false),
                    integer("email_messages_count").label("Email_messages_count")
                        .description("The count of email messages related to the organization")
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
                    string("cc_email").label("Cc_email")
                        .description("The BCC email of the organization")
                        .required(false),
                    integer("open_deals_count").label("Open_deals_count")
                        .description("The count of open deals related with the item")
                        .required(false),
                    integer("people_count").label("People_count")
                        .description("The count of persons related to the organization")
                        .required(false),
                    string("next_activity_date").label("Next_activity_date")
                        .description("The date of the next activity associated with the deal")
                        .required(false),
                    integer("id").label("Id")
                        .description("The ID of the organization")
                        .required(false),
                    string("address_admin_area_level_2").label("Address_admin_area_level_2")
                        .description("The level 2 admin area of the organization location")
                        .required(false),
                    string("address_admin_area_level_1").label("Address_admin_area_level_1")
                        .description("The level 1 admin area of the organization location")
                        .required(false),
                    string("owner_name").label("Owner_name")
                        .description("The name of the organization owner")
                        .required(false),
                    string("address_formatted_address").label("Address_formatted_address")
                        .description("The formatted organization location")
                        .required(false),
                    integer("related_won_deals_count").label("Related_won_deals_count")
                        .description("The count of related won deals related with the item")
                        .required(false),
                    integer("undone_activities_count").label("Undone_activities_count")
                        .description("The count of undone activities related to the organization")
                        .required(false),
                    string("address_subpremise").label("Address_subpremise")
                        .description("The sub-premise of the organization location")
                        .required(false),
                    string("last_activity_date").label("Last_activity_date")
                        .description("The date of the last activity associated with the deal")
                        .required(false),
                    integer("next_activity_id").label("Next_activity_id")
                        .description("The ID of the next activity associated with the deal")
                        .required(false),
                    string("country_code").label("Country_code")
                        .description("The country code of the organization")
                        .required(false),
                    string("visible_to").label("Visible_to")
                        .description("The visibility group ID of who can see the organization")
                        .required(false),
                    integer("notes_count").label("Notes_count")
                        .description("The count of notes related to the organization")
                        .required(false),
                    string("name").label("Name")
                        .description("The name of the organization")
                        .required(false),
                    string("address_locality").label("Address_locality")
                        .description("The locality of the organization location")
                        .required(false),
                    integer("lost_deals_count").label("Lost_deals_count")
                        .description("The count of lost deals related with the item")
                        .required(false),
                    string("next_activity_time").label("Next_activity_time")
                        .description("The time of the next activity associated with the deal")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the organization is active or not")
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
                    integer("value").label("Value")
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
                    string("update_time").label("Update_time")
                        .description("The last updated date and time of the organization")
                        .required(false),
                    object("last_activity").label("Last_activity")
                        .description(
                            "Please refer to response schema of <a href=\"https://developers.pipedrive.com/docs/api/v1/Activities#getActivity\">Activity</a>")
                        .required(false),
                    object("next_activity").label("Next_activity")
                        .description(
                            "Please refer to response schema of <a href=\"https://developers.pipedrive.com/docs/api/v1/Activities#getActivity\">Activity</a>")
                        .required(false),
                    integer("activities_count").label("Activities_count")
                        .description("The count of activities related to the organization")
                        .required(false),
                    integer("won_deals_count").label("Won_deals_count")
                        .description("The count of won deals related with the item")
                        .required(false),
                    string("address_street_number").label("Address_street_number")
                        .description("The street number of the organization location")
                        .required(false),
                    integer("files_count").label("Files_count")
                        .description("The count of files related to the organization")
                        .required(false),
                    string("address").label("Address")
                        .description("The full address of the organization")
                        .required(false),
                    bool("edit_name").label("Edit_name")
                        .description(
                            "If the company ID of the organization and company ID of the request is same or not")
                        .required(false),
                    integer("company_id").label("Company_id")
                        .description("The ID of the company related to the organization")
                        .required(false),
                    string("address_postal_code").label("Address_postal_code")
                        .description("The postal code of the organization location")
                        .required(false),
                    string("address_country").label("Address_country")
                        .description("The country of the organization location")
                        .required(false),
                    string("first_char").label("First_char")
                        .description("The first character of the organization name")
                        .required(false),
                    integer("closed_deals_count").label("Closed_deals_count")
                        .description("The count of closed deals related with the item")
                        .required(false),
                    integer("label").label("Label")
                        .description("The ID of the label")
                        .required(false),
                    integer("related_open_deals_count").label("Related_open_deals_count")
                        .description("The count of related open deals related with the item")
                        .required(false),
                    integer("related_lost_deals_count").label("Related_lost_deals_count")
                        .description("The count of related lost deals related with the item")
                        .required(false),
                    integer("followers_count").label("Followers_count")
                        .description("The count of followers related to the organization")
                        .required(false),
                    string("address_sublocality").label("Address_sublocality")
                        .description("The sub-locality of the organization location")
                        .required(false),
                    string("add_time").label("Add_time")
                        .description("The creation date and time of the organization")
                        .required(false),
                    integer("done_activities_count").label("Done_activities_count")
                        .description("The count of done activities related to the organization")
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
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"allOf\":[{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"},{\"type\":\"object\",\"properties\":{\"last_activity\":{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_lat\":59.4281884,\"location_long\":24.7041378,\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}},\"next_activity\":null,\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false,\"next_start\":100}},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}}]}}"),
        action("updateOrganization")
            .display(
                display("Update an organization")
                    .description("Updates the properties of an organization."))
            .metadata(
                Map.of(
                    "requestMethod", "PUT",
                    "path", "/organizations/{id}", "bodyContentType", "JSON"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the organization")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")),
                object(null).properties(string("name").label("Name")
                    .description("The name of the organization")
                    .required(false),
                    string("visible_to").label("Visible_to")
                        .description(
                            "The visibility of the organization. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                        .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                        .required(false),
                    integer("owner_id").label("Owner_id")
                        .description(
                            "The ID of the user who will be marked as the owner of this organization. When omitted, the authorized user ID will be used.")
                        .required(false))
                    .metadata(
                        Map.of(
                            "type", "BODY")))
            .output(object(null).properties(string("address_route").label("Address_route")
                .description("The route of the organization location")
                .required(false),
                integer("related_closed_deals_count").label("Related_closed_deals_count")
                    .description("The count of related closed deals related with the item")
                    .required(false),
                integer("email_messages_count").label("Email_messages_count")
                    .description("The count of email messages related to the organization")
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
                string("cc_email").label("Cc_email")
                    .description("The BCC email of the organization")
                    .required(false),
                integer("open_deals_count").label("Open_deals_count")
                    .description("The count of open deals related with the item")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the organization is active or not")
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
                integer("value").label("Value")
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
                integer("people_count").label("People_count")
                    .description("The count of persons related to the organization")
                    .required(false),
                integer("last_activity_id").label("Last_activity_id")
                    .description("The ID of the last activity associated with the deal")
                    .required(false),
                string("next_activity_date").label("Next_activity_date")
                    .description("The date of the next activity associated with the deal")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last updated date and time of the organization")
                    .required(false),
                integer("activities_count").label("Activities_count")
                    .description("The count of activities related to the organization")
                    .required(false),
                integer("id").label("Id")
                    .description("The ID of the organization")
                    .required(false),
                string("address_admin_area_level_2").label("Address_admin_area_level_2")
                    .description("The level 2 admin area of the organization location")
                    .required(false),
                integer("won_deals_count").label("Won_deals_count")
                    .description("The count of won deals related with the item")
                    .required(false),
                string("address_admin_area_level_1").label("Address_admin_area_level_1")
                    .description("The level 1 admin area of the organization location")
                    .required(false),
                string("address_street_number").label("Address_street_number")
                    .description("The street number of the organization location")
                    .required(false),
                string("owner_name").label("Owner_name")
                    .description("The name of the organization owner")
                    .required(false),
                integer("files_count").label("Files_count")
                    .description("The count of files related to the organization")
                    .required(false),
                string("address").label("Address")
                    .description("The full address of the organization")
                    .required(false),
                integer("company_id").label("Company_id")
                    .description("The ID of the company related to the organization")
                    .required(false),
                string("address_formatted_address").label("Address_formatted_address")
                    .description("The formatted organization location")
                    .required(false),
                string("address_postal_code").label("Address_postal_code")
                    .description("The postal code of the organization location")
                    .required(false),
                integer("related_won_deals_count").label("Related_won_deals_count")
                    .description("The count of related won deals related with the item")
                    .required(false),
                string("address_country").label("Address_country")
                    .description("The country of the organization location")
                    .required(false),
                string("first_char").label("First_char")
                    .description("The first character of the organization name")
                    .required(false),
                integer("undone_activities_count").label("Undone_activities_count")
                    .description("The count of undone activities related to the organization")
                    .required(false),
                integer("closed_deals_count").label("Closed_deals_count")
                    .description("The count of closed deals related with the item")
                    .required(false),
                string("address_subpremise").label("Address_subpremise")
                    .description("The sub-premise of the organization location")
                    .required(false),
                string("last_activity_date").label("Last_activity_date")
                    .description("The date of the last activity associated with the deal")
                    .required(false),
                integer("label").label("Label")
                    .description("The ID of the label")
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
                string("country_code").label("Country_code")
                    .description("The country code of the organization")
                    .required(false),
                string("visible_to").label("Visible_to")
                    .description("The visibility group ID of who can see the organization")
                    .required(false),
                integer("notes_count").label("Notes_count")
                    .description("The count of notes related to the organization")
                    .required(false),
                integer("followers_count").label("Followers_count")
                    .description("The count of followers related to the organization")
                    .required(false),
                string("name").label("Name")
                    .description("The name of the organization")
                    .required(false),
                string("address_sublocality").label("Address_sublocality")
                    .description("The sub-locality of the organization location")
                    .required(false),
                string("address_locality").label("Address_locality")
                    .description("The locality of the organization location")
                    .required(false),
                integer("lost_deals_count").label("Lost_deals_count")
                    .description("The count of lost deals related with the item")
                    .required(false),
                string("next_activity_time").label("Next_activity_time")
                    .description("The time of the next activity associated with the deal")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the organization")
                    .required(false),
                integer("done_activities_count").label("Done_activities_count")
                    .description("The count of done activities related to the organization")
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
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}"),
        action("getOrganizationDeals")
            .display(
                display("List deals associated with an organization")
                    .description("Lists deals associated with an organization."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/organizations/{id}/deals"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the organization")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")),
                integer("start").label("Start")
                    .description("Pagination start")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("status").label("Status")
                    .description(
                        "Only fetch deals with a specific status. If omitted, all not deleted deals are returned. If set to deleted, deals that have been deleted up to 30 days ago will be included.")
                    .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                        option("Deleted", "deleted"), option("All_not_deleted", "all_not_deleted"))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("sort").label("Sort")
                    .description(
                        "The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                number("only_primary_association").label("Only_primary_association")
                    .description(
                        "If set, only deals that are directly associated to the organization are fetched. If not set (default), all deals are fetched that are either directly or indirectly related to the organization. Indirect relations include relations through custom, organization-type fields and through persons of the given organization.")
                    .options(option("0", 0), option("1", 1))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
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
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":1,\"creator_user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"person_id\":{\"active_flag\":true,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"37244499911\",\"primary\":true}],\"value\":1101},\"org_id\":{\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":5},\"stage_id\":2,\"title\":\"Deal One\",\"value\":5000,\"currency\":\"EUR\",\"add_time\":\"2019-05-29 04:21:51\",\"update_time\":\"2019-11-28 16:19:50\",\"stage_change_time\":\"2019-11-28 15:41:22\",\"active\":true,\"deleted\":false,\"status\":\"open\",\"probability\":null,\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":null,\"last_activity_date\":null,\"lost_reason\":null,\"visible_to\":\"1\",\"close_time\":null,\"pipeline_id\":1,\"won_time\":\"2019-11-27 11:40:36\",\"first_won_time\":\"2019-11-27 11:40:36\",\"lost_time\":\"2019-11-27 11:40:36\",\"products_count\":0,\"files_count\":0,\"notes_count\":2,\"followers_count\":0,\"email_messages_count\":4,\"activities_count\":1,\"done_activities_count\":0,\"undone_activities_count\":1,\"participants_count\":1,\"expected_close_date\":\"2019-06-29\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":11,\"stage_order_nr\":2,\"person_name\":\"Person\",\"org_name\":\"Organization\",\"next_activity_subject\":\"Call\",\"next_activity_type\":\"call\",\"next_activity_duration\":\"00:30:00\",\"next_activity_note\":\"Note content\",\"formatted_value\":\"€5,000\",\"weighted_value\":5000,\"formatted_weighted_value\":\"€5,000\",\"weighted_value_currency\":\"EUR\",\"rotten_time\":null,\"owner_name\":\"Creator\",\"cc_email\":\"company+deal1@pipedrivemail.com\",\"org_hidden\":false,\"person_hidden\":false}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":true}},\"related_objects\":{\"user\":{\"8877\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"person\":{\"1101\":{\"active_flag\":true,\"id\":1101,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"stage\":{\"2\":{\"id\":2,\"company_id\":123,\"order_nr\":1,\"name\":\"Stage Name\",\"active_flag\":true,\"deal_probability\":100,\"pipeline_id\":1,\"rotten_flag\":false,\"rotten_days\":null,\"add_time\":\"2015-12-08 13:54:06\",\"update_time\":\"2015-12-08 13:54:06\",\"pipeline_name\":\"Pipeline\",\"pipeline_deal_probability\":true}},\"pipeline\":{\"1\":{\"id\":1,\"name\":\"Pipeline\",\"url_title\":\"Pipeline\",\"order_nr\":0,\"active\":true,\"deal_probability\":true,\"add_time\":\"2015-12-08 10:00:24\",\"update_time\":\"2015-12-08 10:00:24\"}}}}"),
        action("mergeOrganizations")
            .display(
                display("Merge two organizations")
                    .description(
                        "Merges an organization with another organization. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/merging-two-organizations\" target=\"_blank\" rel=\"noopener noreferrer\">merging two organizations</a>."))
            .metadata(
                Map.of(
                    "requestMethod", "PUT",
                    "path", "/organizations/{id}/merge", "bodyContentType", "JSON"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the organization")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")),
                object(null).properties(integer("merge_with_id").label("Merge_with_id")
                    .description("The ID of the organization that the organization will be merged with")
                    .required(true))
                    .metadata(
                        Map.of(
                            "type", "BODY")))
            .output(object(null).properties(bool("success").label("Success")
                .description("If the request was successful or not")
                .required(false),
                object("data").properties(integer("id").label("Id")
                    .description("The ID of the merged organization")
                    .required(false))
                    .label("Data")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput("{\"success\":true,\"data\":{\"id\":123}}"));
}
