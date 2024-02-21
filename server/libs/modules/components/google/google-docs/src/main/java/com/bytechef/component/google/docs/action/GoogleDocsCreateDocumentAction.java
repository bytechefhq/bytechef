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
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.BODY;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.CREATE_DOCUMENT;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.TITLE;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.createDocument;
import static com.bytechef.component.google.docs.util.GoogleDocsUtils.writeToDocument;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.EndOfSegmentLocation;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Request;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleDocsCreateDocumentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_DOCUMENT)
        .title("Create document")
        .description("Create a document on Google Docs")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Document title")
                .required(true),
            string(BODY)
                .label("Content")
                .description("Document content")
                .required(true))
        .perform(GoogleDocsCreateDocumentAction::perform);

    private GoogleDocsCreateDocumentAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Docs docs = GoogleServices.getDocs(connectionParameters);

        Document newDocument = createDocument(inputParameters.getRequiredString(TITLE), docs);

        InsertTextRequest insertTextRequest = new InsertTextRequest()
            .setText(inputParameters.getRequiredString(BODY))
            .setEndOfSegmentLocation(new EndOfSegmentLocation());

        Request request = new Request()
            .setInsertText(insertTextRequest);

        writeToDocument(docs, newDocument.getDocumentId(), List.of(request));

        return null;
    }
}
