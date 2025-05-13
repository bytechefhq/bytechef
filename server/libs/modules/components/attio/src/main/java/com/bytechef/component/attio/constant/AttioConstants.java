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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import java.util.List;

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
    public static final String WORKSPACES = "workspaces";

    private AttioConstants() {
    }

    public static final List<ModifiableValueProperty<?, ?>> PERSON_RECORD = List.of(
        array("name")
            .label("Name")
            .items(
                object()
                    .properties(
                        string("first_name")
                            .label("First Name")
                            .description("The first name."),
                        string("last_name")
                            .label("Last Name")
                            .description("The last name."),
                        string("full_name")
                            .label("Full Name")
                            .description("The full name."))),
        array("email_addresses")
            .label("Email Addresses")
            .items(
                object()
                    .properties(
                        string("email_address")
                            .label("Email Address"))),
        array("description")
            .label("Description")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("company")
            .label("Company")
            .items(
                object()
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Company", COMPANIES))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(COMPANIES))
                            .required(true))),
        array("job_title")
            .label("Job Title")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("phone_numbers")
            .label("Phone Numbers")
            .items(
                object()
                    .properties(
                        string("original_phone_number")
                            .label("Original Phone Number")
                            .required(true))),
        array("primary_location")
            .label("Primary Location")
            .items(
                object()
                    .properties(
                        string("city")
                            .label("City")
                            .required(true),
                        string("state")
                            .label("State")
                            .required(true),
                        string("country")
                            .label("Country")
                            .required(true))),
        array("angellist")
            .label("AngelList")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("facebook")
            .label("Facebook")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("instagram")
            .label("Instagram")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("linkedin")
            .label("LinkedIn")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("twitter")
            .label("Twitter")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("associated_deals")
            .label("Associated Deals")
            .items(
                object("")
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Deal", DEALS))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(DEALS))
                            .required(true))),
        array("associated_users")
            .label("Associated Users")
            .items(
                object("")
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("User", USERS))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options((ActionOptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                            .required(true))));

    public static final List<ModifiableValueProperty<?, ?>> COMPANY_RECORD = List.of(
        array("domains")
            .label("Domains")
            .items(
                object()
                    .properties(
                        string("domain")
                            .label("Domain"))),
        array("name")
            .label("Name")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value"))),
        array("description")
            .label("Description")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Value")
                            .required(true))),
        array("categories")
            .label("Categories")
            .items(
                object()
                    .properties(
                        string("option")
                            .label("Option")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getCompanyIdOptions("categories")),
                        array("primary_location")
                            .label("Primary Location")
                            .items(
                                object()
                                    .properties(
                                        string("city")
                                            .label("City")
                                            .required(true),
                                        string("state")
                                            .label("State")
                                            .required(true),
                                        string("country")
                                            .label("Country")
                                            .required(true))),
                        array("angellist")
                            .label("AngelList")
                            .items(
                                object()
                                    .properties(
                                        string("value")
                                            .required(true))),
                        array("facebook")
                            .label("Facebook")
                            .items(
                                object()
                                    .properties(
                                        string("value")
                                            .required(true))),
                        array("instagram")
                            .label("Instagram")
                            .items(
                                object()
                                    .properties(
                                        string("value")
                                            .required(true))),
                        array("linkedin")
                            .label("LinkedIn")
                            .items(
                                object()
                                    .properties(
                                        string("value")
                                            .required(true))),
                        array("twitter")
                            .label("Twitter")
                            .items(
                                object()
                                    .properties(
                                        string("value")
                                            .required(true))),
                        array("estimated_arr_usd")
                            .label("Estimated ARR")
                            .items(
                                object()
                                    .properties(
                                        string("option")
                                            .label("Option")
                                            .options(AttioUtils.getCompanyIdOptions("estimated_arr_usd"))
                                            .required(true))),
                        array("foundation_date")
                            .label("Foundation Date")
                            .items(
                                object()
                                    .properties(
                                        date("value")
                                            .required(true))),
                        array("employee_range")
                            .label("Employee Range")
                            .items(
                                object()
                                    .properties(
                                        string("option")
                                            .label("Option")
                                            .options(AttioUtils.getCompanyIdOptions("employee_range"))
                                            .required(true))),
                        array("associated_deals")
                            .label("Associated Deals")
                            .items(
                                object()
                                    .properties(
                                        string(TARGET_OBJECT)
                                            .label("Target object")
                                            .options(option("Deal", DEALS))
                                            .required(true),
                                        string(TARGET_RECORD_ID)
                                            .label("Target Record ID")
                                            .optionsLookupDependsOn(TARGET_OBJECT)
                                            .options(AttioUtils.getTargetRecordIdOptions(DEALS))
                                            .required(true))),
                        array("associated_workspaces")
                            .label("Associated Workspaces")
                            .items(
                                object()
                                    .properties(
                                        string(TARGET_OBJECT)
                                            .label("Target object")
                                            .options(option("Workspace", WORKSPACES))
                                            .required(true),
                                        string(TARGET_RECORD_ID)
                                            .label("Target Record ID")
                                            .optionsLookupDependsOn(TARGET_OBJECT)
                                            .options(AttioUtils.getTargetRecordIdOptions(WORKSPACES))
                                            .required(true))))));

    public static final List<ModifiableValueProperty<?, ?>> USER_RECORD = List.of(
        array("person")
            .label("Person")
            .description("Person that will become a user.")
            .items(
                object()
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Person", PEOPLE))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(PEOPLE))
                            .required(true))),
        array("primary_email_address")
            .label("Primary Email Address")
            .items(
                object()
                    .properties(
                        string("email_address")
                            .label("Email Address")))
            .required(true),
        array("user_id")
            .label("User ID")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("ID")))
            .required(true),
        array("workspace")
            .label("Workspace")
            .items(
                object()
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Workspace", WORKSPACES))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(WORKSPACES))
                            .required(true))));

    public static final List<ModifiableValueProperty<?, ?>> DEAL_RECORD = List.of(
        array("name")
            .label("Deal Name")
            .items(
                object()
                    .properties(
                        string("value")
                            .label("Deal Name")))
            .required(true),
        array("stage")
            .label("Deal Stage")
            .items(
                object()
                    .properties(
                        string("status")
                            .options((ActionOptionsFunction<String>) AttioUtils::getDealStageIdOptions)
                            .label("Status")))
            .required(true),
        array("owner")
            .label("Deal Owner")
            .items(
                object()
                    .properties(
                        string(REFERENCED_ACTOR_TYPE)
                            .label("Reference Actor Type")
                            .options(option("Workspace Member", "workspace-member"))
                            .required(true),
                        string(REFERENCED_ACTOR_ID)
                            .label("Reference Actor ID")
                            .options((ActionOptionsFunction<String>) AttioUtils::getWorkSpaceMemberIdOptions)
                            .required(true)))
            .required(true),
        array("value")
            .label("Deal Value")
            .items(
                object()
                    .properties(
                        number("currency_value")
                            .label("Currency Value"))),
        array("associated_people")
            .label("Associated People")
            .items(
                object()
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Person", PEOPLE))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(PEOPLE))
                            .required(true))),
        array("associated_company")
            .label("Associated Company")
            .items(
                object()
                    .properties(
                        string(TARGET_OBJECT)
                            .label("Target object")
                            .options(option("Company", COMPANIES))
                            .required(true),
                        string(TARGET_RECORD_ID)
                            .label("Target Record ID")
                            .optionsLookupDependsOn(TARGET_OBJECT)
                            .options(AttioUtils.getTargetRecordIdOptions(COMPANIES))
                            .required(true))));

    public static final List<ModifiableValueProperty<?, ?>> WORKSPACE_RECORD =
        List.of(
            array("workspace_id")
                .label("Workspace ID")
                .items(
                    object()
                        .properties(
                            string("value")
                                .label("ID")))
                .required(true),
            array("name")
                .label("Name")
                .items(
                    object()
                        .properties(
                            string("value")
                                .label("Name"))),
            array("users")
                .label("Users")
                .items(
                    object()
                        .properties(
                            string(REFERENCED_ACTOR_TYPE)
                                .label("Reference Actor Type")
                                .options(option("Workspace Member", "workspace-member"))
                                .required(true),
                            string(REFERENCED_ACTOR_ID)
                                .label("Reference Actor ID")
                                .options((ActionOptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                                .required(true))),
            array("company")
                .label("Company")
                .maxItems(1)
                .items(
                    object()
                        .properties(
                            string(TARGET_OBJECT)
                                .label("Target object")
                                .options(option("Company", COMPANIES))
                                .required(true),
                            string(TARGET_RECORD_ID)
                                .label("Target Record ID")
                                .optionsLookupDependsOn(TARGET_OBJECT)
                                .options(AttioUtils.getTargetRecordIdOptions(COMPANIES))
                                .required(true))),
            array("avatar_url")
                .label("Avatar URL")
                .items(
                    object()
                        .properties(
                            string("value")
                                .label("Avatar URL"))));
}
