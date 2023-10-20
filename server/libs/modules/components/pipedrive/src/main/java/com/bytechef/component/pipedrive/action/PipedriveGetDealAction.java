
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
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
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
public class PipedriveGetDealAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getDeal")
        .title("Get details of a deal")
        .description(
            "Returns the details of a specific deal. Note that this also returns some additional fields which are not present when asking for all deals – such as deal age and stay in pipeline stages. Also note that custom fields appear as long hashes in the resulting data. These hashes can be mapped against the `key` value of dealFields. For more information, see the tutorial for <a href=\"https://pipedrive.readme.io/docs/getting-details-of-a-deal\" target=\"_blank\" rel=\"noopener noreferrer\">getting details of a deal</a>.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/deals/{id}"

            ))
        .properties(integer("id").label("Id")
            .description("The ID of the deal")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object().properties(bool("success").label("Success")
            .description("If the response is successful or not")
            .required(false),
            integer("email_messages_count").label("Email_messages_count")
                .description("The number of emails associated with the deal")
                .required(false),
            string("cc_email").label("Cc_email")
                .description("The BCC email of the deal")
                .required(false),
            object("stay_in_pipeline_stages").properties(object("times_in_stages").label("Times_in_stages")
                .description("The number of seconds a deal has been in each stage of the pipeline")
                .required(false),
                array("order_of_stages")
                    .items(integer(null).description("The order of the deal progression through the pipeline stages"))
                    .placeholder("Add")
                    .label("Order_of_stages")
                    .description("The order of the deal progression through the pipeline stages")
                    .required(false))
                .label("Stay_in_pipeline_stages")
                .description("The details of the duration of the deal being in each stage of the pipeline")
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
            array("phone").items(object().properties(string("label").label("Label")
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
            array("email").items(object().properties(string("label").label("Label")
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
            number("average_stage_progress").label("Average_stage_progress")
                .description("The average of the deal stage progression")
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
                .description("The number of people connected with the organization that is associated with the deal")
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
            object("average_time_to_won").properties(integer("y").label("Y")
                .description("Years")
                .required(false),
                integer("m").label("M")
                    .description("Months")
                    .required(false),
                integer("d").label("D")
                    .description("Days")
                    .required(false),
                integer("h").label("H")
                    .description("Hours")
                    .required(false),
                integer("i").label("I")
                    .description("Minutes")
                    .required(false),
                integer("s").label("S")
                    .description("Seconds")
                    .required(false),
                integer("total_seconds").label("Total_seconds")
                    .description("The total time in seconds")
                    .required(false))
                .label("Average_time_to_won")
                .description("The average time to win the deal")
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
            object("last_activity").label("Last_activity")
                .description("The details of the last activity associated with the deal")
                .required(false),
            object("next_activity").label("Next_activity")
                .description("The details of the next activity associated with the deal")
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
                .required(false),
            object("age").properties(integer("y").label("Y")
                .description("Years")
                .required(false),
                integer("m").label("M")
                    .description("Months")
                    .required(false),
                integer("d").label("D")
                    .description("Days")
                    .required(false),
                integer("h").label("H")
                    .description("Hours")
                    .required(false),
                integer("i").label("I")
                    .description("Minutes")
                    .required(false),
                integer("s").label("S")
                    .description("Seconds")
                    .required(false),
                integer("total_seconds").label("Total_seconds")
                    .description("The total time in seconds")
                    .required(false))
                .label("Age")
                .description("The lifetime of the deal")
                .required(false),
            object("additional_data").properties(string("dropbox_email").label("Dropbox_email")
                .description("The BCC email of the deal")
                .required(false))
                .label("Additional_data")
                .required(false),
            object("related_objects").properties(object("user").properties(integer("id").label("Id")
                .description("The ID of the user")
                .required(false),
                string("name").label("Name")
                    .description("The name of the user")
                    .required(false),
                string("email").label("Email")
                    .description("The email of the user")
                    .required(false),
                bool("has_pic").label("Has_pic")
                    .description("If the user has a picture or not")
                    .required(false),
                string("pic_hash").label("Pic_hash")
                    .description("The user picture hash")
                    .required(false),
                bool("active_flag").label("Active_flag")
                    .description("Whether the user is active or not")
                    .required(false))
                .label("User")
                .description("The user who is associated with the deal")
                .required(false),
                object("organization").properties(string("name").label("Name")
                    .description("The name of the organization associated with the deal")
                    .required(false),
                    integer("people_count").label("People_count")
                        .description(
                            "The number of people connected with the organization that is associated with the deal")
                        .required(false),
                    integer("owner_id").label("Owner_id")
                        .description("The ID of the owner of the organization that is associated with the deal")
                        .required(false),
                    string("address").label("Address")
                        .description("The address of the organization that is associated with the deal")
                        .required(false),
                    bool("active_flag").label("Active_flag")
                        .description("Whether the associated organization is active or not")
                        .required(false),
                    string("cc_email").label("Cc_email")
                        .description("The BCC email of the organization associated with the deal")
                        .required(false))
                    .label("Organization")
                    .description("The organization which is associated with the deal")
                    .required(false),
                object("person").properties(bool("active_flag").label("Active_flag")
                    .description("Whether the associated person is active or not")
                    .required(false),
                    string("name").label("Name")
                        .description("The name of the person associated with the deal")
                        .required(false),
                    array("email").items(object().properties(string("label").label("Label")
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
                    array("phone").items(object().properties(string("label").label("Label")
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
                    integer("owner_id").label("Owner_id")
                        .description("The ID of the owner of the person that is associated with the deal")
                        .required(false))
                    .label("Person")
                    .description("The person who is associated with the deal")
                    .required(false))
                .label("Related_objects")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"id\":1,\"creator_user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"user_id\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true,\"value\":8877},\"person_id\":{\"active_flag\":true,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"37244499911\",\"primary\":true}],\"value\":1101},\"org_id\":{\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\",\"value\":5},\"stage_id\":2,\"title\":\"Deal One\",\"value\":5000,\"currency\":\"EUR\",\"add_time\":\"2019-05-29 04:21:51\",\"update_time\":\"2019-11-28 16:19:50\",\"stage_change_time\":\"2019-11-28 15:41:22\",\"active\":true,\"deleted\":false,\"status\":\"open\",\"probability\":null,\"next_activity_date\":\"2019-11-29\",\"next_activity_time\":\"11:30:00\",\"next_activity_id\":128,\"last_activity_id\":null,\"last_activity_date\":null,\"lost_reason\":null,\"visible_to\":\"1\",\"close_time\":null,\"pipeline_id\":1,\"won_time\":\"2019-11-27 11:40:36\",\"first_won_time\":\"2019-11-27 11:40:36\",\"lost_time\":\"\",\"products_count\":0,\"files_count\":0,\"notes_count\":2,\"followers_count\":0,\"email_messages_count\":4,\"activities_count\":1,\"done_activities_count\":0,\"undone_activities_count\":1,\"participants_count\":1,\"expected_close_date\":\"2019-06-29\",\"last_incoming_mail_time\":\"2019-05-29 18:21:42\",\"last_outgoing_mail_time\":\"2019-05-30 03:45:35\",\"label\":11,\"stage_order_nr\":2,\"person_name\":\"Person\",\"org_name\":\"Organization\",\"next_activity_subject\":\"Call\",\"next_activity_type\":\"call\",\"next_activity_duration\":\"00:30:00\",\"next_activity_note\":\"Note content\",\"formatted_value\":\"€5,000\",\"weighted_value\":5000,\"formatted_weighted_value\":\"€5,000\",\"weighted_value_currency\":\"EUR\",\"rotten_time\":null,\"owner_name\":\"Creator\",\"cc_email\":\"company+deal1@pipedrivemail.com\",\"org_hidden\":false,\"person_hidden\":false,\"average_time_to_won\":{\"y\":0,\"m\":0,\"d\":0,\"h\":0,\"i\":20,\"s\":49,\"total_seconds\":1249},\"average_stage_progress\":4.99,\"age\":{\"y\":0,\"m\":6,\"d\":14,\"h\":8,\"i\":57,\"s\":26,\"total_seconds\":17139446},\"stay_in_pipeline_stages\":{\"times_in_stages\":{\"1\":15721267,\"2\":1288449,\"3\":4368,\"4\":3315,\"5\":26460},\"order_of_stages\":[1,2,3,4,5]},\"last_activity\":null,\"next_activity\":null},\"additional_data\":{\"dropbox_email\":\"company+deal1@pipedrivemail.com\"},\"related_objects\":{\"user\":{\"8877\":{\"id\":8877,\"name\":\"Creator\",\"email\":\"john.doe@pipedrive.com\",\"has_pic\":false,\"pic_hash\":null,\"active_flag\":true}},\"organization\":{\"2\":{\"id\":2,\"name\":\"Organization\",\"people_count\":2,\"owner_id\":8877,\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"active_flag\":true,\"cc_email\":\"org@pipedrivemail.com\"}},\"person\":{\"1101\":{\"active_flag\":true,\"id\":1101,\"name\":\"Person\",\"email\":[{\"label\":\"work\",\"value\":\"person@pipedrive.com\",\"primary\":true}],\"phone\":[{\"label\":\"work\",\"value\":\"3421787767\",\"primary\":true}],\"owner_id\":8877}},\"stage\":{\"2\":{\"id\":2,\"company_id\":123,\"order_nr\":1,\"name\":\"Stage Name\",\"active_flag\":true,\"deal_probability\":100,\"pipeline_id\":1,\"rotten_flag\":false,\"rotten_days\":null,\"add_time\":\"2015-12-08 13:54:06\",\"update_time\":\"2015-12-08 13:54:06\",\"pipeline_name\":\"Pipeline\",\"pipeline_deal_probability\":true}},\"pipeline\":{\"1\":{\"id\":1,\"name\":\"Pipeline\",\"url_title\":\"Pipeline\",\"order_nr\":0,\"active\":true,\"deal_probability\":true,\"add_time\":\"2015-12-08 10:00:24\",\"update_time\":\"2015-12-08 10:00:24\"}}}}");
}
