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

package com.bytechef.component.microsoft.outlook.constant;

import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365Constants {

    public static final String ADDRESS = "address";
    public static final String BASE_URL = "https://graph.microsoft.com/v1.0/me";
    public static final String BCC_RECIPIENTS = "bccRecipients";
    public static final String BODY = "body";
    public static final String CATEGORY = "category";
    public static final String CC_RECIPIENTS = "ccRecipients";
    public static final String CONTENT = "content";
    public static final String CONTENT_TYPE = "contentType";
    public static final String EMAIL_ADDRESS = "emailAddress";
    public static final String FROM = "from";
    public static final String GET_MAIL = "getMail";
    public static final String ID = "id";
    public static final String MICROSOFT_OUTLOOK_365 = "microsoftOutlook365";
    public static final String NAME = "name";
    public static final String RECIPIENT = "recipient";
    public static final String REPLY_TO = "replyTo";
    public static final String SEARCH_EMAIL = "searchEmail";
    public static final String SEND_EMAIL = "sendEmail";
    public static final String SUBJECT = "subject";
    public static final String TENANT_ID = "tenantId";
    public static final String TO = "to";
    public static final String TO_RECIPIENTS = "toRecipients";
    public static final ModifiableStringProperty CONTENT_PROPERTY = string(CONTENT)
        .label("Content")
        .description("The content of the item.")
        .required(false);

    public static final ModifiableStringProperty CONTENT_TYPE_PROPERTY = string(CONTENT_TYPE)
        .label("Content type")
        .description("The type of the content.")
        .options(
            option("Text", "text"),
            option("Html", "html"))
        .required(false);

    public static final ModifiableObjectProperty RECIPIENT_PROPERTY = object(RECIPIENT)
        .label("Recipient")
        .properties(
            object(EMAIL_ADDRESS)
                .properties(
                    string(ADDRESS)
                        .label("Address")
                        .description("The email address of the person or entity.")
                        .required(false),
                    string(NAME)
                        .label("Name")
                        .description("The display name of the person or entity.")
                        .required(false)));

    private MicrosoftOutlook365Constants() {
    }
}
