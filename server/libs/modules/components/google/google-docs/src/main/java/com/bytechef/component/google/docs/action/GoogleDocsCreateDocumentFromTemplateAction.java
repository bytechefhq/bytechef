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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.APPLICATION_VND_GOOGLE_APPS_DOCUMENT;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DOCUMENT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.getDocument;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.writeToDocument;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.PLACEHOLDER_FORMAT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class GoogleDocsCreateDocumentFromTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDocumentFromTemplate")
        .title("Create Document From Template")
        .description(
            "Creates a new document based on an existing one and can replace any placeholder variables found in your " +
                "template document, like [[name]], [[email]], etc.")
        .help("", "https://docs.bytechef.io/reference/components/google-docs_v1#create-from-template")
        .properties(
            string(FILE_ID)
                .label("Template Document ID")
                .description("The ID of the template document from which the new document will be created.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_DOCUMENT, true))
                .required(true),
            string(PLACEHOLDER_FORMAT)
                .label("Placeholder Format")
                .description("Choose the format of placeholders in your template.")
                .options(
                    option("Curly Braces {{}}", "{{}}"),
                    option("Square Brackets [[]]", "[[]]"))
                .defaultValue("{{}}")
                .required(true),
            string(FILE_NAME)
                .label("Title of New Document")
                .description("Name of the new document.")
                .required(true),
            string(FOLDER_ID)
                .label("Folder ID")
                .description(
                    "ID of the folder where the new document will be saved. If not provided, the new document " +
                        "will be saved in the same folder as the template document.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.folder", true))
                .required(false),
            object(VALUES)
                .label("Variables")
                .description("Don't include the \"[[]]\", only the key name and its value.")
                .additionalProperties(string())
                .required(false))
        .output(outputSchema(DOCUMENT_OUTPUT_PROPERTY))
        .perform(GoogleDocsCreateDocumentFromTemplateAction::perform);

    private GoogleDocsCreateDocumentFromTemplateAction() {
    }

    public static Document perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Docs docs = GoogleServices.getDocs(connectionParameters);
        File copiedPresentation = GoogleUtils.copyFileOnGoogleDrive(connectionParameters, inputParameters);
        List<Request> requests = createRequests(inputParameters);

        BatchUpdateDocumentResponse batchUpdateDocumentResponse =
            writeToDocument(docs, copiedPresentation.getId(), requests);

        return getDocument(docs, batchUpdateDocumentResponse.getDocumentId());
    }

    private static List<Request> createRequests(Parameters inputParameters) {
        String placeholderFormat = inputParameters.getRequiredString(PLACEHOLDER_FORMAT);
        Map<String, String> values = inputParameters.getMap(VALUES, String.class, Map.of());

        boolean isBracketFormat = "[[]]".equals(placeholderFormat);

        return values.entrySet()
            .stream()
            .map(entry -> {
                String key = entry.getKey();
                String text = isBracketFormat ? "[[" + key + "]]" : "{{" + key + "}}";

                return new Request()
                    .setReplaceAllText(
                        new ReplaceAllTextRequest()
                            .setContainsText(
                                new SubstringMatchCriteria()
                                    .setText(text)
                                    .setMatchCase(true))
                            .setReplaceText(entry.getValue()));
            })
            .collect(Collectors.toList());
    }
}
