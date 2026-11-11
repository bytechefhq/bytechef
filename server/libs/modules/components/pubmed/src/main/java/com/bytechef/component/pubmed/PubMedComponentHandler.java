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

package com.bytechef.component.pubmed;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pubmed.action.PubMedGetArticleAbstractAction;
import com.bytechef.component.pubmed.action.PubMedSearchArticlesAction;
import com.bytechef.component.pubmed.connection.PubMedConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class PubMedComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("pubmed")
        .title("PubMed")
        .description(
            "PubMed is a free search engine maintained by the U.S. National Library of Medicine that indexes more " +
                "than 35 million citations and abstracts of biomedical and life sciences literature.")
        .icon("path:assets/pubmed.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(PubMedConnection.CONNECTION_DEFINITION)
        .actions(
            PubMedGetArticleAbstractAction.ACTION_DEFINITION,
            PubMedSearchArticlesAction.ACTION_DEFINITION)
        .clusterElements(
            tool(PubMedGetArticleAbstractAction.ACTION_DEFINITION),
            tool(PubMedSearchArticlesAction.ACTION_DEFINITION))
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://www.ncbi.nlm.nih.gov/books/NBK25501/");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
