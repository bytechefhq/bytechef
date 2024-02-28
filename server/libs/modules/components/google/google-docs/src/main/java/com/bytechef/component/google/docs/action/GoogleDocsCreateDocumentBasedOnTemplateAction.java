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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.CREATE_DOCUMENT_BASED_ON_TEMPLATE;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DESTINATION_FILE;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.IMAGES;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.writeToDocument;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.ReplaceImageRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleDocsCreateDocumentBasedOnTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_DOCUMENT_BASED_ON_TEMPLATE)
        .title("Edit template file")
        .description("Edit a template file and replace the values with the ones provided")
        .properties(
            string(DESTINATION_FILE)
                .label("Destination file")
                .description("The ID of the file to replace the values")
                .required(true),
            object(VALUES)
                .label("Variables")
                .description("Don't include the \"[[]]\", only the key name and its value")
                .additionalProperties(string())
                .required(false),
            object(IMAGES)
                .label("Images")
                .description("Key: Image ID (get it manually from the Read File Action), Value: Image URL")
                .additionalProperties(string())
                .required(false))
        .perform(GoogleDocsCreateDocumentBasedOnTemplateAction::perform);

    private GoogleDocsCreateDocumentBasedOnTemplateAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Docs docs = GoogleServices.getDocs(connectionParameters);

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

        writeToDocument(docs, inputParameters.getRequiredString(DESTINATION_FILE), requests);

        return null;
    }

}
