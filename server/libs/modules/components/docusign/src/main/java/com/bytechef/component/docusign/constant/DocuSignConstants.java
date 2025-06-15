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

package com.bytechef.component.docusign.constant;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.FileEntry;

/**
 * @author Nikolina Spehar
 */
public class DocuSignConstants {

    public static final String ACCOUNT_ID = "accountId";
    public static final String BASE_URI = "baseUri";
    public static final String CARBON_COPIES = "carbonCopies";
    public static final String DOCUMENT_ID = "documentId";
    public static final String DOCUMENTS = "documents";
    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String ENVELOPE_ID = "envelopeId";
    public static final String ENVIRONMENT = "environment";
    public static final String FROM_DATE = "fromDate";
    public static final String RECIPIENTS = "recipients";
    public static final String SIGNERS = "signers";
    public static final String STATUS = "status";

    public static final ModifiableObjectProperty RECIPIENT_PROPERTY = object("recipient")
        .label("Recipient")
        .required(true)
        .properties(
            string("name")
                .label("Name")
                .description("The name of the recipient.")
                .required(true),
            string("email")
                .label("Email")
                .description("The email of the recipient.")
                .required(true),
            integer("recipientId")
                .label("Recipient ID")
                .description("Unique integer identifier of the recipient.")
                .required(true));

    private DocuSignConstants() {
    }

    public record DocumentRecord(FileEntry documentFile, String name, int documentId) {
    }
}
