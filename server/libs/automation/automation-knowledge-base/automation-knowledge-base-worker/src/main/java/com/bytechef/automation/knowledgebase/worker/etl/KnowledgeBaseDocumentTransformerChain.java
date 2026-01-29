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

package com.bytechef.automation.knowledgebase.worker.etl;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Transformer chain for processing knowledge base documents using Spring AI transformers.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledgebase", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentTransformerChain {

    /**
     * Transforms documents by splitting into chunks.
     *
     * @param documents         the documents to transform
     * @param minChunkSizeChars the minimum chunk size in characters
     * @param maxChunkSize      the maximum chunk size in tokens
     * @param overlap           the number of tokens to overlap between chunks (0 for no overlap)
     * @return the transformed documents
     */
    public List<Document> transform(List<Document> documents, int minChunkSizeChars, int maxChunkSize, int overlap) {
        if (overlap > 0) {
            OverlappingTokenTextSplitter overlappingSplitter = OverlappingTokenTextSplitter.builder()
                .withChunkSize(maxChunkSize)
                .withMinChunkSizeChars(minChunkSizeChars)
                .withOverlap(overlap)
                .build();

            return overlappingSplitter.apply(documents);
        }

        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
            .withChunkSize(maxChunkSize)
            .withMinChunkSizeChars(minChunkSizeChars)
            .withKeepSeparator(true)
            .build();

        return tokenTextSplitter.apply(documents);
    }
}
