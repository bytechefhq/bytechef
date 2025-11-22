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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Igor Beslic
 * @author Ivica Cardic
 */
public class ObfuscateUtils {

    /**
     * Obfuscates original string value. Argument maxLength value may be used to influence the length of new obfuscated
     * string. Argument visibleLength may be used to leave the portion unobfuscated. It can be used as a hint.
     *
     * @param string        value to obfuscate
     * @param maxLength     maximum length of obfuscated value
     * @param visibleLength maximum number of unobfuscated characters
     * @return obfuscated value
     */
    public static String obfuscate(String string, int maxLength, int visibleLength) {
        if (!StringUtils.isEmpty(string)) {
            if (string.length() > maxLength) {
                string = string.substring(string.length() - maxLength);
            }

            string =
                ".".repeat(maxLength) + string.substring(string.length() - Math.min(string.length(), visibleLength));
        }

        return string;
    }

    /**
     * Obfuscates values stored in the map. Arguments maxLength and visibleLength operate as described in
     * {@link #obfuscate(String, int, int)} method.
     *
     * @param map           the map with values to obfuscate
     * @param maxLength     maximum length of obfuscated value
     * @param visibleLength maximum number of unobfuscated characters
     * @return new hash map with obfuscated values
     */
    public static Map<String, Object> toObfuscatedMap(Map<String, Object> map, int maxLength, int visibleLength) {

        Map<String, Object> obfuscatedMap = new HashMap<>();

        map.forEach(
            (key, valueIn) -> {
                int secureVisibleLength = getSecureVisibleLength(String.valueOf(key), visibleLength);

                obfuscatedMap.computeIfAbsent(
                    key, k -> obfuscate(String.valueOf(valueIn), maxLength, secureVisibleLength));

            });

        return obfuscatedMap;
    }

    /**
     * Returns secure number of the visible characters depending on the nature of the key. Keys like password point to
     * very sensitive value. In that case visible length is always 0.
     *
     * @param keyName               name of the key
     * @param expectedVisibleLength the visible length requested by the caller
     * @return secure visible length
     */
    private static int getSecureVisibleLength(String keyName, int expectedVisibleLength) {
        if (SENSITIVE_KEYS.contains(String.valueOf(keyName))) {
            return 0;
        }

        return expectedVisibleLength;
    }

    private static final String SENSITIVE_KEYS = "password";

}
