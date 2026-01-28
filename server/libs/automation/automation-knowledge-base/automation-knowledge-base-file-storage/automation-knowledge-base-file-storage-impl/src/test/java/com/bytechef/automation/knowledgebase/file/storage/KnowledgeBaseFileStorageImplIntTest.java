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

package com.bytechef.automation.knowledgebase.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for {@link KnowledgeBaseFileStorageImpl}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseFileStorageImplIntTest {

    private static final String KNOWLEDGE_BASE_CHUNKS_DIR = "knowledge_base_chunks";
    private static final String KNOWLEDGE_BASE_DOCUMENTS_DIR = "knowledge_base";

    @Mock
    private FileStorageService fileStorageService;

    private KnowledgeBaseFileStorageImpl knowledgeBaseFileStorage;

    @BeforeEach
    void beforeEach() {
        knowledgeBaseFileStorage = new KnowledgeBaseFileStorageImpl(fileStorageService);
    }

    @Test
    void testStoreDocument() {
        String filename = "test-document.txt";
        InputStream inputStream = new ByteArrayInputStream("Test content".getBytes(StandardCharsets.UTF_8));
        FileEntry expectedFileEntry = new FileEntry(filename, "file://test/" + filename);

        when(
            fileStorageService.storeFileContent(eq(KNOWLEDGE_BASE_DOCUMENTS_DIR), eq(filename), any(InputStream.class)))
                .thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeDocument(filename, inputStream);

        assertThat(result).isEqualTo(expectedFileEntry);

        verify(fileStorageService).storeFileContent(eq(KNOWLEDGE_BASE_DOCUMENTS_DIR), eq(filename),
            any(InputStream.class));
    }

    @Test
    void testStoreChunkContent() {
        long chunkId = 123L;
        String content = "Chunk content text";
        FileEntry expectedFileEntry = new FileEntry(chunkId + ".txt", "file://test/chunks/" + chunkId + ".txt");

        when(fileStorageService.storeFileContent(
            eq(KNOWLEDGE_BASE_CHUNKS_DIR), eq(chunkId + ".txt"), any(InputStream.class))).thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeChunkContent(chunkId, content);

        assertThat(result).isEqualTo(expectedFileEntry);

        verify(fileStorageService).storeFileContent(
            eq(KNOWLEDGE_BASE_CHUNKS_DIR), eq(chunkId + ".txt"), any(InputStream.class));
    }

    @Test
    void testReadChunkContent() {
        FileEntry fileEntry = new FileEntry("123.txt", "file://test/chunks/123.txt");
        String expectedContent = "Chunk content";

        when(fileStorageService.readFileToBytes(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry))
            .thenReturn(expectedContent.getBytes(StandardCharsets.UTF_8));

        String result = knowledgeBaseFileStorage.readChunkContent(fileEntry);

        assertThat(result).isEqualTo(expectedContent);

        verify(fileStorageService).readFileToBytes(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry);
    }

    @Test
    void testReadDocumentToBytes() {
        FileEntry fileEntry = new FileEntry("document.txt", "file://test/document.txt");
        byte[] expectedBytes = "Document content".getBytes(StandardCharsets.UTF_8);

        when(fileStorageService.readFileToBytes(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry)).thenReturn(expectedBytes);

        byte[] result = knowledgeBaseFileStorage.readDocumentToBytes(fileEntry);

        assertThat(result).isEqualTo(expectedBytes);

        verify(fileStorageService).readFileToBytes(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Test
    void testGetDocumentInputStream() {
        FileEntry fileEntry = new FileEntry("document.txt", "file://test/document.txt");
        InputStream expectedInputStream = new ByteArrayInputStream("Document content".getBytes(StandardCharsets.UTF_8));

        when(fileStorageService.getInputStream(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry))
            .thenReturn(expectedInputStream);

        InputStream result = knowledgeBaseFileStorage.getDocumentInputStream(fileEntry);

        assertThat(result).isEqualTo(expectedInputStream);

        verify(fileStorageService).getInputStream(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Test
    void testDeleteDocument() {
        FileEntry fileEntry = new FileEntry("document.txt", "file://test/document.txt");

        knowledgeBaseFileStorage.deleteDocument(fileEntry);

        verify(fileStorageService).deleteFile(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Test
    void testDeleteChunkContent() {
        FileEntry fileEntry = new FileEntry("123.txt", "file://test/chunks/123.txt");

        knowledgeBaseFileStorage.deleteChunkContent(fileEntry);

        verify(fileStorageService).deleteFile(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry);
    }

    @Test
    void testStoreDocumentWithSpecialCharactersInFilename() {
        String filename = "document with spaces & special-chars.txt";
        InputStream inputStream = new ByteArrayInputStream("Content".getBytes(StandardCharsets.UTF_8));
        FileEntry expectedFileEntry = new FileEntry(filename, "file://test/" + filename);

        when(
            fileStorageService.storeFileContent(eq(KNOWLEDGE_BASE_DOCUMENTS_DIR), eq(filename), any(InputStream.class)))
                .thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeDocument(filename, inputStream);

        assertThat(result).isEqualTo(expectedFileEntry);
    }

    @Test
    void testReadChunkContentWithUtf8Characters() {
        FileEntry fileEntry = new FileEntry("123.txt", "file://test/chunks/123.txt");
        String expectedContent = "Content with UTF-8: 你好世界 🌍";

        when(fileStorageService.readFileToBytes(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry))
            .thenReturn(expectedContent.getBytes(StandardCharsets.UTF_8));

        String result = knowledgeBaseFileStorage.readChunkContent(fileEntry);

        assertThat(result).isEqualTo(expectedContent);
    }

    @Test
    void testStoreChunkContentWithUtf8Characters() {
        long chunkId = 456L;
        String content = "Content with UTF-8: 你好世界 🌍";
        FileEntry expectedFileEntry = new FileEntry(chunkId + ".txt", "file://test/chunks/" + chunkId + ".txt");

        when(fileStorageService.storeFileContent(
            eq(KNOWLEDGE_BASE_CHUNKS_DIR), eq(chunkId + ".txt"), any(InputStream.class))).thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeChunkContent(chunkId, content);

        assertThat(result).isEqualTo(expectedFileEntry);
    }

    @Test
    void testStoreDocumentWithEmptyContent() {
        String filename = "empty.txt";
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        FileEntry expectedFileEntry = new FileEntry(filename, "file://test/" + filename);

        when(
            fileStorageService.storeFileContent(eq(KNOWLEDGE_BASE_DOCUMENTS_DIR), eq(filename), any(InputStream.class)))
                .thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeDocument(filename, inputStream);

        assertThat(result).isEqualTo(expectedFileEntry);
    }

    @Test
    void testStoreChunkContentWithEmptyString() {
        long chunkId = 789L;
        String content = "";
        FileEntry expectedFileEntry = new FileEntry(chunkId + ".txt", "file://test/chunks/" + chunkId + ".txt");

        when(fileStorageService.storeFileContent(
            eq(KNOWLEDGE_BASE_CHUNKS_DIR), eq(chunkId + ".txt"), any(InputStream.class))).thenReturn(expectedFileEntry);

        FileEntry result = knowledgeBaseFileStorage.storeChunkContent(chunkId, content);

        assertThat(result).isEqualTo(expectedFileEntry);
    }
}
