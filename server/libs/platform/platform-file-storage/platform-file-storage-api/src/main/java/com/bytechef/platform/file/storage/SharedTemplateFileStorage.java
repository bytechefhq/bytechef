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

package com.bytechef.platform.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import java.io.InputStream;

/**
 * Defines operations for managing shared template files in a storage system. This interface provides methods for
 * storing, retrieving, and deleting files. It ensures that shared templates can be managed securely and consistently.
 *
 * @author Ivica Cardic
 */
public interface SharedTemplateFileStorage {

    /**
     * Retrieves an InputStream for the specified file entry.
     *
     * @param fileEntry the file entry representing the file to be accessed
     * @return an InputStream to read the content of the file
     */
    InputStream getInputStream(FileEntry fileEntry);

    /**
     * Stores the content of a file using the specified file name and input stream.
     *
     * @param filename    the name of the file to store
     * @param inputStream the input stream containing the file content
     * @return a {@code FileEntry} representing the stored file, including its metadata and location
     */
    FileEntry storeFileContent(String filename, InputStream inputStream);

    /**
     * Deletes the specified file represented by the given {@code FileEntry}.
     *
     * @param fileEntry the file entry representing the file to be deleted
     */
    void deleteFile(FileEntry fileEntry);
}
