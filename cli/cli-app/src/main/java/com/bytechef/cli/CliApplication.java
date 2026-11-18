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
            DefaultCommandParser commandParser = new DefaultCommandParser(commandRegistry);
            NonInteractiveShellRunner runner = new NonInteractiveShellRunner(commandParser, commandRegistry);

            runner.run(quoteArguments(applicationArguments.getSourceArgs()));
        };
    }

    static String[] quoteArguments(String... arguments) {
        String[] quotedArguments = new String[arguments.length];

        for (int index = 0; index < arguments.length; index++) {
            quotedArguments[index] = quoteArgument(arguments[index]);
        }

        return quotedArguments;
    }

    private static String quoteArgument(String argument) {
        if (argument.startsWith("-")) {
            int equalsIndex = argument.indexOf('=');

            if (equalsIndex > 0) {
                String value = argument.substring(equalsIndex + 1);

                return needsQuotes(value) ? argument.substring(0, equalsIndex + 1) + quote(value) : argument;
            }
        }

        return needsQuotes(argument) ? quote(argument) : argument;
    }

    private static boolean needsQuotes(String value) {
        if (value.isEmpty()) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }

        return false;
    }

    private static String quote(String value) {
        String escapedValue = value.replace("\\", "\\\\");

        escapedValue = escapedValue.replace("\"", "\\\"");

        return "\"" + escapedValue + "\"";
    }
}
