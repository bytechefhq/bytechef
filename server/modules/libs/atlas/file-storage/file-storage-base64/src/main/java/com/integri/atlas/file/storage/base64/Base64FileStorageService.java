/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.file.storage.base64;

import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.exception.FileStorageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * @author Ivica Cardic
 */
public class Base64FileStorageService implements FileStorageService {

    @Override
    public void deleteFile(String url) throws FileStorageException {}

    @Override
    public void deleteFiles(long retentionTime) throws FileStorageException {}

    @Override
    public boolean fileExists(String url) throws FileStorageException {
        return true;
    }

    @Override
    public FileEntry storeFileContent(String fileName, String content) throws FileStorageException {
        Base64.Encoder encoder = Base64.getEncoder();

        return FileEntry.of(fileName, encoder.encodeToString(content.getBytes()));
    }

    @Override
    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        Base64.Encoder encoder = Base64.getEncoder();

        try {
            return FileEntry.of(fileName, encoder.encodeToString(toByteArray(inputStream)));
        } catch (IOException ioe) {
            throw new FileStorageException("Failed to store file", ioe);
        }
    }

    @Override
    public String readFileContent(String url) throws FileStorageException {
        Base64.Decoder decoder = Base64.getDecoder();

        return new String(decoder.decode(url));
    }

    @Override
    public InputStream getFileContentStream(String url) {
        Base64.Decoder decoder = Base64.getDecoder();

        return new ByteArrayInputStream(decoder.decode(url));
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
