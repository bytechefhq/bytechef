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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.databind.ObjectMapper;

/**
 * Computes a stable, change-detection-friendly hash of a record payload.
 *
 * <p>
 * Returns the first 8 bytes of SHA-256 over the canonical (key-sorted) JSON serialization, hex-encoded (16 chars). Per
 * project memory: prefer SHA-256 first 8 bytes for deterministic long IDs.
 *
 * <p>
 * Used by sync mechanisms (Context Store, Knowledge Base Source, etc.) for change detection — re-syncing an unchanged
 * record can take the cheap fast-path of bumping {@code last_seen_at} without re-running expensive downstream pipelines
 * (chunker/embedder, index rebuild, etc.) when the payload's content hash matches the previously-stored hash.
 *
 * @author Ivica Cardic
 */
public final class PayloadHashUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PayloadHashUtil() {
    }

    public static String hash(Map<String, ?> payload) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(canonicalize(payload));
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] full = messageDigest.digest(json.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(16);

            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", full[i]));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();

            map.forEach((key, val) -> sorted.put(key.toString(), canonicalize(val)));

            return sorted;
        }

        if (value instanceof List<?> list) {
            return list.stream()
                .map(PayloadHashUtil::canonicalize)
                .toList();
        }

        return value;
    }
}
