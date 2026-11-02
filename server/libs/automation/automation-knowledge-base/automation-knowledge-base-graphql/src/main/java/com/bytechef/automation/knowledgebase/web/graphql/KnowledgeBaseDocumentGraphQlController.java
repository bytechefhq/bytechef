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

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
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

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentGraphQlController(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocument", field = "chunks")
    List<KnowledgeBaseDocumentChunk> documentChunks(KnowledgeBaseDocument knowledgeBaseDocument) {
        return knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(
            knowledgeBaseDocument.getId());
    }

    @QueryMapping
    public List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks(@Argument Long id) {
        return workspaceKnowledgeBaseFacade.getKnowledgeBaseDocumentChunks(id);
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
    public KnowledgeBaseDocument knowledgeBaseDocument(@Argument Long id) {
        return workspaceKnowledgeBaseFacade.getKnowledgeBaseDocument(id);
    }

    @QueryMapping
    public DocumentStatusUpdate knowledgeBaseDocumentStatus(@Argument Long id) {
        return workspaceKnowledgeBaseFacade.getKnowledgeBaseDocumentStatus(id);
    }

    @MutationMapping
    public boolean deleteKnowledgeBaseDocument(@Argument Long id) {
        workspaceKnowledgeBaseFacade.deleteKnowledgeBaseDocument(id);

        return true;
    }
}
