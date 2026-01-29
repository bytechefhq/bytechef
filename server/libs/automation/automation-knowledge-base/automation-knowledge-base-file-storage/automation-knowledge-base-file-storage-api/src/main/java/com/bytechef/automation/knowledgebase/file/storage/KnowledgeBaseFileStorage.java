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
import java.io.InputStream;

/**
 * @author Ivica Cardic
 */
public interface KnowledgeBaseFileStorage {

    /**
     * Deletes the chunk content associated with the given file entry.
     *
     * @param fileEntry the file entry whose associated chunk content is to be deleted
     */
    void deleteChunkContent(FileEntry fileEntry);

    /**
     * Deletes the document associated with the given file entry. This operation will remove the underlying file data
     * linked with the specified FileEntry.
     *
     * @param fileEntry the file entry representing the document to be deleted
     */
    void deleteDocument(FileEntry fileEntry);

    /**
     * Reads the chunk content associated with the given file entry.
     *
     * @param fileEntry the file entry representing the chunk whose content is to be read
     * @return the content of the chunk as a string
     */
    String readChunkContent(FileEntry fileEntry);

    /**
     * Retrieves an input stream for reading the content of the document associated with the specified file entry.
     *
     * @param fileEntry the file entry representing the document whose input stream is to be retrieved
     * @return an InputStream to read the content of the document
     */
    InputStream getDocumentInputStream(FileEntry fileEntry);

    /**
     * Reads the content of the document associated with the specified file entry and returns it as a byte array.
     *
     * @param fileEntry the file entry representing the document whose content is to be read and converted to a byte
     *                  array
     * @return a byte array containing the content of the document
     */
    byte[] readDocumentToBytes(FileEntry fileEntry);

    /**
     * Stores the content of a chunk identified by the given chunk ID. The chunk content is associated with a
     * {@link FileEntry} that represents the stored file metadata and details.
     *
     * @param chunkId the unique identifier of the chunk whose content is to be stored
     * @param content the content to be stored for the specified chunk
     * @return a {@link FileEntry} representing the metadata and details of the stored content
     */
    FileEntry storeChunkContent(long chunkId, String content);

    /**
     * Stores the content of a document with the specified filename and input stream. This method handles the creation
     * of a {@link FileEntry} object representing the metadata and details of the stored document.
     *
     * @param filename    the name of the file to be stored
     * @param inputStream the input stream containing the content of the document
     * @return a {@link FileEntry} representing the metadata and details of the stored document
     */
    FileEntry storeDocument(String filename, InputStream inputStream);
}
