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

package com.bytechef.component.ai.vectorstore.knowledgebase.util;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared option builders used by Knowledge Base actions and cluster elements.
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseOptionsUtils {

    private KnowledgeBaseOptionsUtils() {
    }

    public static ClusterElementDefinition.OptionsFunction<Long> knowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (
            inputParameters, connectionParameters, lookupDependsOnPaths, searchText,
            context) -> buildKnowledgeBaseOptions(searchText, knowledgeBaseService);
    }

    public static ActionDefinition.OptionsFunction<Long> knowledgeBaseActionOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (
            inputParameters, connectionParameters, dependencyPaths, searchText,
            context) -> buildKnowledgeBaseOptions(searchText, knowledgeBaseService);
    }

    public static ClusterElementDefinition.OptionsFunction<String> tagOptions(
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> buildTagOptions(
            inputParameters, searchText, knowledgeBaseDocumentTagService);
    }

    public static ClusterElementDefinition.OptionsFunction<Long> documentOptions(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        return (
            inputParameters, connectionParameters, lookupDependsOnPaths, searchText,
            context) -> buildDocumentOptions(inputParameters, searchText, knowledgeBaseDocumentService);
    }

    public static ActionDefinition.OptionsFunction<Long> documentActionOptions(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> buildDocumentOptions(
            inputParameters, searchText, knowledgeBaseDocumentService);
    }

    public static ClusterElementDefinition.OptionsFunction<Long> documentChunkOptions(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade) {

        return (
            inputParameters, connectionParameters, lookupDependsOnPaths, searchText,
            context) -> buildDocumentChunkOptions(inputParameters, knowledgeBaseDocumentChunkFacade);
    }

    public static ActionDefinition.OptionsFunction<Long> documentChunkActionOptions(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade) {

        return (
            inputParameters, connectionParameters, dependencyPaths, searchText,
            context) -> buildDocumentChunkOptions(inputParameters, knowledgeBaseDocumentChunkFacade);
    }

    private static List<Option<Long>> buildKnowledgeBaseOptions(
        String searchText, KnowledgeBaseService knowledgeBaseService) {

        List<Option<Long>> options = new ArrayList<>();

        for (KnowledgeBase knowledgeBase : knowledgeBaseService.getKnowledgeBases()) {
            String name = knowledgeBase.getName();

            if (matchesSearchText(name, searchText)) {
                options.add(option(name, knowledgeBase.getId()
                    .longValue()));
            }
        }

        return options;
    }

    private static List<Option<String>> buildTagOptions(
        Parameters inputParameters, String searchText,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        Long knowledgeBaseId = inputParameters.getLong(KNOWLEDGE_BASE_ID);

        List<String> tagNames = knowledgeBaseId == null
            ? knowledgeBaseDocumentTagService.getAllTagNames()
            : knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseId(knowledgeBaseId);

        List<Option<String>> options = new ArrayList<>();

        for (String tagName : tagNames) {
            if (matchesSearchText(tagName, searchText)) {
                options.add(option(tagName, tagName));
            }
        }

        return options;
    }

    private static List<Option<Long>> buildDocumentOptions(
        Parameters inputParameters, String searchText,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService) {

        Long knowledgeBaseId = inputParameters.getLong(KNOWLEDGE_BASE_ID);

        if (knowledgeBaseId == null) {
            return List.of();
        }

        List<Option<Long>> options = new ArrayList<>();

        List<KnowledgeBaseDocument> documents =
            knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId);

        for (KnowledgeBaseDocument document : documents) {
            String name = document.getName();

            if (matchesSearchText(name, searchText)) {
                options.add(option(name, document.getId()
                    .longValue()));
            }
        }

        return options;
    }

    private static List<Option<Long>> buildDocumentChunkOptions(
        Parameters inputParameters, KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade) {

        Long knowledgeBaseDocumentId = inputParameters.getLong(KNOWLEDGE_BASE_DOCUMENT_ID);

        if (knowledgeBaseDocumentId == null) {
            return List.of();
        }

        List<Option<Long>> options = new ArrayList<>();

        List<KnowledgeBaseDocumentChunk> chunks =
            knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId);

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeBaseDocumentChunk chunk = chunks.get(i);
            String textContent = chunk.getTextContent();
            String label = (textContent != null && !textContent.isBlank())
                ? (textContent.length() > 50 ? textContent.substring(0, 50) + "..." : textContent)
                : "Chunk #" + (i + 1);

            options.add(option(label, chunk.getId()
                .longValue()));
        }

        return options;
    }

    private static boolean matchesSearchText(String value, String searchText) {
        if (searchText == null) {
            return true;
        }

        return value.toLowerCase(Locale.ROOT)
            .contains(searchText.toLowerCase(Locale.ROOT));
    }
}
