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

package com.bytechef.component.docusign.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.CARBON_COPIES;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENTS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.EMAIL_SUBJECT;
import static com.bytechef.component.docusign.constant.DocuSignConstants.RECIPIENTS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.RECIPIENT_PROPERTY;
import static com.bytechef.component.docusign.constant.DocuSignConstants.SIGNERS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.STATUS;
import static com.bytechef.component.docusign.util.DocuSignUtils.getDocumentsList;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.constant.DocuSignConstants.DocumentRecord;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class DocuSignCreateEnvelopeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEnvelope")
        .title("Create Envelope")
        .description("Creates a new envelope.")
        .properties(
            array(DOCUMENTS)
                .label("Documents")
                .description("The documents to be signed.")
                .required(true)
                .items(
                    object("document")
                        .label("Document")
                        .properties(
                            fileEntry("documentFile")
                                .label("Document")
                                .description("The document to be signed.")
                                .required(true),
                            string("name")
                                .label("Name")
                                .description("The name of the document to be signed.")
                                .required(true),
                            integer("documentId")
                                .label("Document ID")
                                .description("Unique integer identifier of the document to be signed.")
                                .required(true))),
            string(STATUS)
                .label("Status")
                .description("The status of the envelope.")
                .options(
                    option("Sent", "sent"),
                    option("Created", "created"))
                .required(true),
            string(EMAIL_SUBJECT)
                .label("Email Subject")
                .description("The subject of the email used to send the envelope.")
                .required(true),
            array(SIGNERS)
                .label("Signer Recipients")
                .description("The recipients of the envelope that have to sign the documents inside it.")
                .required(true)
                .items(RECIPIENT_PROPERTY),
            array(CARBON_COPIES)
                .label("Cc Recipients")
                .description("The recipients of the envelope that can only view the documents inside it.")
                .required(false)
                .items(RECIPIENT_PROPERTY))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("envelopeid")
                            .description("The id of the envelope."),
                        string("uri")
                            .description("A URI containing the user ID."),
                        string("statusDateTime")
                            .description("The DateTime that the envelope changed status (i.e. was created or sent.)"),
                        string("status")
                            .description("Indicates the envelope status."))))
        .perform(DocuSignCreateEnvelopeAction::perform);

    private DocuSignCreateEnvelopeAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<DocumentRecord> documentRecords = inputParameters.getRequiredList(DOCUMENTS, DocumentRecord.class);

        return context
            .http(http -> http.post(
                "/restapi/v2.1/accounts/%s/envelopes".formatted(connectionParameters.getRequiredString(ACCOUNT_ID))))
            .body(
                Body.of(
                    STATUS, inputParameters.getRequiredString(STATUS),
                    EMAIL_SUBJECT, inputParameters.getRequiredString(EMAIL_SUBJECT),
                    DOCUMENTS, getDocumentsList(documentRecords, context),
                    RECIPIENTS, Map.of(SIGNERS, inputParameters.getRequiredList(SIGNERS),
                        CARBON_COPIES, inputParameters.getRequiredList(CARBON_COPIES))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
