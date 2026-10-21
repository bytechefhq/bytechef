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

package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.bytechef.cli.client.automationapp.model.AppWorkspaceConnectionModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.output.OutputRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Lists the workspace's connections in DEVELOPMENT for the coding-agent loop: which ones exist, whether the caller can
 * use them, and which are virtual AI-provider connections. Never parameters or credentials.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationConnectionCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();
    private PrintStream out = System.out;

    @Command(
        name = "automation connection list",
        description = "List the workspace's DEVELOPMENT connections: component, credential status, whether you can "
            + "use it, and whether it is an AI provider connection.")
    public void workspaceConnectionList(
        @Option(longName = "component-name") String componentName,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = AppCommandSupport.resolveDevelopment(
            configPath, environmentVariables, profile, host, token, environment, workspaceId);
        long resolvedWorkspaceId = AppCommandSupport.requireWorkspaceId(config);
        WorkspaceConnectionApi workspaceConnectionApi = AutomationClientFactory.workspaceConnectionApi(config);
        List<AppWorkspaceConnectionModel> connections;

        try {
            connections = workspaceConnectionApi.getWorkspaceConnections(resolvedWorkspaceId, componentName, null);
        } catch (ApiException exception) {
            throw AutomationClientFactory.toCliException(exception);
        }

        new OutputRenderer(out).render(connections, output);
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void setOutput(PrintStream out) {
        this.out = out;
    }
}
