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

package com.bytechef.component.crypto.helper.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Option;
import com.bytechef.component.helper.util.CryptoHelperUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperUtilTest {

    @Test
    void getHashAlgorithmOptionsTest() {
        List<Option<String>> expected = List.of(
            option("MD5", "MD5"),
            option("SHA-1", "SHA-1"),
            option("SHA-256", "SHA-256"));

        List<Option<String>> result = CryptoHelperUtil.getHashAlgorithmOptions();

        assertEquals(expected, result);
    }

    @Test
    void getHmacAlgorithmOptionsTest() {
        List<Option<String>> expected = List.of(
            option("MD5", "HmacMD5"),
            option("SHA-1", "HmacSHA1"),
            option("SHA-256", "HmacSHA256"));

        List<Option<String>> result = CryptoHelperUtil.getHmacAlgorithmOptions();

        assertEquals(expected, result);
    }

    @Test
    void convertBytesToHexStringTest() {
        byte[] binaryInput = "test".getBytes(StandardCharsets.UTF_8);

        String result = CryptoHelperUtil.convertBytesToHexString(binaryInput);

        assertEquals("74657374", result);
    }
}
