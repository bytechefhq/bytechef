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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The CLI's commands, read from the {@code @Command(name = "…")} literals in {@code cli/commands/**}
 * {@code *Command.java} and from the {@code @Option(longName = "…")} literals on each command method's parameters, for
 * the tests that check a document naming CLI commands and options against the ones that exist. Every command declares
 * its own options, the connection ones ({@code --profile}, {@code --host}, …) included, so the parameter list of a
 * command method is the whole truth about the options it accepts.
 *
 * @author Ivica Cardic
 */
final class CliCommandInventory {

    private static final String COMMAND_ANNOTATION = "@Command(";

    private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^\\s*name\\s*=\\s*\"([^\"]+)\"");

    private static final String OPTION_ANNOTATION = "@Option(";

    private static final Pattern OPTION_LONG_NAME_PATTERN = Pattern.compile("longName\\s*=\\s*\"([^\"]+)\"");

    private CliCommandInventory() {
    }

    static Path repositoryRoot() {
        Path workingDirectory = Path.of("")
            .toAbsolutePath();

        Path directory = workingDirectory;

        while (directory != null && !Files.exists(directory.resolve("settings.gradle.kts"))) {
            directory = directory.getParent();
        }

        if (directory == null) {
            throw new IllegalStateException("Could not locate the repository root from " + workingDirectory);
        }

        return directory;
    }

    static Set<String> commandNames(Path repositoryRoot) throws IOException {
        Map<String, Set<String>> commandOptions = commandOptions(repositoryRoot);

        return new HashSet<>(commandOptions.keySet());
    }

    static Map<String, Set<String>> commandOptions(Path repositoryRoot) throws IOException {
        List<Path> commandFiles;

        try (Stream<Path> paths = Files.walk(repositoryRoot.resolve("cli/commands"))) {
            commandFiles = paths.filter(path -> String.valueOf(path.getFileName())
                .endsWith("Command.java"))
                .filter(path -> !path.toString()
                    .contains("/build/"))
                .toList();
        }

        Map<String, Set<String>> commandOptions = new LinkedHashMap<>();

        for (Path commandFile : commandFiles) {
            commandOptions.putAll(commandOptions(Files.readString(commandFile)));
        }

        return commandOptions;
    }

    static Map<String, Set<String>> commandOptions(String source) {
        Map<String, Set<String>> commandOptions = new LinkedHashMap<>();
        int searchFrom = 0;

        while (true) {
            int annotationStart = source.indexOf(COMMAND_ANNOTATION, searchFrom);

            if (annotationStart < 0) {
                break;
            }

            int annotationOpen = annotationStart + COMMAND_ANNOTATION.length() - 1;
            int annotationClose = closingParenthesis(source, annotationOpen);

            searchFrom = annotationClose + 1;

            Matcher nameMatcher = COMMAND_NAME_PATTERN.matcher(source.substring(annotationOpen + 1, annotationClose));

            if (!nameMatcher.find()) {
                continue;
            }

            int parametersOpen = parameterListStart(source, annotationClose + 1);
            int parametersClose = closingParenthesis(source, parametersOpen);

            commandOptions.put(
                nameMatcher.group(1), optionLongNames(source.substring(parametersOpen + 1, parametersClose)));
        }

        return commandOptions;
    }

    private static int parameterListStart(String source, int from) {
        int index = from;

        while (true) {
            while (Character.isWhitespace(source.charAt(index))) {
                index++;
            }

            if (source.charAt(index) != '@') {
                break;
            }

            while (source.charAt(index) != '(' && !Character.isWhitespace(source.charAt(index))) {
                index++;
            }

            if (source.charAt(index) == '(') {
                index = closingParenthesis(source, index) + 1;
            }
        }

        return source.indexOf('(', index);
    }

    private static Set<String> optionLongNames(String parameters) {
        Set<String> longNames = new HashSet<>();
        int searchFrom = 0;

        while (true) {
            int annotationStart = parameters.indexOf(OPTION_ANNOTATION, searchFrom);

            if (annotationStart < 0) {
                break;
            }

            int annotationOpen = annotationStart + OPTION_ANNOTATION.length() - 1;
            int annotationClose = closingParenthesis(parameters, annotationOpen);

            searchFrom = annotationClose + 1;

            Matcher matcher =
                OPTION_LONG_NAME_PATTERN.matcher(parameters.substring(annotationOpen + 1, annotationClose));

            if (matcher.find()) {
                longNames.add(matcher.group(1));
            }
        }

        return longNames;
    }

    private static int closingParenthesis(String source, int openParenthesis) {
        int depth = 0;

        for (int index = openParenthesis; index < source.length(); index++) {
            char character = source.charAt(index);

            if (character == '"') {
                index = endOfStringLiteral(source, index);
            } else if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;

                if (depth == 0) {
                    return index;
                }
            }
        }

        throw new IllegalStateException("Unbalanced parentheses after: " + source.substring(openParenthesis));
    }

    private static int endOfStringLiteral(String source, int openQuote) {
        int index = openQuote + 1;

        while (index < source.length() && source.charAt(index) != '"') {
            index += source.charAt(index) == '\\' ? 2 : 1;
        }

        return index;
    }
}
