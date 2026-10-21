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

package com.bytechef.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Checks that every {@code bytechef …} command the app template's {@code AGENTS.md} names exists as a
 * {@code @Command(name = …)} under {@code cli/commands}, and that every {@code --option} written in the same span is an
 * {@code @Option(longName = …)} of that command. The template is read by coding agents that run those commands as
 * written, and the template's own Node check cannot catch the drift: its CI job has no CLI binary.
 * <p>
 * A command path is the run of lowercase words after {@code bytechef } in an inline code span or a fenced code line,
 * stopping at the first token that is not a command word ({@code --option}, {@code <placeholder>}, {@code …}). The
 * options of the span are the {@code --long-option} tokens after it; a fenced line ending in a backslash continues on
 * the next line.
 *
 * @author Ivica Cardic
 */
class AppTemplateCliCommandDriftTest {

    private static final String AGENTS_FILE = "sdks/frontend/automation/app/create/template/AGENTS.md";

    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

    private static final Pattern COMMAND_WORD_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    private static final Pattern LONG_OPTION_PATTERN = Pattern.compile("(?<![\\w-])--([a-z][a-z0-9-]*)");

    private static final String COMMAND_PREFIX = "bytechef ";

    private static final List<String> LOOP_COMMANDS = List.of(
        "configure", "automation app create", "automation connection list", "automation app bind connection",
        "automation app bind workflow", "automation app status", "automation app key rotate");

    record CommandInvocation(String commandPath, List<String> options) {
    }

    static List<String> commandPaths(String markdown) {
        return commandInvocations(markdown).stream()
            .map(CommandInvocation::commandPath)
            .toList();
    }

    static List<CommandInvocation> commandInvocations(String markdown) {
        List<String> candidates = new ArrayList<>();
        StringBuilder prose = new StringBuilder();
        boolean insideFence = false;
        boolean continuesPreviousLine = false;

        for (String line : markdown.split("\n", -1)) {
            String strippedLine = line.strip();

            if (strippedLine.startsWith("```")) {
                insideFence = !insideFence;
                continuesPreviousLine = false;

                continue;
            }

            if (insideFence) {
                boolean continues = strippedLine.endsWith("\\");
                String content = continues ? strippedLine.substring(0, strippedLine.length() - 1)
                    .strip() : strippedLine;

                if (continuesPreviousLine) {
                    int lastIndex = candidates.size() - 1;

                    candidates.set(lastIndex, candidates.get(lastIndex) + " " + content);
                } else {
                    candidates.add(content);
                }

                continuesPreviousLine = continues;
            } else {
                prose.append(line)
                    .append(' ');
            }
        }

        Matcher matcher = INLINE_CODE_PATTERN.matcher(prose);

        while (matcher.find()) {
            String inlineCode = matcher.group(1);

            candidates.add(inlineCode.strip());
        }

        List<CommandInvocation> commandInvocations = new ArrayList<>();

        for (String candidate : candidates) {
            if (!candidate.startsWith(COMMAND_PREFIX)) {
                continue;
            }

            String arguments = candidate.substring(COMMAND_PREFIX.length())
                .strip();
            List<String> commandWords = new ArrayList<>();

            for (String token : arguments.split("\\s+")) {
                if (!COMMAND_WORD_PATTERN.matcher(token)
                    .matches()) {

                    break;
                }

                commandWords.add(token);
            }

            if (commandWords.isEmpty()) {
                continue;
            }

            List<String> options = new ArrayList<>();
            Matcher optionMatcher = LONG_OPTION_PATTERN.matcher(arguments);

            while (optionMatcher.find()) {
                options.add(optionMatcher.group(1));
            }

            commandInvocations.add(new CommandInvocation(String.join(" ", commandWords), options));
        }

        return commandInvocations;
    }

    private static List<String> agentsCommandPaths() throws IOException {
        Path agentsFile = CliCommandInventory.repositoryRoot()
            .resolve(AGENTS_FILE);

        return commandPaths(Files.readString(agentsFile));
    }

