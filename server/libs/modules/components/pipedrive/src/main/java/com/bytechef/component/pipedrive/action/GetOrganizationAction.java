
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
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GetOrganizationAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getOrganization")
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
                    "type", PropertyType.PATH)))
        .output(
            object(null)
                .properties(object("additional_data").properties(object("followers").properties(string("name")
                    .label("Name")
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
                        "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"allOf\":[{\"id\":1,\"company_id\":77,\"owner_id\":{\"id\":10,\"name\":\"Will Smith\",\"email\":\"will.smith@pipedrive.com\",\"has_pic\":0,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true,\"value\":10},\"name\":\"Bolt\",\"open_deals_count\":1,\"related_open_deals_count\":2,\"closed_deals_count\":3,\"related_closed_deals_count\":1,\"email_messages_count\":2,\"people_count\":1,\"activities_count\":2,\"done_activities_count\":1,\"undone_activities_count\":0,\"files_count\":0,\"notes_count\":0,\"followers_count\":1,\"won_deals_count\":0,\"related_won_deals_count\":0,\"lost_deals_count\":0,\"related_lost_deals_count\":0,\"active_flag\":true,\"picture_id\":{\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cf14ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb2ac8f269_512.jpg\"},\"value\":101},\"country_code\":\"USA\",\"first_char\":\"b\",\"update_time\":\"2020-09-08 12:14:11\",\"add_time\":\"2020-02-25 10:04:08\",\"visible_to\":\"3\",\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":34,\"last_activity_date\":\"2019-11-28\",\"label\":7,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"address_subpremise\":\"\",\"address_street_number\":\"3a\",\"address_route\":\"Mustamäe tee\",\"address_sublocality\":\"Kristiine\",\"address_locality\":\"Tallinn\",\"address_admin_area_level_1\":\"Harju maakond\",\"address_admin_area_level_2\":\"\",\"address_country\":\"Estonia\",\"address_postal_code\":\"10616\",\"address_formatted_address\":\"Mustamäe tee 3a, 10616 Tallinn, Estonia\",\"owner_name\":\"John Doe\",\"cc_email\":\"org@pipedrivemail.com\"},{\"type\":\"object\",\"properties\":{\"last_activity\":{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}},\"next_activity\":null,\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false,\"next_start\":100}},\"related_objects\":{\"organization\":{\"1\":{\"id\":1,\"name\":\"Org Name\",\"people_count\":1,\"owner_id\":123,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"user\":{\"123\":{\"id\":123,\"name\":\"Jane Doe\",\"email\":\"jane@pipedrive.com\",\"has_pic\":1,\"pic_hash\":\"2611ace8ac6a3afe2f69ed56f9e08c6b\",\"active_flag\":true}},\"picture\":{\"1\":{\"id\":1,\"item_type\":\"person\",\"item_id\":25,\"active_flag\":true,\"add_time\":\"2020-09-08 08:17:52\",\"update_time\":\"0000-00-00 00:00:00\",\"added_by_user_id\":967055,\"pictures\":{\"128\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_128.jpg\",\"512\":\"https://pipedrive-profile-pics.s3.example.com/f8893852574273f2747bf6ef09d11cfb4ac8f269_512.jpg\"}}}}}}]}}");
}
