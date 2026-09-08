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

package com.bytechef.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class RememberMeKeyTest {

    @TempDir
    private Path bytechefPath;

    @Test
    void testConfiguredKeyIsUsedAsIs() {
        assertEquals("configured", RememberMeKey.resolveKey("configured", bytechefPath));
    }

    @Test
    void testConfiguredKeyIsNotPersisted() {
        RememberMeKey.resolveKey("configured", bytechefPath);

        assertFalse(Files.exists(bytechefPath.resolve("remember-me-key")));
    }

    @Test
    void testKeyIsGeneratedAndPersistedWhenNotConfigured() throws IOException {
        String key = RememberMeKey.resolveKey(null, bytechefPath);

        assertFalse(key.isBlank());
        assertEquals(key, Files.readString(bytechefPath.resolve("remember-me-key")));
    }

    @Test
    void testGeneratedKeyIsReusedOnTheNextStart() {
        String key = RememberMeKey.resolveKey(null, bytechefPath);

        assertEquals(key, RememberMeKey.resolveKey(null, bytechefPath));
    }

    @Test
    void testBlankConfiguredKeyFallsBackToAGeneratedOne() {
        String key = RememberMeKey.resolveKey("   ", bytechefPath);

        assertFalse(key.isBlank());
        assertTrue(Files.exists(bytechefPath.resolve("remember-me-key")));
    }

    @Test
    void testPersistedKeyIsTrimmedOfSurroundingWhitespace() throws IOException {
        Path keyPath = bytechefPath.resolve("remember-me-key");

        Files.writeString(keyPath, "  persisted-key\n");

        assertEquals("persisted-key", RememberMeKey.resolveKey(null, bytechefPath));
    }

    @Test
    void testBlankPersistedKeyIsReplacedByAGeneratedOne() throws IOException {
        Path keyPath = bytechefPath.resolve("remember-me-key");

        Files.writeString(keyPath, "   \n");

        String key = RememberMeKey.resolveKey(null, bytechefPath);

        assertFalse(key.isBlank());
        assertEquals(key, Files.readString(keyPath));
    }

    @Test
    void testGeneratedKeysDifferBetweenHomes(@TempDir Path otherBytechefPath) {
        assertNotEquals(
            RememberMeKey.resolveKey(null, bytechefPath), RememberMeKey.resolveKey(null, otherBytechefPath));
    }
}
