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

package com.bytechef.component.sendgrid.constant;

import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.Property;

/**
 * @author Marko Krišković
 */
public final class SendgridConstants {

    public static final String SENDGRID = "sendgrid";
    public static final String EMAIL = "email";
    public static final String EMAIL_ADDRESS = "Email address";
    public static final String FROM = "from";
    public static final String SUBJECT = "subject";
    public static final String TO = "to";
    public static final String REPLY_TO = "reply_to";
    public static final String CC = "cc";
    public static final String BCC = "bcc";
    public static final String ATTACHMENTS = "attachments";
    public static final String CONTENT_TYPE = "text/plain";
    public static final String CONTENT_VALUE = "contentValue";
    public static final String TEMPLATE_ID = "template_id";
    public static final String DYNAMIC_TEMPLATE = "dynamicTemplate";
    public static final String SENDEMAIL = "sendEmail";
    public static final ModifiableStringProperty EMAIL_PROPERTY = string(EMAIL)
        .label(EMAIL_ADDRESS)
        .controlType(Property.ControlType.EMAIL);

    private SendgridConstants() {
    }
}
