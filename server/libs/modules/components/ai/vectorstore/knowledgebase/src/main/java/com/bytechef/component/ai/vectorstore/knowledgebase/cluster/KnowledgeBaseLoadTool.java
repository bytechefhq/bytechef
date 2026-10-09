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

package com.bytechef.component.ai.vectorstore.knowledgebase.cluster;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.ADDITIONAL_METADATA_PROPERTY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.CONTENT;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.LOAD;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseOptionsUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/**
 * @author Marko Kriskovic
 */
public class KnowledgeBaseLoadTool {

    private KnowledgeBaseLoadTool() {
    }

    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService) {

        VectorStore kbVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement(LOAD)
            .title("Load Knowledge Base Documents")
            .description("Loads new text content as a document into the knowledge base.")
            .type(TOOLS)
            .properties(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .resourceReference(ResourceType.KNOWLEDGE_BASE)
                    .description("The knowledge base to load documents into.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                ADDITIONAL_METADATA_PROPERTY,
                string(CONTENT)
                    .label("Content")
                    .description("The text content to store in the knowledge base.")
                    .required(true))
            .object(() -> (MultipleConnectionsToolFunction) (
                inputParameters, connectionParameters, extensions, componentConnections, context) -> {

                String content = inputParameters.getRequiredString(CONTENT);

                kbVectorStore.load(
                    inputParameters,
                    ParametersFactory.create(Map.of()),
                    null,
                    () -> List.of(new Document(content)),
                    List.of());

                return null;
            });
    }
}
