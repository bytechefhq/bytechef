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

package com.bytechef.platform.configuration.workflow.diff;

import com.bytechef.commons.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Ivica Cardic
 */
public class SensitiveValueMasker {

    public static final String MASK = "••••••";

    private static final int DIGEST_HEX_LENGTH = 16;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "secret", "clientsecret", "token", "accesstoken", "refreshtoken", "apikey", "apitoken",
        "authorization", "privatekey", "credentials", "passphrase");

    private SensitiveValueMasker() {
    }

    public static boolean isSensitiveKey(String key) {
        String lowerCaseKey = key.toLowerCase();
        String withoutUnderscores = lowerCaseKey.replace("_", "");
        String normalizedKey = withoutUnderscores.replace("-", "");

        return SENSITIVE_KEYS.contains(normalizedKey);
    }

    public static Map<String, Object> mask(Map<String, Object> map) {
        Map<String, Object> maskedMap = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSensitiveKey(key) && value != null && !isExpression(value)) {
                maskedMap.put(key, maskValue(value));
            } else {
                maskedMap.put(key, maskNested(value));
            }
        }

        return maskedMap;
    }

    @SuppressWarnings("unchecked")
    private static Object maskNested(Object value) {
        if (value instanceof Map<?, ?> map) {
            return mask((Map<String, Object>) map);
        }

        if (value instanceof List<?> list) {
            List<Object> maskedList = new ArrayList<>();

            for (Object item : list) {
                maskedList.add(maskNested(item));
            }

            return maskedList;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static Object plainMasks(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> plainMap = new TreeMap<>();

            for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                plainMap.put(entry.getKey(), plainMasks(entry.getValue()));
            }

            return plainMap;
        }

        if (value instanceof List<?> list) {
            List<Object> plainList = new ArrayList<>();

            for (Object item : list) {
                plainList.add(plainMasks(item));
            }

            return plainList;
        }

        if (value instanceof String string && string.startsWith(MASK)) {
            return MASK;
        }

        return value;
    }

    private static boolean isExpression(Object value) {
        return value instanceof String string && string.startsWith("${");
    }

    private static String maskValue(Object value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            String json = JsonUtils.write(value);

            byte[] digest = messageDigest.digest(json.getBytes(StandardCharsets.UTF_8));

            String hex = HexFormat.of()
                .formatHex(digest);

            return MASK + ":" + hex.substring(0, DIGEST_HEX_LENGTH);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }
}
