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

package com.bytechef.component.agile.crm.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmConstants {

    public static final String ADDRESS = "address";
    public static final String CITY = "city";
    public static final String COMPANY = "company";
    public static final String COUNTRY = "country";
    public static final String CREATED_TIME = "created_time";
    public static final String DESCRIPTION = "description";
    public static final String DOMAIN = "domain";
    public static final String DUE = "due";
    public static final String EMAIL = "email";
    public static final String EXPECTED_VALUE = "expected_value";
    public static final String FIRST_NAME = "first_name";
    public static final String ID = "id";
    public static final String LAST_NAME = "last_name";
    public static final String LAST_TIME_CHECKED = "lastTimeChecked";
    public static final String MILESTONE = "milestone";
    public static final String NAME = "name";
    public static final String OWNER_ID = "owner_id";
    public static final String PHONE = "phone";
    public static final String PIPELINE_ID = "pipeline_id";
    public static final String PRIORITY_TYPE = "priority_type";
    public static final String PROBABILITY = "probability";
    public static final String PROPERTIES = "properties";
    public static final String STATE = "state";
    public static final String SUBJECT = "subject";
    public static final String TAGS = "tags";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String WEBSITE = "website";
    public static final String ZIP_CODE = "zip_code";

    public static final ModifiableObjectProperty TASK_OUTPUT_PROPERTY = object()
        .properties(
            number("id")
                .description("The ID of the task."),
            string("type")
                .description("The type of the task."),
            string("priority_type")
                .description("The priority of the task."),
            integer("due")
                .description("The due date of the task."),
            integer("task_completed_time")
                .description("The time when task was completed."),
            integer("task_start_time")
                .description("The time when task was started."),
            integer("created_time")
                .description("The time when task was created."),
            bool("is_complete")
                .description("Whether the task is completed."),
            array("contacts")
                .description("Contacts that are connected to the task.")
                .items(
                    object()
                        .properties(
                            number("id")
                                .description("The ID of the contact."),
                            string("type")
                                .description("The type of the contact."),
                            integer("created_time")
                                .description("The time when contact was created."),
                            integer("updated_time")
                                .description("The time when contact was updated."))),
            string("subject")
                .description("The subject of the task."),
            string("entity_type")
                .description("The entity type."),
            array("notes")
                .description("Notes of the task."),
            array("note_ids")
                .description("Notes ID."),
            integer("progress")
                .description("The progress of the task."),
            string("status")
                .description("The status of the task."),
            array("deal_ids")
                .description("IDs of the deals that are connected to the task."),
            object("taskOwner")
                .description("Owner of the task.")
                .properties(
                    number("id")
                        .description("The ID of the owner."),
                    string("domain")
                        .description("The domain of the owner."),
                    string("email")
                        .description("The email of the owner."),
                    string("phone")
                        .description("The phone number of the owner."),
                    string("name")
                        .description("The name of the owner."),
                    string("pic")
                        .description("The picture of the owner."),
                    string("schedule_id")
                        .description("The schedule ID of the owner."),
                    string("calendar_url")
                        .description("The calendar URL of the owner."),
                    string("calendarURL")
                        .description("The calendar URL of the owner.")));

    private AgileCrmConstants() {
    }
}
