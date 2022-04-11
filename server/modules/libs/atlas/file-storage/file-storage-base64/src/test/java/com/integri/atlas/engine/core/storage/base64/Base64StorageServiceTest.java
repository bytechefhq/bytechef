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

import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class Base64StorageServiceTest {

    private static final String string = "test string";

    private static final Base64FileStorageService base64StorageService = new Base64FileStorageService();

    @Test
    public void testOpenInputStream() throws IOException {
        InputStream inputStream = base64StorageService.getFileContentStream(
            Base64.getEncoder().encodeToString(string.getBytes())
        );

        Assertions.assertThat(new String(inputStream.readAllBytes())).isEqualTo(string);
    }

    @Test
    public void testRead() {
        Assertions
            .assertThat(base64StorageService.readFileContent(Base64.getEncoder().encodeToString(string.getBytes())))
            .isEqualTo(string);
    }

    @Test
    public void testWrite() {
        FileEntry fileEntry = base64StorageService.storeFileContent("fileEntry", new ByteArrayInputStream(string.getBytes()));

        Assertions
            .assertThat(fileEntry.getUrl())
            .isEqualTo("base64:" + Base64.getEncoder().encodeToString(string.getBytes()));
    }
}
