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

package com.bytechef.cli;

import com.bytechef.cli.command.automation.AutomationExecutionCommand;
import com.bytechef.cli.command.automation.AutomationProjectCommand;
import com.bytechef.cli.command.component.ComponentCommand;
import com.bytechef.cli.command.config.ConfigureCommand;
import com.bytechef.cli.command.embedded.EmbeddedCodeWorkflowCommand;
import com.bytechef.cli.command.embedded.EmbeddedExecutionCommand;
import com.bytechef.cli.command.embedded.EmbeddedIntegrationCommand;
import com.bytechef.cli.command.embedded.EmbeddedUserCommand;
import com.bytechef.cli.command.embedded.EmbeddedWorkflowCommand;
import com.bytechef.cli.core.error.CliException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.core.NonInteractiveShellRunner;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.annotation.EnableCommand;

/**
 * @author Ivica Cardic
 */
@SpringBootApplication
@EnableCommand({
    ComponentCommand.class, ConfigureCommand.class, AutomationExecutionCommand.class, AutomationProjectCommand.class,
    EmbeddedIntegrationCommand.class, EmbeddedWorkflowCommand.class, EmbeddedExecutionCommand.class,
    EmbeddedUserCommand.class, EmbeddedCodeWorkflowCommand.class
})
public class CliApplication {

    public static void main(String... args) {
        System.exit(execute(args));
    }

    public static int execute(String... args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(CliApplication.class, args);

            context.close();

            return 0;
        } catch (Throwable throwable) {
            Throwable cause = throwable;

            while (cause != null) {
                if (cause instanceof CliException cliException) {
                    System.err.println(cliException.getMessage());

                    return cliException.exitCode();
                }

                cause = cause.getCause();
            }

            String message = throwable.getMessage();

            System.err.println(message == null ? "Command failed: " + throwable.getClass()
                .getSimpleName() : message);

            return 1;
        }
    }

    @Bean
    ApplicationRunner shellRunner(CommandRegistry commandRegistry, ApplicationArguments applicationArguments) {
        return args -> {
            DefaultCommandParser commandParser = new DefaultCommandParser(commandRegistry);
            NonInteractiveShellRunner runner = new NonInteractiveShellRunner(commandParser, commandRegistry);

            runner.run(applicationArguments.getSourceArgs());
        };
    }
}
