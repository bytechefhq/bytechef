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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;

/**
 * @author Nikolina Spehar
 */
public class AttioConstants {

    public static final String ASSIGNEES = "assignees";
    public static final String COMPANIES = "companies";
    public static final String COMPANY = "company";
    public static final String CONTENT = "content";
    public static final String DATA = "data";
    public static final String DEADLINE_AT = "deadline_at";
    public static final String DEALS = "deals";
    public static final String FIRST_NAME = "first_name";
    public static final String FORMAT = "format";
    public static final String FULL_NAME = "full_name";
    public static final String ID = "id";
    public static final String IS_COMPLETED = "is_completed";
    public static final String LAST_NAME = "last_name";
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
                .options((OptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                .required(false)
                .items(
                    string("user")
                        .label("Users")
                        .required(false)));
}
