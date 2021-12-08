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

    private static final Base64StorageService base64StorageService = new Base64StorageService();

    @Test
    public void testOpenInputStream() throws IOException {
        InputStream inputStream = base64StorageService.openInputStream(
            "binary",
            Base64.getEncoder().encodeToString(string.getBytes())
        );

        Assertions.assertThat(new String(inputStream.readAllBytes())).isEqualTo(string);
    }

    @Test
    public void testRead() {
        Assertions
            .assertThat(base64StorageService.read("binary", Base64.getEncoder().encodeToString(string.getBytes())))
            .isEqualTo(string);
    }

    @Test
    public void testWrite() {
        Assertions
            .assertThat(base64StorageService.write("binary", new ByteArrayInputStream(string.getBytes())))
            .isEqualTo(Base64.getEncoder().encodeToString(string.getBytes()));
    }
}
