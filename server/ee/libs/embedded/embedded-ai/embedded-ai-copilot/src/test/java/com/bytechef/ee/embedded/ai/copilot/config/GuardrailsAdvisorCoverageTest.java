/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

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

    private static final Pattern AGENT_BUILDER_CALL_PATTERN = Pattern.compile("[A-Za-z]+SpringAIAgent\\.builder\\(\\)");

    private static final Pattern CHAT_CLIENT_BUILDER_CALL_PATTERN = Pattern.compile("ChatClient\\.builder\\(");

    private static final Pattern CHAT_CLIENT_BUILDER_WRAPPER_PATTERN =
        Pattern.compile("ChatClient\\.Builder\\s+chatClientBuilder\\(ChatModel\\s+\\w+\\)\\s*\\{");

    private static final String BUILD_CALL = ".build()";

    private static final String GUARDRAILS_ADVISORS_CALL = "guardrailsAdvisors(";

    @Test
    void testEverySpringAIAgentBuilderChainInEveryConfigurationClassAttachesGuardrails() throws IOException {
        assertTrue(
            Files.isDirectory(MAIN_SOURCE_ROOT),
            "Source root not found, working directory is wrong: " + MAIN_SOURCE_ROOT);

        List<Path> configurationSourceFiles = findConfigurationSourceFiles();

        assertFalse(
            configurationSourceFiles.isEmpty(),
            "Found no *Configuration.java files under " + MAIN_SOURCE_ROOT + " - the scan itself is broken, "
                + "this is not a claim that the module has no configuration classes.");

        List<String> uncoveredSites = new ArrayList<>();

        int totalBuilderSiteCount = 0;

        for (Path sourceFile : configurationSourceFiles) {
            String sourceWithoutComments = withoutComments(sourceFile);

            Matcher matcher = AGENT_BUILDER_CALL_PATTERN.matcher(sourceWithoutComments);

            int siteOrdinal = 0;

            while (matcher.find()) {
                siteOrdinal++;
                totalBuilderSiteCount++;

                String chain = extractChain(sourceWithoutComments, matcher.start(), '(', ')', BUILD_CALL);

                if (chain == null) {
                    uncoveredSites.add(
                        sourceFile + ": call site #" + siteOrdinal + " (" + matcher.group()
                            + ") never reaches a top-level .build() - the chain-extraction scan itself may need "
                            + "updating for new syntax, or this is a genuinely unterminated builder chain.");
                } else if (!chain.contains(GUARDRAILS_ADVISORS_CALL)) {
                    uncoveredSites.add(
                        sourceFile + ": call site #" + siteOrdinal + " (" + matcher.group()
                            + ") builds a LocalAgent bean but its chain never calls guardrailsAdvisors() before "
                            + ".build() - "
                            + "add .advisors(copilotGuardrailsAdvisorFactory.guardrailsAdvisors()) (inject "
                            + "CopilotGuardrailsAdvisorFactory as a bean-method or constructor parameter) additively, "
                            + "alongside any other advisor the bean already attaches, never replacing it.");
                }
            }
        }

        assertTrue(
            totalBuilderSiteCount > 0,
            "Found no *SpringAIAgent.builder() call sites in any *Configuration.java file under " + MAIN_SOURCE_ROOT
                + " - the pattern itself is broken, this is not a claim that the module has no such beans.");

        if (!uncoveredSites.isEmpty()) {
            fail(
                "Found " + uncoveredSites.size() + " of " + totalBuilderSiteCount + " *SpringAIAgent.builder() call "
                    + "site(s) across the module without guardrails coverage:\n"
                    + String.join("\n", uncoveredSites));
        }
    }

    @Test
    void testEveryChatClientBuilderCallSiteRoutesThroughTheModulesGuardedWrapper() throws IOException {
        List<Path> configurationSourceFiles = findConfigurationSourceFiles();

        List<String> violations = new ArrayList<>();

        for (Path sourceFile : configurationSourceFiles) {
            String sourceWithoutComments = withoutComments(sourceFile);

            Matcher wrapperMatcher = CHAT_CLIENT_BUILDER_WRAPPER_PATTERN.matcher(sourceWithoutComments);

            boolean hasWrapper = wrapperMatcher.find();
            int allowedDirectCallCount = 0;

            if (hasWrapper) {
                String wrapperBody = extractChain(
                    sourceWithoutComments, wrapperMatcher.end() - 1, '{', '}', null);

                if (wrapperBody == null) {
                    violations.add(
                        sourceFile + ": chatClientBuilder(ChatModel) wrapper's body never reaches a closing brace - "
                            + "the brace-depth scan itself may need updating for new syntax.");
                } else if (!wrapperBody.contains(GUARDRAILS_ADVISORS_CALL)) {
                    violations.add(
                        sourceFile + ": chatClientBuilder(ChatModel) wrapper never calls guardrailsAdvisors() in its "
                            + "own body - every one-shot subagent ChatClient bean in this file routes through this "
                            + "wrapper for guardrails coverage, so a broken wrapper leaves every one of them "
                            + "unguarded.");
                }

                allowedDirectCallCount = 1;
            }

            Matcher directCallMatcher = CHAT_CLIENT_BUILDER_CALL_PATTERN.matcher(sourceWithoutComments);

            int directCallCount = 0;

            while (directCallMatcher.find()) {
                directCallCount++;
            }

            if (directCallCount > allowedDirectCallCount) {
                violations.add(
                    sourceFile + ": found " + directCallCount + " ChatClient.builder( call site(s) but only "
                        + allowedDirectCallCount + " explained by this file's own chatClientBuilder(ChatModel) "
                        + "wrapper - a bean method is calling ChatClient.builder(...) directly instead of "
                        + "chatClientBuilder(chatModel), bypassing guardrails the same way this module's original "
                        + "subagent ChatClient site went unguarded.");
            }
        }

        if (!violations.isEmpty()) {
            fail(
                "Found " + violations.size() + " ChatClient.builder( coverage violation(s) across the module:\n"
                    + String.join("\n", violations));
        }
    }

    private static List<Path> findConfigurationSourceFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString()
                    .endsWith("Configuration.java"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
    }

    private static String withoutComments(Path sourceFile) throws IOException {
        return COMMENT_PATTERN.matcher(Files.readString(sourceFile))
            .replaceAll("");
    }

    private static String extractChain(
        String source, int chainStart, char openChar, char closeChar, String terminator) {

        int depth = 0;
        int position = chainStart;

        while (position < source.length()) {
            char character = source.charAt(position);

            if (character == openChar) {
                depth++;
            } else if (character == closeChar) {
                depth--;

                if (depth == 0) {
                    int endExclusive = position + 1;

                    if (terminator == null) {
                        return source.substring(chainStart, endExclusive);
                    }

                    if (source.startsWith(terminator, endExclusive - terminator.length())) {
                        return source.substring(chainStart, endExclusive);
                    }
                }
            }

            position++;
        }

        return null;
    }
}
