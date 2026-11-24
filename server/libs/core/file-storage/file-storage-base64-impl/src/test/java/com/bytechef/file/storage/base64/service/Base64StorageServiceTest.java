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
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class Base64StorageServiceTest {

    private static final String DATA = "data";
    private static final String STRING = "test string";

    private static final Base64FileStorageService base64StorageService = new Base64FileStorageService();

    @Test
    public void testOpenInputStream() throws IOException {
        InputStream inputStream = base64StorageService.getInputStream(
            DATA, new FileEntry("file.text", "base64://" + EncodingUtils.base64EncodeToString(STRING)));

        Assertions.assertThat(
            new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo(STRING);
    }

    @Test
    public void testRead() {
        Assertions.assertThat(
            base64StorageService.readFileToString(
                DATA, new FileEntry("file.text", "base64://" + EncodingUtils.base64EncodeToString(STRING))))
            .isEqualTo(STRING);
    }

    @Test
    public void testWrite() {
        FileEntry fileEntry = base64StorageService.storeFileContent(
            DATA, "fileEntry", new ByteArrayInputStream(STRING.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertThat(fileEntry.getUrl())
            .isEqualTo("base64://" + EncodingUtils.base64EncodeToString(STRING));
    }
}
