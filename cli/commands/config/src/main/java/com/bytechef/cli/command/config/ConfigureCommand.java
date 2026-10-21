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

import com.bytechef.cli.core.config.ConfigFile;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Profile;
import com.bytechef.cli.core.error.CliException;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Stores host, token, environment and default workspace in a named profile.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class ConfigureCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");

    @Command(name = "configure", description = "Store host, token and environment in a named profile.")
    public void configure(
        @Option(longName = "profile", description = "profile name", defaultValue = "default") String profile,
        @Option(longName = "host", description = "ByteChef host URL", required = true) String host,
        @Option(longName = "token", description = "public API token", required = true) String token,
        @Option(
            longName = "environment", description = "DEVELOPMENT, STAGING or PRODUCTION",
            defaultValue = "PRODUCTION") String environment,
        @Option(longName = "workspace-id", description = "default workspace id") Long workspaceId) {

        try {
            ConfigFile.write(
                configPath, profile, new Profile(host, token, Environment.valueOf(environment), workspaceId));
        } catch (IOException e) {
            throw new CliException(1, "Failed to write config: " + e.getMessage());
        }

        System.out.println("Saved profile '" + profile + "'.");
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }
}
