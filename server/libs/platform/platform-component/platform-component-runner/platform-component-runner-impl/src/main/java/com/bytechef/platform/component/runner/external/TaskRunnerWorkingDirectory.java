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

package com.bytechef.platform.component.runner.external;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class TaskRunnerWorkingDirectory implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerWorkingDirectory.class);

    private static final String INPUT_FILE_NAME = "input.json";
    private static final String OUTPUT_DIRECTORY_NAME = "output";

    private final Path path;
    private final Path inputFilePath;
    private final Path outputPath;

    private TaskRunnerWorkingDirectory(Path path, Path inputFilePath, Path outputPath) {
        this.path = path;
        this.inputFilePath = inputFilePath;
        this.outputPath = outputPath;
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static TaskRunnerWorkingDirectory create(TaskRunnerRequest request) throws IOException {
        Path path = Files.createTempDirectory("bytechef-run-" + UUID.randomUUID() + "-");

        try {
            Path outputPath = Files.createDirectory(path.resolve(OUTPUT_DIRECTORY_NAME));

            Path inputFilePath = path.resolve(INPUT_FILE_NAME);

            Files.writeString(inputFilePath, JsonUtils.write(request.input()), StandardCharsets.UTF_8);

            writeInputFiles(request, path);

            return new TaskRunnerWorkingDirectory(path, inputFilePath, outputPath);
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(path);

            throw exception;
        }
    }

    @Override
    public void close() {
        deleteRecursively(path);
    }

    public Map<String, String> getEnvironment() {
        return Map.of(
            "BYTECHEF_WORKING_DIR", path.toString(),
            "BYTECHEF_INPUT_FILE", inputFilePath.toString(),
            "BYTECHEF_OUTPUT_DIR", outputPath.toString());
    }

    public Path getInputFilePath() {
        return inputFilePath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public Path getPath() {
        return path;
    }

    public Path writeSourceFile(String fileName, String content) throws IOException {
        Path sourcePath = resolveInside(path, fileName);

        Files.writeString(sourcePath, content, StandardCharsets.UTF_8);

        return sourcePath;
    }

    private static void copyFileEntry(ActionContext actionContext, FileEntry fileEntry, Path filePath)
        throws IOException {

        try (InputStream inputStream = actionContext.file(file -> file.getInputStream(fileEntry))) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not remove {}", path, exception);
        }
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(TaskRunnerWorkingDirectory::deleteQuietly);
        } catch (IOException | UncheckedIOException exception) {
            log.warn("Could not remove the task runner working directory {}", path, exception);
        }
    }

    private static Path resolveInside(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName)
            .normalize();

        if (!candidate.startsWith(directory) || candidate.equals(directory)) {
            throw new IllegalArgumentException(
                "File name '%s' resolves outside the working directory".formatted(fileName));
        }

        return candidate;
    }

    private static void writeInputFiles(TaskRunnerRequest request, Path path) throws IOException {
        for (Map.Entry<String, ?> entry : request.inputFiles()
            .entrySet()) {

            String fileName = entry.getKey();

            Path filePath = resolveInside(path, fileName);

            Path parentPath = filePath.getParent();

            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            Object value = entry.getValue();

            if (value instanceof FileEntry fileEntry) {
                copyFileEntry(request.actionContext(), fileEntry, filePath);
            } else {
                Files.writeString(filePath, String.valueOf(value), StandardCharsets.UTF_8);
            }
        }
    }
}
