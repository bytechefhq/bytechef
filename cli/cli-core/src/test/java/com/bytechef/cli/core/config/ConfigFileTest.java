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

package com.bytechef.cli.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ConfigFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteThenReadRoundTrips() throws Exception {
        Path file = tempDir.resolve("config");

        ConfigFile.write(
            file, "default", new Profile("https://app.bytechef.io", "btc_x", Environment.PRODUCTION, 1L));
        ConfigFile.write(
            file, "staging", new Profile("https://staging.bytechef.io", "btc_y", Environment.STAGING, 4L));

        Map<String, Profile> profiles = ConfigFile.read(file);

        assertEquals("https://app.bytechef.io", profiles.get("default")
            .host());
        assertEquals(Environment.STAGING, profiles.get("staging")
            .environment());
        assertEquals(4L, profiles.get("staging")
            .workspaceId());
    }

    @Test
    void testWriteSetsOwnerOnlyPermissions() throws Exception {
        Path file = tempDir.resolve("config");

        ConfigFile.write(
            file, "default", new Profile("https://app.bytechef.io", "btc_x", Environment.PRODUCTION, 1L));

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);

        assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
        assertEquals(2, permissions.size(), "only owner read/write expected");
    }

    @Test
    void testReadMissingFileReturnsEmpty() throws Exception {
        assertTrue(
            ConfigFile.read(tempDir.resolve("nope"))
                .isEmpty());
    }
}
