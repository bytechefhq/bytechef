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

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public interface FileStorageService {

    void deleteFile(String directory, FileEntry fileEntry);

    boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException;

    boolean fileExists(String directory, String filename) throws FileStorageException;

    long getContentLength(String directory, FileEntry fileEntry) throws FileStorageException;

    FileEntry getFileEntry(String directory, String filename) throws FileStorageException;

    Set<FileEntry> getFileEntries(String directory) throws FileStorageException;

    URL getFileEntryURL(String directory, FileEntry fileEntry);

    InputStream getInputStream(String directory, FileEntry fileEntry) throws FileStorageException;

    OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException;

    byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException;

    String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException;

    String getType();

    FileEntry storeFileContent(String directory, String filename, byte[] data) throws FileStorageException;

    FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename)
        throws FileStorageException;

    FileEntry storeFileContent(String directory, String filename, String data) throws FileStorageException;

    FileEntry storeFileContent(String directory, String filename, String data, boolean generateFilename)
        throws FileStorageException;

    FileEntry storeFileContent(String directory, String filename, InputStream inputStream) throws FileStorageException;

    FileEntry storeFileContent(String directory, String filename, InputStream inputStream, boolean generateFilename)
        throws FileStorageException;
}
