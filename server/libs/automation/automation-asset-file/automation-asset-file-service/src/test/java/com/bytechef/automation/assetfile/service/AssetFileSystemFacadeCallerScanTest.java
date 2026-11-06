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

package com.bytechef.automation.assetfile.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Ivica Cardic
 */
class AssetFileSystemFacadeCallerScanTest {

    private static final String SETTINGS_FILE_NAME = "settings.gradle.kts";

    private static final String SYSTEM_FACADE_SIMPLE_NAME = "AssetFileSystemFacade";

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/|//[^\\r\\n]*", Pattern.DOTALL);

    private static final Set<String> EXPECTED_SYSTEM_FACADE_CALLERS = Set.of(
        "AssetFileComponentHandler",
        "AssetFileDeleteAction",
        "AssetFileDownloadAction",
        "AssetFileFacadeImpl",
        "AssetFileFindAction",
        "AssetFileGetAction",
        "AssetFilePublicDownloadController",
        "AssetFileRenameAction",
        "AssetFileUpdateContentAction",
        "AssetFileUploadAction",
        "AiHubRoutingAgent",
        "WebhookBridgeAgent",
        "AiHubConfiguration",
        "AssetFileSystemFacade",
        "AssetFileSystemFacadeImpl",
        "RemoteAssetFileSystemFacadeClient");

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void testOnlyExpectedClassesReferenceTheSystemFacade() throws IOException {
        List<Path> mainSourceFiles = findMainSourceFiles(repositoryRoot().resolve("server"));

        assertThat(mainSourceFiles)
            .as("Found no *.java files under any src/main/java directory beneath server/ - the file walk itself is "
                + "broken, this is not a claim that server/ has no production sources.")
            .isNotEmpty();

        List<String> unexpected = mainSourceFiles.stream()
            .filter(AssetFileSystemFacadeCallerScanTest::referencesSystemFacade)
            .map(AssetFileSystemFacadeCallerScanTest::simpleClassName)
            .filter(className -> !EXPECTED_SYSTEM_FACADE_CALLERS.contains(className))
            .sorted()
            .toList();

        assertThat(unexpected)
            .as("AssetFileSystemFacade performs an ownership check instead of a membership check, which is only "
                + "sound for a caller with no principal and a server-derived workspace. A caller that HAS a "
                + "principal must use AssetFileFacade; reaching for this one turns the substitution into a bypass. "
                + "If this caller is genuinely principal-less, add it above in the same commit.")
            .isEmpty();
    }

    @Test
    void testTheSystemFacadeOffersExactlyTheEightPermittedOperations() {
        Set<String> methodNames = Arrays.stream(AssetFileSystemFacade.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertThat(methodNames)
            .as("adding a method here widens what a caller with no principal can do on nothing but a "
                + "server-derived workspace; the three link-minting operations are excluded deliberately")
            .containsExactlyInAnyOrder(
                "createFromUpload", "findAllByWorkspaceIdAndEnvironment", "findByIdInWorkspace",
                "renameInWorkspace", "deleteInWorkspace", "downloadContentInWorkspace",
                "updateContentInWorkspace", "fetchByPublicLinkToken");
    }

    private static boolean referencesSystemFacade(Path sourceFile) {
        String source = readString(sourceFile);

        if (!source.contains(SYSTEM_FACADE_SIMPLE_NAME)) {
            return false;
        }

        String sourceWithoutComments = COMMENT_PATTERN.matcher(source)
            .replaceAll("");

        return sourceWithoutComments.contains(SYSTEM_FACADE_SIMPLE_NAME);
    }

    private static String readString(Path sourceFile) {
        try {
            return Files.readString(sourceFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String simpleClassName(Path sourceFile) {
        String fileName = String.valueOf(sourceFile.getFileName());

        return fileName.substring(0, fileName.length() - ".java".length());
    }

    private static Path repositoryRoot() {
        Path candidate = Path.of("")
            .toAbsolutePath();

        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve(SETTINGS_FILE_NAME))) {
                return candidate;
            }

            candidate = candidate.getParent();
        }

        throw new IllegalStateException(
            "Could not locate " + SETTINGS_FILE_NAME + " by walking up from "
                + Path.of("")
                    .toAbsolutePath()
                + " - the working directory is unexpected.");
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
