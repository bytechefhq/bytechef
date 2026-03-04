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
 * @author Igor Beslic
 */
public class ObfuscateUtilsTest {

    @Test
    public void testObfuscate() {
        assertThat(ObfuscateUtils.obfuscate(null, 10, 3))
            .isNull();

        assertThat(ObfuscateUtils.obfuscate("", 10, 3))
            .isEmpty();

        assertThat(ObfuscateUtils.obfuscate("12345678", 10, 3))
            .isEqualTo("..........678");

        assertThat(ObfuscateUtils.obfuscate("123456789012", 10, 3))
            .isEqualTo("..........012");

        assertThat(ObfuscateUtils.obfuscate("12345678", 10, 15))
            .isEqualTo("..........12345678");
    }

    @Test
    public void testToObfuscatedMap() {
        Map<String, Object> map = Map.of(
            "authorizationUrl", "https://example.com/auth",
            "password", "secret123",
            "apiKey", "1234567890",
            "region", "eu-ireland",
            "bucketName", "project-mandragora",
            "token", "AB048-F4E5A-00234-0045AB");

        Map<String, Object> obfuscatedMap = ObfuscateUtils.toObfuscatedMap(map, 5, 2);

        assertThat(obfuscatedMap.get("authorizationUrl"))
            .isEqualTo("https://example.com/auth");

        assertThat(obfuscatedMap.get("password"))
            .isEqualTo(".....");

        assertThat(obfuscatedMap.get("apiKey"))
            .isEqualTo(".....90");

        assertThat(obfuscatedMap.get("region"))
            .isEqualTo("eu-ireland");

        assertThat(obfuscatedMap.get("bucketName"))
            .isEqualTo("project-mandragora");

        assertThat(obfuscatedMap.get("token"))
            .isEqualTo(".....AB");
    }
}
