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

import static com.bytechef.component.ai.vectorstore.s3.vectorstore.S3TextPreservingVectorStore.CONTENT_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.s3.S3VectorStore;

/**
 * @author Marko Krišković
 */
class S3TextPreservingVectorStoreTest {

    private final S3VectorStore s3VectorStore = mock(S3VectorStore.class);
    private final S3TextPreservingVectorStore textPreservingVectorStore =
        new S3TextPreservingVectorStore(s3VectorStore);

    @Test
    void testAddCopiesTextIntoMetadata() {
        textPreservingVectorStore.add(List.of(new Document("id-1", "some content", Map.of("source", "readme.md"))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(s3VectorStore).add(argumentCaptor.capture());

        Document document = argumentCaptor.getValue()
            .getFirst();

        assertEquals("id-1", document.getId());
        assertEquals("some content", document.getText());
        assertEquals("some content", document.getMetadata()
            .get(CONTENT_METADATA_KEY));
        assertEquals("readme.md", document.getMetadata()
            .get("source"));
    }

    @Test
    void testSimilaritySearchRestoresTextAndKey() {
        when(s3VectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(
                List.of(
                    Document.builder()
                        .text("id-1")
                        .metadata(Map.of(CONTENT_METADATA_KEY, "some content", "source", "readme.md"))
                        .build()));

        List<Document> documents = textPreservingVectorStore.similaritySearch(
            SearchRequest.builder()
                .query("content")
                .build());

        Document document = documents.getFirst();

        assertEquals("id-1", document.getId());
        assertEquals("some content", document.getText());
        assertEquals("readme.md", document.getMetadata()
            .get("source"));
        assertFalse(document.getMetadata()
            .containsKey(CONTENT_METADATA_KEY));
    }

    @Test
    void testSimilaritySearchKeepsDocumentsWithoutStoredText() {
        when(s3VectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(
                List.of(
                    Document.builder()
                        .text("id-1")
                        .metadata(Map.of("source", "readme.md"))
                        .build()));

        List<Document> documents = textPreservingVectorStore.similaritySearch(
            SearchRequest.builder()
                .query("content")
                .build());

        Document document = documents.getFirst();

        assertEquals("id-1", document.getText());
        assertEquals("readme.md", document.getMetadata()
            .get("source"));
    }

    @Test
    void testSimilaritySearchReturnsEmptyListWhenStoreReturnsNull() {
        when(s3VectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        List<Document> documents = textPreservingVectorStore.similaritySearch(
            SearchRequest.builder()
                .query("content")
                .build());

        assertEquals(List.of(), documents);
    }
}
