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

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public class KnowledgeBaseFileStorageImpl implements KnowledgeBaseFileStorage {

    private static final String KNOWLEDGE_BASE_CHUNKS_DIR = "knowledge_base_chunks";
    private static final String KNOWLEDGE_BASE_DOCUMENTS_DIR = "knowledge_base";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteChunkContent(FileEntry fileEntry) {
        fileStorageService.deleteFile(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry);
    }

    @Override
    public void deleteDocument(FileEntry fileEntry) {
        fileStorageService.deleteFile(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Override
    public String readChunkContent(FileEntry fileEntry) {
        byte[] bytes = fileStorageService.readFileToBytes(KNOWLEDGE_BASE_CHUNKS_DIR, fileEntry);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public InputStream getDocumentInputStream(FileEntry fileEntry) {
        return fileStorageService.getInputStream(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Override
    public byte[] readDocumentToBytes(FileEntry fileEntry) {
        return fileStorageService.readFileToBytes(KNOWLEDGE_BASE_DOCUMENTS_DIR, fileEntry);
    }

    @Override
    public FileEntry storeChunkContent(long chunkId, String content) {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        return fileStorageService.storeFileContent(
            KNOWLEDGE_BASE_CHUNKS_DIR, chunkId + ".txt", new ByteArrayInputStream(contentBytes));
    }

    @Override
    public FileEntry storeDocument(String filename, InputStream inputStream) {
        return fileStorageService.storeFileContent(KNOWLEDGE_BASE_DOCUMENTS_DIR, filename, inputStream);
    }
}
