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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AppLinkFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteThenReadRoundTripsWithoutASecret() throws IOException {
        AppLink appLink = new AppLink(42, "app-uuid", 1, "DEVELOPMENT", "http://localhost:9555");

        AppLinkFile.write(tempDir, appLink);

        assertEquals(appLink, AppLinkFile.read(tempDir));

        String content = Files.readString(tempDir.resolve(".bytechef/app.json"));

        assertTrue(content.contains("\"uuid\" : \"app-uuid\""), content);
        assertFalse(content.toLowerCase()
            .contains("key"), content);
    }

    @Test
    void testFindWalksUpFromANestedDirectory() throws IOException {
        Path appDirectory = tempDir.resolve("app");

        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", "http://localhost:9555"));

        Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/server"));

        Path expectedDirectory = appDirectory.toAbsolutePath()
            .normalize();

        assertEquals(Optional.of(expectedDirectory), AppLinkFile.findAppDirectory(nestedDirectory));
    }

    @Test
    void testFindSkipsABytechefDirectoryWithoutAppJson() throws IOException {
        Path homeDirectory = tempDir.resolve("home");

        Files.createDirectories(homeDirectory.resolve(".bytechef"));
        Files.writeString(homeDirectory.resolve(".bytechef/config"), "[default]\nhost = http://localhost:9555\n");

        Path projectDirectory = Files.createDirectories(homeDirectory.resolve("projects/my-app"));

        assertEquals(Optional.empty(), AppLinkFile.findAppDirectory(projectDirectory));
    }

    @Test
    void testReadRejectsAFileMissingAFieldAndNamesIt() throws IOException {
        Path appDirectory = Files.createDirectories(tempDir.resolve("incomplete/.bytechef"));

        Files.writeString(appDirectory.resolve("app.json"), "{\"id\": 42, \"uuid\": \"app-uuid\"}");

        IOException exception = assertThrows(IOException.class, () -> AppLinkFile.read(appDirectory.getParent()));

        assertTrue(exception.getMessage()
            .contains(appDirectory.resolve("app.json")
                .toString()));
    }

    @Test
    void testReadRejectsBlankOrNonPositiveValues() throws IOException {
        Path appDirectory = Files.createDirectories(tempDir.resolve("blank/.bytechef"));

        Files.writeString(
            appDirectory.resolve("app.json"),
            "{\"id\": 0, \"uuid\": \"\", \"workspaceId\": 1, \"environment\": \"DEVELOPMENT\", \"host\": \"h\"}");

        assertThrows(IOException.class, () -> AppLinkFile.read(appDirectory.getParent()));

        Files.writeString(
            appDirectory.resolve("app.json"),
            "{\"id\": 1, \"uuid\": \"u\", \"workspaceId\": 1, \"environment\": null, \"host\": \"h\"}");

        assertThrows(IOException.class, () -> AppLinkFile.read(appDirectory.getParent()));
    }

    @Test
    void testReadRejectsMalformedJsonAsAnIoException() throws IOException {
        Path appDirectory = Files.createDirectories(tempDir.resolve("malformed/.bytechef"));

        Files.writeString(appDirectory.resolve("app.json"), "{not json");

        assertThrows(IOException.class, () -> AppLinkFile.read(appDirectory.getParent()));
    }

    @Test
    void testWriteRefusesToReplaceAnExistingLinkAndLeavesNoTemporaryFile() throws IOException {
        AppLink original = new AppLink(42, "app-uuid", 1, "DEVELOPMENT", "http://localhost:9555");

        AppLinkFile.write(tempDir, original);

        assertThrows(
            FileAlreadyExistsException.class,
            () -> AppLinkFile.write(tempDir, new AppLink(43, "other-uuid", 1, "DEVELOPMENT", "http://localhost:9555")));
        assertEquals(original, AppLinkFile.read(tempDir));

        try (Stream<Path> paths = Files.list(tempDir.resolve(".bytechef"))) {
            assertEquals(
                List.of("app.json"), paths.map(path -> String.valueOf(path.getFileName()))
                    .toList());
        }
    }

    @Test
    void testPublishFallsBackToAMoveWhenTheFileSystemHasNoHardLinks() throws IOException {
        Path temporaryFile = Files.writeString(tempDir.resolve(".app.1.tmp"), "{}\n");
        Path target = tempDir.resolve("app.json");

        AppLinkFile.publish(
            temporaryFile, target, (link, existing) -> {
                throw new FileSystemException(link.toString(), existing.toString(), "Operation not supported");
            });

        assertEquals("{}\n", Files.readString(target));
        assertFalse(Files.exists(temporaryFile));
    }

    @Test
    void testPublishNeverReplacesAnExistingTarget() throws IOException {
        Path temporaryFile = Files.writeString(tempDir.resolve(".app.1.tmp"), "{\"new\": true}\n");
        Path target = Files.writeString(tempDir.resolve("app.json"), "{\"old\": true}\n");

        assertThrows(
            FileAlreadyExistsException.class,
            () -> AppLinkFile.publish(temporaryFile, target, Files::createLink));
        assertThrows(
            FileAlreadyExistsException.class,
            () -> AppLinkFile.publish(
                temporaryFile, target, (link, existing) -> {
                    throw new FileSystemException(link.toString(), existing.toString(), "Operation not supported");
                }));
        assertEquals("{\"old\": true}\n", Files.readString(target));
    }

    @Test
    void testReadNamesTheFileWhenItIsNotUtf8() throws IOException {
        Path appDirectory = Files.createDirectories(tempDir.resolve("latin1/.bytechef"));

        Files.write(appDirectory.resolve("app.json"),
            "{\"host\": \"caf\u00e9\"}".getBytes(StandardCharsets.ISO_8859_1));

        IOException exception = assertThrows(IOException.class, () -> AppLinkFile.read(appDirectory.getParent()));

        assertTrue(exception.getMessage()
            .startsWith("Cannot read " + appDirectory.resolve("app.json")), exception.getMessage());
    }
}
