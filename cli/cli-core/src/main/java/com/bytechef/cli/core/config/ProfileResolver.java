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

import com.bytechef.cli.core.error.CliException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Resolves the effective {@link CliConfig} from, in decreasing precedence, per-command flags, environment variables,
 * and the selected profile in the config file.
 *
 * @author Ivica Cardic
 */
public class ProfileResolver {

    private final Path configFile;
    private final Map<String, String> environmentVariables;

    public ProfileResolver(Path configFile, Map<String, String> environmentVariables) {
        this.configFile = configFile;
        this.environmentVariables = Map.copyOf(environmentVariables);
    }

    public CliConfig resolve(Overrides overrides) {
        Profile profile = readProfile(overrides.profileName() == null ? "default" : overrides.profileName());

        String host = pick(
            overrides.host(), environmentVariables.get("BYTECHEF_HOST"), profile == null ? null : profile.host());
        String token = pick(
            overrides.token(), environmentVariables.get("BYTECHEF_TOKEN"), profile == null ? null : profile.token());

        Environment environment = resolveEnvironment(overrides, profile);
        Long workspaceId = resolveWorkspaceId(overrides, profile);

        if (host == null || host.isBlank()) {
            throw new CliException(4, "No host configured. Run 'bytechef configure' or pass --host.");
        }

        if (token == null || token.isBlank()) {
            throw new CliException(4, "No token configured. Run 'bytechef configure' or pass --token.");
        }

        return new CliConfig(host, token, environment, workspaceId);
    }

    private Environment resolveEnvironment(Overrides overrides, Profile profile) {
        if (overrides.environment() != null) {
            return overrides.environment();
        }

        if (environmentVariables.containsKey("BYTECHEF_ENVIRONMENT")) {
            return Environment.valueOf(environmentVariables.get("BYTECHEF_ENVIRONMENT"));
        }

        if (profile == null || profile.environment() == null) {
            return Environment.PRODUCTION;
        }

        return profile.environment();
    }

    private Long resolveWorkspaceId(Overrides overrides, Profile profile) {
        if (overrides.workspaceId() != null) {
            return overrides.workspaceId();
        }

        if (environmentVariables.containsKey("BYTECHEF_WORKSPACE_ID")) {
            return Long.valueOf(environmentVariables.get("BYTECHEF_WORKSPACE_ID"));
        }

        return profile == null ? null : profile.workspaceId();
    }

    private Profile readProfile(String name) {
        try {
            return ConfigFile.read(configFile)
                .get(name);
        } catch (IOException e) {
            throw new CliException(4, "Cannot read config file: " + e.getMessage());
        }
    }

    private static String pick(String flagValue, String environmentValue, String fileValue) {
        if (flagValue != null) {
            return flagValue;
        }

        if (environmentValue != null) {
            return environmentValue;
        }

        return fileValue;
    }
}
