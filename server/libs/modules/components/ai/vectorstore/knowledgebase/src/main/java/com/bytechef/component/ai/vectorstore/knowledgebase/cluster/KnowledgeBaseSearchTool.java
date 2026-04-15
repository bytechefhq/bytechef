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

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.SEARCH_PROPERTIES;
import static com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.TAG_IDS;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseTagService;
import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.tag.domain.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class KnowledgeBaseSearchTool {

    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(
        org.springframework.ai.vectorstore.VectorStore vectorStore, KnowledgeBaseService knowledgeBaseService,
        KnowledgeBaseTagService knowledgeBaseTagService) {

        VectorStore kbVectorStore = createVectorStore(vectorStore);

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("search")
            .title("Knowledge Base Search")
            .description("Search data from the knowledge base.")
            .type(TOOLS)
            .properties(
                Stream
                    .of(
                        Stream.of(
                            integer(KNOWLEDGE_BASE_ID)
                                .label("Knowledge Base")
                                .description("The knowledge base to search.")
                                .options(getKnowledgeBaseOptions(knowledgeBaseService))
                                .required(true),
                            array(TAG_IDS)
                                .label("Tags")
                                .description(
                                    "Filter results by tags. Documents with ANY of the selected tags will be " +
                                        "returned (OR logic).")
                                .items(integer())
                                .options(getTagOptions(knowledgeBaseTagService))
                                .optionsLookupDependsOn(KNOWLEDGE_BASE_ID)
                                .required(false),
                            QUERY_PROPERTY),
                        SEARCH_PROPERTIES.stream())
                    .flatMap(stream -> stream)
                    .toList())
            .object(() -> (MultipleConnectionsToolFunction) (
                inputParameters, connectionParameters, extensions, componentConnections, context) -> {
                ComponentConnection vectorStoreComponentConnection = componentConnections.get(KNOWLEDGE_BASE);

                return kbVectorStore.search(
                    inputParameters,
                    ParametersFactory.create(
                        vectorStoreComponentConnection == null
                            ? Map.of() : vectorStoreComponentConnection.getParameters()),
                    null);
            });
    }

    private static ClusterElementDefinition.OptionsFunction<Long> getKnowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                String name = knowledgeBase.getName();

                if (searchText == null || name.toLowerCase()
                    .contains(searchText.toLowerCase())) {

                    options.add(option(name, knowledgeBase.getId()
                        .longValue()));
                }
            }

            return options;
        };
    }

    private static ClusterElementDefinition.OptionsFunction<Long> getTagOptions(
        KnowledgeBaseTagService knowledgeBaseTagService) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            Long knowledgeBaseId = inputParameters.getLong(KNOWLEDGE_BASE_ID);

            List<Tag> tags;

            if (knowledgeBaseId == null) {
                tags = knowledgeBaseTagService.getAllTags();
            } else {
                tags = knowledgeBaseTagService.getTagsByKnowledgeBaseId()
                    .getOrDefault(knowledgeBaseId, List.of());
            }

            Map<Long, Tag> unique = new LinkedHashMap<>();

            for (Tag tag : tags) {
                Long tagId = Objects.requireNonNull(tag.getId());

                unique.putIfAbsent(tagId, tag);
            }

            List<Option<Long>> options = new ArrayList<>();

            for (Tag tag : unique.values()) {
                String tagName = tag.getName();

                if (searchText == null || tagName.toLowerCase()
                    .contains(searchText.toLowerCase())) {

                    options.add(option(tagName, Objects.requireNonNull(tag.getId())
                        .longValue()));
                }
            }

            return options;
        };
    }
}
