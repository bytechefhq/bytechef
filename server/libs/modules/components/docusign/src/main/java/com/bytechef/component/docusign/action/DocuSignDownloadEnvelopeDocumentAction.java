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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ENVELOPE_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.FROM_DATE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.util.DocuSignUtils;

/**
 * @author Nikolina Spehar
 */
public class DocuSignDownloadEnvelopeDocumentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadEnvelopeDocument")
        .title("Download Envelope Document")
        .description("Downloads a single document or all documents from an envelope.")
        .properties(
            date(FROM_DATE)
                .label("From Date")
                .description("Envelops that were created from this date will be fetched.")
                .required(true),
            string(ENVELOPE_ID)
                .label("Envelope ID")
                .description("The ID of the envelope.")
                .required(true)
                .optionsLookupDependsOn(FROM_DATE)
                .options((OptionsFunction<String>) DocuSignUtils::getEnvelopeIdOptions),
            string(DOCUMENT_ID)
                .label("Document ID")
                .description("ID of the document that will be downloaded from the envelope.")
                .required(true)
                .optionsLookupDependsOn(ENVELOPE_ID)
                .options((OptionsFunction<String>) DocuSignUtils::getDocumentIdOptions))
        .output(outputSchema(fileEntry().description("Downloaded document.")))
        .perform(DocuSignDownloadEnvelopeDocumentAction::perform);

    private DocuSignDownloadEnvelopeDocumentAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.get("/restapi/v2.1/accounts/%s/envelopes/%s/documents/%s".formatted(
                connectionParameters.getRequiredString(ACCOUNT_ID),
                inputParameters.getRequiredString(ENVELOPE_ID),
                inputParameters.getRequiredString(DOCUMENT_ID))))
            .configuration(responseType(ResponseType.binary("application/pdf")))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
