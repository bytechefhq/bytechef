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

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.ADDITIONAL_METADATA;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_FILTER;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.IS_MULTIPLE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.CONTENT;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.UPDATE;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseOptionsUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
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
public class KnowledgeBaseUpdateTool {

    private KnowledgeBaseUpdateTool() {
    }

    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, KnowledgeBaseService knowledgeBaseService) {

        VectorStore kbVectorStore = createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, vectorStore);

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement(UPDATE)
            .title("Update Knowledge Base Documents")
            .description(
                "Updates documents in the knowledge base by deleting existing ones matching the selected " +
                    "document or chunk and loading new text content.")
            .type(TOOLS)
            .properties(
                bool(IS_MULTIPLE)
                    .label("Update Multiple")
                    .description("Whether to update multiple documents or chunks.")
                    .required(true),
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to update documents in.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                integer(KNOWLEDGE_BASE_DOCUMENT_ID)
                    .label("Document")
                    .description("The document to update in the knowledge base.")
                    .options(KnowledgeBaseOptionsUtils.documentOptions(knowledgeBaseDocumentService))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_ID)
                    .required(false),
                integer(KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID)
                    .label("Document Chunk")
                    .description(
                        "The specific chunk to update. If not selected, all chunks of the selected document " +
                            "will be replaced.")
                    .options(KnowledgeBaseOptionsUtils.documentChunkOptions(knowledgeBaseDocumentChunkFacade))
                    .optionsLookupDependsOn(KNOWLEDGE_BASE_DOCUMENT_ID)
                    .required(false),
                object(ADDITIONAL_METADATA)
                    .label("Additional Metadata")
                    .description("Additional metadata key-value pairs to add to the stored documents.")
                    .additionalProperties(
                        string(), integer(), number(), bool(), dateTime(), date(), time())
                    .displayCondition(IS_MULTIPLE + "== false")
                    .required(false),
                array(METADATA_FILTER)
                    .label("Metadata Filter")
                    .description("List of metadata key-value pairs to filter by. Entries within a group are AND-ed; groups are OR-ed.")
                    .items(
                        object()
                            .additionalProperties(
                                string(), integer(), number(), bool(), dateTime(), date(), time()))
                    .displayCondition(IS_MULTIPLE + "== true")
                    .required(false),
                string(CONTENT)
                    .label("Content")
                    .description("The text content to store in the knowledge base.")
                    .required(true))
            .object(() -> (MultipleConnectionsToolFunction) (
                inputParameters, connectionParameters, extensions, componentConnections, context) -> {

                String content = inputParameters.getRequiredString(CONTENT);

                kbVectorStore.update(
                    inputParameters,
                    ParametersFactory.create(Map.of()),
                    null,
                    () -> List.of(new Document(content)),
                    List.of());

                return null;
            });
    }
}
