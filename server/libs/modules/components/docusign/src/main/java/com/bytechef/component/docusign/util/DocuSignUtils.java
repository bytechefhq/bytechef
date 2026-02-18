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

package com.bytechef.component.docusign.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.EMAIL_SUBJECT;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ENVELOPE_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.FROM_DATE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.constant.DocuSignConstants.DocumentRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class DocuSignUtils {

    public static String getAuthorizationUrl(String environment) {
        return "https://%s/oauth/"
            .formatted(environment.equals("demo") ? "account-d.docusign.com" : "account.docusign.com");
    }

    public static List<Option<String>> getDocumentIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> response = context.http(
            http -> http.get(
                "/restapi/v2.1/accounts/%s/envelopes/%s/documents".formatted(
                    connectionParameters.getRequiredString(ACCOUNT_ID),
                    inputParameters.getRequiredString(ENVELOPE_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get("envelopeDocuments") instanceof List<?> envelopeDocuments) {
            for (Object documentObject : envelopeDocuments) {
                if (documentObject instanceof Map<?, ?> document) {
                    options.add(option((String) document.get("name"), (String) document.get(DOCUMENT_ID)));
                }
            }
        }

        return options;
    }

    public static List<Map<String, Object>> getDocumentsList(List<DocumentRecord> documentRecords, Context context) {
        List<Map<String, Object>> documents = new ArrayList<>();

        for (DocumentRecord documentRecord : documentRecords) {
            Map<String, Object> document = new HashMap<>();

            document.put("documentBase64", encodeFileEntry(documentRecord.documentFile(), context));
            document.put("name", documentRecord.name());
            document.put("documentId", documentRecord.documentId());

            documents.add(document);
        }

        return documents;
    }

    private static String encodeFileEntry(FileEntry fileEntry, Context context) {
        byte[] fileContent = context.file(file -> file.readAllBytes(fileEntry));

        return context.encoder(encoder -> encoder.base64Encode(fileContent));
    }

    public static List<Option<String>> getEnvelopeIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> response = context
            .http(http -> http.get(
                "/restapi/v2.1/accounts/%s/envelopes".formatted(connectionParameters.getRequiredString(ACCOUNT_ID))))
            .queryParameter("from_date", inputParameters.getRequiredString(FROM_DATE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get("envelopes") instanceof List<?> envelopes) {
            for (Object envelopeObject : envelopes) {
                if (envelopeObject instanceof Map<?, ?> envelope) {
                    options.add(option((String) envelope.get(EMAIL_SUBJECT), (String) envelope.get(ENVELOPE_ID)));
                }
            }
        }

        return options;
    }
}
