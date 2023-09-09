
/*
 * Copyright 2021 <your company/name>.
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
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ivica Cardic
 */
public class Base64FileStorageService implements FileStorageService {

    public Base64FileStorageService() {
    }

    @Override
    public boolean fileExists(String directory, FileEntry fileEntry) throws FileStorageException {
        return true;
    }

    @Override
    public InputStream getFileStream(String directory, FileEntry fileEntry) {
        String url = fileEntry.getUrl();

        return new ByteArrayInputStream(EncodingUtils.decodeBase64(url.replace("base64://", "")));
    }

    @Override
    public byte[] readFileToBytes(String directory, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        return EncodingUtils.decodeBase64(url.replace("base64://", ""));
    }

    @Override
    public String readFileToString(String directory, FileEntry fileEntry) throws FileStorageException {
        String url = fileEntry.getUrl();

        return EncodingUtils.decodeBase64ToString(url.replace("base64://", ""));
    }

    @Override
    public FileEntry storeFileContent(String directory, String fileName, byte[] data) throws FileStorageException {
        return new FileEntry(
            fileName, "base64://" + EncodingUtils.encodeBase64ToString(data));
    }

    @Override
    public FileEntry storeFileContent(String directory, String fileName, String data) throws FileStorageException {
        return new FileEntry(fileName, "base64://" + EncodingUtils.encodeBase64ToString(data));
    }

    @Override
    public FileEntry storeFileContent(String directory, String fileName, InputStream inputStream) {
        try {
            return new FileEntry(fileName, "base64://" + EncodingUtils.encodeBase64ToString(toByteArray(inputStream)));
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
