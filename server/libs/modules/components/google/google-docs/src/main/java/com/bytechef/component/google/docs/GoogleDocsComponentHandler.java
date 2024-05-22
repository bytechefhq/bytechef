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

package com.bytechef.component.google.docs;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.google.docs.connection.GoogleDocsConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.GOOGLE_DOCS;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.docs.action.GoogleDocsCreateDocumentAction;
import com.bytechef.component.google.docs.action.GoogleDocsCreateDocumentBasedOnTemplateAction;
import com.bytechef.component.google.docs.action.GoogleDocsReadDocumentAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class GoogleDocsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(GOOGLE_DOCS)
        .title("Google Docs")
        .description(
            "Google Docs is a cloud-based collaborative word processing platform that allows multiple users to " +
                "create, edit, and share documents in real-time.")
        .icon("path:assets/google-docs.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleDocsCreateDocumentAction.ACTION_DEFINITION,
            GoogleDocsCreateDocumentBasedOnTemplateAction.ACTION_DEFINITION,
            GoogleDocsReadDocumentAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
