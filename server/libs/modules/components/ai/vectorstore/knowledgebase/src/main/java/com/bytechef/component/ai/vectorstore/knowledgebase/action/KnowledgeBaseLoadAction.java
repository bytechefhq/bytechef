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

package com.bytechef.component.ai.vectorstore.knowledgebase.action;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.createVectorStore;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.component.ai.vectorstore.action.AbstractLoadAction;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Load action for adding documents to a knowledge base using document readers and transformers.
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseLoadAction {

    private KnowledgeBaseLoadAction() {
    }

    public static ActionDefinition of(
        ClusterElementDefinitionService clusterElementDefinitionService, KnowledgeBaseService knowledgeBaseService,
        VectorStore vectorStore) {

        return AbstractLoadAction.of(
            KNOWLEDGE_BASE, createVectorStore(vectorStore),
            List.of(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to load documents into.")
                    .options(getKnowledgeBaseOptions(knowledgeBaseService))
                    .required(true)),
            clusterElementDefinitionService);
    }

    private static ActionDefinition.OptionsFunction<Long> getKnowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                if (searchText == null ||
                    knowledgeBase.getName()
                        .toLowerCase()
                        .contains(searchText.toLowerCase())) {

                    options.add(option(knowledgeBase.getName(), knowledgeBase.getId()
                        .longValue()));
                }
            }

            return options;
        };
    }
}
