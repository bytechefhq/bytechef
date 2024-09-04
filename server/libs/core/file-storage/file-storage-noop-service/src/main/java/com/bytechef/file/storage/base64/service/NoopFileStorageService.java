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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class NoopFileStorageService implements FileStorageService {

    private static final String URL_PREFIX = "noop://";

    public NoopFileStorageService() {
    }

    @Override
    public void deleteFile(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull FileEntry fileEntry) throws FileStorageException {
        return true;
    }

    @Override
    public boolean fileExists(@NonNull String directoryPath, @NonNull String nonRandomFilename)
        throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileEntry getFileEntry(@NonNull String directoryPath, @NonNull String nonRandomFilename)
        throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<FileEntry> getFileEntries(@NonNull String directoryPath) throws FileStorageException {
        return Set.of();
    }

    @Override
    public InputStream getFileStream(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        String url = fileEntry.getUrl();

        String data = url.replace(URL_PREFIX, "");

        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public URL getFileEntryURL(@NonNull String directoryPath, @NonNull FileEntry fileEntry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] readFileToBytes(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        String url = fileEntry.getUrl();

        String data = url.replace(URL_PREFIX, "");

        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String readFileToString(@NonNull String directoryPath, @NonNull FileEntry fileEntry)
        throws FileStorageException {
        String url = fileEntry.getUrl();

        return url.replace(URL_PREFIX, "");
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String filename, byte[] data)
        throws FileStorageException {
        return new FileEntry(filename, URL_PREFIX + new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public FileEntry
        storeFileContent(@NonNull String directoryPath, @NonNull String filename, byte[] data, boolean randomFilename)
            throws FileStorageException {

        return storeFileContent(directoryPath, filename, data);
    }

    @Override
    public FileEntry storeFileContent(@NonNull String directoryPath, @NonNull String filename, @NonNull String data)
        throws FileStorageException {
        return new FileEntry(filename, URL_PREFIX + data);
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull String data, boolean randomFilename)
        throws FileStorageException {

        return storeFileContent(directoryPath, filename, data);
    }

    @Override
    public FileEntry
        storeFileContent(@NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream) {
        try {
            return new FileEntry(filename, URL_PREFIX + new String(toByteArray(inputStream), StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file", ioe);
        }
    }

    @Override
    public FileEntry storeFileContent(
        @NonNull String directoryPath, @NonNull String filename, @NonNull InputStream inputStream,
        boolean randomFilename) throws FileStorageException {

        return storeFileContent(directoryPath, filename, inputStream);
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
