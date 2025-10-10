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

package com.bytechef.file.storage.base64.service;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public class Base64FileStorageService implements FileStorageService {

    public static final String URL_PREFIX = "base64://";

    public Base64FileStorageService() {
    }

    @Override
    public void deleteFile(String directory, FileEntry fileEntry) {
    }

    @Override
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        return true;
    }

    @Override
    public boolean fileExists(String directory, String filename) throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getContentLength(String directory, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        String string = EncodingUtils.base64DecodeToString(url.replace(URL_PREFIX, ""));

        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);

        return bytes.length;
    }

    @Override
    public FileEntry getFileEntry(String directory, String filename) throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<FileEntry> getFileEntries(String directory) throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getFileEntryURL(String directory, FileEntry fileEntry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream(String directory, FileEntry fileEntry) {
        String url = fileEntry.getUrl();

        return new ByteArrayInputStream(EncodingUtils.base64Decode(url.replace(URL_PREFIX, "")));
    }

    @Override
    public OutputStream getOutputStream(String directory, FileEntry fileEntry) throws FileStorageException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        return EncodingUtils.base64Decode(url.replace(URL_PREFIX, ""));
    }

    @Override
    public String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        return EncodingUtils.base64DecodeToString(url.replace(URL_PREFIX, ""));
    }

    @Override
    public String getType() {
        return ApplicationProperties.FileStorage.Provider.JDBC.name();
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data) throws FileStorageException {
        return new FileEntry(filename, URL_PREFIX + EncodingUtils.base64EncodeToString(data));
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, byte[] data, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, data);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data) throws FileStorageException {
        return new FileEntry(filename, URL_PREFIX + EncodingUtils.base64EncodeToString(data));
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, String data, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, data);
    }

    @Override
    public FileEntry storeFileContent(String directory, String filename, InputStream inputStream) {
        try {
            return new FileEntry(
                filename, URL_PREFIX + EncodingUtils.base64EncodeToString(toByteArray(inputStream)));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file", ioe);
        }
    }

    @Override
    public FileEntry storeFileContent(
        String directory, String filename, InputStream inputStream, boolean generateFilename)
        throws FileStorageException {

        return storeFileContent(directory, filename, inputStream);
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
