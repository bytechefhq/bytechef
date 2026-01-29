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

import com.bytechef.cli.command.component.ComponentCommand;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.core.NonInteractiveShellRunner;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.annotation.EnableCommand;

/**
 * @author Ivica Cardic
 */
@SpringBootApplication
@EnableCommand(ComponentCommand.class)
public class CliApplication {

    public static void main(String... args) {
        SpringApplication.run(CliApplication.class, args);
    }

    @Bean
    ApplicationRunner shellRunner(CommandRegistry commandRegistry, ApplicationArguments applicationArguments) {
        return args -> {
            DefaultCommandParser commandParser = new DefaultCommandParser();
            NonInteractiveShellRunner runner = new NonInteractiveShellRunner(commandParser, commandRegistry);

            runner.run(applicationArguments.getSourceArgs());
        };
    }
}
