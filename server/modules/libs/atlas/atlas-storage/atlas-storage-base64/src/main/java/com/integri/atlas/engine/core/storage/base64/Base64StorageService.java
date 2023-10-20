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

package com.integri.atlas.engine.core.storage.base64;

import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.storage.exception.StorageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * @author Ivica Cardic
 */
public class Base64StorageService implements StorageService {

    @Override
    public String write(String bucketName, String data) throws StorageException {
        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(data.getBytes());
    }

    @Override
    public String write(String bucketName, InputStream inputStream) {
        Base64.Encoder encoder = Base64.getEncoder();

        try {
            return encoder.encodeToString(toByteArray(inputStream));
        } catch (IOException ioe) {
            throw new StorageException("Failed to store file", ioe);
        }
    }

    private byte[] toByteArray(InputStream inputStream) throws StorageException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[4];

        while ((nRead = inputStream.readNBytes(data, 0, data.length)) != 0) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    @Override
    public InputStream openInputStream(String bucketName, String fileName) {
        Base64.Decoder decoder = Base64.getDecoder();

        return new ByteArrayInputStream(decoder.decode(fileName));
    }

    @Override
    public String read(String bucketName, String fileName) throws StorageException {
        Base64.Decoder decoder = Base64.getDecoder();

        return new String(decoder.decode(fileName));
    }
}
