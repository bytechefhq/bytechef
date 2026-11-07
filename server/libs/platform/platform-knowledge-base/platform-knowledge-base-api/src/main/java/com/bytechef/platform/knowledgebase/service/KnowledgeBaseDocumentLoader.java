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

package com.bytechef.platform.knowledgebase.service;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface KnowledgeBaseDocumentLoader {

    String DEFAULT_DOCUMENT_NAME = "Workflow Import";

    long load(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, String documentName, List<Chunk> chunks,
        Map<String, Object> additionalMetadata);

    long update(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, @Nullable Long knowledgeBaseDocumentChunkId,
        List<Chunk> chunks, Map<String, Object> additionalMetadata);

    static String deriveDocumentName(List<Chunk> chunks, @Nullable Map<String, Object> additionalMetadata) {
        if (!chunks.isEmpty()) {
            Object filename = chunks.getLast()
                .metadata()
                .get("filename");

            if (filename != null) {
                return filename.toString();
            }
        }

        if (additionalMetadata != null) {
            Object filename = additionalMetadata.get("filename");

            if (filename != null) {
                return filename.toString();
            }
        }

        return DEFAULT_DOCUMENT_NAME;
    }

    record Chunk(@Nullable String id, @Nullable String text, Map<String, Object> metadata) {

        public Chunk {
            metadata = Map.copyOf(metadata);
        }
    }
}
