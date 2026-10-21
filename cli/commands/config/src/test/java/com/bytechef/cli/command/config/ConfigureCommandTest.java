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

package com.bytechef.cli.command.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.cli.core.config.ConfigFile;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Profile;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ConfigureCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testConfigureWritesProfile() throws Exception {
        Path configFile = tempDir.resolve("config");

        ConfigureCommand command = new ConfigureCommand();

        command.setConfigPath(configFile);

        command.configure("default", "https://app.bytechef.io", "btc_x", "STAGING", 3L);

        Map<String, Profile> profiles = ConfigFile.read(configFile);

        Profile profile = profiles.get("default");

        assertEquals("https://app.bytechef.io", profile.host());
        assertEquals(Environment.STAGING, profile.environment());
        assertEquals(3L, profile.workspaceId());
    }
}
