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

package com.bytechef.file.storage.base64.service;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public class NoopFileStorageService implements FileStorageService {

    private static final String NOOP = "noop://";

    public NoopFileStorageService() {
    }

    @Override
    public void deleteFile(String directoryPath, FileEntry fileEntry) {
    }

    @Override
    public boolean fileExists(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        return true;
    }

    @Override
    public InputStream getFileStream(String directoryPath, FileEntry fileEntry) {
        String url = fileEntry.getUrl();

        String data = url.replace(NOOP, "");

        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] readFileToBytes(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        String data = url.replace(NOOP, "");

        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String readFileToString(String directoryPath, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        return url.replace(NOOP, "");
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, byte[] data) throws FileStorageException {
        return new FileEntry(fileName, NOOP + new String(data));
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, String data) throws FileStorageException {
        return new FileEntry(fileName, NOOP + data);
    }

    @Override
    public FileEntry storeFileContent(String directoryPath, String fileName, InputStream inputStream) {
        try {
            return new FileEntry(fileName, NOOP + new String(toByteArray(inputStream)));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file", ioe);
        }
    }

    private byte[] toByteArray(InputStream inputStream) throws FileStorageException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[4];

        while ((nRead = inputStream.readNBytes(data, 0, data.length)) != 0) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }
}
