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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class CryptoHelperUtil {

    public static List<Option<String>> getHashAlgorithmOptions() {

        return Arrays.stream(CryptographicAlgorithmsEnum.values())
            .map(algorithm -> option(algorithm.getLabel(), algorithm.getLabel()))
            .collect(Collectors.toList());
    }

    public static List<Option<String>> getHmacAlgorithmOptions() {

        return Arrays.stream(CryptographicAlgorithmsEnum.values())
            .map(algorithm -> option(algorithm.getLabel(), algorithm.getHmacLabel()))
            .collect(Collectors.toList());
    }

    public static List<Option<String>> getCharacterSetOptions() {

        return List.of(
            option("Alphanumeric", ALPHANUMERIC_CHARACTERS),
            option("Alphanumeric + Symbols", ALPHANUMERIC_CHARACTERS + SYMBOL_CHARACTERS));
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }
}
