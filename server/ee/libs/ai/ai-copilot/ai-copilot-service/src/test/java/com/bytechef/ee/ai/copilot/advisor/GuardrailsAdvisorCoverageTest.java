/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.advisor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class GuardrailsAdvisorCoverageTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/|//[^\\r\\n]*", Pattern.DOTALL);

    private static final Pattern DIRECT_CHAT_MODEL_CALL_PATTERN =
        Pattern.compile("\\b\\w*[cC]hatModel\\.(?:call|stream)\\(");

    private static final String PROMPT_CALL = ".prompt(";

    private static final String TERMINAL_CALL = ".call()";

    private static final String GUARDED_CHAT_CLIENT_CALL = "guardedChatClient(";

    @Test
    void testNoSourceFileInvokesAChatModelDirectly() throws IOException {
        List<Path> sourceFiles = findMainSourceFiles();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : sourceFiles) {
            String sourceWithoutComments = withoutComments(sourceFile);

            Matcher matcher = DIRECT_CHAT_MODEL_CALL_PATTERN.matcher(sourceWithoutComments);

            while (matcher.find()) {
                violations.add(
                    sourceFile + ": invokes a ChatModel directly (" + matcher.group() + "...). AiGuardrailsAdvisor is "
                        + "a ChatClient advisor, so nothing can guard a ChatModel call - route it through "
                        + "copilotGuardrailsAdvisorFactory.guardedChatClient(chatModel).prompt(...).call() instead.");
            }
        }

        if (!violations.isEmpty()) {
            fail(
                "Found " + violations.size() + " direct ChatModel invocation(s) in this module:\n"
                    + String.join("\n", violations));
        }
    }

    @Test
    void testEveryChatClientInvocationUsesAGuardedClient() throws IOException {
        List<Path> sourceFiles = findMainSourceFiles();

        assertFalse(
            sourceFiles.isEmpty(),
            "Found no .java files under " + MAIN_SOURCE_ROOT + " - the scan itself is broken, this is not a claim "
                + "that the module has no sources.");

        List<String> violations = new ArrayList<>();

        int totalInvocationCount = 0;

        for (Path sourceFile : sourceFiles) {
            String sourceWithoutComments = withoutComments(sourceFile);

            for (String statement : sourceWithoutComments.split(";")) {
                if (!statement.contains(PROMPT_CALL) || !statement.contains(TERMINAL_CALL)) {
                    continue;
                }

                totalInvocationCount++;

                if (!statement.contains(GUARDED_CHAT_CLIENT_CALL)) {
                    violations.add(
                        sourceFile + ": a ChatClient invocation statement (one carrying both " + PROMPT_CALL + " and "
                            + TERMINAL_CALL + ") does not carry " + GUARDED_CHAT_CLIENT_CALL + " - wrap the client in "
                            + "copilotGuardrailsAdvisorFactory.guardedChatClient(...) so the guardrails and PII "
                            + "tool-boundary advisors are attached before the prompt leaves the process.");
                }
            }
        }

        assertTrue(
            totalInvocationCount > 0,
            "Found no ChatClient invocation statements under " + MAIN_SOURCE_ROOT + " - the statement-splitting scan "
                + "is broken, this is not a claim that the module never calls a model.");

        if (!violations.isEmpty()) {
            fail(
                "Found " + violations.size() + " of " + totalInvocationCount + " ChatClient invocation(s) using an "
                    + "unguarded client:\n" + String.join("\n", violations));
        }
    }

    private static List<Path> findMainSourceFiles() throws IOException {
        assertTrue(
            Files.isDirectory(MAIN_SOURCE_ROOT),
            "Source root not found, working directory is wrong: " + MAIN_SOURCE_ROOT);

        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString()
                    .endsWith(".java"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
    }

    private static String withoutComments(Path sourceFile) throws IOException {
        return COMMENT_PATTERN.matcher(Files.readString(sourceFile))
            .replaceAll("");
    }
}
