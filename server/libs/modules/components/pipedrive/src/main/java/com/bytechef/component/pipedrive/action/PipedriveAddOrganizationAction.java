
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
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
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
public class PipedriveAddOrganizationAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addOrganization")
        .title("Add an organization")
        .description(
            "Adds a new organization. Note that you can supply additional custom fields along with the request that are not described here. These custom fields are different for each Pipedrive account and can be recognized by long hashes as keys. To determine which custom fields exists, fetch the organizationFields and look for `key` values. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/adding-an-organization\" target=\"_blank\" rel=\"noopener noreferrer\">adding an organization</a>.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/organizations", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("name").label("Name")
            .description("The name of the organization")
            .required(false),
            string("add_time").label("Add Time")
                .description(
                    "The optional creation date & time of the organization in UTC. Requires admin user API token. Format: YYYY-MM-DD HH:MM:SS")
                .required(false),
            string("visible_to").label("Visible To")
                .description(
                    "The visibility of the organization. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width:40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                .required(false),
            integer("owner_id").label("Owner Id")
                .description(
                    "The ID of the user who will be marked as the owner of this organization. When omitted, the authorized user ID will be used.")
                .required(false))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(string("address_route").label("Address Route")
            .description("The route of the organization location")
            .required(false),
            integer("related_closed_deals_count").label("Related Closed Deals Count")
                .description("The count of related closed deals related with the item")
                .required(false),
            integer("email_messages_count").label("Email Messages Count")
                .description("The count of email messages related to the organization")
                .required(false),
            string("name").label("Name")
                .description("The name of the user")
                .required(false),
            integer("has_pic").label("Has Pic")
                .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                .required(false),
            bool("active_flag").label("Active Flag")
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
            string("pic_hash").label("Pic Hash")
                .description("The user picture hash")
                .required(false),
            string("cc_email").label("Cc Email")
                .description("The BCC email of the organization")
                .required(false),
            integer("open_deals_count").label("Open Deals Count")
                .description("The count of open deals related with the item")
                .required(false),
            bool("active_flag").label("Active Flag")
                .description("Whether the organization is active or not")
                .required(false),
            string("update_time").label("Update Time")
                .description("The update time of the picture")
                .required(false),
            integer("added_by_user_id").label("Added By User Id")
                .description("The ID of the user who added the picture")
                .required(false),
            integer("item_id").label("Item Id")
                .description("The ID of related item")
                .required(false),
            string("item_type").label("Item Type")
                .description("The type of item the picture is related to")
                .required(false),
            bool("active_flag").label("Active Flag")
                .description("Whether the associated picture is active or not")
                .required(false),
            integer("value").label("Value")
                .description("The ID of the picture associated with the item")
                .required(false),
            string("add_time").label("Add Time")
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
            integer("people_count").label("People Count")
                .description("The count of persons related to the organization")
                .required(false),
            integer("last_activity_id").label("Last Activity Id")
                .description("The ID of the last activity associated with the deal")
                .required(false),
            string("next_activity_date").label("Next Activity Date")
                .description("The date of the next activity associated with the deal")
                .required(false),
            string("update_time").label("Update Time")
                .description("The last updated date and time of the organization")
                .required(false),
            integer("activities_count").label("Activities Count")
                .description("The count of activities related to the organization")
                .required(false),
            integer("id").label("Id")
                .description("The ID of the organization")
                .required(false),
            string("address_admin_area_level_2").label("Address Admin Area Level 2")
                .description("The level 2 admin area of the organization location")
                .required(false),
            integer("won_deals_count").label("Won Deals Count")
                .description("The count of won deals related with the item")
                .required(false),
            string("address_admin_area_level_1").label("Address Admin Area Level 1")
                .description("The level 1 admin area of the organization location")
                .required(false),
            string("address_street_number").label("Address Street Number")
                .description("The street number of the organization location")
                .required(false),
            string("owner_name").label("Owner Name")
                .description("The name of the organization owner")
                .required(false),
            integer("files_count").label("Files Count")
                .description("The count of files related to the organization")
                .required(false),
            string("address").label("Address")
                .description("The full address of the organization")
                .required(false),
            bool("edit_name").label("Edit Name")
                .description("If the company ID of the organization and company ID of the request is same or not")
                .required(false),
            integer("company_id").label("Company Id")
                .description("The ID of the company related to the organization")
                .required(false),
            string("address_formatted_address").label("Address Formatted Address")
                .description("The formatted organization location")
                .required(false),
            string("address_postal_code").label("Address Postal Code")
                .description("The postal code of the organization location")
                .required(false),
            integer("related_won_deals_count").label("Related Won Deals Count")
                .description("The count of related won deals related with the item")
                .required(false),
            string("address_country").label("Address Country")
                .description("The country of the organization location")
                .required(false),
            string("first_char").label("First Char")
                .description("The first character of the organization name")
                .required(false),
            integer("undone_activities_count").label("Undone Activities Count")
                .description("The count of undone activities related to the organization")
                .required(false),
            integer("closed_deals_count").label("Closed Deals Count")
                .description("The count of closed deals related with the item")
                .required(false),
            string("address_subpremise").label("Address Subpremise")
                .description("The sub-premise of the organization location")
                .required(false),
            string("last_activity_date").label("Last Activity Date")
                .description("The date of the last activity associated with the deal")
                .required(false),
            integer("label").label("Label")
                .description("The ID of the label")
                .required(false),
            integer("related_open_deals_count").label("Related Open Deals Count")
                .description("The count of related open deals related with the item")
                .required(false),
            integer("related_lost_deals_count").label("Related Lost Deals Count")
                .description("The count of related lost deals related with the item")
                .required(false),
            integer("next_activity_id").label("Next Activity Id")
                .description("The ID of the next activity associated with the deal")
                .required(false),
            string("country_code").label("Country Code")
                .description("The country code of the organization")
                .required(false),
            string("visible_to").label("Visible To")
                .description("The visibility group ID of who can see the organization")
                .required(false),
            integer("notes_count").label("Notes Count")
                .description("The count of notes related to the organization")
                .required(false),
            integer("followers_count").label("Followers Count")
                .description("The count of followers related to the organization")
                .required(false),
            string("name").label("Name")
                .description("The name of the organization")
                .required(false),
            string("address_sublocality").label("Address Sublocality")
                .description("The sub-locality of the organization location")
                .required(false),
            string("address_locality").label("Address Locality")
                .description("The locality of the organization location")
                .required(false),
            integer("lost_deals_count").label("Lost Deals Count")
                .description("The count of lost deals related with the item")
                .required(false),
            string("next_activity_time").label("Next Activity Time")
                .description("The time of the next activity associated with the deal")
                .required(false),
            string("add_time").label("Add Time")
                .description("The creation date and time of the organization")
                .required(false),
            integer("done_activities_count").label("Done Activities Count")
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
                integer("people_count").label("People Count")
                    .description(
                        "The number of people connected with the organization that is associated with the item")
                    .required(false),
                integer("owner_id").label("Owner Id")
                    .description("The ID of the owner of the organization that is associated with the item")
                    .required(false),
                string("cc_email").label("Cc Email")
                    .description("The BCC email of the organization associated with the item")
                    .required(false))
                .label("Organization")
                .required(false),
                object("user").properties(string("name").label("Name")
                    .description("The name of the user")
                    .required(false),
                    object("USER_ID").label("USER ID")
                        .required(false),
                    integer("has_pic").label("Has Pic")
                        .description("Whether the user has picture or not. 0 = No picture, 1 = Has picture.")
                        .required(false),
                    bool("active_flag").label("Active Flag")
                        .description("Whether the user is active or not")
                        .required(false),
                    integer("id").label("Id")
                        .description("The ID of the user")
                        .required(false),
                    string("email").label("Email")
                        .description("The email of the user")
                        .required(false),
                    string("pic_hash").label("Pic Hash")
                        .description("The user picture hash")
                        .required(false))
                    .label("User")
                    .required(false),
                object("picture").properties(string("update_time").label("Update Time")
                    .description("The update time of the picture")
                    .required(false),
                    integer("added_by_user_id").label("Added By User Id")
                        .description("The ID of the user who added the picture")
                        .required(false),
                    integer("item_id").label("Item Id")
                        .description("The ID of related item")
                        .required(false),
                    string("item_type").label("Item Type")
                        .description("The type of item the picture is related to")
                        .required(false),
                    bool("active_flag").label("Active Flag")
                        .description("Whether the associated picture is active or not")
                        .required(false),
                    integer("id").label("Id")
                        .description("The ID of the picture associated with the item")
                        .required(false),
                    string("add_time").label("Add Time")
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
                .label("Related Objects")
                .required(false),
            bool("success").label("Success")
                .description("If the response is successful or not")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .sampleOutput(
            "{\"success\":true,\"data\":{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}");
}
