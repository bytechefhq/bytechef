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

package com.bytechef.platform.ai.guardrails;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Ivica Cardic
 */
class GuardrailSurfaceCoverageTest {

    private static final String SETTINGS_FILE_NAME = "settings.gradle.kts";

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/|//[^\\r\\n]*", Pattern.DOTALL);

    private static final Pattern DIRECT_MODEL_CALL_PATTERN =
        Pattern.compile("\\w*[Cc]hatModel\\s*\\.\\s*call\\s*\\(|\\.\\s*call\\s*\\(\\s*new\\s+Prompt\\s*\\(");

    private static final String DIRECT_MODEL_CALL_HINT = ".call(";

    private static final Set<String> EXEMPT_DIRECT_MODEL_CALLERS = Set.of("ApiConnectorAiServiceImpl");

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testEveryDirectModelCallerIsAKnownExemption() throws IOException {
        Path serverRoot = findRepositoryRoot().resolve("server");

        assertTrue(Files.isDirectory(serverRoot), "server/ directory not found under " + serverRoot);

        List<Path> mainSourceFiles = findMainSourceFiles(serverRoot);

        assertFalse(
            mainSourceFiles.isEmpty(),
            "Found no *.java files under any src/main/java beneath " + serverRoot
                + " - the file walk itself is broken, this is not a claim that server/ has no production sources.");

        List<String> unexpected = new ArrayList<>();

        for (Path sourceFile : mainSourceFiles) {
            String source = Files.readString(sourceFile);

            if (!source.contains(DIRECT_MODEL_CALL_HINT)) {
                continue;
            }

            String sourceWithoutComments = COMMENT_PATTERN.matcher(source)
                .replaceAll("");

            if (!DIRECT_MODEL_CALL_PATTERN.matcher(sourceWithoutComments)
                .find()) {

                continue;
            }

            if (!EXEMPT_DIRECT_MODEL_CALLERS.contains(simpleClassName(sourceFile))) {
                unexpected.add(sourceFile.toString());
            }
        }

        if (!unexpected.isEmpty()) {
            fail(
                "Found " + unexpected.size() + " production class(es) calling a model directly rather than through a "
                    + "ChatClient. Guardrails attach as a ChatClient advisor, so such a call cannot be brought under "
                    + "guardrails by any settings change - it is unguardable until the call itself is rewritten. Use "
                    + "a ChatClient so the advisor chain applies, or add the class to "
                    + "EXEMPT_DIRECT_MODEL_CALLERS with the reason it must bypass guardrails:\n"
                    + String.join("\n", unexpected));
        }
    }

    @Test
    void testEverySurfaceNameIsRegistered() {
        assertTrue(
            GuardrailSurface.ALL.contains(GuardrailSurface.AI_AGENT),
            "GuardrailSurface.ALL must list every declared surface; a surface missing from it resolves its own "
                + "policy and reports its own metrics under a name nothing else uses.");

        assertTrue(
            GuardrailSurface.ALL.size() == 1,
            "A surface was added or removed without updating this test. Adding one is a real change: it introduces a "
                + "place where workspace data reaches a model, so the addition should be reviewed rather than "
                + "absorbed. Found: " + GuardrailSurface.ALL);
    }

    private static String simpleClassName(Path sourceFile) {
        String fileName = String.valueOf(sourceFile.getFileName());

        return fileName.substring(0, fileName.length() - ".java".length());
    }

    private static Path findRepositoryRoot() {
        Path candidate = Path.of("")
            .toAbsolutePath();

        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve(SETTINGS_FILE_NAME))) {
                return candidate;
            }

            candidate = candidate.getParent();
        }

        throw new IllegalStateException("Could not locate " + SETTINGS_FILE_NAME + " by walking up from the "
            + "working directory, which is unexpected.");
    }

    private static List<Path> findMainSourceFiles(Path serverRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(serverRoot)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString()
                    .endsWith(".java"))
                .filter(path -> {
                    String normalizedPath = path.toString()
                        .replace(File.separatorChar, '/');

                    return normalizedPath.contains("/src/main/java/") && !normalizedPath.contains("/build/");
                })
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        }
    }
}
