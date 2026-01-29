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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@SuppressFBWarnings("EI")
class KnowledgeBaseDocumentChunkGraphQlController {

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentChunkGraphQlController(KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade) {
        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocumentChunk", field = "content")
    String chunkContent(KnowledgeBaseDocumentChunk chunk) {
        return chunk.getTextContent();
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocumentChunk", field = "metadata")
    Map<String, ?> chunkMetadata(KnowledgeBaseDocumentChunk chunk) {
        return chunk.getMetadata();
    }

    @SchemaMapping(typeName = "KnowledgeBaseDocumentChunk", field = "score")
    Float chunkScore(KnowledgeBaseDocumentChunk chunk) {
        return chunk.getScore();
    }

    @MutationMapping
    KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(
        @Argument Long id,
        @Argument("knowledgeBaseDocumentChunk") KnowledgeBaseDocumentChunkInput knowledgeBaseDocumentChunk) {

        return knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(
            id, knowledgeBaseDocumentChunk.content());
    }

    @MutationMapping
    boolean deleteKnowledgeBaseDocumentChunk(@Argument Long id) {
        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(id);

        return true;
    }

    record KnowledgeBaseDocumentChunkInput(String content) {
    }
}
