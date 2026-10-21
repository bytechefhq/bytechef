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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.cli.core.error.CliException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ProfileResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void testFileValuesUsedWhenNoOverrides() throws Exception {
        ProfileResolver resolver = new ProfileResolver(writeDefault(), Map.of());

        CliConfig config = resolver.resolve(new Overrides(null, null, null, null, null));

        assertEquals("https://file-host", config.host());
        assertEquals("file-token", config.token());
        assertEquals(1L, config.workspaceId());
    }

    @Test
    void testEnvOverridesFile() throws Exception {
        ProfileResolver resolver = new ProfileResolver(
            writeDefault(), Map.of("BYTECHEF_HOST", "https://env-host", "BYTECHEF_TOKEN", "env-token"));

        CliConfig config = resolver.resolve(new Overrides(null, null, null, null, null));

        assertEquals("https://env-host", config.host());
        assertEquals("env-token", config.token());
    }

    @Test
    void testFlagOverridesEnvAndFile() throws Exception {
        ProfileResolver resolver = new ProfileResolver(writeDefault(), Map.of("BYTECHEF_HOST", "https://env-host"));

        CliConfig config = resolver.resolve(new Overrides("https://flag-host", null, null, 9L, null));

        assertEquals("https://flag-host", config.host());
        assertEquals(9L, config.workspaceId());
    }

    @Test
    void testMissingTokenThrowsExitCode4() throws Exception {
        ProfileResolver resolver = new ProfileResolver(tempDir.resolve("none"), Map.of());

        CliException exception = assertThrows(
            CliException.class, () -> resolver.resolve(new Overrides("https://h", null, null, null, null)));

        assertEquals(4, exception.exitCode());
    }

    private Path writeDefault() throws Exception {
        Path file = tempDir.resolve("config");

        ConfigFile.write(file, "default", new Profile("https://file-host", "file-token", Environment.PRODUCTION, 1L));

        return file;
    }
}
