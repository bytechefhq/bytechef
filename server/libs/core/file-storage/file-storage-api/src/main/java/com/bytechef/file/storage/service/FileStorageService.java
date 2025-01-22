/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface FileStorageService {

    void deleteFile(@NonNull String directoryPath, @NonNull FileEntry fileEntry);

    boolean fileExists(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException;

    boolean fileExists(@NonNull String directoryPath, @NonNull String nonRandomFilename) throws FileStorageException;

    FileEntry getFileEntry(
        @NonNull String directoryPath, @NonNull String nonRandomFilename) throws FileStorageException;

    Set<FileEntry> getFileEntries(@NonNull String directoryPath) throws FileStorageException;

    InputStream getFileStream(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException;

    URL getFileEntryURL(@NonNull String directoryPath, @NonNull FileEntry fileEntry);

    byte[] readFileToBytes(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException;

    String readFileToString(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException;

    String getType();

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, byte[] data) throws FileStorageException;

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, byte[] data, boolean randomFilename)
        throws FileStorageException;

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull String data) throws FileStorageException;

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull String data, boolean randomFilename)
        throws FileStorageException;

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream)
        throws FileStorageException;

    FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream,
        boolean randomFilename) throws FileStorageException;
}
