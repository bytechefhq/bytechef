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

package com.bytechef.component.attio.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;

/**
 * @author Nikolina Spehar
 */
public class AttioConstants {

    public static final String ASSIGNEES = "assignees";
    public static final String COMPANIES = "companies";
    public static final String CONTENT = "content";
    public static final String DATA = "data";
    public static final String DEADLINE_AT = "deadline_at";
    public static final String DEALS = "deals";
    public static final String FORMAT = "format";
    public static final String ID = "id";
    public static final String IS_COMPLETED = "is_completed";
    public static final String LINKED_RECORDS = "linked_records";
    public static final String PEOPLE = "people";
    public static final String RECORD_ID = "record_id";
    public static final String RECORD_TYPE = "record_type";
    public static final String REFERENCED_ACTOR_ID = "referenced_actor_id";
    public static final String REFERENCED_ACTOR_TYPE = "referenced_actor_type";
    public static final String TARGET_OBJECT = "target_object";
    public static final String TARGET_RECORD_ID = "target_record_id";
    public static final String USERS = "users";
    public static final String VALUE = "value";
    public static final String VALUES = "values";
    public static final String WORKSPACES = "workspaces";
    public static final String WORKSPACE_MEMBER = "workspace-member";

    public static final ModifiableObjectProperty ID_RECORD_OBJECT =
        object("id")
            .description("The id object of the user.")
            .properties(
                string("workspace_id")
                    .description("UUID of the workspace."),
                string("object_id")
                    .description("UUID of the object type."),
                string("record_id")
                    .description("UUID of the record."));

    public static final ModifiableValueProperty<?, ?> VALUE_BASIC_INFO = object()
        .properties(
            string("active_from")
                .description("When this association became active."),
            string("active_until")
                .description("When this association ends (null if still active)."),
            object("created_by_actor")
                .properties(
                    string("type")
                        .description("Type of actor who created this."),
                    string("id")
                        .description("ID of the creating actor.")));

    private AttioConstants() {
    }

    public static final ModifiableObjectProperty TRIGGER_OUTPUT = object()
        .properties(
            string("event_type")
                .description("Type of an event that triggers the trigger."),
            object(ID)
                .properties(
                    string("workspace_id"),
                    string("task_id")),
            object("actor")
                .properties(
                    string("type"),
                    string(ID)));

