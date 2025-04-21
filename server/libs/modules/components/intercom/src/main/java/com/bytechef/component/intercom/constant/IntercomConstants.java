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

package com.bytechef.component.intercom.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;

/**
 * @author Luka Ljubić
 */
public class IntercomConstants {

    public static final String AVATAR = "avatar";
    public static final String BODY = "body";
    public static final String EMAIL = "email";
    public static final String FROM = "from";
    public static final String ID = "id";
    public static final String LEAD = "lead";
    public static final String MESSAGE_TYPE = "message_type";
    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String ROLE = "role";
    public static final String SUBJECT = "subject";
    public static final String TEMPLATE = "template";
    public static final String TO = "to";
    public static final String TYPE = "type";
    public static final String USER = "user";

    public static final ComponentDsl.ModifiableObjectProperty CONTACT_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE)
                .description("The type of the contact."),
            string(ID)
                .description("ID of the contact."),
            string(ROLE)
                .description("Role of the contact."),
            string(EMAIL)
                .description("Email of the contact."),
            string(PHONE)
                .description("The contacts phone."),
            string(NAME)
                .description("The contacts name."));

    private IntercomConstants() {
    }
}
