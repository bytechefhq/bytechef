/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.example.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.example.constant.CryptoHelperConstants.ALPHANUMERIC_CHARACTERS;
import static com.bytechef.component.example.constant.CryptoHelperConstants.SYMBOL_CHARACTERS;

import com.bytechef.component.definition.Option;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperUtilTest {

    @Test
    void getHashAlgorithmOptionsTest() {
        List<Option<String>> expected = getHashOptionsList();

        List<Option<String>> refactor = CryptoHelperUtil.getHashAlgorithmOptions();

        Assertions.assertEquals(expected, refactor);
    }

    @Test
    void getHmacAlgorithmOptionsTest() {
        List<Option<String>> expected = getHmacOptionsList();

        List<Option<String>> refactor = CryptoHelperUtil.getHmacAlgorithmOptions();

        Assertions.assertEquals(expected, refactor);
    }

    @Test
    void getCharacterSetOptionsTest() {
        List<Option<String>> expected = getCharSetOptionsList();

        List<Option<String>> result = CryptoHelperUtil.getCharacterSetOptions();

        Assertions.assertEquals(expected, result);
    }

    @Test
    void bytesToHexTest() {
        byte[] binaryInput = "test".getBytes(StandardCharsets.UTF_8);

        String expected = "74657374";

        String result = CryptoHelperUtil.bytesToHex(binaryInput);

        Assertions.assertEquals(expected, result);
    }

    private List<Option<String>> getHashOptionsList() {
        return List.of(
            option("MD5", "MD5"),
            option("SHA-1", "SHA-1"),
            option("SHA-256", "SHA-256"));
    }

    private List<Option<String>> getHmacOptionsList() {
        return List.of(
            option("MD5", "HmacMD5"),
            option("SHA-1", "HmacSHA1"),
            option("SHA-256", "HmacSHA256"));
    }

    private List<Option<String>> getCharSetOptionsList() {
        return List.of(
            option("Alphanumeric", ALPHANUMERIC_CHARACTERS),
            option("Alphanumeric + Symbols", ALPHANUMERIC_CHARACTERS + SYMBOL_CHARACTERS));
    }
}
