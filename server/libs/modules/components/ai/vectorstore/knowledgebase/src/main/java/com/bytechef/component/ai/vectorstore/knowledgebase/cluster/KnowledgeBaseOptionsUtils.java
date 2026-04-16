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

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseTagService;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.tag.domain.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Shared option builders used by Knowledge Base cluster elements.
 *
 * @author Ivica Cardic
 */
final class KnowledgeBaseOptionsUtils {

    private KnowledgeBaseOptionsUtils() {
    }

    static ClusterElementDefinition.OptionsFunction<Long> knowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                String name = knowledgeBase.getName();

                if (matchesSearchText(name, searchText)) {
                    options.add(option(name, knowledgeBase.getId()
                        .longValue()));
                }
            }

            return options;
        };
    }

    static ClusterElementDefinition.OptionsFunction<Long> tagOptions(KnowledgeBaseTagService knowledgeBaseTagService) {
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

                if (matchesSearchText(tagName, searchText)) {
                    options.add(option(tagName, Objects.requireNonNull(tag.getId())
                        .longValue()));
                }
            }

            return options;
        };
    }

    private static boolean matchesSearchText(String value, String searchText) {
        if (searchText == null) {
            return true;
        }

        return value.toLowerCase(Locale.ROOT)
            .contains(searchText.toLowerCase(Locale.ROOT));
    }
}
