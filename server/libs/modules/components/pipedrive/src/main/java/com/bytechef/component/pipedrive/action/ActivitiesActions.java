
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
public class ActivitiesActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("deleteActivities")
        .display(
            display("Delete multiple activities in bulk")
                .description(
                    "Marks multiple activities as deleted. After 30 days, the activities will be permanently deleted."))
        .metadata(
            Map.of(
                "requestMethod", "DELETE",
                "path", "/activities"

            ))
        .properties(string("ids").label("Ids")
            .description("The comma-separated IDs of activities that will be deleted")
            .required(true)
            .metadata(
                Map.of(
                    "type", "QUERY")))
        .output(object(null).properties(bool("success").label("Success")
            .required(false),
            object("data")
                .properties(
                    array("id").items(integer(null).description("An array of the IDs of activities that were deleted"))
                        .placeholder("Add")
                        .label("Id")
                        .description("An array of the IDs of activities that were deleted")
                        .required(false))
                .label("Data")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", "JSON")))
        .exampleOutput("{\"success\":true,\"data\":{\"id\":[625,627]}}"),
        action("getActivities")
            .display(
                display("Get all activities assigned to a particular user")
                    .description("Returns all activities assigned to a particular user."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/activities"

                ))
            .properties(integer("user_id").label("User_id")
                .description(
                    "The ID of the user whose activities will be fetched. If omitted, the user associated with the API token will be used. If 0, activities for all company users will be fetched based on the permission sets.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
                integer("filter_id").label("Filter_id")
                    .description(
                        "The ID of the filter to use (will narrow down results if used together with `user_id` parameter)")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("type").label("Type")
                    .description(
                        "The type of the activity, can be one type or multiple types separated by a comma. This is in correlation with the `key_string` parameter of ActivityTypes.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("limit").label("Limit")
                    .description(
                        "For pagination, the limit of entries to be returned. If not provided, 100 items will be returned.")
                    .required(false)
                    .exampleValue(100)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("start").label("Start")
                    .description("For pagination, the position that represents the first result for the page")
                    .required(false)
                    .exampleValue(0)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                date("start_date").label("Start_date")
                    .description(
                        "Use the activity due date where you wish to begin fetching activities from. Insert due date in YYYY-MM-DD format.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                date("end_date").label("End_date")
                    .description(
                        "Use the activity due date where you wish to stop fetching activities from. Insert due date in YYYY-MM-DD format.")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                number("done").label("Done")
                    .description(
                        "Whether the activity is done or not. 0 = Not done, 1 = Done. If omitted returns both done and not done activities.")
                    .options(option("0", 0), option("1", 1))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
            .output(object(null).properties(bool("success").label("Success")
                .required(false),
                array("data").items(object(null).properties(string("last_notification_time")
                    .label("Last_notification_time")
                    .description(
                        "The date and time of latest notifications sent about this activity to the participants or the attendees of this activity")
                    .required(false),
                    string("location_street_number").label("Location_street_number")
                        .description("Subfield of location field. Indicates house number.")
                        .required(false),
                    string("public_description").label("Public_description")
                        .description(
                            "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                        .required(false),
                    integer("reference_id").label("Reference_id")
                        .description("Together with the `reference_type`, gives the ID of the other object")
                        .required(false),
                    string("location_route").label("Location_route")
                        .description("Subfield of location field. Indicates street name.")
                        .required(false),
                    integer("notification_language_id").label("Notification_language_id")
                        .description("The ID of the language the notifications are sent in")
                        .required(false),
                    string("subject").label("Subject")
                        .description("The subject of the activity")
                        .required(false),
                    string("type").label("Type")
                        .description(
                            "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes.")
                        .required(false),
                    string("google_calendar_etag").label("Google_calendar_etag")
                        .description(
                            "The Google calendar API etag (version) that is used for syncing this activity. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                        .required(false),
                    string("google_calendar_id").label("Google_calendar_id")
                        .description(
                            "The Google calendar ID that this activity syncs to. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                        .required(false),
                    string("deal_title").label("Deal_title")
                        .description("The name of the deal this activity is associated with")
                        .required(false),
                    integer("id").label("Id")
                        .description("The activity ID, generated when the activity was created")
                        .required(false),
                    integer("deal_id").label("Deal_id")
                        .description("The ID of the deal this activity is associated with")
                        .required(false),
                    string("gcal_event_id").label("Gcal_event_id")
                        .description(
                            "For the activity which syncs to Google calendar, this is the Google event ID. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                        .required(false),
                    number("location_lat").label("Location_lat")
                        .description("Subfield of location field. Indicates latitude.")
                        .required(false),
                    integer("person_id").label("Person_id")
                        .description("The ID of the person this activity is associated with")
                        .required(false),
                    bool("busy_flag").label("Busy_flag")
                        .description(
                            "Marks if the activity is set as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                        .options(option("True", true), option("False", false))
                        .required(false),
                    number("location_long").label("Location_long")
                        .description("Subfield of location field. Indicates longitude.")
                        .required(false),
                    string("owner_name").label("Owner_name")
                        .description("The name of the user this activity is owned by")
                        .required(false),
                    array("attendees").items(object(null).description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address."))
                        .placeholder("Add")
                        .label("Attendees")
                        .description(
                            "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address.")
                        .required(false),
                    string("person_name").label("Person_name")
                        .description("The name of the person this activity is associated with")
                        .required(false),
                    string("rec_rule_extension").label("Rec_rule_extension")
                        .description(
                            "Additional rules for the recurrence of the activity, extend the `rec_rule`. Is important for activities synced into Pipedrive from an external calendar.")
                        .required(false),
                    bool("done").label("Done")
                        .description("Whether the activity is done or not")
                        .required(false),
                    integer("created_by_user_id").label("Created_by_user_id")
                        .description("The ID of the user who created the activity")
                        .required(false),
                    string("location_sublocality").label("Location_sublocality")
                        .description("Subfield of location field. Indicates district/sublocality.")
                        .required(false),
                    string("rec_rule").label("Rec_rule")
                        .description(
                            "The rule for the recurrence of the activity. Is important for activities synced into Pipedrive from an external calendar. Example: \"RRULE:FREQ=WEEKLY;BYDAY=WE\"")
                        .required(false),
                    string("location_admin_area_level_2").label("Location_admin_area_level_2")
                        .description("Subfield of location field. Indicates region.")
                        .required(false),
                    integer("user_id").label("User_id")
                        .description("The ID of the user whom the activity is assigned to")
                        .required(false),
                    string("location_admin_area_level_1").label("Location_admin_area_level_1")
                        .description("Subfield of location field. Indicates state/county.")
                        .required(false),
                    integer("org_id").label("Org_id")
                        .description("The ID of the organization this activity is associated with")
                        .required(false),
                    string("conference_meeting_client").label("Conference_meeting_client")
                        .description("The ID of Marketplace app, which is connected to this activity")
                        .required(false),
                    string("due_time").label("Due_time")
                        .description("The due time of the activity in UTC. Format: HH:MM")
                        .required(false),
                    string("note").label("Note")
                        .description("The note of the activity (HTML format)")
                        .required(false),
                    integer("rec_master_activity_id").label("Rec_master_activity_id")
                        .description(
                            "The ID of parent activity for a recurrent activity if the current activity is an exception to recurrence rules")
                        .required(false),
                    string("location_country").label("Location_country")
                        .description("Subfield of location field. Indicates country.")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the activity is active or not")
                        .required(false),
                    string("duration").label("Duration")
                        .description("The duration of the activity. Format: HH:MM")
                        .required(false),
                    string("location_postal_code").label("Location_postal_code")
                        .description("Subfield of location field. Indicates ZIP/postal code.")
                        .required(false),
                    string("update_time").label("Update_time")
                        .description("The last update date and time of the activity. Format: YYYY-MM-DD HH:MM:SS.")
                        .required(false),
                    object("file").label("File")
                        .description(
                            "The file that is attached to this activity. For example, this can be a reference to an audio note file generated with Pipedrive mobile app.")
                        .required(false),
                    integer("update_user_id").label("Update_user_id")
                        .description("The ID of the user who was the last to update this activity")
                        .required(false),
                    string("source_timezone").label("Source_timezone")
                        .description("The timezone the activity was created in an external calendar")
                        .required(false),
                    string("conference_meeting_id").label("Conference_meeting_id")
                        .description(
                            "The meeting ID of the meeting provider (Zoom, MS Teams etc.) that is associated with this activity")
                        .required(false),
                    string("person_dropbox_bcc").label("Person_dropbox_bcc")
                        .description("The BCC email address of the person")
                        .required(false),
                    string("location_locality").label("Location_locality")
                        .description("Subfield of location field. Indicates city/town/village/locality.")
                        .required(false),
                    string("org_name").label("Org_name")
                        .description("The name of the organization this activity is associated with")
                        .required(false),
                    integer("assigned_to_user_id").label("Assigned_to_user_id")
                        .description("The ID of the user to whom the activity is assigned to. Equal to `user_id`.")
                        .required(false),
                    string("lead_id").label("Lead_id")
                        .description("The ID of the lead in the UUID format this activity is associated with")
                        .required(false),
                    array("participants")
                        .items(object(null)
                            .description("List of multiple persons (participants) this activity is associated with"))
                        .placeholder("Add")
                        .label("Participants")
                        .description("List of multiple persons (participants) this activity is associated with")
                        .required(false),
                    string("location_subpremise").label("Location_subpremise")
                        .description("Subfield of location field. Indicates apartment/suite number.")
                        .required(false),
                    integer("company_id").label("Company_id")
                        .description("The user's company ID")
                        .required(false),
                    date("due_date").label("Due_date")
                        .description("The due date of the activity. Format: YYYY-MM-DD")
                        .required(false),
                    string("reference_type").label("Reference_type")
                        .description(
                            "If the activity references some other object, it is indicated here. For example, value `Salesphone` refers to activities created with Caller.")
                        .required(false),
                    integer("last_notification_user_id").label("Last_notification_user_id")
                        .description(
                            "The ID of the user who triggered the sending of the latest notifications about this activity to the participants or the attendees of this activity")
                        .required(false),
                    string("calendar_sync_include_context").label("Calendar_sync_include_context")
                        .description(
                            "For activities that sync to an external calendar, this setting indicates if the activity syncs with context (what are the deals, persons, organizations this activity is related to)")
                        .required(false),
                    string("marked_as_done_time").label("Marked_as_done_time")
                        .description("The date and time this activity was marked as done. Format: YYYY-MM-DD HH:MM:SS.")
                        .required(false),
                    string("location_formatted_address").label("Location_formatted_address")
                        .description("Subfield of location field. Indicates full/combined address.")
                        .required(false),
                    string("conference_meeting_url").label("Conference_meeting_url")
                        .description("The link to join the meeting which is associated with this activity")
                        .required(false),
                    array("series").items(object(null).description(
                        "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`"))
                        .placeholder("Add")
                        .label("Series")
                        .description(
                            "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`")
                        .required(false),
                    string("location").label("Location")
                        .description(
                            "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                        .required(false),
                    string("deal_dropbox_bcc").label("Deal_dropbox_bcc")
                        .description("The BCC email address of the deal")
                        .required(false),
                    string("add_time").label("Add_time")
                        .description("The creation date and time of the activity in UTC. Format: YYYY-MM-DD HH:MM:SS.")
                        .required(false)))
                    .placeholder("Add")
                    .label("Data")
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
                    .required(false),
                    object("deal").properties(object("DEAL_ID").properties(integer("id").label("Id")
                        .description("The ID of the deal associated with the item")
                        .required(false),
                        string("title").label("Title")
                            .description("The title of the deal associated with the item")
                            .required(false),
                        string("status").label("Status")
                            .description("The status of the deal associated with the item")
                            .required(false),
                        number("value").label("Value")
                            .description("The value of the deal that is associated with the item")
                            .required(false),
                        string("currency").label("Currency")
                            .description("The currency of the deal value")
                            .required(false),
                        integer("stage_id").label("Stage_id")
                            .description("The ID of the stage the deal is currently at")
                            .required(false),
                        integer("pipeline_id").label("Pipeline_id")
                            .description("The ID of the pipeline the deal is in")
                            .required(false))
                        .label("DEAL_ID")
                        .description("The ID of the deal which is associated with the item")
                        .required(false))
                        .label("Deal")
                        .required(false),
                    object("person").properties(string("name").label("Name")
                        .description("The name of the person associated with the item")
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
                    object("organization").properties(string("name").label("Name")
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
                        .required(false))
                    .label("Related_objects")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_lat\":59.4281884,\"location_long\":24.7041378,\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}}],\"related_objects\":{\"user\":{\"1234\":{\"id\":1234,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"cc_email\":\"org@pipedrivemail.com\"}},\"person\":{\"1101\":{\"id\":1101,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"deal\":{\"300\":{\"id\":300,\"title\":\"Deal\",\"status\":\"open\",\"value\":856,\"currency\":\"EUR\",\"stage_id\":1,\"pipeline_id\":1}}},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false,\"next_start\":1}}}"),
        action("addActivity")
            .display(
                display("Add an activity")
                    .description(
                        "Adds a new activity. Includes `more_activities_scheduled_in_context` property in response's `additional_data` which indicates whether there are more undone activities scheduled with the same deal, person or organization (depending on the supplied data). For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/adding-an-activity\" target=\"_blank\" rel=\"noopener noreferrer\">adding an activity</a>."))
            .metadata(
                Map.of(
                    "requestMethod", "POST",
                    "path", "/activities", "bodyContentType", "JSON"

                ))
            .properties(object(null).properties(string("due_time").label("Due_time")
                .description("The due time of the activity in UTC. Format: HH:MM")
                .required(false),
                string("note").label("Note")
                    .description("The note of the activity (HTML format)")
                    .required(false),
                string("public_description").label("Public_description")
                    .description(
                        "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                    .required(false),
                string("subject").label("Subject")
                    .description(
                        "The subject of the activity. When value for subject is not set, it will be given a default value `Call`.")
                    .required(false),
                array("attendees").items(object(null).description(
                    "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address. It requires a structure as follows: `[{\"email_address\":\"mail@example.org\"}]` or `[{\"person_id\":1, \"email_address\":\"mail@example.org\"}]`"))
                    .placeholder("Add")
                    .label("Attendees")
                    .description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address. It requires a structure as follows: `[{\"email_address\":\"mail@example.org\"}]` or `[{\"person_id\":1, \"email_address\":\"mail@example.org\"}]`")
                    .required(false),
                date("due_date").label("Due_date")
                    .description("The due date of the activity. Format: YYYY-MM-DD")
                    .required(false),
                string("type").label("Type")
                    .description(
                        "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes. When value for type is not set, it will be given a default value `Call`.")
                    .required(false),
                number("done").label("Done")
                    .description("Whether the activity is done or not. 0 = Not done, 1 = Done")
                    .options(option("0", 0), option("1", 1))
                    .required(false),
                string("duration").label("Duration")
                    .description("The duration of the activity. Format: HH:MM")
                    .required(false),
                integer("user_id").label("User_id")
                    .description(
                        "The ID of the user whom the activity is assigned to. If omitted, the activity is assigned to the authorized user.")
                    .required(false),
                integer("org_id").label("Org_id")
                    .description("The ID of the organization this activity is associated with")
                    .required(false),
                string("location").label("Location")
                    .description(
                        "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                    .required(false),
                integer("deal_id").label("Deal_id")
                    .description("The ID of the deal this activity is associated with")
                    .required(false),
                string("lead_id").label("Lead_id")
                    .description("The ID of the lead this activity is associated with")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of the person this activity is associated with")
                    .required(false),
                array("participants").items(object(null).description(
                    "List of multiple persons (participants) this activity is associated with. If omitted, single participant from `person_id` field is used. It requires a structure as follows: `[{\"person_id\":1,\"primary_flag\":true}]`"))
                    .placeholder("Add")
                    .label("Participants")
                    .description(
                        "List of multiple persons (participants) this activity is associated with. If omitted, single participant from `person_id` field is used. It requires a structure as follows: `[{\"person_id\":1,\"primary_flag\":true}]`")
                    .required(false),
                bool("busy_flag").label("Busy_flag")
                    .description(
                        "Set the activity as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset by never setting it or overriding it with `null`. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                    .options(option("True", true), option("False", false))
                    .required(false))
                .metadata(
                    Map.of(
                        "type", "BODY")))
            .output(object(null).properties(bool("success").label("Success")
                .required(false),
                string("last_notification_time").label("Last_notification_time")
                    .description(
                        "The date and time of latest notifications sent about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("location_street_number").label("Location_street_number")
                    .description("Subfield of location field. Indicates house number.")
                    .required(false),
                string("public_description").label("Public_description")
                    .description(
                        "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                    .required(false),
                integer("reference_id").label("Reference_id")
                    .description("Together with the `reference_type`, gives the ID of the other object")
                    .required(false),
                string("location_route").label("Location_route")
                    .description("Subfield of location field. Indicates street name.")
                    .required(false),
                integer("notification_language_id").label("Notification_language_id")
                    .description("The ID of the language the notifications are sent in")
                    .required(false),
                string("subject").label("Subject")
                    .description("The subject of the activity")
                    .required(false),
                string("type").label("Type")
                    .description(
                        "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes.")
                    .required(false),
                string("google_calendar_etag").label("Google_calendar_etag")
                    .description(
                        "The Google calendar API etag (version) that is used for syncing this activity. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("google_calendar_id").label("Google_calendar_id")
                    .description(
                        "The Google calendar ID that this activity syncs to. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("deal_title").label("Deal_title")
                    .description("The name of the deal this activity is associated with")
                    .required(false),
                integer("id").label("Id")
                    .description("The activity ID, generated when the activity was created")
                    .required(false),
                integer("deal_id").label("Deal_id")
                    .description("The ID of the deal this activity is associated with")
                    .required(false),
                string("gcal_event_id").label("Gcal_event_id")
                    .description(
                        "For the activity which syncs to Google calendar, this is the Google event ID. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                number("location_lat").label("Location_lat")
                    .description("Subfield of location field. Indicates latitude.")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of the person this activity is associated with")
                    .required(false),
                bool("busy_flag").label("Busy_flag")
                    .description(
                        "Marks if the activity is set as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                    .options(option("True", true), option("False", false))
                    .required(false),
                number("location_long").label("Location_long")
                    .description("Subfield of location field. Indicates longitude.")
                    .required(false),
                string("owner_name").label("Owner_name")
                    .description("The name of the user this activity is owned by")
                    .required(false),
                array("attendees").items(object(null).description(
                    "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address."))
                    .placeholder("Add")
                    .label("Attendees")
                    .description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address.")
                    .required(false),
                string("person_name").label("Person_name")
                    .description("The name of the person this activity is associated with")
                    .required(false),
                string("rec_rule_extension").label("Rec_rule_extension")
                    .description(
                        "Additional rules for the recurrence of the activity, extend the `rec_rule`. Is important for activities synced into Pipedrive from an external calendar.")
                    .required(false),
                bool("done").label("Done")
                    .description("Whether the activity is done or not")
                    .required(false),
                integer("created_by_user_id").label("Created_by_user_id")
                    .description("The ID of the user who created the activity")
                    .required(false),
                string("location_sublocality").label("Location_sublocality")
                    .description("Subfield of location field. Indicates district/sublocality.")
                    .required(false),
                string("rec_rule").label("Rec_rule")
                    .description(
                        "The rule for the recurrence of the activity. Is important for activities synced into Pipedrive from an external calendar. Example: \"RRULE:FREQ=WEEKLY;BYDAY=WE\"")
                    .required(false),
                string("location_admin_area_level_2").label("Location_admin_area_level_2")
                    .description("Subfield of location field. Indicates region.")
                    .required(false),
                integer("user_id").label("User_id")
                    .description("The ID of the user whom the activity is assigned to")
                    .required(false),
                string("location_admin_area_level_1").label("Location_admin_area_level_1")
                    .description("Subfield of location field. Indicates state/county.")
                    .required(false),
                integer("org_id").label("Org_id")
                    .description("The ID of the organization this activity is associated with")
                    .required(false),
                string("conference_meeting_client").label("Conference_meeting_client")
                    .description("The ID of Marketplace app, which is connected to this activity")
                    .required(false),
                string("due_time").label("Due_time")
                    .description("The due time of the activity in UTC. Format: HH:MM")
                    .required(false),
                string("note").label("Note")
                    .description("The note of the activity (HTML format)")
                    .required(false),
                integer("rec_master_activity_id").label("Rec_master_activity_id")
                    .description(
                        "The ID of parent activity for a recurrent activity if the current activity is an exception to recurrence rules")
                    .required(false),
                string("location_country").label("Location_country")
                    .description("Subfield of location field. Indicates country.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the activity is active or not")
                    .required(false),
                string("duration").label("Duration")
                    .description("The duration of the activity. Format: HH:MM")
                    .required(false),
                string("location_postal_code").label("Location_postal_code")
                    .description("Subfield of location field. Indicates ZIP/postal code.")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last update date and time of the activity. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                object("file").label("File")
                    .description(
                        "The file that is attached to this activity. For example, this can be a reference to an audio note file generated with Pipedrive mobile app.")
                    .required(false),
                integer("update_user_id").label("Update_user_id")
                    .description("The ID of the user who was the last to update this activity")
                    .required(false),
                string("source_timezone").label("Source_timezone")
                    .description("The timezone the activity was created in an external calendar")
                    .required(false),
                string("conference_meeting_id").label("Conference_meeting_id")
                    .description(
                        "The meeting ID of the meeting provider (Zoom, MS Teams etc.) that is associated with this activity")
                    .required(false),
                string("person_dropbox_bcc").label("Person_dropbox_bcc")
                    .description("The BCC email address of the person")
                    .required(false),
                string("location_locality").label("Location_locality")
                    .description("Subfield of location field. Indicates city/town/village/locality.")
                    .required(false),
                string("org_name").label("Org_name")
                    .description("The name of the organization this activity is associated with")
                    .required(false),
                integer("assigned_to_user_id").label("Assigned_to_user_id")
                    .description("The ID of the user to whom the activity is assigned to. Equal to `user_id`.")
                    .required(false),
                string("lead_id").label("Lead_id")
                    .description("The ID of the lead in the UUID format this activity is associated with")
                    .required(false),
                array("participants")
                    .items(object(null)
                        .description("List of multiple persons (participants) this activity is associated with"))
                    .placeholder("Add")
                    .label("Participants")
                    .description("List of multiple persons (participants) this activity is associated with")
                    .required(false),
                string("location_subpremise").label("Location_subpremise")
                    .description("Subfield of location field. Indicates apartment/suite number.")
                    .required(false),
                integer("company_id").label("Company_id")
                    .description("The user's company ID")
                    .required(false),
                date("due_date").label("Due_date")
                    .description("The due date of the activity. Format: YYYY-MM-DD")
                    .required(false),
                string("reference_type").label("Reference_type")
                    .description(
                        "If the activity references some other object, it is indicated here. For example, value `Salesphone` refers to activities created with Caller.")
                    .required(false),
                integer("last_notification_user_id").label("Last_notification_user_id")
                    .description(
                        "The ID of the user who triggered the sending of the latest notifications about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("calendar_sync_include_context").label("Calendar_sync_include_context")
                    .description(
                        "For activities that sync to an external calendar, this setting indicates if the activity syncs with context (what are the deals, persons, organizations this activity is related to)")
                    .required(false),
                string("marked_as_done_time").label("Marked_as_done_time")
                    .description("The date and time this activity was marked as done. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                string("location_formatted_address").label("Location_formatted_address")
                    .description("Subfield of location field. Indicates full/combined address.")
                    .required(false),
                string("conference_meeting_url").label("Conference_meeting_url")
                    .description("The link to join the meeting which is associated with this activity")
                    .required(false),
                array("series").items(object(null).description(
                    "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`"))
                    .placeholder("Add")
                    .label("Series")
                    .description(
                        "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`")
                    .required(false),
                string("location").label("Location")
                    .description(
                        "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                    .required(false),
                string("deal_dropbox_bcc").label("Deal_dropbox_bcc")
                    .description("The BCC email address of the deal")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the activity in UTC. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                object("additional_data").properties(integer("updates_story_id").label("Updates_story_id")
                    .description("This field will be deprecated")
                    .required(false))
                    .label("Additional_data")
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
                    .required(false),
                    object("deal").properties(object("DEAL_ID").properties(integer("id").label("Id")
                        .description("The ID of the deal associated with the item")
                        .required(false),
                        string("title").label("Title")
                            .description("The title of the deal associated with the item")
                            .required(false),
                        string("status").label("Status")
                            .description("The status of the deal associated with the item")
                            .required(false),
                        number("value").label("Value")
                            .description("The value of the deal that is associated with the item")
                            .required(false),
                        string("currency").label("Currency")
                            .description("The currency of the deal value")
                            .required(false),
                        integer("stage_id").label("Stage_id")
                            .description("The ID of the stage the deal is currently at")
                            .required(false),
                        integer("pipeline_id").label("Pipeline_id")
                            .description("The ID of the pipeline the deal is in")
                            .required(false))
                        .label("DEAL_ID")
                        .description("The ID of the deal which is associated with the item")
                        .required(false))
                        .label("Deal")
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
                        .required(false))
                    .label("Related_objects")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_lat\":59.4281884,\"location_long\":24.7041378,\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}},\"related_objects\":{\"user\":{\"1234\":{\"id\":1234,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"cc_email\":\"org@pipedrivemail.com\",\"active_flag\":true}},\"person\":{\"1101\":{\"id\":1101,\"name\":\"Person\",\"active_flag\":true,\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"deal\":{\"300\":{\"id\":300,\"title\":\"Deal\",\"status\":\"open\",\"value\":856,\"currency\":\"EUR\",\"stage_id\":1,\"pipeline_id\":1}}},\"additional_data\":{\"updates_story_id\":2039}}"),
        action("deleteActivity")
            .display(
                display("Delete an activity")
                    .description(
                        "Marks an activity as deleted. After 30 days, the activity will be permanently deleted."))
            .metadata(
                Map.of(
                    "requestMethod", "DELETE",
                    "path", "/activities/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the activity")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")))
            .output(object(null).properties(bool("success").label("Success")
                .required(false),
                object("data").properties(integer("id").label("Id")
                    .description("The ID of the activity that was deleted")
                    .required(false))
                    .label("Data")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput("{\"success\":true,\"data\":{\"id\":624}}"),
        action("getActivity")
            .display(
                display("Get details of an activity")
                    .description("Returns the details of a specific activity."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/activities/{id}"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the activity")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")))
            .output(object(null).properties(bool("success").label("Success")
                .required(false),
                string("last_notification_time").label("Last_notification_time")
                    .description(
                        "The date and time of latest notifications sent about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("location_street_number").label("Location_street_number")
                    .description("Subfield of location field. Indicates house number.")
                    .required(false),
                string("public_description").label("Public_description")
                    .description(
                        "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                    .required(false),
                integer("reference_id").label("Reference_id")
                    .description("Together with the `reference_type`, gives the ID of the other object")
                    .required(false),
                string("location_route").label("Location_route")
                    .description("Subfield of location field. Indicates street name.")
                    .required(false),
                integer("notification_language_id").label("Notification_language_id")
                    .description("The ID of the language the notifications are sent in")
                    .required(false),
                string("subject").label("Subject")
                    .description("The subject of the activity")
                    .required(false),
                string("type").label("Type")
                    .description(
                        "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes.")
                    .required(false),
                string("google_calendar_etag").label("Google_calendar_etag")
                    .description(
                        "The Google calendar API etag (version) that is used for syncing this activity. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("google_calendar_id").label("Google_calendar_id")
                    .description(
                        "The Google calendar ID that this activity syncs to. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("deal_title").label("Deal_title")
                    .description("The name of the deal this activity is associated with")
                    .required(false),
                integer("id").label("Id")
                    .description("The activity ID, generated when the activity was created")
                    .required(false),
                integer("deal_id").label("Deal_id")
                    .description("The ID of the deal this activity is associated with")
                    .required(false),
                string("gcal_event_id").label("Gcal_event_id")
                    .description(
                        "For the activity which syncs to Google calendar, this is the Google event ID. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                number("location_lat").label("Location_lat")
                    .description("Subfield of location field. Indicates latitude.")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of the person this activity is associated with")
                    .required(false),
                bool("busy_flag").label("Busy_flag")
                    .description(
                        "Marks if the activity is set as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                    .options(option("True", true), option("False", false))
                    .required(false),
                number("location_long").label("Location_long")
                    .description("Subfield of location field. Indicates longitude.")
                    .required(false),
                string("owner_name").label("Owner_name")
                    .description("The name of the user this activity is owned by")
                    .required(false),
                array("attendees").items(object(null).description(
                    "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address."))
                    .placeholder("Add")
                    .label("Attendees")
                    .description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address.")
                    .required(false),
                string("person_name").label("Person_name")
                    .description("The name of the person this activity is associated with")
                    .required(false),
                string("rec_rule_extension").label("Rec_rule_extension")
                    .description(
                        "Additional rules for the recurrence of the activity, extend the `rec_rule`. Is important for activities synced into Pipedrive from an external calendar.")
                    .required(false),
                bool("done").label("Done")
                    .description("Whether the activity is done or not")
                    .required(false),
                integer("created_by_user_id").label("Created_by_user_id")
                    .description("The ID of the user who created the activity")
                    .required(false),
                string("location_sublocality").label("Location_sublocality")
                    .description("Subfield of location field. Indicates district/sublocality.")
                    .required(false),
                string("rec_rule").label("Rec_rule")
                    .description(
                        "The rule for the recurrence of the activity. Is important for activities synced into Pipedrive from an external calendar. Example: \"RRULE:FREQ=WEEKLY;BYDAY=WE\"")
                    .required(false),
                string("location_admin_area_level_2").label("Location_admin_area_level_2")
                    .description("Subfield of location field. Indicates region.")
                    .required(false),
                integer("user_id").label("User_id")
                    .description("The ID of the user whom the activity is assigned to")
                    .required(false),
                string("location_admin_area_level_1").label("Location_admin_area_level_1")
                    .description("Subfield of location field. Indicates state/county.")
                    .required(false),
                integer("org_id").label("Org_id")
                    .description("The ID of the organization this activity is associated with")
                    .required(false),
                string("conference_meeting_client").label("Conference_meeting_client")
                    .description("The ID of Marketplace app, which is connected to this activity")
                    .required(false),
                string("due_time").label("Due_time")
                    .description("The due time of the activity in UTC. Format: HH:MM")
                    .required(false),
                string("note").label("Note")
                    .description("The note of the activity (HTML format)")
                    .required(false),
                integer("rec_master_activity_id").label("Rec_master_activity_id")
                    .description(
                        "The ID of parent activity for a recurrent activity if the current activity is an exception to recurrence rules")
                    .required(false),
                string("location_country").label("Location_country")
                    .description("Subfield of location field. Indicates country.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the activity is active or not")
                    .required(false),
                string("duration").label("Duration")
                    .description("The duration of the activity. Format: HH:MM")
                    .required(false),
                string("location_postal_code").label("Location_postal_code")
                    .description("Subfield of location field. Indicates ZIP/postal code.")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last update date and time of the activity. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                object("file").label("File")
                    .description(
                        "The file that is attached to this activity. For example, this can be a reference to an audio note file generated with Pipedrive mobile app.")
                    .required(false),
                integer("update_user_id").label("Update_user_id")
                    .description("The ID of the user who was the last to update this activity")
                    .required(false),
                string("source_timezone").label("Source_timezone")
                    .description("The timezone the activity was created in an external calendar")
                    .required(false),
                string("conference_meeting_id").label("Conference_meeting_id")
                    .description(
                        "The meeting ID of the meeting provider (Zoom, MS Teams etc.) that is associated with this activity")
                    .required(false),
                string("person_dropbox_bcc").label("Person_dropbox_bcc")
                    .description("The BCC email address of the person")
                    .required(false),
                string("location_locality").label("Location_locality")
                    .description("Subfield of location field. Indicates city/town/village/locality.")
                    .required(false),
                string("org_name").label("Org_name")
                    .description("The name of the organization this activity is associated with")
                    .required(false),
                integer("assigned_to_user_id").label("Assigned_to_user_id")
                    .description("The ID of the user to whom the activity is assigned to. Equal to `user_id`.")
                    .required(false),
                string("lead_id").label("Lead_id")
                    .description("The ID of the lead in the UUID format this activity is associated with")
                    .required(false),
                array("participants")
                    .items(object(null)
                        .description("List of multiple persons (participants) this activity is associated with"))
                    .placeholder("Add")
                    .label("Participants")
                    .description("List of multiple persons (participants) this activity is associated with")
                    .required(false),
                string("location_subpremise").label("Location_subpremise")
                    .description("Subfield of location field. Indicates apartment/suite number.")
                    .required(false),
                integer("company_id").label("Company_id")
                    .description("The user's company ID")
                    .required(false),
                date("due_date").label("Due_date")
                    .description("The due date of the activity. Format: YYYY-MM-DD")
                    .required(false),
                string("reference_type").label("Reference_type")
                    .description(
                        "If the activity references some other object, it is indicated here. For example, value `Salesphone` refers to activities created with Caller.")
                    .required(false),
                integer("last_notification_user_id").label("Last_notification_user_id")
                    .description(
                        "The ID of the user who triggered the sending of the latest notifications about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("calendar_sync_include_context").label("Calendar_sync_include_context")
                    .description(
                        "For activities that sync to an external calendar, this setting indicates if the activity syncs with context (what are the deals, persons, organizations this activity is related to)")
                    .required(false),
                string("marked_as_done_time").label("Marked_as_done_time")
                    .description("The date and time this activity was marked as done. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                string("location_formatted_address").label("Location_formatted_address")
                    .description("Subfield of location field. Indicates full/combined address.")
                    .required(false),
                string("conference_meeting_url").label("Conference_meeting_url")
                    .description("The link to join the meeting which is associated with this activity")
                    .required(false),
                array("series").items(object(null).description(
                    "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`"))
                    .placeholder("Add")
                    .label("Series")
                    .description(
                        "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`")
                    .required(false),
                string("location").label("Location")
                    .description(
                        "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                    .required(false),
                string("deal_dropbox_bcc").label("Deal_dropbox_bcc")
                    .description("The BCC email address of the deal")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the activity in UTC. Format: YYYY-MM-DD HH:MM:SS.")
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
                    .required(false),
                    object("deal").properties(object("DEAL_ID").properties(integer("id").label("Id")
                        .description("The ID of the deal associated with the item")
                        .required(false),
                        string("title").label("Title")
                            .description("The title of the deal associated with the item")
                            .required(false),
                        string("status").label("Status")
                            .description("The status of the deal associated with the item")
                            .required(false),
                        number("value").label("Value")
                            .description("The value of the deal that is associated with the item")
                            .required(false),
                        string("currency").label("Currency")
                            .description("The currency of the deal value")
                            .required(false),
                        integer("stage_id").label("Stage_id")
                            .description("The ID of the stage the deal is currently at")
                            .required(false),
                        integer("pipeline_id").label("Pipeline_id")
                            .description("The ID of the pipeline the deal is in")
                            .required(false))
                        .label("DEAL_ID")
                        .description("The ID of the deal which is associated with the item")
                        .required(false))
                        .label("Deal")
                        .required(false),
                    object("person").properties(string("name").label("Name")
                        .description("The name of the person associated with the item")
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
                    object("organization").properties(string("name").label("Name")
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
                        .required(false))
                    .label("Related_objects")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_lat\":59.4281884,\"location_long\":24.7041378,\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}},\"related_objects\":{\"user\":{\"1234\":{\"id\":1234,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"cc_email\":\"org@pipedrivemail.com\"}},\"person\":{\"1101\":{\"id\":1101,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"deal\":{\"300\":{\"id\":300,\"title\":\"Deal\",\"status\":\"open\",\"value\":856,\"currency\":\"EUR\",\"stage_id\":1,\"pipeline_id\":1}}}}"),
        action("updateActivity")
            .display(
                display("Update an activity")
                    .description(
                        "Updates an activity. Includes `more_activities_scheduled_in_context` property in response's `additional_data` which indicates whether there are more undone activities scheduled with the same deal, person or organization (depending on the supplied data)."))
            .metadata(
                Map.of(
                    "requestMethod", "PUT",
                    "path", "/activities/{id}", "bodyContentType", "JSON"

                ))
            .properties(integer("id").label("Id")
                .description("The ID of the activity")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "PATH")),
                object(null).properties(string("due_time").label("Due_time")
                    .description("The due time of the activity in UTC. Format: HH:MM")
                    .required(false),
                    string("note").label("Note")
                        .description("The note of the activity (HTML format)")
                        .required(false),
                    string("public_description").label("Public_description")
                        .description(
                            "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                        .required(false),
                    string("subject").label("Subject")
                        .description("The subject of the activity")
                        .required(false),
                    array("attendees").items(object(null).description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address. It requires a structure as follows: `[{\"email_address\":\"mail@example.org\"}]` or `[{\"person_id\":1, \"email_address\":\"mail@example.org\"}]`"))
                        .placeholder("Add")
                        .label("Attendees")
                        .description(
                            "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address. It requires a structure as follows: `[{\"email_address\":\"mail@example.org\"}]` or `[{\"person_id\":1, \"email_address\":\"mail@example.org\"}]`")
                        .required(false),
                    date("due_date").label("Due_date")
                        .description("The due date of the activity. Format: YYYY-MM-DD")
                        .required(false),
                    string("type").label("Type")
                        .description(
                            "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes.")
                        .required(false),
                    number("done").label("Done")
                        .description("Whether the activity is done or not. 0 = Not done, 1 = Done")
                        .options(option("0", 0), option("1", 1))
                        .required(false),
                    string("duration").label("Duration")
                        .description("The duration of the activity. Format: HH:MM")
                        .required(false),
                    integer("user_id").label("User_id")
                        .description("The ID of the user whom the activity is assigned to")
                        .required(false),
                    integer("org_id").label("Org_id")
                        .description("The ID of the organization this activity is associated with")
                        .required(false),
                    string("location").label("Location")
                        .description(
                            "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                        .required(false),
                    integer("deal_id").label("Deal_id")
                        .description("The ID of the deal this activity is associated with")
                        .required(false),
                    string("lead_id").label("Lead_id")
                        .description("The ID of the lead this activity is associated with")
                        .required(false),
                    integer("person_id").label("Person_id")
                        .description("The ID of the person this activity is associated with")
                        .required(false),
                    array("participants").items(object(null).description(
                        "List of multiple persons (participants) this activity is associated with. It requires a structure as follows: `[{\"person_id\":1,\"primary_flag\":true}]`"))
                        .placeholder("Add")
                        .label("Participants")
                        .description(
                            "List of multiple persons (participants) this activity is associated with. It requires a structure as follows: `[{\"person_id\":1,\"primary_flag\":true}]`")
                        .required(false),
                    bool("busy_flag").label("Busy_flag")
                        .description(
                            "Set the activity as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset by never setting it or overriding it with `null`. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                        .options(option("True", true), option("False", false))
                        .required(false))
                    .metadata(
                        Map.of(
                            "type", "BODY")))
            .output(object(null).properties(bool("success").label("Success")
                .required(false),
                string("last_notification_time").label("Last_notification_time")
                    .description(
                        "The date and time of latest notifications sent about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("location_street_number").label("Location_street_number")
                    .description("Subfield of location field. Indicates house number.")
                    .required(false),
                string("public_description").label("Public_description")
                    .description(
                        "Additional details about the activity that is synced to your external calendar. Unlike the note added to the activity, the description is publicly visible to any guests added to the activity.")
                    .required(false),
                integer("reference_id").label("Reference_id")
                    .description("Together with the `reference_type`, gives the ID of the other object")
                    .required(false),
                string("location_route").label("Location_route")
                    .description("Subfield of location field. Indicates street name.")
                    .required(false),
                integer("notification_language_id").label("Notification_language_id")
                    .description("The ID of the language the notifications are sent in")
                    .required(false),
                string("subject").label("Subject")
                    .description("The subject of the activity")
                    .required(false),
                string("type").label("Type")
                    .description(
                        "The type of the activity. This is in correlation with the `key_string` parameter of ActivityTypes.")
                    .required(false),
                string("google_calendar_etag").label("Google_calendar_etag")
                    .description(
                        "The Google calendar API etag (version) that is used for syncing this activity. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("google_calendar_id").label("Google_calendar_id")
                    .description(
                        "The Google calendar ID that this activity syncs to. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                string("deal_title").label("Deal_title")
                    .description("The name of the deal this activity is associated with")
                    .required(false),
                integer("id").label("Id")
                    .description("The activity ID, generated when the activity was created")
                    .required(false),
                integer("deal_id").label("Deal_id")
                    .description("The ID of the deal this activity is associated with")
                    .required(false),
                string("gcal_event_id").label("Gcal_event_id")
                    .description(
                        "For the activity which syncs to Google calendar, this is the Google event ID. NB! This field is related to old Google calendar sync and will be deprecated soon.")
                    .required(false),
                number("location_lat").label("Location_lat")
                    .description("Subfield of location field. Indicates latitude.")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of the person this activity is associated with")
                    .required(false),
                bool("busy_flag").label("Busy_flag")
                    .description(
                        "Marks if the activity is set as 'Busy' or 'Free'. If the flag is set to `true`, your customers will not be able to book that time slot through any Scheduler links. The flag can also be unset. When the value of the flag is unset (`null`), the flag defaults to 'Busy' if it has a time set, and 'Free' if it is an all-day event without specified time.")
                    .options(option("True", true), option("False", false))
                    .required(false),
                number("location_long").label("Location_long")
                    .description("Subfield of location field. Indicates longitude.")
                    .required(false),
                string("owner_name").label("Owner_name")
                    .description("The name of the user this activity is owned by")
                    .required(false),
                array("attendees").items(object(null).description(
                    "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address."))
                    .placeholder("Add")
                    .label("Attendees")
                    .description(
                        "The attendees of the activity. This can be either your existing Pipedrive contacts or an external email address.")
                    .required(false),
                string("person_name").label("Person_name")
                    .description("The name of the person this activity is associated with")
                    .required(false),
                string("rec_rule_extension").label("Rec_rule_extension")
                    .description(
                        "Additional rules for the recurrence of the activity, extend the `rec_rule`. Is important for activities synced into Pipedrive from an external calendar.")
                    .required(false),
                bool("done").label("Done")
                    .description("Whether the activity is done or not")
                    .required(false),
                integer("created_by_user_id").label("Created_by_user_id")
                    .description("The ID of the user who created the activity")
                    .required(false),
                string("location_sublocality").label("Location_sublocality")
                    .description("Subfield of location field. Indicates district/sublocality.")
                    .required(false),
                string("rec_rule").label("Rec_rule")
                    .description(
                        "The rule for the recurrence of the activity. Is important for activities synced into Pipedrive from an external calendar. Example: \"RRULE:FREQ=WEEKLY;BYDAY=WE\"")
                    .required(false),
                string("location_admin_area_level_2").label("Location_admin_area_level_2")
                    .description("Subfield of location field. Indicates region.")
                    .required(false),
                integer("user_id").label("User_id")
                    .description("The ID of the user whom the activity is assigned to")
                    .required(false),
                string("location_admin_area_level_1").label("Location_admin_area_level_1")
                    .description("Subfield of location field. Indicates state/county.")
                    .required(false),
                integer("org_id").label("Org_id")
                    .description("The ID of the organization this activity is associated with")
                    .required(false),
                string("conference_meeting_client").label("Conference_meeting_client")
                    .description("The ID of Marketplace app, which is connected to this activity")
                    .required(false),
                string("due_time").label("Due_time")
                    .description("The due time of the activity in UTC. Format: HH:MM")
                    .required(false),
                string("note").label("Note")
                    .description("The note of the activity (HTML format)")
                    .required(false),
                integer("rec_master_activity_id").label("Rec_master_activity_id")
                    .description(
                        "The ID of parent activity for a recurrent activity if the current activity is an exception to recurrence rules")
                    .required(false),
                string("location_country").label("Location_country")
                    .description("Subfield of location field. Indicates country.")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the activity is active or not")
                    .required(false),
                string("duration").label("Duration")
                    .description("The duration of the activity. Format: HH:MM")
                    .required(false),
                string("location_postal_code").label("Location_postal_code")
                    .description("Subfield of location field. Indicates ZIP/postal code.")
                    .required(false),
                string("update_time").label("Update_time")
                    .description("The last update date and time of the activity. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                object("file").label("File")
                    .description(
                        "The file that is attached to this activity. For example, this can be a reference to an audio note file generated with Pipedrive mobile app.")
                    .required(false),
                integer("update_user_id").label("Update_user_id")
                    .description("The ID of the user who was the last to update this activity")
                    .required(false),
                string("source_timezone").label("Source_timezone")
                    .description("The timezone the activity was created in an external calendar")
                    .required(false),
                string("conference_meeting_id").label("Conference_meeting_id")
                    .description(
                        "The meeting ID of the meeting provider (Zoom, MS Teams etc.) that is associated with this activity")
                    .required(false),
                string("person_dropbox_bcc").label("Person_dropbox_bcc")
                    .description("The BCC email address of the person")
                    .required(false),
                string("location_locality").label("Location_locality")
                    .description("Subfield of location field. Indicates city/town/village/locality.")
                    .required(false),
                string("org_name").label("Org_name")
                    .description("The name of the organization this activity is associated with")
                    .required(false),
                integer("assigned_to_user_id").label("Assigned_to_user_id")
                    .description("The ID of the user to whom the activity is assigned to. Equal to `user_id`.")
                    .required(false),
                string("lead_id").label("Lead_id")
                    .description("The ID of the lead in the UUID format this activity is associated with")
                    .required(false),
                array("participants")
                    .items(object(null)
                        .description("List of multiple persons (participants) this activity is associated with"))
                    .placeholder("Add")
                    .label("Participants")
                    .description("List of multiple persons (participants) this activity is associated with")
                    .required(false),
                string("location_subpremise").label("Location_subpremise")
                    .description("Subfield of location field. Indicates apartment/suite number.")
                    .required(false),
                integer("company_id").label("Company_id")
                    .description("The user's company ID")
                    .required(false),
                date("due_date").label("Due_date")
                    .description("The due date of the activity. Format: YYYY-MM-DD")
                    .required(false),
                string("reference_type").label("Reference_type")
                    .description(
                        "If the activity references some other object, it is indicated here. For example, value `Salesphone` refers to activities created with Caller.")
                    .required(false),
                integer("last_notification_user_id").label("Last_notification_user_id")
                    .description(
                        "The ID of the user who triggered the sending of the latest notifications about this activity to the participants or the attendees of this activity")
                    .required(false),
                string("calendar_sync_include_context").label("Calendar_sync_include_context")
                    .description(
                        "For activities that sync to an external calendar, this setting indicates if the activity syncs with context (what are the deals, persons, organizations this activity is related to)")
                    .required(false),
                string("marked_as_done_time").label("Marked_as_done_time")
                    .description("The date and time this activity was marked as done. Format: YYYY-MM-DD HH:MM:SS.")
                    .required(false),
                string("location_formatted_address").label("Location_formatted_address")
                    .description("Subfield of location field. Indicates full/combined address.")
                    .required(false),
                string("conference_meeting_url").label("Conference_meeting_url")
                    .description("The link to join the meeting which is associated with this activity")
                    .required(false),
                array("series").items(object(null).description(
                    "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`"))
                    .placeholder("Add")
                    .label("Series")
                    .description(
                        "The list of recurring activity instances. It is in a structure as follows: `[{due_date: \"2020-06-24\", due_time: \"10:00:00\"}]`")
                    .required(false),
                string("location").label("Location")
                    .description(
                        "The address of the activity. Pipedrive will automatically check if the location matches a geo-location on Google maps.")
                    .required(false),
                string("deal_dropbox_bcc").label("Deal_dropbox_bcc")
                    .description("The BCC email address of the deal")
                    .required(false),
                string("add_time").label("Add_time")
                    .description("The creation date and time of the activity in UTC. Format: YYYY-MM-DD HH:MM:SS.")
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
                    .required(false),
                    object("deal").properties(object("DEAL_ID").properties(integer("id").label("Id")
                        .description("The ID of the deal associated with the item")
                        .required(false),
                        string("title").label("Title")
                            .description("The title of the deal associated with the item")
                            .required(false),
                        string("status").label("Status")
                            .description("The status of the deal associated with the item")
                            .required(false),
                        number("value").label("Value")
                            .description("The value of the deal that is associated with the item")
                            .required(false),
                        string("currency").label("Currency")
                            .description("The currency of the deal value")
                            .required(false),
                        integer("stage_id").label("Stage_id")
                            .description("The ID of the stage the deal is currently at")
                            .required(false),
                        integer("pipeline_id").label("Pipeline_id")
                            .description("The ID of the pipeline the deal is in")
                            .required(false))
                        .label("DEAL_ID")
                        .description("The ID of the deal which is associated with the item")
                        .required(false))
                        .label("Deal")
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
                        .required(false))
                    .label("Related_objects")
                    .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", "JSON")))
            .exampleOutput(
                "{\"success\":true,\"data\":{\"id\":8,\"company_id\":22122,\"user_id\":1234,\"done\":false,\"type\":\"deadline\",\"reference_type\":\"scheduler-service\",\"reference_id\":7,\"conference_meeting_client\":\"871b8bc88d3a1202\",\"conference_meeting_url\":\"https://pipedrive.zoom.us/link\",\"conference_meeting_id\":\"01758746701\",\"due_date\":\"2020-06-09\",\"due_time\":\"10:00\",\"duration\":\"01:00\",\"busy_flag\":true,\"add_time\":\"2020-06-08 12:37:56\",\"marked_as_done_time\":\"2020-08-08 08:08:38\",\"last_notification_time\":\"2020-08-08 12:37:56\",\"last_notification_user_id\":7655,\"notification_language_id\":1,\"subject\":\"Deadline\",\"public_description\":\"This is a description\",\"calendar_sync_include_context\":\"\",\"location\":\"Mustamäe tee 3, Tallinn, Estonia\",\"org_id\":5,\"person_id\":1101,\"deal_id\":300,\"lead_id\":\"46c3b0e1-db35-59ca-1828-4817378dff71\",\"active_flag\":true,\"update_time\":\"2020-08-08 12:37:56\",\"update_user_id\":5596,\"gcal_event_id\":\"\",\"google_calendar_id\":\"\",\"google_calendar_etag\":\"\",\"source_timezone\":\"\",\"rec_rule\":\"RRULE:FREQ=WEEKLY;BYDAY=WE\",\"rec_rule_extension\":\"\",\"rec_master_activity_id\":1,\"series\":[],\"note\":\"A note for the activity\",\"created_by_user_id\":1234,\"location_subpremise\":\"\",\"location_street_number\":\"3\",\"location_route\":\"Mustamäe tee\",\"location_sublocality\":\"Kristiine\",\"location_locality\":\"Tallinn\",\"location_lat\":59.4281884,\"location_long\":24.7041378,\"location_admin_area_level_1\":\"Harju maakond\",\"location_admin_area_level_2\":\"\",\"location_country\":\"Estonia\",\"location_postal_code\":\"10616\",\"location_formatted_address\":\"Mustamäe tee 3, 10616 Tallinn, Estonia\",\"attendees\":[{\"email_address\":\"attendee@pipedrivemail.com\",\"is_organizer\":0,\"name\":\"Attendee\",\"person_id\":25312,\"status\":\"noreply\",\"user_id\":null}],\"participants\":[{\"person_id\":17985,\"primary_flag\":false},{\"person_id\":1101,\"primary_flag\":true}],\"org_name\":\"Organization\",\"person_name\":\"Person\",\"deal_title\":\"Deal\",\"owner_name\":\"Creator\",\"person_dropbox_bcc\":\"company@pipedrivemail.com\",\"deal_dropbox_bcc\":\"company+deal300@pipedrivemail.com\",\"assigned_to_user_id\":1235,\"file\":{\"id\":\"376892,\",\"clean_name\":\"Audio 10:55:07.m4a\",\"url\":\"https://pipedrive-files.s3-eu-west-1.amazonaws.com/Audio-recording.m4a\"}},\"related_objects\":{\"user\":{\"1234\":{\"id\":1234,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"5\":{\"id\":5,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"cc_email\":\"org@pipedrivemail.com\",\"active_flag\":true}},\"person\":{\"1101\":{\"id\":1101,\"name\":\"Person\",\"active_flag\":true,\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"deal\":{\"300\":{\"id\":300,\"title\":\"Deal\",\"status\":\"open\",\"value\":856,\"currency\":\"EUR\",\"stage_id\":1,\"pipeline_id\":1}}}}"));
}
