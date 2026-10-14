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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskRunnerOutputsTest {

    @Test
    void testReadOutputJsonReturnsNullWhenTheExecutionWroteNone() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request(List.of()))) {
            assertThat(TaskRunnerOutputs.readOutputJson(workingDirectory)).isNull();
        }
    }

    @Test
    void testReadOutputJsonReturnsTheParsedValue() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request(List.of()))) {
            writeOutputFile(workingDirectory, "output.json", "{\"greeting\": \"hi\", \"count\": 2}");

            assertThat(TaskRunnerOutputs.readOutputJson(workingDirectory))
                .isEqualTo(Map.of("greeting", "hi", "count", 2));
        }
    }

    @Test
    void testReadOutputJsonReturnsNullForABlankFile() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request(List.of()))) {
            writeOutputFile(workingDirectory, "output.json", "   ");

            assertThat(TaskRunnerOutputs.readOutputJson(workingDirectory)).isNull();
        }
    }

    @Test
    void testReadOutputJsonDoesNotFollowASymbolicLink() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request(List.of()))) {
            Path targetPath = writeLinkTarget(workingDirectory, "{\"secret\": true}");

            assumeTrue(createSymbolicLink(
                workingDirectory.getOutputPath()
                    .resolve("output.json"),
                targetPath));

            assertThat(TaskRunnerOutputs.readOutputJson(workingDirectory)).isNull();
        }
    }

    @Test
    void testReadOutputJsonDoesNotFollowASymbolicLinkedOutputDirectory() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request(List.of()))) {
            assumeTrue(replaceOutputDirectoryWithASymbolicLink(workingDirectory, "{\"secret\": true}"));

            assertThat(TaskRunnerOutputs.readOutputJson(workingDirectory)).isNull();
        }
    }

    @Test
    void testCollectOutputFilesDoesNotFollowASymbolicLinkedOutputDirectory() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of("*"), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            assumeTrue(replaceOutputDirectoryWithASymbolicLink(workingDirectory, "{\"secret\": true}"));

            assertThat(TaskRunnerOutputs.collectOutputFiles(request, workingDirectory)).isEmpty();
            assertThat(storedFiles).isEmpty();
        }
    }

    @Test
    void testCollectOutputFilesCollectsNothingWithoutPatterns() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of(), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            writeOutputFile(workingDirectory, "report.csv", "a,b\n");

            assertThat(TaskRunnerOutputs.collectOutputFiles(request, workingDirectory)).isEmpty();
            assertThat(storedFiles).isEmpty();
        }
    }

    @Test
    void testCollectOutputFilesStoresOnlyTheMatchingFiles() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of("*.csv"), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            writeOutputFile(workingDirectory, "report.csv", "a,b\n");
            writeOutputFile(workingDirectory, "notes.txt", "ignored");

            Map<String, FileEntry> outputFiles = TaskRunnerOutputs.collectOutputFiles(request, workingDirectory);

            assertThat(outputFiles).containsOnlyKeys("report.csv");
            assertThat(storedFiles).containsOnlyKeys("report.csv");
            assertThat(storedFiles.get("report.csv")).isEqualTo("a,b\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void testCollectOutputFilesMatchesANestedGlob() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of("**/*.csv"), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            writeOutputFile(workingDirectory, "nested/report.csv", "a,b\n");

            Map<String, FileEntry> outputFiles = TaskRunnerOutputs.collectOutputFiles(request, workingDirectory);

            assertThat(outputFiles).containsOnlyKeys(
                Path.of("nested", "report.csv")
                    .toString());
        }
    }

    @Test
    void testCollectOutputFilesExcludesOutputJson() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of("*"), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            writeOutputFile(workingDirectory, "output.json", "{\"greeting\": \"hi\"}");
            writeOutputFile(workingDirectory, "report.csv", "a,b\n");

            assertThat(TaskRunnerOutputs.collectOutputFiles(request, workingDirectory))
                .containsOnlyKeys("report.csv");
        }
    }

    @Test
    void testCollectOutputFilesDoesNotFollowASymbolicLink() throws IOException {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerRequest request = request(List.of("*.txt"), storingActionContext(storedFiles));

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            Path targetPath = writeLinkTarget(workingDirectory, "host-only-secret");

            assumeTrue(createSymbolicLink(
                workingDirectory.getOutputPath()
                    .resolve("leak.txt"),
                targetPath));

            assertThat(TaskRunnerOutputs.collectOutputFiles(request, workingDirectory)).isEmpty();
            assertThat(storedFiles).isEmpty();
        }
    }

    private static boolean createSymbolicLink(Path linkPath, Path targetPath) {
        try {
            Files.createSymbolicLink(linkPath, targetPath);

            return true;
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private static boolean replaceOutputDirectoryWithASymbolicLink(
        TaskRunnerWorkingDirectory workingDirectory, String targetOutputJson) throws IOException {

        Path targetDirectoryPath = Files.createDirectory(
            workingDirectory.getPath()
                .resolve("elsewhere"));

        Files.writeString(targetDirectoryPath.resolve("output.json"), targetOutputJson, StandardCharsets.UTF_8);

        Path outputPath = workingDirectory.getOutputPath();

        Files.delete(outputPath);

        return createSymbolicLink(outputPath, targetDirectoryPath);
    }

    private static TaskRunnerRequest request(List<String> outputFilePatterns) {
        return request(outputFilePatterns, mock(ActionContext.class));
    }

    private static TaskRunnerRequest request(List<String> outputFilePatterns, ActionContext actionContext) {
        return new TaskRunnerRequest(
            "javascript", "console.log(1);", List.of(), Map.of(), Map.of(), Map.of(), outputFilePatterns, null, null,
            Duration.ofMinutes(1), Map.of(), actionContext);
    }

    @SuppressWarnings("unchecked")
    private static ActionContext storingActionContext(Map<String, byte[]> storedFiles) throws IOException {
        ActionContext actionContext = mock(ActionContext.class);
        File file = mock(File.class);

        ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor =
            ArgumentCaptor.forClass(ContextFunction.class);

        when(actionContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> fileFunction = fileFunctionArgumentCaptor.getValue();

                return fileFunction.apply(file);
            });
        when(file.storeContent(anyString(), any(InputStream.class)))
            .thenAnswer(invocation -> {
                String fileName = invocation.getArgument(0);
                InputStream inputStream = invocation.getArgument(1);

                storedFiles.put(fileName, inputStream.readAllBytes());

                return mock(FileEntry.class);
            });

        return actionContext;
    }

    private static Path writeLinkTarget(TaskRunnerWorkingDirectory workingDirectory, String content)
        throws IOException {

        Path targetPath = workingDirectory.getPath()
            .resolve("link-target.json");

        Files.writeString(targetPath, content, StandardCharsets.UTF_8);

        return targetPath;
    }

    private static void writeOutputFile(TaskRunnerWorkingDirectory workingDirectory, String fileName, String content)
        throws IOException {

        Path filePath = workingDirectory.getOutputPath()
            .resolve(fileName);

        Path parentPath = filePath.getParent();

        if (parentPath != null) {
            Files.createDirectories(parentPath);
        }

        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }
}
