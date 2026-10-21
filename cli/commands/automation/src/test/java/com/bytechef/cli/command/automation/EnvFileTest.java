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

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class EnvFileTest {

    @TempDir
    Path tempDir;

    private static Map<String, String> values(String bytechefUrl, String appKey) {
        Map<String, String> values = new LinkedHashMap<>();

        values.put(EnvFile.BYTECHEF_URL, bytechefUrl);
        values.put(EnvFile.BYTECHEF_APP_KEY, appKey);

        return values;
    }

    private static boolean isPosix(Path path) {
        return path.getFileSystem()
            .supportedFileAttributeViews()
            .contains("posix");
    }

    private List<String> fileNamesIn(Path directory) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.map(path -> String.valueOf(path.getFileName()))
                .sorted()
                .toList();
        }
    }

    @Test
    void testMergeReplacesOnlyTheByteChefLinesAndKeepsEveryOtherLine() {
        List<String> lines = List.of(
            "# local settings", "OTHER_SECRET=keep-me", "export BYTECHEF_URL=https://old.example",
            "BYTECHEF_APP_KEY=old-key", "", "LAST=1");

        List<String> mergedLines = EnvFile.merge(lines, values("https://new.example", "new-key"));

        assertEquals(
            List.of(
                "# local settings", "OTHER_SECRET=keep-me", "export BYTECHEF_URL=https://new.example",
                "BYTECHEF_APP_KEY=new-key", "", "LAST=1"),
            mergedLines);
    }

    @Test
    void testMergeAppendsMissingKeysAndLeavesCommentedOutKeysAlone() {
        List<String> lines = List.of("OTHER=1", "# BYTECHEF_APP_KEY=commented");

        List<String> mergedLines = EnvFile.merge(lines, values("https://new.example", "new-key"));

        assertEquals(
            List.of(
                "OTHER=1", "# BYTECHEF_APP_KEY=commented", "BYTECHEF_URL=https://new.example",
                "BYTECHEF_APP_KEY=new-key"),
            mergedLines);
    }

    @Test
    void testWriteCreatesAnOwnerOnlyFile() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path envFile = tempDir.resolve(".env.local");

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-1");

        assertEquals(
            "BYTECHEF_URL=https://bytechef.example\nBYTECHEF_APP_KEY=app-key-1\n", Files.readString(envFile));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(envFile));
    }

    @SuppressFBWarnings(
        value = "OVERLY_PERMISSIVE_FILE_PERMISSION",
        justification = "The test needs a world-readable file to prove the writer tightens it.")
    @Test
    void testWriteTightensAnExistingFileAndKeepsItsOtherLines() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path envFile = tempDir.resolve(".env.local");

        Files.writeString(envFile, "OTHER_SECRET=keep-me\nBYTECHEF_APP_KEY=old-key\n\n# end\n");
        Files.setPosixFilePermissions(envFile, PosixFilePermissions.fromString("rw-r--r--"));

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-2");

        assertEquals(
            "OTHER_SECRET=keep-me\nBYTECHEF_APP_KEY=app-key-2\n\n# end\nBYTECHEF_URL=https://bytechef.example\n",
            Files.readString(envFile));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(envFile));
    }

    @Test
    void testWriteLeavesNoTemporaryFileBehind() throws IOException {
        EnvFile.writeBytechefValues(tempDir.resolve(".env.local"), "https://bytechef.example", "app-key-3");

        assertEquals(List.of(".env.local"), fileNamesIn(tempDir));
    }

    @Test
    void testWriteOntoADirectoryFailsAndLeavesNoTemporaryFile() throws IOException {
        Path envDirectory = tempDir.resolve("env");

        Files.createDirectories(envDirectory.resolve("inside"));

        assertThrows(
            IOException.class,
            () -> EnvFile.writeBytechefValues(envDirectory, "https://bytechef.example", "app-key-4"));
        assertEquals(List.of("env"), fileNamesIn(tempDir));
    }

    @Test
    void testWriteKeepsCrlfLineEndingsAndTheLackOfAFinalNewline() throws IOException {
        Path envFile = tempDir.resolve(".env.local");

        Files.writeString(envFile, "OTHER=1\r\nBYTECHEF_APP_KEY=old-key\r\nLAST=2", StandardCharsets.UTF_8);

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-5");

        assertEquals(
            "OTHER=1\r\nBYTECHEF_APP_KEY=app-key-5\r\nLAST=2\r\nBYTECHEF_URL=https://bytechef.example",
            Files.readString(envFile));
    }

    @Test
    void testWriteKeepsALoneCarriageReturnAndAFinalNewline() throws IOException {
        Path envFile = tempDir.resolve(".env.local");

        Files.writeString(envFile, "A=1\rBYTECHEF_URL=old\nB=2\n", StandardCharsets.UTF_8);

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-6");

        assertEquals(
            "A=1\rBYTECHEF_URL=https://bytechef.example\nB=2\nBYTECHEF_APP_KEY=app-key-6\n",
            Files.readString(envFile));
    }

    @Test
    void testWriteKeepsAFileWithoutAFinalNewlineWhenOnlyReplacing() throws IOException {
        Path envFile = tempDir.resolve(".env.local");

        Files.writeString(envFile, "BYTECHEF_URL=old\nBYTECHEF_APP_KEY=old-key", StandardCharsets.UTF_8);

        EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-7");

        assertEquals(
            "BYTECHEF_URL=https://bytechef.example\nBYTECHEF_APP_KEY=app-key-7", Files.readString(envFile));
    }

    @Test
    void testWriteRefusesALiveSymbolicLinkAndLeavesItsTargetAlone() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path target = tempDir.resolve("shared.env");

        Files.writeString(target, "SHARED=1\n");

        Path envFile = tempDir.resolve(".env.local");

        Files.createSymbolicLink(envFile, target);

        IOException exception = assertThrows(
            IOException.class, () -> EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-8"));

        assertTrue(exception.getMessage()
            .contains(envFile.toString()));
        assertTrue(Files.isSymbolicLink(envFile));
        assertEquals("SHARED=1\n", Files.readString(target));
        assertEquals(List.of(".env.local", "shared.env"), fileNamesIn(tempDir));
    }

    @Test
    void testWriteRefusesADanglingSymbolicLink() throws IOException {
        assumeTrue(isPosix(tempDir));

        Path envFile = tempDir.resolve(".env.local");

        Files.createSymbolicLink(envFile, tempDir.resolve("missing.env"));

        assertThrows(
            IOException.class, () -> EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "app-key-9"));
        assertTrue(Files.isSymbolicLink(envFile));
        assertFalse(Files.exists(tempDir.resolve("missing.env")));
        assertEquals(List.of(".env.local"), fileNamesIn(tempDir));
    }

    @Test
    void testWriteRejectsValuesThatCouldInjectLinesWithoutEchoingThem() throws IOException {
        Path envFile = tempDir.resolve(".env.local");

        IllegalArgumentException keyException = assertThrows(
            IllegalArgumentException.class,
            () -> EnvFile.writeBytechefValues(envFile, "https://bytechef.example", "secret\nINJECTED=1"));

        assertFalse(keyException.getMessage()
            .contains("secret"));

        assertThrows(
            IllegalArgumentException.class,
            () -> EnvFile.writeBytechefValues(envFile, "https://x.example\rINJECTED=1", "app-key"));
        assertThrows(
            IllegalArgumentException.class, () -> EnvFile.writeBytechefValues(envFile, null, "app-key"));
        assertThrows(
            IllegalArgumentException.class, () -> EnvFile.writeBytechefValues(envFile, "https://x.example", "  "));
        assertEquals(List.of(), fileNamesIn(tempDir));
    }

    @Test
    void testGitIgnoreStatusIsIgnoredWhenTheRepositoryIgnoresTheFile() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), ".env.local\n");
        Path envFile = repository.resolve(".env.local");

        Files.writeString(envFile, "X=1\n");

        assertEquals(EnvFile.GitIgnoreStatus.IGNORED, EnvFile.gitIgnoreStatus(envFile));
    }

    @Test
    void testGitIgnoreStatusIsNotIgnoredWhenTheRepositoryUnignoresTheFile() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), "!.env.local\n");
        Path envFile = repository.resolve(".env.local");

        Files.writeString(envFile, "X=1\n");

        assertEquals(EnvFile.GitIgnoreStatus.NOT_IGNORED, EnvFile.gitIgnoreStatus(envFile));
    }
}
