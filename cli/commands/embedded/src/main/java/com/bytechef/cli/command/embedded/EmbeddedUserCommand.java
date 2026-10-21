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

import com.bytechef.cli.client.embeddedconfiguration.ApiException;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for a connected user's account and connections in the embedded product.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class EmbeddedUserCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "embedded user update", description = "Update a connected user from a JSON body.")
    @SuppressWarnings("unchecked")
    public void userUpdate(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        Map<String, Object> body = EmbeddedSupport.readBody(data, Map.class);

        try {
            EmbeddedConfigurationClientFactory.connectedUserApi(config)
                .updateConnectedUser(externalUserId, null, body);

            new OutputRenderer(System.out).message("Updated connected user " + externalUserId + ".");
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(name = "embedded connection list", description = "List a connected user's connections for a component.")
    public void connectionList(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "component-name", required = true) String componentName,
        @Option(longName = "connection-ids") String connectionIds,
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).render(
                EmbeddedConfigurationClientFactory.connectionApi(config)
                    .getConnections(externalUserId, componentName, null, CliArgs.splitCsvLong(connectionIds)),
                output);
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment) {
        return EmbeddedSupport.resolve(configPath, environmentVariables, profile, host, token, environment);
    }
}
