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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

/**
 * Finds, reads and writes {@code .bytechef/app.json}. The lookup walks up from a directory like git does and keys on
 * the {@code app.json} file, never on the {@code .bytechef} directory alone, because the CLI keeps its own
 * configuration in {@code ~/.bytechef/}.
 *
 * @author Ivica Cardic
 */
final class AppLinkFile {

    static final String DIRECTORY_NAME = ".bytechef";
    static final String FILE_NAME = "app.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private AppLinkFile() {
    }

    static Path pathIn(Path appDirectory) {
        Path linkDirectory = appDirectory.resolve(DIRECTORY_NAME);

        return linkDirectory.resolve(FILE_NAME);
    }

    static Optional<Path> findAppDirectory(Path start) {
        Path absoluteStart = start.toAbsolutePath();

        Path directory = absoluteStart.normalize();

        while (directory != null) {
            if (Files.isRegularFile(pathIn(directory))) {
                return Optional.of(directory);
            }

            directory = directory.getParent();
        }

        return Optional.empty();
    }

    static AppLink read(Path appDirectory) throws IOException {
        Path file = pathIn(appDirectory);
        String content;

        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IOException("Cannot read " + file + ": " + exception.getMessage(), exception);
        }

        AppLink appLink;

        try {
            appLink = OBJECT_MAPPER.readValue(content, AppLink.class);
        } catch (JsonProcessingException exception) {
            throw new IOException(
                "Cannot read " + file + ": " + exception.getOriginalMessage() +
                    ". Delete the file and run 'bytechef automation app link --app-id <id>' to recreate it.",
                exception);
        }

        if (appLink == null || appLink.id() <= 0 || appLink.workspaceId() <= 0 || isBlank(appLink.uuid()) ||
            isBlank(appLink.environment()) || isBlank(appLink.host())) {

            throw new IOException(
                "Incomplete app link in " + file +
                    ". Delete the file and run 'bytechef automation app link --app-id <id>' to recreate it.");
        }

        return appLink;
    }

    @SuppressFBWarnings(
        value = {
            "OVERLY_PERMISSIVE_FILE_PERMISSION", "PATH_TRAVERSAL_IN"
        },
        justification = "The link file is meant to be committed, so it keeps ordinary readable permissions; the app "
            + "directory is the CLI user's own working directory or one of its parents.")
    static void write(Path appDirectory, AppLink appLink) throws IOException {
        Path linkDirectory = appDirectory.resolve(DIRECTORY_NAME);

        Files.createDirectories(linkDirectory);

        Path target = linkDirectory.resolve(FILE_NAME);
        Path temporaryFile = Files.createTempFile(linkDirectory, ".app.", ".tmp");

        try {
            if (temporaryFile.getFileSystem()
                .supportedFileAttributeViews()
                .contains("posix")) {

                Files.setPosixFilePermissions(temporaryFile, PosixFilePermissions.fromString("rw-r--r--"));
            }

            Files.writeString(
                temporaryFile, OBJECT_MAPPER.writeValueAsString(appLink) + "\n", StandardCharsets.UTF_8);

            publish(temporaryFile, target, Files::createLink);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Puts the written temporary file in place without replacing an existing target: a hard link where the file system
     * has them, a move otherwise.
     */
    static void publish(Path temporaryFile, Path target, HardLinker hardLinker) throws IOException {
        try {
            hardLinker.createLink(target, temporaryFile);
        } catch (FileAlreadyExistsException exception) {
            throw exception;
        } catch (UnsupportedOperationException | FileSystemException exception) {
            Files.move(temporaryFile, target);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    interface HardLinker {

        void createLink(Path link, Path existing) throws IOException;
    }
}
