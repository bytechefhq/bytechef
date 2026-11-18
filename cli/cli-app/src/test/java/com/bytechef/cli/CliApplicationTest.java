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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.DefaultCommandParser;
import org.springframework.shell.core.command.ParsedInput;

/**
 * @author Ivica Cardic
 */
class CliApplicationTest {
    @Test
    void testValuesWithWhitespaceSurviveTheShellParser() {
        ParsedInput parsedInput = parse(
            "app", "--name", "Airtable Dashboard", "--description=Records from a table", "--empty", "",
            "--quote", "say \"hi\" now", "--path", "C:\\My Files", "--offset", "-5 days");

        assertEquals("Airtable Dashboard", getOptionValue(parsedInput, "name"));
        assertEquals("Records from a table", getOptionValue(parsedInput, "description"));
        assertEquals("", getOptionValue(parsedInput, "empty"));
        assertEquals("say \"hi\" now", getOptionValue(parsedInput, "quote"));
        assertEquals("C:\\My Files", getOptionValue(parsedInput, "path"));
        assertEquals("-5 days", getOptionValue(parsedInput, "offset"));
    }

    @Test
    void testValuesWithoutWhitespaceAreLeftAsTheyAre() {
        assertArrayEquals(
            new String[] {
                "app", "--name", "dashboard", "--setting=key=value", "-x"
            },
            CliApplication.quoteArguments("app", "--name", "dashboard", "--setting=key=value", "-x"));
    }

    private static String getOptionValue(ParsedInput parsedInput, String longName) {
        List<CommandOption> options = parsedInput.options();

        return options.stream()
            .filter(option -> longName.equals(option.longName()))
            .map(CommandOption::value)
            .findFirst()
            .orElseThrow();
    }

    private static ParsedInput parse(String... arguments) {
        DefaultCommandParser commandParser = new DefaultCommandParser(new CommandRegistry());

        return commandParser.parse(String.join(" ", CliApplication.quoteArguments(arguments)));
    }
}
