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

package com.integri.atlas.engine.core.binary;

import com.integri.atlas.engine.core.storage.StorageService;
import java.io.InputStream;

/**
 * @author Ivica Cardic
 */
public class BinaryHelper {

    private static final String BUCKET_NAME = "binary";

    private final StorageService storageService;

    public BinaryHelper(StorageService storageService) {
        this.storageService = storageService;
    }

    public InputStream openDataInputStream(Binary binary) {
        return storageService.openInputStream(BUCKET_NAME, binary.getData());
    }

    public String readBinaryData(Binary binary) {
        return storageService.read(BUCKET_NAME, binary.getData());
    }

    public Binary writeBinaryData(String fileName, String data) {
        return Binary.of(fileName, storageService.write(BUCKET_NAME, data));
    }

    public Binary writeBinaryData(String fileName, InputStream inputStream) {
        return Binary.of(fileName, storageService.write(BUCKET_NAME, inputStream));
    }
}
