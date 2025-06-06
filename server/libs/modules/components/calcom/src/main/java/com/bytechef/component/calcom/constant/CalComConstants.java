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

package com.bytechef.component.calcom.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class CalComConstants {

    public static final String DATA = "data";
    public static final String WEBHOOK_ID = "webhookId";
    public static final String ID = "id";
    public static final String PAYLOAD = "payload";

    public static final ModifiableObjectProperty PERSON_RECORD = object("personRecord")
        .properties(
            integer("id")
                .description("ID of the organizer."),
            string("name")
                .description("Name of the organizer."),
            string("email")
                .description("Email of the organizer."),
            string("username")
                .description("Username of the organizer."),
            string("timezone")
                .description("Timezone of the organizer."),
            object("language")
                .description("Language of the organizer.")
                .properties(
                    string("locale")
                        .description("Locale language of the organizer.")),
            string("timeFormat")
                .description("Time format that the organizer uses."),
            integer("utcOffset")
                .description("UTC offset of the organizer."));

    public static final ModifiableObjectProperty RESPONSE_VALUE = object("responseValue")
        .properties(
            string("label")
                .description("Label of the response."),
            string("value")
                .description("Value of the response."),
            bool("isHidden")
                .description("Whether the response is hidden."));

    private CalComConstants() {
    }
}
