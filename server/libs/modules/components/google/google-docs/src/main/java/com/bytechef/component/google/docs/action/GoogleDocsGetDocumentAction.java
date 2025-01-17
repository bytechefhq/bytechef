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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DOCUMENT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleDocsGetDocumentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDocument")
        .title("Get Document")
        .description("Retrieve a specified document from your Google Drive.")
        .properties(
            string(DOCUMENT_ID)
                .label("Document Id")
                .description("The ID of the document to read.")
                .options((ActionOptionsFunction<String>) GoogleDocsUtils::getDocsIdOptions)
                .required(true))
        .output()
        .perform(GoogleDocsGetDocumentAction::perform);

    private GoogleDocsGetDocumentAction() {
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
