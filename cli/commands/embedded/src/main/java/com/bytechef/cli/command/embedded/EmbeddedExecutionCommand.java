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

import com.bytechef.cli.client.embeddedexecution.ApiException;
import com.bytechef.cli.client.embeddedexecution.model.ExecuteActionRequestModel;
import com.bytechef.cli.client.embeddedexecution.model.ExecuteToolRequestModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for a connected user's tools and actions in the embedded product.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class EmbeddedExecutionCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "embedded tool list", description = "List the tools available to a connected user.")
    public void toolList(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "categories") String categories,
        @Option(longName = "components") String components,
        @Option(longName = "tools") String tools,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).renderJson(
                EmbeddedExecutionClientFactory.toolApi(config)
                    .getTools(
                        externalUserId, null, CliArgs.splitCsv(categories), CliArgs.splitCsv(components),
                        CliArgs.splitCsv(tools)));
        } catch (ApiException e) {
            throw EmbeddedExecutionClientFactory.toCliException(e);
        }
    }

    @Command(name = "embedded tool execute", description = "Execute a tool for a connected user from a JSON body.")
    public void toolExecute(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        ExecuteToolRequestModel body = EmbeddedSupport.readBody(data, ExecuteToolRequestModel.class);

        try {
            new OutputRenderer(System.out).renderJson(
                EmbeddedExecutionClientFactory.toolApi(config)
                    .executeTool(externalUserId, body, null, instanceId));
        } catch (ApiException e) {
            throw EmbeddedExecutionClientFactory.toCliException(e);
        }
    }

    @Command(name = "embedded action execute", description = "Execute a component action for a connected user.")
    public void actionExecute(
        @Option(longName = "external-user-id", required = true) String externalUserId,
        @Option(longName = "component-name", required = true) String componentName,
        @Option(longName = "component-version", required = true) Integer componentVersion,
        @Option(longName = "action-name", required = true) String actionName,
        @Option(longName = "data", required = true) String data,
        @Option(longName = "instance-id") Long instanceId,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        ExecuteActionRequestModel body = EmbeddedSupport.readBody(data, ExecuteActionRequestModel.class);

        try {
            new OutputRenderer(System.out).renderJson(
                EmbeddedExecutionClientFactory.actionApi(config)
                    .executeAction(externalUserId, componentName, componentVersion, actionName, body, null,
                        instanceId));
        } catch (ApiException e) {
            throw EmbeddedExecutionClientFactory.toCliException(e);
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
