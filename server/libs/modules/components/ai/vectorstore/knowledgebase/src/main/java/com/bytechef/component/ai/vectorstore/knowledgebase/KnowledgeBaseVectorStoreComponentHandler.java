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

package com.bytechef.component.ai.vectorstore.knowledgebase;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseLoadAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.action.KnowledgeBaseSearchAction;
import com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStore;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.tag.service.TagService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Component handler for the internal Knowledge Base vector store.
 *
 * @author Ivica Cardic
 */
@Component(KNOWLEDGE_BASE + "_v1_ComponentHandler")
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
public class KnowledgeBaseVectorStoreComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public KnowledgeBaseVectorStoreComponentHandler(
        ClusterElementDefinitionService clusterElementDefinitionService,
        KnowledgeBaseService knowledgeBaseService, TagService tagService,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore) {

        this.componentDefinition =
            new KnowledgeBaseVectorStoreComponentDefinitionImpl(
                clusterElementDefinitionService, knowledgeBaseService, tagService, vectorStore);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class KnowledgeBaseVectorStoreComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public KnowledgeBaseVectorStoreComponentDefinitionImpl(
            ClusterElementDefinitionService clusterElementDefinitionService,
            KnowledgeBaseService knowledgeBaseService, TagService tagService, VectorStore vectorStore) {

            super(component(KNOWLEDGE_BASE)
                .title("Knowledge Base")
                .description(
                    "Search ByteChef's internal knowledge base to retrieve relevant document chunks using " +
                        "semantic similarity search powered by vector embeddings.")
                .icon("path:assets/knowledge-base.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    KnowledgeBaseLoadAction.of(clusterElementDefinitionService, knowledgeBaseService, vectorStore),
                    KnowledgeBaseSearchAction.of(knowledgeBaseService, tagService, vectorStore))
                .clusterElements(
                    KnowledgeBaseVectorStore.of(clusterElementDefinitionService, vectorStore)));
        }
    }
}
