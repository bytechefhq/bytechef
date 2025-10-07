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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.APPLICATION_VND_GOOGLE_APPS_DOCUMENT;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.IMAGES;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.writeToDocument;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.ReplaceImageRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import com.google.api.services.drive.model.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleDocsCreateDocumentFromTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDocumentFromTemplate")
        .title("Create Document From Template")
        .description(
            "Creates a new document based on an existing one and can replace any placeholder variables found in your " +
                "template document, like [[name]], [[email]], etc.")
        .properties(
            string(FILE_ID)
                .label("Template Document ID")
                .description("The ID of the template document from which the new document will be created.")
                .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_DOCUMENT, true))
                .required(true),
            string(FILE_NAME)
                .label("New Document Name")
                .description("Name of the new document.")
                .required(true),
            string(FOLDER_ID)
                .label("Folder for New Document")
                .description(
                    "Folder ID where the new document will be saved. If not provided, the new document " +
                        "will be saved in the same folder as the template document.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.folder", true))
                .required(false),
            object(VALUES)
                .label("Variables")
                .description("Don't include the \"[[]]\", only the key name and its value.")
                .additionalProperties(string())
                .required(false),
            object(IMAGES)
                .label("Images")
                .description("Key: Image ID (get it manually from the Read File Action), Value: Image URL.")
                .additionalProperties(string())
                .required(false))
        .output()
        .perform(GoogleDocsCreateDocumentFromTemplateAction::perform);

    private GoogleDocsCreateDocumentFromTemplateAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Docs docs = GoogleServices.getDocs(connectionParameters);
        File copiedPresentation = GoogleUtils.copyFileOnGoogleDrive(connectionParameters, inputParameters);
        List<Request> requests = createRequests(inputParameters);

        return writeToDocument(docs, copiedPresentation.getId(), requests);
    }

    private static List<Request> createRequests(Parameters inputParameters) {
        List<Request> requests = new ArrayList<>();

        Map<String, String> values = inputParameters.getMap(VALUES, String.class, Map.of());

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Request request = new Request()
                .setReplaceAllText(
                    new ReplaceAllTextRequest()
                        .setContainsText(
                            new SubstringMatchCriteria()
                                .setText("[[" + key + "]]")
                                .setMatchCase(true))
                        .setReplaceText(value));

            requests.add(request);
        }

        Map<String, String> images = inputParameters.getMap(IMAGES, String.class, Map.of());

        for (Map.Entry<String, String> entry : images.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Request request = new Request()
                .setReplaceImage(
                    new ReplaceImageRequest()
                        .setImageObjectId(key)
                        .setUri(value));

            requests.add(request);
        }
        return requests;
    }

}
