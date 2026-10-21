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

package com.bytechef.cli.command.embedded;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Shared helpers for embedded commands: config resolution and request-body parsing.
 *
 * @author Ivica Cardic
 */
final class EmbeddedSupport {

    static final String EMBEDDED_API_PATH = "/api/embedded/v1";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EmbeddedSupport() {
    }

    static CliConfig resolve(
        Path configPath, Map<String, String> environmentVariables, String profile, String host, String token,
        String environment) {

        return new ProfileResolver(configPath, environmentVariables).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), null, profile));
    }

    /**
     * Reads a request body from {@code data}, which is either a literal JSON string or, when prefixed with {@code @},
     * the path to a JSON file.
     */
    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The data file path is supplied by the CLI user on purpose.")
    static <T> T readBody(String data, Class<T> type) {
        try {
            String json = data.startsWith("@")
                ? Files.readString(Path.of(data.substring(1)))
                : data;

            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new CliException(1, "Invalid --data JSON: " + e.getMessage());
        }
    }
}
