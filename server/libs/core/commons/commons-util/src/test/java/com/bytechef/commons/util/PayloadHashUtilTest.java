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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PayloadHashUtilTest {

    @Test
    void testHashIsStableAcrossKeyOrder() {
        String hash1 = PayloadHashUtil.hash(Map.of("a", 1, "b", 2));
        String hash2 = PayloadHashUtil.hash(Map.of("b", 2, "a", 1));

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(16);
    }

    @Test
    void testHashChangesWhenValueChanges() {
        String hash1 = PayloadHashUtil.hash(Map.of("name", "Alice"));
        String hash2 = PayloadHashUtil.hash(Map.of("name", "Bob"));

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void testHashIsStableAcrossNestedKeyOrder() {
        String hash1 = PayloadHashUtil.hash(Map.of(
            "name", "Acme",
            "address", Map.of("city", "NYC", "zip", "10001")));
        String hash2 = PayloadHashUtil.hash(Map.of(
            "address", Map.of("zip", "10001", "city", "NYC"),
            "name", "Acme"));

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testHashIsHexString() {
        String hash = PayloadHashUtil.hash(Map.of("x", 1));

        assertThat(hash).matches("^[0-9a-f]{16}$");
    }
}
