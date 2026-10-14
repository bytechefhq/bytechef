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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TaskRunnerWorkingDirectoryTest {

    @Test
    void testCreateLaysOutTheDirectory() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory =
            TaskRunnerWorkingDirectory.create(request(Map.of("name", "world"), Map.of()))) {

            Path path = workingDirectory.getPath();

            assertThat(path).isDirectory();
            assertThat(path.getFileName())
                .asString()
                .startsWith("bytechef-run-");
            assertThat(workingDirectory.getOutputPath()).isDirectory();
            assertThat(workingDirectory.getInputFilePath()).isRegularFile();
            assertThat(Files.readString(workingDirectory.getInputFilePath())).contains("world");
        }
    }

    @Test
    void testEnvironmentNamesTheThreePaths() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory =
            TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of()))) {

            Map<String, String> environment = workingDirectory.getEnvironment();

            assertThat(environment).containsOnlyKeys(
                "BYTECHEF_WORKING_DIR", "BYTECHEF_INPUT_FILE", "BYTECHEF_OUTPUT_DIR");
            assertThat(environment).containsEntry(
                "BYTECHEF_WORKING_DIR", workingDirectory.getPath()
                    .toString());
        }
    }

    @Test
    void testInlineInputFileIsMaterialised() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory =
            TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of("data.csv", "a,b\n1,2\n")))) {

            Path dataPath = workingDirectory.getPath()
                .resolve("data.csv");

            assertThat(Files.readString(dataPath)).isEqualTo("a,b\n1,2\n");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFileEntryInputFileIsMaterialised() throws IOException {
        ActionContext actionContext = mock(ActionContext.class);
        File file = mock(File.class);
        FileEntry fileEntry = mock(FileEntry.class);
        byte[] fileContent = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8);

        ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor =
            ArgumentCaptor.forClass(ContextFunction.class);

        when(actionContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> fileFunction = fileFunctionArgumentCaptor.getValue();

                return fileFunction.apply(file);
            });
        when(file.getInputStream(fileEntry)).thenReturn(new ByteArrayInputStream(fileContent));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "javascript", "console.log(1);", List.of(), Map.of(), Map.of(), Map.of("data.csv", fileEntry),
            List.of(), null, null, Duration.ofMinutes(1), Map.of(), actionContext);

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request)) {
            Path dataPath = workingDirectory.getPath()
                .resolve("data.csv");

            assertThat(Files.readAllBytes(dataPath)).isEqualTo(fileContent);
        }
    }

    @Test
    void testWriteSourceFileReturnsThePathItWrote() throws IOException {
        try (TaskRunnerWorkingDirectory workingDirectory =
            TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of()))) {

            Path sourcePath = workingDirectory.writeSourceFile("script.js", "console.log(1);");

            assertThat(sourcePath).hasFileName("script.js");
            assertThat(Files.readString(sourcePath)).isEqualTo("console.log(1);");
        }
    }

    @Test
    void testCloseRemovesTheDirectoryAndItsContents() throws IOException {
        Path path;

        try (TaskRunnerWorkingDirectory workingDirectory =
            TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of("data.csv", "x")))) {

            path = workingDirectory.getPath();

            workingDirectory.writeSourceFile("script.js", "1");
        }

        assertThat(path).doesNotExist();
    }

    @Test
    void testInputFileNameEscapingTheDirectoryIsRejected() throws IOException {
        Set<Path> runDirectoriesBefore = bytechefRunDirectories();

        assertThatThrownBy(
            () -> TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of("../escape.txt", "x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("../escape.txt");

        assertThat(bytechefRunDirectories()).isEqualTo(runDirectoriesBefore);
    }

    @Test
    void testAbsoluteInputFileNameIsRejected() throws IOException {
        Set<Path> runDirectoriesBefore = bytechefRunDirectories();

        assertThatThrownBy(
            () -> TaskRunnerWorkingDirectory.create(request(Map.of(), Map.of("/etc/passwd", "x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/etc/passwd");

        assertThat(bytechefRunDirectories()).isEqualTo(runDirectoriesBefore);
    }

    static Set<Path> bytechefRunDirectories() throws IOException {
        Path tempDirectory = Path.of(System.getProperty("java.io.tmpdir"));

        try (Stream<Path> paths = Files.list(tempDirectory)) {
            return paths.filter(TaskRunnerWorkingDirectoryTest::isBytechefRunDirectory)
                .collect(Collectors.toSet());
        }
    }

    private static boolean isBytechefRunDirectory(Path path) {
        Path fileName = path.getFileName();

        return fileName != null && fileName.toString()
            .startsWith("bytechef-run-");
    }

    private static TaskRunnerRequest request(Map<String, ?> input, Map<String, ?> inputFiles) {
        return new TaskRunnerRequest(
            "javascript", "console.log(1);", List.of(), input, Map.of(), inputFiles, List.of(), null, null,
            Duration.ofMinutes(1), Map.of(), mock(ActionContext.class));
    }
}
