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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@SuppressFBWarnings("EI")
class KnowledgeBaseDocumentGraphQlController {

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentGraphQlController(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, TagService tagService) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.tagService = tagService;
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocument", field = "chunks")
    List<KnowledgeBaseDocumentChunk> documentChunks(KnowledgeBaseDocument knowledgeBaseDocument) {
        return knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(
            knowledgeBaseDocument.getId());
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocument", field = "tags")
    List<Tag> documentTags(KnowledgeBaseDocument document) {
        List<Long> tagIds = document.getTagIds();

        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @QueryMapping
    KnowledgeBaseDocument knowledgeBaseDocument(@Argument Long id) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocument(id);
    }

    @QueryMapping
    DocumentStatusUpdate knowledgeBaseDocumentStatus(@Argument Long id) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(id);
    }

    @MutationMapping
    boolean deleteKnowledgeBaseDocument(@Argument Long id) {
        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(id);

        return true;
    }
}
