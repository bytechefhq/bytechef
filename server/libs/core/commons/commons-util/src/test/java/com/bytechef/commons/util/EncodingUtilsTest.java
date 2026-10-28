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

package com.bytechef.commons.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncodingUtilsTest {

    @Test
    public void testBase64EncodeToString() {
        String result = EncodingUtils.base64EncodeToString("test", "value");
        Assertions.assertThat(result)
            .isNotNull();

        Assertions.assertThat(result)
            .isEqualTo("dGVzdHZhbHVl");

        Assertions.assertThat(EncodingUtils.base64DecodeToString(result))
            .isEqualTo("testvalue");

        result = EncodingUtils.base64EncodeToString("username", ":", "password");

        Assertions.assertThat(EncodingUtils.base64DecodeToString(result))
            .isEqualTo("username:password");
    }

    @Test
    public void testBase64EncodingOutputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (OutputStream outputStream = EncodingUtils.base64EncodingOutputStream(byteArrayOutputStream)) {
            outputStream.write("Hello".getBytes(StandardCharsets.UTF_8));
            outputStream.write(" World".getBytes(StandardCharsets.UTF_8));
        }

        Assertions.assertThat(byteArrayOutputStream.toString(StandardCharsets.UTF_8))
            .isEqualTo("SGVsbG8gV29ybGQ=");
    }

}
