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
import com.bytechef.cli.client.embeddedconfiguration.model.AutomationProjectCodeWorkflowDeployResultModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.output.OutputRenderer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class EmbeddedCodeWorkflowCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The project file path is supplied by the CLI user on purpose.")
    @Command(name = "embedded code-workflow deploy", description = "Deploy a code workflow into the embedded catalog.")
    public void codeWorkflowDeploy(
        @Option(longName = "file", required = true) String file,
        @Option(
            longName = "release",
            description = "also point the configuration of the profile environment at the published version",
            defaultValue = "false") boolean release,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        File projectFile = new File(file);

        if (!projectFile.exists()) {
            throw new CliException(1, "Project file not found: " + file);
        }

        CliConfig config = resolve(profile, host, token, environment);

        try {
            AutomationProjectCodeWorkflowDeployResultModel result =
                EmbeddedConfigurationClientFactory.automationProjectCodeWorkflowApi(config)
                    .deployAutomationProjectCodeWorkflow(null, projectFile, release);

            OutputRenderer outputRenderer = new OutputRenderer(System.out);

            outputRenderer.message("Project deployed.");

            List<String> warnings = result.getWarnings();

            if (warnings != null) {
                for (String warning : warnings) {
                    outputRenderer.message("WARNING: " + warning);
                }
            }
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    @Command(
        name = "embedded code-workflow list",
        description = "List catalog projects in the embedded automation bridge.")
    public void codeWorkflowList(
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).render(
                EmbeddedConfigurationClientFactory.automationProjectCodeWorkflowApi(config)
                    .listAutomationProjectCodeWorkflows(),
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
