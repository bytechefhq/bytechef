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

package com.bytechef.file.storage.service;

import com.bytechef.file.storage.domain.VersionedFileContent;
import com.bytechef.file.storage.exception.FileStorageException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A {@link FileStorageService} that can write a file only if it has not been changed since it was read, which lets
 * several nodes update the same file without losing each other's changes.
 *
 * @author Asish Kumar
 */
public interface ConditionalFileStorageService extends FileStorageService {

    /**
     * Reads the content of a file together with its current version.
     *
     * @return the content and version, or empty if the file doesn't exist
     */
    Optional<VersionedFileContent> readFileContentVersioned(String directory, String filename)
        throws FileStorageException;

    /**
     * Writes the file only if its version still equals {@code expectedVersion}, or only if the file doesn't exist yet
     * when {@code expectedVersion} is {@code null}.
     *
     * @return {@code true} if the content was written, {@code false} if the file was changed, created or deleted by
     *         someone else in the meantime
     */
    boolean storeFileContentIfVersion(
        String directory, String filename, byte[] data, @Nullable String expectedVersion) throws FileStorageException;
}
