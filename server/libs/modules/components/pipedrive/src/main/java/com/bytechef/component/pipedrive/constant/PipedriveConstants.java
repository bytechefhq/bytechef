/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.pipedrive.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public final class PipedriveConstants {

    public static final String ADDED = "added";
    public static final String CC_EMAIL = "cc_email";
    public static final String CURRENT = "current";
    public static final String EMAIL_MESSAGES_COUNT = "email_messages_count";
    public static final String ID = "id";
    public static final String OWNER_NAME = "owner_name";
    public static final String UPDATED = "updated";
    public static final String VALUE = "value";

    public static final ModifiableObjectProperty DEAL_OUTPUT_PROPERTY = object()
        .properties(
            integer(EMAIL_MESSAGES_COUNT),
            string(CC_EMAIL),
            integer("id"),
            integer("person_id"),
            string(OWNER_NAME),
            string("status"),
            string("title"),
            string("currency"),
            integer(VALUE));

    public static final ModifiableObjectProperty ORGANIZATION_OUTPUT_PROPERTY = object()
        .properties(
            integer(EMAIL_MESSAGES_COUNT),
            string(CC_EMAIL),
            integer("owner_id"),
            integer("id"),
            string(OWNER_NAME),
            string("name"),
            integer("company_id"));

    public static final ModifiableObjectProperty PERSON_OUTPUT_PROPERTY = object()
        .properties(
            integer(EMAIL_MESSAGES_COUNT),
            string(CC_EMAIL),
            integer("owner_id"),
            integer("id"),
            string(OWNER_NAME),
            array("phone")
                .items(
                    object()
                        .properties(
                            string(VALUE),
                            bool("primary"))),
            string("name"),
            array("email")
                .items(
                    object()
                        .properties(
                            string(VALUE),
                            bool("primary"))),
            integer("company_id"));

    private PipedriveConstants() {
    }

}
