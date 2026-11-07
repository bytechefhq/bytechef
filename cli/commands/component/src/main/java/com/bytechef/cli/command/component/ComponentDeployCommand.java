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

package com.bytechef.cli.command.component;

import com.bytechef.cli.client.platformcustomcomponent.ApiException;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class ComponentDeployCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The component file path is supplied by the CLI user on purpose.")
    @Command(name = "component deploy", description = "Deploy a custom component to a ByteChef instance.")
    public void componentDeploy(
        @Option(longName = "file", required = true) String file,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        File componentFile = new File(file);

        if (!componentFile.exists()) {
            throw new CliException(1, "Component file not found: " + file);
        }

        CliConfig config = resolve(profile, host, token, environment);

        try {
            PlatformCustomComponentClientFactory.customComponentApi(config)
                .deployCustomComponent(componentFile);

            System.out.println("Custom component deployed.");
        } catch (ApiException e) {
            throw PlatformCustomComponentClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment) {
        return ComponentSupport.resolve(configPath, environmentVariables, profile, host, token, environment);
    }
}
