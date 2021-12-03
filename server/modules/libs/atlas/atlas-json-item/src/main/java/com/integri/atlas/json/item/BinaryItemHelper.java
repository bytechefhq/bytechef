/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.json.item;

import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.io.InputStream;
import org.apache.commons.io.FilenameUtils;

/**
 * @author Ivica Cardic
 */
public class BinaryItemHelper {

    private static final String BUCKET_NAME = "binary";

    private final StorageService storageService;

    public BinaryItemHelper(StorageService storageService) {
        this.storageService = storageService;
    }

    public BinaryItem writeBinaryData(String fileName, InputStream inputStream) {
        return BinaryItem.of(
            fileName,
            storageService.write(
                BUCKET_NAME,
                UUIDGenerator.generate() + "_" + FilenameUtils.getName(fileName),
                inputStream
            )
        );
    }

    public InputStream openDataInputStream(BinaryItem binaryItem) {
        return storageService.openInputStream(BUCKET_NAME, binaryItem.getData());
    }
}
