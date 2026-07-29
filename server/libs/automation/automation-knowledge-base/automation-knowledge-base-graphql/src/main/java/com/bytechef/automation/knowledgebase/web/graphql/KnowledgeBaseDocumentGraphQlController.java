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

package com.bytechef.automation.knowledgebase.web.graphql;

import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentApiFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class KnowledgeBaseDocumentGraphQlController {

    private final KnowledgeBaseDocumentApiFacade knowledgeBaseDocumentApiFacade;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentGraphQlController(KnowledgeBaseDocumentApiFacade knowledgeBaseDocumentApiFacade) {
        this.knowledgeBaseDocumentApiFacade = knowledgeBaseDocumentApiFacade;
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocument", field = "chunks")
    List<KnowledgeBaseDocumentChunk> documentChunks(KnowledgeBaseDocument knowledgeBaseDocument) {
        return knowledgeBaseDocumentApiFacade.getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(
            knowledgeBaseDocument.getId());
    }

    @QueryMapping
    List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks(@Argument Long id) {
        return knowledgeBaseDocumentApiFacade.getKnowledgeBaseDocumentChunksByDocumentId(id);
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocument", field = "tags")
    List<String> documentTags(KnowledgeBaseDocument document) {
        List<String> tagNames = document.getTagNames();

        if (tagNames == null) {
            return List.of();
        }

        return tagNames;
    }

    @QueryMapping
    KnowledgeBaseDocument knowledgeBaseDocument(@Argument Long id) {
        return knowledgeBaseDocumentApiFacade.getKnowledgeBaseDocument(id);
    }

    @QueryMapping
    DocumentStatusUpdate knowledgeBaseDocumentStatus(@Argument Long id) {
        return knowledgeBaseDocumentApiFacade.getKnowledgeBaseDocumentStatus(id);
    }

    @MutationMapping
    boolean deleteKnowledgeBaseDocument(@Argument Long id) {
        knowledgeBaseDocumentApiFacade.deleteKnowledgeBaseDocument(id);

        return true;
    }
}