    public static final ModifiableObjectProperty COMPANY_OUTPUT =
        object()
            .properties(
                object("data")
                    .properties(
                        ID_RECORD_OBJECT, string("created_at")
                            .description("Timestamp when the record was created in ISO 8601 format."),
                        string("web_url")
                            .description("Web URL to access this record."),
                        object("values")
                            .properties(
                                array("domains")
                                    .description("Associated domains.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("domain")
                                                    .description("Domain part of the domain."),
                                                string("root_domain")
                                                    .description("Root domain of the domain."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("name")
                                    .description("Name values")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("The value of the name."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("description")
                                    .description("Description values")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("The value of the description."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("created_at")
                                    .description("Creation timestamps")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description(
                                                        "Date when the record was created in ISO 8601 format."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("created_by")
                                    .description("Creator references")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("referenced_actor_type")
                                                    .description("Type of referenced actor."),
                                                string("referenced_actor_id")
                                                    .description("ID of the referenced actor."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("twitter")
                                    .description("Twitter handles.")
                                    .items(object()),
                                array("team")
                                    .description("Team members.")
                                    .items(object()),
                                array("primary_location")
                                    .description("Primary locations.")
                                    .items(object()),
                                array("last_email_interaction")
                                    .description("Timestamps of last email interactions.")
                                    .items(object()),
                                array("categories")
                                    .description("Associated categories.")
                                    .items(object()),
                                array("logo_url")
                                    .description("URLs of logos.")
                                    .items(object()),
                                array("twitter_follower_count")
                                    .description("Twitter follower counts.")
                                    .items(object()),
                                array("last_calendar_interaction")
                                    .description("Last calendar interaction timestamps.")
                                    .items(object()),
                                array("linkedin")
                                    .description("LinkedIn information.")
                                    .items(object()),
                                array("foundation_date")
                                    .description("Foundation dates.")
                                    .items(object()),
                                array("strongest_connection_user")
                                    .description("Strongest connection user details.")
                                    .items(object()),
                                array("estimated_arr_usd")
                                    .description("Estimated annual recurring revenue in USD.")
                                    .items(object()),
                                array("strongest_connection_strength_legacy")
                                    .description("Legacy connection strength metrics.")
                                    .items(object()),
                                array("next_calendar_interaction")
                                    .description("Next calendar interaction timestamps.")
                                    .items(object()),
                                array("employee_range")
                                    .description("Employee count ranges.")
                                    .items(object()),
                                array("first_interaction")
                                    .description("First interaction timestamps.")
                                    .items(object()),
                                array("angellist")
                                    .description("AngelList information.")
                                    .items(object()),
                                array("facebook")
                                    .description("Facebook information.")
                                    .items(object()),
                                array("first_email_interaction")
                                    .description("First email interaction timestamps.")
                                    .items(object()),
                                array("strongest_connection_strength")
                                    .description("Connection strength metrics.")
                                    .items(object()),
                                array("first_calendar_interaction")
                                    .description("First calendar interaction timestamps.")
                                    .items(object()),
                                array("instagram")
                                    .description("Instagram information.")
                                    .items(object()),
                                array("last_interaction")
                                    .description("Last interaction timestamps.")
                                    .items(object()))));

    public static final ModifiableValueProperty<?, ?> COMPANY_RECORD = object(COMPANIES)
        .label("Company")
        .properties(
            string("domains")
                .label("Domain")
                .description("The full domain of the website.")
                .required(false),
            string("name")
                .label("Name")
                .description("The name of the company.")
                .required(false),
            string("description")
                .label("Description")
                .description("The description of the company.")
                .required(false),
            string("facebook")
                .label("Facebook")
                .description("The facebook profile of the company.")
                .required(false),
            string("instagram")
                .label("instagram")
                .description("The instagram profile of the company.")
                .required(false),
            string("linkedin")
                .label("LinkedIn")
                .description("The linkedin profile of the company.")
                .required(false),
            string("estimated_arr_usd")
                .label("Estimated ARR")
                .description("The annual recurring revenue (ARR) of the company.")
                .options(AttioUtils.getCompanyIdOptions("estimated_arr_usd"))
                .required(false),
            date("foundation_date")
                .label("Foundation Date")
                .description("The date when the company was founded.")
                .required(false),
            string("employee_range")
                .label("Employee Range")
                .description("The employee range of the company.")
                .options(AttioUtils.getCompanyIdOptions("employee_range"))
                .required(false),
            array("categories")
                .label("Categories")
                .description("Categories of the company.")
                .options(AttioUtils.getCompanyIdOptions("categories"))
                .required(false)
                .items(
                    string("category")
                        .label("Category")
                        .required(false)),
            array("associated_deals")
                .label("Associated Deals")
                .description("The associated deals of the company.")
                .options(AttioUtils.getTargetRecordIdOptions(DEALS))
                .required(false)
                .items(
                    string("deal")
                        .label("Deal")
                        .required(false)),
            array("associated_workspaces")
                .label("Associated Workspace")
                .description("The associated workspace of the company.")
                .options(AttioUtils.getTargetRecordIdOptions(WORKSPACES))
                .required(false)
                .items(
                    string("workspace")
                        .label("Workspace")
                        .required(false)));

    public static final ModifiableObjectProperty DEAL_OUTPUT =
        object()
            .properties(
                object("data")
                    .properties(
                        ID_RECORD_OBJECT,
                        string("created_at")
                            .description("When the record was created in ISO 8601 format."),
                        string("web_url")
                            .description("Web URL to access the record."),
                        object("values")
                            .properties(
                                array("name")
                                    .description("List of name values for the record.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("The value of the deal name."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("stage")
                                    .description("List of stage values for the record.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                object("status")
                                                    .description("Status of the deal.")
                                                    .properties(
                                                        string("title")
                                                            .description("Title of the deal."),
                                                        object("id")
                                                            .description("ID object")
                                                            .properties(
                                                                string("workspace_id")
                                                                    .description("ID of the workspace."),
                                                                string("object_id")
                                                                    .description("ID of the object."),
                                                                string("attribute_id")
                                                                    .description("ID of the attribute."),
                                                                string("status_id")
                                                                    .description("ID of the status.")),
                                                        bool("is_archived")
                                                            .description("Whether the status is archived."),
                                                        bool("celebration_enabled")
                                                            .description("Whether the celebration is enabled."),
                                                        string("target_time_in_status")
                                                            .description("Target time of the status.")),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("owner")
                                    .description("List of owners for the record.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("referenced_actor_type")
                                                    .description("Type of referenced actor."),
                                                string("referenced_actor_id")
                                                    .description("ID of the referenced actor."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("value")
                                    .description("List of currency values for the record.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                number("currency_value")
                                                    .description("Value of the deal in currency."),
                                                string("currency_code")
                                                    .description("Currency code of the currency used in the deal."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("associated_people")
                                    .description("List of associated people records.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("target_object")
                                                    .description("Target object of the record."),
                                                string("target_record_id")
                                                    .description("ID of the record that created the deal."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("associated_company")
                                    .description("List of associated company records.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("target_object")
                                                    .description("Target object of the record."),
                                                string("target_record_id")
                                                    .description("ID of the record that created the deal."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))))));

    public static final ModifiableObjectProperty PERSON_OUTPUT =
        object()
            .properties(
                object("data")
                    .properties(
                        ID_RECORD_OBJECT,
                        string("created_at")
                            .description("Record creation timestamp in ISO 8601 format."),
                        string("web_url")
                            .description("Direct URL to access this record."),
                        object("values")
                            .properties(
                                array("email_addresses")
                                    .description("Associated email addresses.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("original_email_address")
                                                    .description(
                                                        "The original, unprocessed email address as provided by the source."),
                                                string("email_address")
                                                    .description(
                                                        "The normalized email address (lowercase, whitespace-trimmed)."),
                                                string("email_domain")
                                                    .description(
                                                        "The domain part of the email address (e.g., 'example.com' from 'user@example.com')."),
                                                string("email_root_domain")
                                                    .description(
                                                        "The root domain, excluding subdomains (e.g., 'company.com' from 'user@sub.company.com')."),
                                                string("email_local_specifier")
                                                    .description(
                                                        "The local part of the email address (e.g., 'user' from 'user@example.com')."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("name")
                                    .description("Personal name information")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("first_name")
                                                    .description("Given name of the individual."),
                                                string("last_name")
                                                    .description("Family name/surname of the individual."),
                                                string("full_name")
                                                    .description(
                                                        "Complete name in display format (e.g., 'First Last')."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("description")
                                    .description("Descriptive text about the person.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("Description of the person."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute.")

                                            )),
                                array("created_at")
                                    .description("Creation timestamps")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("Timestamp of the creation date."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute.")

                                            )),
                                array("created_by")
                                    .description("Actor who created this record")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("referenced_actor_type")
                                                    .description("Type of referenced actor."),
                                                string("referenced_actor_id")
                                                    .description("ID of the referenced actor."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("strongest_connection_strength_legacy")
                                    .description("Legacy connection strength scores (deprecated).")
                                    .items(object()),
                                array("last_interaction")
                                    .description("Most recent interaction timestamps.")
                                    .items(object()),
                                array("twitter")
                                    .description("Twitter profile URLs or handles.")
                                    .items(object()),
                                array("avatar_url")
                                    .description("Profile image URLs.")
                                    .items(object()),
                                array("job_title")
                                    .description("Current/most recent job positions.")
                                    .items(object()),
                                array("next_calendar_interaction")
                                    .description("Upcoming scheduled meeting dates.")
                                    .items(object()),
                                array("company")
                                    .description("Current/most recent employers.")
                                    .items(object()),
                                array("primary_location")
                                    .description("Primary geographic locations.")
                                    .items(object()),
                                array("angellist")
                                    .description("AngelList profile URLs.")
                                    .items(object()),
                                array("strongest_connection_user")
                                    .description("Users with strongest relationships.")
                                    .items(object()),
                                array("strongest_connection_strength")
                                    .description("Current connection strength metrics.")
                                    .items(object()),
                                array("last_email_interaction")
                                    .description("Timestamps of last email exchanges.")
                                    .items(object()),
                                array("first_interaction")
                                    .description("Initial contact timestamps.")
                                    .items(object()),
                                array("last_calendar_interaction")
                                    .description("Most recent meeting dates.")
                                    .items(object()),
                                array("linkedin")
                                    .description("LinkedIn profile URLs.")
                                    .items(object()),
                                array("facebook")
                                    .description("Facebook profile URLs.")
                                    .items(object()),
                                array("first_calendar_interaction")
                                    .description("First meeting dates.")
                                    .items(object()),
                                array("twitter_follower_count")
                                    .description("Twitter follower counts.")
                                    .items(object()),
                                array("instagram")
                                    .description("Instagram profile URLs.")
                                    .items(object()),
                                array("first_email_interaction")
                                    .description("First email contact timestamps.")
                                    .items(object()),
                                array("phone_numbers")
                                    .description("Contact phone numbers.")
                                    .items(object()))));

    public static final ModifiableValueProperty<?, ?> PERSON_RECORD = object(PEOPLE)
        .label("Person")
        .properties(
            string("first_name")
                .label("First Name")
                .description("The first name of the person.")
                .required(false),
            string("last_name")
                .label("Last Name")
                .description("The last name of the person.")
                .required(false),
            string("email_address")
                .label("Email Address")
                .description("Email address of the person.")
                .required(false),
            string("description")
                .label("Description")
                .description("The description of the person.")
                .required(false),
            string("company")
                .label("Company")
                .description("The company where the person works.")
                .options(AttioUtils.getTargetRecordIdOptions(COMPANIES))
                .required(false),
            string("job_title")
                .label("Job Title")
                .description("The job title of the person.")
                .required(false),
            string("facebook")
                .label("Facebook")
                .description("Facebook profile of the person.")
                .required(false),
            string("instagram")
                .label("Instagram")
                .description("Instagram profile of the person.")
                .required(false),
            string("linkedin")
                .label("LinkedIn")
                .description("LinkedIn profile of the person.")
                .required(false),
            array("associated_deals")
                .label("Associated Deals")
                .description("The associated deals of the person.")
                .options(AttioUtils.getTargetRecordIdOptions(DEALS))
                .required(false)
                .items(
                    string("deal")
                        .label("Deal")
                        .required(false)),
            array("associated_users")
                .label("Associated Users")
                .description("The associated users of the person.")
                .options((ActionOptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                .required(false)
                .items(
                    string("user")
                        .label("Users")
                        .required(false)));

    public static final ModifiableObjectProperty USER_OUTPUT =
        object()
            .properties(
                object("data")
                    .properties(
                        ID_RECORD_OBJECT,
                        string("created_at")
                            .description("Timestamp when the record was created in ISO 8601 format."),
                        string("web_url")
                            .description("Web URL to access this record."),
                        object("values")
                            .properties(
                                array("person")
                                    .description("Associated person records.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("target_object")
                                                    .description("Type of object being referenced."),
                                                string("target_record_id")
                                                    .description("ID of the referenced record."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("primary_email_address")
                                    .description("Primary email addresses for this record.")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("original_email_address")
                                                    .description(
                                                        "The original, unprocessed email address as provided by the source."),
                                                string("email_address")
                                                    .description(
                                                        "The normalized email address (lowercase, whitespace-trimmed)."),
                                                string("email_domain")
                                                    .description(
                                                        "The domain part of the email address (e.g., 'example.com' from 'user@example.com')."),
                                                string("email_root_domain")
                                                    .description(
                                                        "The root domain, excluding subdomains (e.g., 'company.com' from 'user@sub.company.com')."),
                                                string("email_local_specifier")
                                                    .description(
                                                        "The local part of the email address (e.g., 'user' from 'user@example.com')."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("user_id")
                                    .description("User identifiers for this record")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("value")
                                                    .description("The user ID value"),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))),
                                array("workspace")
                                    .description("Associated workspaces")
                                    .items(
                                        object()
                                            .properties(
                                                VALUE_BASIC_INFO,
                                                string("target_object")
                                                    .description("Type of object being referenced."),
                                                string("target_record_id")
                                                    .description("ID of the referenced workspace."),
                                                string("attribute_type")
                                                    .description("The type of this reference attribute."))))));

    public static final ModifiableObjectProperty WORKSPACE_OUTPUT = object()
        .properties(
            object("data")
                .properties(
                    ID_RECORD_OBJECT,
                    string("created_at")
                        .description("Timestamp when the record was created in ISO 8601 format."),
                    string("web_url")
                        .description("Direct URL to access this record in the Attio application."),
                    object("values")
                        .properties(
                            array("workspace_id")
                                .description("Workspace identifier information.")
                                .items(
                                    object()
                                        .properties(
                                            VALUE_BASIC_INFO,
                                            string("value")
                                                .description("The actual workspace ID value."),
                                            string("attribute_type")
                                                .description("The type of this reference attribute.")

                                        )),
                            array("name")
                                .description("Name information for the record.")
                                .items(
                                    object()
                                        .properties(
                                            VALUE_BASIC_INFO,
                                            string("value")
                                                .description("The actual name value."),
                                            string("attribute_type")
                                                .description("The type of this reference attribute."))),
                            array("users")
                                .description("User references associated with this record.")
                                .items(
                                    object()
                                        .properties(
                                            VALUE_BASIC_INFO,
                                            string("target_object")
                                                .description(
                                                    "The type of object being referenced (always 'users' in this case)."),
                                            string("target_record_id")
                                                .description("The ID of the user record being referenced."),
                                            string("attribute_type")
                                                .description("The type of this reference attribute."))),
                            array("company")
                                .description("Company references associated with this record.")
                                .items(
                                    object()
                                        .properties(
                                            VALUE_BASIC_INFO,
                                            string("target_object")
                                                .description(
                                                    "The type of object being referenced (always 'companies' in this case)."),
                                            string("target_record_id")
                                                .description("The ID of the company record being referenced."),
                                            string("attribute_type")
                                                .description("The type of this reference attribute."))),
                            array("avatar_url")
                                .description("Avatar URL information for the record.")
                                .description("Avatar URL information for the record.")
                                .items(
                                    object()
                                        .properties(
                                            VALUE_BASIC_INFO,
                                            string("value")
                                                .description("The actual URL of the avatar image."),
                                            string("attribute_type")
                                                .description("The type of this reference attribute."))))));
}
