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

package com.bytechef.component.docusign;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.docusign.action.DocuSignCreateEnvelopeAction;
import com.bytechef.component.docusign.action.DocuSignDownloadEnvelopeDocumentAction;
import com.bytechef.component.docusign.connection.DocuSignConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class DocuSignComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("docusign")
        .title("DocuSign")
        .description(
            "DocuSign is a cloud-based e-signature platform that enables secure digital document signing and " +
                "workflow automation.")
        .icon("path:assets/docusign.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(DocuSignConnection.CONNECTION_DEFINITION)
        .actions(
            DocuSignCreateEnvelopeAction.ACTION_DEFINITION,
            DocuSignDownloadEnvelopeDocumentAction.ACTION_DEFINITION)
        .clusterElements(
            tool(DocuSignCreateEnvelopeAction.ACTION_DEFINITION),
            tool(DocuSignDownloadEnvelopeDocumentAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