    @Test
    void testCommandPathsStopAtTheFirstNonCommandToken() {
        assertEquals(
            List.of("automation app bind workflow"),
            commandPaths("Run `bytechef automation app bind workflow --workflow-key … --deployment-id …`."));
        assertEquals(
            List.of("automation app unbind connection"),
            commandPaths("```bash\nbytechef automation app unbind connection --binding-key crm\n```\n"));
        assertEquals(
            List.of("automation app create"),
            commandPaths("Run `bytechef automation app\n   create --name my-app` once."));
        assertEquals(
            List.of(), commandPaths("`@bytechef/app`, `npm run -s bytechef:status`, `context.bytechef`, `bytechef`"));
    }

    static List<String> driftFailures(String markdown, Map<String, Set<String>> commandOptions) {
        List<String> failures = new ArrayList<>();

        for (CommandInvocation commandInvocation : commandInvocations(markdown)) {
            String commandPath = commandInvocation.commandPath();
            Set<String> options = commandOptions.get(commandPath);

            if (options == null) {
                failures.add(
                    AGENTS_FILE + " names `bytechef " + commandPath + "`, but no @Command(name = \"" + commandPath
                        + "\") exists under cli/commands");

                continue;
            }

            for (String option : commandInvocation.options()) {
                if (!options.contains(option)) {
                    failures.add(
                        AGENTS_FILE + " names `bytechef " + commandPath + " --" + option + "`, but that command has "
                            + "no @Option(longName = \"" + option + "\"); it accepts " + sorted(options));
                }
            }
        }

        return failures;
    }

    private static List<String> sorted(Set<String> options) {
        return options.stream()
            .sorted()
            .map(option -> "--" + option)
            .toList();
    }

    @Test
    void testDriftFailuresNameAnOptionTheCommandDoesNotHave() {
        Map<String, Set<String>> commandOptions = Map.of("automation app create", Set.of("name", "profile"));

        assertEquals(
            List.of(),
            driftFailures("Run `bytechef automation app create --name my-app --profile dev`.", commandOptions));

        List<String> failures = driftFailures(
            "Run `bytechef automation app create --name my-app --no-such-option x`.", commandOptions);

        assertEquals(1, failures.size(), failures.toString());
        assertTrue(failures.get(0)
            .contains("--no-such-option"), failures.toString());
    }

    @Test
    void testCommandInvocationsCollectTheLongOptionsOfEachSpan() {
        assertEquals(
            List.of(new CommandInvocation("automation app bind workflow", List.of("workflow-key", "deployment-id"))),
            commandInvocations("Run `bytechef automation app bind workflow --workflow-key … --deployment-id …`."));
        assertEquals(
            List.of(new CommandInvocation("configure", List.of("host", "token"))),
            commandInvocations("```bash\nbytechef configure --host https://example.com \\\n  --token=<key>\n```\n"));
        assertEquals(
            List.of(new CommandInvocation("automation app status", List.of())),
            commandInvocations("`bytechef automation app status` reads `npm run -s bytechef:status --silent`."));
    }

    @Test
    void testEveryCommandAgentsMdNamesExists() throws IOException {
        Map<String, Set<String>> commandOptions =
            CliCommandInventory.commandOptions(CliCommandInventory.repositoryRoot());

        assertTrue(commandOptions.containsKey("automation app create"), "The CLI command inventory found nothing");

        List<String> failures = driftFailures(
            Files.readString(CliCommandInventory.repositoryRoot()
                .resolve(AGENTS_FILE)),
            commandOptions);

        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void testAgentsMdNamesTheAppLoopCommands() throws IOException {
        Set<String> namedCommandPaths = new HashSet<>(agentsCommandPaths());

        for (String loopCommand : LOOP_COMMANDS) {
            assertTrue(
                namedCommandPaths.contains(loopCommand),
                AGENTS_FILE + " no longer names `bytechef " + loopCommand + "`");
        }
    }
}
