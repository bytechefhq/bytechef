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
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DOCUMENT_ID;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.READ_DOCUMENT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import java.io.IOException;

/**
 * @author Monika Domiter
 */
public class GoogleDocsReadDocumentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ_DOCUMENT)
        .title("Read document")
        .description("Read a document from Google Docs")
        .properties(
            string(DOCUMENT_ID)
                .label("Document id")
                .description("The ID of the document to read")
                .required(true))
        .perform(GoogleDocsReadDocumentAction::perform);

    private GoogleDocsReadDocumentAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Docs docs = GoogleServices.getDocs(connectionParameters);

        return docs
            .documents()
            .get(inputParameters.getRequiredString(DOCUMENT_ID))
            .execute();
    }

}
