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

import com.bytechef.cli.client.automation.ApiException;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;
import com.bytechef.cli.client.automation.model.PageModel;
import com.bytechef.cli.client.automation.model.WorkflowExecutionStatusModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.input.CliArgs;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for listing and fetching automation workflow executions via the public API.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class AutomationExecutionCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "automation execution list", description = "List workflow executions.")
    public void list(
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "status") String status,
        @Option(longName = "project-id") Long projectId,
        @Option(longName = "project-deployment-id") Long projectDeploymentId,
        @Option(longName = "workflow-id") String workflowId,
        @Option(longName = "start-date") String startDate,
        @Option(longName = "end-date") String endDate,
        @Option(longName = "page", defaultValue = "0") Integer page,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        WorkflowExecutionApi api = AutomationClientFactory.workflowExecutionApi(config);

        PageModel pageModel;

        try {
            pageModel = api.getWorkflowExecutionsPage(
                config.workspaceId(), null,
                status == null ? null : WorkflowExecutionStatusModel.fromValue(status),
                CliArgs.parseDateTime(startDate),
                CliArgs.parseDateTime(endDate), projectId, projectDeploymentId, workflowId, page);
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }

        new OutputRenderer(System.out).render(pageModel, output);
    }

    @Command(name = "automation execution get", description = "Get a workflow execution by id.")
    public void get(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        WorkflowExecutionApi api = AutomationClientFactory.workflowExecutionApi(config);

        try {
            new OutputRenderer(System.out).renderJson(api.getWorkflowExecution(id));
        } catch (ApiException e) {
            throw AutomationClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment, Long workspaceId) {
        return new ProfileResolver(configPath, environmentVariables).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), workspaceId, profile));
    }
}
