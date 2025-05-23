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

package com.bytechef.component.resend.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Monika Domiter
 */
public final class ResendConstants {

    private ResendConstants() {
    }

    public static final String ATTACHMENTS = "attachments";
    public static final String BCC = "bcc";
    public static final String CC = "cc";
    public static final String CONTENT_TYPE = "contentType";
    public static final String EMAIL = "email";
    public static final String FROM = "from";
    public static final String HEADERS = "headers";
    public static final String HTML = "html";
    public static final String NAME = "name";
    public static final String REPLY_TO = "reply_to";
    public static final String SUBJECT = "subject";
    public static final String TAGS = "tags";
    public static final String TEXT = "text";
    public static final String TO = "to";
    public static final String VALUE = "value";

    public static final ModifiableStringProperty EMAIL_PROPERTY = string(EMAIL)
        .label("Email Address")
        .controlType(ControlType.EMAIL);
}
