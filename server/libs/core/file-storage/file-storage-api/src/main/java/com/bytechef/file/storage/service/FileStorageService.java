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

/**
 * @author Ivica Cardic
 */
public interface FileStorageService {

    void deleteFile(String directoryPath, FileEntry fileEntry);

    boolean fileExists(String directoryPath, FileEntry fileEntry) throws FileStorageException;

    InputStream getFileStream(String directoryPath, FileEntry fileEntry) throws FileStorageException;

    URL getFileEntryURL(String directoryPath, FileEntry fileEntry);

    byte[] readFileToBytes(String directoryPath, FileEntry fileEntry) throws FileStorageException;

    String readFileToString(String directoryPath, FileEntry fileEntry) throws FileStorageException;

    FileEntry storeFileContent(String directoryPath, String filename, byte[] data) throws FileStorageException;

    FileEntry storeFileContent(String directoryPath, String filename, byte[] data, boolean randomFilename)
        throws FileStorageException;

    FileEntry storeFileContent(String directoryPath, String filename, String data) throws FileStorageException;

    FileEntry storeFileContent(String directoryPath, String filename, InputStream inputStream)
        throws FileStorageException;

        throws FileStorageException;
}
