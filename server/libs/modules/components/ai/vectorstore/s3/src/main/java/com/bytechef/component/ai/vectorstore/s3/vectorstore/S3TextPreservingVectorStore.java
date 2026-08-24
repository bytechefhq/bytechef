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

package com.bytechef.component.ai.vectorstore.s3.vectorstore;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.s3.S3VectorStore;

/**
 * Carries the document text through S3 Vectors, which stores only the embedding, a key and metadata. Without this,
 * {@code S3VectorStore} returns search results whose text is the vector key rather than the indexed content, leaving
 * both the search action and agent retrieval without anything to read.
 *
 * <p>
 * The text is written to {@value #CONTENT_METADATA_KEY} on the way in and lifted back into the document on the way out.
 * The index must declare that key as non-filterable, otherwise S3 Vectors rejects any chunk above its 2 KB filterable
 * metadata limit.
 *
 * @author Marko Krišković
 */
public class S3TextPreservingVectorStore implements VectorStore {

    public static final String CONTENT_METADATA_KEY = "bytechef_content";

    private final S3VectorStore s3VectorStore;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public S3TextPreservingVectorStore(S3VectorStore s3VectorStore) {
        this.s3VectorStore = s3VectorStore;
    }

    @Override
    public void add(List<Document> documents) {
        s3VectorStore.add(
            documents.stream()
                .map(S3TextPreservingVectorStore::storeTextAsMetadata)
                .toList());
    }

    @Override
    public void delete(List<String> idList) {
        s3VectorStore.delete(idList);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        s3VectorStore.delete(filterExpression);
    }

    @Override
    public String getName() {
        return s3VectorStore.getName();
    }

    @Override
    public <T> Optional<T> getNativeClient() {
        return s3VectorStore.getNativeClient();
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        List<Document> documents = s3VectorStore.similaritySearch(searchRequest);

        if (documents == null) {
            return List.of();
        }

        return documents.stream()
            .map(S3TextPreservingVectorStore::restoreTextFromMetadata)
            .toList();
    }

    private static Document storeTextAsMetadata(Document document) {
        String text = document.getText();

        if (text == null) {
            return document;
        }

        Map<String, Object> metadata = new HashMap<>(document.getMetadata());

        metadata.put(CONTENT_METADATA_KEY, text);

        return new Document(document.getId(), text, metadata);
    }

    /**
     * {@code S3VectorStore} maps a query result onto a document whose text is the vector key, so the key is read back
     * from there and the indexed content is taken out of the metadata.
     */
    private static Document restoreTextFromMetadata(Document document) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());

        Object text = metadata.remove(CONTENT_METADATA_KEY);

        if (text == null) {
            return document;
        }

        Document.Builder builder = Document.builder()
            .text(text.toString())
            .metadata(metadata)
            .score(document.getScore());

        String key = document.getText();

        if (key != null && !key.isBlank()) {
            builder.id(key);
        }

        return builder.build();
    }
}
